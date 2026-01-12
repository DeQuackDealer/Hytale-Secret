use super::config::CinemaConfig;
use super::paths::{CameraPath, PathKeyframe};
use dashmap::DashMap;
use parking_lot::RwLock;
use serde::{Deserialize, Serialize};
use std::sync::Arc;
use uuid::Uuid;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum PlaybackState {
    Stopped,
    Playing,
    Paused,
}

struct ActivePlayback {
    path: CameraPath,
    state: PlaybackState,
    current_time_ms: u64,
    speed: f32,
}

pub struct CinemaService {
    config: Arc<RwLock<CinemaConfig>>,
    saved_paths: DashMap<Uuid, CameraPath>,
    owner_index: DashMap<Uuid, Vec<Uuid>>,
    active_playbacks: DashMap<Uuid, ActivePlayback>,
    recording_sessions: DashMap<Uuid, CameraPath>,
}

impl CinemaService {
    pub fn new(config: CinemaConfig) -> Self {
        Self {
            config: Arc::new(RwLock::new(config)),
            saved_paths: DashMap::new(),
            owner_index: DashMap::new(),
            active_playbacks: DashMap::new(),
            recording_sessions: DashMap::new(),
        }
    }

    pub fn is_enabled(&self) -> bool {
        self.config.read().enabled
    }

    pub fn start_recording(&self, player_id: Uuid, name: String) -> Result<(), String> {
        if !self.is_enabled() {
            return Err("Cinematic camera is disabled".to_string());
        }

        if self.recording_sessions.contains_key(&player_id) {
            return Err("Already recording".to_string());
        }

        let path = CameraPath::new(player_id, name);
        self.recording_sessions.insert(player_id, path);
        Ok(())
    }

    pub fn add_keyframe(&self, player_id: Uuid, keyframe: PathKeyframe) -> Result<(), String> {
        let config = self.config.read();
        let max_keyframes = config.max_keyframes as usize;
        drop(config);

        let mut path = self.recording_sessions.get_mut(&player_id)
            .ok_or("Not recording")?;

        if path.keyframes.len() >= max_keyframes {
            return Err(format!("Maximum keyframes ({}) reached", max_keyframes));
        }

        path.add_keyframe(keyframe);
        Ok(())
    }

    pub fn stop_recording(&self, player_id: Uuid) -> Result<CameraPath, String> {
        let (_, path) = self.recording_sessions.remove(&player_id)
            .ok_or("Not recording")?;
        Ok(path)
    }

    pub fn save_path(&self, path: CameraPath) -> Result<Uuid, String> {
        let config = self.config.read();
        if !config.permissions.allow_save {
            return Err("Saving paths is not allowed".to_string());
        }
        drop(config);

        let id = path.id;
        let owner = path.owner_id;

        self.saved_paths.insert(id, path);
        self.owner_index.entry(owner)
            .or_insert_with(Vec::new)
            .push(id);

        Ok(id)
    }

    pub fn delete_path(&self, path_id: Uuid, requester_id: Uuid) -> Result<(), String> {
        let path = self.saved_paths.get(&path_id)
            .ok_or("Path not found")?;

        if path.owner_id != requester_id {
            return Err("You can only delete your own paths".to_string());
        }

        drop(path);
        self.saved_paths.remove(&path_id);

        if let Some(mut paths) = self.owner_index.get_mut(&requester_id) {
            paths.retain(|id| *id != path_id);
        }

        Ok(())
    }

    pub fn get_path(&self, path_id: Uuid) -> Option<CameraPath> {
        self.saved_paths.get(&path_id).map(|p| p.clone())
    }

    pub fn get_player_paths(&self, player_id: Uuid) -> Vec<CameraPath> {
        self.owner_index.get(&player_id)
            .map(|ids| ids.iter()
                .filter_map(|id| self.saved_paths.get(id).map(|p| p.clone()))
                .collect())
            .unwrap_or_default()
    }

    pub fn play_path(&self, player_id: Uuid, path_id: Uuid) -> Result<(), String> {
        let path = self.saved_paths.get(&path_id)
            .ok_or("Path not found")?
            .clone();

        let playback = ActivePlayback {
            path,
            state: PlaybackState::Playing,
            current_time_ms: 0,
            speed: 1.0,
        };

        self.active_playbacks.insert(player_id, playback);
        Ok(())
    }

    pub fn pause_playback(&self, player_id: Uuid) -> Result<(), String> {
        let mut playback = self.active_playbacks.get_mut(&player_id)
            .ok_or("No active playback")?;
        playback.state = PlaybackState::Paused;
        Ok(())
    }

    pub fn resume_playback(&self, player_id: Uuid) -> Result<(), String> {
        let mut playback = self.active_playbacks.get_mut(&player_id)
            .ok_or("No active playback")?;
        playback.state = PlaybackState::Playing;
        Ok(())
    }

    pub fn stop_playback(&self, player_id: Uuid) {
        self.active_playbacks.remove(&player_id);
    }

    pub fn set_playback_speed(&self, player_id: Uuid, speed: f32) -> Result<(), String> {
        let mut playback = self.active_playbacks.get_mut(&player_id)
            .ok_or("No active playback")?;
        playback.speed = speed.clamp(0.1, 10.0);
        Ok(())
    }

    pub fn seek(&self, player_id: Uuid, time_ms: u64) -> Result<(), String> {
        let mut playback = self.active_playbacks.get_mut(&player_id)
            .ok_or("No active playback")?;
        playback.current_time_ms = time_ms.min(playback.path.duration_ms);
        Ok(())
    }

    pub fn tick(&self, delta_ms: u64) -> Vec<(Uuid, PathKeyframe)> {
        let mut updates = Vec::new();

        for mut playback in self.active_playbacks.iter_mut() {
            if playback.state != PlaybackState::Playing {
                continue;
            }

            let advance = (delta_ms as f32 * playback.speed) as u64;
            playback.current_time_ms += advance;

            if playback.current_time_ms >= playback.path.duration_ms {
                if playback.path.loop_enabled {
                    playback.current_time_ms %= playback.path.duration_ms;
                } else {
                    playback.state = PlaybackState::Stopped;
                    playback.current_time_ms = playback.path.duration_ms;
                }
            }

            if let Some(keyframe) = playback.path.get_position_at(playback.current_time_ms) {
                updates.push((*playback.key(), keyframe));
            }
        }

        updates
    }

    pub fn config(&self) -> &Arc<RwLock<CinemaConfig>> {
        &self.config
    }
}
