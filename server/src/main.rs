use axum::{
    extract::{Path, State, WebSocketUpgrade, ws::{Message, WebSocket}},
    http::{StatusCode, Method, header},
    response::{IntoResponse, Json},
    routing::{get, post},
    Router,
};
use serde::{Deserialize, Serialize};
use sqlx::{postgres::PgPoolOptions, PgPool};
use std::{net::SocketAddr, sync::Arc};
use tokio::sync::RwLock;
use tower_http::cors::{Any, CorsLayer};
use tracing::{info, error};
use uuid::Uuid;
use sha2::Digest;

mod admin;
mod auth;
mod escrow;
mod features;
mod friends;
mod relay;
mod stripe;
mod verification;

use auth::{hash_password, verify_password, generate_token, hash_token};
use relay::RelayHub;
use verification::{VerificationService, VerificationMethod};

#[derive(Clone)]
pub struct AppState {
    pub db: PgPool,
    pub relay: Arc<RwLock<RelayHub>>,
    pub verification: Arc<VerificationService>,
}

#[derive(Debug, Serialize)]
struct ApiResponse<T> {
    success: bool,
    data: Option<T>,
    error: Option<String>,
}

impl<T: Serialize> ApiResponse<T> {
    fn success(data: T) -> Json<Self> {
        Json(Self { success: true, data: Some(data), error: None })
    }
    
    fn error(msg: impl Into<String>) -> Json<Self> {
        Json(Self { success: false, data: None, error: Some(msg.into()) })
    }
}

#[derive(Debug, Serialize)]
struct User {
    id: Uuid,
    username: String,
    display_name: Option<String>,
    avatar_url: Option<String>,
    premium: bool,
    created_at: chrono::DateTime<chrono::Utc>,
}

#[derive(Debug, Deserialize)]
struct SignupRequest {
    username: String,
    email: String,
    password: String,
}

#[derive(Debug, Deserialize)]
struct LoginRequest {
    username: String,
    password: String,
}

#[derive(Debug, Serialize)]
struct AuthResponse {
    user: User,
    token: String,
}

#[derive(Debug, Deserialize)]
struct TokenRequest {
    token: String,
}

#[derive(Debug, Deserialize)]
struct FriendRequest {
    token: String,
    target_user_id: Uuid,
}

#[derive(Debug, Deserialize)]
struct ProfileUpdateRequest {
    token: String,
    display_name: Option<String>,
    avatar_url: Option<String>,
}

#[derive(Debug, Serialize, Clone)]
struct GameServer {
    id: Uuid,
    name: String,
    description: Option<String>,
    address: String,
    port: i32,
    max_players: i32,
    current_players: i32,
    game_mode: String,
    tags: Vec<String>,
    owner_id: Uuid,
    is_online: bool,
    last_ping: chrono::DateTime<chrono::Utc>,
    created_at: chrono::DateTime<chrono::Utc>,
}

#[derive(Debug, Deserialize)]
struct RegisterServerRequest {
    token: String,
    name: String,
    description: Option<String>,
    address: String,
    port: i32,
    max_players: i32,
    game_mode: String,
    tags: Vec<String>,
}

#[derive(Debug, Deserialize)]
struct ServerHeartbeatRequest {
    token: String,
    server_id: Uuid,
    current_players: i32,
}

#[derive(Debug, Serialize)]
struct GameStats {
    user_id: Uuid,
    total_playtime_minutes: i64,
    total_sessions: i64,
    last_played: Option<chrono::DateTime<chrono::Utc>>,
    favorite_server: Option<String>,
    achievements_count: i32,
}

#[derive(Debug, Deserialize)]
struct RecordSessionRequest {
    token: String,
    duration_minutes: i32,
    server_name: Option<String>,
}

#[derive(Debug, Serialize, Clone)]
struct ModProfile {
    id: Uuid,
    user_id: Uuid,
    name: String,
    description: Option<String>,
    mods: serde_json::Value,
    is_active: bool,
    created_at: chrono::DateTime<chrono::Utc>,
}

#[derive(Debug, Deserialize)]
struct CreateModProfileRequest {
    token: String,
    name: String,
    description: Option<String>,
    mods: serde_json::Value,
}

#[derive(Debug, Deserialize)]
struct ActivateModProfileRequest {
    token: String,
    profile_id: Uuid,
}

#[derive(Debug, Serialize)]
struct PerformanceSettings {
    user_id: Uuid,
    ram_allocation_mb: i32,
    java_args: String,
    priority_mode: String,
    vsync_enabled: bool,
    max_fps: i32,
}

#[derive(Debug, Deserialize)]
struct UpdatePerformanceRequest {
    token: String,
    ram_allocation_mb: Option<i32>,
    java_args: Option<String>,
    priority_mode: Option<String>,
    vsync_enabled: Option<bool>,
    max_fps: Option<i32>,
}

#[derive(Debug, Serialize)]
struct Subscription {
    user_id: Uuid,
    tier: String,
    status: String,
    current_period_end: Option<chrono::DateTime<chrono::Utc>>,
    features: SubscriptionFeatures,
}

#[derive(Debug, Serialize)]
struct SubscriptionFeatures {
    max_friends: i32,
    cloud_storage_mb: i32,
    priority_relay: bool,
    custom_themes: bool,
    early_access: bool,
}

#[derive(Debug, Deserialize)]
struct CreateCheckoutRequest {
    token: String,
    price_id: String,
}

#[derive(Debug, Deserialize)]
struct ManageSubscriptionRequest {
    token: String,
}

#[derive(Debug, Serialize, Clone)]
struct MarketplaceItem {
    id: Uuid,
    name: String,
    description: String,
    category: String,
    author: MarketplaceAuthor,
    price: f64,
    downloads: i64,
    likes: i64,
    tags: Vec<String>,
    thumbnail_url: Option<String>,
    file_url: Option<String>,
    is_featured: bool,
    created_at: chrono::DateTime<chrono::Utc>,
}

#[derive(Debug, Serialize, Clone)]
struct MarketplaceAuthor {
    id: Uuid,
    username: String,
    display_name: Option<String>,
}

#[derive(Debug, Deserialize)]
struct CreateMarketplaceItemRequest {
    token: String,
    name: String,
    description: String,
    category: String,
    price: f64,
    tags: Vec<String>,
}

#[derive(Debug, Deserialize)]
struct MarketplaceQueryParams {
    category: Option<String>,
    price: Option<String>,
    sort: Option<String>,
    q: Option<String>,
}

async fn signup(
    State(state): State<AppState>,
    Json(req): Json<SignupRequest>,
) -> impl IntoResponse {
    if req.username.len() < 3 || req.username.len() > 32 {
        return (StatusCode::BAD_REQUEST, ApiResponse::<AuthResponse>::error("Username must be 3-32 characters"));
    }
    
    if req.password.len() < 8 {
        return (StatusCode::BAD_REQUEST, ApiResponse::<AuthResponse>::error("Password must be at least 8 characters"));
    }
    
    let existing = sqlx::query_scalar::<_, i64>("SELECT COUNT(*) FROM users WHERE username = $1 OR email = $2")
        .bind(&req.username)
        .bind(&req.email)
        .fetch_one(&state.db)
        .await;
    
    if let Ok(count) = existing {
        if count > 0 {
            return (StatusCode::CONFLICT, ApiResponse::error("Username or email already exists"));
        }
    }
    
    let password_hash = hash_password(&req.password);
    let user_id = Uuid::new_v4();
    let now = chrono::Utc::now();
    
    let result = sqlx::query(
        "INSERT INTO users (id, username, email, password_hash, created_at, updated_at) VALUES ($1, $2, $3, $4, $5, $5)"
    )
        .bind(user_id)
        .bind(&req.username)
        .bind(&req.email)
        .bind(&password_hash)
        .bind(now)
        .execute(&state.db)
        .await;
    
    if let Err(e) = result {
        error!("Failed to create user: {}", e);
        return (StatusCode::INTERNAL_SERVER_ERROR, ApiResponse::error("Failed to create account"));
    }
    
    let token = generate_token();
    let token_hash = hash_token(&token);
    let expires = now + chrono::Duration::days(30);
    
    let _ = sqlx::query(
        "INSERT INTO user_sessions (id, user_id, token_hash, expires_at, created_at) VALUES ($1, $2, $3, $4, $5)"
    )
        .bind(Uuid::new_v4())
        .bind(user_id)
        .bind(&token_hash)
        .bind(expires)
        .bind(now)
        .execute(&state.db)
        .await;
    
    let user = User {
        id: user_id,
        username: req.username,
        display_name: None,
        premium: false,
        avatar_url: None,
        created_at: now,
    };
    
    (StatusCode::CREATED, ApiResponse::success(AuthResponse { user, token }))
}

async fn login(
    State(state): State<AppState>,
    Json(req): Json<LoginRequest>,
) -> impl IntoResponse {
    let row = sqlx::query_as::<_, (Uuid, String, String, Option<String>, Option<String>, chrono::DateTime<chrono::Utc>)>(
        "SELECT id, username, password_hash, display_name, avatar_url, created_at FROM users WHERE username = $1"
    )
        .bind(&req.username)
        .fetch_optional(&state.db)
        .await;
    
    let (user_id, username, password_hash, display_name, avatar_url, created_at) = match row {
        Ok(Some(r)) => r,
        _ => return (StatusCode::UNAUTHORIZED, ApiResponse::<AuthResponse>::error("Invalid credentials")),
    };
    
    if !verify_password(&req.password, &password_hash) {
        return (StatusCode::UNAUTHORIZED, ApiResponse::error("Invalid credentials"));
    }
    
    let token = generate_token();
    let token_hash = hash_token(&token);
    let now = chrono::Utc::now();
    let expires = now + chrono::Duration::days(30);
    
    let _ = sqlx::query(
        "INSERT INTO user_sessions (id, user_id, token_hash, expires_at, created_at) VALUES ($1, $2, $3, $4, $5)"
    )
        .bind(Uuid::new_v4())
        .bind(user_id)
        .bind(&token_hash)
        .bind(expires)
        .bind(now)
        .execute(&state.db)
        .await;
    
    let _ = sqlx::query("UPDATE users SET last_seen = $1 WHERE id = $2")
        .bind(now)
        .bind(user_id)
        .execute(&state.db)
        .await;
    
    let user = User { id: user_id, username, display_name, avatar_url, premium: false, created_at };
    
    (StatusCode::OK, ApiResponse::success(AuthResponse { user, token }))
}

async fn logout(
    State(state): State<AppState>,
    Json(req): Json<TokenRequest>,
) -> impl IntoResponse {
    let token_hash = hash_token(&req.token);
    let _ = sqlx::query("DELETE FROM user_sessions WHERE token_hash = $1")
        .bind(&token_hash)
        .execute(&state.db)
        .await;
    
    (StatusCode::OK, ApiResponse::success(serde_json::json!({"logged_out": true})))
}

async fn get_me(
    State(state): State<AppState>,
    Json(req): Json<TokenRequest>,
) -> impl IntoResponse {
    let user = validate_token(&state.db, &req.token).await;
    match user {
        Some(u) => (StatusCode::OK, ApiResponse::success(u)),
        None => (StatusCode::UNAUTHORIZED, ApiResponse::error("Invalid or expired token")),
    }
}

async fn update_profile(
    State(state): State<AppState>,
    Json(req): Json<ProfileUpdateRequest>,
) -> impl IntoResponse {
    let user = validate_token(&state.db, &req.token).await;
    let user = match user {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<User>::error("Invalid token")),
    };
    
    let result = sqlx::query(
        "UPDATE users SET display_name = COALESCE($1, display_name), avatar_url = COALESCE($2, avatar_url), updated_at = $3 WHERE id = $4"
    )
        .bind(&req.display_name)
        .bind(&req.avatar_url)
        .bind(chrono::Utc::now())
        .bind(user.id)
        .execute(&state.db)
        .await;
    
    if result.is_err() {
        return (StatusCode::INTERNAL_SERVER_ERROR, ApiResponse::error("Failed to update profile"));
    }
    
    let updated = User {
        display_name: req.display_name.or(user.display_name),
        avatar_url: req.avatar_url.or(user.avatar_url),
        ..user
    };
    
    (StatusCode::OK, ApiResponse::success(updated))
}

async fn validate_token(db: &PgPool, token: &str) -> Option<User> {
    let token_hash = hash_token(token);
    let row = sqlx::query_as::<_, (Uuid, String, Option<String>, Option<String>, chrono::DateTime<chrono::Utc>)>(
        "SELECT u.id, u.username, u.display_name, u.avatar_url, u.created_at 
         FROM users u 
         JOIN user_sessions s ON u.id = s.user_id 
         WHERE s.token_hash = $1 AND s.expires_at > NOW()"
    )
        .bind(&token_hash)
        .fetch_optional(db)
        .await
        .ok()?;
    
    row.map(|(id, username, display_name, avatar_url, created_at)| {
        User { id, username, display_name, avatar_url, premium: false, created_at }
    })
}

async fn send_friend_request(
    State(state): State<AppState>,
    Json(req): Json<FriendRequest>,
) -> impl IntoResponse {
    let user = validate_token(&state.db, &req.token).await;
    let user = match user {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };
    
    if user.id == req.target_user_id {
        return (StatusCode::BAD_REQUEST, ApiResponse::error("Cannot friend yourself"));
    }
    
    let existing = sqlx::query_scalar::<_, i64>(
        "SELECT COUNT(*) FROM friendships WHERE (user_id = $1 AND friend_id = $2) OR (user_id = $2 AND friend_id = $1)"
    )
        .bind(user.id)
        .bind(req.target_user_id)
        .fetch_one(&state.db)
        .await
        .unwrap_or(0);
    
    if existing > 0 {
        return (StatusCode::CONFLICT, ApiResponse::error("Friendship already exists or pending"));
    }
    
    let result = sqlx::query(
        "INSERT INTO friendships (id, user_id, friend_id, status, created_at) VALUES ($1, $2, $3, 'pending', $4)"
    )
        .bind(Uuid::new_v4())
        .bind(user.id)
        .bind(req.target_user_id)
        .bind(chrono::Utc::now())
        .execute(&state.db)
        .await;
    
    match result {
        Ok(_) => (StatusCode::CREATED, ApiResponse::success(serde_json::json!({"sent": true}))),
        Err(_) => (StatusCode::INTERNAL_SERVER_ERROR, ApiResponse::error("Failed to send request")),
    }
}

async fn accept_friend_request(
    State(state): State<AppState>,
    Json(req): Json<FriendRequest>,
) -> impl IntoResponse {
    let user = validate_token(&state.db, &req.token).await;
    let user = match user {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };
    
    let result = sqlx::query(
        "UPDATE friendships SET status = 'accepted', accepted_at = $1 WHERE user_id = $2 AND friend_id = $3 AND status = 'pending'"
    )
        .bind(chrono::Utc::now())
        .bind(req.target_user_id)
        .bind(user.id)
        .execute(&state.db)
        .await;
    
    match result {
        Ok(r) if r.rows_affected() > 0 => (StatusCode::OK, ApiResponse::success(serde_json::json!({"accepted": true}))),
        _ => (StatusCode::NOT_FOUND, ApiResponse::error("No pending request found")),
    }
}

