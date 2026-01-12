use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AnticheatConfig {
    pub enabled: bool,
    pub movement: MovementCheckConfig,
    pub combat: CombatCheckConfig,
    pub packet: PacketCheckConfig,
    pub findings_ring_size: usize,
    pub sample_rate: f64,
    pub log_violations: bool,
    pub auto_kick_threshold: u32,
}

impl Default for AnticheatConfig {
    fn default() -> Self {
        Self {
            enabled: true,
            movement: MovementCheckConfig::default(),
            combat: CombatCheckConfig::default(),
            packet: PacketCheckConfig::default(),
            findings_ring_size: 1000,
            sample_rate: 0.25,
            log_violations: true,
            auto_kick_threshold: 10,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MovementCheckConfig {
    pub enabled: bool,
    pub max_speed: f64,
    pub max_acceleration: f64,
    pub gravity_tolerance: f64,
    pub fly_detection: bool,
    pub teleport_threshold: f64,
    pub sample_window_ms: u64,
}

impl Default for MovementCheckConfig {
    fn default() -> Self {
        Self {
            enabled: true,
            max_speed: 10.0,
            max_acceleration: 50.0,
            gravity_tolerance: 0.1,
            fly_detection: true,
            teleport_threshold: 20.0,
            sample_window_ms: 1000,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CombatCheckConfig {
    pub enabled: bool,
    pub max_cps: u32,
    pub max_reach: f64,
    pub hit_consistency_threshold: f64,
    pub swing_direction_check: bool,
    pub killaura_detection: bool,
    pub sample_window_ms: u64,
}

impl Default for CombatCheckConfig {
    fn default() -> Self {
        Self {
            enabled: true,
            max_cps: 20,
            max_reach: 4.5,
            hit_consistency_threshold: 0.95,
            swing_direction_check: true,
            killaura_detection: true,
            sample_window_ms: 2000,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PacketCheckConfig {
    pub enabled: bool,
    pub max_packets_per_second: u32,
    pub keepalive_variance_threshold: f64,
    pub malformed_packet_action: MalformedPacketAction,
    pub duplicate_packet_threshold: u32,
}

impl Default for PacketCheckConfig {
    fn default() -> Self {
        Self {
            enabled: true,
            max_packets_per_second: 500,
            keepalive_variance_threshold: 2.0,
            malformed_packet_action: MalformedPacketAction::Flag,
            duplicate_packet_threshold: 10,
        }
    }
}

#[derive(Debug, Clone, Copy, Serialize, Deserialize, PartialEq, Eq)]
pub enum MalformedPacketAction {
    Ignore,
    Flag,
    Kick,
}
