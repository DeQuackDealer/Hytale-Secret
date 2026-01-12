use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum MapMode {
    Minimap,
    WorldMap,
    Disabled,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MappingConfig {
    pub enabled: bool,
    pub mode: MapMode,
    pub minimap: MinimapConfig,
    pub worldmap: WorldMapConfig,
    pub markers: MarkerConfig,
    pub permissions: MapPermissions,
}

impl Default for MappingConfig {
    fn default() -> Self {
        Self {
            enabled: true,
            mode: MapMode::Minimap,
            minimap: MinimapConfig::default(),
            worldmap: WorldMapConfig::default(),
            markers: MarkerConfig::default(),
            permissions: MapPermissions::default(),
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MinimapConfig {
    pub enabled: bool,
    pub size: u32,
    pub zoom_min: f32,
    pub zoom_max: f32,
    pub zoom_default: f32,
    pub update_rate_ms: u32,
    pub render_distance: u32,
    pub show_entities: bool,
    pub show_players: bool,
    pub show_mobs: bool,
    pub show_waypoints: bool,
    pub show_north_indicator: bool,
    pub rotate_with_player: bool,
    pub square_map: bool,
    pub transparency: f32,
    pub position: MinimapPosition,
    pub terrain_colors: bool,
    pub cave_mode: bool,
}

impl Default for MinimapConfig {
    fn default() -> Self {
        Self {
            enabled: true,
            size: 150,
            zoom_min: 0.5,
            zoom_max: 4.0,
            zoom_default: 1.0,
            update_rate_ms: 100,
            render_distance: 128,
            show_entities: true,
            show_players: true,
            show_mobs: true,
            show_waypoints: true,
            show_north_indicator: true,
            rotate_with_player: false,
            square_map: false,
            transparency: 1.0,
            position: MinimapPosition::TopRight,
            terrain_colors: true,
            cave_mode: true,
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum MinimapPosition {
    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct WorldMapConfig {
    pub enabled: bool,
    pub max_zoom: f32,
    pub min_zoom: f32,
    pub default_zoom: f32,
    pub chunk_render_distance: u32,
    pub show_biome_colors: bool,
    pub show_heightmap: bool,
    pub show_structure_icons: bool,
    pub show_grid: bool,
    pub show_coordinates: bool,
    pub show_player_trail: bool,
    pub trail_length: u32,
    pub exploration_fog: bool,
    pub share_exploration: bool,
}

impl Default for WorldMapConfig {
    fn default() -> Self {
        Self {
            enabled: true,
            max_zoom: 8.0,
            min_zoom: 0.125,
            default_zoom: 1.0,
            chunk_render_distance: 512,
            show_biome_colors: true,
            show_heightmap: true,
            show_structure_icons: true,
            show_grid: true,
            show_coordinates: true,
            show_player_trail: true,
            trail_length: 1000,
            exploration_fog: true,
            share_exploration: false,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MarkerConfig {
    pub custom_markers_enabled: bool,
    pub max_markers_per_player: u32,
    pub death_markers: bool,
    pub spawn_markers: bool,
    pub poi_markers: bool,
    pub player_markers: bool,
}

impl Default for MarkerConfig {
    fn default() -> Self {
        Self {
            custom_markers_enabled: true,
            max_markers_per_player: 100,
            death_markers: true,
            spawn_markers: true,
            poi_markers: true,
            player_markers: true,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MapPermissions {
    pub allow_minimap: bool,
    pub allow_worldmap: bool,
    pub allow_entity_radar: bool,
    pub allow_cave_mode: bool,
    pub require_exploration: bool,
    pub allowed_dimensions: Vec<String>,
}

impl Default for MapPermissions {
    fn default() -> Self {
        Self {
            allow_minimap: true,
            allow_worldmap: true,
            allow_entity_radar: true,
            allow_cave_mode: true,
            require_exploration: false,
            allowed_dimensions: vec!["overworld".to_string(), "nether".to_string(), "end".to_string()],
        }
    }
}
