use async_trait::async_trait;
use serde::{Deserialize, Serialize};
use std::path::PathBuf;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum AssetType {
    Texture,
    Model,
    Sound,
    Animation,
    Shader,
    Script,
    Config,
    Localization,
    Unknown,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AssetManifest {
    pub version: String,
    pub base_url: Option<String>,
    pub entries: Vec<AssetEntry>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AssetEntry {
    pub path: String,
    pub hash: String,
    pub size: u64,
    pub asset_type: AssetType,
    pub compressed: bool,
    pub priority: u8,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AssetProgress {
    pub total: u64,
    pub downloaded: u64,
    pub current_file: Option<String>,
    pub speed_bytes_sec: u64,
    pub eta_seconds: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum AssetError {
    NotFound(String),
    DownloadFailed(String),
    HashMismatch { expected: String, got: String },
    IoError(String),
    Cancelled,
}

impl std::fmt::Display for AssetError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::NotFound(p) => write!(f, "Asset not found: {}", p),
            Self::DownloadFailed(e) => write!(f, "Download failed: {}", e),
            Self::HashMismatch { expected, got } => {
                write!(f, "Hash mismatch: expected {}, got {}", expected, got)
            }
            Self::IoError(e) => write!(f, "IO error: {}", e),
            Self::Cancelled => write!(f, "Download cancelled"),
        }
    }
}

impl std::error::Error for AssetError {}

#[async_trait]
pub trait AssetLoader: Send + Sync {
    async fn fetch_manifest(&self) -> Result<AssetManifest, AssetError>;
    async fn verify_local(&self, manifest: &AssetManifest) -> Vec<AssetEntry>;
    async fn download(&self, entries: &[AssetEntry], progress: tokio::sync::mpsc::Sender<AssetProgress>) -> Result<(), AssetError>;
    async fn cancel_download(&self);
    fn local_path(&self, entry: &AssetEntry) -> PathBuf;
}

pub struct HytaleAssetLoader {
    cache_dir: PathBuf,
    cancelled: std::sync::atomic::AtomicBool,
}

impl HytaleAssetLoader {
    pub fn new(cache_dir: PathBuf) -> Self {
        Self {
            cache_dir,
            cancelled: std::sync::atomic::AtomicBool::new(false),
        }
    }
}

#[async_trait]
impl AssetLoader for HytaleAssetLoader {
    async fn fetch_manifest(&self) -> Result<AssetManifest, AssetError> {
        Ok(AssetManifest {
            version: "0.0.0".to_string(),
            base_url: None,
            entries: vec![],
        })
    }
    
    async fn verify_local(&self, _manifest: &AssetManifest) -> Vec<AssetEntry> {
        vec![]
    }
    
    async fn download(&self, entries: &[AssetEntry], progress: tokio::sync::mpsc::Sender<AssetProgress>) -> Result<(), AssetError> {
        let total: u64 = entries.iter().map(|e| e.size).sum();
        let _ = progress.send(AssetProgress {
            total,
            downloaded: 0,
            current_file: None,
            speed_bytes_sec: 0,
            eta_seconds: 0,
        }).await;
        
        for entry in entries {
            if self.cancelled.load(std::sync::atomic::Ordering::Relaxed) {
                return Err(AssetError::Cancelled);
            }
            
            let _ = progress.send(AssetProgress {
                total,
                downloaded: entry.size,
                current_file: Some(entry.path.clone()),
                speed_bytes_sec: 0,
                eta_seconds: 0,
            }).await;
        }
        
        Ok(())
    }
    
    async fn cancel_download(&self) {
        self.cancelled.store(true, std::sync::atomic::Ordering::Relaxed);
    }
    
    fn local_path(&self, entry: &AssetEntry) -> PathBuf {
        self.cache_dir.join(&entry.path)
    }
}
