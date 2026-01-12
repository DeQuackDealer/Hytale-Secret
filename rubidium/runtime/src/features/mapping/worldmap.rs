use super::config::WorldMapConfig;
use super::markers::MarkerRegistry;
use super::minimap::ChunkData;
use dashmap::DashMap;
use parking_lot::RwLock;
use serde::{Deserialize, Serialize};
use std::collections::VecDeque;
use std::sync::Arc;
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct WorldMapState {
    pub player_id: Uuid,
    pub view_center_x: f64,
    pub view_center_z: f64,
    pub zoom: f32,
    pub dimension: String,
    pub show_grid: bool,
    pub show_coordinates: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PlayerTrail {
    pub positions: VecDeque<(f64, f64)>,
    pub max_length: usize,
}

impl PlayerTrail {
    pub fn new(max_length: usize) -> Self {
        Self {
            positions: VecDeque::with_capacity(max_length),
            max_length,
        }
    }

    pub fn add_position(&mut self, x: f64, z: f64) {
        if self.positions.len() >= self.max_length {
            self.positions.pop_front();
        }
        self.positions.push_back((x, z));
    }
}

pub struct WorldMapService {
    config: Arc<RwLock<WorldMapConfig>>,
    chunk_cache: DashMap<(i32, i32, String), ChunkData>,
    player_states: DashMap<Uuid, WorldMapState>,
    player_trails: DashMap<Uuid, PlayerTrail>,
    markers: Arc<MarkerRegistry>,
    exploration: DashMap<(Uuid, i32, i32, String), bool>,
}

impl WorldMapService {
    pub fn new(config: WorldMapConfig, markers: Arc<MarkerRegistry>) -> Self {
        Self {
            config: Arc::new(RwLock::new(config)),
            chunk_cache: DashMap::new(),
            player_states: DashMap::new(),
            player_trails: DashMap::new(),
            markers,
            exploration: DashMap::new(),
        }
    }

    pub fn update_player_position(&self, player_id: Uuid, x: f64, z: f64, dimension: &str) {
        let chunk_x = (x / 16.0).floor() as i32;
        let chunk_z = (z / 16.0).floor() as i32;
        self.exploration.insert((player_id, chunk_x, chunk_z, dimension.to_string()), true);
        
        let config = self.config.read();
        let trail_length = config.trail_length as usize;
        drop(config);
        
        self.player_trails
            .entry(player_id)
            .or_insert_with(|| PlayerTrail::new(trail_length))
            .add_position(x, z);
    }

    pub fn open_map(&self, player_id: Uuid, center_x: f64, center_z: f64, dimension: &str) {
        let config = self.config.read();
        
        let state = WorldMapState {
            player_id,
            view_center_x: center_x,
            view_center_z: center_z,
            zoom: config.default_zoom,
            dimension: dimension.to_string(),
            show_grid: config.show_grid,
            show_coordinates: config.show_coordinates,
        };
        
        self.player_states.insert(player_id, state);
    }

    pub fn close_map(&self, player_id: Uuid) {
        self.player_states.remove(&player_id);
    }

    pub fn pan(&self, player_id: Uuid, dx: f64, dz: f64) {
        if let Some(mut state) = self.player_states.get_mut(&player_id) {
            let scale = 1.0 / state.zoom as f64;
            state.view_center_x += dx * scale;
            state.view_center_z += dz * scale;
        }
    }

    pub fn zoom(&self, player_id: Uuid, zoom: f32) {
        if let Some(mut state) = self.player_states.get_mut(&player_id) {
            let config = self.config.read();
            state.zoom = zoom.clamp(config.min_zoom, config.max_zoom);
        }
    }

    pub fn zoom_in(&self, player_id: Uuid) {
        if let Some(mut state) = self.player_states.get_mut(&player_id) {
            let config = self.config.read();
            state.zoom = (state.zoom * 2.0).min(config.max_zoom);
        }
    }

    pub fn zoom_out(&self, player_id: Uuid) {
        if let Some(mut state) = self.player_states.get_mut(&player_id) {
            let config = self.config.read();
            state.zoom = (state.zoom / 2.0).max(config.min_zoom);
        }
    }

    pub fn center_on_player(&self, player_id: Uuid, player_x: f64, player_z: f64) {
        if let Some(mut state) = self.player_states.get_mut(&player_id) {
            state.view_center_x = player_x;
            state.view_center_z = player_z;
        }
    }

    pub fn center_on_waypoint(&self, player_id: Uuid, waypoint_x: f64, waypoint_z: f64) {
        if let Some(mut state) = self.player_states.get_mut(&player_id) {
            state.view_center_x = waypoint_x;
            state.view_center_z = waypoint_z;
        }
    }

    pub fn update_chunk(&self, chunk_x: i32, chunk_z: i32, dimension: &str, data: ChunkData) {
        self.chunk_cache.insert((chunk_x, chunk_z, dimension.to_string()), data);
    }

    pub fn get_world_map_data(&self, player_id: Uuid) -> Option<WorldMapData> {
        let state = self.player_states.get(&player_id)?;
        let config = self.config.read();
        
        let center_chunk_x = (state.view_center_x / 16.0).floor() as i32;
        let center_chunk_z = (state.view_center_z / 16.0).floor() as i32;
        let view_radius = (config.chunk_render_distance as f32 / state.zoom) as i32;
        
        let mut chunks = Vec::new();
        for dx in -view_radius..=view_radius {
            for dz in -view_radius..=view_radius {
                let key = (center_chunk_x + dx, center_chunk_z + dz, state.dimension.clone());
                
                let explored = !config.exploration_fog || 
                    self.exploration.contains_key(&(player_id, center_chunk_x + dx, center_chunk_z + dz, state.dimension.clone()));
                
                if explored {
                    if let Some(chunk) = self.chunk_cache.get(&key) {
                        chunks.push(chunk.clone());
                    }
                }
            }
        }

        let waypoints = self.markers.get_all_markers_in_dimension(&state.dimension);
        
        let trail = self.player_trails.get(&player_id)
            .map(|t| t.positions.iter().cloned().collect())
            .unwrap_or_default();

        Some(WorldMapData {
            state: state.clone(),
            chunks,
            waypoints,
            player_trail: trail,
        })
    }

    pub fn toggle_grid(&self, player_id: Uuid) {
        if let Some(mut state) = self.player_states.get_mut(&player_id) {
            state.show_grid = !state.show_grid;
        }
    }

    pub fn toggle_coordinates(&self, player_id: Uuid) {
        if let Some(mut state) = self.player_states.get_mut(&player_id) {
            state.show_coordinates = !state.show_coordinates;
        }
    }

    pub fn config(&self) -> &Arc<RwLock<WorldMapConfig>> {
        &self.config
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct WorldMapData {
    pub state: WorldMapState,
    pub chunks: Vec<ChunkData>,
    pub waypoints: Vec<super::markers::MapMarker>,
    pub player_trail: Vec<(f64, f64)>,
}
