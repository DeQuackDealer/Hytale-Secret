use crate::core::telemetry::TelemetryCollector;
use dashmap::DashMap;
use std::sync::Arc;
use std::sync::atomic::{AtomicBool, AtomicU64, Ordering};
use tokio::sync::RwLock;
use tracing::info;

#[derive(Debug, Clone)]
pub struct PerformanceMetrics {
    pub avg_tick_ms: f64,
    pub max_tick_ms: f64,
    pub min_tick_ms: f64,
    pub tps: f64,
    pub memory_used_mb: f64,
    pub memory_allocated_mb: f64,
    pub task_metrics: Vec<TaskMetrics>,
}

#[derive(Debug, Clone)]
pub struct TaskMetrics {
    pub name: String,
    pub avg_duration_ms: f64,
    pub max_duration_ms: f64,
    pub call_count: u64,
    pub budget_exceeded_count: u64,
}

struct TickStats {
    durations: Vec<f64>,
    last_reset: std::time::Instant,
}

impl TickStats {
    fn new() -> Self {
        Self {
            durations: Vec::with_capacity(1000),
            last_reset: std::time::Instant::now(),
        }
    }
}

pub struct PerformanceMonitor {
    telemetry: Arc<TelemetryCollector>,
    tick_stats: RwLock<TickStats>,
    task_durations: DashMap<String, Vec<f64>>,
    running: AtomicBool,
    tick_count: AtomicU64,
    entity_budget: RwLock<EntityBudget>,
}

#[derive(Debug, Clone)]
pub struct EntityBudget {
    pub max_entities_per_tick: u32,
    pub max_chunk_updates_per_tick: u32,
    pub throttle_threshold: f64,
    pub adaptive: bool,
}

impl Default for EntityBudget {
    fn default() -> Self {
        Self {
            max_entities_per_tick: 100,
            max_chunk_updates_per_tick: 50,
            throttle_threshold: 45.0,
            adaptive: true,
        }
    }
}

impl PerformanceMonitor {
    pub fn new(telemetry: Arc<TelemetryCollector>) -> Self {
        Self {
            telemetry,
            tick_stats: RwLock::new(TickStats::new()),
            task_durations: DashMap::new(),
            running: AtomicBool::new(false),
            tick_count: AtomicU64::new(0),
            entity_budget: RwLock::new(EntityBudget::default()),
        }
    }
    
    pub async fn start_monitoring(&self) {
        self.running.store(true, Ordering::SeqCst);
        info!("Performance monitoring started");
    }
    
    pub async fn stop_monitoring(&self) {
        self.running.store(false, Ordering::SeqCst);
        info!("Performance monitoring stopped");
    }
    
    pub async fn record_tick_duration(&self, duration_ms: f64) {
        if !self.running.load(Ordering::Relaxed) {
            return;
        }
        
        self.tick_count.fetch_add(1, Ordering::Relaxed);
        
        let mut stats = self.tick_stats.write().await;
        stats.durations.push(duration_ms);
        
        if stats.durations.len() > 1200 {
            stats.durations.drain(0..200);
        }
        
        if stats.last_reset.elapsed().as_secs() >= 60 {
            let metrics = self.calculate_metrics_internal(&stats).await;
            self.telemetry.record_performance_snapshot(metrics).await;
            stats.last_reset = std::time::Instant::now();
        }
        
        if duration_ms > 45.0 {
            self.adjust_entity_budget(duration_ms).await;
        }
    }
    
    pub async fn record_task_duration(&self, task_name: &str, duration_ms: f64) {
        let mut durations = self.task_durations.entry(task_name.to_string())
            .or_insert_with(Vec::new);
        durations.push(duration_ms);
        
        if durations.len() > 100 {
            durations.drain(0..50);
        }
    }
    
    async fn calculate_metrics_internal(&self, stats: &TickStats) -> PerformanceMetrics {
        let durations = &stats.durations;
        
        let avg = if durations.is_empty() { 0.0 } else {
            durations.iter().sum::<f64>() / durations.len() as f64
        };
        let max = durations.iter().cloned().fold(0.0, f64::max);
        let min = durations.iter().cloned().fold(f64::INFINITY, f64::min);
        let min = if min.is_infinite() { 0.0 } else { min };
        
        let tps = if avg > 0.0 { 1000.0 / avg } else { 20.0 };
        let tps = tps.min(20.0);
        
        let task_metrics: Vec<TaskMetrics> = self.task_durations.iter()
            .map(|entry| {
                let name = entry.key().clone();
                let durs = entry.value();
                let avg = if durs.is_empty() { 0.0 } else {
                    durs.iter().sum::<f64>() / durs.len() as f64
                };
                let max = durs.iter().cloned().fold(0.0, f64::max);
                TaskMetrics {
                    name,
                    avg_duration_ms: avg,
                    max_duration_ms: max,
                    call_count: durs.len() as u64,
                    budget_exceeded_count: durs.iter().filter(|&&d| d > 5.0).count() as u64,
                }
            })
            .collect();
        
        PerformanceMetrics {
            avg_tick_ms: avg,
            max_tick_ms: max,
            min_tick_ms: min,
            tps,
            memory_used_mb: 0.0,
            memory_allocated_mb: 0.0,
            task_metrics,
        }
    }
    
    pub async fn get_metrics(&self) -> PerformanceMetrics {
        let stats = self.tick_stats.read().await;
        self.calculate_metrics_internal(&stats).await
    }
    
    async fn adjust_entity_budget(&self, tick_duration: f64) {
        let mut budget = self.entity_budget.write().await;
        if !budget.adaptive {
            return;
        }
        
        if tick_duration > budget.throttle_threshold {
            let reduction = ((tick_duration - budget.throttle_threshold) / 10.0).min(0.5);
            budget.max_entities_per_tick = 
                ((budget.max_entities_per_tick as f64) * (1.0 - reduction)).max(10.0) as u32;
            budget.max_chunk_updates_per_tick = 
                ((budget.max_chunk_updates_per_tick as f64) * (1.0 - reduction)).max(5.0) as u32;
        } else if tick_duration < budget.throttle_threshold * 0.5 {
            budget.max_entities_per_tick = (budget.max_entities_per_tick + 5).min(200);
            budget.max_chunk_updates_per_tick = (budget.max_chunk_updates_per_tick + 2).min(100);
        }
    }
    
    pub async fn get_entity_budget(&self) -> EntityBudget {
        self.entity_budget.read().await.clone()
    }
    
    pub async fn set_entity_budget(&self, budget: EntityBudget) {
        *self.entity_budget.write().await = budget;
    }
}
