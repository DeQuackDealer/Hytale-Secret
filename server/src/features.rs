#![allow(dead_code)]

use axum::{
    extract::State,
    response::Json,
};
use serde::{Deserialize, Serialize};
use sqlx::PgPool;

use crate::admin::verify_admin;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FeatureFlag {
    pub name: String,
    pub enabled: bool,
    pub description: Option<String>,
    pub requires_premium: bool,
    pub category: String,
}

#[derive(Debug, Deserialize)]
pub struct GetFeaturesRequest {
    pub token: Option<String>,
}

#[derive(Debug, Deserialize)]
pub struct ToggleFeatureRequest {
    pub token: String,
    pub feature_name: String,
    pub enabled: bool,
}

#[derive(Debug, Deserialize)]
pub struct CreateFeatureRequest {
    pub token: String,
    pub name: String,
    pub description: Option<String>,
    pub enabled: bool,
    pub requires_premium: bool,
    pub category: String,
}

pub const DEFAULT_FEATURES: &[(&str, bool, &str, bool, &str)] = &[
    ("replay_system", true, "Record and replay game sessions", false, "gameplay"),
    ("minimap", true, "In-game minimap overlay", false, "interface"),
    ("worldmap", true, "Full world map view", false, "interface"),
    ("waypoints", true, "Custom waypoint markers", false, "navigation"),
    ("party_system", true, "Create and manage parties", false, "social"),
    ("friend_activity", true, "See friends' online status", false, "social"),
    ("cinema_camera", true, "Cinematic camera controls", false, "creative"),
    ("mod_marketplace", true, "Access mod marketplace", false, "mods"),
    ("cosmetics", true, "Custom cosmetic items", false, "premium"),
    ("premium_capes", false, "Exclusive premium capes", true, "premium"),
    ("server_queue_priority", false, "Priority server queue", true, "premium"),
    ("anticheat_bypass_report", true, "Report false positives", false, "anticheat"),
    ("voice_chat", false, "In-game voice chat", false, "social"),
    ("stream_mode", true, "Hide sensitive info while streaming", false, "interface"),
];

pub async fn initialize_features(db: &PgPool) -> Result<(), sqlx::Error> {
    for (name, enabled, description, premium, category) in DEFAULT_FEATURES {
        sqlx::query!(
            r#"
            INSERT INTO feature_flags (name, enabled, description, requires_premium, category)
            VALUES ($1, $2, $3, $4, $5)
            ON CONFLICT (name) DO NOTHING
            "#,
            name,
            enabled,
            Some(description.to_string()),
            premium,
            category
        )
        .execute(db)
        .await?;
    }
    Ok(())
}

pub async fn get_features(
    State(db): State<PgPool>,
    Json(req): Json<GetFeaturesRequest>,
) -> Json<crate::ApiResponse<Vec<FeatureFlag>>> {
    let is_premium = if let Some(token) = &req.token {
        let token_hash = crate::auth::hash_token(token);
        sqlx::query_scalar!(
            r#"
            SELECT u.premium
            FROM users u
            JOIN user_sessions s ON u.id = s.user_id
            WHERE s.token_hash = $1 AND s.expires_at > NOW()
            "#,
            token_hash
        )
        .fetch_optional(&db)
        .await
        .ok()
        .flatten()
        .flatten()
        .unwrap_or(false)
    } else {
        false
    };
    
    let features = sqlx::query_as!(
        FeatureFlag,
        r#"
        SELECT name, enabled, description, requires_premium, category
        FROM feature_flags
        WHERE enabled = true AND (requires_premium = false OR $1 = true)
        ORDER BY category, name
        "#,
        is_premium
    )
    .fetch_all(&db)
    .await;
    
    match features {
        Ok(f) => crate::ApiResponse::success(f),
        Err(e) => crate::ApiResponse::error(format!("Failed to get features: {}", e)),
    }
}

pub async fn get_all_features(
    State(db): State<PgPool>,
    Json(req): Json<crate::admin::AdminActionRequest>,
) -> Json<crate::ApiResponse<Vec<FeatureFlag>>> {
    if let Err(e) = verify_admin(&db, &req.token).await {
        return crate::ApiResponse::error(e);
    }
    
    let features = sqlx::query_as!(
        FeatureFlag,
        r#"
        SELECT name, enabled, description, requires_premium, category
        FROM feature_flags
        ORDER BY category, name
        "#
    )
    .fetch_all(&db)
    .await;
    
    match features {
        Ok(f) => crate::ApiResponse::success(f),
        Err(e) => crate::ApiResponse::error(format!("Failed to get features: {}", e)),
    }
}

pub async fn toggle_feature(
    State(db): State<PgPool>,
    Json(req): Json<ToggleFeatureRequest>,
) -> Json<crate::ApiResponse<FeatureFlag>> {
    if let Err(e) = verify_admin(&db, &req.token).await {
        return crate::ApiResponse::error(e);
    }
    
    let result = sqlx::query_as!(
        FeatureFlag,
        r#"
        UPDATE feature_flags
        SET enabled = $1
        WHERE name = $2
        RETURNING name, enabled, description, requires_premium, category
        "#,
        req.enabled,
        req.feature_name
    )
    .fetch_optional(&db)
    .await;
    
    match result {
        Ok(Some(f)) => crate::ApiResponse::success(f),
        Ok(None) => crate::ApiResponse::error("Feature not found"),
        Err(e) => crate::ApiResponse::error(format!("Failed to toggle feature: {}", e)),
    }
}

pub async fn create_feature(
    State(db): State<PgPool>,
    Json(req): Json<CreateFeatureRequest>,
) -> Json<crate::ApiResponse<FeatureFlag>> {
    if let Err(e) = verify_admin(&db, &req.token).await {
        return crate::ApiResponse::error(e);
    }
    
    let result = sqlx::query_as!(
        FeatureFlag,
        r#"
        INSERT INTO feature_flags (name, enabled, description, requires_premium, category)
        VALUES ($1, $2, $3, $4, $5)
        RETURNING name, enabled, description, requires_premium, category
        "#,
        req.name,
        req.enabled,
        req.description,
        req.requires_premium,
        req.category
    )
    .fetch_one(&db)
    .await;
    
    match result {
        Ok(f) => crate::ApiResponse::success(f),
        Err(e) => crate::ApiResponse::error(format!("Failed to create feature: {}", e)),
    }
}
