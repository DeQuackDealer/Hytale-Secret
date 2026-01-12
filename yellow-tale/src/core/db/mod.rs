use sqlx::{postgres::PgPoolOptions, PgPool, Error as SqlxError};
use thiserror::Error;
use tracing::{info, error};

#[derive(Error, Debug)]
pub enum DbError {
    #[error("Database connection failed: {0}")]
    ConnectionFailed(String),
    
    #[error("Migration failed: {0}")]
    MigrationFailed(String),
    
    #[error("Query failed: {0}")]
    QueryFailed(#[from] SqlxError),
    
    #[error("Environment variable DATABASE_URL not set")]
    MissingDatabaseUrl,
}

pub struct Database {
    pool: PgPool,
}

impl Database {
    pub async fn connect() -> Result<Self, DbError> {
        let database_url = std::env::var("DATABASE_URL")
            .map_err(|_| DbError::MissingDatabaseUrl)?;
        
        info!("Connecting to database...");
        
        let pool = PgPoolOptions::new()
            .max_connections(10)
            .connect(&database_url)
            .await
            .map_err(|e| DbError::ConnectionFailed(e.to_string()))?;
        
        info!("Database connected successfully");
        
        Ok(Self { pool })
    }
    
    pub fn pool(&self) -> &PgPool {
        &self.pool
    }
    
    pub async fn run_migrations(&self) -> Result<(), DbError> {
        info!("Running database migrations...");
        
        sqlx::query("CREATE EXTENSION IF NOT EXISTS pgcrypto")
            .execute(&self.pool)
            .await
            .ok();
        
        sqlx::query(r#"
            CREATE TABLE IF NOT EXISTS users (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                username VARCHAR(32) NOT NULL UNIQUE,
                display_name VARCHAR(64) NOT NULL,
                email VARCHAR(255) NOT NULL UNIQUE,
                password_hash VARCHAR(255) NOT NULL,
                avatar_url VARCHAR(512),
                status VARCHAR(32) DEFAULT 'offline',
                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                last_seen_at TIMESTAMPTZ
            )
        "#)
        .execute(&self.pool)
        .await
        .map_err(|e| DbError::MigrationFailed(e.to_string()))?;
        
        sqlx::query(r#"
            CREATE TABLE IF NOT EXISTS user_sessions (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                token_hash VARCHAR(255) NOT NULL UNIQUE,
                device_info VARCHAR(255),
                ip_address VARCHAR(45),
                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                expires_at TIMESTAMPTZ NOT NULL,
                revoked_at TIMESTAMPTZ
            )
        "#)
        .execute(&self.pool)
        .await
        .map_err(|e| DbError::MigrationFailed(e.to_string()))?;
        
        sqlx::query(r#"
            CREATE TABLE IF NOT EXISTS friendships (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                friend_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                status VARCHAR(32) NOT NULL DEFAULT 'pending',
                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                UNIQUE(user_id, friend_id)
            )
        "#)
        .execute(&self.pool)
        .await
        .map_err(|e| DbError::MigrationFailed(e.to_string()))?;
        
        sqlx::query(r#"
            CREATE TABLE IF NOT EXISTS blocks (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                blocker_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                blocked_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                reason VARCHAR(255),
                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                UNIQUE(blocker_id, blocked_id)
            )
        "#)
        .execute(&self.pool)
        .await
        .map_err(|e| DbError::MigrationFailed(e.to_string()))?;
        
        sqlx::query(r#"
            CREATE TABLE IF NOT EXISTS game_sessions (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                host_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                invite_code VARCHAR(14) NOT NULL UNIQUE,
                name VARCHAR(64) NOT NULL,
                max_players INT NOT NULL DEFAULT 8,
                is_public BOOLEAN NOT NULL DEFAULT false,
                game_type VARCHAR(64),
                status VARCHAR(32) NOT NULL DEFAULT 'waiting',
                relay_address VARCHAR(255),
                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                started_at TIMESTAMPTZ,
                ended_at TIMESTAMPTZ
            )
        "#)
        .execute(&self.pool)
        .await
        .map_err(|e| DbError::MigrationFailed(e.to_string()))?;
        
        sqlx::query(r#"
            CREATE TABLE IF NOT EXISTS session_participants (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                session_id UUID NOT NULL REFERENCES game_sessions(id) ON DELETE CASCADE,
                user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                role VARCHAR(32) NOT NULL DEFAULT 'player',
                joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                left_at TIMESTAMPTZ,
                UNIQUE(session_id, user_id)
            )
        "#)
        .execute(&self.pool)
        .await
        .map_err(|e| DbError::MigrationFailed(e.to_string()))?;
        
        let indexes = [
            "CREATE INDEX IF NOT EXISTS idx_users_username ON users(username)",
            "CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)",
            "CREATE INDEX IF NOT EXISTS idx_friendships_user ON friendships(user_id)",
            "CREATE INDEX IF NOT EXISTS idx_friendships_friend ON friendships(friend_id)",
            "CREATE INDEX IF NOT EXISTS idx_sessions_host ON game_sessions(host_id)",
            "CREATE INDEX IF NOT EXISTS idx_sessions_code ON game_sessions(invite_code)",
            "CREATE INDEX IF NOT EXISTS idx_participants_session ON session_participants(session_id)",
            "CREATE INDEX IF NOT EXISTS idx_participants_user ON session_participants(user_id)",
        ];
        
        for index_sql in indexes {
            sqlx::query(index_sql)
                .execute(&self.pool)
                .await
                .map_err(|e| DbError::MigrationFailed(e.to_string()))?;
        }
        
        info!("Database migrations completed");
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    #[test]
    fn test_db_module_exists() {
        assert!(true);
    }
}
