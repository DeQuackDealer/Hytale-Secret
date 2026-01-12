pub mod movement;
pub mod combat;
pub mod packet;

use crate::anticheat::Finding;
use crate::abstraction::snapshots::{MovementSnapshot, CombatSnapshot, PacketSnapshot};
use uuid::Uuid;

pub trait Detector: Send + Sync {
    fn name(&self) -> &'static str;
    fn is_enabled(&self) -> bool;
    fn set_enabled(&mut self, enabled: bool);
}

pub trait MovementDetector: Detector {
    fn check(&self, player_id: Uuid, snapshot: &MovementSnapshot, history: &[MovementSnapshot]) -> Vec<Finding>;
}

pub trait CombatDetector: Detector {
    fn check(&self, player_id: Uuid, snapshot: &CombatSnapshot, history: &[CombatSnapshot]) -> Vec<Finding>;
}

pub trait PacketDetector: Detector {
    fn check(&self, player_id: Uuid, snapshot: &PacketSnapshot, stats: &PlayerPacketStats) -> Vec<Finding>;
}

pub struct PlayerPacketStats {
    pub packets_this_second: u32,
    pub avg_keepalive_latency: f64,
    pub keepalive_variance: f64,
    pub duplicate_count: u32,
    pub malformed_count: u32,
}

impl Default for PlayerPacketStats {
    fn default() -> Self {
        Self {
            packets_this_second: 0,
            avg_keepalive_latency: 0.0,
            keepalive_variance: 0.0,
            duplicate_count: 0,
            malformed_count: 0,
        }
    }
}
