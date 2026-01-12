use parking_lot::RwLock;
use serde::{Deserialize, Serialize};
use std::path::PathBuf;
use tracing::info;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ServerConfig {
    pub server: ServerSettings,
    pub plugins: PluginSettings,
    pub performance: PerformanceSettings,
    pub assets: AssetSettings,
    pub integration: IntegrationSettings,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ServerSettings {
    pub name: String,
    pub port: u16,
    pub max_players: u32,
    pub tick_rate: u32,
    pub description: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PluginSettings {
    pub directory: String,
    pub auto_load: bool,
    pub hot_reload: bool,
    pub sandbox_enabled: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PerformanceSettings {
    pub tick_budget_ms: f64,
    pub adaptive_throttling: bool,
    pub max_entities_per_tick: u32,
    pub max_chunk_updates_per_tick: u32,
    pub memory_pool_size_mb: u32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AssetSettings {
    pub max_cosmetic_size_mb: u32,
    pub allowed_types: Vec<String>,
    pub require_approval: bool,
    pub cache_directory: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct IntegrationSettings {
    pub enabled: bool,
    pub launcher_api_port: u16,
    pub advertise_capabilities: bool,
    pub accept_asset_manifests: bool,
}

impl Default for ServerConfig {
    fn default() -> Self {
        Self {
            server: ServerSettings {
                name: "Pond Server".to_string(),
                port: 25565,
                max_players: 100,
                tick_rate: 20,
                description: "A Pond-powered server".to_string(),
            },
            plugins: PluginSettings {
                directory: "plugins".to_string(),
                auto_load: true,
                hot_reload: true,
                sandbox_enabled: true,
            },
            performance: PerformanceSettings {
                tick_budget_ms: 50.0,
                adaptive_throttling: true,
                max_entities_per_tick: 100,
                max_chunk_updates_per_tick: 50,
                memory_pool_size_mb: 256,
            },
            assets: AssetSettings {
                max_cosmetic_size_mb: 5,
                allowed_types: vec![
                    "skin".to_string(),
                    "cape".to_string(),
                    "hat".to_string(),
                ],
                require_approval: true,
                cache_directory: "cache/assets".to_string(),
            },
            integration: IntegrationSettings {
                enabled: true,
                launcher_api_port: 25566,
                advertise_capabilities: true,
                accept_asset_manifests: true,
            },
        }
    }
}

pub struct ConfigManager {
    path: PathBuf,
    config: RwLock<ServerConfig>,
    version: RwLock<u64>,
}

impl ConfigManager {
    pub fn new(path: &str) -> Result<Self, String> {
        let path = PathBuf::from(path);
        
        let config = if path.exists() {
            let content = std::fs::read_to_string(&path)
                .map_err(|e| format!("Failed to read config: {}", e))?;
            toml::from_str(&content)
                .map_err(|e| format!("Failed to parse config: {}", e))?
        } else {
            let config = ServerConfig::default();
            let content = toml::to_string_pretty(&config)
                .map_err(|e| format!("Failed to serialize config: {}", e))?;
            std::fs::write(&path, &content)
                .map_err(|e| format!("Failed to write config: {}", e))?;
            info!("Created default config at {:?}", path);
            config
        };
        
        Ok(Self {
            path,
            config: RwLock::new(config),
            version: RwLock::new(1),
        })
    }
    
    pub fn reload(&self) -> Result<(), String> {
        let content = std::fs::read_to_string(&self.path)
            .map_err(|e| format!("Failed to read config: {}", e))?;
        let new_config: ServerConfig = toml::from_str(&content)
            .map_err(|e| format!("Failed to parse config: {}", e))?;
        
        *self.config.write() = new_config;
        *self.version.write() += 1;
        
        info!("Configuration reloaded");
        Ok(())
    }
    
    pub fn save(&self) -> Result<(), String> {
        let config = self.config.read();
        let content = toml::to_string_pretty(&*config)
            .map_err(|e| format!("Failed to serialize config: {}", e))?;
        std::fs::write(&self.path, &content)
            .map_err(|e| format!("Failed to write config: {}", e))?;
        info!("Configuration saved");
        Ok(())
    }
    
    pub fn get(&self) -> ServerConfig {
        self.config.read().clone()
    }
    
    pub fn get_string(&self, key: &str) -> Option<String> {
        let config = self.config.read();
        match key {
            "server.name" => Some(config.server.name.clone()),
            "server.description" => Some(config.server.description.clone()),
            "plugins.directory" => Some(config.plugins.directory.clone()),
            "assets.cache_directory" => Some(config.assets.cache_directory.clone()),
            _ => None,
        }
    }
    
    pub fn get_int(&self, key: &str) -> Option<i64> {
        let config = self.config.read();
        match key {
            "server.port" => Some(config.server.port as i64),
            "server.max_players" => Some(config.server.max_players as i64),
            "server.tick_rate" => Some(config.server.tick_rate as i64),
            "performance.max_entities_per_tick" => Some(config.performance.max_entities_per_tick as i64),
            "integration.launcher_api_port" => Some(config.integration.launcher_api_port as i64),
            _ => None,
        }
    }
    
    pub fn get_float(&self, key: &str) -> Option<f64> {
        let config = self.config.read();
        match key {
            "performance.tick_budget_ms" => Some(config.performance.tick_budget_ms),
            _ => None,
        }
    }
    
    pub fn get_bool(&self, key: &str) -> Option<bool> {
        let config = self.config.read();
        match key {
            "plugins.auto_load" => Some(config.plugins.auto_load),
            "plugins.hot_reload" => Some(config.plugins.hot_reload),
            "plugins.sandbox_enabled" => Some(config.plugins.sandbox_enabled),
            "performance.adaptive_throttling" => Some(config.performance.adaptive_throttling),
            "assets.require_approval" => Some(config.assets.require_approval),
            "integration.enabled" => Some(config.integration.enabled),
            "integration.advertise_capabilities" => Some(config.integration.advertise_capabilities),
            "integration.accept_asset_manifests" => Some(config.integration.accept_asset_manifests),
            _ => None,
        }
    }
    
    pub fn version(&self) -> u64 {
        *self.version.read()
    }
}
