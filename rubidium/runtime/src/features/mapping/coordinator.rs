use super::config::{MappingConfig, MapMode};
use super::minimap::{MinimapService, MinimapData};
use super::worldmap::{WorldMapService, WorldMapData};
use super::markers::MarkerRegistry;
use parking_lot::RwLock;
use std::sync::Arc;
use uuid::Uuid;

pub enum MapData {
    Minimap(MinimapData),
    WorldMap(WorldMapData),
    Disabled,
}

pub struct MappingCoordinator {
    config: Arc<RwLock<MappingConfig>>,
    minimap: MinimapService,
    worldmap: WorldMapService,
    markers: Arc<MarkerRegistry>,
}

impl MappingCoordinator {
    pub fn new(config: MappingConfig) -> Self {
        let markers = Arc::new(MarkerRegistry::new());
        let minimap = MinimapService::new(config.minimap.clone(), markers.clone());
        let worldmap = WorldMapService::new(config.worldmap.clone(), markers.clone());
        
        Self {
            config: Arc::new(RwLock::new(config)),
            minimap,
            worldmap,
            markers,
        }
    }

    pub fn is_enabled(&self) -> bool {
        let config = self.config.read();
        config.enabled && config.mode != MapMode::Disabled
    }

    pub fn get_mode(&self) -> MapMode {
        self.config.read().mode
    }

    pub fn set_mode(&self, mode: MapMode) {
        self.config.write().mode = mode;
    }

    pub fn is_minimap_allowed(&self) -> bool {
        let config = self.config.read();
        config.enabled && config.mode == MapMode::Minimap && config.permissions.allow_minimap
    }

    pub fn is_worldmap_allowed(&self) -> bool {
        let config = self.config.read();
        config.enabled && config.mode == MapMode::WorldMap && config.permissions.allow_worldmap
    }

    pub fn update_player_position(&self, player_id: Uuid, x: f64, y: i32, z: f64, yaw: f32, dimension: &str) {
        if !self.is_enabled() {
            return;
        }

        let mode = self.get_mode();
        match mode {
            MapMode::Minimap => {
                self.minimap.update_player_position(player_id, x, y, z, yaw, dimension);
            }
            MapMode::WorldMap => {
                self.worldmap.update_player_position(player_id, x, z, dimension);
            }
            MapMode::Disabled => {}
        }
    }

    pub fn get_map_data(&self, player_id: Uuid) -> MapData {
        if !self.is_enabled() {
            return MapData::Disabled;
        }

        let mode = self.get_mode();
        match mode {
            MapMode::Minimap => {
                if !self.is_minimap_allowed() {
                    return MapData::Disabled;
                }
                self.minimap.get_minimap_data(player_id)
                    .map(MapData::Minimap)
                    .unwrap_or(MapData::Disabled)
            }
            MapMode::WorldMap => {
                if !self.is_worldmap_allowed() {
                    return MapData::Disabled;
                }
                self.worldmap.get_world_map_data(player_id)
                    .map(MapData::WorldMap)
                    .unwrap_or(MapData::Disabled)
            }
            MapMode::Disabled => MapData::Disabled,
        }
    }

    pub fn open_world_map(&self, player_id: Uuid, x: f64, z: f64, dimension: &str) -> Result<(), String> {
        if !self.is_worldmap_allowed() {
            return Err("World map is not available. Admin has selected minimap mode.".to_string());
        }
        self.worldmap.open_map(player_id, x, z, dimension);
        Ok(())
    }

    pub fn close_world_map(&self, player_id: Uuid) {
        self.worldmap.close_map(player_id);
    }

    pub fn pan_world_map(&self, player_id: Uuid, dx: f64, dz: f64) -> Result<(), String> {
        if !self.is_worldmap_allowed() {
            return Err("World map is not available".to_string());
        }
        self.worldmap.pan(player_id, dx, dz);
        Ok(())
    }

    pub fn zoom_minimap(&self, player_id: Uuid, zoom: f32) -> Result<(), String> {
        if !self.is_minimap_allowed() {
            return Err("Minimap is not available".to_string());
        }
        self.minimap.set_zoom(player_id, zoom);
        Ok(())
    }

    pub fn toggle_cave_mode(&self, player_id: Uuid) -> Result<(), String> {
        let config = self.config.read();
        if !config.permissions.allow_cave_mode {
            return Err("Cave mode is not allowed".to_string());
        }
        drop(config);

        if !self.is_minimap_allowed() {
            return Err("Minimap is not available".to_string());
        }
        self.minimap.toggle_cave_mode(player_id);
        Ok(())
    }

    pub fn markers(&self) -> &Arc<MarkerRegistry> {
        &self.markers
    }

    pub fn minimap(&self) -> Option<&MinimapService> {
        if self.is_minimap_allowed() {
            Some(&self.minimap)
        } else {
            None
        }
    }

    pub fn worldmap(&self) -> Option<&WorldMapService> {
        if self.is_worldmap_allowed() {
            Some(&self.worldmap)
        } else {
            None
        }
    }

    pub fn config(&self) -> &Arc<RwLock<MappingConfig>> {
        &self.config
    }
}