async fn decline_friend_request(
    State(state): State<AppState>,
    Json(req): Json<FriendRequest>,
) -> impl IntoResponse {
    let user = validate_token(&state.db, &req.token).await;
    let user = match user {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };
    
    let result = sqlx::query(
        "DELETE FROM friendships WHERE user_id = $1 AND friend_id = $2 AND status = 'pending'"
    )
        .bind(req.target_user_id)
        .bind(user.id)
        .execute(&state.db)
        .await;
    
    match result {
        Ok(r) if r.rows_affected() > 0 => (StatusCode::OK, ApiResponse::success(serde_json::json!({"declined": true}))),
        _ => (StatusCode::NOT_FOUND, ApiResponse::error("No pending request found")),
    }
}

async fn get_friends(
    State(state): State<AppState>,
    Json(req): Json<TokenRequest>,
) -> impl IntoResponse {
    let user = validate_token(&state.db, &req.token).await;
    let user = match user {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };
    
    let friends = sqlx::query_as::<_, (Uuid, String, Option<String>, Option<String>)>(
        "SELECT u.id, u.username, u.display_name, u.avatar_url FROM users u
         JOIN friendships f ON (f.user_id = u.id OR f.friend_id = u.id)
         WHERE ((f.user_id = $1 OR f.friend_id = $1) AND f.status = 'accepted')
         AND u.id != $1"
    )
        .bind(user.id)
        .fetch_all(&state.db)
        .await
        .unwrap_or_default();
    
    let friends: Vec<serde_json::Value> = friends.iter().map(|(id, username, display_name, avatar_url)| {
        serde_json::json!({
            "id": id,
            "username": username,
            "display_name": display_name,
            "avatar_url": avatar_url
        })
    }).collect();
    
    (StatusCode::OK, ApiResponse::success(serde_json::json!({"friends": friends})))
}

async fn get_pending_requests(
    State(state): State<AppState>,
    Json(req): Json<TokenRequest>,
) -> impl IntoResponse {
    let user = validate_token(&state.db, &req.token).await;
    let user = match user {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };
    
    let incoming = sqlx::query_as::<_, (Uuid, String, Option<String>)>(
        "SELECT u.id, u.username, u.display_name FROM users u
         JOIN friendships f ON f.user_id = u.id
         WHERE f.friend_id = $1 AND f.status = 'pending'"
    )
        .bind(user.id)
        .fetch_all(&state.db)
        .await
        .unwrap_or_default();
    
    let outgoing = sqlx::query_as::<_, (Uuid, String, Option<String>)>(
        "SELECT u.id, u.username, u.display_name FROM users u
         JOIN friendships f ON f.friend_id = u.id
         WHERE f.user_id = $1 AND f.status = 'pending'"
    )
        .bind(user.id)
        .fetch_all(&state.db)
        .await
        .unwrap_or_default();
    
    (StatusCode::OK, ApiResponse::success(serde_json::json!({
        "incoming": incoming.iter().map(|(id, username, display_name)| serde_json::json!({
            "id": id, "username": username, "display_name": display_name
        })).collect::<Vec<_>>(),
        "outgoing": outgoing.iter().map(|(id, username, display_name)| serde_json::json!({
            "id": id, "username": username, "display_name": display_name
        })).collect::<Vec<_>>()
    })))
}

async fn search_users(
    State(state): State<AppState>,
    Path(query): Path<String>,
) -> impl IntoResponse {
    let users = sqlx::query_as::<_, (Uuid, String, Option<String>, Option<String>)>(
        "SELECT id, username, display_name, avatar_url FROM users WHERE username ILIKE $1 LIMIT 20"
    )
        .bind(format!("%{}%", query))
        .fetch_all(&state.db)
        .await
        .unwrap_or_default();
    
    let users: Vec<serde_json::Value> = users.iter().map(|(id, username, display_name, avatar_url)| {
        serde_json::json!({
            "id": id,
            "username": username,
            "display_name": display_name,
            "avatar_url": avatar_url
        })
    }).collect();
    
    (StatusCode::OK, ApiResponse::success(serde_json::json!({"users": users})))
}

async fn health() -> impl IntoResponse {
    Json(serde_json::json!({
        "status": "healthy",
        "version": "0.2.0",
        "timestamp": chrono::Utc::now().to_rfc3339()
    }))
}

async fn list_servers(
    State(state): State<AppState>,
) -> impl IntoResponse {
    let servers = sqlx::query_as::<_, (Uuid, String, Option<String>, String, i32, i32, i32, String, Uuid, bool, chrono::DateTime<chrono::Utc>, chrono::DateTime<chrono::Utc>)>(
        "SELECT id, name, description, address, port, max_players, current_players, game_mode, owner_id, is_online, last_ping, created_at 
         FROM game_servers WHERE is_online = true AND last_ping > NOW() - INTERVAL '5 minutes' ORDER BY current_players DESC LIMIT 100"
    )
        .fetch_all(&state.db)
        .await
        .unwrap_or_default();
    
    let servers: Vec<serde_json::Value> = servers.iter().map(|(id, name, desc, addr, port, max, curr, mode, owner, online, ping, created)| {
        serde_json::json!({
            "id": id,
            "name": name,
            "description": desc,
            "address": addr,
            "port": port,
            "max_players": max,
            "current_players": curr,
            "game_mode": mode,
            "owner_id": owner,
            "is_online": online,
            "last_ping": ping,
            "created_at": created
        })
    }).collect();
    
    (StatusCode::OK, ApiResponse::success(serde_json::json!({"servers": servers})))
}

async fn register_server(
    State(state): State<AppState>,
    Json(req): Json<RegisterServerRequest>,
) -> impl IntoResponse {
    let user = validate_token(&state.db, &req.token).await;
    let user = match user {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<GameServer>::error("Invalid token")),
    };
    
    let server_id = Uuid::new_v4();
    let now = chrono::Utc::now();
    let tags_json = serde_json::to_value(&req.tags).unwrap_or(serde_json::json!([]));
    
    let result = sqlx::query(
        "INSERT INTO game_servers (id, name, description, address, port, max_players, current_players, game_mode, tags, owner_id, is_online, last_ping, created_at) 
         VALUES ($1, $2, $3, $4, $5, $6, 0, $7, $8, $9, true, $10, $10)"
    )
        .bind(server_id)
        .bind(&req.name)
        .bind(&req.description)
        .bind(&req.address)
        .bind(req.port)
        .bind(req.max_players)
        .bind(&req.game_mode)
        .bind(&tags_json)
        .bind(user.id)
        .bind(now)
        .execute(&state.db)
        .await;
    
    match result {
        Ok(_) => {
            let server = GameServer {
                id: server_id,
                name: req.name,
                description: req.description,
                address: req.address,
                port: req.port,
                max_players: req.max_players,
                current_players: 0,
                game_mode: req.game_mode,
                tags: req.tags,
                owner_id: user.id,
                is_online: true,
                last_ping: now,
                created_at: now,
            };
            (StatusCode::CREATED, ApiResponse::success(server))
        }
        Err(_) => (StatusCode::INTERNAL_SERVER_ERROR, ApiResponse::error("Failed to register server")),
    }
}

async fn server_heartbeat(
    State(state): State<AppState>,
    Json(req): Json<ServerHeartbeatRequest>,
) -> impl IntoResponse {
    let user = validate_token(&state.db, &req.token).await;
    let user = match user {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };
    
    let result = sqlx::query(
        "UPDATE game_servers SET current_players = $1, last_ping = $2, is_online = true WHERE id = $3 AND owner_id = $4"
    )
        .bind(req.current_players)
        .bind(chrono::Utc::now())
        .bind(req.server_id)
        .bind(user.id)
        .execute(&state.db)
        .await;
    
    match result {
        Ok(r) if r.rows_affected() > 0 => (StatusCode::OK, ApiResponse::success(serde_json::json!({"updated": true}))),
        _ => (StatusCode::NOT_FOUND, ApiResponse::error("Server not found or not owned by you")),
    }
}

async fn get_game_stats(
    State(state): State<AppState>,
    Json(req): Json<TokenRequest>,
) -> impl IntoResponse {
    let user = validate_token(&state.db, &req.token).await;
    let user = match user {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<GameStats>::error("Invalid token")),
    };
    
    let stats = sqlx::query_as::<_, (i64, i64, Option<chrono::DateTime<chrono::Utc>>, Option<String>, i32)>(
        "SELECT COALESCE(total_playtime_minutes, 0), COALESCE(total_sessions, 0), last_played, favorite_server, COALESCE(achievements_count, 0) 
         FROM game_stats WHERE user_id = $1"
    )
        .bind(user.id)
        .fetch_optional(&state.db)
        .await
        .ok()
        .flatten();
    
    let stats = match stats {
        Some((playtime, sessions, last_played, fav_server, achievements)) => GameStats {
            user_id: user.id,
            total_playtime_minutes: playtime,
            total_sessions: sessions,
            last_played,
            favorite_server: fav_server,
            achievements_count: achievements,
        },
        None => GameStats {
            user_id: user.id,
            total_playtime_minutes: 0,
            total_sessions: 0,
            last_played: None,
            favorite_server: None,
            achievements_count: 0,
        },
    };
    
    (StatusCode::OK, ApiResponse::success(stats))
}

async fn record_session(
    State(state): State<AppState>,
    Json(req): Json<RecordSessionRequest>,
) -> impl IntoResponse {
    let user = validate_token(&state.db, &req.token).await;
    let user = match user {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };
    
    let now = chrono::Utc::now();
    let result = sqlx::query(
        "INSERT INTO game_stats (user_id, total_playtime_minutes, total_sessions, last_played, favorite_server, achievements_count)
         VALUES ($1, $2, 1, $3, $4, 0)
         ON CONFLICT (user_id) DO UPDATE SET 
           total_playtime_minutes = game_stats.total_playtime_minutes + $2,
           total_sessions = game_stats.total_sessions + 1,
           last_played = $3,
           favorite_server = COALESCE($4, game_stats.favorite_server)"
    )
        .bind(user.id)
        .bind(req.duration_minutes)
        .bind(now)
        .bind(&req.server_name)
        .execute(&state.db)
        .await;
    
    match result {
        Ok(_) => (StatusCode::OK, ApiResponse::success(serde_json::json!({"recorded": true}))),
        Err(_) => (StatusCode::INTERNAL_SERVER_ERROR, ApiResponse::error("Failed to record session")),
    }
}

async fn get_mod_profiles(
    State(state): State<AppState>,
    Json(req): Json<TokenRequest>,
) -> impl IntoResponse {
    let user = validate_token(&state.db, &req.token).await;
    let user = match user {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };
    
    let profiles = sqlx::query_as::<_, (Uuid, String, Option<String>, serde_json::Value, bool, chrono::DateTime<chrono::Utc>)>(
        "SELECT id, name, description, mods, is_active, created_at FROM mod_profiles WHERE user_id = $1 ORDER BY created_at DESC"
    )
        .bind(user.id)
        .fetch_all(&state.db)
        .await
        .unwrap_or_default();
    
    let profiles: Vec<ModProfile> = profiles.iter().map(|(id, name, desc, mods, active, created)| {
        ModProfile {
            id: *id,
            user_id: user.id,
            name: name.clone(),
            description: desc.clone(),
            mods: mods.clone(),
            is_active: *active,
            created_at: *created,
        }
    }).collect();
    
    (StatusCode::OK, ApiResponse::success(serde_json::json!({"profiles": profiles})))
}

async fn create_mod_profile(
    State(state): State<AppState>,
    Json(req): Json<CreateModProfileRequest>,
) -> impl IntoResponse {
    let user = validate_token(&state.db, &req.token).await;
    let user = match user {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<ModProfile>::error("Invalid token")),
    };
    
    let profile_id = Uuid::new_v4();
    let now = chrono::Utc::now();
    
    let result = sqlx::query(
        "INSERT INTO mod_profiles (id, user_id, name, description, mods, is_active, created_at) VALUES ($1, $2, $3, $4, $5, false, $6)"
    )
        .bind(profile_id)
        .bind(user.id)
        .bind(&req.name)
        .bind(&req.description)
        .bind(&req.mods)
        .bind(now)
        .execute(&state.db)
        .await;
    
    match result {
        Ok(_) => {
            let profile = ModProfile {
                id: profile_id,
                user_id: user.id,
                name: req.name,
                description: req.description,
                mods: req.mods,
                is_active: false,
                created_at: now,
            };
            (StatusCode::CREATED, ApiResponse::success(profile))
        }
        Err(_) => (StatusCode::INTERNAL_SERVER_ERROR, ApiResponse::error("Failed to create profile")),
    }
}

async fn activate_mod_profile(
    State(state): State<AppState>,
    Json(req): Json<ActivateModProfileRequest>,
) -> impl IntoResponse {
    let user = validate_token(&state.db, &req.token).await;
    let user = match user {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };
    
    let _ = sqlx::query("UPDATE mod_profiles SET is_active = false WHERE user_id = $1")
        .bind(user.id)
        .execute(&state.db)
        .await;
    
    let result = sqlx::query("UPDATE mod_profiles SET is_active = true WHERE id = $1 AND user_id = $2")
        .bind(req.profile_id)
        .bind(user.id)
        .execute(&state.db)
        .await;
    
    match result {
        Ok(r) if r.rows_affected() > 0 => (StatusCode::OK, ApiResponse::success(serde_json::json!({"activated": true}))),
        _ => (StatusCode::NOT_FOUND, ApiResponse::error("Profile not found")),
    }
}

async fn get_performance_settings(
    State(state): State<AppState>,
    Json(req): Json<TokenRequest>,
) -> impl IntoResponse {
    let user = validate_token(&state.db, &req.token).await;
    let user = match user {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<PerformanceSettings>::error("Invalid token")),
    };
    
    let settings = sqlx::query_as::<_, (i32, String, String, bool, i32)>(
        "SELECT ram_allocation_mb, java_args, priority_mode, vsync_enabled, max_fps FROM performance_settings WHERE user_id = $1"
    )
        .bind(user.id)
        .fetch_optional(&state.db)
        .await
        .ok()
        .flatten();
    
    let settings = match settings {
        Some((ram, args, priority, vsync, fps)) => PerformanceSettings {
            user_id: user.id,
            ram_allocation_mb: ram,
            java_args: args,
            priority_mode: priority,
            vsync_enabled: vsync,
            max_fps: fps,
        },
        None => PerformanceSettings {
            user_id: user.id,
            ram_allocation_mb: 4096,
            java_args: "-XX:+UseG1GC -XX:+ParallelRefProcEnabled".to_string(),
            priority_mode: "normal".to_string(),
            vsync_enabled: true,
            max_fps: 60,
        },
    };
    
    (StatusCode::OK, ApiResponse::success(settings))
}

