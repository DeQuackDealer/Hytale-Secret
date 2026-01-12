use dashmap::DashMap;
use serde::{Deserialize, Serialize};
use std::sync::atomic::{AtomicU64, Ordering};
use tokio::sync::RwLock;
use uuid::Uuid;
use chrono::{DateTime, Utc};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PeerInfo {
    pub user_id: Uuid,
    pub session_token: String,
    pub public_ip: String,
    pub public_port: u16,
    pub private_ip: Option<String>,
    pub private_port: Option<u16>,
    pub nat_type: NatType,
    pub connected_at: DateTime<Utc>,
    pub last_heartbeat: DateTime<Utc>,
    pub latency_ms: u32,
    pub premium: bool,
}

#[derive(Debug, Clone, Copy, Serialize, Deserialize, PartialEq)]
pub enum NatType {
    Open,
    FullCone,
    RestrictedCone,
    PortRestricted,
    Symmetric,
    Unknown,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RelaySession {
    pub id: String,
    pub host_id: Uuid,
    pub created_at: DateTime<Utc>,
    pub peers: Vec<Uuid>,
    pub max_peers: u32,
    pub password_hash: Option<String>,
    pub region: String,
    pub relay_mode: RelayMode,
}

#[derive(Debug, Clone, Copy, Serialize, Deserialize, PartialEq)]
pub enum RelayMode {
    Direct,
    Relay,
    Hybrid,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ConnectionAttempt {
    pub from_peer: Uuid,
    pub to_peer: Uuid,
    pub method: ConnectionMethod,
    pub started_at: DateTime<Utc>,
    pub completed_at: Option<DateTime<Utc>>,
    pub success: bool,
    pub latency_ms: Option<u32>,
}

#[derive(Debug, Clone, Copy, Serialize, Deserialize)]
pub enum ConnectionMethod {
    DirectUdp,
    UdpHolePunch,
    TcpRelay,
    QuicRelay,
    WebRtc,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct IceCandidate {
    pub candidate_type: IceCandidateType,
    pub ip: String,
    pub port: u16,
    pub priority: u32,
    pub foundation: String,
}

#[derive(Debug, Clone, Copy, Serialize, Deserialize)]
pub enum IceCandidateType {
    Host,
    ServerReflexive,
    PeerReflexive,
    Relay,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct StunResponse {
    pub public_ip: String,
    pub public_port: u16,
    pub nat_type: NatType,
    pub latency_ms: u32,
}

pub struct RelayHub {
    sessions: DashMap<String, RelaySession>,
    peers: DashMap<Uuid, PeerInfo>,
    ice_candidates: DashMap<Uuid, Vec<IceCandidate>>,
    connection_attempts: RwLock<Vec<ConnectionAttempt>>,
    stun_servers: Vec<String>,
    turn_servers: Vec<TurnServer>,
    stats: RelayStats,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TurnServer {
    pub url: String,
    pub username: String,
    pub credential: String,
    pub region: String,
}

pub struct RelayStats {
    pub total_sessions: AtomicU64,
    pub active_peers: AtomicU64,
    pub successful_connections: AtomicU64,
    pub failed_connections: AtomicU64,
    pub bytes_relayed: AtomicU64,
}

impl RelayHub {
    pub fn new() -> Self {
        Self {
            sessions: DashMap::new(),
            peers: DashMap::new(),
            ice_candidates: DashMap::new(),
            connection_attempts: RwLock::new(Vec::new()),
            stun_servers: vec![
                "stun:stun.l.google.com:19302".to_string(),
                "stun:stun1.l.google.com:19302".to_string(),
                "stun:stun2.l.google.com:19302".to_string(),
            ],
            turn_servers: vec![],
            stats: RelayStats {
                total_sessions: AtomicU64::new(0),
                active_peers: AtomicU64::new(0),
                successful_connections: AtomicU64::new(0),
                failed_connections: AtomicU64::new(0),
                bytes_relayed: AtomicU64::new(0),
            },
        }
    }

