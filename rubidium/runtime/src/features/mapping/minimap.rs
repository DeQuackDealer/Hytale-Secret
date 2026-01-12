use super::config::MinimapConfig;
use super::markers::MarkerRegistry;
use dashmap::DashMap;
use parking_lot::RwLock;
use serde::{Deserialize, Serialize};
use std::sync::Arc;
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ChunkData {
    pub x: i32,
    pub z: i32,
    pub heightmap: Vec<u8>,
    pub color_data: Vec<u32>,
    pub biome_id: u32,
    pub last_update: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct EntityMarker {
    pub id: Uuid,
    pub entity_type: String,
    pub x: f64,
    pub z: f64,
    pub yaw: f32,
    pub is_hostile: bool,
    pub is_player: bool,
    pub name: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MinimapState {
    pub player_id: Uuid,
    pub center_x: f64,
    pub center_z: f64,
    pub player_yaw: f32,
    pub zoom: f32,
    pub dimension: String,
    pub cave_mode: bool,
    pub y_level: i32,
}

pub struct MinimapService {
    config: Arc<RwLock<MinimapConfig>>,
    chunk_cache: DashMap<(i32, i32, String), ChunkData>,
    player_states: DashMap<Uuid, MinimapState>,
    markers: Arc<MarkerRegistry>,
    exploration: DashMap<(Uuid, i32, i32, String), bool>,
}

impl MinimapService {
    pub fn new(config: MinimapConfig, markers: Arc<MarkerRegistry>) -> Self {
        Self {
            config: Arc::new(RwLock::new(config)),
            chunk_cache: DashMap::new(),
            player_states: DashMap::new(),
            markers,
            exploration: DashMap::new(),
        }
    }

    pub fn update_player_position(&self, player_id: Uuid, x: f64, y: i32, z: f64, yaw: f32, dimension: &str) {
        let config = self.config.read();
        
        let state = MinimapState {
            player_id,
            center_x: x,
            center_z: z,
            player_yaw: yaw,
            zoom: config.zoom_default,
            dimension: dimension.to_string(),
            cave_mode: y < 60,
            y_level: y,
        };
        
        self.player_states.insert(player_id, state);
        
        let chunk_x = (x / 16.0).floor() as i32;
        let chunk_z = (z / 16.0).floor() as i32;
        self.exploration.insert((player_id, chunk_x, chunk_z, dimension.to_string()), true);
    }

    pub fn update_chunk(&self, chunk_x: i32, chunk_z: i32, dimension: &str, heightmap: Vec<u8>, colors: Vec<u32>, biome: u32) {
        let chunk = ChunkData {
            x: chunk_x,
            z: chunk_z,
            heightmap,
            color_data: colors,
            biome_id: biome,
            last_update: std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .unwrap()
                .as_secs(),
        };
        
        self.chunk_cache.insert((chunk_x, chunk_z, dimension.to_string()), chunk);
    }

    pub fn get_minimap_data(&self, player_id: Uuid) -> Option<MinimapData> {
        let state = self.player_states.get(&player_id)?;
        let config = self.config.read();
        
        let center_chunk_x = (state.center_x / 16.0).floor() as i32;
        let center_chunk_z = (state.center_z / 16.0).floor() as i32;
        let render_chunks = (config.render_distance / 16) as i32;
        
        let mut chunks = Vec::new();
        for dx in -render_chunks..=render_chunks {
            for dz in -render_chunks..=render_chunks {
                let key = (center_chunk_x + dx, center_chunk_z + dz, state.dimension.clone());
                if let Some(chunk) = self.chunk_cache.get(&key) {
                    chunks.push(chunk.clone());
                }
            }
        }

        let mut entities = Vec::new();
        
        let waypoints = self.markers.get_visible_markers(player_id, &state.dimension);

        Some(MinimapData {
            state: state.clone(),
            chunks,
            entities,
            waypoints,
        })
    }

    pub fn set_zoom(&self, player_id: Uuid, zoom: f32) {
        if let Some(mut state) = self.player_states.get_mut(&player_id) {
            let config = self.config.read();
            state.zoom = zoom.clamp(config.zoom_min, config.zoom_max);
        }
    }

    pub fn toggle_cave_mode(&self, player_id: Uuid) {
        if let Some(mut state) = self.player_states.get_mut(&player_id) {
            state.cave_mode = !state.cave_mode;
        }
    }

    pub fn is_chunk_explored(&self, player_id: Uuid, chunk_x: i32, chunk_z: i32, dimension: &str) -> bool {
        self.exploration.contains_key(&(player_id, chunk_x, chunk_z, dimension.to_string()))
    }

    pub fn get_exploration_percentage(&self, player_id: Uuid, dimension: &str) -> f64 {
        let explored = self.exploration.iter()
            .filter(|e| e.key().0 == player_id && e.key().3 == dimension)
            .count();
        
        let total_cached = self.chunk_cache.iter()
            .filter(|e| e.key().2 == dimension)
            .count();
        
        if total_cached == 0 {
            0.0
        } else {
            (explored as f64 / total_cached as f64) * 100.0
        }
    }

    pub fn config(&self) -> &Arc<RwLock<MinimapConfig>> {
        &self.config
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MinimapData {
    pub state: MinimapState,
    pub chunks: Vec<ChunkData>,
    pub entities: Vec<EntityMarker>,
    pub waypoints: Vec<super::markers::MapMarker>,
}
