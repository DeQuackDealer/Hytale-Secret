use serde::{Deserialize, Serialize};
use sha2::{Sha256, Digest};
use std::collections::HashMap;
use std::path::{Path, PathBuf};
use parking_lot::RwLock;
use tokio::fs;
use walkdir::WalkDir;

const DEFAULT_CACHE_SIZE_MB: u64 = 2048;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CachedAsset {
    pub hash: String,
    pub original_path: String,
    pub cached_path: String,
    pub size_bytes: u64,
    pub asset_type: AssetType,
    pub cached_at: chrono::DateTime<chrono::Utc>,
    pub last_accessed: chrono::DateTime<chrono::Utc>,
    pub access_count: u32,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum AssetType {
    Texture,
    Model,
    Shader,
    Audio,
    Script,
    Config,
    Other,
}

impl AssetType {
    pub fn from_extension(ext: &str) -> Self {
        match ext.to_lowercase().as_str() {
            "png" | "jpg" | "jpeg" | "dds" | "tga" | "bmp" | "ktx" | "ktx2" => Self::Texture,
            "obj" | "fbx" | "gltf" | "glb" | "dae" => Self::Model,
            "glsl" | "hlsl" | "spv" | "vert" | "frag" | "comp" | "shader" => Self::Shader,
            "ogg" | "wav" | "mp3" | "flac" | "opus" => Self::Audio,
            "lua" | "js" | "py" | "wasm" => Self::Script,
            "json" | "yaml" | "toml" | "xml" | "cfg" | "ini" => Self::Config,
            _ => Self::Other,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CacheStats {
    pub total_size_bytes: u64,
    pub max_size_bytes: u64,
    pub asset_count: usize,
    pub hit_count: u64,
    pub miss_count: u64,
    pub hit_rate: f64,
    pub deduplicated_bytes: u64,
}

pub struct AssetCache {
    cache_dir: RwLock<PathBuf>,
    assets: RwLock<HashMap<String, CachedAsset>>,
    hash_to_path: RwLock<HashMap<String, PathBuf>>,
    max_size_bytes: RwLock<u64>,
    hit_count: RwLock<u64>,
    miss_count: RwLock<u64>,
    deduplicated_bytes: RwLock<u64>,
}

impl AssetCache {
    pub fn new() -> Self {
        let cache_dir = directories::ProjectDirs::from("com", "yellowtale", "YellowTale")
            .map(|dirs| dirs.cache_dir().join("assets"))
            .unwrap_or_else(|| PathBuf::from(".cache/assets"));
        
        Self {
            cache_dir: RwLock::new(cache_dir),
            assets: RwLock::new(HashMap::new()),
            hash_to_path: RwLock::new(HashMap::new()),
            max_size_bytes: RwLock::new(DEFAULT_CACHE_SIZE_MB * 1024 * 1024),
            hit_count: RwLock::new(0),
            miss_count: RwLock::new(0),
            deduplicated_bytes: RwLock::new(0),
        }
    }
    
    pub fn set_cache_dir(&self, path: PathBuf) {
        *self.cache_dir.write() = path;
    }
    
    pub fn set_max_size_mb(&self, size_mb: u64) {
        *self.max_size_bytes.write() = size_mb * 1024 * 1024;
    }
    
    pub async fn initialize(&self) -> Result<(), String> {
        let cache_dir = self.cache_dir.read().clone();
        fs::create_dir_all(&cache_dir).await
            .map_err(|e| format!("Failed to create cache directory: {}", e))?;
        
        self.scan_cache().await?;
        Ok(())
    }
    
    async fn scan_cache(&self) -> Result<(), String> {
        let cache_dir = self.cache_dir.read().clone();
        let index_path = cache_dir.join("index.json");
        
        if index_path.exists() {
            if let Ok(content) = fs::read_to_string(&index_path).await {
                if let Ok(assets) = serde_json::from_str::<HashMap<String, CachedAsset>>(&content) {
                    *self.assets.write() = assets.clone();
                    
                    let mut hash_map = self.hash_to_path.write();
                    for asset in assets.values() {
                        hash_map.insert(asset.hash.clone(), PathBuf::from(&asset.cached_path));
                    }
                }
            }
        }
        
        Ok(())
    }
    
    async fn save_index(&self) -> Result<(), String> {
        let cache_dir = self.cache_dir.read().clone();
        let index_path = cache_dir.join("index.json");
        
        let assets = self.assets.read().clone();
        let content = serde_json::to_string_pretty(&assets)
            .map_err(|e| format!("Failed to serialize index: {}", e))?;
        
        fs::write(&index_path, content).await
            .map_err(|e| format!("Failed to write index: {}", e))?;
        
        Ok(())
    }
    
    pub async fn cache_asset(&self, source_path: &Path) -> Result<CachedAsset, String> {
        let content = fs::read(source_path).await
            .map_err(|e| format!("Failed to read asset: {}", e))?;
        
        let mut hasher = Sha256::new();
        hasher.update(&content);
        let hash = hex::encode(hasher.finalize());
        
        if let Some(existing_path) = self.hash_to_path.read().get(&hash) {
            *self.hit_count.write() += 1;
            *self.deduplicated_bytes.write() += content.len() as u64;
            
            if let Some(mut asset) = self.assets.write().get_mut(&hash) {
                asset.last_accessed = chrono::Utc::now();
                asset.access_count += 1;
                return Ok(asset.clone());
            }
        }
        
        *self.miss_count.write() += 1;
        
        self.ensure_space(content.len() as u64).await?;
        
        let ext = source_path.extension()
            .and_then(|e| e.to_str())
            .unwrap_or("");
        let asset_type = AssetType::from_extension(ext);
        
        let cache_dir = self.cache_dir.read().clone();
        let cached_filename = format!("{}.{}", &hash[..16], ext);
        let cached_path = cache_dir.join(&cached_filename);
        
        fs::write(&cached_path, &content).await
            .map_err(|e| format!("Failed to write cached asset: {}", e))?;
        
        let now = chrono::Utc::now();
        let asset = CachedAsset {
            hash: hash.clone(),
            original_path: source_path.to_string_lossy().to_string(),
            cached_path: cached_path.to_string_lossy().to_string(),
            size_bytes: content.len() as u64,
            asset_type,
            cached_at: now,
            last_accessed: now,
            access_count: 1,
        };
        
        self.assets.write().insert(hash.clone(), asset.clone());
        self.hash_to_path.write().insert(hash, cached_path);
        
        let _ = self.save_index().await;
        
        Ok(asset)
    }
    
    pub fn get_cached(&self, hash: &str) -> Option<CachedAsset> {
        let mut assets = self.assets.write();
        if let Some(asset) = assets.get_mut(hash) {
            *self.hit_count.write() += 1;
            asset.last_accessed = chrono::Utc::now();
            asset.access_count += 1;
            Some(asset.clone())
        } else {
            *self.miss_count.write() += 1;
            None
        }
    }
    
    pub async fn get_or_cache(&self, source_path: &Path) -> Result<CachedAsset, String> {
        let content = fs::read(source_path).await
            .map_err(|e| format!("Failed to read asset: {}", e))?;
        
        let mut hasher = Sha256::new();
        hasher.update(&content);
        let hash = hex::encode(hasher.finalize());
        
        if let Some(asset) = self.get_cached(&hash) {
            return Ok(asset);
        }
        
        self.cache_asset(source_path).await
    }
    
    async fn ensure_space(&self, needed_bytes: u64) -> Result<(), String> {
        let max_size = *self.max_size_bytes.read();
        let current_size = self.get_total_size();
        
        if current_size + needed_bytes <= max_size {
            return Ok(());
        }
        
        let mut assets: Vec<CachedAsset> = self.assets.read().values().cloned().collect();
        assets.sort_by(|a, b| a.last_accessed.cmp(&b.last_accessed));
        
        let mut freed = 0u64;
        let to_free = (current_size + needed_bytes).saturating_sub(max_size);
        
        for asset in assets {
            if freed >= to_free {
                break;
            }
            
            if let Err(e) = fs::remove_file(&asset.cached_path).await {
                tracing::warn!("Failed to remove cached asset {}: {}", asset.cached_path, e);
                continue;
            }
            
            self.assets.write().remove(&asset.hash);
            self.hash_to_path.write().remove(&asset.hash);
            freed += asset.size_bytes;
        }
        
        Ok(())
    }
    
    fn get_total_size(&self) -> u64 {
        self.assets.read().values().map(|a| a.size_bytes).sum()
    }
    
    pub fn get_stats(&self) -> CacheStats {
        let assets = self.assets.read();
        let total_size: u64 = assets.values().map(|a| a.size_bytes).sum();
        let hit_count = *self.hit_count.read();
        let miss_count = *self.miss_count.read();
        let total_requests = hit_count + miss_count;
        
        CacheStats {
            total_size_bytes: total_size,
            max_size_bytes: *self.max_size_bytes.read(),
            asset_count: assets.len(),
            hit_count,
            miss_count,
            hit_rate: if total_requests > 0 {
                hit_count as f64 / total_requests as f64
            } else {
                0.0
            },
            deduplicated_bytes: *self.deduplicated_bytes.read(),
        }
    }
    
    pub async fn clear(&self) -> Result<(), String> {
        let cache_dir = self.cache_dir.read().clone();
        
        if cache_dir.exists() {
            fs::remove_dir_all(&cache_dir).await
                .map_err(|e| format!("Failed to clear cache: {}", e))?;
            fs::create_dir_all(&cache_dir).await
                .map_err(|e| format!("Failed to recreate cache directory: {}", e))?;
        }
        
        self.assets.write().clear();
        self.hash_to_path.write().clear();
        *self.hit_count.write() = 0;
        *self.miss_count.write() = 0;
        *self.deduplicated_bytes.write() = 0;
        
        Ok(())
    }
    
    pub async fn prewarm_shaders(&self, shader_dir: &Path) -> Result<usize, String> {
        if !shader_dir.exists() {
            return Ok(0);
        }
        
        let mut count = 0;
        
        for entry in WalkDir::new(shader_dir).into_iter().filter_map(|e| e.ok()) {
            let path = entry.path();
            if !path.is_file() {
                continue;
            }
            
            let ext = path.extension().and_then(|e| e.to_str()).unwrap_or("");
            if matches!(ext, "glsl" | "hlsl" | "spv" | "vert" | "frag" | "comp" | "shader") {
                if let Ok(_) = self.cache_asset(path).await {
                    count += 1;
                }
            }
        }
        
        tracing::info!("Pre-warmed {} shaders", count);
        Ok(count)
    }
    
    pub async fn prewarm_textures(&self, texture_dir: &Path, max_count: usize) -> Result<usize, String> {
        if !texture_dir.exists() {
            return Ok(0);
        }
        
        let mut count = 0;
        
        for entry in WalkDir::new(texture_dir).into_iter().filter_map(|e| e.ok()) {
            if count >= max_count {
                break;
            }
            
            let path = entry.path();
            if !path.is_file() {
                continue;
            }
            
            let ext = path.extension().and_then(|e| e.to_str()).unwrap_or("");
            if matches!(ext, "png" | "jpg" | "jpeg" | "dds" | "tga" | "ktx" | "ktx2") {
                if let Ok(_) = self.cache_asset(path).await {
                    count += 1;
                }
            }
        }
        
        tracing::info!("Pre-warmed {} textures", count);
        Ok(count)
    }
}
