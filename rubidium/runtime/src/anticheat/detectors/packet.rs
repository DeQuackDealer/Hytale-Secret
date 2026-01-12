use super::{Detector, PacketDetector, PlayerPacketStats};
use crate::anticheat::{Finding, FindingLevel, FindingType, PacketCheckConfig, MalformedPacketAction};
use crate::abstraction::snapshots::PacketSnapshot;
use uuid::Uuid;

pub struct PacketFloodDetector {
    config: PacketCheckConfig,
    enabled: bool,
}

impl PacketFloodDetector {
    pub fn new(config: &PacketCheckConfig) -> Self {
        Self {
            config: config.clone(),
            enabled: config.enabled,
        }
    }
}

impl Detector for PacketFloodDetector {
    fn name(&self) -> &'static str { "PacketFloodDetector" }
    fn is_enabled(&self) -> bool { self.enabled }
    fn set_enabled(&mut self, enabled: bool) { self.enabled = enabled; }
}

impl PacketDetector for PacketFloodDetector {
    fn check(&self, player_id: Uuid, _snapshot: &PacketSnapshot, stats: &PlayerPacketStats) -> Vec<Finding> {
        if !self.enabled {
            return Vec::new();
        }

        let mut findings = Vec::new();
        
        if stats.packets_this_second > self.config.max_packets_per_second {
            let level = if stats.packets_this_second > self.config.max_packets_per_second * 3 {
                FindingLevel::Definite
            } else if stats.packets_this_second > self.config.max_packets_per_second * 2 {
                FindingLevel::Likely
            } else {
                FindingLevel::Suspicious
            };
            
            findings.push(
                Finding::new(
                    player_id,
                    FindingType::PacketFlood,
                    level,
                    format!("Packets/s: {} (max: {})", 
                            stats.packets_this_second, self.config.max_packets_per_second),
                )
            );
        }

        findings
    }
}

pub struct KeepAliveDetector {
    config: PacketCheckConfig,
    enabled: bool,
}

impl KeepAliveDetector {
    pub fn new(config: &PacketCheckConfig) -> Self {
        Self {
            config: config.clone(),
            enabled: config.enabled,
        }
    }
}

impl Detector for KeepAliveDetector {
    fn name(&self) -> &'static str { "KeepAliveDetector" }
    fn is_enabled(&self) -> bool { self.enabled }
    fn set_enabled(&mut self, enabled: bool) { self.enabled = enabled; }
}

impl PacketDetector for KeepAliveDetector {
    fn check(&self, player_id: Uuid, _snapshot: &PacketSnapshot, stats: &PlayerPacketStats) -> Vec<Finding> {
        if !self.enabled {
            return Vec::new();
        }

        let mut findings = Vec::new();
        
        if stats.keepalive_variance > self.config.keepalive_variance_threshold {
            findings.push(
                Finding::new(
                    player_id,
                    FindingType::KeepAliveManipulation,
                    FindingLevel::Suspicious,
                    format!("KeepAlive variance: {:.2} (threshold: {:.2})", 
                            stats.keepalive_variance, self.config.keepalive_variance_threshold),
                )
            );
        }

        findings
    }
}

pub struct MalformedPacketDetector {
    config: PacketCheckConfig,
    enabled: bool,
}

impl MalformedPacketDetector {
    pub fn new(config: &PacketCheckConfig) -> Self {
        Self {
            config: config.clone(),
            enabled: config.enabled,
        }
    }
}

impl Detector for MalformedPacketDetector {
    fn name(&self) -> &'static str { "MalformedPacketDetector" }
    fn is_enabled(&self) -> bool { self.enabled }
    fn set_enabled(&mut self, enabled: bool) { self.enabled = enabled; }
}

impl PacketDetector for MalformedPacketDetector {
    fn check(&self, player_id: Uuid, snapshot: &PacketSnapshot, stats: &PlayerPacketStats) -> Vec<Finding> {
        if !self.enabled || self.config.malformed_packet_action == MalformedPacketAction::Ignore {
            return Vec::new();
        }

        let mut findings = Vec::new();
        
        if snapshot.is_malformed {
            let level = match self.config.malformed_packet_action {
                MalformedPacketAction::Kick => FindingLevel::Definite,
                MalformedPacketAction::Flag => FindingLevel::Suspicious,
                MalformedPacketAction::Ignore => return Vec::new(),
            };
            
            findings.push(
                Finding::new(
                    player_id,
                    FindingType::InvalidPacket,
                    level,
                    format!("Malformed packet (type: {:?}, count: {})", 
                            snapshot.packet_type, stats.malformed_count),
                )
            );
        }

        if stats.duplicate_count > self.config.duplicate_packet_threshold {
            findings.push(
                Finding::new(
                    player_id,
                    FindingType::InvalidPacket,
                    FindingLevel::Suspicious,
                    format!("Excessive duplicate packets: {} (threshold: {})", 
                            stats.duplicate_count, self.config.duplicate_packet_threshold),
                )
            );
        }

        findings
    }
}
