use crate::core::assets::{AssetRegistry, AssetManifest, ValidationResult};
use dashmap::DashMap;
use serde::{Deserialize, Serialize};
use std::sync::Arc;
use std::sync::atomic::{AtomicBool, AtomicU32, Ordering};
use tokio::sync::RwLock;
use tracing::{info, debug};
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ServerCapabilities {
    pub cosmetics: CosmeticCapabilities,
    pub performance: PerformanceHints,
    pub connectivity: ConnectivityFeatures,
    pub sync: SyncCapabilities,
    pub features: Vec<String>,
    pub api_version: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CosmeticCapabilities {
    pub supported_types: Vec<String>,
    pub max_file_size_mb: u32,
    pub allows_animated: bool,
    pub allows_custom: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PerformanceHints {
    pub recommended_render_distance: u32,
    pub recommended_entity_distance: u32,
    pub server_tick_rate: u32,
    pub adaptive_throttling: bool,
    pub asset_preload_list: Vec<String>,
    pub texture_streaming: bool,
    pub chunk_preload_radius: u32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ConnectivityFeatures {
    pub connection_pooling: bool,
    pub keep_alive_interval_ms: u32,
    pub ping_optimization: bool,
    pub route_selection: bool,
    pub session_handoff: bool,
    pub reconnect_grace_period_secs: u32,
    pub queue_position_tracking: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SyncCapabilities {
    pub friend_activity: bool,
    pub player_presence: bool,
    pub session_transfer: bool,
    pub profile_sync: bool,
    pub settings_sync: bool,
    pub achievement_sync: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LauncherHandshake {
    pub launcher_version: String,
    pub user_id: Uuid,
    pub capabilities_requested: bool,
    pub asset_manifest: Option<AssetManifest>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct HandshakeResponse {
    pub accepted: bool,
    pub server_capabilities: Option<ServerCapabilities>,
    pub asset_validation: Option<ValidationResult>,
    pub message: Option<String>,
}

pub struct LauncherBridge {
    assets: Arc<AssetRegistry>,
    running: AtomicBool,
    capabilities: ServerCapabilities,
    connected_launchers: DashMap<Uuid, LauncherSession>,
    queue: RwLock<Vec<QueueEntry>>,
    player_count: AtomicU32,
    max_players: AtomicU32,
}

#[derive(Debug, Clone)]
pub struct LauncherSession {
    pub user_id: Uuid,
    pub launcher_version: String,
    pub connected_at: chrono::DateTime<chrono::Utc>,
    pub last_ping: chrono::DateTime<chrono::Utc>,
    pub latency_ms: u32,
    pub premium: bool,
    pub friends_online: Vec<Uuid>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct QueueEntry {
    pub user_id: Uuid,
    pub position: u32,
    pub joined_at: chrono::DateTime<chrono::Utc>,
    pub estimated_wait_secs: u32,
    pub priority: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PlayerActivity {
    pub user_id: Uuid,
    pub status: PlayerStatus,
    pub server_id: Option<Uuid>,
    pub activity: String,
    pub updated_at: chrono::DateTime<chrono::Utc>,
}

#[derive(Debug, Clone, Copy, Serialize, Deserialize, PartialEq)]
pub enum PlayerStatus {
    Online,
    InGame,
    Away,
    DoNotDisturb,
    Offline,
}

impl LauncherBridge {
    pub fn new(assets: Arc<AssetRegistry>) -> Self {
        Self {
            assets,
            running: AtomicBool::new(false),
            capabilities: ServerCapabilities {
                cosmetics: CosmeticCapabilities {
                    supported_types: vec![
                        "skin".to_string(),
                        "cape".to_string(),
                        "hat".to_string(),
                        "particle".to_string(),
                        "emote".to_string(),
                    ],
                    max_file_size_mb: 5,
                    allows_animated: true,
                    allows_custom: true,
                },
                performance: PerformanceHints {
                    recommended_render_distance: 12,
                    recommended_entity_distance: 64,
                    server_tick_rate: 20,
                    adaptive_throttling: true,
                    asset_preload_list: vec![],
                    texture_streaming: true,
                    chunk_preload_radius: 4,
                },
                connectivity: ConnectivityFeatures {
                    connection_pooling: true,
                    keep_alive_interval_ms: 15000,
                    ping_optimization: true,
                    route_selection: true,
                    session_handoff: true,
                    reconnect_grace_period_secs: 30,
                    queue_position_tracking: true,
                },
                sync: SyncCapabilities {
                    friend_activity: true,
                    player_presence: true,
                    session_transfer: true,
                    profile_sync: true,
                    settings_sync: true,
                    achievement_sync: true,
                },
                features: vec![
                    "cosmetics".to_string(),
                    "mod_profiles".to_string(),
                    "session_persistence".to_string(),
                    "relay_routing".to_string(),
                    "friend_activity".to_string(),
                    "queue_priority".to_string(),
                    "asset_streaming".to_string(),
                    "ping_optimization".to_string(),
                ],
                api_version: "1.1.0".to_string(),
            },
            connected_launchers: DashMap::new(),
            queue: RwLock::new(Vec::new()),
            player_count: AtomicU32::new(0),
            max_players: AtomicU32::new(100),
        }
    }
    
    pub async fn start(&self) {
        self.running.store(true, Ordering::SeqCst);
        info!("Launcher bridge started");
    }
    
    pub async fn stop(&self) {
        self.running.store(false, Ordering::SeqCst);
        info!("Launcher bridge stopped");
    }
    
    pub fn handle_handshake(&self, handshake: LauncherHandshake) -> HandshakeResponse {
        debug!("Processing launcher handshake from user {}", handshake.user_id);
        
        let capabilities = if handshake.capabilities_requested {
            Some(self.capabilities.clone())
        } else {
            None
        };
        
        let asset_validation = handshake.asset_manifest.map(|manifest| {
            self.assets.validate_asset_manifest(&manifest)
        });
        
        HandshakeResponse {
            accepted: true,
            server_capabilities: capabilities,
            asset_validation,
            message: Some("Welcome to the server".to_string()),
        }
    }
    
    pub fn get_capabilities(&self) -> ServerCapabilities {
        self.capabilities.clone()
    }
    
    pub fn validate_cosmetic_ownership(&self, user_id: Uuid, cosmetic_ids: &[Uuid]) -> Vec<bool> {
        cosmetic_ids.iter()
            .map(|id| self.assets.check_ownership(user_id, *id))
            .collect()
    }
    
    pub fn export_server_info(&self) -> ServerInfo {
        ServerInfo {
            name: "Pond Server".to_string(),
            description: "A modular Hytale server".to_string(),
            capabilities: self.capabilities.clone(),
            online: self.running.load(Ordering::Relaxed),
            player_count: self.player_count.load(Ordering::Relaxed),
            max_players: self.max_players.load(Ordering::Relaxed),
            queue_length: 0,
        }
    }
    
    pub async fn register_launcher(&self, handshake: &LauncherHandshake, premium: bool) -> Result<LauncherSession, String> {
        let session = LauncherSession {
            user_id: handshake.user_id,
            launcher_version: handshake.launcher_version.clone(),
            connected_at: chrono::Utc::now(),
            last_ping: chrono::Utc::now(),
            latency_ms: 0,
            premium,
            friends_online: vec![],
        };
        
        self.connected_launchers.insert(handshake.user_id, session.clone());
        info!("Registered launcher for user {}", handshake.user_id);
        Ok(session)
    }
    
    pub fn update_launcher_ping(&self, user_id: Uuid, latency_ms: u32) {
        if let Some(mut session) = self.connected_launchers.get_mut(&user_id) {
            session.last_ping = chrono::Utc::now();
            session.latency_ms = latency_ms;
        }
    }
    
    pub fn disconnect_launcher(&self, user_id: Uuid) {
        self.connected_launchers.remove(&user_id);
        debug!("Disconnected launcher for user {}", user_id);
    }
    
    pub async fn get_queue_position(&self, user_id: Uuid) -> Option<QueueEntry> {
        let queue = self.queue.read().await;
        queue.iter().find(|e| e.user_id == user_id).cloned()
    }
    
    pub async fn join_queue(&self, user_id: Uuid, priority: bool) -> QueueEntry {
        let mut queue = self.queue.write().await;
        
        if let Some(existing) = queue.iter().position(|e| e.user_id == user_id) {
            return queue[existing].clone();
        }
        
        let position = if priority {
            let priority_end = queue.iter().take_while(|e| e.priority).count();
            priority_end as u32
        } else {
            queue.len() as u32
        };
        
        let entry = QueueEntry {
            user_id,
            position,
            joined_at: chrono::Utc::now(),
            estimated_wait_secs: position * 30,
            priority,
        };
        
        if priority {
            let insert_pos = queue.iter().take_while(|e| e.priority).count();
            queue.insert(insert_pos, entry.clone());
            for (i, e) in queue.iter_mut().enumerate() {
                e.position = i as u32;
                e.estimated_wait_secs = e.position * 30;
            }
        } else {
            queue.push(entry.clone());
        }
        
        entry
    }
    
    pub async fn leave_queue(&self, user_id: Uuid) {
        let mut queue = self.queue.write().await;
        if let Some(pos) = queue.iter().position(|e| e.user_id == user_id) {
            queue.remove(pos);
            for (i, e) in queue.iter_mut().enumerate() {
                e.position = i as u32;
                e.estimated_wait_secs = e.position * 30;
            }
        }
    }
    
    pub fn get_friends_on_server(&self, friend_ids: &[Uuid]) -> Vec<PlayerActivity> {
        friend_ids.iter()
            .filter_map(|id| {
                self.connected_launchers.get(id).map(|session| {
                    PlayerActivity {
                        user_id: *id,
                        status: PlayerStatus::InGame,
                        server_id: None,
                        activity: "Playing on this server".to_string(),
                        updated_at: session.last_ping,
                    }
                })
            })
            .collect()
    }
    
    pub fn get_connected_count(&self) -> u32 {
        self.connected_launchers.len() as u32
    }
    
    pub fn get_asset_preload_manifest(&self) -> AssetPreloadManifest {
        AssetPreloadManifest {
            textures: vec![],
            models: vec![],
            sounds: vec![],
            priority_assets: vec![],
            total_size_mb: 0,
            cache_duration_hours: 24,
        }
    }
    
    pub fn get_network_optimization_hints(&self, latency_ms: u32) -> NetworkOptimizationHints {
        let update_rate = if latency_ms < 50 {
            20
        } else if latency_ms < 100 {
            15
        } else if latency_ms < 200 {
            10
        } else {
            5
        };
        
        NetworkOptimizationHints {
            recommended_update_rate: update_rate,
            interpolation_delay_ms: latency_ms + 50,
            prediction_enabled: latency_ms > 80,
            compression_level: if latency_ms > 150 { 2 } else { 1 },
            batch_updates: latency_ms > 100,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ServerInfo {
    pub name: String,
    pub description: String,
    pub capabilities: ServerCapabilities,
    pub online: bool,
    pub player_count: u32,
    pub max_players: u32,
    pub queue_length: u32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AssetPreloadManifest {
    pub textures: Vec<String>,
    pub models: Vec<String>,
    pub sounds: Vec<String>,
    pub priority_assets: Vec<String>,
    pub total_size_mb: u32,
    pub cache_duration_hours: u32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct NetworkOptimizationHints {
    pub recommended_update_rate: u32,
    pub interpolation_delay_ms: u32,
    pub prediction_enabled: bool,
    pub compression_level: u32,
    pub batch_updates: bool,
}
