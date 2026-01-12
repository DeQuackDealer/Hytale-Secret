use super::capture::CaptureFrame;
use chrono::{DateTime, Utc};
use parking_lot::RwLock;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::fs;
use std::io::{Read, Write};
use std::path::PathBuf;
use std::sync::Arc;
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ReplayManifest {
    pub id: Uuid,
    pub player_id: Uuid,
    pub player_name: Option<String>,
    pub world: String,
    pub start_time: DateTime<Utc>,
    pub end_time: DateTime<Utc>,
    pub start_tick: u64,
    pub end_tick: u64,
    pub duration_secs: u64,
    pub frame_count: usize,
    pub segment_count: usize,
    pub total_size_bytes: u64,
    pub compressed: bool,
    pub capture_center: (f64, f64, f64),
    pub capture_radius: f64,
    pub tags: Vec<String>,
    pub shared_with: Vec<Uuid>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ReplaySegment {
    pub segment_id: u32,
    pub start_tick: u64,
    pub end_tick: u64,
    pub frame_count: usize,
    pub data: Vec<u8>,
}

pub struct ReplayStorage {
    storage_path: PathBuf,
    index: RwLock<HashMap<Uuid, ReplayManifest>>,
    player_index: RwLock<HashMap<Uuid, Vec<Uuid>>>,
    max_storage_bytes: u64,
}

impl ReplayStorage {
    pub fn new(storage_path: PathBuf, max_storage_gb: f64) -> Self {
        fs::create_dir_all(&storage_path).ok();
        
        let storage = Self {
            storage_path,
            index: RwLock::new(HashMap::new()),
            player_index: RwLock::new(HashMap::new()),
            max_storage_bytes: (max_storage_gb * 1024.0 * 1024.0 * 1024.0) as u64,
        };
        
        storage.load_index();
        storage
    }

    fn load_index(&self) {
        let index_path = self.storage_path.join("index.json");
        if let Ok(data) = fs::read_to_string(&index_path) {
            if let Ok(index) = serde_json::from_str::<HashMap<Uuid, ReplayManifest>>(&data) {
                let mut idx = self.index.write();
                let mut player_idx = self.player_index.write();
                
                for (id, manifest) in index {
                    player_idx.entry(manifest.player_id)
                        .or_insert_with(Vec::new)
                        .push(id);
                    idx.insert(id, manifest);
                }
            }
        }
    }

    fn save_index(&self) {
        let index_path = self.storage_path.join("index.json");
        let index = self.index.read();
        if let Ok(data) = serde_json::to_string_pretty(&*index) {
            fs::write(&index_path, data).ok();
        }
    }

    pub fn save_replay(
        &self,
        player_id: Uuid,
        start_time: DateTime<Utc>,
        end_time: DateTime<Utc>,
        start_tick: u64,
        end_tick: u64,
        frames: Vec<CaptureFrame>,
    ) -> Result<Uuid, String> {
        let replay_id = Uuid::new_v4();
        let replay_dir = self.storage_path.join(replay_id.to_string());
        fs::create_dir_all(&replay_dir).map_err(|e| e.to_string())?;

        let frame_count = frames.len();
        let frames_per_segment = 1200;
        let mut segments = Vec::new();
        let mut total_size = 0u64;

        for (i, chunk) in frames.chunks(frames_per_segment).enumerate() {
            let segment_data = serde_json::to_vec(chunk).map_err(|e| e.to_string())?;
            let compressed = Self::compress(&segment_data);
            
            let segment = ReplaySegment {
                segment_id: i as u32,
                start_tick: chunk.first().map(|f| f.tick).unwrap_or(0),
                end_tick: chunk.last().map(|f| f.tick).unwrap_or(0),
                frame_count: chunk.len(),
                data: compressed.clone(),
            };

            let segment_path = replay_dir.join(format!("segment_{:04}.bin", i));
            fs::write(&segment_path, &compressed).map_err(|e| e.to_string())?;
            
            total_size += compressed.len() as u64;
            segments.push(segment);
        }

        let manifest = ReplayManifest {
            id: replay_id,
            player_id,
            player_name: None,
            world: "world".to_string(),
            start_time,
            end_time,
            start_tick,
            end_tick,
            duration_secs: (end_tick - start_tick) / 20,
            frame_count,
            segment_count: segments.len(),
            total_size_bytes: total_size,
            compressed: true,
            capture_center: (0.0, 0.0, 0.0),
            capture_radius: 64.0,
            tags: Vec::new(),
            shared_with: Vec::new(),
        };

        let manifest_path = replay_dir.join("manifest.json");
        let manifest_data = serde_json::to_string_pretty(&manifest).map_err(|e| e.to_string())?;
        fs::write(&manifest_path, &manifest_data).map_err(|e| e.to_string())?;

        self.index.write().insert(replay_id, manifest.clone());
        self.player_index.write()
            .entry(player_id)
            .or_insert_with(Vec::new)
            .push(replay_id);
        
        self.save_index();
        self.cleanup_old_replays();

        Ok(replay_id)
    }

    pub fn load_replay(&self, replay_id: Uuid) -> Result<Vec<CaptureFrame>, String> {
        let manifest = self.get_manifest(replay_id)
            .ok_or("Replay not found")?;
        
        let replay_dir = self.storage_path.join(replay_id.to_string());
        let mut all_frames = Vec::with_capacity(manifest.frame_count);

        for i in 0..manifest.segment_count {
            let segment_path = replay_dir.join(format!("segment_{:04}.bin", i));
            let compressed = fs::read(&segment_path).map_err(|e| e.to_string())?;
            let data = Self::decompress(&compressed)?;
            let frames: Vec<CaptureFrame> = serde_json::from_slice(&data).map_err(|e| e.to_string())?;
            all_frames.extend(frames);
        }

        Ok(all_frames)
    }

    pub fn load_segment(&self, replay_id: Uuid, segment_id: u32) -> Result<Vec<CaptureFrame>, String> {
        let replay_dir = self.storage_path.join(replay_id.to_string());
        let segment_path = replay_dir.join(format!("segment_{:04}.bin", segment_id));
        let compressed = fs::read(&segment_path).map_err(|e| e.to_string())?;
        let data = Self::decompress(&compressed)?;
        serde_json::from_slice(&data).map_err(|e| e.to_string())
    }

    pub fn get_manifest(&self, replay_id: Uuid) -> Option<ReplayManifest> {
        self.index.read().get(&replay_id).cloned()
    }

    pub fn list_replays(&self) -> Vec<ReplayManifest> {
        self.index.read().values().cloned().collect()
    }

    pub fn list_player_replays(&self, player_id: Uuid) -> Vec<ReplayManifest> {
        let player_index = self.player_index.read();
        let index = self.index.read();
        
        player_index.get(&player_id)
            .map(|ids| ids.iter()
                .filter_map(|id| index.get(id).cloned())
                .collect())
            .unwrap_or_default()
    }

    pub fn delete_replay(&self, replay_id: Uuid) -> Result<(), String> {
        let manifest = self.index.write().remove(&replay_id)
            .ok_or("Replay not found")?;
        
        if let Some(replays) = self.player_index.write().get_mut(&manifest.player_id) {
            replays.retain(|id| *id != replay_id);
        }

        let replay_dir = self.storage_path.join(replay_id.to_string());
        fs::remove_dir_all(&replay_dir).map_err(|e| e.to_string())?;
        
        self.save_index();
        Ok(())
    }

    pub fn add_tag(&self, replay_id: Uuid, tag: String) -> Result<(), String> {
        let mut index = self.index.write();
        let manifest = index.get_mut(&replay_id).ok_or("Replay not found")?;
        if !manifest.tags.contains(&tag) {
            manifest.tags.push(tag);
        }
        drop(index);
        self.save_index();
        Ok(())
    }

    pub fn share_with(&self, replay_id: Uuid, target_player: Uuid) -> Result<(), String> {
        let mut index = self.index.write();
        let manifest = index.get_mut(&replay_id).ok_or("Replay not found")?;
        if !manifest.shared_with.contains(&target_player) {
            manifest.shared_with.push(target_player);
        }
        drop(index);
        self.save_index();
        Ok(())
    }

    pub fn get_total_size(&self) -> u64 {
        self.index.read().values().map(|m| m.total_size_bytes).sum()
    }

    fn cleanup_old_replays(&self) {
        let total = self.get_total_size();
        if total <= self.max_storage_bytes {
            return;
        }

        let mut replays: Vec<_> = self.index.read().values().cloned().collect();
        replays.sort_by_key(|r| r.start_time);

        let mut current_size = total;
        for replay in replays {
            if current_size <= self.max_storage_bytes {
                break;
            }
            current_size -= replay.total_size_bytes;
            let _ = self.delete_replay(replay.id);
        }
    }

    fn compress(data: &[u8]) -> Vec<u8> {
        data.to_vec()
    }

    fn decompress(data: &[u8]) -> Result<Vec<u8>, String> {
        Ok(data.to_vec())
    }
}
