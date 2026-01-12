use serde::{Deserialize, Serialize};
use std::path::{Path, PathBuf};
use chrono::{DateTime, Utc};
use parking_lot::RwLock;
use sha2::{Sha256, Digest};
use tokio::fs;
use flate2::write::GzEncoder;
use flate2::read::GzDecoder;
use flate2::Compression;
use std::io::{Read, Write};
use walkdir::WalkDir;

const MAX_SNAPSHOTS: usize = 10;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Snapshot {
    pub id: String,
    pub name: String,
    pub world_path: String,
    pub created_at: DateTime<Utc>,
    pub size_bytes: u64,
    pub file_count: usize,
    pub checksum: String,
    pub description: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SnapshotConfig {
    pub snapshot_dir: PathBuf,
    pub max_snapshots: usize,
    pub auto_snapshot_enabled: bool,
    pub auto_snapshot_interval_minutes: u32,
}

impl Default for SnapshotConfig {
    fn default() -> Self {
        let snapshot_dir = directories::ProjectDirs::from("com", "yellowtale", "YellowTale")
            .map(|dirs| dirs.data_dir().join("snapshots"))
            .unwrap_or_else(|| PathBuf::from(".data/snapshots"));
        
        Self {
            snapshot_dir,
            max_snapshots: MAX_SNAPSHOTS,
            auto_snapshot_enabled: true,
            auto_snapshot_interval_minutes: 30,
        }
    }
}

pub struct SaveSnapshotManager {
    config: RwLock<SnapshotConfig>,
    snapshots: RwLock<Vec<Snapshot>>,
}

impl SaveSnapshotManager {
    pub fn new() -> Self {
        Self {
            config: RwLock::new(SnapshotConfig::default()),
            snapshots: RwLock::new(Vec::new()),
        }
    }
    
    pub fn set_config(&self, config: SnapshotConfig) {
        *self.config.write() = config;
    }
    
    pub fn get_config(&self) -> SnapshotConfig {
        self.config.read().clone()
    }
    
    pub async fn initialize(&self) -> Result<(), String> {
        let config = self.config.read().clone();
        
        fs::create_dir_all(&config.snapshot_dir).await
            .map_err(|e| format!("Failed to create snapshot directory: {}", e))?;
        
        self.load_snapshots().await?;
        
        Ok(())
    }
    
    async fn load_snapshots(&self) -> Result<(), String> {
        let config = self.config.read().clone();
        let index_path = config.snapshot_dir.join("index.json");
        
        if index_path.exists() {
            let content = fs::read_to_string(&index_path).await
                .map_err(|e| e.to_string())?;
            
            if let Ok(snapshots) = serde_json::from_str::<Vec<Snapshot>>(&content) {
                *self.snapshots.write() = snapshots;
            }
        }
        
        Ok(())
    }
    
    async fn save_index(&self) -> Result<(), String> {
        let config = self.config.read().clone();
        let index_path = config.snapshot_dir.join("index.json");
        
        let snapshots = self.snapshots.read().clone();
        let content = serde_json::to_string_pretty(&snapshots)
            .map_err(|e| e.to_string())?;
        
        fs::write(&index_path, content).await
            .map_err(|e| e.to_string())?;
        
        Ok(())
    }
    
    pub async fn create_snapshot(
        &self,
        world_path: &Path,
        name: &str,
        description: Option<String>,
    ) -> Result<Snapshot, String> {
        if !world_path.exists() {
            return Err("World path does not exist".to_string());
        }
        
        let config = self.config.read().clone();
        let snapshot_id = uuid::Uuid::new_v4().to_string();
        let archive_name = format!("{}.tar.gz", snapshot_id);
        let archive_path = config.snapshot_dir.join(&archive_name);
        
        let (size, file_count, checksum) = self.create_archive(world_path, &archive_path).await?;
        
        let snapshot = Snapshot {
            id: snapshot_id,
            name: name.to_string(),
            world_path: world_path.to_string_lossy().to_string(),
            created_at: Utc::now(),
            size_bytes: size,
            file_count,
            checksum,
            description,
        };
        
        {
            let mut snapshots = self.snapshots.write();
            snapshots.push(snapshot.clone());
            
            while snapshots.len() > config.max_snapshots {
                if let Some(oldest) = snapshots.first().cloned() {
                    let old_path = config.snapshot_dir.join(format!("{}.tar.gz", oldest.id));
                    let _ = std::fs::remove_file(&old_path);
                    snapshots.remove(0);
                }
            }
        }
        
        self.save_index().await?;
        
        tracing::info!("Created snapshot '{}' ({} files, {} bytes)", name, file_count, size);
        
        Ok(snapshot)
    }
    
    async fn create_archive(&self, source: &Path, dest: &Path) -> Result<(u64, usize, String), String> {
        let source = source.to_path_buf();
        let dest = dest.to_path_buf();
        
        tokio::task::spawn_blocking(move || {
            let file = std::fs::File::create(&dest)
                .map_err(|e| format!("Failed to create archive: {}", e))?;
            
            let enc = GzEncoder::new(file, Compression::default());
            let mut tar = tar::Builder::new(enc);
            
            let mut hasher = Sha256::new();
            let mut file_count = 0;
            
            for entry in WalkDir::new(&source).into_iter().filter_map(|e| e.ok()) {
                let path = entry.path();
                if path.is_file() {
                    let relative = path.strip_prefix(&source).unwrap_or(path);
                    
                    if let Ok(content) = std::fs::read(path) {
                        hasher.update(&content);
                        file_count += 1;
                    }
                    
                    let _ = tar.append_path_with_name(path, relative);
                }
            }
            
            tar.finish().map_err(|e| format!("Failed to finalize archive: {}", e))?;
            
            let checksum = hex::encode(hasher.finalize());
            let size = std::fs::metadata(&dest).map(|m| m.len()).unwrap_or(0);
            
            Ok((size, file_count, checksum))
        }).await.map_err(|e| e.to_string())?
    }
    
    pub async fn restore_snapshot(&self, snapshot_id: &str, target_path: &Path) -> Result<(), String> {
        let config = self.config.read().clone();
        
        let snapshot = self.snapshots.read()
            .iter()
            .find(|s| s.id == snapshot_id)
            .cloned()
            .ok_or_else(|| "Snapshot not found".to_string())?;
        
        let archive_path = config.snapshot_dir.join(format!("{}.tar.gz", snapshot_id));
        
        if !archive_path.exists() {
            return Err("Snapshot archive not found".to_string());
        }
        
        if target_path.exists() {
            let backup_name = format!("{}_backup_{}", 
                target_path.file_name().and_then(|n| n.to_str()).unwrap_or("world"),
                Utc::now().format("%Y%m%d_%H%M%S")
            );
            let backup_path = target_path.parent()
                .unwrap_or(Path::new("."))
                .join(backup_name);
            
            fs::rename(target_path, &backup_path).await
                .map_err(|e| format!("Failed to backup current world: {}", e))?;
        }
        
        fs::create_dir_all(target_path).await
            .map_err(|e| format!("Failed to create target directory: {}", e))?;
        
        let archive = archive_path.clone();
        let target = target_path.to_path_buf();
        
        tokio::task::spawn_blocking(move || {
            let file = std::fs::File::open(&archive)
                .map_err(|e| format!("Failed to open archive: {}", e))?;
            
            let dec = GzDecoder::new(file);
            let mut tar = tar::Archive::new(dec);
            
            tar.unpack(&target)
                .map_err(|e| format!("Failed to extract archive: {}", e))?;
            
            Ok::<(), String>(())
        }).await.map_err(|e| e.to_string())??;
        
        tracing::info!("Restored snapshot '{}' to {:?}", snapshot.name, target_path);
        
        Ok(())
    }
    
    pub fn get_snapshots(&self) -> Vec<Snapshot> {
        self.snapshots.read().clone()
    }
    
    pub async fn delete_snapshot(&self, snapshot_id: &str) -> Result<(), String> {
        let config = self.config.read().clone();
        
        {
            let mut snapshots = self.snapshots.write();
            let index = snapshots.iter()
                .position(|s| s.id == snapshot_id)
                .ok_or_else(|| "Snapshot not found".to_string())?;
            
            let snapshot = snapshots.remove(index);
            
            let archive_path = config.snapshot_dir.join(format!("{}.tar.gz", snapshot.id));
            let _ = fs::remove_file(&archive_path).await;
        }
        
        self.save_index().await?;
        
        Ok(())
    }
}
