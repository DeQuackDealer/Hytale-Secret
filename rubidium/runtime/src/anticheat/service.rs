use super::config::AnticheatConfig;
use super::detectors::*;
use super::detectors::movement::*;
use super::detectors::combat::*;
use super::detectors::packet::*;
use super::findings::{Finding, FindingLevel, FindingRing};
use crate::abstraction::snapshots::{MovementSnapshot, CombatSnapshot, PacketSnapshot};
use dashmap::DashMap;
use parking_lot::RwLock;
use std::sync::Arc;
use std::sync::atomic::{AtomicBool, AtomicU64, Ordering};
use tracing::info;
use uuid::Uuid;
use ahash::RandomState;

const MAX_HISTORY_PER_PLAYER: usize = 20;

pub struct AnticheatService {
    config: RwLock<AnticheatConfig>,
    enabled: AtomicBool,
    
    movement_history: DashMap<Uuid, Vec<MovementSnapshot>, RandomState>,
    combat_history: DashMap<Uuid, Vec<CombatSnapshot>, RandomState>,
    packet_stats: DashMap<Uuid, PlayerPacketStats, RandomState>,
    
    findings: Arc<FindingRing>,
    
    movement_detectors: RwLock<Vec<Box<dyn MovementDetector>>>,
    combat_detectors: RwLock<Vec<Box<dyn CombatDetector>>>,
    packet_detectors: RwLock<Vec<Box<dyn PacketDetector>>>,
    
    current_tick: AtomicU64,
    sample_counter: AtomicU64,
}

impl AnticheatService {
    pub fn new(config: AnticheatConfig) -> Self {
        let findings = Arc::new(FindingRing::new(config.findings_ring_size));
        
        let mut movement_detectors: Vec<Box<dyn MovementDetector>> = Vec::new();
        movement_detectors.push(Box::new(SpeedDetector::new(&config.movement)));
        movement_detectors.push(Box::new(FlyDetector::new(&config.movement)));
        movement_detectors.push(Box::new(NoFallDetector::new(&config.movement)));
        movement_detectors.push(Box::new(TeleportDetector::new(&config.movement)));
        
        let mut combat_detectors: Vec<Box<dyn CombatDetector>> = Vec::new();
        combat_detectors.push(Box::new(ClickSpeedDetector::new(&config.combat)));
        combat_detectors.push(Box::new(ReachDetector::new(&config.combat)));
        combat_detectors.push(Box::new(KillauraDetector::new(&config.combat)));
        
        let mut packet_detectors: Vec<Box<dyn PacketDetector>> = Vec::new();
        packet_detectors.push(Box::new(PacketFloodDetector::new(&config.packet)));
        packet_detectors.push(Box::new(KeepAliveDetector::new(&config.packet)));
        packet_detectors.push(Box::new(MalformedPacketDetector::new(&config.packet)));
        
        let enabled = config.enabled;
        
        Self {
            config: RwLock::new(config),
            enabled: AtomicBool::new(enabled),
            movement_history: DashMap::with_hasher(RandomState::new()),
            combat_history: DashMap::with_hasher(RandomState::new()),
            packet_stats: DashMap::with_hasher(RandomState::new()),
            findings,
            movement_detectors: RwLock::new(movement_detectors),
            combat_detectors: RwLock::new(combat_detectors),
            packet_detectors: RwLock::new(packet_detectors),
            current_tick: AtomicU64::new(0),
            sample_counter: AtomicU64::new(0),
        }
    }

    pub fn is_enabled(&self) -> bool {
        self.enabled.load(Ordering::Relaxed)
    }

    pub fn set_enabled(&self, enabled: bool) {
        self.enabled.store(enabled, Ordering::Relaxed);
        info!("Anticheat {}", if enabled { "enabled" } else { "disabled" });
    }

    fn should_sample(&self) -> bool {
        let config = self.config.read();
        if config.sample_rate >= 1.0 {
            return true;
        }
        
        let counter = self.sample_counter.fetch_add(1, Ordering::Relaxed);
        let threshold = (1.0 / config.sample_rate) as u64;
        counter % threshold == 0
    }

    pub fn process_movement(&self, player_id: Uuid, snapshot: MovementSnapshot) -> Vec<Finding> {
        if !self.is_enabled() || !self.should_sample() {
            return Vec::new();
        }

        let mut history = self.movement_history.entry(player_id)
            .or_insert_with(Vec::new);
        
        let history_slice: &[MovementSnapshot] = &history;
        let detectors = self.movement_detectors.read();
        let mut all_findings = Vec::new();
        
        for detector in detectors.iter() {
            if detector.is_enabled() {
                let findings = detector.check(player_id, &snapshot, history_slice);
                all_findings.extend(findings);
            }
        }
        
        history.push(snapshot);
        let len = history.len();
        if len > MAX_HISTORY_PER_PLAYER {
            history.drain(0..len - MAX_HISTORY_PER_PLAYER);
        }
        
        let tick = self.current_tick.load(Ordering::Relaxed);
        for finding in &mut all_findings {
            let finding_with_tick = std::mem::replace(finding, Finding::new(
                player_id,
                crate::anticheat::FindingType::InvalidMovement,
                FindingLevel::Info,
                "",
            ));
            *finding = finding_with_tick.with_tick(tick);
            self.findings.push(finding.clone());
        }
        
        all_findings
    }