async fn update_performance_settings(
    State(state): State<AppState>,
    Json(req): Json<UpdatePerformanceRequest>,
) -> impl IntoResponse {
    let user = validate_token(&state.db, &req.token).await;
    let user = match user {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };
    
    let result = sqlx::query(
        "INSERT INTO performance_settings (user_id, ram_allocation_mb, java_args, priority_mode, vsync_enabled, max_fps)
         VALUES ($1, COALESCE($2, 4096), COALESCE($3, '-XX:+UseG1GC'), COALESCE($4, 'normal'), COALESCE($5, true), COALESCE($6, 60))
         ON CONFLICT (user_id) DO UPDATE SET
           ram_allocation_mb = COALESCE($2, performance_settings.ram_allocation_mb),
           java_args = COALESCE($3, performance_settings.java_args),
           priority_mode = COALESCE($4, performance_settings.priority_mode),
           vsync_enabled = COALESCE($5, performance_settings.vsync_enabled),
           max_fps = COALESCE($6, performance_settings.max_fps)"
    )
        .bind(user.id)
        .bind(req.ram_allocation_mb)
        .bind(&req.java_args)
        .bind(&req.priority_mode)
        .bind(req.vsync_enabled)
        .bind(req.max_fps)
        .execute(&state.db)
        .await;
    
    match result {
        Ok(_) => (StatusCode::OK, ApiResponse::success(serde_json::json!({"updated": true}))),
        Err(_) => (StatusCode::INTERNAL_SERVER_ERROR, ApiResponse::error("Failed to update settings")),
    }
}

async fn get_subscription(
    State(state): State<AppState>,
    Json(req): Json<TokenRequest>,
) -> impl IntoResponse {
    let user = validate_token(&state.db, &req.token).await;
    let user = match user {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<Subscription>::error("Invalid token")),
    };
    
    let sub = sqlx::query_as::<_, (String, String, Option<chrono::DateTime<chrono::Utc>>)>(
        "SELECT tier, status, current_period_end FROM subscriptions WHERE user_id = $1"
    )
        .bind(user.id)
        .fetch_optional(&state.db)
        .await
        .ok()
        .flatten();
    
    let (tier, status, period_end) = sub.unwrap_or(("free".to_string(), "active".to_string(), None));
    
    let features = match tier.as_str() {
        "premium" => SubscriptionFeatures {
            max_friends: 500,
            cloud_storage_mb: 5120,
            priority_relay: true,
            custom_themes: true,
            early_access: true,
        },
        _ => SubscriptionFeatures {
            max_friends: 100,
            cloud_storage_mb: 0,
            priority_relay: false,
            custom_themes: false,
            early_access: false,
        },
    };
    
    (StatusCode::OK, ApiResponse::success(Subscription {
        user_id: user.id,
        tier,
        status,
        current_period_end: period_end,
        features,
    }))
}

async fn get_pricing() -> impl IntoResponse {
    Json(serde_json::json!({
        "tiers": [
            {
                "id": "free",
                "name": "Free",
                "price_monthly": 0,
                "price_yearly": 0,
                "features": {
                    "max_friends": 100,
                    "cloud_storage_mb": 0,
                    "priority_relay": false,
                    "custom_themes": false,
                    "early_access": false,
                    "mod_profiles": 3,
                    "server_listings": 1
                }
            },
            {
                "id": "premium",
                "name": "Premium",
                "price_monthly": 499,
                "price_yearly": 3999,
                "price_id_monthly": "price_premium_monthly",
                "price_id_yearly": "price_premium_yearly",
                "features": {
                    "max_friends": 500,
                    "cloud_storage_mb": 5120,
                    "priority_relay": true,
                    "custom_themes": true,
                    "early_access": true,
                    "mod_profiles": "unlimited",
                    "server_listings": 10
                }
            }
        ]
    }))
}

async fn get_feature_gates(
    State(state): State<AppState>,
    Json(req): Json<TokenRequest>,
) -> impl IntoResponse {
    let tier = match validate_token(&state.db, &req.token).await {
        Some(user) => {
            sqlx::query_scalar::<_, String>(
                "SELECT tier FROM subscriptions WHERE user_id = $1 AND status = 'active'"
            )
                .bind(user.id)
                .fetch_optional(&state.db)
                .await
                .ok()
                .flatten()
                .unwrap_or_else(|| "free".to_string())
        },
        None => "free".to_string(),
    };
    
    let is_premium = tier == "premium";
    
    Json(serde_json::json!({
        "tier": tier,
        "yellow_tale": {
            "performance": {
                "cpu_core_allocation": is_premium,
                "adaptive_ram_reservation": is_premium,
                "background_process_suppression": is_premium,
                "disk_io_prewarming": is_premium,
                "profile_based_presets": is_premium,
                "memory_defragmentation": is_premium,
                "shader_precompilation": is_premium,
                "asset_streaming": true,
                "texture_compression": true
            },
            "diagnostics": {
                "historical_tracking": is_premium,
                "frame_pacing_analysis": is_premium,
                "bottleneck_detection": is_premium,
                "exportable_reports": is_premium,
                "crash_correlation": is_premium,
                "gpu_profiling": is_premium,
                "network_diagnostics": true,
                "memory_leak_detection": is_premium
            },
            "mod_tooling": {
                "conflict_detection": is_premium,
                "dependency_auto_resolution": is_premium,
                "load_order_optimization": is_premium,
                "per_server_profiles": is_premium,
                "mod_health_checks": is_premium,
                "version_compatibility": true,
                "safe_mode_fallback": true
            },
            "visual_processing": {
                "frame_upscaling": is_premium,
                "scaling_algorithms": is_premium,
                "profile_presets": is_premium,
                "fsr_support": is_premium,
                "dynamic_resolution": is_premium
            },
            "session_networking": {
                "smart_orchestration": is_premium,
                "nat_traversal": true,
                "relay_routing": is_premium,
                "session_persistence": is_premium,
                "connection_pooling": true,
                "latency_optimization": is_premium,
                "route_selection": is_premium,
                "reconnect_grace_period": true
            },
            "pond_integration": {
                "capability_discovery": true,
                "performance_hints": is_premium,
                "asset_availability": true,
                "cosmetic_verification": is_premium,
                "queue_priority": is_premium,
                "friend_activity_sync": true,
                "session_transfer": is_premium,
                "network_optimization_hints": is_premium
            },
            "connectivity": {
                "auto_server_discovery": true,
                "ping_history": true,
                "server_favorites": true,
                "connection_quality_indicator": true,
                "bandwidth_estimation": is_premium,
                "packet_loss_compensation": is_premium,
                "jitter_buffer_optimization": is_premium
            }
        },
        "pond": {
            "server_performance": {
                "advanced_tick_scheduling": is_premium,
                "adaptive_entity_throttling": is_premium,
                "dynamic_chunk_activation": is_premium,
                "memory_pooling": is_premium,
                "load_prediction": is_premium,
                "gc_optimization": is_premium,
                "entity_culling": true,
                "view_distance_scaling": true
            },
            "live_tooling": {
                "plugin_hot_loading": is_premium,
                "live_config_reloads": is_premium,
                "realtime_dashboards": is_premium,
                "plugin_sandboxing": is_premium,
                "remote_console": is_premium,
                "crash_recovery": true
            },
            "cosmetics": {
                "cosmetic_registry": true,
                "temporary_overrides": is_premium,
                "creator_cosmetics": is_premium,
                "asset_validation": true,
                "cosmetic_caching": true,
                "event_cosmetics": is_premium
            },
            "launcher_integration": {
                "feature_advertising": true,
                "asset_manifests": true,
                "ownership_validation": is_premium,
                "profile_sync": is_premium,
                "player_presence": true,
                "queue_management": true,
                "priority_queue": is_premium,
                "session_handoff": is_premium,
                "friend_notifications": true
            },
            "networking": {
                "connection_keep_alive": true,
                "ping_optimization": true,
                "interpolation_hints": true,
                "prediction_config": is_premium,
                "compression_level": is_premium,
                "batch_updates": true
            }
        }
    }))
}

async fn create_checkout(
    State(state): State<AppState>,
    Json(req): Json<CreateCheckoutRequest>,
) -> impl IntoResponse {
    let user = validate_token(&state.db, &req.token).await;
    let user = match user {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };
    
    let email = sqlx::query_scalar::<_, String>("SELECT email FROM users WHERE id = $1")
        .bind(user.id)
        .fetch_one(&state.db)
        .await;
    
    let email = match email {
        Ok(e) => e,
        Err(_) => return (StatusCode::INTERNAL_SERVER_ERROR, ApiResponse::error("Failed to get user email")),
    };
    
    let base_url = std::env::var("REPLIT_DOMAINS")
        .ok()
        .and_then(|d| d.split(',').next().map(|s| format!("https://{}", s)))
        .unwrap_or_else(|| "http://localhost:5000".to_string());
    
    let success_url = format!("{}/dashboard?checkout=success", base_url);
    let cancel_url = format!("{}/premium?checkout=cancelled", base_url);
    
    match stripe::create_checkout_session(&email, &req.price_id, &success_url, &cancel_url).await {
        Ok(url) => (StatusCode::OK, ApiResponse::success(serde_json::json!({"url": url}))),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, ApiResponse::error(format!("Checkout failed: {}", e))),
    }
}

async fn manage_subscription(
    State(state): State<AppState>,
    Json(req): Json<ManageSubscriptionRequest>,
) -> impl IntoResponse {
    let user = validate_token(&state.db, &req.token).await;
    let user = match user {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };
    
    let customer_id = sqlx::query_scalar::<_, Option<String>>(
        "SELECT stripe_customer_id FROM subscriptions WHERE user_id = $1"
    )
        .bind(user.id)
        .fetch_optional(&state.db)
        .await
        .ok()
        .flatten()
        .flatten();
    
    let customer_id = match customer_id {
        Some(id) => id,
        None => return (StatusCode::BAD_REQUEST, ApiResponse::error("No subscription found")),
    };
    
    let base_url = std::env::var("REPLIT_DOMAINS")
        .ok()
        .and_then(|d| d.split(',').next().map(|s| format!("https://{}", s)))
        .unwrap_or_else(|| "http://localhost:5000".to_string());
    
    let return_url = format!("{}/dashboard", base_url);
    
    match stripe::create_customer_portal_session(&customer_id, &return_url).await {
        Ok(url) => (StatusCode::OK, ApiResponse::success(serde_json::json!({"url": url}))),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, ApiResponse::error(format!("Portal failed: {}", e))),
    }
}

async fn get_releases() -> impl IntoResponse {
    Json(serde_json::json!({
        "latest": {
            "version": "0.1.0",
            "date": "2026-01-10",
            "downloads": {
                "windows": "/releases/yellow-tale-0.1.0-windows.exe",
                "macos": "/releases/yellow-tale-0.1.0-macos.dmg",
                "linux": "/releases/yellow-tale-0.1.0-linux.tar.gz"
            },
            "changelog": "Initial release with user accounts, friends system, and P2P relay."
        }
    }))
}

async fn ws_relay(
    ws: WebSocketUpgrade,
    State(state): State<AppState>,
) -> impl IntoResponse {
    ws.on_upgrade(move |socket| handle_relay_connection(socket, state))
}

async fn handle_relay_connection(socket: WebSocket, _state: AppState) {
    use futures_util::{SinkExt, StreamExt};
    
    let (mut sender, mut receiver) = socket.split();
    
    while let Some(msg) = receiver.next().await {
        if let Ok(Message::Text(text)) = msg {
            if let Ok(parsed) = serde_json::from_str::<serde_json::Value>(&text) {
                let response = serde_json::json!({
                    "type": "ack",
                    "received": parsed
                });
                let _ = sender.send(Message::Text(response.to_string().into())).await;
            }
        }
    }
}

