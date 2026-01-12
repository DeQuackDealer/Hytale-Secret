use super::capture::CaptureFrame;
use super::storage::{ReplayStorage, ReplayManifest};
use super::camera::{ReplayCamera, CameraMode};
use parking_lot::RwLock;
use std::sync::atomic::{AtomicBool, AtomicU64, Ordering};
use std::sync::Arc;
use uuid::Uuid;

#[derive(Debug, Clone, Copy, PartialEq)]
pub enum PlaybackState {
    Stopped,
    Playing,
    Paused,
    Seeking,
    Finished,
}

#[derive(Debug, Clone, Copy, PartialEq)]
pub enum PlaybackSpeed {
    Slow025,
    Slow050,
    Normal,
    Fast150,
    Fast200,
    Fast400,
}

impl PlaybackSpeed {
    pub fn multiplier(&self) -> f64 {
        match self {
            PlaybackSpeed::Slow025 => 0.25,
            PlaybackSpeed::Slow050 => 0.5,
            PlaybackSpeed::Normal => 1.0,
            PlaybackSpeed::Fast150 => 1.5,
            PlaybackSpeed::Fast200 => 2.0,
            PlaybackSpeed::Fast400 => 4.0,
        }
    }
}

pub struct ReplayPlayer {
    storage: Arc<ReplayStorage>,
    current_replay: RwLock<Option<ActivePlayback>>,
    camera: RwLock<ReplayCamera>,
}

struct ActivePlayback {
    manifest: ReplayManifest,
    frames: Vec<CaptureFrame>,
    current_frame: usize,
    state: PlaybackState,
    speed: PlaybackSpeed,
    loop_enabled: bool,
    start_frame: usize,
    end_frame: usize,
}

impl ReplayPlayer {
    pub fn new(storage: Arc<ReplayStorage>) -> Self {
        Self {
            storage,
            current_replay: RwLock::new(None),
            camera: RwLock::new(ReplayCamera::new()),
        }
    }

    pub fn load(&self, replay_id: Uuid) -> Result<ReplayManifest, String> {
        let manifest = self.storage.get_manifest(replay_id)
            .ok_or("Replay not found")?;
        
        let frames = self.storage.load_replay(replay_id)?;
        let frame_count = frames.len();
        
        let playback = ActivePlayback {
            manifest: manifest.clone(),
            frames,
            current_frame: 0,
            state: PlaybackState::Stopped,
            speed: PlaybackSpeed::Normal,
            loop_enabled: false,
            start_frame: 0,
            end_frame: frame_count.saturating_sub(1),
        };

        *self.current_replay.write() = Some(playback);
        Ok(manifest)
    }

    pub fn unload(&self) {
        *self.current_replay.write() = None;
    }

    pub fn play(&self) -> Result<(), String> {
        let mut replay = self.current_replay.write();
        let playback = replay.as_mut().ok_or("No replay loaded")?;
        playback.state = PlaybackState::Playing;
        Ok(())
    }

    pub fn pause(&self) -> Result<(), String> {
        let mut replay = self.current_replay.write();
        let playback = replay.as_mut().ok_or("No replay loaded")?;
        playback.state = PlaybackState::Paused;
        Ok(())
    }

    pub fn stop(&self) -> Result<(), String> {
        let mut replay = self.current_replay.write();
        let playback = replay.as_mut().ok_or("No replay loaded")?;
        playback.state = PlaybackState::Stopped;
        playback.current_frame = playback.start_frame;
        Ok(())
    }

    pub fn seek(&self, frame: usize) -> Result<(), String> {
        let mut replay = self.current_replay.write();
        let playback = replay.as_mut().ok_or("No replay loaded")?;
        
        if frame >= playback.frames.len() {
            return Err("Frame out of range".to_string());
        }
        
        playback.current_frame = frame;
        Ok(())
    }

    pub fn seek_to_tick(&self, tick: u64) -> Result<(), String> {
        let replay = self.current_replay.read();
        let playback = replay.as_ref().ok_or("No replay loaded")?;
        
        let frame_idx = playback.frames.iter()
            .position(|f| f.tick >= tick)
            .ok_or("Tick out of range")?;
        
        drop(replay);
        self.seek(frame_idx)
    }

