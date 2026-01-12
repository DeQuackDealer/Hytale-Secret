use serde::{Serialize, Deserialize};
use std::time::Instant;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum HealthStatus {
    Healthy,
    Degraded,
    Unhealthy,
    Unknown,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct HealthCheck {
    pub status: HealthStatus,
    pub checks: Vec<ComponentHealth>,
    pub timestamp: i64,
    pub version: String,
}

impl HealthCheck {
    pub fn new(version: impl Into<String>) -> Self {
        Self {
            status: HealthStatus::Unknown,
            checks: Vec::new(),
            timestamp: chrono::Utc::now().timestamp(),
            version: version.into(),
        }
    }

    pub fn add_check(&mut self, check: ComponentHealth) {
        self.checks.push(check);
        self.recalculate_status();
    }

    fn recalculate_status(&mut self) {
        let has_unhealthy = self.checks.iter().any(|c| c.status == HealthStatus::Unhealthy);
        let has_degraded = self.checks.iter().any(|c| c.status == HealthStatus::Degraded);
        let all_healthy = self.checks.iter().all(|c| c.status == HealthStatus::Healthy);
        
        self.status = if has_unhealthy {
            HealthStatus::Unhealthy
        } else if has_degraded {
            HealthStatus::Degraded
        } else if all_healthy && !self.checks.is_empty() {
            HealthStatus::Healthy
        } else {
            HealthStatus::Unknown
        };
    }

    pub fn is_ready(&self) -> bool {
        self.status == HealthStatus::Healthy || self.status == HealthStatus::Degraded
    }

    pub fn is_live(&self) -> bool {
        self.status != HealthStatus::Unhealthy
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ComponentHealth {
    pub name: String,
    pub status: HealthStatus,
    pub message: Option<String>,
    pub latency_ms: Option<f64>,
    pub details: std::collections::HashMap<String, String>,
}

impl ComponentHealth {
    pub fn healthy(name: impl Into<String>) -> Self {
        Self {
            name: name.into(),
            status: HealthStatus::Healthy,
            message: None,
            latency_ms: None,
            details: std::collections::HashMap::new(),
        }
    }

    pub fn degraded(name: impl Into<String>, message: impl Into<String>) -> Self {
        Self {
            name: name.into(),
            status: HealthStatus::Degraded,
            message: Some(message.into()),
            latency_ms: None,
            details: std::collections::HashMap::new(),
        }
    }

    pub fn unhealthy(name: impl Into<String>, message: impl Into<String>) -> Self {
        Self {
            name: name.into(),
            status: HealthStatus::Unhealthy,
            message: Some(message.into()),
            latency_ms: None,
            details: std::collections::HashMap::new(),
        }
    }

    pub fn with_latency(mut self, latency_ms: f64) -> Self {
        self.latency_ms = Some(latency_ms);
        self
    }

    pub fn with_detail(mut self, key: impl Into<String>, value: impl Into<String>) -> Self {
        self.details.insert(key.into(), value.into());
        self
    }
}

pub struct HealthChecker {
    checks: Vec<Box<dyn Fn() -> ComponentHealth + Send + Sync>>,
}

impl HealthChecker {
    pub fn new() -> Self {
        Self { checks: Vec::new() }
    }

    pub fn add_check<F>(&mut self, check: F)
    where
        F: Fn() -> ComponentHealth + Send + Sync + 'static,
    {
        self.checks.push(Box::new(check));
    }

    pub fn run(&self, version: &str) -> HealthCheck {
        let mut health = HealthCheck::new(version);
        
        for check in &self.checks {
            let start = Instant::now();
            let mut result = check();
            result.latency_ms = Some(start.elapsed().as_secs_f64() * 1000.0);
            health.add_check(result);
        }
        
        health
    }
}

impl Default for HealthChecker {
    fn default() -> Self {
        Self::new()
    }
}