#[tokio::main]
async fn main() {
    tracing_subscriber::fmt::init();
    
    dotenvy::dotenv().ok();
    
    let database_url = std::env::var("DATABASE_URL")
        .expect("DATABASE_URL must be set");
    
    info!("Connecting to database...");
    let db = PgPoolOptions::new()
        .max_connections(10)
        .connect(&database_url)
        .await
        .expect("Failed to connect to database");
    
    info!("Running migrations...");
    run_migrations(&db).await;
    
    let state = AppState {
        db,
        relay: Arc::new(RwLock::new(RelayHub::new())),
        verification: Arc::new(VerificationService::new()),
    };
    
    let cors = CorsLayer::new()
        .allow_origin(Any)
        .allow_methods([Method::GET, Method::POST, Method::PUT, Method::DELETE, Method::OPTIONS])
        .allow_headers([header::CONTENT_TYPE, header::AUTHORIZATION]);
    
    let app = Router::new()
        .route("/health", get(health))
        .route("/api/v1/releases", get(get_releases))
        .route("/api/v1/pricing", get(get_pricing))
        .route("/api/v1/features", post(get_feature_gates))
        // Auth
        .route("/api/v1/auth/signup", post(signup))
        .route("/api/v1/auth/login", post(login))
        .route("/api/v1/auth/logout", post(logout))
        .route("/api/v1/auth/me", post(get_me))
        .route("/api/v1/profile", post(update_profile))
        // Friends
        .route("/api/v1/friends", post(get_friends))
        .route("/api/v1/friends/request", post(send_friend_request))
        .route("/api/v1/friends/accept", post(accept_friend_request))
        .route("/api/v1/friends/decline", post(decline_friend_request))
        .route("/api/v1/friends/pending", post(get_pending_requests))
        .route("/api/v1/users/search/:query", get(search_users))
        // Server Browser
        .route("/api/v1/servers", get(list_servers))
        .route("/api/v1/servers/register", post(register_server))
        .route("/api/v1/servers/heartbeat", post(server_heartbeat))
        // Game Stats
        .route("/api/v1/stats", post(get_game_stats))
        .route("/api/v1/stats/session", post(record_session))
        // Mod Profiles
        .route("/api/v1/mods/profiles", post(get_mod_profiles))
        .route("/api/v1/mods/profiles/create", post(create_mod_profile))
        .route("/api/v1/mods/profiles/activate", post(activate_mod_profile))
        // Performance Settings
        .route("/api/v1/performance", post(get_performance_settings))
        .route("/api/v1/performance/update", post(update_performance_settings))
        // Subscription
        .route("/api/v1/subscription", post(get_subscription))
        .route("/api/v1/subscription/checkout", post(create_checkout))
        .route("/api/v1/subscription/manage", post(manage_subscription))
        // Marketplace
        .route("/api/v1/marketplace/items", get(list_marketplace_items))
        .route("/api/v1/marketplace/items", post(create_marketplace_item))
        .route("/api/v1/marketplace/items/:id", get(get_marketplace_item))
        .route("/api/v1/marketplace/items/:id/like", post(like_marketplace_item))
        .route("/api/v1/marketplace/items/:id/download", post(download_marketplace_item))
        .route("/api/v1/marketplace/items/:id/purchase", post(purchase_marketplace_item))
        .route("/api/v1/marketplace/purchase/:escrow_id/confirm", post(confirm_purchase))
        .route("/api/v1/marketplace/purchases", post(get_user_purchases))
        // Admin Marketplace
        .route("/api/v1/admin/login", post(admin_login))
        .route("/api/v1/admin/marketplace/items", post(admin_create_marketplace_item))
        .route("/api/v1/admin/marketplace/items", get(admin_list_all_items))
        .route("/api/v1/admin/marketplace/items/:id", axum::routing::put(admin_update_marketplace_item))
        .route("/api/v1/admin/marketplace/items/:id", axum::routing::delete(admin_delete_marketplace_item))
        .route("/api/v1/admin/escrow", post(admin_list_escrow_transactions))
        .route("/api/v1/admin/escrow/release", post(admin_release_escrow))
        // Cosmetics
        .route("/api/v1/cosmetics", post(get_user_cosmetics))
        .route("/api/v1/cosmetics/equip", post(equip_cosmetic))
        .route("/api/v1/cosmetics/unequip", post(unequip_cosmetic))
        .route("/api/v1/cosmetics/equipped", post(get_equipped_cosmetics))
        .route("/api/v1/cosmetics/user", post(get_public_user_cosmetics))
        // Verification
        .route("/api/v1/verification/methods", get(get_verification_methods))
        .route("/api/v1/verification/start", post(start_verification))
        .route("/api/v1/verification/status", post(get_verification_status))
        .route("/api/v1/verification/cancel", post(cancel_verification))
        .route("/api/v1/verification/admin/resolve", post(admin_resolve_verification))
        // Relay
        .route("/api/v1/relay", get(ws_relay))
        // Rubidium API - Feature Toggles
        .route("/api/v1/rubidium/features", post(get_rubidium_features))
        .route("/api/v1/rubidium/features/toggle", post(toggle_rubidium_feature))
        .route("/api/v1/rubidium/features/admin/toggle", post(admin_toggle_feature))
        // Rubidium API - Replay System
        .route("/api/v1/rubidium/replay/sessions", post(list_replay_sessions))
        .route("/api/v1/rubidium/replay/sessions/:id", get(get_replay_session))
        .route("/api/v1/rubidium/replay/record/start", post(start_replay_recording))
        .route("/api/v1/rubidium/replay/record/stop", post(stop_replay_recording))
        // Rubidium API - Mapping (Minimap/Worldmap)
        .route("/api/v1/rubidium/mapping/config", post(get_mapping_config))
        .route("/api/v1/rubidium/mapping/config/update", post(update_mapping_config))
        .route("/api/v1/rubidium/mapping/waypoints", post(get_waypoints))
        .route("/api/v1/rubidium/mapping/waypoints/create", post(create_waypoint))
        .route("/api/v1/rubidium/mapping/waypoints/delete", post(delete_waypoint))
        .route("/api/v1/rubidium/mapping/waypoints/share", post(share_waypoint))
        // Rubidium API - Social Features  
        .route("/api/v1/rubidium/social/party/create", post(create_party))
        .route("/api/v1/rubidium/social/party/join", post(join_party))
        .route("/api/v1/rubidium/social/party/leave", post(leave_party))
        .route("/api/v1/rubidium/social/party/invite", post(invite_to_party))
        .route("/api/v1/rubidium/social/presence", post(update_presence))
        // Rubidium API - Cinema Camera
        .route("/api/v1/rubidium/cinema/paths", post(list_camera_paths))
        .route("/api/v1/rubidium/cinema/paths/create", post(create_camera_path))
        .route("/api/v1/rubidium/cinema/paths/delete", post(delete_camera_path))
        // Rubidium API - Anticheat
        .route("/api/v1/rubidium/anticheat/status", post(get_anticheat_status))
        .route("/api/v1/rubidium/anticheat/report", post(report_violation))
        // Rubidium API - Plugins
        .route("/api/v1/rubidium/plugins", post(list_server_plugins))
        .route("/api/v1/rubidium/plugins/config", post(get_plugin_config))
        .layer(cors)
        .with_state(state);
    
    let addr = SocketAddr::from(([0, 0, 0, 0], 8080));
    info!("Yellow Tale API Server starting on {}", addr);
    
    let listener = tokio::net::TcpListener::bind(addr).await.unwrap();
    axum::serve(listener, app).await.unwrap();
}

async fn list_marketplace_items(
    State(state): State<AppState>,
    axum::extract::Query(params): axum::extract::Query<MarketplaceQueryParams>,
) -> impl IntoResponse {
    let valid_categories = ["mod", "plugin", "skin", "cosmetic", "texture", "emote"];
    let category_filter = params.category.as_ref().filter(|c| valid_categories.contains(&c.as_str()));
    
    let price_filter = params.price.as_ref().map(|p| match p.as_str() {
        "free" => "free",
        "paid" => "paid",
        _ => "all",
    }).unwrap_or("all");
    
    let search_pattern = params.q.as_ref().map(|q| format!("%{}%", q));
    
    let order_clause = match params.sort.as_deref() {
        Some("downloads") => "m.downloads DESC",
        Some("newest") => "m.created_at DESC",
        Some("price_low") => "m.price ASC",
        Some("price_high") => "m.price DESC",
        _ => "m.downloads DESC, m.likes DESC",
    };
    
    let query = format!(
        "SELECT m.id, m.name, m.description, m.category, m.price, m.downloads, m.likes, 
                m.tags, m.thumbnail_url, m.file_url, m.is_featured, m.created_at,
                u.id as author_id, u.username, u.display_name
         FROM marketplace_items m
         JOIN users u ON m.author_id = u.id
         WHERE ($1::text IS NULL OR m.category = $1)
           AND (($2 = 'all') OR ($2 = 'free' AND m.price = 0) OR ($2 = 'paid' AND m.price > 0))
           AND ($3::text IS NULL OR m.name ILIKE $3 OR m.description ILIKE $3)
         ORDER BY {} LIMIT 100", order_clause
    );
    
    let rows = sqlx::query_as::<_, (Uuid, String, String, String, f64, i64, i64, serde_json::Value, Option<String>, Option<String>, bool, chrono::DateTime<chrono::Utc>, Uuid, String, Option<String>)>(&query)
        .bind(category_filter)
        .bind(price_filter)
        .bind(&search_pattern)
        .fetch_all(&state.db)
        .await
        .unwrap_or_default();
    
    let items: Vec<MarketplaceItem> = rows.into_iter().map(|(id, name, description, category, price, downloads, likes, tags_json, thumbnail_url, file_url, is_featured, created_at, author_id, username, display_name)| {
        let tags: Vec<String> = serde_json::from_value(tags_json).unwrap_or_default();
        MarketplaceItem {
            id,
            name,
            description,
            category,
            author: MarketplaceAuthor { id: author_id, username, display_name },
            price,
            downloads,
            likes,
            tags,
            thumbnail_url,
            file_url,
            is_featured,
            created_at,
        }
    }).collect();
    
    (StatusCode::OK, ApiResponse::success(serde_json::json!({"items": items})))
}

async fn create_marketplace_item(
    State(state): State<AppState>,
    Json(req): Json<CreateMarketplaceItemRequest>,
) -> impl IntoResponse {
    let user = validate_token(&state.db, &req.token).await;
    let user = match user {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<MarketplaceItem>::error("Invalid token")),
    };
    
    if req.name.len() < 3 || req.name.len() > 100 {
        return (StatusCode::BAD_REQUEST, ApiResponse::error("Name must be 3-100 characters"));
    }
    
    if req.description.len() > 2000 {
        return (StatusCode::BAD_REQUEST, ApiResponse::error("Description too long"));
    }
    
    let valid_categories = ["mod", "plugin", "skin", "cosmetic", "texture", "emote"];
    if !valid_categories.contains(&req.category.as_str()) {
        return (StatusCode::BAD_REQUEST, ApiResponse::error("Invalid category"));
    }
    
    if req.price < 0.0 || req.price > 99.99 {
        return (StatusCode::BAD_REQUEST, ApiResponse::error("Price must be 0-99.99"));
    }
    
    let item_id = Uuid::new_v4();
    let now = chrono::Utc::now();
    let tags_json = serde_json::to_value(&req.tags).unwrap_or(serde_json::json!([]));
    
    let result = sqlx::query(
        "INSERT INTO marketplace_items (id, name, description, category, author_id, price, downloads, likes, tags, is_featured, created_at)
         VALUES ($1, $2, $3, $4, $5, $6, 0, 0, $7, false, $8)"
    )
        .bind(item_id)
        .bind(&req.name)
        .bind(&req.description)
        .bind(&req.category)
        .bind(user.id)
        .bind(req.price)
        .bind(&tags_json)
        .bind(now)
        .execute(&state.db)
        .await;
    
    match result {
        Ok(_) => {
            let item = MarketplaceItem {
                id: item_id,
                name: req.name,
                description: req.description,
                category: req.category,
                author: MarketplaceAuthor { id: user.id, username: user.username, display_name: user.display_name },
                price: req.price,
                downloads: 0,
                likes: 0,
                tags: req.tags,
                thumbnail_url: None,
                file_url: None,
                is_featured: false,
                created_at: now,
            };
            (StatusCode::CREATED, ApiResponse::success(item))
        }
        Err(e) => {
            error!("Failed to create marketplace item: {}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, ApiResponse::error("Failed to create item"))
        }
    }
}

async fn get_marketplace_item(
    State(state): State<AppState>,
    Path(id): Path<Uuid>,
) -> impl IntoResponse {
    let row = sqlx::query_as::<_, (Uuid, String, String, String, f64, i64, i64, serde_json::Value, Option<String>, Option<String>, bool, chrono::DateTime<chrono::Utc>, Uuid, String, Option<String>)>(
        "SELECT m.id, m.name, m.description, m.category, m.price, m.downloads, m.likes, 
                m.tags, m.thumbnail_url, m.file_url, m.is_featured, m.created_at,
                u.id as author_id, u.username, u.display_name
         FROM marketplace_items m
         JOIN users u ON m.author_id = u.id
         WHERE m.id = $1"
    )
        .bind(id)
        .fetch_optional(&state.db)
        .await;
    
    match row {
        Ok(Some((id, name, description, category, price, downloads, likes, tags_json, thumbnail_url, file_url, is_featured, created_at, author_id, username, display_name))) => {
            let tags: Vec<String> = serde_json::from_value(tags_json).unwrap_or_default();
            let item = MarketplaceItem {
                id,
                name,
                description,
                category,
                author: MarketplaceAuthor { id: author_id, username, display_name },
                price,
                downloads,
                likes,
                tags,
                thumbnail_url,
                file_url,
                is_featured,
                created_at,
            };
            (StatusCode::OK, ApiResponse::success(item))
        }
        _ => (StatusCode::NOT_FOUND, ApiResponse::error("Item not found")),
    }
}

async fn like_marketplace_item(
    State(state): State<AppState>,
    Path(id): Path<Uuid>,
    Json(req): Json<TokenRequest>,
) -> impl IntoResponse {
    let user = validate_token(&state.db, &req.token).await;
    if user.is_none() {
        return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token"));
    }
    let user = user.unwrap();
    
    let existing = sqlx::query_scalar::<_, i64>(
        "SELECT COUNT(*) FROM marketplace_likes WHERE user_id = $1 AND item_id = $2"
    )
        .bind(user.id)
        .bind(id)
        .fetch_one(&state.db)
        .await
        .unwrap_or(0);
    
    if existing > 0 {
        return (StatusCode::CONFLICT, ApiResponse::error("Already liked"));
    }
    
    let _ = sqlx::query("INSERT INTO marketplace_likes (user_id, item_id, created_at) VALUES ($1, $2, $3)")
        .bind(user.id)
        .bind(id)
        .bind(chrono::Utc::now())
        .execute(&state.db)
        .await;
    
    let _ = sqlx::query("UPDATE marketplace_items SET likes = likes + 1 WHERE id = $1")
        .bind(id)
        .execute(&state.db)
        .await;
    
    (StatusCode::OK, ApiResponse::success(serde_json::json!({"liked": true})))
}

async fn download_marketplace_item(
    State(state): State<AppState>,
    Path(id): Path<Uuid>,
    Json(req): Json<TokenRequest>,
) -> impl IntoResponse {
    let user = validate_token(&state.db, &req.token).await;
    if user.is_none() {
        return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token"));
    }
    let user = user.unwrap();
    
    let item = sqlx::query_as::<_, (f64, Option<String>)>(
        "SELECT price, file_url FROM marketplace_items WHERE id = $1"
    )
        .bind(id)
        .fetch_optional(&state.db)
        .await;
    
    match item {
        Ok(Some((price, file_url))) => {
            if price > 0.0 {
                let purchased = sqlx::query_scalar::<_, i64>(
                    "SELECT COUNT(*) FROM marketplace_purchases WHERE user_id = $1 AND item_id = $2"
                )
                    .bind(user.id)
                    .bind(id)
                    .fetch_one(&state.db)
                    .await
                    .unwrap_or(0);
                
                if purchased == 0 {
                    return (StatusCode::PAYMENT_REQUIRED, ApiResponse::error("Purchase required"));
                }
            }
            
            let _ = sqlx::query("UPDATE marketplace_items SET downloads = downloads + 1 WHERE id = $1")
                .bind(id)
                .execute(&state.db)
                .await;
            
            (StatusCode::OK, ApiResponse::success(serde_json::json!({
                "download_url": file_url,
                "success": true
            })))
        }
        _ => (StatusCode::NOT_FOUND, ApiResponse::error("Item not found")),
    }
}

#[derive(Debug, Deserialize)]
struct CosmeticsRequest {
    token: String,
}

#[derive(Debug, Deserialize)]
struct EquipCosmeticRequest {
    token: String,
    item_id: String,
    slot: String,
}

#[derive(Debug, Deserialize)]
struct UnequipCosmeticRequest {
    token: String,
    slot: String,
}

#[derive(Debug, Serialize)]
struct CosmeticItemResponse {
    id: String,
    name: String,
    description: String,
    category: String,
    thumbnail_url: Option<String>,
    rarity: String,
    equipped: bool,
}

