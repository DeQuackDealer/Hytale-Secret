use serde::{Serialize, Deserialize};
use std::time::Duration;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ServerStats {
    pub status: String,
    pub uptime_secs: u64,
    pub player_count: usize,
    pub max_players: usize,
    pub tps: f64,
    pub tick: u64,
    pub memory_used_mb: f64,
    pub memory_max_mb: f64,
    pub cpu_usage: f64,
    pub event_count: u64,
    pub plugin_count: usize,
}

impl ServerStats {
    pub fn is_healthy(&self) -> bool {
        self.tps >= 18.0 && self.status == "Running"
    }

    pub fn health_score(&self) -> f64 {
        let tps_score = (self.tps / 20.0).min(1.0) * 50.0;
        let memory_score = (1.0 - self.memory_used_mb / self.memory_max_mb).max(0.0) * 30.0;
        let cpu_score = (1.0 - self.cpu_usage / 100.0).max(0.0) * 20.0;
        tps_score + memory_score + cpu_score
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct StatusReport {
    pub server: ServerStats,
    pub anticheat: AnticheatStats,
    pub sessions: SessionStats,
    pub performance: PerformanceStats,
    pub generated_at: i64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AnticheatStats {
    pub enabled: bool,
    pub sample_rate: f64,
    pub total_findings: usize,
    pub findings_by_level: FingingsBreakdown,
    pub active_players_monitored: usize,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FingingsBreakdown {
    pub info: usize,
    pub suspicious: usize,
    pub likely: usize,
    pub definite: usize,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SessionStats {
    pub total_sessions: usize,
    pub premium_sessions: usize,
    pub yellow_tale_linked: usize,
    pub avg_session_duration: Duration,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PerformanceStats {
    pub avg_tick_ms: f64,
    pub max_tick_ms: f64,
    pub min_tick_ms: f64,
    pub tick_stddev: f64,
    pub ticks_over_budget: u64,
    pub load_factor: f64,
    pub is_degraded: bool,
}
