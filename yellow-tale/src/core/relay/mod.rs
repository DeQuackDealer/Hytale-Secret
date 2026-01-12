use std::collections::HashMap;
use std::net::SocketAddr;
use std::sync::Arc;
use chrono::{DateTime, Utc};
use futures_util::{SinkExt, StreamExt};
use serde::{Deserialize, Serialize};
use thiserror::Error;
use tokio::net::{TcpListener, TcpStream};
use tokio::sync::{broadcast, mpsc, RwLock};
use tokio_tungstenite::{accept_async, tungstenite::Message};
use tracing::{info, warn, error};
use uuid::Uuid;

#[derive(Error, Debug)]
pub enum RelayError {
    #[error("Relay server not running")]
    NotRunning,
    
    #[error("Failed to bind: {0}")]
    BindFailed(String),
    
    #[error("Session not found")]
    SessionNotFound,
    
    #[error("Connection failed: {0}")]
    ConnectionFailed(String),
    
    #[error("Peer not found")]
    PeerNotFound,
    
    #[error("Session full")]
    SessionFull,
    
    #[error("Invalid message")]
    InvalidMessage,
    
    #[error("IO error: {0}")]
    Io(#[from] std::io::Error),
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "type", rename_all = "snake_case")]
pub enum RelayMessage {
    Join {
        session_id: String,
        user_id: Uuid,
        username: String,
    },
    Leave {
        session_id: String,
        user_id: Uuid,
    },
    Data {
        from: Uuid,
        to: Option<Uuid>,
        payload: Vec<u8>,
    },
    PeerList {
        peers: Vec<PeerInfo>,
    },
    PeerJoined {
        peer: PeerInfo,
    },
    PeerLeft {
        user_id: Uuid,
    },
    Ping,
    Pong,
    Error {
        message: String,
    },
    Ack {
        message_id: String,
    },
    HostMigration {
        new_host: Uuid,
    },
    SessionClosed {
        reason: String,
    },
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PeerInfo {
    pub user_id: Uuid,
    pub username: String,
    pub is_host: bool,
    pub joined_at: DateTime<Utc>,
    pub latency_ms: Option<u32>,
}

#[derive(Debug, Clone)]
struct ConnectedPeer {
    user_id: Uuid,
    username: String,
    #[allow(dead_code)]
    session_id: String,
    sender: mpsc::UnboundedSender<Message>,
    joined_at: DateTime<Utc>,
    is_host: bool,
}

#[derive(Debug)]
struct RelaySession {
    id: String,
    host_id: Uuid,
    peers: HashMap<Uuid, ConnectedPeer>,
    max_peers: usize,
    created_at: DateTime<Utc>,
}

pub struct RelayServer {
    sessions: Arc<RwLock<HashMap<String, RelaySession>>>,
    peers_by_id: Arc<RwLock<HashMap<Uuid, String>>>,
    shutdown_tx: Option<broadcast::Sender<()>>,
    bind_addr: Option<SocketAddr>,
}

impl RelayServer {
    pub fn new() -> Self {
        Self {
            sessions: Arc::new(RwLock::new(HashMap::new())),
            peers_by_id: Arc::new(RwLock::new(HashMap::new())),
            shutdown_tx: None,
            bind_addr: None,
        }
    }
    
    pub async fn start(&mut self, addr: &str) -> Result<SocketAddr, RelayError> {
        let listener = TcpListener::bind(addr).await
            .map_err(|e| RelayError::BindFailed(e.to_string()))?;
        
        let local_addr = listener.local_addr()?;
        self.bind_addr = Some(local_addr);
        
        let (shutdown_tx, _) = broadcast::channel(1);
        self.shutdown_tx = Some(shutdown_tx.clone());
        
        let sessions = Arc::clone(&self.sessions);
        let peers_by_id = Arc::clone(&self.peers_by_id);
        
        info!("Relay server starting on {}", local_addr);
        
        tokio::spawn(async move {
            let mut shutdown_rx = shutdown_tx.subscribe();
            
            loop {
                tokio::select! {
                    result = listener.accept() => {
                        match result {
                            Ok((stream, addr)) => {
                                let sessions = Arc::clone(&sessions);
                                let peers_by_id = Arc::clone(&peers_by_id);
                                tokio::spawn(Self::handle_connection(stream, addr, sessions, peers_by_id));
                            }
                            Err(e) => {
                                error!("Failed to accept connection: {}", e);
                            }
                        }
                    }
                    _ = shutdown_rx.recv() => {
                        info!("Relay server shutting down");
                        break;
                    }
                }
            }
        });
        
        Ok(local_addr)
    }
    
