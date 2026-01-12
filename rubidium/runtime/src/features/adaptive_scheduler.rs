use std::collections::VecDeque;
use std::sync::atomic::{AtomicBool, AtomicU64, Ordering};
use parking_lot::RwLock;

pub struct AdaptiveScheduler {
    tick_history: RwLock<VecDeque<f64>>,
    target_tick_ms: f64,
    current_load_factor: RwLock<f64>,
    enabled: AtomicBool,
    tick_count: AtomicU64,
    degraded_mode: AtomicBool,
}

impl AdaptiveScheduler {
    pub fn new(target_tick_ms: f64) -> Self {
        Self {
            tick_history: RwLock::new(VecDeque::with_capacity(100)),
            target_tick_ms,
            current_load_factor: RwLock::new(1.0),
            enabled: AtomicBool::new(true),
            tick_count: AtomicU64::new(0),
            degraded_mode: AtomicBool::new(false),
        }
    }

    pub fn record_tick(&self, duration_ms: f64) {
        self.tick_count.fetch_add(1, Ordering::Relaxed);
        
        let mut history = self.tick_history.write();
        history.push_back(duration_ms);
        if history.len() > 100 {
            history.pop_front();
        }
        
        if self.enabled.load(Ordering::Relaxed) {
            self.adapt_load_factor(&history, duration_ms);
        }
    }

    fn adapt_load_factor(&self, history: &VecDeque<f64>, latest_tick: f64) {
        if history.len() < 10 {
            return;
        }

        let avg: f64 = history.iter().sum::<f64>() / history.len() as f64;
        let variance: f64 = history.iter()
            .map(|t| (t - avg).powi(2))
            .sum::<f64>() / history.len() as f64;
        let std_dev = variance.sqrt();
        
        let p99 = self.percentile(history, 99);
        
        let mut load_factor = self.current_load_factor.write();
        
        if p99 > self.target_tick_ms * 0.9 {
            *load_factor = (*load_factor * 0.95).max(0.2);
            self.degraded_mode.store(true, Ordering::Relaxed);
        } else if avg < self.target_tick_ms * 0.5 && std_dev < 5.0 {
            *load_factor = (*load_factor * 1.05).min(1.0);
            if *load_factor > 0.8 {
                self.degraded_mode.store(false, Ordering::Relaxed);
            }
        }
    }

    fn percentile(&self, history: &VecDeque<f64>, p: u8) -> f64 {
        let mut sorted: Vec<f64> = history.iter().cloned().collect();
        sorted.sort_by(|a, b| a.partial_cmp(b).unwrap_or(std::cmp::Ordering::Equal));
        
        let index = (sorted.len() as f64 * (p as f64 / 100.0)).ceil() as usize;
        sorted.get(index.saturating_sub(1)).cloned().unwrap_or(0.0)
    }

    pub fn get_load_factor(&self) -> f64 {
        *self.current_load_factor.read()
    }

    pub fn should_defer(&self, priority: u8) -> bool {
        let load_factor = *self.current_load_factor.read();
        
        match priority {
            0 => false,
            1 => load_factor < 0.3,
            2 => load_factor < 0.5,
            3 => load_factor < 0.7,
            _ => load_factor < 0.9,
        }
    }

    pub fn is_degraded(&self) -> bool {
        self.degraded_mode.load(Ordering::Relaxed)
    }

    pub fn set_enabled(&self, enabled: bool) {
        self.enabled.store(enabled, Ordering::Relaxed);
    }

    pub fn is_enabled(&self) -> bool {
        self.enabled.load(Ordering::Relaxed)
    }

    pub fn get_avg_tick_ms(&self) -> f64 {
        let history = self.tick_history.read();
        if history.is_empty() {
            return 0.0;
        }
        history.iter().sum::<f64>() / history.len() as f64
    }

    pub fn get_tps(&self) -> f64 {
        let avg = self.get_avg_tick_ms();
        if avg > 0.0 {
            (1000.0 / avg).min(20.0)
        } else {
            20.0
        }
    }
}
