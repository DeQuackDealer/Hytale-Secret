use dashmap::DashMap;
use parking_lot::RwLock;
use std::sync::Arc;
use std::time::{Duration, Instant};
use ahash::RandomState;

pub struct LazyAssetLoader<T: Clone + Send + Sync> {
    cache: DashMap<String, CachedAsset<T>, RandomState>,
    loader: Arc<dyn Fn(&str) -> Option<T> + Send + Sync>,
    max_cache_size: usize,
    ttl: Duration,
    access_counts: DashMap<String, u64, RandomState>,
}

struct CachedAsset<T> {
    asset: T,
    loaded_at: Instant,
    last_accessed: RwLock<Instant>,
    size_bytes: usize,
}

impl<T: Clone + Send + Sync> LazyAssetLoader<T> {
    pub fn new(
        loader: impl Fn(&str) -> Option<T> + Send + Sync + 'static,
        max_cache_size: usize,
        ttl: Duration,
    ) -> Self {
        Self {
            cache: DashMap::with_hasher(RandomState::new()),
            loader: Arc::new(loader),
            max_cache_size,
            ttl,
            access_counts: DashMap::with_hasher(RandomState::new()),
        }
    }

    pub fn get(&self, key: &str) -> Option<T> {
        if let Some(cached) = self.cache.get(key) {
            if cached.loaded_at.elapsed() < self.ttl {
                *cached.last_accessed.write() = Instant::now();
                *self.access_counts.entry(key.to_string()).or_insert(0) += 1;
                return Some(cached.asset.clone());
            } else {
                drop(cached);
                self.cache.remove(key);
            }
        }

        self.load_and_cache(key)
    }

    pub fn get_or_load<F>(&self, key: &str, custom_loader: F) -> Option<T>
    where
        F: FnOnce() -> Option<T>,
    {
        if let Some(cached) = self.cache.get(key) {
            if cached.loaded_at.elapsed() < self.ttl {
                *cached.last_accessed.write() = Instant::now();
                return Some(cached.asset.clone());
            }
        }

        let asset = custom_loader()?;
        self.insert(key, asset.clone(), 0);
        Some(asset)
    }

    fn load_and_cache(&self, key: &str) -> Option<T> {
        let asset = (self.loader)(key)?;
        self.insert(key, asset.clone(), 0);
        Some(asset)
    }

    pub fn insert(&self, key: &str, asset: T, size_bytes: usize) {
        self.evict_if_needed();
        
        self.cache.insert(
            key.to_string(),
            CachedAsset {
                asset,
                loaded_at: Instant::now(),
                last_accessed: RwLock::new(Instant::now()),
                size_bytes,
            },
        );
    }

    pub fn invalidate(&self, key: &str) -> bool {
        self.cache.remove(key).is_some()
    }

    pub fn clear(&self) {
        self.cache.clear();
        self.access_counts.clear();
    }

    fn evict_if_needed(&self) {
        if self.cache.len() < self.max_cache_size {
            return;
        }

        let mut oldest_key: Option<String> = None;
        let mut oldest_time = Instant::now();

        for entry in self.cache.iter() {
            let last_accessed = *entry.value().last_accessed.read();
            if last_accessed < oldest_time {
                oldest_time = last_accessed;
                oldest_key = Some(entry.key().clone());
            }
        }

        if let Some(key) = oldest_key {
            self.cache.remove(&key);
        }
    }

    pub fn size(&self) -> usize {
        self.cache.len()
    }

    pub fn stats(&self) -> CacheStats {
        let total_size: usize = self.cache.iter()
            .map(|e| e.value().size_bytes)
            .sum();
        
        let total_accesses: u64 = self.access_counts.iter()
            .map(|e| *e.value())
            .sum();

        CacheStats {
            entries: self.cache.len(),
            total_size_bytes: total_size,
            total_accesses,
            hit_ratio: 0.0,
        }
    }
}

#[derive(Debug, Clone)]
pub struct CacheStats {
    pub entries: usize,
    pub total_size_bytes: usize,
    pub total_accesses: u64,
    pub hit_ratio: f64,
}