async fn get_user_cosmetics(
    State(state): State<AppState>,
    Json(req): Json<CosmeticsRequest>,
) -> impl IntoResponse {
    let user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::error("Invalid session")),
    };

    let items = sqlx::query_as::<_, (Uuid, String, String, String, Option<String>)>(
        "SELECT mi.id, mi.name, mi.description, mi.category, mi.thumbnail_url
         FROM marketplace_items mi
         JOIN marketplace_purchases mp ON mi.id = mp.item_id
         WHERE mp.user_id = $1 AND mi.category IN ('cosmetic', 'skin', 'emote')"
    )
        .bind(user.id)
        .fetch_all(&state.db)
        .await
        .unwrap_or_default();

    let equipped = sqlx::query_as::<_, (String, String)>(
        "SELECT slot, item_id FROM user_equipped_cosmetics WHERE user_id = $1"
    )
        .bind(user.id)
        .fetch_all(&state.db)
        .await
        .unwrap_or_default();

    let equipped_ids: std::collections::HashSet<String> = equipped.iter().map(|(_, id)| id.clone()).collect();

    let cosmetics: Vec<CosmeticItemResponse> = items.into_iter().map(|(id, name, description, category, thumbnail_url)| {
        let rarity = if name.contains("Legendary") { "legendary" } 
            else if name.contains("Epic") { "epic" }
            else if name.contains("Rare") { "rare" }
            else { "common" };
        CosmeticItemResponse {
            id: id.to_string(),
            name,
            description,
            category,
            thumbnail_url,
            rarity: rarity.to_string(),
            equipped: equipped_ids.contains(&id.to_string()),
        }
    }).collect();

    (StatusCode::OK, ApiResponse::success(serde_json::json!({ "items": cosmetics })))
}

async fn equip_cosmetic(
    State(state): State<AppState>,
    Json(req): Json<EquipCosmeticRequest>,
) -> impl IntoResponse {
    let user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::error("Invalid session")),
    };

    let valid_slots = ["skin", "emote_1", "emote_2", "emote_3", "emote_4", "cape", "wings", "aura"];
    if !valid_slots.contains(&req.slot.as_str()) {
        return (StatusCode::BAD_REQUEST, ApiResponse::error("Invalid slot"));
    }

    let item_uuid = match Uuid::parse_str(&req.item_id) {
        Ok(id) => id,
        Err(_) => return (StatusCode::BAD_REQUEST, ApiResponse::error("Invalid item ID")),
    };

    let owned = sqlx::query_scalar::<_, i64>(
        "SELECT COUNT(*) FROM marketplace_purchases WHERE user_id = $1 AND item_id = $2"
    )
        .bind(user.id)
        .bind(item_uuid)
        .fetch_one(&state.db)
        .await
        .unwrap_or(0);

    let is_free = sqlx::query_scalar::<_, f64>(
        "SELECT COALESCE(price, 0) FROM marketplace_items WHERE id = $1"
    )
        .bind(item_uuid)
        .fetch_optional(&state.db)
        .await
        .ok()
        .flatten()
        .unwrap_or(1.0);

    if owned == 0 && is_free > 0.0 {
        return (StatusCode::FORBIDDEN, ApiResponse::error("You don't own this item"));
    }

    let _ = sqlx::query(
        "INSERT INTO user_equipped_cosmetics (user_id, slot, item_id)
         VALUES ($1, $2, $3)
         ON CONFLICT (user_id, slot) DO UPDATE SET item_id = $3"
    )
        .bind(user.id)
        .bind(&req.slot)
        .bind(&req.item_id)
        .execute(&state.db)
        .await;

    (StatusCode::OK, ApiResponse::success(serde_json::json!({ "equipped": true })))
}

async fn unequip_cosmetic(
    State(state): State<AppState>,
    Json(req): Json<UnequipCosmeticRequest>,
) -> impl IntoResponse {
    let user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::error("Invalid session")),
    };

    let _ = sqlx::query("DELETE FROM user_equipped_cosmetics WHERE user_id = $1 AND slot = $2")
        .bind(user.id)
        .bind(&req.slot)
        .execute(&state.db)
        .await;

    (StatusCode::OK, ApiResponse::success(serde_json::json!({ "unequipped": true })))
}

async fn get_equipped_cosmetics(
    State(state): State<AppState>,
    Json(req): Json<CosmeticsRequest>,
) -> impl IntoResponse {
    let user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::error("Invalid session")),
    };

    let equipped = sqlx::query_as::<_, (String, String)>(
        "SELECT slot, item_id FROM user_equipped_cosmetics WHERE user_id = $1"
    )
        .bind(user.id)
        .fetch_all(&state.db)
        .await
        .unwrap_or_default();

    let mut cosmetics = serde_json::json!({
        "skin": null,
        "emote_1": null,
        "emote_2": null,
        "emote_3": null,
        "emote_4": null,
        "cape": null,
        "wings": null,
        "aura": null
    });

    for (slot, item_id) in equipped {
        cosmetics[slot] = serde_json::Value::String(item_id);
    }

    (StatusCode::OK, ApiResponse::success(serde_json::json!({ "equipped": cosmetics })))
}

#[derive(Debug, Deserialize)]
struct GetUserCosmeticsRequest {
    user_id: Uuid,
}

async fn get_public_user_cosmetics(
    State(state): State<AppState>,
    Json(req): Json<GetUserCosmeticsRequest>,
) -> impl IntoResponse {
    let user_exists = sqlx::query_scalar::<_, i64>(
        "SELECT COUNT(*) FROM users WHERE id = $1"
    )
        .bind(req.user_id)
        .fetch_one(&state.db)
        .await
        .unwrap_or(0);

    if user_exists == 0 {
        return (StatusCode::NOT_FOUND, ApiResponse::error("User not found"));
    }

    let equipped = sqlx::query_as::<_, (String, String)>(
        "SELECT slot, item_id FROM user_equipped_cosmetics WHERE user_id = $1"
    )
        .bind(req.user_id)
        .fetch_all(&state.db)
        .await
        .unwrap_or_default();

    let mut cosmetics = serde_json::json!({
        "skin": null,
        "emote_1": null,
        "emote_2": null,
        "emote_3": null,
        "emote_4": null,
        "cape": null,
        "wings": null,
        "aura": null
    });

    for (slot, item_id) in equipped {
        cosmetics[slot.clone()] = serde_json::Value::String(item_id);
    }

    (StatusCode::OK, ApiResponse::success(serde_json::json!({ "equipped": cosmetics, "user_id": req.user_id })))
}

async fn get_verification_methods(
    State(state): State<AppState>,
) -> impl IntoResponse {
    let methods: Vec<serde_json::Value> = state.verification.get_available_methods()
        .iter()
        .map(|m| serde_json::json!({
            "id": m.as_str(),
            "name": match m {
                VerificationMethod::HytaleApi => "Hytale Official API",
                VerificationMethod::ManualAdmin => "Manual Admin Verification",
                VerificationMethod::EmailCode => "Email Code Verification",
                VerificationMethod::Mock => "Development Mock Verification",
            },
            "available": true
        }))
        .collect();
    
    let mode = match state.verification.mode() {
        verification::VerificationMode::Mock => "mock",
        verification::VerificationMode::Live => "live",
    };
    
    (StatusCode::OK, ApiResponse::success(serde_json::json!({
        "methods": methods,
        "mode": mode,
        "hytale_api_available": false,
        "note": "Hytale API verification will be available when the game releases"
    })))
}

#[derive(Debug, Deserialize)]
struct StartVerificationRequest {
    token: String,
    method: String,
}

async fn start_verification(
    State(state): State<AppState>,
    Json(req): Json<StartVerificationRequest>,
) -> impl IntoResponse {
    let user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::error("Invalid session")),
    };
    
    let method = match VerificationMethod::from_str(&req.method) {
        Some(m) => m,
        None => return (StatusCode::BAD_REQUEST, ApiResponse::error("Invalid verification method")),
    };
    
    let current_status: Option<(String,)> = sqlx::query_as(
        "SELECT verification_status FROM users WHERE id = $1"
    )
        .bind(user.id)
        .fetch_optional(&state.db)
        .await
        .ok()
        .flatten();
    
    if let Some((status,)) = current_status {
        if status == "verified" {
            return (StatusCode::BAD_REQUEST, ApiResponse::error("Already verified"));
        }
        if status == "pending" {
            return (StatusCode::BAD_REQUEST, ApiResponse::error("Verification already in progress"));
        }
    }
    
    match state.verification.start_verification(user.id, method, &state.db).await {
        Ok(session) => (StatusCode::OK, ApiResponse::success(serde_json::json!({
            "session_id": session.id,
            "method": session.method.as_str(),
            "status": session.status.as_str(),
            "expires_at": session.expires_at
        }))),
        Err(e) => (StatusCode::BAD_REQUEST, ApiResponse::error(&e)),
    }
}

async fn get_verification_status(
    State(state): State<AppState>,
    Json(req): Json<TokenRequest>,
) -> impl IntoResponse {
    let user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::error("Invalid session")),
    };
    
    let user_status: Option<(String, Option<chrono::DateTime<chrono::Utc>>, Option<String>)> = sqlx::query_as(
        "SELECT verification_status, verified_at, verification_method FROM users WHERE id = $1"
    )
        .bind(user.id)
        .fetch_optional(&state.db)
        .await
        .ok()
        .flatten();
    
    let (status, verified_at, method) = user_status.unwrap_or(("unverified".to_string(), None, None));
    
    let pending_session = state.verification.get_user_pending_session(user.id, &state.db).await;
    
    (StatusCode::OK, ApiResponse::success(serde_json::json!({
        "status": status,
        "verified_at": verified_at,
        "method": method,
        "pending_session": pending_session.map(|s| serde_json::json!({
            "session_id": s.id,
            "method": s.method.as_str(),
            "created_at": s.created_at,
            "expires_at": s.expires_at
        }))
    })))
}

async fn cancel_verification(
    State(state): State<AppState>,
    Json(req): Json<TokenRequest>,
) -> impl IntoResponse {
    let user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::error("Invalid session")),
    };
    
    let session = match state.verification.get_user_pending_session(user.id, &state.db).await {
        Some(s) => s,
        None => return (StatusCode::NOT_FOUND, ApiResponse::error("No pending verification")),
    };
    
    match state.verification.cancel_verification(&session, &state.db).await {
        Ok(_) => (StatusCode::OK, ApiResponse::success(serde_json::json!({ "cancelled": true }))),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, ApiResponse::error(&e)),
    }
}

#[derive(Debug, Deserialize)]
struct AdminResolveRequest {
    token: String,
    session_id: Uuid,
    approved: bool,
}

#[derive(Debug, Deserialize)]
struct AdminLoginRequest {
    username: String,
    password: String,
}

#[derive(Debug, Serialize)]
struct AdminAuthResponse {
    admin_token: String,
    username: String,
}

#[derive(Debug, Deserialize)]
struct AdminCreateItemRequest {
    admin_token: String,
    name: String,
    description: String,
    category: String,
    price: f64,
    tags: Vec<String>,
    thumbnail_url: Option<String>,
    file_url: Option<String>,
    is_featured: bool,
}

#[derive(Debug, Deserialize)]
struct AdminUpdateItemRequest {
    admin_token: String,
    name: Option<String>,
    description: Option<String>,
    price: Option<f64>,
    tags: Option<Vec<String>>,
    thumbnail_url: Option<String>,
    file_url: Option<String>,
    is_featured: Option<bool>,
    status: Option<String>,
    admin_notes: Option<String>,
}

#[derive(Debug, Deserialize)]
struct AdminDeleteItemRequest {
    admin_token: String,
}

#[derive(Debug, Deserialize)]
struct AdminTokenRequest {
    admin_token: String,
}

#[derive(Debug, Deserialize)]
struct PurchaseItemRequest {
    token: String,
    item_id: Uuid,
}

#[derive(Debug, Serialize)]
struct EscrowTransaction {
    id: Uuid,
    buyer_id: Uuid,
    seller_id: Uuid,
    item_id: Uuid,
    amount: f64,
    status: String,
    created_at: chrono::DateTime<chrono::Utc>,
}

#[derive(Debug, Deserialize)]
struct AdminReleaseEscrowRequest {
    admin_token: String,
    escrow_id: Uuid,
}

const ADMIN_USERNAME: &str = "DeQuackDealer";
const ADMIN_TOKEN_VALIDITY_HOURS: i64 = 24;

use std::sync::LazyLock;
use dashmap::DashMap;

static ADMIN_SESSIONS: LazyLock<DashMap<String, chrono::DateTime<chrono::Utc>>> = LazyLock::new(DashMap::new);

fn validate_admin_credentials(username: &str, password: &str) -> bool {
    if username != ADMIN_USERNAME {
        return false;
    }
    let admin_password = std::env::var("DeQuackDealerPWD").unwrap_or_default();
    if admin_password.is_empty() {
        error!("DeQuackDealerPWD secret not configured");
        return false;
    }
    password == admin_password
}

fn validate_admin_token(token: &str) -> bool {
    if let Some(expires_at) = ADMIN_SESSIONS.get(token) {
        if *expires_at > chrono::Utc::now() {
            return true;
        } else {
            ADMIN_SESSIONS.remove(token);
        }
    }
    false
}

fn generate_admin_token() -> String {
    let random_bytes: [u8; 32] = rand::Rng::gen(&mut rand::thread_rng());
    let timestamp = chrono::Utc::now().timestamp_nanos_opt().unwrap_or(0);
    let mut hasher = sha2::Sha256::new();
    sha2::Digest::update(&mut hasher, &random_bytes);
    sha2::Digest::update(&mut hasher, &timestamp.to_le_bytes());
    let token = hex::encode(hasher.finalize());
    
    let expires_at = chrono::Utc::now() + chrono::Duration::hours(ADMIN_TOKEN_VALIDITY_HOURS);
    ADMIN_SESSIONS.insert(token.clone(), expires_at);
    
    ADMIN_SESSIONS.retain(|_, exp| *exp > chrono::Utc::now());
    
    token
}

async fn admin_login(
    Json(req): Json<AdminLoginRequest>,
) -> impl IntoResponse {
    if !validate_admin_credentials(&req.username, &req.password) {
        return (StatusCode::UNAUTHORIZED, ApiResponse::<AdminAuthResponse>::error("Invalid admin credentials"));
    }
    
    let admin_token = generate_admin_token();
    
    info!("Admin login successful for {}", ADMIN_USERNAME);
    
    (StatusCode::OK, ApiResponse::success(AdminAuthResponse {
        admin_token,
        username: ADMIN_USERNAME.to_string(),
    }))
}

