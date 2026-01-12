//! IPC API Module
//! 
//! Provides the communication layer between UI and core:
//! - Strict JSON schema
//! - Versioned commands
//! - Error-first responses
//! 
//! The UI communicates ONLY via IPC - no filesystem access from UI.

use serde::{Deserialize, Serialize};
use thiserror::Error;
use uuid::Uuid;
use tracing::info;

use crate::core::{
    launcher::LauncherService,
    profiles::ProfileManager,
    cache::CacheManager,
    sessions::SessionOrchestrator,
    diagnostics::DiagnosticsCollector,
    users::{UserService, SignupRequest, LoginRequest},
    friends::FriendsService,
    relay::RelayServer,
};
use std::sync::Arc;
use tokio::sync::RwLock;

/// IPC API version
pub const IPC_VERSION: &str = "1.0.0";

#[derive(Error, Debug)]
pub enum IpcError {
    #[error("Unknown command: {0}")]
    UnknownCommand(String),
    
    #[error("Invalid parameters: {0}")]
    InvalidParameters(String),
    
    #[error("Command failed: {0}")]
    CommandFailed(String),
    
    #[error("Serialization error: {0}")]
    SerializationError(String),
    
    #[error("Version mismatch: expected {expected}, got {actual}")]
    VersionMismatch { expected: String, actual: String },
}

/// An IPC request from the UI
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct IpcRequest {
    /// Request ID for correlation
    pub id: Uuid,
    
    /// API version
    pub version: String,
    
    /// Command to execute
    pub command: String,
    
    /// Command parameters (JSON value)
    pub params: serde_json::Value,
}

/// An IPC response to the UI
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct IpcResponse {
    /// Request ID this responds to
    pub id: Uuid,
    
    /// API version
    pub version: String,
    
    /// Whether the command succeeded
    pub success: bool,
    
    /// Error message (if success is false)
    pub error: Option<String>,
    
    /// Result data (if success is true)
    pub data: Option<serde_json::Value>,
}

impl IpcResponse {
    /// Create a success response
    pub fn success(id: Uuid, data: serde_json::Value) -> Self {
        Self {
            id,
            version: IPC_VERSION.to_string(),
            success: true,
            error: None,
            data: Some(data),
        }
    }
    
    /// Create an error response
    pub fn error(id: Uuid, error: impl Into<String>) -> Self {
        Self {
            id,
            version: IPC_VERSION.to_string(),
            success: false,
            error: Some(error.into()),
            data: None,
        }
    }
}

/// Available IPC commands
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum Command {
    // System commands
    GetVersion,
    GetStatus,
    
    // Launcher commands
    LaunchGame,
    GetGameState,
    TerminateGame,
    
    // Profile commands
    ListProfiles,
    GetProfile,
    CreateProfile,
    UpdateProfile,
    DeleteProfile,
    
    // Mod commands
    ListMods,
    InstallMod,
    RemoveMod,
    EnableMod,
    DisableMod,
    
    // Cache commands
    GetCacheStats,
    ClearCache,
    
    // Performance commands
    GetSystemSnapshot,
    PrepareForLaunch,
    
    // Diagnostics commands
    CollectMetrics,
    GetDiagnosticsReport,
    ExportDiagnostics,
    
    // Session commands
    CreateSession,
    JoinSession,
    LeaveSession,
    GetSessionInfo,
    GetInviteCode,
    
    // User/Auth commands
    Signup,
    Login,
    Logout,
    ValidateSession,
    GetCurrentUser,
    UpdateUserProfile,
    SearchUsers,
    
    // Friends commands
    SendFriendRequest,
    AcceptFriendRequest,
    DeclineFriendRequest,
    RemoveFriend,
    GetFriends,
    GetPendingRequests,
    GetOnlineFriends,
    BlockUser,
    UnblockUser,
    GetBlockedUsers,
    
    // Relay commands
    StartRelayServer,
    StopRelayServer,
    GetRelayStatus,
    ConnectToRelay,
    DisconnectFromRelay,
}

/// The IPC server handling UI communication
pub struct IpcServer {
    launcher: LauncherService,
    profiles: ProfileManager,
    cache: CacheManager,
    sessions: SessionOrchestrator,
    diagnostics: DiagnosticsCollector,
    users: Option<UserService>,
    friends: Option<FriendsService>,
    relay: Arc<RwLock<RelayServer>>,
}