    pub fn register_peer(&self, info: PeerInfo) -> Result<(), RelayError> {
        let user_id = info.user_id;
        self.peers.insert(user_id, info);
        self.stats.active_peers.fetch_add(1, Ordering::Relaxed);
        Ok(())
    }

    pub fn unregister_peer(&self, user_id: Uuid) {
        if self.peers.remove(&user_id).is_some() {
            self.stats.active_peers.fetch_sub(1, Ordering::Relaxed);
        }
        self.ice_candidates.remove(&user_id);
        
        for mut session in self.sessions.iter_mut() {
            session.peers.retain(|&id| id != user_id);
        }
    }

    pub fn update_heartbeat(&self, user_id: Uuid, latency_ms: u32) {
        if let Some(mut peer) = self.peers.get_mut(&user_id) {
            peer.last_heartbeat = Utc::now();
            peer.latency_ms = latency_ms;
        }
    }

    pub fn create_session(&self, host_id: Uuid, max_peers: u32, password: Option<String>) -> Result<String, RelayError> {
        let session_id = Uuid::new_v4().to_string();
        
        let session = RelaySession {
            id: session_id.clone(),
            host_id,
            created_at: Utc::now(),
            peers: vec![host_id],
            max_peers,
            password_hash: password.map(|p| hex::encode(sha256(&p))),
            region: "auto".to_string(),
            relay_mode: RelayMode::Hybrid,
        };
        
        self.sessions.insert(session_id.clone(), session);
        self.stats.total_sessions.fetch_add(1, Ordering::Relaxed);
        
        Ok(session_id)
    }

    pub fn join_session(&self, session_id: &str, user_id: Uuid, password: Option<String>) -> Result<RelaySession, RelayError> {
        let mut session = self.sessions.get_mut(session_id)
            .ok_or(RelayError::SessionNotFound)?;
        
        if session.peers.len() >= session.max_peers as usize {
            return Err(RelayError::SessionFull);
        }
        
        if let Some(ref hash) = session.password_hash {
            let provided_hash = password.map(|p| hex::encode(sha256(&p)));
            if provided_hash.as_ref() != Some(hash) {
                return Err(RelayError::InvalidPassword);
            }
        }
        
        if !session.peers.contains(&user_id) {
            session.peers.push(user_id);
        }
        
        Ok(session.clone())
    }

    pub fn leave_session(&self, session_id: &str, user_id: Uuid) {
        if let Some(mut session) = self.sessions.get_mut(session_id) {
            session.peers.retain(|&id| id != user_id);
            
            if session.peers.is_empty() || session.host_id == user_id {
                drop(session);
                self.sessions.remove(session_id);
            }
        }
    }

    pub fn add_ice_candidate(&self, user_id: Uuid, candidate: IceCandidate) {
        self.ice_candidates
            .entry(user_id)
            .or_insert_with(Vec::new)
            .push(candidate);
    }

    pub fn get_ice_candidates(&self, user_id: Uuid) -> Vec<IceCandidate> {
        self.ice_candidates.get(&user_id)
            .map(|c| c.clone())
            .unwrap_or_default()
    }

    pub fn get_peer_info(&self, user_id: Uuid) -> Option<PeerInfo> {
        self.peers.get(&user_id).map(|p| p.clone())
    }

    pub fn get_session(&self, session_id: &str) -> Option<RelaySession> {
        self.sessions.get(session_id).map(|s| s.clone())
    }

    pub fn get_session_peers(&self, session_id: &str) -> Vec<PeerInfo> {
        self.sessions.get(session_id)
            .map(|session| {
                session.peers.iter()
                    .filter_map(|&id| self.peers.get(&id).map(|p| p.clone()))
                    .collect()
            })
            .unwrap_or_default()
    }

