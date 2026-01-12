use crate::core::performance::PerformanceMonitor;
use dashmap::DashMap;
use std::sync::Arc;
use std::sync::atomic::{AtomicBool, AtomicU64, Ordering};
use tokio::sync::RwLock;
use tracing::{debug, warn};
use uuid::Uuid;

#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord)]
pub enum TaskPriority {
    Critical = 0,
    High = 1,
    Normal = 2,
    Low = 3,
    Background = 4,
}

#[derive(Debug, Clone)]
pub struct Task {
    pub id: Uuid,
    pub name: String,
    pub priority: TaskPriority,
    pub interval_ticks: u64,
    pub last_run: u64,
    pub enabled: bool,
    pub budget_ms: f64,
}

impl Task {
    pub fn new(name: impl Into<String>, priority: TaskPriority) -> Self {
        Self {
            id: Uuid::new_v4(),
            name: name.into(),
            priority,
            interval_ticks: 1,
            last_run: 0,
            enabled: true,
            budget_ms: 5.0,
        }
    }
    
    pub fn with_interval(mut self, ticks: u64) -> Self {
        self.interval_ticks = ticks;
        self
    }
    
    pub fn with_budget(mut self, ms: f64) -> Self {
        self.budget_ms = ms;
        self
    }
}

pub struct Scheduler {
    tasks: DashMap<Uuid, Task>,
    current_tick: AtomicU64,
    running: AtomicBool,
    performance: Arc<PerformanceMonitor>,
    tick_budget_ms: RwLock<f64>,
    adaptive_throttling: AtomicBool,
}

impl Scheduler {
    pub fn new(performance: Arc<PerformanceMonitor>) -> Self {
        Self {
            tasks: DashMap::new(),
            current_tick: AtomicU64::new(0),
            running: AtomicBool::new(false),
            performance,
            tick_budget_ms: RwLock::new(50.0),
            adaptive_throttling: AtomicBool::new(true),
        }
    }
    
    pub async fn start(&self) {
        self.running.store(true, Ordering::SeqCst);
        debug!("Scheduler started");
    }
    
    pub async fn stop(&self) {
        self.running.store(false, Ordering::SeqCst);
        debug!("Scheduler stopped");
    }
    
    pub async fn tick(&self) {
        if !self.running.load(Ordering::SeqCst) {
            return;
        }
        
        let tick = self.current_tick.fetch_add(1, Ordering::SeqCst);
        let start = std::time::Instant::now();
        let budget = *self.tick_budget_ms.read().await;
        let mut used_ms = 0.0;
        
        let mut runnable: Vec<Task> = self.tasks.iter()
            .filter(|t| t.enabled && (tick - t.last_run) >= t.interval_ticks)
            .map(|t| t.clone())
            .collect();
        
        runnable.sort_by_key(|t| t.priority);
        
        for task in runnable {
            if self.adaptive_throttling.load(Ordering::Relaxed) && used_ms >= budget {
                warn!("Tick budget exhausted, deferring {} remaining tasks", 
                    self.tasks.iter().filter(|t| t.enabled).count());
                break;
            }
            
            let task_start = std::time::Instant::now();
            
            if let Some(mut t) = self.tasks.get_mut(&task.id) {
                t.last_run = tick;
            }
            
            let task_duration = task_start.elapsed().as_secs_f64() * 1000.0;
            used_ms += task_duration;
            
            self.performance.record_task_duration(&task.name, task_duration).await;
        }
        
        let total_ms = start.elapsed().as_secs_f64() * 1000.0;
        self.performance.record_tick_duration(total_ms).await;
    }
    
    pub fn register_task(&self, task: Task) -> Uuid {
        let id = task.id;
        self.tasks.insert(id, task);
        id
    }
    
    pub fn unregister_task(&self, id: Uuid) -> bool {
        self.tasks.remove(&id).is_some()
    }
    
    pub fn set_task_enabled(&self, id: Uuid, enabled: bool) -> bool {
        if let Some(mut task) = self.tasks.get_mut(&id) {
            task.enabled = enabled;
            true
        } else {
            false
        }
    }
    
    pub async fn set_tick_budget(&self, ms: f64) {
        *self.tick_budget_ms.write().await = ms;
    }
    
    pub fn set_adaptive_throttling(&self, enabled: bool) {
        self.adaptive_throttling.store(enabled, Ordering::Relaxed);
    }
    
    pub fn current_tick(&self) -> u64 {
        self.current_tick.load(Ordering::SeqCst)
    }
    
    pub fn task_count(&self) -> usize {
        self.tasks.len()
    }
}
