use super::{Detector, MovementDetector};
use crate::anticheat::{Finding, FindingLevel, FindingType, MovementCheckConfig};
use crate::abstraction::snapshots::MovementSnapshot;
use uuid::Uuid;

pub struct SpeedDetector {
    config: MovementCheckConfig,
    enabled: bool,
}

impl SpeedDetector {
    pub fn new(config: &MovementCheckConfig) -> Self {
        Self {
            config: config.clone(),
            enabled: config.enabled,
        }
    }
}

impl Detector for SpeedDetector {
    fn name(&self) -> &'static str { "SpeedDetector" }
    fn is_enabled(&self) -> bool { self.enabled }
    fn set_enabled(&mut self, enabled: bool) { self.enabled = enabled; }
}

impl MovementDetector for SpeedDetector {
    fn check(&self, player_id: Uuid, snapshot: &MovementSnapshot, history: &[MovementSnapshot]) -> Vec<Finding> {
        if !self.enabled || history.is_empty() {
            return Vec::new();
        }

        let mut findings = Vec::new();
        let prev = &history[history.len() - 1];
        
        let dx = snapshot.x - prev.x;
        let dy = snapshot.y - prev.y;
        let dz = snapshot.z - prev.z;
        let horizontal_distance = (dx * dx + dz * dz).sqrt();
        let dt = (snapshot.timestamp - prev.timestamp).max(1) as f64 / 1000.0;
        
        let horizontal_speed = horizontal_distance / dt;
        
        if horizontal_speed > self.config.max_speed && !snapshot.on_ground {
            let level = if horizontal_speed > self.config.max_speed * 2.0 {
                FindingLevel::Definite
            } else if horizontal_speed > self.config.max_speed * 1.5 {
                FindingLevel::Likely
            } else {
                FindingLevel::Suspicious
            };
            
            findings.push(
                Finding::new(
                    player_id,
                    FindingType::SpeedHack,
                    level,
                    format!("Speed: {:.2} blocks/s (max: {:.2})", horizontal_speed, self.config.max_speed),
                )
                .with_data(format!(r#"{{"speed":{:.2},"max":{:.2}}}"#, horizontal_speed, self.config.max_speed))
            );
        }

        findings
    }
}

pub struct FlyDetector {
    config: MovementCheckConfig,
    enabled: bool,
}

impl FlyDetector {
    pub fn new(config: &MovementCheckConfig) -> Self {
        Self {
            config: config.clone(),
            enabled: config.fly_detection,
        }
    }
}

impl Detector for FlyDetector {
    fn name(&self) -> &'static str { "FlyDetector" }
    fn is_enabled(&self) -> bool { self.enabled }
    fn set_enabled(&mut self, enabled: bool) { self.enabled = enabled; }
}

impl MovementDetector for FlyDetector {
    fn check(&self, player_id: Uuid, snapshot: &MovementSnapshot, history: &[MovementSnapshot]) -> Vec<Finding> {
        if !self.enabled || history.len() < 5 {
            return Vec::new();
        }

        let mut findings = Vec::new();
        
        let air_ticks: usize = history.iter()
            .rev()
            .take(10)
            .filter(|s| !s.on_ground)
            .count();
        
        if air_ticks >= 10 {
            let vertical_changes: Vec<f64> = history.windows(2)
                .map(|w| w[1].y - w[0].y)
                .collect();
            
            let has_normal_gravity = vertical_changes.iter()
                .any(|&dy| dy < -0.08);
            
            if !has_normal_gravity && !snapshot.in_water && !snapshot.is_gliding {
                findings.push(
                    Finding::new(
                        player_id,
                        FindingType::FlyHack,
                        FindingLevel::Likely,
                        "Player appears to be flying (no gravity applied)",
                    )
                );
            }
        }

        findings
    }
}

pub struct NoFallDetector {
    config: MovementCheckConfig,
    enabled: bool,
}

impl NoFallDetector {
    pub fn new(config: &MovementCheckConfig) -> Self {
        Self {
            config: config.clone(),
            enabled: config.enabled,
        }
    }
}

impl Detector for NoFallDetector {
    fn name(&self) -> &'static str { "NoFallDetector" }
    fn is_enabled(&self) -> bool { self.enabled }
    fn set_enabled(&mut self, enabled: bool) { self.enabled = enabled; }
}

impl MovementDetector for NoFallDetector {
    fn check(&self, player_id: Uuid, snapshot: &MovementSnapshot, history: &[MovementSnapshot]) -> Vec<Finding> {
        if !self.enabled || history.len() < 3 {
            return Vec::new();
        }

        let mut findings = Vec::new();
        
        let fall_distance: f64 = history.windows(2)
            .filter(|w| w[1].y < w[0].y && !w[1].on_ground)
            .map(|w| w[0].y - w[1].y)
            .sum();
        
        if fall_distance > 4.0 && snapshot.on_ground && !snapshot.took_fall_damage {
            findings.push(
                Finding::new(
                    player_id,
                    FindingType::NoFall,
                    FindingLevel::Suspicious,
                    format!("Fell {:.1} blocks without taking damage", fall_distance),
                )
            );
        }

        findings
    }
}

pub struct TeleportDetector {
    config: MovementCheckConfig,
    enabled: bool,
}

impl TeleportDetector {
    pub fn new(config: &MovementCheckConfig) -> Self {
        Self {
            config: config.clone(),
            enabled: config.enabled,
        }
    }
}

impl Detector for TeleportDetector {
    fn name(&self) -> &'static str { "TeleportDetector" }
    fn is_enabled(&self) -> bool { self.enabled }
    fn set_enabled(&mut self, enabled: bool) { self.enabled = enabled; }
}

impl MovementDetector for TeleportDetector {
    fn check(&self, player_id: Uuid, snapshot: &MovementSnapshot, history: &[MovementSnapshot]) -> Vec<Finding> {
        if !self.enabled || history.is_empty() {
            return Vec::new();
        }

        let mut findings = Vec::new();
        let prev = &history[history.len() - 1];
        
        let dx = snapshot.x - prev.x;
        let dy = snapshot.y - prev.y;
        let dz = snapshot.z - prev.z;
        let distance = (dx * dx + dy * dy + dz * dz).sqrt();
        
        if distance > self.config.teleport_threshold && !snapshot.is_teleporting {
            findings.push(
                Finding::new(
                    player_id,
                    FindingType::Teleport,
                    FindingLevel::Definite,
                    format!("Moved {:.1} blocks in one tick (threshold: {:.1})", 
                            distance, self.config.teleport_threshold),
                )
            );
        }

        findings
    }
}
