use dashmap::DashMap;
use std::sync::atomic::{AtomicU64, Ordering};
use ahash::RandomState;

pub struct WorldHeatmap {
    regions: DashMap<(i32, i32, String), RegionStats, RandomState>,
    region_size: i32,
    decay_factor: f64,
    last_decay_tick: AtomicU64,
    decay_interval_ticks: u64,
}

struct RegionStats {
    activity_score: AtomicU64,
    player_visits: AtomicU64,
    entity_count: AtomicU64,
    block_changes: AtomicU64,
}

impl RegionStats {
    fn new() -> Self {
        Self {
            activity_score: AtomicU64::new(0),
            player_visits: AtomicU64::new(0),
            entity_count: AtomicU64::new(0),
            block_changes: AtomicU64::new(0),
        }
    }

    fn apply_decay(&self, factor: f64) {
        let current = self.activity_score.load(Ordering::Relaxed);
        let decayed = (current as f64 * factor) as u64;
        self.activity_score.store(decayed, Ordering::Relaxed);
    }
}

impl WorldHeatmap {
    pub fn new(region_size: i32) -> Self {
        Self {
            regions: DashMap::with_hasher(RandomState::new()),
            region_size,
            decay_factor: 0.99,
            last_decay_tick: AtomicU64::new(0),
            decay_interval_ticks: 20,
        }
    }

    fn get_region_key(&self, x: f64, z: f64, world: &str) -> (i32, i32, String) {
        let region_x = (x as i32).div_euclid(self.region_size);
        let region_z = (z as i32).div_euclid(self.region_size);
        (region_x, region_z, world.to_string())
    }

    pub fn record_player_position(&self, x: f64, z: f64, world: &str) {
        let key = self.get_region_key(x, z, world);
        let stats = self.regions.entry(key).or_insert_with(RegionStats::new);
        stats.activity_score.fetch_add(1, Ordering::Relaxed);
        stats.player_visits.fetch_add(1, Ordering::Relaxed);
    }

    pub fn record_entity_spawn(&self, x: f64, z: f64, world: &str) {
        let key = self.get_region_key(x, z, world);
        let stats = self.regions.entry(key).or_insert_with(RegionStats::new);
        stats.activity_score.fetch_add(2, Ordering::Relaxed);
        stats.entity_count.fetch_add(1, Ordering::Relaxed);
    }

    pub fn record_block_change(&self, x: f64, z: f64, world: &str) {
        let key = self.get_region_key(x, z, world);
        let stats = self.regions.entry(key).or_insert_with(RegionStats::new);
        stats.activity_score.fetch_add(5, Ordering::Relaxed);
        stats.block_changes.fetch_add(1, Ordering::Relaxed);
    }

    pub fn get_region_activity(&self, x: f64, z: f64, world: &str) -> u64 {
        let key = self.get_region_key(x, z, world);
        self.regions.get(&key)
            .map(|s| s.activity_score.load(Ordering::Relaxed))
            .unwrap_or(0)
    }

    pub fn get_hotspots(&self, world: &str, top_n: usize) -> Vec<HotspotInfo> {
        let mut hotspots: Vec<_> = self.regions.iter()
            .filter(|e| e.key().2 == world)
            .map(|e| {
                let (rx, rz, _) = e.key();
                HotspotInfo {
                    region_x: *rx,
                    region_z: *rz,
                    center_x: (*rx * self.region_size + self.region_size / 2) as f64,
                    center_z: (*rz * self.region_size + self.region_size / 2) as f64,
                    activity_score: e.value().activity_score.load(Ordering::Relaxed),
                    player_visits: e.value().player_visits.load(Ordering::Relaxed),
                }
            })
            .collect();

        hotspots.sort_by(|a, b| b.activity_score.cmp(&a.activity_score));
        hotspots.truncate(top_n);
        hotspots
    }

    pub fn tick(&self, current_tick: u64) {
        let last_decay = self.last_decay_tick.load(Ordering::Relaxed);
        if current_tick - last_decay >= self.decay_interval_ticks {
            self.last_decay_tick.store(current_tick, Ordering::Relaxed);
            
            for entry in self.regions.iter() {
                entry.value().apply_decay(self.decay_factor);
            }
            
            self.regions.retain(|_, stats| {
                stats.activity_score.load(Ordering::Relaxed) > 0
            });
        }
    }

    pub fn region_count(&self) -> usize {
        self.regions.len()
    }

    pub fn clear(&self) {
        self.regions.clear();
    }
}

#[derive(Debug, Clone)]
pub struct HotspotInfo {
    pub region_x: i32,
    pub region_z: i32,
    pub center_x: f64,
    pub center_z: f64,
    pub activity_score: u64,
    pub player_visits: u64,
}
