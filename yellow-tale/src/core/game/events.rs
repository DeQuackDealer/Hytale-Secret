use async_trait::async_trait;
use dashmap::DashMap;
use serde::{Deserialize, Serialize};
use std::sync::Arc;
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum GameEvent {
    GameStarted { version: String },
    GameStopped { exit_code: i32 },
    ServerConnected { address: String, port: u16 },
    ServerDisconnected { reason: String },
    PlayerJoined { player_id: Uuid, name: String },
    PlayerLeft { player_id: Uuid },
    ChatMessage { sender: String, message: String },
    ModLoaded { mod_id: String, version: String },
    ModUnloaded { mod_id: String },
    ProfileChanged { profile_id: String },
    AssetDownloadProgress { current: u64, total: u64 },
    PerformanceWarning { metric: String, value: f64 },
    Error { code: String, message: String },
    Custom { event_type: String, data: serde_json::Value },
}

impl GameEvent {
    pub fn event_type(&self) -> &str {
        match self {
            Self::GameStarted { .. } => "game_started",
            Self::GameStopped { .. } => "game_stopped",
            Self::ServerConnected { .. } => "server_connected",
            Self::ServerDisconnected { .. } => "server_disconnected",
            Self::PlayerJoined { .. } => "player_joined",
            Self::PlayerLeft { .. } => "player_left",
            Self::ChatMessage { .. } => "chat_message",
            Self::ModLoaded { .. } => "mod_loaded",
            Self::ModUnloaded { .. } => "mod_unloaded",
            Self::ProfileChanged { .. } => "profile_changed",
            Self::AssetDownloadProgress { .. } => "asset_download_progress",
            Self::PerformanceWarning { .. } => "performance_warning",
            Self::Error { .. } => "error",
            Self::Custom { event_type, .. } => event_type,
        }
    }
}

#[async_trait]
pub trait EventHandler: Send + Sync {
    fn handles(&self, event_type: &str) -> bool;
    async fn handle(&self, event: &GameEvent);
}

type HandlerId = u64;

pub struct EventBus {
    handlers: DashMap<HandlerId, Arc<dyn EventHandler>>,
    next_id: std::sync::atomic::AtomicU64,
    history: tokio::sync::RwLock<Vec<GameEvent>>,
    history_limit: usize,
}

impl EventBus {
    pub fn new() -> Self {
        Self {
            handlers: DashMap::new(),
            next_id: std::sync::atomic::AtomicU64::new(1),
            history: tokio::sync::RwLock::new(Vec::new()),
            history_limit: 1000,
        }
    }
    
    pub fn with_history_limit(limit: usize) -> Self {
        Self {
            handlers: DashMap::new(),
            next_id: std::sync::atomic::AtomicU64::new(1),
            history: tokio::sync::RwLock::new(Vec::new()),
            history_limit: limit,
        }
    }
    
    pub fn subscribe(&self, handler: Arc<dyn EventHandler>) -> HandlerId {
        let id = self.next_id.fetch_add(1, std::sync::atomic::Ordering::Relaxed);
        self.handlers.insert(id, handler);
        id
    }
    
    pub fn unsubscribe(&self, id: HandlerId) {
        self.handlers.remove(&id);
    }
    
    pub async fn emit(&self, event: GameEvent) {
        {
            let mut history = self.history.write().await;
            history.push(event.clone());
            if history.len() > self.history_limit {
                history.drain(0..100);
            }
        }
        
        let event_type = event.event_type();
        for entry in self.handlers.iter() {
            let handler = entry.value();
            if handler.handles(event_type) {
                handler.handle(&event).await;
            }
        }
    }
    
    pub async fn history(&self, event_type: Option<&str>, limit: usize) -> Vec<GameEvent> {
        let history = self.history.read().await;
        let iter = history.iter().rev();
        
        match event_type {
            Some(t) => iter
                .filter(|e| e.event_type() == t)
                .take(limit)
                .cloned()
                .collect(),
            None => iter.take(limit).cloned().collect(),
        }
    }
    
    pub async fn clear_history(&self) {
        let mut history = self.history.write().await;
        history.clear();
    }
}

impl Default for EventBus {
    fn default() -> Self {
        Self::new()
    }
}

pub struct LoggingEventHandler {
    log_level: tracing::Level,
}

impl LoggingEventHandler {
    pub fn new(log_level: tracing::Level) -> Self {
        Self { log_level }
    }
}

#[async_trait]
impl EventHandler for LoggingEventHandler {
    fn handles(&self, _event_type: &str) -> bool {
        true
    }
    
    async fn handle(&self, event: &GameEvent) {
        match self.log_level {
            tracing::Level::ERROR => tracing::error!(?event, "game event"),
            tracing::Level::WARN => tracing::warn!(?event, "game event"),
            tracing::Level::INFO => tracing::info!(?event, "game event"),
            tracing::Level::DEBUG => tracing::debug!(?event, "game event"),
            tracing::Level::TRACE => tracing::trace!(?event, "game event"),
        }
    }
}
