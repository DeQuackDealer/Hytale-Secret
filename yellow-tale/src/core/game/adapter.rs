use async_trait::async_trait;
use serde::{Deserialize, Serialize};
use std::path::PathBuf;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GameAdapterConfig {
    pub game_path: PathBuf,
    pub data_path: PathBuf,
    pub version: String,
    pub launch_args: Vec<String>,
    pub environment: std::collections::HashMap<String, String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AdapterCapabilities {
    pub supports_mods: bool,
    pub supports_server_list: bool,
    pub supports_direct_connect: bool,
    pub supports_relay: bool,
    pub supports_cosmetics: bool,
    pub supports_profiles: bool,
    pub mod_format: Option<String>,
    pub protocol_version: Option<u32>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GameInfo {
    pub name: String,
    pub version: String,
    pub executable: PathBuf,
    pub install_path: PathBuf,
    pub is_running: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LaunchOptions {
    pub server_address: Option<String>,
    pub profile_id: Option<String>,
    pub mods: Vec<String>,
    pub performance_preset: Option<String>,
    pub custom_args: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LaunchResult {
    pub success: bool,
    pub process_id: Option<u32>,
    pub error: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ServerInfo {
    pub address: String,
    pub port: u16,
    pub name: String,
    pub player_count: u32,
    pub max_players: u32,
    pub ping_ms: u32,
    pub version: String,
    pub motd: Option<String>,
}

#[async_trait]
pub trait GameAdapter: Send + Sync {
    fn name(&self) -> &str;
    fn capabilities(&self) -> AdapterCapabilities;
    
    async fn detect_installation(&self) -> Option<GameInfo>;
    async fn get_version(&self) -> Result<String, AdapterError>;
    
    async fn launch(&self, options: LaunchOptions) -> Result<LaunchResult, AdapterError>;
    async fn is_running(&self) -> bool;
    async fn terminate(&self) -> Result<(), AdapterError>;
    
    async fn ping_server(&self, address: &str, port: u16) -> Result<ServerInfo, AdapterError>;
    async fn query_server_list(&self) -> Result<Vec<ServerInfo>, AdapterError>;
    
    async fn get_mod_list(&self) -> Result<Vec<ModInfo>, AdapterError>;
    async fn validate_mod(&self, path: &PathBuf) -> Result<ModInfo, AdapterError>;
    
    async fn get_asset_manifest(&self) -> Result<GameAssetManifest, AdapterError>;
    async fn verify_assets(&self) -> Result<AssetVerifyResult, AdapterError>;
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModInfo {
    pub id: String,
    pub name: String,
    pub version: String,
    pub path: PathBuf,
    pub dependencies: Vec<String>,
    pub conflicts: Vec<String>,
    pub enabled: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GameAssetManifest {
    pub version: String,
    pub total_size_bytes: u64,
    pub assets: Vec<GameAsset>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GameAsset {
    pub path: String,
    pub hash: String,
    pub size_bytes: u64,
    pub asset_type: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AssetVerifyResult {
    pub valid: bool,
    pub missing: Vec<String>,
    pub corrupted: Vec<String>,
    pub extra: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum AdapterError {
    NotInstalled,
    NotRunning,
    AlreadyRunning,
    LaunchFailed(String),
    NetworkError(String),
    ModError(String),
    AssetError(String),
    Unsupported(String),
    Unknown(String),
}

impl std::fmt::Display for AdapterError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::NotInstalled => write!(f, "Game not installed"),
            Self::NotRunning => write!(f, "Game not running"),
            Self::AlreadyRunning => write!(f, "Game already running"),
            Self::LaunchFailed(e) => write!(f, "Launch failed: {}", e),
            Self::NetworkError(e) => write!(f, "Network error: {}", e),
            Self::ModError(e) => write!(f, "Mod error: {}", e),
            Self::AssetError(e) => write!(f, "Asset error: {}", e),
            Self::Unsupported(e) => write!(f, "Unsupported: {}", e),
            Self::Unknown(e) => write!(f, "Unknown error: {}", e),
        }
    }
}

impl std::error::Error for AdapterError {}

pub struct HytaleAdapter {
    config: GameAdapterConfig,
    process_id: std::sync::atomic::AtomicU32,
}

impl HytaleAdapter {
    pub fn new(config: GameAdapterConfig) -> Self {
        Self {
            config,
            process_id: std::sync::atomic::AtomicU32::new(0),
        }
    }
    
    pub fn with_defaults() -> Self {
        Self::new(GameAdapterConfig {
            game_path: PathBuf::from(""),
            data_path: PathBuf::from(""),
            version: "0.0.0".to_string(),
            launch_args: vec![],
            environment: std::collections::HashMap::new(),
        })
    }
}

#[async_trait]
impl GameAdapter for HytaleAdapter {
    fn name(&self) -> &str {
        "Hytale"
    }
    
    fn capabilities(&self) -> AdapterCapabilities {
        AdapterCapabilities {
            supports_mods: true,
            supports_server_list: true,
            supports_direct_connect: true,
            supports_relay: true,
            supports_cosmetics: true,
            supports_profiles: true,
            mod_format: Some("hytale".to_string()),
            protocol_version: None,
        }
    }
    
    async fn detect_installation(&self) -> Option<GameInfo> {
        None
    }
    
    async fn get_version(&self) -> Result<String, AdapterError> {
        Ok(self.config.version.clone())
    }
    
    async fn launch(&self, _options: LaunchOptions) -> Result<LaunchResult, AdapterError> {
        Err(AdapterError::NotInstalled)
    }
    
    async fn is_running(&self) -> bool {
        self.process_id.load(std::sync::atomic::Ordering::Relaxed) != 0
    }
    
    async fn terminate(&self) -> Result<(), AdapterError> {
        self.process_id.store(0, std::sync::atomic::Ordering::Relaxed);
        Ok(())
    }
    
    async fn ping_server(&self, address: &str, port: u16) -> Result<ServerInfo, AdapterError> {
        Ok(ServerInfo {
            address: address.to_string(),
            port,
            name: "Unknown".to_string(),
            player_count: 0,
            max_players: 0,
            ping_ms: 0,
            version: "0.0.0".to_string(),
            motd: None,
        })
    }
    
    async fn query_server_list(&self) -> Result<Vec<ServerInfo>, AdapterError> {
        Ok(vec![])
    }
    
    async fn get_mod_list(&self) -> Result<Vec<ModInfo>, AdapterError> {
        Ok(vec![])
    }
    
    async fn validate_mod(&self, path: &PathBuf) -> Result<ModInfo, AdapterError> {
        Ok(ModInfo {
            id: "unknown".to_string(),
            name: path.file_name().map(|s| s.to_string_lossy().to_string()).unwrap_or_default(),
            version: "0.0.0".to_string(),
            path: path.clone(),
            dependencies: vec![],
            conflicts: vec![],
            enabled: false,
        })
    }
    
    async fn get_asset_manifest(&self) -> Result<GameAssetManifest, AdapterError> {
        Ok(GameAssetManifest {
            version: self.config.version.clone(),
            total_size_bytes: 0,
            assets: vec![],
        })
    }
    
    async fn verify_assets(&self) -> Result<AssetVerifyResult, AdapterError> {
        Ok(AssetVerifyResult {
            valid: true,
            missing: vec![],
            corrupted: vec![],
            extra: vec![],
        })
    }
}
