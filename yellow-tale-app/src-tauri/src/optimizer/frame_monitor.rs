use serde::{Deserialize, Serialize};
use parking_lot::RwLock;
use std::collections::VecDeque;
use std::time::{Duration, Instant};

const FRAME_HISTORY_SIZE: usize = 300;
const STABILITY_THRESHOLD_MS: f64 = 2.0;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FrameStats {
    pub current_fps: f64,
    pub average_fps: f64,
    pub min_fps: f64,
    pub max_fps: f64,
    pub frame_time_ms: f64,
    pub avg_frame_time_ms: f64,
    pub percentile_1_low: f64,
    pub percentile_01_low: f64,
    pub stability_score: f64,
    pub stutter_count: u32,
    pub dropped_frames: u32,
}

impl Default for FrameStats {
    fn default() -> Self {
        Self {
            current_fps: 0.0,
            average_fps: 0.0,
            min_fps: 0.0,
            max_fps: 0.0,
            frame_time_ms: 0.0,
            avg_frame_time_ms: 0.0,
            percentile_1_low: 0.0,
            percentile_01_low: 0.0,
            stability_score: 100.0,
            stutter_count: 0,
            dropped_frames: 0,
        }
    }
}

#[derive(Debug, Clone)]
struct FrameSample {
    timestamp: Instant,
    frame_time_ms: f64,
}

pub struct FrameMonitor {
    samples: RwLock<VecDeque<FrameSample>>,
    target_framerate: RwLock<Option<u32>>,
    is_monitoring: RwLock<bool>,
    last_frame: RwLock<Option<Instant>>,
    stutter_count: RwLock<u32>,
    dropped_frames: RwLock<u32>,
}

impl FrameMonitor {
    pub fn new() -> Self {
        Self {
            samples: RwLock::new(VecDeque::with_capacity(FRAME_HISTORY_SIZE)),
            target_framerate: RwLock::new(None),
            is_monitoring: RwLock::new(false),
            last_frame: RwLock::new(None),
            stutter_count: RwLock::new(0),
            dropped_frames: RwLock::new(0),
        }
    }
    
    pub fn set_target_framerate(&self, fps: Option<u32>) {
        *self.target_framerate.write() = fps;
    }
    
    pub fn start_monitoring(&self) {
        *self.is_monitoring.write() = true;
        *self.last_frame.write() = Some(Instant::now());
        self.samples.write().clear();
        *self.stutter_count.write() = 0;
        *self.dropped_frames.write() = 0;
    }
    
    pub fn stop_monitoring(&self) {
        *self.is_monitoring.write() = false;
        *self.last_frame.write() = None;
    }
    
    pub fn is_monitoring(&self) -> bool {
        *self.is_monitoring.read()
    }
    
    pub fn record_frame(&self) {
        if !*self.is_monitoring.read() {
            return;
        }
        
        let now = Instant::now();
        let mut last_frame = self.last_frame.write();
        
        if let Some(last) = *last_frame {
            let frame_time = now.duration_since(last);
            let frame_time_ms = frame_time.as_secs_f64() * 1000.0;
            
            let sample = FrameSample {
                timestamp: now,
                frame_time_ms,
            };
            
            let mut samples = self.samples.write();
            if samples.len() >= FRAME_HISTORY_SIZE {
                samples.pop_front();
            }
            samples.push_back(sample);
            
            if frame_time_ms > 50.0 {
                *self.stutter_count.write() += 1;
            }
            
            if let Some(target) = *self.target_framerate.read() {
                let target_frame_time = 1000.0 / target as f64;
                if frame_time_ms > target_frame_time * 1.5 {
                    *self.dropped_frames.write() += 1;
                }
            }
        }
        
        *last_frame = Some(now);
    }
    
    pub fn record_frame_time(&self, frame_time_ms: f64) {
        if !*self.is_monitoring.read() {
            return;
        }
        
        let sample = FrameSample {
            timestamp: Instant::now(),
            frame_time_ms,
        };
        
        let mut samples = self.samples.write();
        if samples.len() >= FRAME_HISTORY_SIZE {
            samples.pop_front();
        }
        samples.push_back(sample);
        
        if frame_time_ms > 50.0 {
            *self.stutter_count.write() += 1;
        }
    }
    
    pub fn get_stats(&self) -> FrameStats {
        let samples = self.samples.read();
        
        if samples.is_empty() {
            return FrameStats::default();
        }
        
        let frame_times: Vec<f64> = samples.iter().map(|s| s.frame_time_ms).collect();
        let count = frame_times.len() as f64;
        
        let current_frame_time = frame_times.last().copied().unwrap_or(16.67);
        let current_fps = 1000.0 / current_frame_time;
        
        let sum: f64 = frame_times.iter().sum();
        let avg_frame_time = sum / count;
        let average_fps = 1000.0 / avg_frame_time;
        
        let min_frame_time = frame_times.iter().cloned().fold(f64::INFINITY, f64::min);
        let max_frame_time = frame_times.iter().cloned().fold(0.0, f64::max);
        let max_fps = 1000.0 / min_frame_time;
        let min_fps = 1000.0 / max_frame_time;
        
        let mut sorted = frame_times.clone();
        sorted.sort_by(|a, b| a.partial_cmp(b).unwrap_or(std::cmp::Ordering::Equal));
        
        let p1_idx = ((sorted.len() as f64 * 0.99) as usize).min(sorted.len() - 1);
        let p01_idx = ((sorted.len() as f64 * 0.999) as usize).min(sorted.len() - 1);
        
        let percentile_1_low = 1000.0 / sorted[p1_idx];
        let percentile_01_low = 1000.0 / sorted[p01_idx];
        
        let variance: f64 = frame_times.iter()
            .map(|t| (t - avg_frame_time).powi(2))
            .sum::<f64>() / count;
        let std_dev = variance.sqrt();
        
        let stability_score = (100.0 - (std_dev / avg_frame_time * 100.0)).max(0.0).min(100.0);
        
        FrameStats {
            current_fps,
            average_fps,
            min_fps,
            max_fps,
            frame_time_ms: current_frame_time,
            avg_frame_time_ms: avg_frame_time,
            percentile_1_low,
            percentile_01_low,
            stability_score,
            stutter_count: *self.stutter_count.read(),
            dropped_frames: *self.dropped_frames.read(),
        }
    }
    
    pub fn get_recent_frame_times(&self, count: usize) -> Vec<f64> {
        let samples = self.samples.read();
        samples.iter()
            .rev()
            .take(count)
            .map(|s| s.frame_time_ms)
            .collect()
    }
    
    pub fn is_stable(&self) -> bool {
        let samples = self.samples.read();
        
        if samples.len() < 30 {
            return true;
        }
        
        let recent: Vec<f64> = samples.iter()
            .rev()
            .take(30)
            .map(|s| s.frame_time_ms)
            .collect();
        
        let avg: f64 = recent.iter().sum::<f64>() / recent.len() as f64;
        let variance: f64 = recent.iter()
            .map(|t| (t - avg).powi(2))
            .sum::<f64>() / recent.len() as f64;
        let std_dev = variance.sqrt();
        
        std_dev < STABILITY_THRESHOLD_MS
    }
    
    pub fn clear(&self) {
        self.samples.write().clear();
        *self.stutter_count.write() = 0;
        *self.dropped_frames.write() = 0;
    }
}
