#![allow(dead_code)]

use axum::{
    extract::State,
    response::Json,
};
use serde::{Deserialize, Serialize};
use sqlx::PgPool;
use uuid::Uuid;

use crate::auth::hash_token;

#[derive(Debug, Serialize)]
pub struct AdminUser {
    pub id: Uuid,
    pub username: String,
    pub is_admin: bool,
}

#[derive(Debug, Deserialize)]
pub struct AdminLoginRequest {
    pub username: String,
    pub password: String,
}

#[derive(Debug, Deserialize)]
pub struct AdminActionRequest {
    pub token: String,
}

#[derive(Debug, Deserialize)]
pub struct BanUserRequest {
    pub token: String,
    pub user_id: Uuid,
    pub reason: String,
    pub duration_hours: Option<i32>,
}

#[derive(Debug, Deserialize)]
pub struct ToggleFeatureRequest {
    pub token: String,
    pub feature_name: String,
    pub enabled: bool,
}

#[derive(Debug, Deserialize)]
pub struct SetUserPremiumRequest {
    pub token: String,
    pub user_id: Uuid,
    pub premium: bool,
}

#[derive(Debug, Serialize)]
pub struct AdminStats {
    pub total_users: i64,
    pub active_users_24h: i64,
    pub premium_users: i64,
    pub total_servers: i64,
    pub online_servers: i64,
    pub pending_marketplace_items: i64,
    pub total_revenue_cents: i64,
}

pub async fn verify_admin(db: &PgPool, token: &str) -> Result<Uuid, String> {
    let token_hash = hash_token(token);
    
    let result = sqlx::query!(
        r#"
        SELECT u.id, u.is_admin
        FROM users u
        JOIN user_sessions s ON u.id = s.user_id
        WHERE s.token_hash = $1 AND s.expires_at > NOW()
        "#,
        token_hash
    )
    .fetch_optional(db)
    .await
    .map_err(|e| format!("Database error: {}", e))?;
    
    match result {
        Some(row) if row.is_admin => Ok(row.id),
        Some(_) => Err("Not authorized: admin access required".to_string()),
        None => Err("Invalid or expired session".to_string()),
    }
}

pub async fn get_admin_stats(
    State(db): State<PgPool>,
    Json(req): Json<AdminActionRequest>,
) -> Json<crate::ApiResponse<AdminStats>> {
    if let Err(e) = verify_admin(&db, &req.token).await {
        return crate::ApiResponse::error(e);
    }
    
    let total_users = sqlx::query_scalar!("SELECT COUNT(*) FROM users")
        .fetch_one(&db)
        .await
        .unwrap_or(Some(0))
        .unwrap_or(0);
    
    let active_users_24h = sqlx::query_scalar!(
        "SELECT COUNT(*) FROM user_sessions WHERE expires_at > NOW() AND created_at > NOW() - INTERVAL '24 hours'"
    )
    .fetch_one(&db)
    .await
    .unwrap_or(Some(0))
    .unwrap_or(0);
    
    let premium_users = sqlx::query_scalar!("SELECT COUNT(*) FROM users WHERE premium = true")
        .fetch_one(&db)
        .await
        .unwrap_or(Some(0))
        .unwrap_or(0);
    
    let total_servers = sqlx::query_scalar!("SELECT COUNT(*) FROM game_servers")
        .fetch_one(&db)
        .await
        .unwrap_or(Some(0))
        .unwrap_or(0);
    
    let online_servers = sqlx::query_scalar!("SELECT COUNT(*) FROM game_servers WHERE is_online = true")
        .fetch_one(&db)
        .await
        .unwrap_or(Some(0))
        .unwrap_or(0);
    
    let pending_marketplace_items = sqlx::query_scalar!(
        "SELECT COUNT(*) FROM marketplace_items WHERE status = 'pending_review'"
    )
    .fetch_one(&db)
    .await
    .unwrap_or(Some(0))
    .unwrap_or(0);
    
    let total_revenue_cents = sqlx::query_scalar!(
        "SELECT COALESCE(SUM(amount_cents), 0) FROM payments WHERE status = 'completed'"
    )
    .fetch_one(&db)
    .await
    .unwrap_or(Some(0))
    .unwrap_or(0);
    
    crate::ApiResponse::success(AdminStats {
        total_users,
        active_users_24h,
        premium_users,
        total_servers,
        online_servers,
        pending_marketplace_items,
        total_revenue_cents,
    })
}

pub async fn ban_user(
    State(db): State<PgPool>,
    Json(req): Json<BanUserRequest>,
) -> Json<crate::ApiResponse<()>> {
    if let Err(e) = verify_admin(&db, &req.token).await {
        return crate::ApiResponse::error(e);
    }
    
    let expires_at = req.duration_hours
        .map(|h| chrono::Utc::now() + chrono::Duration::hours(h as i64));
    
    let result = sqlx::query!(
        r#"
        INSERT INTO user_bans (user_id, reason, expires_at, created_at)
        VALUES ($1, $2, $3, NOW())
        "#,
        req.user_id,
        req.reason,
        expires_at
    )
    .execute(&db)
    .await;
    
    match result {
        Ok(_) => {
            sqlx::query!("DELETE FROM user_sessions WHERE user_id = $1", req.user_id)
                .execute(&db)
                .await
                .ok();
            crate::ApiResponse::success(())
        }
        Err(e) => crate::ApiResponse::error(format!("Failed to ban user: {}", e)),
    }
}

pub async fn set_user_premium(
    State(db): State<PgPool>,
    Json(req): Json<SetUserPremiumRequest>,
) -> Json<crate::ApiResponse<()>> {
    if let Err(e) = verify_admin(&db, &req.token).await {
        return crate::ApiResponse::error(e);
    }
    
    let result = sqlx::query!(
        "UPDATE users SET premium = $1 WHERE id = $2",
        req.premium,
        req.user_id
    )
    .execute(&db)
    .await;
    
    match result {
        Ok(_) => crate::ApiResponse::success(()),
        Err(e) => crate::ApiResponse::error(format!("Failed to update premium status: {}", e)),
    }
}
