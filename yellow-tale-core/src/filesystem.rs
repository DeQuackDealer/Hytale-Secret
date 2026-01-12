use serde::{Deserialize, Serialize};
use std::path::{Path, PathBuf};
use thiserror::Error;
use sha2::{Sha256, Digest};

#[derive(Debug, Error)]
pub enum FsError {
    #[error("IO error: {0}")]
    IoError(#[from] std::io::Error),
    #[error("Path not found: {0}")]
    NotFound(PathBuf),
    #[error("Rollback point not found: {0}")]
    RollbackNotFound(String),
    #[error("Atomic operation failed: {0}")]
    AtomicFailed(String),
    #[error("Quota exceeded: used {used} bytes, limit {limit} bytes")]
    QuotaExceeded { used: u64, limit: u64 },
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RollbackPoint {
    pub id: String,
    pub name: String,
    pub created_at: chrono::DateTime<chrono::Utc>,
    pub files: Vec<FileSnapshot>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FileSnapshot {
    pub path: PathBuf,
    pub hash: String,
    pub size: u64,
}

pub struct FileSystem {
    root: PathBuf,
    rollback_dir: PathBuf,
    quota_bytes: Option<u64>,
}

impl FileSystem {
    pub fn new(root: PathBuf) -> Self {
        let rollback_dir = root.join(".rollback");
        Self {
            root,
            rollback_dir,
            quota_bytes: None,
        }
    }
    
    pub fn with_quota(mut self, bytes: u64) -> Self {
        self.quota_bytes = Some(bytes);
        self
    }
    
    pub fn init(&self) -> Result<(), FsError> {
        std::fs::create_dir_all(&self.root)?;
        std::fs::create_dir_all(&self.rollback_dir)?;
        std::fs::create_dir_all(self.root.join("profiles"))?;
        std::fs::create_dir_all(self.root.join("servers"))?;
        std::fs::create_dir_all(self.root.join("servers/local"))?;
        std::fs::create_dir_all(self.root.join("servers/remote"))?;
        std::fs::create_dir_all(self.root.join("pond"))?;
        std::fs::create_dir_all(self.root.join("pond/core"))?;
        std::fs::create_dir_all(self.root.join("pond/plugins"))?;
        std::fs::create_dir_all(self.root.join("pond/assets"))?;
        std::fs::create_dir_all(self.root.join("cache"))?;
        std::fs::create_dir_all(self.root.join("cache/assets"))?;
        std::fs::create_dir_all(self.root.join("cache/mods"))?;
        std::fs::create_dir_all(self.root.join("logs"))?;
        Ok(())
    }
    
    pub fn profiles_dir(&self) -> PathBuf {
        self.root.join("profiles")
    }
    
    pub fn servers_dir(&self) -> PathBuf {
        self.root.join("servers")
    }
    
    pub fn local_servers_dir(&self) -> PathBuf {
        self.root.join("servers/local")
    }
    
    pub fn remote_servers_dir(&self) -> PathBuf {
        self.root.join("servers/remote")
    }
    
    pub fn pond_dir(&self) -> PathBuf {
        self.root.join("pond")
    }
    
    pub fn plugins_dir(&self) -> PathBuf {
        self.root.join("pond/plugins")
    }
    
    pub fn cache_dir(&self) -> PathBuf {
        self.root.join("cache")
    }
    
    pub fn logs_dir(&self) -> PathBuf {
        self.root.join("logs")
    }
    
    pub fn calculate_size(&self) -> Result<u64, FsError> {
        calculate_dir_size(&self.root)
    }
    
    pub fn check_quota(&self, additional_bytes: u64) -> Result<(), FsError> {
        if let Some(quota) = self.quota_bytes {
            let current = self.calculate_size()?;
            let total = current + additional_bytes;
            if total > quota {
                return Err(FsError::QuotaExceeded { used: total, limit: quota });
            }
        }
        Ok(())
    }
    
    pub fn atomic_write(&self, path: &Path, content: &[u8]) -> Result<(), FsError> {
        self.check_quota(content.len() as u64)?;
        
        let temp_path = path.with_extension("tmp");
        
        if let Some(parent) = path.parent() {
            std::fs::create_dir_all(parent)?;
        }
        
        std::fs::write(&temp_path, content)?;
        std::fs::rename(&temp_path, path)?;
        
        Ok(())
    }
    
    pub fn atomic_copy(&self, src: &Path, dst: &Path) -> Result<(), FsError> {
        let content = std::fs::read(src)?;
        self.atomic_write(dst, &content)
    }
    
    pub fn create_rollback_point(&self, name: &str, paths: &[PathBuf]) -> Result<RollbackPoint, FsError> {
        let id = uuid::Uuid::new_v4().to_string();
        let rollback_dir = self.rollback_dir.join(&id);
        std::fs::create_dir_all(&rollback_dir)?;
        
        let mut files = Vec::new();
        
        for path in paths {
            if path.exists() {
                let relative = path.strip_prefix(&self.root).unwrap_or(path);
                let backup_path = rollback_dir.join(relative);
                
                if let Some(parent) = backup_path.parent() {
                    std::fs::create_dir_all(parent)?;
                }
                
                std::fs::copy(path, &backup_path)?;
                
                let content = std::fs::read(path)?;
                let hash = hex::encode(Sha256::digest(&content));
                let size = content.len() as u64;
                
                files.push(FileSnapshot {
                    path: relative.to_path_buf(),
                    hash,
                    size,
                });
            }
        }
        
        let point = RollbackPoint {
            id: id.clone(),
            name: name.to_string(),
            created_at: chrono::Utc::now(),
            files,
        };
        
        let manifest_path = rollback_dir.join("manifest.json");
        let manifest = serde_json::to_string_pretty(&point)
            .map_err(|e| FsError::AtomicFailed(e.to_string()))?;
        std::fs::write(manifest_path, manifest)?;
        
        Ok(point)
    }
    
    pub fn list_rollback_points(&self) -> Result<Vec<RollbackPoint>, FsError> {
        let mut points = Vec::new();
        
        if !self.rollback_dir.exists() {
            return Ok(points);
        }
        
        for entry in std::fs::read_dir(&self.rollback_dir)? {
            let entry = entry?;
            let manifest_path = entry.path().join("manifest.json");
            
            if manifest_path.exists() {
                let content = std::fs::read_to_string(&manifest_path)?;
                if let Ok(point) = serde_json::from_str(&content) {
                    points.push(point);
                }
            }
        }
        
        points.sort_by(|a, b| b.created_at.cmp(&a.created_at));
        
        Ok(points)
    }
    
    pub fn restore_rollback(&self, id: &str) -> Result<(), FsError> {
        let rollback_dir = self.rollback_dir.join(id);
        let manifest_path = rollback_dir.join("manifest.json");
        
        if !manifest_path.exists() {
            return Err(FsError::RollbackNotFound(id.to_string()));
        }
        
        let content = std::fs::read_to_string(&manifest_path)?;
        let point: RollbackPoint = serde_json::from_str(&content)
            .map_err(|e| FsError::AtomicFailed(e.to_string()))?;
        
        for file in &point.files {
            let backup_path = rollback_dir.join(&file.path);
            let restore_path = self.root.join(&file.path);
            
            if backup_path.exists() {
                if let Some(parent) = restore_path.parent() {
                    std::fs::create_dir_all(parent)?;
                }
                std::fs::copy(&backup_path, &restore_path)?;
            }
        }
        
        Ok(())
    }
    
    pub fn delete_rollback(&self, id: &str) -> Result<(), FsError> {
        let rollback_dir = self.rollback_dir.join(id);
        if rollback_dir.exists() {
            std::fs::remove_dir_all(rollback_dir)?;
        }
        Ok(())
    }
    
    pub fn cleanup_old(&self, max_age_days: i64) -> Result<u64, FsError> {
        let cutoff = chrono::Utc::now() - chrono::Duration::days(max_age_days);
        let mut freed = 0u64;
        
        for point in self.list_rollback_points()? {
            if point.created_at < cutoff {
                let rollback_dir = self.rollback_dir.join(&point.id);
                if rollback_dir.exists() {
                    freed += calculate_dir_size(&rollback_dir)?;
                    std::fs::remove_dir_all(rollback_dir)?;
                }
            }
        }
        
        let cache_dir = self.cache_dir();
        if cache_dir.exists() {
            for entry in std::fs::read_dir(&cache_dir)? {
                let entry = entry?;
                let metadata = entry.metadata()?;
                
                if let Ok(modified) = metadata.modified() {
                    let modified: chrono::DateTime<chrono::Utc> = modified.into();
                    if modified < cutoff {
                        let size = if metadata.is_dir() {
                            calculate_dir_size(&entry.path())?
                        } else {
                            metadata.len()
                        };
                        
                        if metadata.is_dir() {
                            std::fs::remove_dir_all(entry.path())?;
                        } else {
                            std::fs::remove_file(entry.path())?;
                        }
                        
                        freed += size;
                    }
                }
            }
        }
        
        Ok(freed)
    }
}

fn calculate_dir_size(path: &Path) -> Result<u64, FsError> {
    let mut size = 0u64;
    
    if path.is_file() {
        return Ok(std::fs::metadata(path)?.len());
    }
    
    if path.is_dir() {
        for entry in std::fs::read_dir(path)? {
            let entry = entry?;
            let metadata = entry.metadata()?;
            
            if metadata.is_dir() {
                size += calculate_dir_size(&entry.path())?;
            } else {
                size += metadata.len();
            }
        }
    }
    
    Ok(size)
}
