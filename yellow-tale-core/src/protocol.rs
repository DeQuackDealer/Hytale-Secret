use serde::{Deserialize, Serialize};
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "type", content = "payload")]
pub enum ControlMessage {
    Ping,
    StartServer { profile_id: Uuid, server_id: Option<String> },
    StopServer { server_id: String, graceful: bool },
    RestartServer { server_id: String },
    QueryStatus { server_id: Option<String> },
    PushConfig { server_id: String, config: serde_json::Value },
    PushAssets { server_id: String, assets: Vec<AssetPush> },
    Subscribe { events: Vec<EventType> },
    Unsubscribe { events: Vec<EventType> },
    GetMetrics { server_id: String },
    GetCapabilities,
    ExecuteCommand { server_id: String, command: String },
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "type", content = "payload")]
pub enum ControlResponse {
    Pong { timestamp: i64 },
    Success { message: String },
    Error { code: ErrorCode, message: String },
    ServerStatus(ServerStatus),
    Metrics(ServerMetrics),
    Capabilities(Capabilities),
    Event(ServerEvent),
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AssetPush {
    pub id: String,
    pub path: String,
    pub hash: String,
    pub data: Option<Vec<u8>>,
    pub url: Option<String>,
}

#[derive(Debug, Clone, Copy, Serialize, Deserialize, PartialEq, Eq, Hash)]
pub enum EventType {
    ServerLifecycle,
    PlayerJoin,
    PlayerLeave,
    PluginEvent,
    PerformanceAlert,
    Error,
    Custom,
}

#[derive(Debug, Clone, Copy, Serialize, Deserialize)]
pub enum ErrorCode {
    InvalidRequest,
    ServerNotFound,
    ServerAlreadyRunning,
    ServerNotRunning,
    PermissionDenied,
    InternalError,
    Timeout,
    QuotaExceeded,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ServerStatus {
    pub id: String,
    pub name: String,
    pub state: ServerState,
    pub uptime_seconds: u64,
    pub player_count: u32,
    pub max_players: u32,
    pub tps: f32,
    pub memory_used_mb: u64,
    pub cpu_percent: f32,
}

#[derive(Debug, Clone, Copy, Serialize, Deserialize, PartialEq)]
pub enum ServerState {
    Stopped,
    Starting,
    Running,
    Stopping,
    Crashed,
    Restarting,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ServerMetrics {
    pub server_id: String,
    pub timestamp: i64,
    pub tps: f32,
    pub tps_history: Vec<f32>,
    pub memory_used_mb: u64,
    pub memory_max_mb: u64,
    pub cpu_percent: f32,
    pub cpu_history: Vec<f32>,
    pub player_count: u32,
    pub entities: u32,
    pub chunks_loaded: u32,
    pub tick_time_ms: f32,
    pub network_in_kbps: f32,
    pub network_out_kbps: f32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Capabilities {
    pub version: String,
    pub pond_version: Option<String>,
    pub features: Vec<String>,
}

impl Default for Capabilities {
    fn default() -> Self {
        Self {
            version: env!("CARGO_PKG_VERSION").to_string(),
            pond_version: None,
            features: vec![
                "pond.lifecycle".to_string(),
                "pond.metrics".to_string(),
                "pond.config".to_string(),
                "pond.assets".to_string(),
            ],
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ServerEvent {
    pub event_type: EventType,
    pub server_id: String,
    pub timestamp: i64,
    pub data: serde_json::Value,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct HandshakeRequest {
    pub client_version: String,
    pub client_type: ClientType,
    pub auth_token: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct HandshakeResponse {
    pub accepted: bool,
    pub server_version: String,
    pub capabilities: Capabilities,
    pub session_id: Option<String>,
    pub error: Option<String>,
}

#[derive(Debug, Clone, Copy, Serialize, Deserialize)]
pub enum ClientType {
    Launcher,
    Web,
    Cli,
    RemoteAdmin,
}

pub mod ipc {
    use super::*;
    use std::io::{BufRead, BufReader, Write};
    use std::net::TcpStream;
    
    pub struct IpcClient {
        stream: TcpStream,
    }
    
    impl IpcClient {
        pub fn connect(addr: &str) -> std::io::Result<Self> {
            let stream = TcpStream::connect(addr)?;
            Ok(Self { stream })
        }
        
        pub fn send(&mut self, msg: &ControlMessage) -> std::io::Result<()> {
            let json = serde_json::to_string(msg)?;
            writeln!(self.stream, "{}", json)?;
            self.stream.flush()
        }
        
        pub fn receive(&mut self) -> std::io::Result<ControlResponse> {
            let mut reader = BufReader::new(&self.stream);
            let mut line = String::new();
            reader.read_line(&mut line)?;
            serde_json::from_str(&line).map_err(|e| std::io::Error::new(std::io::ErrorKind::InvalidData, e))
        }
        
        pub fn request(&mut self, msg: &ControlMessage) -> std::io::Result<ControlResponse> {
            self.send(msg)?;
            self.receive()
        }
    }
}
