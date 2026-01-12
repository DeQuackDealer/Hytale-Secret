use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use std::collections::VecDeque;
use std::sync::atomic::{AtomicUsize, Ordering};
use parking_lot::RwLock;
use uuid::Uuid;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum FindingLevel {
    Info,
    Suspicious,
    Likely,
    Definite,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum FindingType {
    SpeedHack,
    FlyHack,
    NoFall,
    Teleport,
    InvalidMovement,
    HighCPS,
    Reach,
    Killaura,
    InvalidSwing,
    PacketFlood,
    InvalidPacket,
    KeepAliveManipulation,
    TimerHack,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Finding {
    pub id: Uuid,
    pub player_id: Uuid,
    pub finding_type: FindingType,
    pub level: FindingLevel,
    pub description: String,
    pub data: Option<String>,
    pub timestamp: DateTime<Utc>,
    pub tick: u64,
}

impl Finding {
    pub fn new(
        player_id: Uuid,
        finding_type: FindingType,
        level: FindingLevel,
        description: impl Into<String>,
    ) -> Self {
        Self {
            id: Uuid::new_v4(),
            player_id,
            finding_type,
            level,
            description: description.into(),
            data: None,
            timestamp: Utc::now(),
            tick: 0,
        }
    }

    pub fn with_data(mut self, data: impl Into<String>) -> Self {
        self.data = Some(data.into());
        self
    }

    pub fn with_tick(mut self, tick: u64) -> Self {
        self.tick = tick;
        self
    }
}

pub struct FindingRing {
    findings: RwLock<VecDeque<Finding>>,
    capacity: usize,
    total_count: AtomicUsize,
}

impl FindingRing {
    pub fn new(capacity: usize) -> Self {
        Self {
            findings: RwLock::new(VecDeque::with_capacity(capacity)),
            capacity,
            total_count: AtomicUsize::new(0),
        }
    }

    pub fn push(&self, finding: Finding) {
        let mut findings = self.findings.write();
        if findings.len() >= self.capacity {
            findings.pop_front();
        }
        findings.push_back(finding);
        self.total_count.fetch_add(1, Ordering::Relaxed);
    }

    pub fn get_recent(&self, count: usize) -> Vec<Finding> {
        let findings = self.findings.read();
        findings.iter()
            .rev()
            .take(count)
            .cloned()
            .collect()
    }

    pub fn get_by_player(&self, player_id: Uuid, count: usize) -> Vec<Finding> {
        let findings = self.findings.read();
        findings.iter()
            .rev()
            .filter(|f| f.player_id == player_id)
            .take(count)
            .cloned()
            .collect()
    }

    pub fn count_by_player(&self, player_id: Uuid) -> usize {
        let findings = self.findings.read();
        findings.iter()
            .filter(|f| f.player_id == player_id)
            .count()
    }

    pub fn count_by_player_and_level(&self, player_id: Uuid, min_level: FindingLevel) -> usize {
        let findings = self.findings.read();
        findings.iter()
            .filter(|f| f.player_id == player_id && f.level as u8 >= min_level as u8)
            .count()
    }

    pub fn total_count(&self) -> usize {
        self.total_count.load(Ordering::Relaxed)
    }

    pub fn clear(&self) {
        self.findings.write().clear();
    }

    pub fn len(&self) -> usize {
        self.findings.read().len()
    }

    pub fn is_empty(&self) -> bool {
        self.findings.read().is_empty()
    }
}
