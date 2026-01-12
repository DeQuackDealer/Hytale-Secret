use super::config::ReplayConfig;
use super::storage::{ReplayStorage, ReplaySegment};
use chrono::{DateTime, Utc};
use dashmap::DashMap;
use parking_lot::RwLock;
use serde::{Deserialize, Serialize};
use std::collections::VecDeque;
use std::sync::atomic::{AtomicBool, AtomicU64, Ordering};
use std::sync::Arc;
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CaptureFrame {
    pub tick: u64,
    pub timestamp: DateTime<Utc>,
    pub player_states: Vec<PlayerFrameState>,
    pub entity_states: Vec<EntityFrameState>,
    pub block_changes: Vec<BlockChange>,
    pub particles: Vec<ParticleEvent>,
    pub sounds: Vec<SoundEvent>,
    pub chat_messages: Vec<ChatMessage>,
    pub world_events: Vec<WorldEvent>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PlayerFrameState {
    pub id: Uuid,
    pub name: String,
    pub x: f64,
    pub y: f64,
    pub z: f64,
    pub yaw: f32,
    pub pitch: f32,
    pub on_ground: bool,
    pub sneaking: bool,
    pub sprinting: bool,
    pub health: f32,
    pub held_item: Option<String>,
    pub armor: Vec<String>,
    pub animation: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct EntityFrameState {
    pub id: Uuid,
    pub entity_type: String,
    pub x: f64,
    pub y: f64,
    pub z: f64,
    pub yaw: f32,
    pub pitch: f32,
    pub velocity_x: f64,
    pub velocity_y: f64,
    pub velocity_z: f64,
    pub metadata: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct BlockChange {
    pub x: i32,
    pub y: i32,
    pub z: i32,
    pub old_block: String,
    pub new_block: String,
    pub caused_by: Option<Uuid>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ParticleEvent {
    pub particle_type: String,
    pub x: f64,
    pub y: f64,
    pub z: f64,
    pub count: u32,
    pub spread: f32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SoundEvent {
    pub sound: String,
    pub x: f64,
    pub y: f64,
    pub z: f64,
    pub volume: f32,
    pub pitch: f32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ChatMessage {
    pub sender: Option<Uuid>,
    pub sender_name: String,
    pub message: String,
    pub message_type: ChatMessageType,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum ChatMessageType {
    Chat,
    System,
    ActionBar,
    Title,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum WorldEvent {
    Explosion { x: f64, y: f64, z: f64, power: f32 },
    Lightning { x: f64, y: f64, z: f64 },
    WeatherChange { weather: String },
    TimeChange { time: i64 },
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CaptureConfig {
    pub player_id: Uuid,
    pub center_x: f64,
    pub center_y: f64,
    pub center_z: f64,
    pub radius: f64,
    pub height: f64,
    pub world: String,
}

pub struct ReplayCapture {
    config: Arc<RwLock<ReplayConfig>>,
    active_captures: DashMap<Uuid, ActiveCapture>,
    storage: Arc<ReplayStorage>,
    current_tick: AtomicU64,
    global_enabled: AtomicBool,
}

struct ActiveCapture {
    capture_config: CaptureConfig,
    frames: RwLock<VecDeque<CaptureFrame>>,
    start_tick: u64,
    start_time: DateTime<Utc>,
    paused: AtomicBool,
    segment_counter: AtomicU64,
}

impl ReplayCapture {
    pub fn new(config: ReplayConfig, storage: Arc<ReplayStorage>) -> Self {
        let enabled = config.enabled;
        Self {
            config: Arc::new(RwLock::new(config)),
            active_captures: DashMap::new(),
            storage,
            current_tick: AtomicU64::new(0),
            global_enabled: AtomicBool::new(enabled),
        }
    }

    pub fn is_enabled(&self) -> bool {
        self.global_enabled.load(Ordering::Relaxed)
    }

    pub fn set_enabled(&self, enabled: bool) {
        self.global_enabled.store(enabled, Ordering::Relaxed);
    }

    pub fn start_capture(&self, player_id: Uuid, center_x: f64, center_y: f64, center_z: f64, world: String) -> Result<(), String> {
        if !self.is_enabled() {
            return Err("Replay system is disabled".to_string());
        }

        if self.active_captures.contains_key(&player_id) {
            return Err("Capture already active for this player".to_string());
        }

        let config = self.config.read();
        let capture_config = CaptureConfig {
            player_id,
            center_x,
            center_y,
            center_z,
            radius: config.capture_radius,
            height: config.capture_height,
            world,
        };

        let capture = ActiveCapture {
            capture_config,
            frames: RwLock::new(VecDeque::with_capacity(72000)),
            start_tick: self.current_tick.load(Ordering::Relaxed),
            start_time: Utc::now(),
            paused: AtomicBool::new(false),
            segment_counter: AtomicU64::new(0),
        };

        self.active_captures.insert(player_id, capture);
        Ok(())
    }

    pub fn stop_capture(&self, player_id: Uuid) -> Result<Uuid, String> {
        let (_, capture) = self.active_captures.remove(&player_id)
            .ok_or("No active capture for this player")?;

        let frames = capture.frames.read().iter().cloned().collect::<Vec<_>>();
        let replay_id = self.storage.save_replay(
            player_id,
            capture.start_time,
            Utc::now(),
            capture.start_tick,
            self.current_tick.load(Ordering::Relaxed),
            frames,
        )?;

        Ok(replay_id)
    }

    pub fn pause_capture(&self, player_id: Uuid) -> Result<(), String> {
        let capture = self.active_captures.get(&player_id)
            .ok_or("No active capture for this player")?;
        capture.paused.store(true, Ordering::Relaxed);
        Ok(())
    }

    pub fn resume_capture(&self, player_id: Uuid) -> Result<(), String> {
        let capture = self.active_captures.get(&player_id)
            .ok_or("No active capture for this player")?;
        capture.paused.store(false, Ordering::Relaxed);
        Ok(())
    }

    pub fn is_capturing(&self, player_id: Uuid) -> bool {
        self.active_captures.contains_key(&player_id)
    }

    pub fn record_frame(&self, frame: CaptureFrame) {
        if !self.is_enabled() {
            return;
        }

        self.current_tick.store(frame.tick, Ordering::Relaxed);

        for capture in self.active_captures.iter() {
            if capture.paused.load(Ordering::Relaxed) {
                continue;
            }

            let filtered_frame = self.filter_frame_for_capture(&frame, &capture.capture_config);
            if let Some(filtered) = filtered_frame {
                let mut frames = capture.frames.write();
                let config = self.config.read();
                let max_frames = config.max_duration_secs * 20;
                
                if frames.len() >= max_frames as usize {
                    frames.pop_front();
                }
                frames.push_back(filtered);
            }
        }
    }

    fn filter_frame_for_capture(&self, frame: &CaptureFrame, config: &CaptureConfig) -> Option<CaptureFrame> {
        let in_radius = |x: f64, y: f64, z: f64| -> bool {
            let dx = x - config.center_x;
            let dy = y - config.center_y;
            let dz = z - config.center_z;
            let horizontal_dist = (dx * dx + dz * dz).sqrt();
            horizontal_dist <= config.radius && dy.abs() <= config.height
        };

        let player_states: Vec<_> = frame.player_states.iter()
            .filter(|p| in_radius(p.x, p.y, p.z))
            .cloned()
            .collect();

        let entity_states: Vec<_> = frame.entity_states.iter()
            .filter(|e| in_radius(e.x, e.y, e.z))
            .cloned()
            .collect();

        let block_changes: Vec<_> = frame.block_changes.iter()
            .filter(|b| in_radius(b.x as f64, b.y as f64, b.z as f64))
            .cloned()
            .collect();

        let particles: Vec<_> = frame.particles.iter()
            .filter(|p| in_radius(p.x, p.y, p.z))
            .cloned()
            .collect();

        let sounds: Vec<_> = frame.sounds.iter()
            .filter(|s| in_radius(s.x, s.y, s.z))
            .cloned()
            .collect();

        Some(CaptureFrame {
            tick: frame.tick,
            timestamp: frame.timestamp,
            player_states,
            entity_states,
            block_changes,
            particles,
            sounds,
            chat_messages: frame.chat_messages.clone(),
            world_events: frame.world_events.clone(),
        })
    }

    pub fn update_capture_center(&self, player_id: Uuid, x: f64, y: f64, z: f64) {
        if let Some(mut capture) = self.active_captures.get_mut(&player_id) {
            capture.capture_config.center_x = x;
            capture.capture_config.center_y = y;
            capture.capture_config.center_z = z;
        }
    }

    pub fn get_active_capture_count(&self) -> usize {
        self.active_captures.len()
    }

    pub fn list_active_captures(&self) -> Vec<Uuid> {
        self.active_captures.iter().map(|e| *e.key()).collect()
    }
}
