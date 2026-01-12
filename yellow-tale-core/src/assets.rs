use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::path::{Path, PathBuf};
use sha2::{Sha256, Digest};
use thiserror::Error;

#[derive(Debug, Error)]
pub enum AssetError {
    #[error("Asset not found: {0}")]
    NotFound(String),
    #[error("Hash mismatch: expected {expected}, got {actual}")]
    HashMismatch { expected: String, actual: String },
    #[error("IO error: {0}")]
    IoError(#[from] std::io::Error),
    #[error("Invalid manifest: {0}")]
    InvalidManifest(String),
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AssetManifest {
    pub version: String,
    pub created_at: chrono::DateTime<chrono::Utc>,
    pub assets: Vec<AssetEntry>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AssetEntry {
    pub id: String,
    pub name: String,
    pub path: String,
    pub hash: String,
    pub size: u64,
    pub asset_type: AssetType,
    pub dependencies: Vec<String>,
    pub metadata: HashMap<String, serde_json::Value>,
}

#[derive(Debug, Clone, Copy, Serialize, Deserialize, PartialEq)]
pub enum AssetType {
    Skin,
    Accessory,
    UiPack,
    SoundPack,
    Texture,
    Model,
    Config,
    Plugin,
    Other,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AssetPackage {
    pub id: String,
    pub name: String,
    pub version: String,
    pub author: String,
    pub description: String,
    pub manifest: AssetManifest,
    pub compatibility: Vec<String>,
}

pub struct AssetRegistry {
    root_dir: PathBuf,
    assets: HashMap<String, AssetEntry>,
    manifests: HashMap<String, AssetManifest>,
}

impl AssetRegistry {
    pub fn new(root_dir: PathBuf) -> Self {
        Self {
            root_dir,
            assets: HashMap::new(),
            manifests: HashMap::new(),
        }
    }
    
    pub fn load_manifests(&mut self) -> Result<(), AssetError> {
        let manifest_dir = self.root_dir.join("manifests");
        
        if !manifest_dir.exists() {
            std::fs::create_dir_all(&manifest_dir)?;
            return Ok(());
        }
        
        for entry in std::fs::read_dir(&manifest_dir)? {
            let entry = entry?;
            let path = entry.path();
            
            if path.extension().map(|e| e == "json").unwrap_or(false) {
                let content = std::fs::read_to_string(&path)?;
                let manifest: AssetManifest = serde_json::from_str(&content)
                    .map_err(|e| AssetError::InvalidManifest(e.to_string()))?;
                
                for asset in &manifest.assets {
                    self.assets.insert(asset.id.clone(), asset.clone());
                }
                
                let name = path.file_stem()
                    .and_then(|s| s.to_str())
                    .unwrap_or("unknown")
                    .to_string();
                
                self.manifests.insert(name, manifest);
            }
        }
        
        Ok(())
    }
    
    pub fn get(&self, id: &str) -> Option<&AssetEntry> {
        self.assets.get(id)
    }
    
    pub fn get_path(&self, id: &str) -> Option<PathBuf> {
        self.assets.get(id).map(|a| self.root_dir.join(&a.path))
    }
    
    pub fn list_by_type(&self, asset_type: AssetType) -> Vec<&AssetEntry> {
        self.assets.values().filter(|a| a.asset_type == asset_type).collect()
    }
    
    pub fn verify(&self, id: &str) -> Result<bool, AssetError> {
        let asset = self.assets.get(id)
            .ok_or_else(|| AssetError::NotFound(id.to_string()))?;
        
        let path = self.root_dir.join(&asset.path);
        if !path.exists() {
            return Ok(false);
        }
        
        let content = std::fs::read(&path)?;
        let hash = hex::encode(Sha256::digest(&content));
        
        Ok(hash == asset.hash)
    }
    
    pub fn verify_all(&self) -> HashMap<String, bool> {
        self.assets.keys()
            .map(|id| (id.clone(), self.verify(id).unwrap_or(false)))
            .collect()
    }
    
    pub fn register(&mut self, entry: AssetEntry) -> Result<(), AssetError> {
        let path = self.root_dir.join(&entry.path);
        
        if !path.exists() {
            return Err(AssetError::NotFound(entry.path.clone()));
        }
        
        let content = std::fs::read(&path)?;
        let actual_hash = hex::encode(Sha256::digest(&content));
        
        if actual_hash != entry.hash {
            return Err(AssetError::HashMismatch {
                expected: entry.hash.clone(),
                actual: actual_hash,
            });
        }
        
        self.assets.insert(entry.id.clone(), entry);
        Ok(())
    }
    
    pub fn unregister(&mut self, id: &str) -> Option<AssetEntry> {
        self.assets.remove(id)
    }
    
    pub fn compute_hash(path: &Path) -> Result<String, AssetError> {
        let content = std::fs::read(path)?;
        Ok(hex::encode(Sha256::digest(&content)))
    }
    
    pub fn create_entry(
        id: &str,
        name: &str,
        path: &Path,
        asset_type: AssetType,
    ) -> Result<AssetEntry, AssetError> {
        let content = std::fs::read(path)?;
        let hash = hex::encode(Sha256::digest(&content));
        let size = content.len() as u64;
        
        Ok(AssetEntry {
            id: id.to_string(),
            name: name.to_string(),
            path: path.to_string_lossy().to_string(),
            hash,
            size,
            asset_type,
            dependencies: Vec::new(),
            metadata: HashMap::new(),
        })
    }
    
    pub fn save_manifest(&self, name: &str) -> Result<(), AssetError> {
        let manifest_dir = self.root_dir.join("manifests");
        std::fs::create_dir_all(&manifest_dir)?;
        
        let manifest = AssetManifest {
            version: "1.0.0".to_string(),
            created_at: chrono::Utc::now(),
            assets: self.assets.values().cloned().collect(),
        };
        
        let path = manifest_dir.join(format!("{}.json", name));
        let content = serde_json::to_string_pretty(&manifest)
            .map_err(|e| AssetError::InvalidManifest(e.to_string()))?;
        
        std::fs::write(path, content)?;
        Ok(())
    }
    
    pub fn resolve_dependencies(&self, id: &str) -> Vec<&AssetEntry> {
        let mut resolved = Vec::new();
        let mut visited = std::collections::HashSet::new();
        
        self.resolve_deps_recursive(id, &mut resolved, &mut visited);
        
        resolved
    }
    
    fn resolve_deps_recursive<'a>(
        &'a self,
        id: &str,
        resolved: &mut Vec<&'a AssetEntry>,
        visited: &mut std::collections::HashSet<String>,
    ) {
        if visited.contains(id) {
            return;
        }
        
        visited.insert(id.to_string());
        
        if let Some(asset) = self.assets.get(id) {
            for dep_id in &asset.dependencies {
                self.resolve_deps_recursive(dep_id, resolved, visited);
            }
            resolved.push(asset);
        }
    }
}