impl IpcServer {
    /// Create a new IPC server
    pub fn new(
        launcher: LauncherService,
        profiles: ProfileManager,
        cache: CacheManager,
        sessions: SessionOrchestrator,
        diagnostics: DiagnosticsCollector,
    ) -> Self {
        Self {
            launcher,
            profiles,
            cache,
            sessions,
            diagnostics,
            users: None,
            friends: None,
            relay: Arc::new(RwLock::new(RelayServer::new())),
        }
    }
    
    pub fn with_services(
        mut self,
        users: Option<UserService>,
        friends: Option<FriendsService>,
    ) -> Self {
        self.users = users;
        self.friends = friends;
        self
    }
    
    /// Handle an incoming IPC request
    pub async fn handle(&mut self, request: IpcRequest) -> IpcResponse {
        // Version check
        if request.version != IPC_VERSION {
            return IpcResponse::error(
                request.id,
                format!("Version mismatch: expected {}, got {}", IPC_VERSION, request.version),
            );
        }
        
        info!("Handling IPC command: {}", request.command);
        
        match request.command.as_str() {
            // System commands
            "get_version" => {
                IpcResponse::success(request.id, serde_json::json!({
                    "version": crate::VERSION,
                    "ipc_version": IPC_VERSION,
                }))
            }
            
            "get_status" => {
                let game_state = self.launcher.get_state().await;
                let session = self.sessions.current_session();
                
                IpcResponse::success(request.id, serde_json::json!({
                    "game_state": game_state,
                    "in_session": session.is_some(),
                    "session_id": session.map(|s| s.id.to_string()),
                }))
            }
            
            // Launcher commands
            "launch_game" => {
                match serde_json::from_value::<crate::core::launcher::LaunchConfig>(request.params.clone()) {
                    Ok(config) => {
                        match self.launcher.launch(config).await {
                            Ok(pid) => IpcResponse::success(request.id, serde_json::json!({ "pid": pid })),
                            Err(e) => IpcResponse::error(request.id, e.to_string()),
                        }
                    }
                    Err(e) => IpcResponse::error(request.id, format!("Invalid launch config: {}", e)),
                }
            }
            
            "get_game_state" => {
                let state = self.launcher.get_state().await;
                IpcResponse::success(request.id, serde_json::to_value(state).unwrap_or_default())
            }
            
            "terminate_game" => {
                match self.launcher.terminate().await {
                    Ok(_) => IpcResponse::success(request.id, serde_json::json!({ "terminated": true })),
                    Err(e) => IpcResponse::error(request.id, e.to_string()),
                }
            }
            
            // Profile commands
            "list_profiles" => {
                let profiles: Vec<_> = self.profiles.list().iter().map(|p| {
                    serde_json::json!({
                        "id": p.id.to_string(),
                        "name": p.name,
                        "created_at": p.created_at,
                    })
                }).collect();
                
                IpcResponse::success(request.id, serde_json::json!({ "profiles": profiles }))
            }
            
            "get_profile" => {
                if let Some(id_str) = request.params.get("id").and_then(|v| v.as_str()) {
                    if let Ok(id) = Uuid::parse_str(id_str) {
                        if let Some(profile) = self.profiles.get(&id) {
                            return IpcResponse::success(
                                request.id, 
                                serde_json::to_value(profile).unwrap_or_default()
                            );
                        }
                    }
                }
                IpcResponse::error(request.id, "Profile not found")
            }
            
            "create_profile" => {
                if let Some(name) = request.params.get("name").and_then(|v| v.as_str()) {
                    match self.profiles.create(name).await {
                        Ok(profile) => IpcResponse::success(
                            request.id,
                            serde_json::to_value(&profile).unwrap_or_default()
                        ),
                        Err(e) => IpcResponse::error(request.id, e.to_string()),
                    }
                } else {
                    IpcResponse::error(request.id, "Missing 'name' parameter")
                }
            }
            
            // Cache commands
            "get_cache_stats" => {
                let stats = self.cache.stats();
                IpcResponse::success(request.id, serde_json::to_value(stats).unwrap_or_default())
            }
            
            "clear_cache" => {
                match self.cache.clear().await {
                    Ok(_) => IpcResponse::success(request.id, serde_json::json!({ "cleared": true })),
                    Err(e) => IpcResponse::error(request.id, e.to_string()),
                }
            }
            
            // Diagnostics commands
            "collect_metrics" => {
                let sample = self.diagnostics.collect_sample();
                IpcResponse::success(request.id, serde_json::to_value(sample).unwrap_or_default())
            }
            
            "get_diagnostics_report" => {
                let report = self.diagnostics.generate_report();
                IpcResponse::success(request.id, serde_json::to_value(report).unwrap_or_default())
            }
            
            // Session commands
            "create_session" => {
                let name = request.params.get("name")
                    .and_then(|v| v.as_str())
                    .unwrap_or("Host")
                    .to_string();
                let max = request.params.get("max_participants")
                    .and_then(|v| v.as_u64())
                    .unwrap_or(8) as usize;
                
                match self.sessions.create_session(name, max).await {
                    Ok(session) => IpcResponse::success(
                        request.id,
                        serde_json::json!({
                            "session_id": session.id.to_string(),
                            "invite_code": session.invite_code,
                        })
                    ),
                    Err(e) => IpcResponse::error(request.id, e.to_string()),
                }
            }
            
            "get_invite_code" => {
                match self.sessions.get_invite_code() {
                    Some(code) => IpcResponse::success(request.id, serde_json::json!({ "invite_code": code })),
                    None => IpcResponse::error(request.id, "Not in a session"),
                }
            }
            
            "leave_session" => {
                match self.sessions.leave_session().await {
                    Ok(_) => IpcResponse::success(request.id, serde_json::json!({ "left": true })),
                    Err(e) => IpcResponse::error(request.id, e.to_string()),
                }
            }
            
            // User/Auth commands
            "signup" => {
                let Some(ref users) = self.users else {
                    return IpcResponse::error(request.id, "Database not available");
                };
                match serde_json::from_value::<SignupRequest>(request.params.clone()) {
                    Ok(req) => match users.signup(req).await {
                        Ok(auth) => IpcResponse::success(request.id, serde_json::json!({
                            "user": auth.user,
                            "session": { "token": auth.session.token, "expires_at": auth.session.expires_at }
                        })),
                        Err(e) => IpcResponse::error(request.id, e.to_string()),
                    },
                    Err(e) => IpcResponse::error(request.id, format!("Invalid signup request: {}", e)),
                }
            }
            
            "login" => {
                let Some(ref users) = self.users else {
                    return IpcResponse::error(request.id, "Database not available");
                };
                match serde_json::from_value::<LoginRequest>(request.params.clone()) {
                    Ok(req) => match users.login(req).await {
                        Ok(auth) => IpcResponse::success(request.id, serde_json::json!({
                            "user": auth.user,
                            "session": { "token": auth.session.token, "expires_at": auth.session.expires_at }
                        })),
                        Err(e) => IpcResponse::error(request.id, e.to_string()),
                    },
                    Err(e) => IpcResponse::error(request.id, format!("Invalid login request: {}", e)),
                }
            }
            
            "logout" => {
                let Some(ref users) = self.users else {
                    return IpcResponse::error(request.id, "Database not available");
                };
                let token = request.params.get("token").and_then(|v| v.as_str()).unwrap_or("");
                match users.logout(token).await {
                    Ok(_) => IpcResponse::success(request.id, serde_json::json!({ "logged_out": true })),
                    Err(e) => IpcResponse::error(request.id, e.to_string()),
                }
            }
            
            "validate_session" => {
                let Some(ref users) = self.users else {
                    return IpcResponse::error(request.id, "Database not available");
                };
                let token = request.params.get("token").and_then(|v| v.as_str()).unwrap_or("");
                match users.validate_session(token).await {
                    Ok(user) => IpcResponse::success(request.id, serde_json::to_value(user).unwrap_or_default()),
                    Err(e) => IpcResponse::error(request.id, e.to_string()),
                }
            }
            
            "search_users" => {
                let Some(ref users) = self.users else {
                    return IpcResponse::error(request.id, "Database not available");
                };
                let query = request.params.get("query").and_then(|v| v.as_str()).unwrap_or("");
                let limit = request.params.get("limit").and_then(|v| v.as_i64()).unwrap_or(20);
                match users.search_users(query, limit).await {
                    Ok(results) => IpcResponse::success(request.id, serde_json::json!({ "users": results })),
                    Err(e) => IpcResponse::error(request.id, e.to_string()),
                }
            }
            
            "get_current_user" => {
                let Some(ref users) = self.users else {
                    return IpcResponse::error(request.id, "Database not available");
                };
                let token = request.params.get("token").and_then(|v| v.as_str()).unwrap_or("");
                match users.validate_session(token).await {
                    Ok(user) => IpcResponse::success(request.id, serde_json::to_value(user).unwrap_or_default()),
                    Err(e) => IpcResponse::error(request.id, e.to_string()),
                }
            }
            
            "update_user_profile" => {
                let Some(ref users) = self.users else {
                    return IpcResponse::error(request.id, "Database not available");
                };
                let user_id = request.params.get("user_id").and_then(|v| v.as_str())
                    .and_then(|s| Uuid::parse_str(s).ok());
                let display_name = request.params.get("display_name").and_then(|v| v.as_str());
                let avatar_url = request.params.get("avatar_url").and_then(|v| v.as_str());
                match user_id {
                    Some(id) => match users.update_profile(id, display_name, avatar_url).await {
                        Ok(user) => IpcResponse::success(request.id, serde_json::to_value(user).unwrap_or_default()),
                        Err(e) => IpcResponse::error(request.id, e.to_string()),
                    },
                    None => IpcResponse::error(request.id, "Invalid user ID"),
                }
            }
            
            // Friends commands
            "send_friend_request" => {
                let Some(ref friends) = self.friends else {
                    return IpcResponse::error(request.id, "Database not available");
                };
                let from_id = request.params.get("from_user_id").and_then(|v| v.as_str())
                    .and_then(|s| Uuid::parse_str(s).ok());
                let to_id = request.params.get("to_user_id").and_then(|v| v.as_str())
                    .and_then(|s| Uuid::parse_str(s).ok());
                match (from_id, to_id) {
                    (Some(from), Some(to)) => match friends.send_friend_request(from, to).await {
                        Ok(id) => IpcResponse::success(request.id, serde_json::json!({ "request_id": id })),
                        Err(e) => IpcResponse::error(request.id, e.to_string()),
                    },
                    _ => IpcResponse::error(request.id, "Invalid user IDs"),
                }
            }
            
            "accept_friend_request" => {
                let Some(ref friends) = self.friends else {
                    return IpcResponse::error(request.id, "Database not available");
                };
                let user_id = request.params.get("user_id").and_then(|v| v.as_str())
                    .and_then(|s| Uuid::parse_str(s).ok());
                let from_id = request.params.get("from_user_id").and_then(|v| v.as_str())
                    .and_then(|s| Uuid::parse_str(s).ok());
                match (user_id, from_id) {
                    (Some(user), Some(from)) => match friends.accept_friend_request(user, from).await {
                        Ok(_) => IpcResponse::success(request.id, serde_json::json!({ "accepted": true })),
                        Err(e) => IpcResponse::error(request.id, e.to_string()),
                    },
                    _ => IpcResponse::error(request.id, "Invalid user IDs"),
                }
            }
            
            "decline_friend_request" => {
                let Some(ref friends) = self.friends else {
                    return IpcResponse::error(request.id, "Database not available");
                };
                let user_id = request.params.get("user_id").and_then(|v| v.as_str())
                    .and_then(|s| Uuid::parse_str(s).ok());
                let from_id = request.params.get("from_user_id").and_then(|v| v.as_str())
                    .and_then(|s| Uuid::parse_str(s).ok());
                match (user_id, from_id) {
                    (Some(user), Some(from)) => match friends.decline_friend_request(user, from).await {
                        Ok(_) => IpcResponse::success(request.id, serde_json::json!({ "declined": true })),
                        Err(e) => IpcResponse::error(request.id, e.to_string()),
                    },
                    _ => IpcResponse::error(request.id, "Invalid user IDs"),
                }
            }
            
            "remove_friend" => {
                let Some(ref friends) = self.friends else {
                    return IpcResponse::error(request.id, "Database not available");
                };
                let user_id = request.params.get("user_id").and_then(|v| v.as_str())
                    .and_then(|s| Uuid::parse_str(s).ok());
                let friend_id = request.params.get("friend_id").and_then(|v| v.as_str())
                    .and_then(|s| Uuid::parse_str(s).ok());
                match (user_id, friend_id) {
                    (Some(user), Some(friend)) => match friends.remove_friend(user, friend).await {
                        Ok(_) => IpcResponse::success(request.id, serde_json::json!({ "removed": true })),
                        Err(e) => IpcResponse::error(request.id, e.to_string()),
                    },
                    _ => IpcResponse::error(request.id, "Invalid user IDs"),
                }
            }
            
            "get_friends" => {
                let Some(ref friends) = self.friends else {
                    return IpcResponse::error(request.id, "Database not available");
                };
                let user_id = request.params.get("user_id").and_then(|v| v.as_str())
                    .and_then(|s| Uuid::parse_str(s).ok());
                match user_id {
                    Some(id) => match friends.get_friends(id).await {
                        Ok(list) => IpcResponse::success(request.id, serde_json::json!({ "friends": list })),
                        Err(e) => IpcResponse::error(request.id, e.to_string()),
                    },
                    None => IpcResponse::error(request.id, "Invalid user ID"),
                }
            }
            
            "get_pending_requests" => {
                let Some(ref friends) = self.friends else {
                    return IpcResponse::error(request.id, "Database not available");
                };
                let user_id = request.params.get("user_id").and_then(|v| v.as_str())
                    .and_then(|s| Uuid::parse_str(s).ok());
                match user_id {
                    Some(id) => match friends.get_pending_requests(id).await {
                        Ok(list) => IpcResponse::success(request.id, serde_json::json!({ "requests": list })),
                        Err(e) => IpcResponse::error(request.id, e.to_string()),
                    },
                    None => IpcResponse::error(request.id, "Invalid user ID"),
                }
            }
            
            "get_online_friends" => {
                let Some(ref friends) = self.friends else {
                    return IpcResponse::error(request.id, "Database not available");
                };
                let user_id = request.params.get("user_id").and_then(|v| v.as_str())
                    .and_then(|s| Uuid::parse_str(s).ok());
                match user_id {
                    Some(id) => match friends.get_online_friends(id).await {
                        Ok(list) => IpcResponse::success(request.id, serde_json::json!({ "friends": list })),
                        Err(e) => IpcResponse::error(request.id, e.to_string()),
                    },
                    None => IpcResponse::error(request.id, "Invalid user ID"),
                }
            }
            
            "block_user" => {
                let Some(ref friends) = self.friends else {
                    return IpcResponse::error(request.id, "Database not available");
                };
                let blocker_id = request.params.get("blocker_id").and_then(|v| v.as_str())
                    .and_then(|s| Uuid::parse_str(s).ok());
                let blocked_id = request.params.get("blocked_id").and_then(|v| v.as_str())
                    .and_then(|s| Uuid::parse_str(s).ok());
                let reason = request.params.get("reason").and_then(|v| v.as_str());
                match (blocker_id, blocked_id) {
                    (Some(blocker), Some(blocked)) => match friends.block_user(blocker, blocked, reason).await {
                        Ok(_) => IpcResponse::success(request.id, serde_json::json!({ "blocked": true })),
                        Err(e) => IpcResponse::error(request.id, e.to_string()),
                    },
                    _ => IpcResponse::error(request.id, "Invalid user IDs"),
                }
            }
            
            "unblock_user" => {
                let Some(ref friends) = self.friends else {
                    return IpcResponse::error(request.id, "Database not available");
                };
                let blocker_id = request.params.get("blocker_id").and_then(|v| v.as_str())
                    .and_then(|s| Uuid::parse_str(s).ok());
                let blocked_id = request.params.get("blocked_id").and_then(|v| v.as_str())
                    .and_then(|s| Uuid::parse_str(s).ok());
                match (blocker_id, blocked_id) {
                    (Some(blocker), Some(blocked)) => match friends.unblock_user(blocker, blocked).await {
                        Ok(_) => IpcResponse::success(request.id, serde_json::json!({ "unblocked": true })),
                        Err(e) => IpcResponse::error(request.id, e.to_string()),
                    },
                    _ => IpcResponse::error(request.id, "Invalid user IDs"),
                }
            }
            
            "get_blocked_users" => {
                let Some(ref friends) = self.friends else {
                    return IpcResponse::error(request.id, "Database not available");
                };
                let user_id = request.params.get("user_id").and_then(|v| v.as_str())
                    .and_then(|s| Uuid::parse_str(s).ok());
                match user_id {
                    Some(id) => match friends.get_blocked_users(id).await {
                        Ok(list) => IpcResponse::success(request.id, serde_json::json!({ "blocked": list })),
                        Err(e) => IpcResponse::error(request.id, e.to_string()),
                    },
                    None => IpcResponse::error(request.id, "Invalid user ID"),
                }
            }
            
            // Relay commands
            "start_relay_server" => {
                let addr = request.params.get("address").and_then(|v| v.as_str()).unwrap_or("0.0.0.0:9000");
                let mut relay = self.relay.write().await;
                match relay.start(addr).await {
                    Ok(bound_addr) => IpcResponse::success(request.id, serde_json::json!({ "address": bound_addr.to_string() })),
                    Err(e) => IpcResponse::error(request.id, e.to_string()),
                }
            }
            
            "stop_relay_server" => {
                let mut relay = self.relay.write().await;
                relay.stop().await;
                IpcResponse::success(request.id, serde_json::json!({ "stopped": true }))
            }
            
            "get_relay_status" => {
                let relay = self.relay.read().await;
                IpcResponse::success(request.id, serde_json::json!({
                    "running": relay.is_running(),
                    "address": relay.bind_address().map(|a| a.to_string()),
                    "session_count": relay.get_session_count().await,
                    "peer_count": relay.get_total_peers().await,
                }))
            }
            
            "connect_to_relay" => {
                let relay = self.relay.read().await;
                if !relay.is_running() {
                    return IpcResponse::error(request.id, "Relay server not running");
                }
                let addr = relay.bind_address().map(|a| a.to_string());
                IpcResponse::success(request.id, serde_json::json!({
                    "relay_address": addr,
                    "note": "Use WebSocket client to connect to relay address with session_id and user credentials"
                }))
            }
            
            "disconnect_from_relay" => {
                IpcResponse::success(request.id, serde_json::json!({
                    "disconnected": true,
                    "note": "Client should close WebSocket connection to relay"
                }))
            }
            
            // Unknown command
            _ => IpcResponse::error(request.id, format!("Unknown command: {}", request.command)),
        }
    }
    
