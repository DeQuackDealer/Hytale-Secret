use crate::core::performance::PerformanceMetrics;
use parking_lot::RwLock;
use std::collections::VecDeque;
use std::sync::atomic::{AtomicU64, Ordering};
use tracing::{debug, info};

#[derive(Debug, Clone)]
pub struct TelemetrySnapshot {
    pub timestamp: chrono::DateTime<chrono::Utc>,
    pub tick_count: u64,
    pub performance: Option<PerformanceMetrics>,
    pub plugin_count: u32,
    pub active_connections: u32,
    pub memory_used_mb: f64,
}

pub struct TelemetryCollector {
    snapshots: RwLock<VecDeque<TelemetrySnapshot>>,
    tick_count: AtomicU64,
    max_snapshots: usize,
    sample_interval_secs: u64,
    last_sample: RwLock<std::time::Instant>,
}

impl TelemetryCollector {
    pub fn new() -> Self {
        Self {
            snapshots: RwLock::new(VecDeque::with_capacity(1000)),
            tick_count: AtomicU64::new(0),
            max_snapshots: 1000,
            sample_interval_secs: 60,
            last_sample: RwLock::new(std::time::Instant::now()),
        }
    }
    
    pub async fn record_tick(&self) {
        self.tick_count.fetch_add(1, Ordering::Relaxed);
    }
    
    pub async fn record_performance_snapshot(&self, metrics: PerformanceMetrics) {
        let mut last_sample = self.last_sample.write();
        
        if last_sample.elapsed().as_secs() < self.sample_interval_secs {
            return;
        }
        
        *last_sample = std::time::Instant::now();
        
        let snapshot = TelemetrySnapshot {
            timestamp: chrono::Utc::now(),
            tick_count: self.tick_count.load(Ordering::Relaxed),
            performance: Some(metrics),
            plugin_count: 0,
            active_connections: 0,
            memory_used_mb: 0.0,
        };
        
        let mut snapshots = self.snapshots.write();
        if snapshots.len() >= self.max_snapshots {
            snapshots.pop_front();
        }
        snapshots.push_back(snapshot);
        
        debug!("Recorded telemetry snapshot");
    }
    
    pub fn get_recent_snapshots(&self, count: usize) -> Vec<TelemetrySnapshot> {
        let snapshots = self.snapshots.read();
        snapshots.iter()
            .rev()
            .take(count)
            .cloned()
            .collect()
    }
    
    pub fn get_snapshots_since(&self, since: chrono::DateTime<chrono::Utc>) -> Vec<TelemetrySnapshot> {
        let snapshots = self.snapshots.read();
        snapshots.iter()
            .filter(|s| s.timestamp > since)
            .cloned()
            .collect()
    }
    
    pub fn current_tick(&self) -> u64 {
        self.tick_count.load(Ordering::Relaxed)
    }
    
    pub fn export_metrics(&self) -> TelemetryExport {
        let snapshots = self.snapshots.read();
        
        let avg_tps = snapshots.iter()
            .filter_map(|s| s.performance.as_ref())
            .map(|p| p.tps)
            .sum::<f64>() / snapshots.len().max(1) as f64;
        
        let avg_tick_ms = snapshots.iter()
            .filter_map(|s| s.performance.as_ref())
            .map(|p| p.avg_tick_ms)
            .sum::<f64>() / snapshots.len().max(1) as f64;
        
        let max_tick_ms = snapshots.iter()
            .filter_map(|s| s.performance.as_ref())
            .map(|p| p.max_tick_ms)
            .fold(0.0, f64::max);
        
        TelemetryExport {
            total_ticks: self.tick_count.load(Ordering::Relaxed),
            snapshot_count: snapshots.len(),
            avg_tps,
            avg_tick_ms,
            max_tick_ms,
            uptime_secs: 0,
        }
    }
}

#[derive(Debug, Clone)]
pub struct TelemetryExport {
    pub total_ticks: u64,
    pub snapshot_count: usize,
    pub avg_tps: f64,
    pub avg_tick_ms: f64,
    pub max_tick_ms: f64,
    pub uptime_secs: u64,
}
