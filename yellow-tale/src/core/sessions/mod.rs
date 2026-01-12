//! Session Orchestration Module
//! 
//! Provides connection orchestration (NOT a VPN):
//! - Session broker client
//! - Invite code generation
//! - Session lifecycle tracking
//! - Abstracted P2P attempt layer
//! - Relay interface (stub for future infrastructure)
//! 
//! This is connection orchestration, NOT tunneling.

use std::collections::HashMap;
use std::time::Duration;
use serde::{Deserialize, Serialize};
use thiserror::Error;
use uuid::Uuid;
use chrono::{DateTime, Utc};
use tracing::info;

#[derive(Error, Debug)]
pub enum SessionError {
    #[error("Session not found: {0}")]
    NotFound(String),
    
    #[error("Invalid invite code: {0}")]
    InvalidInviteCode(String),
    
    #[error("Connection failed: {0}")]
    ConnectionFailed(String),
    
    #[error("Session full: {0}")]
    SessionFull(String),
    
    #[error("Already in session")]
    AlreadyInSession,
    
    #[error("Not in session")]
    NotInSession,
    
    #[error("P2P connection failed: {0}")]
    P2PFailed(String),
    
    #[error("Relay not available")]
    RelayUnavailable,
}

/// Connection method for session
#[derive(Debug, Clone, Copy, Serialize, Deserialize, PartialEq)]
pub enum ConnectionMethod {
    /// Direct peer-to-peer connection
    P2P,
    /// Connection through relay server
    Relay,
    /// Hybrid - P2P with relay fallback
    Hybrid,
}

impl Default for ConnectionMethod {
    fn default() -> Self {
        Self::Hybrid
    }
}

/// State of a P2P connection attempt
#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum P2PState {
    /// Not attempted
    Idle,
    /// Gathering network information
    Gathering,
    /// Attempting direct connection
    Connecting,
    /// Successfully connected
    Connected { remote_addr: String },
    /// Failed - needs relay
    Failed { reason: String },
}

/// Relay connection state
#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum RelayState {
    /// Not connected to relay
    Disconnected,
    /// Connecting to relay server
    Connecting,
    /// Connected to relay
    Connected { relay_addr: String },
    /// Relaying session traffic
    Relaying { session_id: String },
}

/// A participant in a session
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Participant {
    /// Unique participant ID
    pub id: Uuid,
    
    /// Display name
    pub name: String,
    
    /// Connection method being used
    pub connection: ConnectionMethod,
    
    /// P2P state if applicable
    pub p2p_state: P2PState,
    
    /// When they joined
    pub joined_at: DateTime<Utc>,
    
    /// Latency in ms (if known)
    pub latency_ms: Option<u32>,
}

/// A multiplayer session
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Session {
    /// Session ID
    pub id: Uuid,
    
    /// Invite code for joining
    pub invite_code: String,
    
    /// Session host
    pub host: Participant,
    
    /// Other participants
    pub participants: Vec<Participant>,
    
    /// Maximum participants allowed
    pub max_participants: usize,
    
    /// Session state
    pub state: SessionState,
    
    /// When session was created
    pub created_at: DateTime<Utc>,
    
    /// Session metadata
    pub metadata: HashMap<String, String>,
}

/// State of a session
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub enum SessionState {
    /// Session is being set up
    Creating,
    /// Session is open for participants
    Open,
    /// Session is in progress (game running)
    InProgress,
    /// Session is closed
    Closed,
}

/// Configuration for the session orchestrator
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SessionConfig {
    /// Preferred connection method
    pub preferred_method: ConnectionMethod,
    
    /// Maximum relay hops allowed
    pub max_relay_hops: u8,
    
    /// P2P connection timeout
    pub p2p_timeout: Duration,
    
    /// Relay server addresses (stubs)
    pub relay_servers: Vec<String>,
    
    /// Whether to auto-accept invites
    pub auto_accept: bool,
}

impl Default for SessionConfig {
    fn default() -> Self {
        Self {
            preferred_method: ConnectionMethod::Hybrid,
            max_relay_hops: 3,
            p2p_timeout: Duration::from_secs(10),
            relay_servers: Vec::new(),
            auto_accept: false,
        }
    }
}

/// Orchestrates session creation, joining, and connection management
pub struct SessionOrchestrator {
    /// Configuration
    config: SessionConfig,
    
    /// Current session (if in one)
    current_session: Option<Session>,
    
    /// Local participant info
    local_participant: Option<Participant>,
    
    /// P2P connection state
    p2p_state: P2PState,
    
    /// Relay connection state
    relay_state: RelayState,
    
    /// Known sessions (from broker) - will be populated by broker queries
    #[allow(dead_code)] // Reserved for future broker integration
    known_sessions: HashMap<String, Session>,
}

impl SessionOrchestrator {
    /// Create a new session orchestrator
    pub fn new() -> Self {
        Self {
            config: SessionConfig::default(),
            current_session: None,
            local_participant: None,
            p2p_state: P2PState::Idle,
            relay_state: RelayState::Disconnected,
            known_sessions: HashMap::new(),
        }
    }
    
    /// Create a new session orchestrator with config
    pub fn with_config(config: SessionConfig) -> Self {
        Self {
            config,
            ..Self::new()
        }
    }
    
    /// Generate a unique invite code
    fn generate_invite_code() -> String {
        // Generate a human-readable invite code
        // Format: XXXX-XXXX-XXXX
        use rand::Rng;
        let mut rng = rand::thread_rng();
        let chars: Vec<char> = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".chars().collect();
        
        let mut code = String::with_capacity(14);
        for i in 0..12 {
            if i > 0 && i % 4 == 0 {
                code.push('-');
            }
            code.push(chars[rng.gen_range(0..chars.len())]);
        }
        code
    }
    