    async fn handle_connection(
        stream: TcpStream,
        addr: SocketAddr,
        sessions: Arc<RwLock<HashMap<String, RelaySession>>>,
        peers_by_id: Arc<RwLock<HashMap<Uuid, String>>>,
    ) {
        info!("New connection from {}", addr);
        
        let ws_stream = match accept_async(stream).await {
            Ok(ws) => ws,
            Err(e) => {
                error!("WebSocket handshake failed for {}: {}", addr, e);
                return;
            }
        };
        
        let (mut ws_sender, mut ws_receiver) = ws_stream.split();
        let (tx, mut rx) = mpsc::unbounded_channel::<Message>();
        
        let send_task = tokio::spawn(async move {
            while let Some(msg) = rx.recv().await {
                if ws_sender.send(msg).await.is_err() {
                    break;
                }
            }
        });
        
        let mut current_user_id: Option<Uuid> = None;
        let mut current_session_id: Option<String> = None;
        
        while let Some(result) = ws_receiver.next().await {
            match result {
                Ok(Message::Text(text)) => {
                    match serde_json::from_str::<RelayMessage>(&text) {
                        Ok(msg) => {
                            match msg {
                                RelayMessage::Join { session_id, user_id, username } => {
                                    let mut sessions_guard = sessions.write().await;
                                    
                                    let session = sessions_guard
                                        .entry(session_id.clone())
                                        .or_insert_with(|| RelaySession {
                                            id: session_id.clone(),
                                            host_id: user_id,
                                            peers: HashMap::new(),
                                            max_peers: 8,
                                            created_at: Utc::now(),
                                        });
                                    
                                    if session.peers.len() >= session.max_peers {
                                        let error_msg = RelayMessage::Error {
                                            message: "Session full".to_string(),
                                        };
                                        let _ = tx.send(Message::Text(serde_json::to_string(&error_msg).unwrap().into()));
                                        continue;
                                    }
                                    
                                    let is_host = session.peers.is_empty() || session.host_id == user_id;
                                    
                                    let peer = ConnectedPeer {
                                        user_id,
                                        username: username.clone(),
                                        session_id: session_id.clone(),
                                        sender: tx.clone(),
                                        joined_at: Utc::now(),
                                        is_host,
                                    };
                                    
                                    let peer_info = PeerInfo {
                                        user_id,
                                        username: username.clone(),
                                        is_host,
                                        joined_at: peer.joined_at,
                                        latency_ms: None,
                                    };
                                    
                                    let existing_peers: Vec<PeerInfo> = session.peers.values()
                                        .map(|p| PeerInfo {
                                            user_id: p.user_id,
                                            username: p.username.clone(),
                                            is_host: p.is_host,
                                            joined_at: p.joined_at,
                                            latency_ms: None,
                                        })
                                        .collect();
                                    
                                    for existing in session.peers.values() {
                                        let join_msg = RelayMessage::PeerJoined { peer: peer_info.clone() };
                                        let _ = existing.sender.send(Message::Text(serde_json::to_string(&join_msg).unwrap().into()));
                                    }
                                    
                                    session.peers.insert(user_id, peer);
                                    
                                    drop(sessions_guard);
                                    peers_by_id.write().await.insert(user_id, session_id.clone());
                                    
                                    current_user_id = Some(user_id);
                                    current_session_id = Some(session_id);
                                    
                                    let peer_list = RelayMessage::PeerList { peers: existing_peers };
                                    let _ = tx.send(Message::Text(serde_json::to_string(&peer_list).unwrap().into()));
                                    
                                    info!("User {} ({}) joined session", username, user_id);
                                }
                                
                                RelayMessage::Data { from, to, payload } => {
                                    if let Some(ref session_id) = current_session_id {
                                        let sessions_guard = sessions.read().await;
                                        if let Some(session) = sessions_guard.get(session_id) {
                                            let data_msg = RelayMessage::Data { from, to, payload };
                                            let msg_text = serde_json::to_string(&data_msg).unwrap();
                                            
                                            if let Some(target_id) = to {
                                                if let Some(target) = session.peers.get(&target_id) {
                                                    let _ = target.sender.send(Message::Text(msg_text.into()));
                                                }
                                            } else {
                                                for (peer_id, peer) in &session.peers {
                                                    if *peer_id != from {
                                                        let _ = peer.sender.send(Message::Text(msg_text.clone().into()));
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                RelayMessage::Ping => {
                                    let _ = tx.send(Message::Text(serde_json::to_string(&RelayMessage::Pong).unwrap().into()));
                                }
                                
                                RelayMessage::Leave { session_id, user_id } => {
                                    Self::remove_peer(&sessions, &peers_by_id, &session_id, user_id).await;
                                    break;
                                }
                                
                                _ => {}
                            }
                        }
                        Err(e) => {
                            warn!("Invalid message from {}: {}", addr, e);
                        }
                    }
                }
                Ok(Message::Binary(data)) => {
                    if let (Some(ref session_id), Some(user_id)) = (&current_session_id, current_user_id) {
                        let sessions_guard = sessions.read().await;
                        if let Some(session) = sessions_guard.get(session_id) {
                            for (peer_id, peer) in &session.peers {
                                if *peer_id != user_id {
                                    let _ = peer.sender.send(Message::Binary(data.clone()));
                                }
                            }
                        }
                    }
                }
                Ok(Message::Close(_)) => {
                    break;
                }
                Err(e) => {
                    error!("WebSocket error from {}: {}", addr, e);
                    break;
                }
                _ => {}
            }
        }
        
        if let (Some(session_id), Some(user_id)) = (current_session_id, current_user_id) {
            Self::remove_peer(&sessions, &peers_by_id, &session_id, user_id).await;
        }
        
        send_task.abort();
        info!("Connection closed for {}", addr);
    }
    
    async fn remove_peer(
        sessions: &Arc<RwLock<HashMap<String, RelaySession>>>,
        peers_by_id: &Arc<RwLock<HashMap<Uuid, String>>>,
        session_id: &str,
        user_id: Uuid,
    ) {
        let mut sessions_guard = sessions.write().await;
        
        if let Some(session) = sessions_guard.get_mut(session_id) {
            let was_host = session.peers.get(&user_id).map(|p| p.is_host).unwrap_or(false);
            session.peers.remove(&user_id);
            
            let leave_msg = RelayMessage::PeerLeft { user_id };
            for peer in session.peers.values() {
                let _ = peer.sender.send(Message::Text(serde_json::to_string(&leave_msg).unwrap().into()));
            }
            
            if was_host && !session.peers.is_empty() {
                let new_host_id = *session.peers.keys().next().unwrap();
                if let Some(new_host) = session.peers.get_mut(&new_host_id) {
                    new_host.is_host = true;
                }
                session.host_id = new_host_id;
                
                let migration_msg = RelayMessage::HostMigration { new_host: new_host_id };
                for peer in session.peers.values() {
                    let _ = peer.sender.send(Message::Text(serde_json::to_string(&migration_msg).unwrap().into()));
                }
                info!("Host migrated to {} in session {}", new_host_id, session_id);
            }
            
            if session.peers.is_empty() {
                sessions_guard.remove(session_id);
                info!("Session {} closed (no peers remaining)", session_id);
            }
        }
        
        peers_by_id.write().await.remove(&user_id);
    }
    
    pub async fn stop(&mut self) {
        if let Some(tx) = self.shutdown_tx.take() {
            let _ = tx.send(());
        }
        self.bind_addr = None;
        info!("Relay server stopped");
    }
    
    pub fn is_running(&self) -> bool {
        self.bind_addr.is_some()
    }
    
    pub fn bind_address(&self) -> Option<SocketAddr> {
        self.bind_addr
    }
    
    pub async fn get_session_count(&self) -> usize {
        self.sessions.read().await.len()
    }
    
    pub async fn get_total_peers(&self) -> usize {
        self.peers_by_id.read().await.len()
    }
    
    pub async fn get_session_info(&self, session_id: &str) -> Option<SessionInfo> {
        let sessions = self.sessions.read().await;
        sessions.get(session_id).map(|s| SessionInfo {
            id: s.id.clone(),
            host_id: s.host_id,
            peer_count: s.peers.len(),
            max_peers: s.max_peers,
            created_at: s.created_at,
        })
    }
}

impl Default for RelayServer {
    fn default() -> Self {
        Self::new()
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SessionInfo {
    pub id: String,
    pub host_id: Uuid,
    pub peer_count: usize,
    pub max_peers: usize,
    pub created_at: DateTime<Utc>,
}

pub struct RelayClient {
    server_url: String,
    sender: Option<mpsc::UnboundedSender<Message>>,
    user_id: Uuid,
    session_id: Option<String>,
}

impl RelayClient {
    pub fn new(server_url: &str, user_id: Uuid) -> Self {
        Self {
            server_url: server_url.to_string(),
            sender: None,
            user_id,
            session_id: None,
        }
    }
    
    pub async fn connect(&mut self, session_id: &str, username: &str) -> Result<mpsc::UnboundedReceiver<RelayMessage>, RelayError> {
        let (ws_stream, _) = tokio_tungstenite::connect_async(&self.server_url)
            .await
            .map_err(|e| RelayError::ConnectionFailed(e.to_string()))?;
        
        let (mut ws_sender, mut ws_receiver) = ws_stream.split();
        let (tx, mut rx) = mpsc::unbounded_channel::<Message>();
        let (msg_tx, msg_rx) = mpsc::unbounded_channel::<RelayMessage>();
        
        self.sender = Some(tx.clone());
        self.session_id = Some(session_id.to_string());
        
        let join_msg = RelayMessage::Join {
            session_id: session_id.to_string(),
            user_id: self.user_id,
            username: username.to_string(),
        };
        
        let _ = tx.send(Message::Text(serde_json::to_string(&join_msg).unwrap().into()));
        
        tokio::spawn(async move {
            while let Some(msg) = rx.recv().await {
                if ws_sender.send(msg).await.is_err() {
                    break;
                }
            }
        });
        
        tokio::spawn(async move {
            while let Some(result) = ws_receiver.next().await {
                match result {
                    Ok(Message::Text(text)) => {
                        if let Ok(msg) = serde_json::from_str::<RelayMessage>(&text) {
                            if msg_tx.send(msg).is_err() {
                                break;
                            }
                        }
                    }
                    Ok(Message::Close(_)) | Err(_) => break,
                    _ => {}
                }
            }
        });
        
        info!("Connected to relay session {}", session_id);
        Ok(msg_rx)
    }
    
    pub fn send_data(&self, payload: Vec<u8>, to: Option<Uuid>) -> Result<(), RelayError> {
        let sender = self.sender.as_ref().ok_or(RelayError::NotRunning)?;
        
        let msg = RelayMessage::Data {
            from: self.user_id,
            to,
            payload,
        };
        
        sender.send(Message::Text(serde_json::to_string(&msg).unwrap().into()))
            .map_err(|_| RelayError::ConnectionFailed("Channel closed".to_string()))
    }
    
    pub fn send_binary(&self, data: Vec<u8>) -> Result<(), RelayError> {
        let sender = self.sender.as_ref().ok_or(RelayError::NotRunning)?;
        sender.send(Message::Binary(data.into()))
            .map_err(|_| RelayError::ConnectionFailed("Channel closed".to_string()))
    }
    
    pub fn disconnect(&mut self) {
        if let (Some(sender), Some(session_id)) = (self.sender.take(), self.session_id.take()) {
            let leave_msg = RelayMessage::Leave {
                session_id,
                user_id: self.user_id,
            };
            let _ = sender.send(Message::Text(serde_json::to_string(&leave_msg).unwrap().into()));
        }
    }
    
    pub fn is_connected(&self) -> bool {
        self.sender.is_some()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    
    #[test]
    fn test_relay_message_serialization() {
        let msg = RelayMessage::Join {
            session_id: "test-123".to_string(),
            user_id: Uuid::new_v4(),
            username: "player1".to_string(),
        };
        let json = serde_json::to_string(&msg).unwrap();
        assert!(json.contains("join"));
        assert!(json.contains("test-123"));
    }
    
    #[test]
    fn test_peer_info() {
        let peer = PeerInfo {
            user_id: Uuid::new_v4(),
            username: "test_user".to_string(),
            is_host: true,
            joined_at: Utc::now(),
            latency_ms: Some(50),
        };
        assert!(peer.is_host);
    }
}
