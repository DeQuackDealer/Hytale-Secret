use argon2::{
    password_hash::{rand_core::OsRng, PasswordHash, PasswordHasher, PasswordVerifier, SaltString},
    Argon2,
};
use chrono::{DateTime, Duration, Utc};
use serde::{Deserialize, Serialize};
use sha2::{Digest, Sha256};
use sqlx::PgPool;
use thiserror::Error;
use tracing::{info, warn};
use uuid::Uuid;

#[derive(Error, Debug)]
pub enum AuthError {
    #[error("Username already exists")]
    UsernameExists,
    
    #[error("Email already exists")]
    EmailExists,
    
    #[error("Invalid credentials")]
    InvalidCredentials,
    
    #[error("User not found")]
    UserNotFound,
    
    #[error("Session expired")]
    SessionExpired,
    
    #[error("Session revoked")]
    SessionRevoked,
    
    #[error("Invalid session")]
    InvalidSession,
    
    #[error("Password too weak: {0}")]
    WeakPassword(String),
    
    #[error("Invalid username: {0}")]
    InvalidUsername(String),
    
    #[error("Invalid email format")]
    InvalidEmail,
    
    #[error("Database error: {0}")]
    Database(#[from] sqlx::Error),
    
    #[error("Password hashing failed: {0}")]
    HashingFailed(String),
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct User {
    pub id: Uuid,
    pub username: String,
    pub display_name: String,
    pub email: String,
    pub avatar_url: Option<String>,
    pub status: String,
    pub created_at: DateTime<Utc>,
    pub last_seen_at: Option<DateTime<Utc>>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UserSession {
    pub id: Uuid,
    pub user_id: Uuid,
    pub token: String,
    pub expires_at: DateTime<Utc>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct SignupRequest {
    pub username: String,
    pub display_name: String,
    pub email: String,
    pub password: String,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct LoginRequest {
    pub username_or_email: String,
    pub password: String,
    pub device_info: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AuthResponse {
    pub user: User,
    pub session: UserSession,
}

pub struct UserService {
    pool: PgPool,
}

impl UserService {
    pub fn new(pool: PgPool) -> Self {
        Self { pool }
    }
    
    fn validate_username(username: &str) -> Result<(), AuthError> {
        if username.len() < 3 {
            return Err(AuthError::InvalidUsername("Must be at least 3 characters".to_string()));
        }
        if username.len() > 32 {
            return Err(AuthError::InvalidUsername("Must be 32 characters or less".to_string()));
        }
        if !username.chars().all(|c| c.is_alphanumeric() || c == '_') {
            return Err(AuthError::InvalidUsername("Only alphanumeric and underscore allowed".to_string()));
        }
        Ok(())
    }
    
    fn validate_password(password: &str) -> Result<(), AuthError> {
        if password.len() < 8 {
            return Err(AuthError::WeakPassword("Must be at least 8 characters".to_string()));
        }
        if !password.chars().any(|c| c.is_uppercase()) {
            return Err(AuthError::WeakPassword("Must contain uppercase letter".to_string()));
        }
        if !password.chars().any(|c| c.is_lowercase()) {
            return Err(AuthError::WeakPassword("Must contain lowercase letter".to_string()));
        }
        if !password.chars().any(|c| c.is_numeric()) {
            return Err(AuthError::WeakPassword("Must contain a number".to_string()));
        }
        Ok(())
    }
    
    fn validate_email(email: &str) -> Result<(), AuthError> {
        if !email.contains('@') || !email.contains('.') {
            return Err(AuthError::InvalidEmail);
        }
        Ok(())
    }
    
    fn hash_password(password: &str) -> Result<String, AuthError> {
        let salt = SaltString::generate(&mut OsRng);
        let argon2 = Argon2::default();
        argon2
            .hash_password(password.as_bytes(), &salt)
            .map(|h| h.to_string())
            .map_err(|e| AuthError::HashingFailed(e.to_string()))
    }
    
    fn verify_password(password: &str, hash: &str) -> bool {
        let parsed = match PasswordHash::new(hash) {
            Ok(h) => h,
            Err(_) => return false,
        };
        Argon2::default()
            .verify_password(password.as_bytes(), &parsed)
            .is_ok()
    }
    
    fn generate_session_token() -> String {
        let random_bytes: [u8; 32] = rand::random();
        hex::encode(random_bytes)
    }
    
    fn hash_token(token: &str) -> String {
        let mut hasher = Sha256::new();
        hasher.update(token.as_bytes());
        hex::encode(hasher.finalize())
    }
    
    pub async fn signup(&self, req: SignupRequest) -> Result<AuthResponse, AuthError> {
        Self::validate_username(&req.username)?;
        Self::validate_password(&req.password)?;
        Self::validate_email(&req.email)?;
        
        let existing = sqlx::query_scalar::<_, i64>(
            "SELECT COUNT(*) FROM users WHERE username = $1"
        )
        .bind(&req.username)
        .fetch_one(&self.pool)
        .await?;
        
        if existing > 0 {
            return Err(AuthError::UsernameExists);
        }
        
        let existing_email = sqlx::query_scalar::<_, i64>(
            "SELECT COUNT(*) FROM users WHERE email = $1"
        )
        .bind(&req.email)
        .fetch_one(&self.pool)
        .await?;
        
        if existing_email > 0 {
            return Err(AuthError::EmailExists);
        }
        
        let password_hash = Self::hash_password(&req.password)?;
        
        let user_id: Uuid = sqlx::query_scalar(
            r#"
            INSERT INTO users (username, display_name, email, password_hash)
            VALUES ($1, $2, $3, $4)
            RETURNING id
            "#
        )
        .bind(&req.username)
        .bind(&req.display_name)
        .bind(&req.email)
        .bind(&password_hash)
        .fetch_one(&self.pool)
        .await?;
        
        info!("New user registered: {} ({})", req.username, user_id);
        
        let user = User {
            id: user_id,
            username: req.username,
            display_name: req.display_name,
            email: req.email,
            avatar_url: None,
            status: "online".to_string(),
            created_at: Utc::now(),
            last_seen_at: Some(Utc::now()),
        };
        
        let session = self.create_session(user_id, None, None).await?;
        
        Ok(AuthResponse { user, session })
    }
    
    pub async fn login(&self, req: LoginRequest) -> Result<AuthResponse, AuthError> {
        let row = sqlx::query_as::<_, (Uuid, String, String, String, Option<String>, String, DateTime<Utc>, Option<DateTime<Utc>>, String)>(
            r#"
            SELECT id, username, display_name, email, avatar_url, status, created_at, last_seen_at, password_hash
            FROM users
            WHERE username = $1 OR email = $1
            "#
        )
        .bind(&req.username_or_email)
        .fetch_optional(&self.pool)
        .await?;
        
        let (id, username, display_name, email, avatar_url, _status, created_at, _last_seen_at, password_hash) = 
            row.ok_or(AuthError::InvalidCredentials)?;
        
        if !Self::verify_password(&req.password, &password_hash) {
            warn!("Failed login attempt for: {}", req.username_or_email);
            return Err(AuthError::InvalidCredentials);
        }
        
        sqlx::query("UPDATE users SET status = 'online', last_seen_at = NOW() WHERE id = $1")
            .bind(id)
            .execute(&self.pool)
            .await?;
        
        let user = User {
            id,
            username,
            display_name,
            email,
            avatar_url,
            status: "online".to_string(),
            created_at,
            last_seen_at: Some(Utc::now()),
        };
        
        let session = self.create_session(id, req.device_info.as_deref(), None).await?;
        
        info!("User logged in: {} ({})", user.username, user.id);
        
        Ok(AuthResponse { user, session })
    }
    
    async fn create_session(&self, user_id: Uuid, device_info: Option<&str>, ip: Option<&str>) -> Result<UserSession, AuthError> {
        let token = Self::generate_session_token();
        let token_hash = Self::hash_token(&token);
        let expires_at = Utc::now() + Duration::days(30);
        
        let session_id: Uuid = sqlx::query_scalar(
            r#"
            INSERT INTO user_sessions (user_id, token_hash, device_info, ip_address, expires_at)
            VALUES ($1, $2, $3, $4, $5)
            RETURNING id
            "#
        )
        .bind(user_id)
        .bind(&token_hash)
        .bind(device_info)
        .bind(ip)
        .bind(expires_at)
        .fetch_one(&self.pool)
        .await?;
        
        Ok(UserSession {
            id: session_id,
            user_id,
            token,
            expires_at,
        })
    }
    
    pub async fn validate_session(&self, token: &str) -> Result<User, AuthError> {
        let token_hash = Self::hash_token(token);
        
        let session = sqlx::query_as::<_, (Uuid, DateTime<Utc>, Option<DateTime<Utc>>)>(
            "SELECT user_id, expires_at, revoked_at FROM user_sessions WHERE token_hash = $1"
        )
        .bind(&token_hash)
        .fetch_optional(&self.pool)
        .await?
        .ok_or(AuthError::InvalidSession)?;
        
        if session.2.is_some() {
            return Err(AuthError::SessionRevoked);
        }
        
        if session.1 < Utc::now() {
            return Err(AuthError::SessionExpired);
        }
        
        self.get_user(session.0).await
    }
    
    pub async fn logout(&self, token: &str) -> Result<(), AuthError> {
        let token_hash = Self::hash_token(token);
        
        sqlx::query("UPDATE user_sessions SET revoked_at = NOW() WHERE token_hash = $1")
            .bind(&token_hash)
            .execute(&self.pool)
            .await?;
        
        Ok(())
    }
    
    pub async fn get_user(&self, user_id: Uuid) -> Result<User, AuthError> {
        let row = sqlx::query_as::<_, (Uuid, String, String, String, Option<String>, String, DateTime<Utc>, Option<DateTime<Utc>>)>(
            "SELECT id, username, display_name, email, avatar_url, status, created_at, last_seen_at FROM users WHERE id = $1"
        )
        .bind(user_id)
        .fetch_optional(&self.pool)
        .await?
        .ok_or(AuthError::UserNotFound)?;
        
        Ok(User {
            id: row.0,
            username: row.1,
            display_name: row.2,
            email: row.3,
            avatar_url: row.4,
            status: row.5,
            created_at: row.6,
            last_seen_at: row.7,
        })
    }
    
    pub async fn get_user_by_username(&self, username: &str) -> Result<User, AuthError> {
        let row = sqlx::query_as::<_, (Uuid, String, String, String, Option<String>, String, DateTime<Utc>, Option<DateTime<Utc>>)>(
            "SELECT id, username, display_name, email, avatar_url, status, created_at, last_seen_at FROM users WHERE username = $1"
        )
        .bind(username)
        .fetch_optional(&self.pool)
        .await?
        .ok_or(AuthError::UserNotFound)?;
        
        Ok(User {
            id: row.0,
            username: row.1,
            display_name: row.2,
            email: row.3,
            avatar_url: row.4,
            status: row.5,
            created_at: row.6,
            last_seen_at: row.7,
        })
    }
    
    pub async fn update_status(&self, user_id: Uuid, status: &str) -> Result<(), AuthError> {
        sqlx::query("UPDATE users SET status = $1, last_seen_at = NOW(), updated_at = NOW() WHERE id = $2")
            .bind(status)
            .bind(user_id)
            .execute(&self.pool)
            .await?;
        Ok(())
    }
    
    pub async fn update_profile(&self, user_id: Uuid, display_name: Option<&str>, avatar_url: Option<&str>) -> Result<User, AuthError> {
        if let Some(name) = display_name {
            sqlx::query("UPDATE users SET display_name = $1, updated_at = NOW() WHERE id = $2")
                .bind(name)
                .bind(user_id)
                .execute(&self.pool)
                .await?;
        }
        
        if let Some(url) = avatar_url {
            sqlx::query("UPDATE users SET avatar_url = $1, updated_at = NOW() WHERE id = $2")
                .bind(url)
                .bind(user_id)
                .execute(&self.pool)
                .await?;
        }
        
        self.get_user(user_id).await
    }
    
    pub async fn search_users(&self, query: &str, limit: i64) -> Result<Vec<User>, AuthError> {
        let pattern = format!("%{}%", query);
        
        let rows = sqlx::query_as::<_, (Uuid, String, String, String, Option<String>, String, DateTime<Utc>, Option<DateTime<Utc>>)>(
            r#"
            SELECT id, username, display_name, email, avatar_url, status, created_at, last_seen_at 
            FROM users 
            WHERE username ILIKE $1 OR display_name ILIKE $1
            LIMIT $2
            "#
        )
        .bind(&pattern)
        .bind(limit)
        .fetch_all(&self.pool)
        .await?;
        
        Ok(rows.into_iter().map(|r| User {
            id: r.0,
            username: r.1,
            display_name: r.2,
            email: r.3,
            avatar_url: r.4,
            status: r.5,
            created_at: r.6,
            last_seen_at: r.7,
        }).collect())
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    
    #[test]
    fn test_username_validation() {
        assert!(UserService::validate_username("valid_user").is_ok());
        assert!(UserService::validate_username("ab").is_err());
        assert!(UserService::validate_username("invalid user").is_err());
    }
    
    #[test]
    fn test_password_validation() {
        assert!(UserService::validate_password("Str0ngPass").is_ok());
        assert!(UserService::validate_password("weak").is_err());
        assert!(UserService::validate_password("nouppercase1").is_err());
    }
    
    #[test]
    fn test_token_generation() {
        let token = UserService::generate_session_token();
        assert_eq!(token.len(), 64);
    }
}
