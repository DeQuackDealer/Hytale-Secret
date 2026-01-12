//! Configuration Module
//! 
//! Handles application configuration:
//! - TOML-based config files
//! - Versioned schemas with migration
//! - Default configuration

use std::path::Path;
use serde::{Deserialize, Serialize};
use thiserror::Error;
use semver::Version;

/// Current config schema version
pub const CONFIG_SCHEMA_VERSION: &str = "1.0.0";

#[derive(Error, Debug)]
pub enum ConfigError {
    #[error("Failed to load config: {0}")]
    LoadFailed(String),
    
    #[error("Failed to save config: {0}")]
    SaveFailed(String),
    
    #[error("Migration failed: {0}")]
    MigrationFailed(String),
    
    #[error("IO error: {0}")]
    IoError(#[from] std::io::Error),
    
    #[error("Parse error: {0}")]
    ParseError(#[from] toml::de::Error),
    
    #[error("Serialize error: {0}")]
    SerializeError(#[from] toml::ser::Error),
}

/// Cache configuration
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CacheConfig {
    /// Maximum cache size in bytes
    pub max_size_bytes: u64,
    
    /// Whether to enable background cache warming
    pub enable_warming: bool,
    
    /// Whether to verify integrity on access
    pub verify_integrity: bool,
}

impl Default for CacheConfig {
    fn default() -> Self {
        Self {
            max_size_bytes: 10 * 1024 * 1024 * 1024, // 10 GB
            enable_warming: true,
            verify_integrity: true,
        }
    }
}

/// Performance configuration
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PerformanceConfig {
    /// Default process priority
    pub default_priority: String,
    
    /// Whether to clear RAM before launch by default
    pub clear_ram_default: bool,
    
    /// Whether to warm disk cache by default
    pub warm_disk_default: bool,
}

impl Default for PerformanceConfig {
    fn default() -> Self {
        Self {
            default_priority: "normal".to_string(),
            clear_ram_default: false,
            warm_disk_default: true,
        }
    }
}

/// Session configuration
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SessionConfig {
    /// Preferred connection method (p2p, relay, hybrid)
    pub preferred_method: String,
    
    /// Maximum relay hops
    pub max_relay_hops: u8,
    
    /// P2P connection timeout in seconds
    pub p2p_timeout_secs: u64,
    
    /// Relay server addresses
    pub relay_servers: Vec<String>,
}

impl Default for SessionConfig {
    fn default() -> Self {
        Self {
            preferred_method: "hybrid".to_string(),
            max_relay_hops: 3,
            p2p_timeout_secs: 10,
            relay_servers: Vec::new(),
        }
    }
}

/// Telemetry configuration
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TelemetryConfig {
    /// Log level (trace, debug, info, warn, error)
    pub log_level: String,
    
    /// Whether to log to file
    pub log_to_file: bool,
    
    /// Maximum log file size in MB
    pub max_log_size_mb: u64,
    
    /// Number of log files to retain
    pub log_retention: u32,
}

impl Default for TelemetryConfig {
    fn default() -> Self {
        Self {
            log_level: "info".to_string(),
            log_to_file: true,
            max_log_size_mb: 50,
            log_retention: 5,
        }
    }
}

/// Main application configuration
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AppConfig {
    /// Schema version for migration
    pub schema_version: String,
    
    /// Cache settings
    pub cache: CacheConfig,
    
    /// Performance settings
    pub performance: PerformanceConfig,
    
    /// Session settings
    pub session: SessionConfig,
    
    /// Telemetry settings
    pub telemetry: TelemetryConfig,
    
    /// Path to game executable (global default)
    pub default_game_path: Option<String>,
}

impl Default for AppConfig {
    fn default() -> Self {
        Self {
            schema_version: CONFIG_SCHEMA_VERSION.to_string(),
            cache: CacheConfig::default(),
            performance: PerformanceConfig::default(),
            session: SessionConfig::default(),
            telemetry: TelemetryConfig::default(),
            default_game_path: None,
        }
    }
}

impl AppConfig {
    /// Load configuration from a file
    pub async fn load(path: &Path) -> Result<Self, ConfigError> {
        let content = tokio::fs::read_to_string(path).await?;
        let mut config: AppConfig = toml::from_str(&content)?;
        
        // Check if migration is needed
        if config.needs_migration() {
            config = Self::migrate(config)?;
        }
        
        Ok(config)
    }
    
    /// Save configuration to a file
    pub async fn save(&self, path: &Path) -> Result<(), ConfigError> {
        // Ensure parent directory exists
        if let Some(parent) = path.parent() {
            tokio::fs::create_dir_all(parent).await?;
        }
        
        let content = toml::to_string_pretty(self)?;
        tokio::fs::write(path, content).await?;
        
        Ok(())
    }
    
    /// Check if this config needs migration
    pub fn needs_migration(&self) -> bool {
        match (Version::parse(&self.schema_version), Version::parse(CONFIG_SCHEMA_VERSION)) {
            (Ok(current), Ok(target)) => current < target,
            _ => true,
        }
    }
    
    /// Migrate config to current schema version
    fn migrate(mut config: AppConfig) -> Result<AppConfig, ConfigError> {
        let from = Version::parse(&config.schema_version)
            .map_err(|e| ConfigError::MigrationFailed(e.to_string()))?;
        let to = Version::parse(CONFIG_SCHEMA_VERSION)
            .map_err(|e| ConfigError::MigrationFailed(e.to_string()))?;
        
        tracing::info!("Migrating config from v{} to v{}", from, to);
        
        // Add migration logic here as schema evolves
        // For now, just update version
        config.schema_version = CONFIG_SCHEMA_VERSION.to_string();
        
        Ok(config)
    }
    
    /// Generate default config file content
    pub fn default_toml() -> String {
        let config = AppConfig::default();
        toml::to_string_pretty(&config).unwrap_or_default()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    
    #[test]
    fn test_default_config() {
        let config = AppConfig::default();
        assert_eq!(config.schema_version, CONFIG_SCHEMA_VERSION);
        assert!(!config.needs_migration());
    }
    
    #[test]
    fn test_default_toml() {
        let toml = AppConfig::default_toml();
        assert!(toml.contains("schema_version"));
        assert!(toml.contains("[cache]"));
        assert!(toml.contains("[performance]"));
    }
}