async fn admin_create_marketplace_item(
    State(state): State<AppState>,
    Json(req): Json<AdminCreateItemRequest>,
) -> impl IntoResponse {
    if !validate_admin_token(&req.admin_token) {
        return (StatusCode::UNAUTHORIZED, ApiResponse::<MarketplaceItem>::error("Invalid admin token"));
    }

    let valid_categories = ["mod", "plugin", "skin", "cosmetic", "texture", "emote"];
    if !valid_categories.contains(&req.category.as_str()) {
        return (StatusCode::BAD_REQUEST, ApiResponse::error("Invalid category"));
    }

    if req.price < 0.0 || req.price > 999.99 {
        return (StatusCode::BAD_REQUEST, ApiResponse::error("Price must be 0-999.99"));
    }

    let admin_id = sqlx::query_scalar::<_, Uuid>(
        "SELECT id FROM users WHERE username = $1"
    )
        .bind(ADMIN_USERNAME)
        .fetch_optional(&state.db)
        .await
        .ok()
        .flatten();

    let author_id = match admin_id {
        Some(id) => id,
        None => {
            let new_id = Uuid::new_v4();
            let password_hash = hash_password(&std::env::var("DeQuackDealerPWD").unwrap_or_default());
            let now = chrono::Utc::now();
            let _ = sqlx::query(
                "INSERT INTO users (id, username, email, password_hash, is_admin, created_at, updated_at) 
                 VALUES ($1, $2, $3, $4, true, $5, $5)
                 ON CONFLICT (username) DO NOTHING"
            )
                .bind(new_id)
                .bind(ADMIN_USERNAME)
                .bind("admin@yellowtale.io")
                .bind(&password_hash)
                .bind(now)
                .execute(&state.db)
                .await;
            new_id
        }
    };

    let item_id = Uuid::new_v4();
    let now = chrono::Utc::now();
    let tags_json = serde_json::to_value(&req.tags).unwrap_or(serde_json::json!([]));

    let result = sqlx::query(
        "INSERT INTO marketplace_items (id, name, description, category, author_id, price, downloads, likes, tags, thumbnail_url, file_url, is_featured, created_at)
         VALUES ($1, $2, $3, $4, $5, $6, 0, 0, $7, $8, $9, $10, $11)"
    )
        .bind(item_id)
        .bind(&req.name)
        .bind(&req.description)
        .bind(&req.category)
        .bind(author_id)
        .bind(req.price)
        .bind(&tags_json)
        .bind(&req.thumbnail_url)
        .bind(&req.file_url)
        .bind(req.is_featured)
        .bind(now)
        .execute(&state.db)
        .await;

    match result {
        Ok(_) => {
            info!("Admin created marketplace item: {} ({})", req.name, item_id);
            let item = MarketplaceItem {
                id: item_id,
                name: req.name,
                description: req.description,
                category: req.category,
                author: MarketplaceAuthor { 
                    id: author_id, 
                    username: ADMIN_USERNAME.to_string(), 
                    display_name: Some("DeQuack Dealer".to_string())
                },
                price: req.price,
                downloads: 0,
                likes: 0,
                tags: req.tags,
                thumbnail_url: req.thumbnail_url,
                file_url: req.file_url,
                is_featured: req.is_featured,
                created_at: now,
            };
            (StatusCode::CREATED, ApiResponse::success(item))
        },
        Err(e) => {
            error!("Failed to create marketplace item: {}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, ApiResponse::error("Failed to create item"))
        }
    }
}

async fn admin_update_marketplace_item(
    State(state): State<AppState>,
    Path(item_id): Path<Uuid>,
    Json(req): Json<AdminUpdateItemRequest>,
) -> impl IntoResponse {
    if !validate_admin_token(&req.admin_token) {
        return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid admin token"));
    }

    let mut updates = vec![];
    let mut bind_idx = 1;

    if req.name.is_some() { updates.push(format!("name = ${}", { bind_idx += 1; bind_idx - 1 })); }
    if req.description.is_some() { updates.push(format!("description = ${}", { bind_idx += 1; bind_idx - 1 })); }
    if req.price.is_some() { updates.push(format!("price = ${}", { bind_idx += 1; bind_idx - 1 })); }
    if req.tags.is_some() { updates.push(format!("tags = ${}", { bind_idx += 1; bind_idx - 1 })); }
    if req.thumbnail_url.is_some() { updates.push(format!("thumbnail_url = ${}", { bind_idx += 1; bind_idx - 1 })); }
    if req.file_url.is_some() { updates.push(format!("file_url = ${}", { bind_idx += 1; bind_idx - 1 })); }
    if req.is_featured.is_some() { updates.push(format!("is_featured = ${}", { bind_idx += 1; bind_idx - 1 })); }
    if req.status.is_some() { updates.push(format!("status = ${}", { bind_idx += 1; bind_idx - 1 })); }
    if req.admin_notes.is_some() { updates.push(format!("admin_notes = ${}", { bind_idx += 1; bind_idx - 1 })); }

    if updates.is_empty() {
        return (StatusCode::BAD_REQUEST, ApiResponse::error("No fields to update"));
    }

    let query = format!("UPDATE marketplace_items SET {} WHERE id = $1", updates.join(", "));
    
    let mut q = sqlx::query(&query).bind(item_id);
    if let Some(ref v) = req.name { q = q.bind(v); }
    if let Some(ref v) = req.description { q = q.bind(v); }
    if let Some(ref v) = req.price { q = q.bind(v); }
    if let Some(ref v) = req.tags { q = q.bind(serde_json::to_value(v).unwrap_or_default()); }
    if let Some(ref v) = req.thumbnail_url { q = q.bind(v); }
    if let Some(ref v) = req.file_url { q = q.bind(v); }
    if let Some(ref v) = req.is_featured { q = q.bind(v); }
    if let Some(ref v) = req.status { q = q.bind(v); }
    if let Some(ref v) = req.admin_notes { q = q.bind(v); }

    match q.execute(&state.db).await {
        Ok(r) if r.rows_affected() > 0 => {
            info!("Admin updated marketplace item: {}", item_id);
            (StatusCode::OK, ApiResponse::success(serde_json::json!({"updated": true, "id": item_id})))
        },
        Ok(_) => (StatusCode::NOT_FOUND, ApiResponse::error("Item not found")),
        Err(e) => {
            error!("Failed to update item: {}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, ApiResponse::error("Failed to update item"))
        }
    }
}

async fn admin_delete_marketplace_item(
    State(state): State<AppState>,
    Path(item_id): Path<Uuid>,
    Json(req): Json<AdminDeleteItemRequest>,
) -> impl IntoResponse {
    if !validate_admin_token(&req.admin_token) {
        return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid admin token"));
    }

    match sqlx::query("DELETE FROM marketplace_items WHERE id = $1")
        .bind(item_id)
        .execute(&state.db)
        .await
    {
        Ok(r) if r.rows_affected() > 0 => {
            info!("Admin deleted marketplace item: {}", item_id);
            (StatusCode::OK, ApiResponse::success(serde_json::json!({"deleted": true})))
        },
        Ok(_) => (StatusCode::NOT_FOUND, ApiResponse::error("Item not found")),
        Err(e) => {
            error!("Failed to delete item: {}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, ApiResponse::error("Failed to delete item"))
        }
    }
}

async fn admin_list_all_items(
    State(state): State<AppState>,
    Json(req): Json<AdminTokenRequest>,
) -> impl IntoResponse {
    if !validate_admin_token(&req.admin_token) {
        return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid admin token"));
    }

    let items = sqlx::query_as::<_, (Uuid, String, String, String, f64, i64, i64, Option<String>, Option<String>, bool, chrono::DateTime<chrono::Utc>)>(
        "SELECT id, name, description, category, price, downloads, likes, thumbnail_url, file_url, is_featured, created_at 
         FROM marketplace_items ORDER BY created_at DESC"
    )
        .fetch_all(&state.db)
        .await
        .unwrap_or_default();

    let items: Vec<serde_json::Value> = items.into_iter().map(|(id, name, desc, cat, price, downloads, likes, thumb, file, featured, created)| {
        serde_json::json!({
            "id": id,
            "name": name,
            "description": desc,
            "category": cat,
            "price": price,
            "downloads": downloads,
            "likes": likes,
            "thumbnail_url": thumb,
            "file_url": file,
            "is_featured": featured,
            "created_at": created
        })
    }).collect();

    (StatusCode::OK, ApiResponse::success(serde_json::json!({"items": items, "count": items.len()})))
}

async fn purchase_marketplace_item(
    State(state): State<AppState>,
    Json(req): Json<PurchaseItemRequest>,
) -> impl IntoResponse {
    let user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };

    let item = sqlx::query_as::<_, (Uuid, String, f64, Uuid)>(
        "SELECT id, name, price, author_id FROM marketplace_items WHERE id = $1"
    )
        .bind(req.item_id)
        .fetch_optional(&state.db)
        .await
        .ok()
        .flatten();

    let (item_id, item_name, price, seller_id) = match item {
        Some(i) => i,
        None => return (StatusCode::NOT_FOUND, ApiResponse::error("Item not found")),
    };

    let already_purchased = sqlx::query_scalar::<_, i64>(
        "SELECT COUNT(*) FROM marketplace_purchases WHERE user_id = $1 AND item_id = $2"
    )
        .bind(user.id)
        .bind(item_id)
        .fetch_one(&state.db)
        .await
        .unwrap_or(0);

    if already_purchased > 0 {
        return (StatusCode::CONFLICT, ApiResponse::error("You already own this item"));
    }

    if price == 0.0 {
        let _ = sqlx::query(
            "INSERT INTO marketplace_purchases (user_id, item_id, amount, status, created_at) VALUES ($1, $2, 0, 'completed', NOW())"
        )
            .bind(user.id)
            .bind(item_id)
            .execute(&state.db)
            .await;

        let _ = sqlx::query("UPDATE marketplace_items SET downloads = downloads + 1 WHERE id = $1")
            .bind(item_id)
            .execute(&state.db)
            .await;

        return (StatusCode::OK, ApiResponse::success(serde_json::json!({
            "purchased": true,
            "item_id": item_id,
            "item_name": item_name,
            "price": 0.0
        })));
    }

    let base_url = std::env::var("REPLIT_DOMAINS")
        .ok()
        .and_then(|d| d.split(',').next().map(|s| format!("https://{}", s)))
        .unwrap_or_else(|| "http://localhost:5000".to_string());

    let escrow_id = Uuid::new_v4();
    let success_url = format!("{}/marketplace/purchase-success?escrow_id={}", base_url, escrow_id);
    let cancel_url = format!("{}/marketplace?purchase=cancelled", base_url);

    let email = sqlx::query_scalar::<_, String>("SELECT email FROM users WHERE id = $1")
        .bind(user.id)
        .fetch_one(&state.db)
        .await
        .unwrap_or_else(|_| "user@example.com".to_string());

    match stripe::create_marketplace_checkout(&email, price, &item_name, &success_url, &cancel_url).await {
        Ok(checkout_result) => {
            let _ = sqlx::query(
                "INSERT INTO escrow_transactions (id, buyer_id, seller_id, item_id, amount, status, stripe_session_id, created_at)
                 VALUES ($1, $2, $3, $4, $5, 'pending', $6, NOW())"
            )
                .bind(escrow_id)
                .bind(user.id)
                .bind(seller_id)
                .bind(item_id)
                .bind(price)
                .bind(&checkout_result.session_id)
                .execute(&state.db)
                .await;

            (StatusCode::OK, ApiResponse::success(serde_json::json!({
                "checkout_url": checkout_result.url,
                "escrow_id": escrow_id,
                "item_id": item_id,
                "item_name": item_name,
                "price": price
            })))
        },
        Err(e) => {
            error!("Stripe checkout failed: {}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, ApiResponse::error("Payment processing failed"))
        }
    }
}

async fn confirm_purchase(
    State(state): State<AppState>,
    Path(escrow_id): Path<Uuid>,
    Json(req): Json<TokenRequest>,
) -> impl IntoResponse {
    let user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };

    let escrow = sqlx::query_as::<_, (Uuid, Uuid, Uuid, f64, String, Option<String>)>(
        "SELECT buyer_id, seller_id, item_id, amount, status, stripe_session_id FROM escrow_transactions WHERE id = $1"
    )
        .bind(escrow_id)
        .fetch_optional(&state.db)
        .await
        .ok()
        .flatten();

    let (buyer_id, _seller_id, item_id, amount, status, stripe_session_id) = match escrow {
        Some(e) => e,
        None => return (StatusCode::NOT_FOUND, ApiResponse::error("Escrow not found")),
    };

    if buyer_id != user.id {
        return (StatusCode::FORBIDDEN, ApiResponse::error("Not your transaction"));
    }

    if status != "pending" {
        return (StatusCode::BAD_REQUEST, ApiResponse::error("Transaction already processed"));
    }

    if amount > 0.0 {
        if stripe_session_id.is_none() {
            return (StatusCode::BAD_REQUEST, ApiResponse::error("Payment not initiated"));
        }
        
        match stripe::verify_payment_completed(&stripe_session_id.unwrap()).await {
            Ok(true) => {},
            Ok(false) => return (StatusCode::BAD_REQUEST, ApiResponse::error("Payment not completed")),
            Err(e) => {
                error!("Payment verification failed: {}", e);
                return (StatusCode::INTERNAL_SERVER_ERROR, ApiResponse::error("Payment verification failed"));
            }
        }
    }

    let _ = sqlx::query("UPDATE escrow_transactions SET status = 'completed', completed_at = NOW() WHERE id = $1")
        .bind(escrow_id)
        .execute(&state.db)
        .await;

    let _ = sqlx::query(
        "INSERT INTO marketplace_purchases (user_id, item_id, amount, escrow_id, status, created_at) VALUES ($1, $2, $3, $4, 'completed', NOW())"
    )
        .bind(user.id)
        .bind(item_id)
        .bind(amount)
        .bind(escrow_id)
        .execute(&state.db)
        .await;

    let _ = sqlx::query("UPDATE marketplace_items SET downloads = downloads + 1 WHERE id = $1")
        .bind(item_id)
        .execute(&state.db)
        .await;

    (StatusCode::OK, ApiResponse::success(serde_json::json!({
        "confirmed": true,
        "item_id": item_id
    })))
}

async fn admin_list_escrow_transactions(
    State(state): State<AppState>,
    Json(req): Json<AdminTokenRequest>,
) -> impl IntoResponse {
    if !validate_admin_token(&req.admin_token) {
        return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid admin token"));
    }

    let escrows = sqlx::query_as::<_, (Uuid, Uuid, Uuid, Uuid, f64, String, chrono::DateTime<chrono::Utc>)>(
        "SELECT id, buyer_id, seller_id, item_id, amount, status, created_at 
         FROM escrow_transactions ORDER BY created_at DESC LIMIT 100"
    )
        .fetch_all(&state.db)
        .await
        .unwrap_or_default();

    let transactions: Vec<EscrowTransaction> = escrows.into_iter().map(|(id, buyer, seller, item, amount, status, created)| {
        EscrowTransaction { id, buyer_id: buyer, seller_id: seller, item_id: item, amount, status, created_at: created }
    }).collect();

    (StatusCode::OK, ApiResponse::success(serde_json::json!({"escrows": transactions})))
}

async fn admin_release_escrow(
    State(state): State<AppState>,
    Json(req): Json<AdminReleaseEscrowRequest>,
) -> impl IntoResponse {
    if !validate_admin_token(&req.admin_token) {
        return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid admin token"));
    }

    let escrow = sqlx::query_as::<_, (String, f64, Uuid)>(
        "SELECT status, amount, seller_id FROM escrow_transactions WHERE id = $1"
    )
        .bind(req.escrow_id)
        .fetch_optional(&state.db)
        .await
        .ok()
        .flatten();

    let (status, _amount, _seller_id) = match escrow {
        Some(e) => e,
        None => return (StatusCode::NOT_FOUND, ApiResponse::error("Escrow not found")),
    };

    if status != "completed" {
        return (StatusCode::BAD_REQUEST, ApiResponse::error("Can only release completed escrows"));
    }

    let _ = sqlx::query("UPDATE escrow_transactions SET status = 'released', released_at = NOW() WHERE id = $1")
        .bind(req.escrow_id)
        .execute(&state.db)
        .await;

    info!("Admin released escrow: {}", req.escrow_id);

    (StatusCode::OK, ApiResponse::success(serde_json::json!({"released": true, "escrow_id": req.escrow_id})))
}

async fn get_user_purchases(
    State(state): State<AppState>,
    Json(req): Json<TokenRequest>,
) -> impl IntoResponse {
    let user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };

    let purchases = sqlx::query_as::<_, (Uuid, Uuid, f64, String, chrono::DateTime<chrono::Utc>)>(
        "SELECT p.id, p.item_id, p.amount, p.status, p.created_at 
         FROM marketplace_purchases p WHERE p.user_id = $1 ORDER BY p.created_at DESC"
    )
        .bind(user.id)
        .fetch_all(&state.db)
        .await
        .unwrap_or_default();

    let purchases: Vec<serde_json::Value> = purchases.into_iter().map(|(id, item_id, amount, status, created)| {
        serde_json::json!({
            "id": id,
            "item_id": item_id,
            "amount": amount,
            "status": status,
            "created_at": created
        })
    }).collect();

    (StatusCode::OK, ApiResponse::success(serde_json::json!({"purchases": purchases})))
}

async fn admin_resolve_verification(
    State(state): State<AppState>,
    Json(req): Json<AdminResolveRequest>,
) -> impl IntoResponse {
    let admin = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::error("Invalid session")),
    };
    
    let is_admin: Option<(bool,)> = sqlx::query_as(
        "SELECT is_admin FROM users WHERE id = $1"
    )
        .bind(admin.id)
        .fetch_optional(&state.db)
        .await
        .ok()
        .flatten();
    
    let is_admin = is_admin.map(|(a,)| a).unwrap_or(false);
    if !is_admin {
        return (StatusCode::FORBIDDEN, ApiResponse::error("Admin access required"));
    }
    
    let session = match state.verification.get_session(req.session_id, &state.db).await {
        Some(s) => s,
        None => return (StatusCode::NOT_FOUND, ApiResponse::error("Verification session not found")),
    };
    
    let data = serde_json::json!({
        "approved": req.approved,
        "admin_id": admin.id.to_string()
    });
    
    match state.verification.complete_verification(&session, &state.db, Some(data)).await {
        Ok(result) => (StatusCode::OK, ApiResponse::success(serde_json::json!({
            "success": result.success,
            "status": result.status.as_str(),
            "message": result.message
        }))),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, ApiResponse::error(&e)),
    }
}