    /// Create a new session as host
    pub async fn create_session(&mut self, name: String, max_participants: usize) -> Result<Session, SessionError> {
        if self.current_session.is_some() {
            return Err(SessionError::AlreadyInSession);
        }
        
        let host = Participant {
            id: Uuid::new_v4(),
            name: name.clone(),
            connection: ConnectionMethod::P2P, // Host is local
            p2p_state: P2PState::Connected { remote_addr: "local".to_string() },
            joined_at: Utc::now(),
            latency_ms: Some(0),
        };
        
        let session = Session {
            id: Uuid::new_v4(),
            invite_code: Self::generate_invite_code(),
            host: host.clone(),
            participants: Vec::new(),
            max_participants,
            state: SessionState::Open,
            created_at: Utc::now(),
            metadata: HashMap::new(),
        };
        
        info!("Created session {} with invite code {}", session.id, session.invite_code);
        
        self.local_participant = Some(host);
        self.current_session = Some(session.clone());
        
        Ok(session)
    }
    
    /// Join a session using an invite code
    pub async fn join_session(&mut self, invite_code: &str, name: String) -> Result<Session, SessionError> {
        if self.current_session.is_some() {
            return Err(SessionError::AlreadyInSession);
        }
        
        // In a real implementation, this would:
        // 1. Contact the session broker with the invite code
        // 2. Get session details and connection info
        // 3. Attempt P2P connection
        // 4. Fall back to relay if needed
        
        // For now, we'll create a stub session
        info!("Attempting to join session with code: {}", invite_code);
        
        // Simulate broker lookup
        if invite_code.len() != 14 {
            return Err(SessionError::InvalidInviteCode(invite_code.to_string()));
        }
        
        let participant = Participant {
            id: Uuid::new_v4(),
            name,
            connection: self.config.preferred_method,
            p2p_state: P2PState::Idle,
            joined_at: Utc::now(),
            latency_ms: None,
        };
        
        self.local_participant = Some(participant);
        
        // Attempt connection
        self.attempt_p2p_connection().await?;
        
        // If P2P fails and we allow relay, try that
        if matches!(self.p2p_state, P2PState::Failed { .. }) {
            if self.config.preferred_method != ConnectionMethod::P2P {
                self.connect_relay().await?;
            }
        }
        
        // This is a stub - real implementation would get session from broker
        Err(SessionError::ConnectionFailed("Session broker not implemented".to_string()))
    }
    
    /// Attempt a P2P connection
    async fn attempt_p2p_connection(&mut self) -> Result<(), SessionError> {
        info!("Attempting P2P connection...");
        
        self.p2p_state = P2PState::Gathering;
        
        // In a real implementation:
        // 1. Gather local network info (NAT type, local/public IPs)
        // 2. Exchange connection info via signaling server
        // 3. Attempt hole punching
        // 4. Establish direct connection
        
        // Simulate the attempt
        self.p2p_state = P2PState::Connecting;
        
        // For now, always "succeed" in stub mode
        self.p2p_state = P2PState::Connected {
            remote_addr: "stub:0".to_string(),
        };
        
        info!("P2P connection established (stub)");
        Ok(())
    }
    
    /// Connect to a relay server
    async fn connect_relay(&mut self) -> Result<(), SessionError> {
        if self.config.relay_servers.is_empty() {
            return Err(SessionError::RelayUnavailable);
        }
        
        info!("Connecting to relay server...");
        
        self.relay_state = RelayState::Connecting;
        
        // In a real implementation:
        // 1. Connect to relay server via WebSocket or QUIC
        // 2. Authenticate and register
        // 3. Join session room on relay
        
        let relay_addr = self.config.relay_servers.first().cloned().unwrap();
        self.relay_state = RelayState::Connected { relay_addr };
        
        info!("Connected to relay (stub)");
        Ok(())
    }
    
    /// Leave the current session
    pub async fn leave_session(&mut self) -> Result<(), SessionError> {
        if self.current_session.is_none() {
            return Err(SessionError::NotInSession);
        }
        
        info!("Leaving session...");
        
        // Clean up connections
        self.p2p_state = P2PState::Idle;
        self.relay_state = RelayState::Disconnected;
        self.current_session = None;
        self.local_participant = None;
        
        Ok(())
    }
    
    /// Get the current session
    pub fn current_session(&self) -> Option<&Session> {
        self.current_session.as_ref()
    }
    
    /// Get invite code for current session
    pub fn get_invite_code(&self) -> Option<&str> {
        self.current_session.as_ref().map(|s| s.invite_code.as_str())
    }
    
    /// Get connection state
    pub fn connection_state(&self) -> (P2PState, RelayState) {
        (self.p2p_state.clone(), self.relay_state.clone())
    }
    
    /// Update configuration
    pub fn set_config(&mut self, config: SessionConfig) {
        self.config = config;
    }
    
    /// Get current configuration
    pub fn config(&self) -> &SessionConfig {
        &self.config
    }
}

impl Default for SessionOrchestrator {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    
    #[tokio::test]
    async fn test_create_session() {
        let mut orchestrator = SessionOrchestrator::new();
        let session = orchestrator.create_session("TestHost".to_string(), 8).await.unwrap();
        
        assert_eq!(session.host.name, "TestHost");
        assert_eq!(session.max_participants, 8);
        assert_eq!(session.invite_code.len(), 14);
    }
    
    #[test]
    fn test_invite_code_format() {
        let code = SessionOrchestrator::generate_invite_code();
        assert_eq!(code.len(), 14);
        assert_eq!(code.chars().filter(|c| *c == '-').count(), 2);
    }
}
