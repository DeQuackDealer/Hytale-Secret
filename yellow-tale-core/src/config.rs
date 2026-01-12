use serde::{Deserialize, Serialize};
use std::path::PathBuf;
use thiserror::Error;

#[derive(Debug, Error)]
pub enum ConfigError {
    #[error("Failed to read config file: {0}")]
    ReadError(#[from] std::io::Error),
    #[error("Failed to parse TOML: {0}")]
    TomlError(#[from] toml::de::Error),
    #[error("Failed to parse JSON: {0}")]
    JsonError(#[from] serde_json::Error),
    #[error("Unsupported config format")]
    UnsupportedFormat,
    #[error("Validation error: {0}")]
    ValidationError(String),
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Config {
    pub general: GeneralConfig,
    pub performance: PerformanceConfig,
    pub network: NetworkConfig,
    pub paths: PathConfig,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GeneralConfig {
    pub language: String,
    pub theme: String,
    pub auto_update: bool,
    pub telemetry: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PerformanceConfig {
    pub ram_allocation_mb: u32,
    pub jvm_args: Vec<String>,
    pub gc_preset: GcPreset,
    pub use_dedicated_gpu: bool,
    pub fps_limit: Option<u32>,
    pub vsync: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum GcPreset {
    Default,
    LowLatency,
    HighThroughput,
    LowMemory,
    Custom(Vec<String>),
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct NetworkConfig {
    pub api_url: String,
    pub cdn_url: String,
    pub relay_servers: Vec<String>,
    pub timeout_ms: u32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PathConfig {
    pub game_path: Option<PathBuf>,
    pub profiles_dir: PathBuf,
    pub servers_dir: PathBuf,
    pub cache_dir: PathBuf,
    pub logs_dir: PathBuf,
    pub pond_plugins_dir: PathBuf,
}

impl Default for Config {
    fn default() -> Self {
        let home = dirs_home();
        Self {
            general: GeneralConfig {
                language: "en".to_string(),
                theme: "dark".to_string(),
                auto_update: true,
                telemetry: false,
            },
            performance: PerformanceConfig {
                ram_allocation_mb: 4096,
                jvm_args: vec![],
                gc_preset: GcPreset::Default,
                use_dedicated_gpu: true,
                fps_limit: None,
                vsync: true,
            },
            network: NetworkConfig {
                api_url: "https://api.yellowtale.com".to_string(),
                cdn_url: "https://cdn.yellowtale.com".to_string(),
                relay_servers: vec![],
                timeout_ms: 30000,
            },
            paths: PathConfig {
                game_path: None,
                profiles_dir: home.join("profiles"),
                servers_dir: home.join("servers"),
                cache_dir: home.join("cache"),
                logs_dir: home.join("logs"),
                pond_plugins_dir: home.join("pond").join("plugins"),
            },
        }
    }
}

impl Config {
    pub fn load(path: &std::path::Path) -> Result<Self, ConfigError> {
        let content = std::fs::read_to_string(path)?;
        
        let ext = path.extension().and_then(|e| e.to_str()).unwrap_or("");
        
        match ext {
            "toml" => Ok(toml::from_str(&content)?),
            "json" => Ok(serde_json::from_str(&content)?),
            _ => Err(ConfigError::UnsupportedFormat),
        }
    }
    
    pub fn save(&self, path: &std::path::Path) -> Result<(), ConfigError> {
        let ext = path.extension().and_then(|e| e.to_str()).unwrap_or("");
        
        let content = match ext {
            "toml" => toml::to_string_pretty(self).map_err(|e| ConfigError::ValidationError(e.to_string()))?,
            "json" => serde_json::to_string_pretty(self)?,
            _ => return Err(ConfigError::UnsupportedFormat),
        };
        
        if let Some(parent) = path.parent() {
            std::fs::create_dir_all(parent)?;
        }
        
        std::fs::write(path, content)?;
        Ok(())
    }
    
    pub fn validate(&self) -> Result<(), ConfigError> {
        if self.performance.ram_allocation_mb < 512 {
            return Err(ConfigError::ValidationError("RAM allocation must be at least 512 MB".to_string()));
        }
        
        if self.performance.ram_allocation_mb > 32768 {
            return Err(ConfigError::ValidationError("RAM allocation cannot exceed 32 GB".to_string()));
        }
        
        Ok(())
    }
    
    pub fn generate_jvm_args(&self) -> Vec<String> {
        let mut args = vec![
            format!("-Xmx{}M", self.performance.ram_allocation_mb),
            format!("-Xms{}", self.performance.ram_allocation_mb / 2),
        ];
        
        match &self.performance.gc_preset {
            GcPreset::Default => {}
            GcPreset::LowLatency => {
                args.extend([
                    "-XX:+UseG1GC".to_string(),
                    "-XX:MaxGCPauseMillis=20".to_string(),
                    "-XX:G1HeapRegionSize=32M".to_string(),
                    "-XX:+ParallelRefProcEnabled".to_string(),
                ]);
            }
            GcPreset::HighThroughput => {
                args.extend([
                    "-XX:+UseParallelGC".to_string(),
                    "-XX:ParallelGCThreads=4".to_string(),
                ]);
            }
            GcPreset::LowMemory => {
                args.extend([
                    "-XX:+UseSerialGC".to_string(),
                ]);
            }
            GcPreset::Custom(custom_args) => {
                args.extend(custom_args.clone());
            }
        }
        
        args.extend(self.performance.jvm_args.clone());
        
        args
    }
}

fn dirs_home() -> PathBuf {
    #[cfg(target_os = "windows")]
    {
        std::env::var("APPDATA")
            .map(PathBuf::from)
            .unwrap_or_else(|_| PathBuf::from("."))
            .join("YellowTale")
    }
    
    #[cfg(target_os = "macos")]
    {
        std::env::var("HOME")
            .map(PathBuf::from)
            .unwrap_or_else(|_| PathBuf::from("."))
            .join("Library")
            .join("Application Support")
            .join("YellowTale")
    }
    
    #[cfg(target_os = "linux")]
    {
        std::env::var("HOME")
            .map(PathBuf::from)
            .unwrap_or_else(|_| PathBuf::from("."))
            .join(".yellowtale")
    }
    
    #[cfg(not(any(target_os = "windows", target_os = "macos", target_os = "linux")))]
    {
        PathBuf::from(".yellowtale")
    }
}
