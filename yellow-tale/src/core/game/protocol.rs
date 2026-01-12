use async_trait::async_trait;
use serde::{Deserialize, Serialize};
use std::sync::Arc;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum ConnectionState {
    Disconnected,
    Connecting,
    Authenticating,
    Connected,
    Playing,
    Disconnecting,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Packet {
    pub id: u32,
    pub channel: PacketChannel,
    pub data: Vec<u8>,
    pub reliable: bool,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum PacketChannel {
    System,
    Game,
    Chat,
    Voice,
    Custom(u8),
}

#[async_trait]
pub trait PacketHandler: Send + Sync {
    async fn handle_packet(&self, packet: Packet) -> Option<Packet>;
    fn supported_channels(&self) -> Vec<PacketChannel>;
}

#[async_trait]
pub trait GameProtocol: Send + Sync {
    fn version(&self) -> u32;
    fn name(&self) -> &str;
    
    async fn connect(&self, address: &str, port: u16) -> Result<(), ProtocolError>;
    async fn disconnect(&self) -> Result<(), ProtocolError>;
    fn state(&self) -> ConnectionState;
    
    async fn send(&self, packet: Packet) -> Result<(), ProtocolError>;
    async fn receive(&self) -> Result<Option<Packet>, ProtocolError>;
    
    fn register_handler(&self, handler: Arc<dyn PacketHandler>);
    fn unregister_handler(&self, channel: PacketChannel);
    
    async fn ping(&self) -> Result<u32, ProtocolError>;
    fn latency(&self) -> u32;
    fn packet_loss(&self) -> f32;
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum ProtocolError {
    NotConnected,
    AlreadyConnected,
    ConnectionFailed(String),
    Timeout,
    InvalidPacket,
    ChannelClosed,
    AuthenticationFailed,
    VersionMismatch { expected: u32, got: u32 },
    Unknown(String),
}

impl std::fmt::Display for ProtocolError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::NotConnected => write!(f, "Not connected"),
            Self::AlreadyConnected => write!(f, "Already connected"),
            Self::ConnectionFailed(e) => write!(f, "Connection failed: {}", e),
            Self::Timeout => write!(f, "Timeout"),
            Self::InvalidPacket => write!(f, "Invalid packet"),
            Self::ChannelClosed => write!(f, "Channel closed"),
            Self::AuthenticationFailed => write!(f, "Authentication failed"),
            Self::VersionMismatch { expected, got } => {
                write!(f, "Version mismatch: expected {}, got {}", expected, got)
            }
            Self::Unknown(e) => write!(f, "Unknown error: {}", e),
        }
    }
}

impl std::error::Error for ProtocolError {}

pub struct HytaleProtocol {
    state: std::sync::atomic::AtomicU8,
    latency: std::sync::atomic::AtomicU32,
}

impl HytaleProtocol {
    pub fn new() -> Self {
        Self {
            state: std::sync::atomic::AtomicU8::new(ConnectionState::Disconnected as u8),
            latency: std::sync::atomic::AtomicU32::new(0),
        }
    }
    
    fn get_state(&self) -> ConnectionState {
        match self.state.load(std::sync::atomic::Ordering::Relaxed) {
            0 => ConnectionState::Disconnected,
            1 => ConnectionState::Connecting,
            2 => ConnectionState::Authenticating,
            3 => ConnectionState::Connected,
            4 => ConnectionState::Playing,
            5 => ConnectionState::Disconnecting,
            _ => ConnectionState::Disconnected,
        }
    }
}

impl Default for HytaleProtocol {
    fn default() -> Self {
        Self::new()
    }
}

#[async_trait]
impl GameProtocol for HytaleProtocol {
    fn version(&self) -> u32 {
        1
    }
    
    fn name(&self) -> &str {
        "Hytale Protocol"
    }
    
    async fn connect(&self, _address: &str, _port: u16) -> Result<(), ProtocolError> {
        self.state.store(ConnectionState::Connecting as u8, std::sync::atomic::Ordering::Relaxed);
        Ok(())
    }
    
    async fn disconnect(&self) -> Result<(), ProtocolError> {
        self.state.store(ConnectionState::Disconnected as u8, std::sync::atomic::Ordering::Relaxed);
        Ok(())
    }
    
    fn state(&self) -> ConnectionState {
        self.get_state()
    }
    
    async fn send(&self, _packet: Packet) -> Result<(), ProtocolError> {
        if self.get_state() == ConnectionState::Disconnected {
            return Err(ProtocolError::NotConnected);
        }
        Ok(())
    }
    
    async fn receive(&self) -> Result<Option<Packet>, ProtocolError> {
        Ok(None)
    }
    
    fn register_handler(&self, _handler: Arc<dyn PacketHandler>) {}
    fn unregister_handler(&self, _channel: PacketChannel) {}
    
    async fn ping(&self) -> Result<u32, ProtocolError> {
        Ok(self.latency.load(std::sync::atomic::Ordering::Relaxed))
    }
    
    fn latency(&self) -> u32 {
        self.latency.load(std::sync::atomic::Ordering::Relaxed)
    }
    
    fn packet_loss(&self) -> f32 {
        0.0
    }
}