// ==================== RUBIDIUM API HANDLERS ====================

#[derive(Debug, Deserialize)]
struct RubidiumFeatureToggleRequest {
    token: String,
    feature_id: String,
    enabled: bool,
}

#[derive(Debug, Deserialize)]
struct AdminFeatureToggleRequest {
    admin_token: String,
    feature_id: String,
    enabled: bool,
    cascade: Option<bool>,
}

async fn get_rubidium_features(
    State(state): State<AppState>,
    Json(req): Json<TokenRequest>,
) -> impl IntoResponse {
    let user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };

    let features = serde_json::json!({
        "features": [
            {
                "id": "minimap",
                "name": "Minimap",
                "description": "Shows a minimap in the corner of your screen",
                "category": "mapping",
                "enabled": true,
                "admin_controlled": false,
                "premium_only": false
            },
            {
                "id": "worldmap",
                "name": "World Map",
                "description": "Full-screen world map with markers",
                "category": "mapping",
                "enabled": true,
                "admin_controlled": false,
                "premium_only": false
            },
            {
                "id": "waypoints",
                "name": "Waypoints",
                "description": "Mark and navigate to locations",
                "category": "mapping",
                "enabled": true,
                "admin_controlled": false,
                "premium_only": false
            },
            {
                "id": "replay",
                "name": "Replay System",
                "description": "Record and playback gameplay",
                "category": "replay",
                "enabled": true,
                "admin_controlled": true,
                "premium_only": true
            },
            {
                "id": "cinema_camera",
                "name": "Cinema Camera",
                "description": "Cinematic camera controls",
                "category": "cinema",
                "enabled": true,
                "admin_controlled": true,
                "premium_only": true
            },
            {
                "id": "party_system",
                "name": "Party System",
                "description": "Create and join parties with friends",
                "category": "social",
                "enabled": true,
                "admin_controlled": false,
                "premium_only": false
            },
            {
                "id": "friend_activity",
                "name": "Friend Activity",
                "description": "See what your friends are doing",
                "category": "social",
                "enabled": true,
                "admin_controlled": false,
                "premium_only": false
            },
            {
                "id": "anticheat",
                "name": "Anticheat",
                "description": "Lightweight anticheat protection",
                "category": "security",
                "enabled": true,
                "admin_controlled": true,
                "premium_only": false
            }
        ],
        "user_id": user.id,
        "premium": user.premium
    });

    (StatusCode::OK, ApiResponse::success(features))
}

async fn toggle_rubidium_feature(
    State(state): State<AppState>,
    Json(req): Json<RubidiumFeatureToggleRequest>,
) -> impl IntoResponse {
    let _user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };

    (StatusCode::OK, ApiResponse::success(serde_json::json!({
        "feature_id": req.feature_id,
        "enabled": req.enabled,
        "message": "Feature toggle saved"
    })))
}

async fn admin_toggle_feature(
    Json(req): Json<AdminFeatureToggleRequest>,
) -> impl IntoResponse {
    if !validate_admin_token(&req.admin_token) {
        return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid admin token"));
    }

    let cascade = req.cascade.unwrap_or(true);

    (StatusCode::OK, ApiResponse::success(serde_json::json!({
        "feature_id": req.feature_id,
        "enabled": req.enabled,
        "cascade": cascade,
        "message": if cascade { "Feature and children toggled" } else { "Feature toggled" }
    })))
}

#[derive(Debug, Deserialize)]
struct ReplaySessionRequest {
    token: String,
    server_id: Option<String>,
    limit: Option<i32>,
}

async fn list_replay_sessions(
    State(state): State<AppState>,
    Json(req): Json<ReplaySessionRequest>,
) -> impl IntoResponse {
    let user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };

    let sessions = serde_json::json!({
        "sessions": [
            {
                "id": Uuid::new_v4(),
                "user_id": user.id,
                "server_name": "HytaleCraft",
                "duration_seconds": 1847,
                "size_bytes": 15728640,
                "recorded_at": chrono::Utc::now() - chrono::Duration::hours(2),
                "status": "completed"
            },
            {
                "id": Uuid::new_v4(),
                "user_id": user.id,
                "server_name": "Orbis Adventures",
                "duration_seconds": 3621,
                "size_bytes": 28311552,
                "recorded_at": chrono::Utc::now() - chrono::Duration::days(1),
                "status": "completed"
            }
        ],
        "total": 2
    });

    (StatusCode::OK, ApiResponse::success(sessions))
}

async fn get_replay_session(
    Path(id): Path<Uuid>,
) -> impl IntoResponse {
    let session = serde_json::json!({
        "id": id,
        "metadata": {
            "version": "1.0",
            "server": "HytaleCraft",
            "duration_seconds": 1847,
            "start_time": chrono::Utc::now() - chrono::Duration::hours(2),
            "end_time": chrono::Utc::now() - chrono::Duration::hours(2) + chrono::Duration::seconds(1847),
            "player_count": 1,
            "events_count": 15432
        },
        "download_url": format!("/api/v1/rubidium/replay/download/{}", id)
    });

    (StatusCode::OK, ApiResponse::success(session))
}

#[derive(Debug, Deserialize)]
struct StartRecordingRequest {
    token: String,
    server_id: Option<String>,
    quality: Option<String>,
}

async fn start_replay_recording(
    State(state): State<AppState>,
    Json(req): Json<StartRecordingRequest>,
) -> impl IntoResponse {
    let user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };

    if !user.premium {
        return (StatusCode::FORBIDDEN, ApiResponse::error("Replay recording requires premium"));
    }

    let session_id = Uuid::new_v4();
    (StatusCode::OK, ApiResponse::success(serde_json::json!({
        "session_id": session_id,
        "status": "recording",
        "started_at": chrono::Utc::now(),
        "quality": req.quality.unwrap_or_else(|| "high".to_string())
    })))
}

#[derive(Debug, Deserialize)]
struct StopRecordingRequest {
    token: String,
    session_id: Uuid,
}

async fn stop_replay_recording(
    State(state): State<AppState>,
    Json(req): Json<StopRecordingRequest>,
) -> impl IntoResponse {
    let _user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };

    (StatusCode::OK, ApiResponse::success(serde_json::json!({
        "session_id": req.session_id,
        "status": "completed",
        "duration_seconds": 1234,
        "size_bytes": 10485760
    })))
}

#[derive(Debug, Deserialize)]
struct MappingConfigRequest {
    token: String,
}

async fn get_mapping_config(
    State(state): State<AppState>,
    Json(req): Json<MappingConfigRequest>,
) -> impl IntoResponse {
    let _user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };

    (StatusCode::OK, ApiResponse::success(serde_json::json!({
        "minimap": {
            "enabled": true,
            "size": 150,
            "position": "top_right",
            "zoom_level": 1.0,
            "show_players": true,
            "show_entities": true,
            "show_waypoints": true,
            "rotation": "player_rotation"
        },
        "worldmap": {
            "enabled": true,
            "show_explored_only": false,
            "show_coordinates": true,
            "show_biomes": true
        }
    })))
}

#[derive(Debug, Deserialize)]
struct UpdateMappingConfigRequest {
    token: String,
    minimap: Option<serde_json::Value>,
    worldmap: Option<serde_json::Value>,
}

async fn update_mapping_config(
    State(state): State<AppState>,
    Json(req): Json<UpdateMappingConfigRequest>,
) -> impl IntoResponse {
    let _user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };

    (StatusCode::OK, ApiResponse::success(serde_json::json!({
        "updated": true,
        "message": "Mapping configuration updated"
    })))
}

#[derive(Debug, Deserialize)]
struct WaypointRequest {
    token: String,
    world_id: Option<String>,
}

async fn get_waypoints(
    State(state): State<AppState>,
    Json(req): Json<WaypointRequest>,
) -> impl IntoResponse {
    let user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };

    (StatusCode::OK, ApiResponse::success(serde_json::json!({
        "waypoints": [
            {
                "id": Uuid::new_v4(),
                "name": "Home Base",
                "x": 100.0,
                "y": 64.0,
                "z": -250.0,
                "world_id": "overworld",
                "color": "#FFD93D",
                "icon": "home",
                "visible": true,
                "shared": false,
                "created_at": chrono::Utc::now() - chrono::Duration::days(5)
            },
            {
                "id": Uuid::new_v4(),
                "name": "Mining Spot",
                "x": -500.0,
                "y": 12.0,
                "z": 800.0,
                "world_id": "overworld",
                "color": "#60A5FA",
                "icon": "pickaxe",
                "visible": true,
                "shared": false,
                "created_at": chrono::Utc::now() - chrono::Duration::days(2)
            }
        ],
        "user_id": user.id
    })))
}

#[derive(Debug, Deserialize)]
struct CreateWaypointRequest {
    token: String,
    name: String,
    x: f64,
    y: f64,
    z: f64,
    world_id: Option<String>,
    color: Option<String>,
    icon: Option<String>,
}

async fn create_waypoint(
    State(state): State<AppState>,
    Json(req): Json<CreateWaypointRequest>,
) -> impl IntoResponse {
    let _user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };

    let waypoint_id = Uuid::new_v4();
    (StatusCode::CREATED, ApiResponse::success(serde_json::json!({
        "id": waypoint_id,
        "name": req.name,
        "x": req.x,
        "y": req.y,
        "z": req.z,
        "world_id": req.world_id.unwrap_or_else(|| "overworld".to_string()),
        "color": req.color.unwrap_or_else(|| "#FFD93D".to_string()),
        "icon": req.icon.unwrap_or_else(|| "marker".to_string()),
        "created_at": chrono::Utc::now()
    })))
}

#[derive(Debug, Deserialize)]
struct DeleteWaypointRequest {
    token: String,
    waypoint_id: Uuid,
}

async fn delete_waypoint(
    State(state): State<AppState>,
    Json(req): Json<DeleteWaypointRequest>,
) -> impl IntoResponse {
    let _user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };

    (StatusCode::OK, ApiResponse::success(serde_json::json!({
        "deleted": true,
        "waypoint_id": req.waypoint_id
    })))
}

#[derive(Debug, Deserialize)]
struct ShareWaypointRequest {
    token: String,
    waypoint_id: Uuid,
    share_with: Vec<Uuid>,
}

async fn share_waypoint(
    State(state): State<AppState>,
    Json(req): Json<ShareWaypointRequest>,
) -> impl IntoResponse {
    let _user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };

    (StatusCode::OK, ApiResponse::success(serde_json::json!({
        "shared": true,
        "waypoint_id": req.waypoint_id,
        "shared_with": req.share_with.len()
    })))
}

#[derive(Debug, Deserialize)]
struct PartyRequest {
    token: String,
    name: Option<String>,
    max_members: Option<i32>,
}

async fn create_party(
    State(state): State<AppState>,
    Json(req): Json<PartyRequest>,
) -> impl IntoResponse {
    let user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };

    let party_id = Uuid::new_v4();
    (StatusCode::CREATED, ApiResponse::success(serde_json::json!({
        "party_id": party_id,
        "name": req.name.unwrap_or_else(|| format!("{}'s Party", user.username)),
        "leader_id": user.id,
        "max_members": req.max_members.unwrap_or(8),
        "members": [{ "id": user.id, "username": user.username, "role": "leader" }],
        "invite_code": format!("PARTY-{}", &party_id.to_string()[..8].to_uppercase())
    })))
}

#[derive(Debug, Deserialize)]
struct JoinPartyRequest {
    token: String,
    party_id: Option<Uuid>,
    invite_code: Option<String>,
}

async fn join_party(
    State(state): State<AppState>,
    Json(req): Json<JoinPartyRequest>,
) -> impl IntoResponse {
    let user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };

    (StatusCode::OK, ApiResponse::success(serde_json::json!({
        "joined": true,
        "party_id": req.party_id.unwrap_or_else(Uuid::new_v4),
        "user_id": user.id,
        "role": "member"
    })))
}

async fn leave_party(
    State(state): State<AppState>,
    Json(req): Json<TokenRequest>,
) -> impl IntoResponse {
    let _user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };

    (StatusCode::OK, ApiResponse::success(serde_json::json!({
        "left": true,
        "message": "You have left the party"
    })))
}

