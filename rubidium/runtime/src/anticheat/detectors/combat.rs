use super::{Detector, CombatDetector};
use crate::anticheat::{Finding, FindingLevel, FindingType, CombatCheckConfig};
use crate::abstraction::snapshots::CombatSnapshot;
use uuid::Uuid;

pub struct ClickSpeedDetector {
    config: CombatCheckConfig,
    enabled: bool,
}

impl ClickSpeedDetector {
    pub fn new(config: &CombatCheckConfig) -> Self {
        Self {
            config: config.clone(),
            enabled: config.enabled,
        }
    }
}

impl Detector for ClickSpeedDetector {
    fn name(&self) -> &'static str { "ClickSpeedDetector" }
    fn is_enabled(&self) -> bool { self.enabled }
    fn set_enabled(&mut self, enabled: bool) { self.enabled = enabled; }
}

impl CombatDetector for ClickSpeedDetector {
    fn check(&self, player_id: Uuid, snapshot: &CombatSnapshot, history: &[CombatSnapshot]) -> Vec<Finding> {
        if !self.enabled || history.is_empty() {
            return Vec::new();
        }

        let mut findings = Vec::new();
        
        let window_start = snapshot.timestamp.saturating_sub(1000);
        let clicks_in_window: usize = history.iter()
            .filter(|s| s.timestamp >= window_start && s.is_attack)
            .count() + if snapshot.is_attack { 1 } else { 0 };
        
        let cps = clicks_in_window as u32;
        
        if cps > self.config.max_cps {
            let level = if cps > self.config.max_cps * 2 {
                FindingLevel::Definite
            } else if cps > (self.config.max_cps as f32 * 1.5) as u32 {
                FindingLevel::Likely
            } else {
                FindingLevel::Suspicious
            };
            
            findings.push(
                Finding::new(
                    player_id,
                    FindingType::HighCPS,
                    level,
                    format!("CPS: {} (max: {})", cps, self.config.max_cps),
                )
                .with_data(format!(r#"{{"cps":{},"max":{}}}"#, cps, self.config.max_cps))
            );
        }

        findings
    }
}

pub struct ReachDetector {
    config: CombatCheckConfig,
    enabled: bool,
}

impl ReachDetector {
    pub fn new(config: &CombatCheckConfig) -> Self {
        Self {
            config: config.clone(),
            enabled: config.enabled,
        }
    }
}

impl Detector for ReachDetector {
    fn name(&self) -> &'static str { "ReachDetector" }
    fn is_enabled(&self) -> bool { self.enabled }
    fn set_enabled(&mut self, enabled: bool) { self.enabled = enabled; }
}

impl CombatDetector for ReachDetector {
    fn check(&self, player_id: Uuid, snapshot: &CombatSnapshot, _history: &[CombatSnapshot]) -> Vec<Finding> {
        if !self.enabled || !snapshot.is_attack || snapshot.target_id.is_none() {
            return Vec::new();
        }

        let mut findings = Vec::new();
        
        if let Some(distance) = snapshot.distance_to_target {
            if distance > self.config.max_reach {
                let level = if distance > self.config.max_reach + 2.0 {
                    FindingLevel::Definite
                } else if distance > self.config.max_reach + 1.0 {
                    FindingLevel::Likely
                } else {
                    FindingLevel::Suspicious
                };
                
                findings.push(
                    Finding::new(
                        player_id,
                        FindingType::Reach,
                        level,
                        format!("Hit from {:.2} blocks (max: {:.2})", distance, self.config.max_reach),
                    )
                    .with_data(format!(r#"{{"distance":{:.2},"max":{:.2}}}"#, distance, self.config.max_reach))
                );
            }
        }

        findings
    }
}

pub struct KillauraDetector {
    config: CombatCheckConfig,
    enabled: bool,
}

impl KillauraDetector {
    pub fn new(config: &CombatCheckConfig) -> Self {
        Self {
            config: config.clone(),
            enabled: config.killaura_detection,
        }
    }
}

impl Detector for KillauraDetector {
    fn name(&self) -> &'static str { "KillauraDetector" }
    fn is_enabled(&self) -> bool { self.enabled }
    fn set_enabled(&mut self, enabled: bool) { self.enabled = enabled; }
}

impl CombatDetector for KillauraDetector {
    fn check(&self, player_id: Uuid, snapshot: &CombatSnapshot, history: &[CombatSnapshot]) -> Vec<Finding> {
        if !self.enabled || history.len() < 5 {
            return Vec::new();
        }

        let mut findings = Vec::new();
        
        let recent_attacks: Vec<&CombatSnapshot> = history.iter()
            .rev()
            .take(10)
            .filter(|s| s.is_attack && s.target_id.is_some())
            .collect();
        
        if recent_attacks.len() >= 5 {
            let unique_targets: std::collections::HashSet<_> = recent_attacks.iter()
                .filter_map(|s| s.target_id)
                .collect();
            
            if unique_targets.len() >= 3 {
                let angles: Vec<f64> = recent_attacks.iter()
                    .filter_map(|s| s.angle_to_target)
                    .collect();
                
                if !angles.is_empty() {
                    let avg_angle: f64 = angles.iter().sum::<f64>() / angles.len() as f64;
                    let variance: f64 = angles.iter()
                        .map(|a| (a - avg_angle).powi(2))
                        .sum::<f64>() / angles.len() as f64;
                    
                    if variance < 5.0 {
                        findings.push(
                            Finding::new(
                                player_id,
                                FindingType::Killaura,
                                FindingLevel::Likely,
                                "Suspiciously consistent attack angles on multiple targets",
                            )
                            .with_data(format!(r#"{{"variance":{:.2},"targets":{}}}"#, variance, unique_targets.len()))
                        );
                    }
                }
            }
        }

        findings
    }
}