    pub fn seek_percent(&self, percent: f64) -> Result<(), String> {
        let replay = self.current_replay.read();
        let playback = replay.as_ref().ok_or("No replay loaded")?;
        
        let percent = percent.clamp(0.0, 100.0);
        let frame = ((playback.frames.len() as f64 - 1.0) * (percent / 100.0)) as usize;
        
        drop(replay);
        self.seek(frame)
    }

    pub fn set_speed(&self, speed: PlaybackSpeed) -> Result<(), String> {
        let mut replay = self.current_replay.write();
        let playback = replay.as_mut().ok_or("No replay loaded")?;
        playback.speed = speed;
        Ok(())
    }

    pub fn set_loop(&self, enabled: bool) -> Result<(), String> {
        let mut replay = self.current_replay.write();
        let playback = replay.as_mut().ok_or("No replay loaded")?;
        playback.loop_enabled = enabled;
        Ok(())
    }

    pub fn set_trim(&self, start_frame: usize, end_frame: usize) -> Result<(), String> {
        let mut replay = self.current_replay.write();
        let playback = replay.as_mut().ok_or("No replay loaded")?;
        
        if start_frame >= playback.frames.len() || end_frame >= playback.frames.len() {
            return Err("Frame out of range".to_string());
        }
        if start_frame > end_frame {
            return Err("Start frame must be before end frame".to_string());
        }
        
        playback.start_frame = start_frame;
        playback.end_frame = end_frame;
        if playback.current_frame < start_frame {
            playback.current_frame = start_frame;
        }
        if playback.current_frame > end_frame {
            playback.current_frame = end_frame;
        }
        Ok(())
    }

    pub fn tick(&self) -> Option<CaptureFrame> {
        let mut replay = self.current_replay.write();
        let playback = replay.as_mut()?;

        if playback.state != PlaybackState::Playing {
            return playback.frames.get(playback.current_frame).cloned();
        }

        let frame = playback.frames.get(playback.current_frame)?.clone();
        
        let advance = playback.speed.multiplier();
        playback.current_frame = ((playback.current_frame as f64 + advance) as usize)
            .min(playback.end_frame);

        if playback.current_frame >= playback.end_frame {
            if playback.loop_enabled {
                playback.current_frame = playback.start_frame;
            } else {
                playback.state = PlaybackState::Finished;
            }
        }

        Some(frame)
    }

    pub fn get_current_frame(&self) -> Option<CaptureFrame> {
        let replay = self.current_replay.read();
        let playback = replay.as_ref()?;
        playback.frames.get(playback.current_frame).cloned()
    }

    pub fn get_state(&self) -> Option<PlaybackState> {
        self.current_replay.read().as_ref().map(|p| p.state)
    }

    pub fn get_progress(&self) -> Option<(usize, usize, f64)> {
        let replay = self.current_replay.read();
        let playback = replay.as_ref()?;
        let total = playback.end_frame - playback.start_frame;
        let current = playback.current_frame - playback.start_frame;
        let percent = if total > 0 { (current as f64 / total as f64) * 100.0 } else { 0.0 };
        Some((playback.current_frame, playback.frames.len(), percent))
    }

    pub fn get_manifest(&self) -> Option<ReplayManifest> {
        self.current_replay.read().as_ref().map(|p| p.manifest.clone())
    }

    pub fn camera(&self) -> &RwLock<ReplayCamera> {
        &self.camera
    }

    pub fn follow_player(&self, player_id: Uuid) {
        self.camera.write().follow_entity(player_id);
    }

    pub fn set_free_camera(&self, x: f64, y: f64, z: f64, yaw: f32, pitch: f32) {
        self.camera.write().set_free_position(x, y, z, yaw, pitch);
    }

    pub fn set_camera_mode(&self, mode: CameraMode) {
        self.camera.write().set_mode(mode);
    }
}
