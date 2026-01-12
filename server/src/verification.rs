#![allow(dead_code)]

use serde::{Deserialize, Serialize};
use sqlx::PgPool;
use uuid::Uuid;
use chrono::{DateTime, Utc};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum VerificationStatus {
    Unverified,
    Pending,
    Verified,
    Failed,
}

impl VerificationStatus {
    pub fn as_str(&self) -> &'static str {
        match self {
            Self::Unverified => "unverified",
            Self::Pending => "pending",
            Self::Verified => "verified",
            Self::Failed => "failed",
        }
    }
    
    pub fn from_str(s: &str) -> Self {
        match s {
            "pending" => Self::Pending,
            "verified" => Self::Verified,
            "failed" => Self::Failed,
            _ => Self::Unverified,
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum VerificationMethod {
    HytaleApi,
    ManualAdmin,
    EmailCode,
    Mock,
}

impl VerificationMethod {
    pub fn as_str(&self) -> &'static str {
        match self {
            Self::HytaleApi => "hytale_api",
            Self::ManualAdmin => "manual_admin",
            Self::EmailCode => "email_code",
            Self::Mock => "mock",
        }
    }
    
    pub fn from_str(s: &str) -> Option<Self> {
        match s {
            "hytale_api" => Some(Self::HytaleApi),
            "manual_admin" => Some(Self::ManualAdmin),
            "email_code" => Some(Self::EmailCode),
            "mock" => Some(Self::Mock),
            _ => None,
        }
    }
    
    pub fn display_name(&self) -> &'static str {
        match self {
            Self::HytaleApi => "Hytale Official API",
            Self::ManualAdmin => "Manual Admin Verification",
            Self::EmailCode => "Email Code Verification",
            Self::Mock => "Development Mock Verification",
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct VerificationSession {
    pub id: Uuid,
    pub user_id: Uuid,
    pub method: VerificationMethod,
    pub status: VerificationStatus,
    pub metadata: Option<serde_json::Value>,
    pub created_at: DateTime<Utc>,
    pub expires_at: Option<DateTime<Utc>>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct VerificationResult {
    pub success: bool,
    pub status: VerificationStatus,
    pub message: String,
    pub session_id: Option<Uuid>,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum VerificationMode {
    Mock,
    Live,
}

pub struct VerificationService {
    mode: VerificationMode,
    hytale_api_available: bool,
}

impl VerificationService {
    pub fn new() -> Self {
        let mode = match std::env::var("HYTALE_VERIFICATION_MODE").as_deref() {
            Ok("live") => VerificationMode::Live,
            _ => VerificationMode::Mock,
        };
        
        let hytale_api_available = std::env::var("HYTALE_API_KEY").is_ok() 
            && std::env::var("HYTALE_API_URL").is_ok();
        
        Self { mode, hytale_api_available }
    }
    
    pub fn mode(&self) -> VerificationMode {
        self.mode
    }
    
    pub fn get_available_methods(&self) -> Vec<VerificationMethod> {
        let mut methods = vec![];
        
        if self.mode == VerificationMode::Mock {
            methods.push(VerificationMethod::Mock);
        }
        
        methods.push(VerificationMethod::ManualAdmin);
        
        if self.hytale_api_available {
            methods.push(VerificationMethod::HytaleApi);
        }
        
        methods
    }
    
    pub fn is_method_available(&self, method: VerificationMethod) -> bool {
        match method {
            VerificationMethod::Mock => self.mode == VerificationMode::Mock,
            VerificationMethod::ManualAdmin => true,
            VerificationMethod::HytaleApi => self.hytale_api_available,
            VerificationMethod::EmailCode => true,
        }
    }
    
    pub async fn start_verification(
        &self,
        user_id: Uuid,
        method: VerificationMethod,
        db: &PgPool,
    ) -> Result<VerificationSession, String> {
        if !self.is_method_available(method) {
            return Err(format!("{} is not currently available", method.display_name()));
        }
        
        let session_id = Uuid::new_v4();
        let now = Utc::now();
        let expires = match method {
            VerificationMethod::Mock => Some(now + chrono::Duration::hours(24)),
            VerificationMethod::HytaleApi => Some(now + chrono::Duration::hours(1)),
            _ => None,
        };
        
        let _ = sqlx::query(
            "INSERT INTO user_verifications (id, user_id, method, status, created_at, updated_at, expires_at)
             VALUES ($1, $2, $3, $4, $5, $5, $6)"
        )
            .bind(session_id)
            .bind(user_id)
            .bind(method.as_str())
            .bind(VerificationStatus::Pending.as_str())
            .bind(now)
            .bind(expires)
            .execute(db)
            .await
            .map_err(|e| e.to_string())?;
        
        let _ = sqlx::query("UPDATE users SET verification_status = $1 WHERE id = $2")
            .bind(VerificationStatus::Pending.as_str())
            .bind(user_id)
            .execute(db)
            .await;
        
        Ok(VerificationSession {
            id: session_id,
            user_id,
            method,
            status: VerificationStatus::Pending,
            metadata: match method {
                VerificationMethod::HytaleApi => Some(serde_json::json!({
                    "note": "Awaiting Hytale official API integration"
                })),
                _ => None,
            },
            created_at: now,
            expires_at: expires,
        })
    }
    
    pub async fn get_session(&self, session_id: Uuid, db: &PgPool) -> Option<VerificationSession> {
        let row: Option<(Uuid, Uuid, String, String, Option<serde_json::Value>, DateTime<Utc>, Option<DateTime<Utc>>)> = sqlx::query_as(
            "SELECT id, user_id, method, status, metadata, created_at, expires_at FROM user_verifications WHERE id = $1"
        )
            .bind(session_id)
            .fetch_optional(db)
            .await
            .ok()?;
        
        row.map(|(id, user_id, method, status, metadata, created_at, expires_at)| {
            VerificationSession {
                id,
                user_id,
                method: VerificationMethod::from_str(&method).unwrap_or(VerificationMethod::Mock),
                status: VerificationStatus::from_str(&status),
                metadata,
                created_at,
                expires_at,
            }
        })
    }
    
    pub async fn get_user_pending_session(&self, user_id: Uuid, db: &PgPool) -> Option<VerificationSession> {
        let row: Option<(Uuid, Uuid, String, String, Option<serde_json::Value>, DateTime<Utc>, Option<DateTime<Utc>>)> = sqlx::query_as(
            "SELECT id, user_id, method, status, metadata, created_at, expires_at 
             FROM user_verifications 
             WHERE user_id = $1 AND status = 'pending' 
             ORDER BY created_at DESC LIMIT 1"
        )
            .bind(user_id)
            .fetch_optional(db)
            .await
            .ok()?;
        
        row.map(|(id, user_id, method, status, metadata, created_at, expires_at)| {
            VerificationSession {
                id,
                user_id,
                method: VerificationMethod::from_str(&method).unwrap_or(VerificationMethod::Mock),
                status: VerificationStatus::from_str(&status),
                metadata,
                created_at,
                expires_at,
            }
        })
    }
    
    pub async fn complete_verification(
        &self,
        session: &VerificationSession,
        db: &PgPool,
        data: Option<serde_json::Value>,
    ) -> Result<VerificationResult, String> {
        let now = Utc::now();
        
        let (success, status, message) = match session.method {
            VerificationMethod::Mock => {
                (true, VerificationStatus::Verified, "Mock verification successful".to_string())
            },
            VerificationMethod::ManualAdmin => {
                let approved = data
                    .as_ref()
                    .and_then(|d| d.get("approved"))
                    .and_then(|v| v.as_bool())
                    .unwrap_or(false);
                
                if approved {
                    (true, VerificationStatus::Verified, "Admin approved verification".to_string())
                } else {
                    (false, VerificationStatus::Failed, "Admin rejected verification".to_string())
                }
            },
            VerificationMethod::HytaleApi => {
                (true, VerificationStatus::Verified, "Hytale ownership verified".to_string())
            },
            VerificationMethod::EmailCode => {
                let code_valid = data
                    .as_ref()
                    .and_then(|d| d.get("code_valid"))
                    .and_then(|v| v.as_bool())
                    .unwrap_or(false);
                
                if code_valid {
                    (true, VerificationStatus::Verified, "Email verified".to_string())
                } else {
                    (false, VerificationStatus::Failed, "Invalid verification code".to_string())
                }
            },
        };
        
        let admin_id = data
            .as_ref()
            .and_then(|d| d.get("admin_id"))
            .and_then(|v| v.as_str())
            .and_then(|s| Uuid::parse_str(s).ok());
        
        let _ = sqlx::query(
            "UPDATE user_verifications SET status = $1, completed_at = $2, updated_at = $2, admin_id = $3 WHERE id = $4"
        )
            .bind(status.as_str())
            .bind(now)
            .bind(admin_id)
            .bind(session.id)
            .execute(db)
            .await;
        
        let _ = sqlx::query(
            "UPDATE users SET verification_status = $1, verified_at = $2, verification_method = $3 WHERE id = $4"
        )
            .bind(status.as_str())
            .bind(if success { Some(now) } else { None })
            .bind(session.method.as_str())
            .bind(session.user_id)
            .execute(db)
            .await;
        
        Ok(VerificationResult {
            success,
            status,
            message,
            session_id: Some(session.id),
        })
    }
    
    pub async fn cancel_verification(
        &self,
        session: &VerificationSession,
        db: &PgPool,
    ) -> Result<(), String> {
        let _ = sqlx::query("DELETE FROM user_verifications WHERE id = $1")
            .bind(session.id)
            .execute(db)
            .await;
        
        let _ = sqlx::query("UPDATE users SET verification_status = 'unverified' WHERE id = $1")
            .bind(session.user_id)
            .execute(db)
            .await;
        
        Ok(())
    }
}

pub fn is_user_verified(status: &str) -> bool {
    status == "verified"
}

pub fn require_verified(status: &str) -> Result<(), String> {
    if is_user_verified(status) {
        Ok(())
    } else {
        Err("This feature requires Hytale ownership verification".to_string())
    }
}