    /// Print current status (for testing)
    pub async fn status(&self) {
        info!("IPC Server ready");
        info!("  Version: {}", IPC_VERSION);
        info!("  Commands available: {}", Self::list_commands().len());
    }
    
    /// List all available commands
    pub fn list_commands() -> Vec<&'static str> {
        vec![
            "get_version",
            "get_status",
            "launch_game",
            "get_game_state",
            "terminate_game",
            "list_profiles",
            "get_profile",
            "create_profile",
            "update_profile",
            "delete_profile",
            "list_mods",
            "install_mod",
            "remove_mod",
            "enable_mod",
            "disable_mod",
            "get_cache_stats",
            "clear_cache",
            "get_system_snapshot",
            "prepare_for_launch",
            "collect_metrics",
            "get_diagnostics_report",
            "export_diagnostics",
            "create_session",
            "join_session",
            "leave_session",
            "get_session_info",
            "get_invite_code",
            "signup",
            "login",
            "logout",
            "validate_session",
            "get_current_user",
            "update_user_profile",
            "search_users",
            "send_friend_request",
            "accept_friend_request",
            "decline_friend_request",
            "remove_friend",
            "get_friends",
            "get_pending_requests",
            "get_online_friends",
            "block_user",
            "unblock_user",
            "get_blocked_users",
            "start_relay_server",
            "stop_relay_server",
            "get_relay_status",
            "connect_to_relay",
            "disconnect_from_relay",
        ]
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    
    #[test]
    fn test_ipc_response_success() {
        let id = Uuid::new_v4();
        let resp = IpcResponse::success(id, serde_json::json!({ "test": true }));
        assert!(resp.success);
        assert!(resp.error.is_none());
        assert!(resp.data.is_some());
    }
    
    #[test]
    fn test_ipc_response_error() {
        let id = Uuid::new_v4();
        let resp = IpcResponse::error(id, "test error");
        assert!(!resp.success);
        assert_eq!(resp.error, Some("test error".to_string()));
        assert!(resp.data.is_none());
    }
}