    pub async fn record_connection_attempt(&self, attempt: ConnectionAttempt) {
        let success = attempt.success;
        let mut attempts = self.connection_attempts.write().await;
        attempts.push(attempt);
        
        if attempts.len() > 10000 {
            attempts.drain(0..1000);
        }
        
        if success {
            self.stats.successful_connections.fetch_add(1, Ordering::Relaxed);
        } else {
            self.stats.failed_connections.fetch_add(1, Ordering::Relaxed);
        }
    }

    pub fn get_stun_servers(&self) -> &[String] {
        &self.stun_servers
    }

    pub fn get_turn_servers(&self, premium: bool) -> Vec<TurnServer> {
        if premium {
            self.turn_servers.clone()
        } else {
            self.turn_servers.iter().take(1).cloned().collect()
        }
    }

    pub fn determine_best_connection_method(&self, from: Uuid, to: Uuid) -> ConnectionMethod {
        let from_peer = self.peers.get(&from);
        let to_peer = self.peers.get(&to);
        
        match (from_peer, to_peer) {
            (Some(f), Some(t)) => {
                match (f.nat_type, t.nat_type) {
                    (NatType::Open, _) | (_, NatType::Open) => ConnectionMethod::DirectUdp,
                    (NatType::FullCone, _) | (_, NatType::FullCone) => ConnectionMethod::UdpHolePunch,
                    (NatType::RestrictedCone, NatType::RestrictedCone) => ConnectionMethod::UdpHolePunch,
                    (NatType::PortRestricted, NatType::PortRestricted) => ConnectionMethod::UdpHolePunch,
                    (NatType::Symmetric, _) | (_, NatType::Symmetric) => ConnectionMethod::QuicRelay,
                    _ => ConnectionMethod::TcpRelay,
                }
            },
            _ => ConnectionMethod::TcpRelay,
        }
    }

    pub fn get_stats(&self) -> RelayStatsSnapshot {
        RelayStatsSnapshot {
            total_sessions: self.stats.total_sessions.load(Ordering::Relaxed),
            active_peers: self.stats.active_peers.load(Ordering::Relaxed),
            successful_connections: self.stats.successful_connections.load(Ordering::Relaxed),
            failed_connections: self.stats.failed_connections.load(Ordering::Relaxed),
            bytes_relayed: self.stats.bytes_relayed.load(Ordering::Relaxed),
            active_sessions: self.sessions.len() as u64,
        }
    }

    pub fn cleanup_stale_peers(&self, timeout_secs: i64) {
        let cutoff = Utc::now() - chrono::Duration::seconds(timeout_secs);
        
        let stale: Vec<Uuid> = self.peers.iter()
            .filter(|p| p.last_heartbeat < cutoff)
            .map(|p| p.user_id)
            .collect();
        
        for user_id in stale {
            self.unregister_peer(user_id);
        }
    }
}

impl Default for RelayHub {
    fn default() -> Self {
        Self::new()
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RelayStatsSnapshot {
    pub total_sessions: u64,
    pub active_peers: u64,
    pub successful_connections: u64,
    pub failed_connections: u64,
    pub bytes_relayed: u64,
    pub active_sessions: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum RelayError {
    SessionNotFound,
    SessionFull,
    InvalidPassword,
    PeerNotFound,
    ConnectionFailed(String),
    Unauthorized,
}

impl std::fmt::Display for RelayError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::SessionNotFound => write!(f, "Session not found"),
            Self::SessionFull => write!(f, "Session is full"),
            Self::InvalidPassword => write!(f, "Invalid password"),
            Self::PeerNotFound => write!(f, "Peer not found"),
            Self::ConnectionFailed(e) => write!(f, "Connection failed: {}", e),
            Self::Unauthorized => write!(f, "Unauthorized"),
        }
    }
}

impl std::error::Error for RelayError {}

fn sha256(input: &str) -> [u8; 32] {
    use sha2::{Sha256, Digest};
    let mut hasher = Sha256::new();
    hasher.update(input.as_bytes());
    hasher.finalize().into()
}