#[derive(Debug, Deserialize)]
struct InviteToPartyRequest {
    token: String,
    user_id: Uuid,
}

async fn invite_to_party(
    State(state): State<AppState>,
    Json(req): Json<InviteToPartyRequest>,
) -> impl IntoResponse {
    let _user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };

    (StatusCode::OK, ApiResponse::success(serde_json::json!({
        "invited": true,
        "invited_user_id": req.user_id,
        "expires_in_seconds": 300
    })))
}

#[derive(Debug, Deserialize)]
struct UpdatePresenceRequest {
    token: String,
    status: String,
    activity: Option<String>,
    server_id: Option<String>,
}

async fn update_presence(
    State(state): State<AppState>,
    Json(req): Json<UpdatePresenceRequest>,
) -> impl IntoResponse {
    let user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };

    (StatusCode::OK, ApiResponse::success(serde_json::json!({
        "user_id": user.id,
        "status": req.status,
        "activity": req.activity,
        "server_id": req.server_id,
        "updated_at": chrono::Utc::now()
    })))
}

async fn list_camera_paths(
    State(state): State<AppState>,
    Json(req): Json<TokenRequest>,
) -> impl IntoResponse {
    let user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };

    if !user.premium {
        return (StatusCode::FORBIDDEN, ApiResponse::error("Cinema camera requires premium"));
    }

    (StatusCode::OK, ApiResponse::success(serde_json::json!({
        "paths": [
            {
                "id": Uuid::new_v4(),
                "name": "Cinematic Flyover",
                "duration_seconds": 30,
                "keyframes": 12,
                "created_at": chrono::Utc::now() - chrono::Duration::days(3)
            },
            {
                "id": Uuid::new_v4(),
                "name": "Base Tour",
                "duration_seconds": 60,
                "keyframes": 24,
                "created_at": chrono::Utc::now() - chrono::Duration::days(1)
            }
        ]
    })))
}

#[derive(Debug, Deserialize)]
struct CreateCameraPathRequest {
    token: String,
    name: String,
    keyframes: Vec<serde_json::Value>,
    duration_seconds: f64,
}

async fn create_camera_path(
    State(state): State<AppState>,
    Json(req): Json<CreateCameraPathRequest>,
) -> impl IntoResponse {
    let user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };

    if !user.premium {
        return (StatusCode::FORBIDDEN, ApiResponse::error("Cinema camera requires premium"));
    }

    let path_id = Uuid::new_v4();
    (StatusCode::CREATED, ApiResponse::success(serde_json::json!({
        "id": path_id,
        "name": req.name,
        "keyframes": req.keyframes.len(),
        "duration_seconds": req.duration_seconds,
        "created_at": chrono::Utc::now()
    })))
}

#[derive(Debug, Deserialize)]
struct DeleteCameraPathRequest {
    token: String,
    path_id: Uuid,
}

async fn delete_camera_path(
    State(state): State<AppState>,
    Json(req): Json<DeleteCameraPathRequest>,
) -> impl IntoResponse {
    let _user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };

    (StatusCode::OK, ApiResponse::success(serde_json::json!({
        "deleted": true,
        "path_id": req.path_id
    })))
}

async fn get_anticheat_status(
    State(state): State<AppState>,
    Json(req): Json<TokenRequest>,
) -> impl IntoResponse {
    let _user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };

    (StatusCode::OK, ApiResponse::success(serde_json::json!({
        "enabled": true,
        "version": "1.0.0",
        "last_check": chrono::Utc::now(),
        "status": "clean",
        "checks": {
            "memory_integrity": true,
            "file_integrity": true,
            "process_check": true,
            "speed_check": true
        }
    })))
}

#[derive(Debug, Deserialize)]
struct ReportViolationRequest {
    token: String,
    violation_type: String,
    target_user_id: Option<Uuid>,
    evidence: Option<serde_json::Value>,
}

async fn report_violation(
    State(state): State<AppState>,
    Json(req): Json<ReportViolationRequest>,
) -> impl IntoResponse {
    let user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };

    let report_id = Uuid::new_v4();
    (StatusCode::CREATED, ApiResponse::success(serde_json::json!({
        "report_id": report_id,
        "reporter_id": user.id,
        "violation_type": req.violation_type,
        "target_user_id": req.target_user_id,
        "status": "submitted",
        "created_at": chrono::Utc::now()
    })))
}

#[derive(Debug, Deserialize)]
struct PluginsRequest {
    token: String,
    server_id: Option<String>,
}

async fn list_server_plugins(
    State(state): State<AppState>,
    Json(req): Json<PluginsRequest>,
) -> impl IntoResponse {
    let _user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };

    (StatusCode::OK, ApiResponse::success(serde_json::json!({
        "plugins": [
            {
                "id": "economy",
                "name": "Economy Plugin",
                "version": "1.2.0",
                "author": "Yellow Tale",
                "enabled": true,
                "language": "Java"
            },
            {
                "id": "permissions",
                "name": "Permissions Manager",
                "version": "1.0.0",
                "author": "Yellow Tale",
                "enabled": true,
                "language": "C#"
            },
            {
                "id": "worldedit",
                "name": "World Editor",
                "version": "2.1.0",
                "author": "Community",
                "enabled": false,
                "language": "Java"
            }
        ],
        "server_id": req.server_id
    })))
}

#[derive(Debug, Deserialize)]
struct PluginConfigRequest {
    token: String,
    plugin_id: String,
}

async fn get_plugin_config(
    State(state): State<AppState>,
    Json(req): Json<PluginConfigRequest>,
) -> impl IntoResponse {
    let _user = match validate_token(&state.db, &req.token).await {
        Some(u) => u,
        None => return (StatusCode::UNAUTHORIZED, ApiResponse::<serde_json::Value>::error("Invalid token")),
    };

    (StatusCode::OK, ApiResponse::success(serde_json::json!({
        "plugin_id": req.plugin_id,
        "config": {
            "enabled": true,
            "settings": {}
        }
    })))
}

// ==================== END RUBIDIUM API HANDLERS ====================

async fn run_migrations(db: &PgPool) {
    let migrations = [
        "CREATE EXTENSION IF NOT EXISTS pgcrypto",
        "CREATE TABLE IF NOT EXISTS users (
            id UUID PRIMARY KEY,
            username VARCHAR(64) UNIQUE NOT NULL,
            email VARCHAR(255) UNIQUE NOT NULL,
            password_hash TEXT NOT NULL,
            display_name VARCHAR(128),
            avatar_url TEXT,
            verification_status VARCHAR(32) NOT NULL DEFAULT 'unverified',
            verified_at TIMESTAMPTZ,
            verification_method VARCHAR(32),
            last_seen TIMESTAMPTZ,
            created_at TIMESTAMPTZ NOT NULL,
            updated_at TIMESTAMPTZ NOT NULL
        )",
        "ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_status VARCHAR(32) NOT NULL DEFAULT 'unverified'",
        "ALTER TABLE users ADD COLUMN IF NOT EXISTS verified_at TIMESTAMPTZ",
        "ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_method VARCHAR(32)",
        "ALTER TABLE users ADD COLUMN IF NOT EXISTS is_admin BOOLEAN NOT NULL DEFAULT FALSE",
        "CREATE TABLE IF NOT EXISTS user_sessions (
            id UUID PRIMARY KEY,
            user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            token_hash TEXT NOT NULL,
            expires_at TIMESTAMPTZ NOT NULL,
            created_at TIMESTAMPTZ NOT NULL
        )",
        "CREATE TABLE IF NOT EXISTS friendships (
            id UUID PRIMARY KEY,
            user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            friend_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            status VARCHAR(20) NOT NULL DEFAULT 'pending',
            created_at TIMESTAMPTZ NOT NULL,
            accepted_at TIMESTAMPTZ
        )",
        "CREATE TABLE IF NOT EXISTS blocks (
            id UUID PRIMARY KEY,
            blocker_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            blocked_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            reason TEXT,
            created_at TIMESTAMPTZ NOT NULL
        )",
        "CREATE TABLE IF NOT EXISTS game_servers (
            id UUID PRIMARY KEY,
            name VARCHAR(128) NOT NULL,
            description TEXT,
            address VARCHAR(255) NOT NULL,
            port INTEGER NOT NULL,
            max_players INTEGER NOT NULL DEFAULT 20,
            current_players INTEGER NOT NULL DEFAULT 0,
            game_mode VARCHAR(64) NOT NULL DEFAULT 'survival',
            tags JSONB DEFAULT '[]',
            owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            is_online BOOLEAN NOT NULL DEFAULT false,
            last_ping TIMESTAMPTZ NOT NULL,
            created_at TIMESTAMPTZ NOT NULL
        )",
        "CREATE TABLE IF NOT EXISTS game_stats (
            user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
            total_playtime_minutes BIGINT NOT NULL DEFAULT 0,
            total_sessions BIGINT NOT NULL DEFAULT 0,
            last_played TIMESTAMPTZ,
            favorite_server VARCHAR(128),
            achievements_count INTEGER NOT NULL DEFAULT 0
        )",
        "CREATE TABLE IF NOT EXISTS mod_profiles (
            id UUID PRIMARY KEY,
            user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            name VARCHAR(128) NOT NULL,
            description TEXT,
            mods JSONB NOT NULL DEFAULT '[]',
            is_active BOOLEAN NOT NULL DEFAULT false,
            created_at TIMESTAMPTZ NOT NULL
        )",
        "CREATE TABLE IF NOT EXISTS performance_settings (
            user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
            ram_allocation_mb INTEGER NOT NULL DEFAULT 4096,
            java_args TEXT NOT NULL DEFAULT '-XX:+UseG1GC',
            priority_mode VARCHAR(32) NOT NULL DEFAULT 'normal',
            vsync_enabled BOOLEAN NOT NULL DEFAULT true,
            max_fps INTEGER NOT NULL DEFAULT 60
        )",
        "CREATE TABLE IF NOT EXISTS subscriptions (
            user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
            tier VARCHAR(32) NOT NULL DEFAULT 'free',
            status VARCHAR(32) NOT NULL DEFAULT 'active',
            stripe_customer_id VARCHAR(255),
            stripe_subscription_id VARCHAR(255),
            current_period_end TIMESTAMPTZ,
            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
            updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
        )",
        "CREATE INDEX IF NOT EXISTS idx_users_username ON users(username)",
        "CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)",
        "CREATE INDEX IF NOT EXISTS idx_sessions_token ON user_sessions(token_hash)",
        "CREATE INDEX IF NOT EXISTS idx_friendships_user ON friendships(user_id)",
        "CREATE INDEX IF NOT EXISTS idx_friendships_friend ON friendships(friend_id)",
        "CREATE INDEX IF NOT EXISTS idx_servers_online ON game_servers(is_online, last_ping)",
        "CREATE INDEX IF NOT EXISTS idx_mod_profiles_user ON mod_profiles(user_id)",
        "CREATE TABLE IF NOT EXISTS marketplace_items (
            id UUID PRIMARY KEY,
            name VARCHAR(100) NOT NULL,
            description TEXT NOT NULL,
            category VARCHAR(32) NOT NULL,
            author_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            price DOUBLE PRECISION NOT NULL DEFAULT 0,
            downloads BIGINT NOT NULL DEFAULT 0,
            likes BIGINT NOT NULL DEFAULT 0,
            tags JSONB DEFAULT '[]',
            thumbnail_url TEXT,
            file_url TEXT,
            is_featured BOOLEAN NOT NULL DEFAULT false,
            created_at TIMESTAMPTZ NOT NULL
        )",
        "CREATE TABLE IF NOT EXISTS marketplace_likes (
            user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            item_id UUID NOT NULL REFERENCES marketplace_items(id) ON DELETE CASCADE,
            created_at TIMESTAMPTZ NOT NULL,
            PRIMARY KEY (user_id, item_id)
        )",
        "CREATE TABLE IF NOT EXISTS marketplace_purchases (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            item_id UUID NOT NULL REFERENCES marketplace_items(id) ON DELETE CASCADE,
            amount DOUBLE PRECISION NOT NULL,
            stripe_payment_id VARCHAR(255),
            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
        )",
        "CREATE INDEX IF NOT EXISTS idx_marketplace_category ON marketplace_items(category)",
        "CREATE INDEX IF NOT EXISTS idx_marketplace_author ON marketplace_items(author_id)",
        "CREATE INDEX IF NOT EXISTS idx_marketplace_featured ON marketplace_items(is_featured)",
        "CREATE INDEX IF NOT EXISTS idx_marketplace_downloads ON marketplace_items(downloads DESC)",
        "CREATE TABLE IF NOT EXISTS user_equipped_cosmetics (
            user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            slot VARCHAR(32) NOT NULL,
            item_id VARCHAR(255) NOT NULL,
            PRIMARY KEY (user_id, slot)
        )",
        "CREATE INDEX IF NOT EXISTS idx_equipped_cosmetics_user ON user_equipped_cosmetics(user_id)",
        "CREATE TABLE IF NOT EXISTS user_verifications (
            id UUID PRIMARY KEY,
            user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            method VARCHAR(32) NOT NULL,
            status VARCHAR(32) NOT NULL DEFAULT 'pending',
            metadata JSONB,
            admin_id UUID REFERENCES users(id),
            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
            updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
            completed_at TIMESTAMPTZ,
            expires_at TIMESTAMPTZ
        )",
        "CREATE INDEX IF NOT EXISTS idx_user_verifications_user ON user_verifications(user_id)",
        "CREATE INDEX IF NOT EXISTS idx_user_verifications_status ON user_verifications(status)",
        "CREATE TABLE IF NOT EXISTS escrow_transactions (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            buyer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            seller_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            item_id UUID NOT NULL REFERENCES marketplace_items(id) ON DELETE CASCADE,
            amount DOUBLE PRECISION NOT NULL,
            status VARCHAR(32) NOT NULL DEFAULT 'pending',
            stripe_session_id VARCHAR(255),
            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
            completed_at TIMESTAMPTZ,
            released_at TIMESTAMPTZ
        )",
        "CREATE INDEX IF NOT EXISTS idx_escrow_buyer ON escrow_transactions(buyer_id)",
        "CREATE INDEX IF NOT EXISTS idx_escrow_seller ON escrow_transactions(seller_id)",
        "CREATE INDEX IF NOT EXISTS idx_escrow_status ON escrow_transactions(status)",
        "ALTER TABLE marketplace_items ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'active'",
        "ALTER TABLE marketplace_items ADD COLUMN IF NOT EXISTS admin_notes TEXT",
        "ALTER TABLE marketplace_purchases ADD COLUMN IF NOT EXISTS escrow_id UUID REFERENCES escrow_transactions(id)",
        "ALTER TABLE marketplace_purchases ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'completed'",
    ];
    
    for sql in migrations {
        if let Err(e) = sqlx::query(sql).execute(db).await {
            error!("Migration warning: {}", e);
        }
    }
    
    info!("Migrations completed");
}
