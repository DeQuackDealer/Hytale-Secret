use async_trait::async_trait;
use serde::{Deserialize, Serialize};
use std::path::PathBuf;
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ServerAdapterConfig {
    pub server_path: PathBuf,
    pub world_path: PathBuf,
    pub max_players: u32,
    pub port: u16,
    pub motd: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ServerCapabilities {
    pub max_players: u32,
    pub supports_plugins: bool,
    pub supports_mods: bool,
    pub supports_cosmetics: bool,
    pub protocol_version: u32,
    pub game_version: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PlayerInfo {
    pub id: Uuid,
    pub name: String,
    pub address: String,
    pub latency_ms: u32,
    pub joined_at: chrono::DateTime<chrono::Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ServerStatus {
    pub online: bool,
    pub player_count: u32,
    pub max_players: u32,
    pub tps: f64,
    pub memory_used_mb: u64,
    pub uptime_secs: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum ServerAdapterError {
    NotRunning,
    AlreadyRunning,
    PlayerNotFound,
    WorldError(String),
    PluginError(String),
    NetworkError(String),
    ConfigError(String),
    Unknown(String),
}

impl std::fmt::Display for ServerAdapterError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::NotRunning => write!(f, "Server not running"),
            Self::AlreadyRunning => write!(f, "Server already running"),
            Self::PlayerNotFound => write!(f, "Player not found"),
            Self::WorldError(e) => write!(f, "World error: {}", e),
            Self::PluginError(e) => write!(f, "Plugin error: {}", e),
            Self::NetworkError(e) => write!(f, "Network error: {}", e),
            Self::ConfigError(e) => write!(f, "Config error: {}", e),
            Self::Unknown(e) => write!(f, "Unknown error: {}", e),
        }
    }
}

impl std::error::Error for ServerAdapterError {}

#[async_trait]
pub trait ServerAdapter: Send + Sync {
    fn name(&self) -> &str;
    fn capabilities(&self) -> ServerCapabilities;
    
    async fn start(&self) -> Result<(), ServerAdapterError>;
    async fn stop(&self) -> Result<(), ServerAdapterError>;
    async fn status(&self) -> ServerStatus;
    
    async fn get_players(&self) -> Vec<PlayerInfo>;
    async fn kick_player(&self, id: Uuid, reason: &str) -> Result<(), ServerAdapterError>;
    async fn ban_player(&self, id: Uuid, reason: &str, duration: Option<chrono::Duration>) -> Result<(), ServerAdapterError>;
    
    async fn broadcast(&self, message: &str);
    async fn send_message(&self, player_id: Uuid, message: &str) -> Result<(), ServerAdapterError>;
    
    async fn execute_command(&self, command: &str) -> Result<String, ServerAdapterError>;
    
    async fn save_world(&self) -> Result<(), ServerAdapterError>;
    async fn reload_config(&self) -> Result<(), ServerAdapterError>;
}

pub struct HytaleServerAdapter {
    config: ServerAdapterConfig,
    running: std::sync::atomic::AtomicBool,
    start_time: tokio::sync::RwLock<Option<std::time::Instant>>,
}

impl HytaleServerAdapter {
    pub fn new(config: ServerAdapterConfig) -> Self {
        Self {
            config,
            running: std::sync::atomic::AtomicBool::new(false),
            start_time: tokio::sync::RwLock::new(None),
        }
    }
    
    pub fn with_defaults() -> Self {
        Self::new(ServerAdapterConfig {
            server_path: PathBuf::from(""),
            world_path: PathBuf::from(""),
            max_players: 100,
            port: 25565,
            motd: "A Pond Server".to_string(),
        })
    }
}

#[async_trait]
impl ServerAdapter for HytaleServerAdapter {
    fn name(&self) -> &str {
        "Hytale"
    }
    
    fn capabilities(&self) -> ServerCapabilities {
        ServerCapabilities {
            max_players: self.config.max_players,
            supports_plugins: true,
            supports_mods: true,
            supports_cosmetics: true,
            protocol_version: 1,
            game_version: "0.0.0".to_string(),
        }
    }
    
    async fn start(&self) -> Result<(), ServerAdapterError> {
        if self.running.load(std::sync::atomic::Ordering::Relaxed) {
            return Err(ServerAdapterError::AlreadyRunning);
        }
        self.running.store(true, std::sync::atomic::Ordering::Relaxed);
        *self.start_time.write().await = Some(std::time::Instant::now());
        Ok(())
    }
    
    async fn stop(&self) -> Result<(), ServerAdapterError> {
        if !self.running.load(std::sync::atomic::Ordering::Relaxed) {
            return Err(ServerAdapterError::NotRunning);
        }
        self.running.store(false, std::sync::atomic::Ordering::Relaxed);
        *self.start_time.write().await = None;
        Ok(())
    }
    
    async fn status(&self) -> ServerStatus {
        let uptime = self.start_time.read().await
            .map(|t| t.elapsed().as_secs())
            .unwrap_or(0);
        
        ServerStatus {
            online: self.running.load(std::sync::atomic::Ordering::Relaxed),
            player_count: 0,
            max_players: self.config.max_players,
            tps: 20.0,
            memory_used_mb: 0,
            uptime_secs: uptime,
        }
    }
    
    async fn get_players(&self) -> Vec<PlayerInfo> {
        vec![]
    }
    
    async fn kick_player(&self, _id: Uuid, _reason: &str) -> Result<(), ServerAdapterError> {
        Err(ServerAdapterError::PlayerNotFound)
    }
    
    async fn ban_player(&self, _id: Uuid, _reason: &str, _duration: Option<chrono::Duration>) -> Result<(), ServerAdapterError> {
        Err(ServerAdapterError::PlayerNotFound)
    }
    
    async fn broadcast(&self, _message: &str) {}
    
    async fn send_message(&self, _player_id: Uuid, _message: &str) -> Result<(), ServerAdapterError> {
        Err(ServerAdapterError::PlayerNotFound)
    }
    
    async fn execute_command(&self, command: &str) -> Result<String, ServerAdapterError> {
        Ok(format!("Executed: {}", command))
    }
    
    async fn save_world(&self) -> Result<(), ServerAdapterError> {
        Ok(())
    }
    
    async fn reload_config(&self) -> Result<(), ServerAdapterError> {
        Ok(())
    }
}