    pub fn process_combat(&self, player_id: Uuid, snapshot: CombatSnapshot) -> Vec<Finding> {
        if !self.is_enabled() {
            return Vec::new();
        }

        let mut history = self.combat_history.entry(player_id)
            .or_insert_with(Vec::new);
        
        let history_slice: &[CombatSnapshot] = &history;
        let detectors = self.combat_detectors.read();
        let mut all_findings = Vec::new();
        
        for detector in detectors.iter() {
            if detector.is_enabled() {
                let findings = detector.check(player_id, &snapshot, history_slice);
                all_findings.extend(findings);
            }
        }
        
        history.push(snapshot);
        let len = history.len();
        if len > MAX_HISTORY_PER_PLAYER {
            history.drain(0..len - MAX_HISTORY_PER_PLAYER);
        }
        
        let tick = self.current_tick.load(Ordering::Relaxed);
        for finding in &mut all_findings {
            let finding_with_tick = std::mem::replace(finding, Finding::new(
                player_id,
                crate::anticheat::FindingType::InvalidMovement,
                FindingLevel::Info,
                "",
            ));
            *finding = finding_with_tick.with_tick(tick);
            self.findings.push(finding.clone());
        }
        
        all_findings
    }

    pub fn process_packet(&self, player_id: Uuid, snapshot: PacketSnapshot) -> Vec<Finding> {
        if !self.is_enabled() {
            return Vec::new();
        }

        let stats = self.packet_stats.entry(player_id)
            .or_insert_with(PlayerPacketStats::default);
        
        let detectors = self.packet_detectors.read();
        let mut all_findings = Vec::new();
        
        for detector in detectors.iter() {
            if detector.is_enabled() {
                let findings = detector.check(player_id, &snapshot, &stats);
                all_findings.extend(findings);
            }
        }
        
        let tick = self.current_tick.load(Ordering::Relaxed);
        for finding in &mut all_findings {
            let finding_with_tick = std::mem::replace(finding, Finding::new(
                player_id,
                crate::anticheat::FindingType::InvalidMovement,
                FindingLevel::Info,
                "",
            ));
            *finding = finding_with_tick.with_tick(tick);
            self.findings.push(finding.clone());
        }
        
        all_findings
    }

    pub fn tick(&self) {
        self.current_tick.fetch_add(1, Ordering::Relaxed);
    }

    pub fn should_kick_player(&self, player_id: Uuid) -> bool {
        let config = self.config.read();
        let violation_count = self.findings.count_by_player_and_level(player_id, FindingLevel::Likely);
        violation_count as u32 >= config.auto_kick_threshold
    }

    pub fn get_player_violation_count(&self, player_id: Uuid) -> usize {
        self.findings.count_by_player(player_id)
    }

    pub fn get_recent_findings(&self, count: usize) -> Vec<Finding> {
        self.findings.get_recent(count)
    }

    pub fn get_player_findings(&self, player_id: Uuid, count: usize) -> Vec<Finding> {
        self.findings.get_by_player(player_id, count)
    }

    pub fn remove_player(&self, player_id: Uuid) {
        self.movement_history.remove(&player_id);
        self.combat_history.remove(&player_id);
        self.packet_stats.remove(&player_id);
    }

    pub fn reload_config(&self, config: AnticheatConfig) {
        self.enabled.store(config.enabled, Ordering::Relaxed);
        
        let mut movement_detectors = self.movement_detectors.write();
        movement_detectors.clear();
        movement_detectors.push(Box::new(SpeedDetector::new(&config.movement)));
        movement_detectors.push(Box::new(FlyDetector::new(&config.movement)));
        movement_detectors.push(Box::new(NoFallDetector::new(&config.movement)));
        movement_detectors.push(Box::new(TeleportDetector::new(&config.movement)));
        
        let mut combat_detectors = self.combat_detectors.write();
        combat_detectors.clear();
        combat_detectors.push(Box::new(ClickSpeedDetector::new(&config.combat)));
        combat_detectors.push(Box::new(ReachDetector::new(&config.combat)));
        combat_detectors.push(Box::new(KillauraDetector::new(&config.combat)));
        
        let mut packet_detectors = self.packet_detectors.write();
        packet_detectors.clear();
        packet_detectors.push(Box::new(PacketFloodDetector::new(&config.packet)));
        packet_detectors.push(Box::new(KeepAliveDetector::new(&config.packet)));
        packet_detectors.push(Box::new(MalformedPacketDetector::new(&config.packet)));
        
        *self.config.write() = config;
        
        info!("Anticheat configuration reloaded");
    }
}
