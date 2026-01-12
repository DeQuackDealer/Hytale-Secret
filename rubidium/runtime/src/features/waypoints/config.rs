use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct WaypointConfig {
    pub enabled: bool,
    pub max_waypoints_per_player: u32,
    pub max_shared_waypoints: u32,
    pub features: WaypointFeatures,
    pub display: WaypointDisplay,
    pub permissions: WaypointPermissions,
}

impl Default for WaypointConfig {
    fn default() -> Self {
        Self {
            enabled: true,
            max_waypoints_per_player: 100,
            max_shared_waypoints: 50,
            features: WaypointFeatures::default(),
            display: WaypointDisplay::default(),
            permissions: WaypointPermissions::default(),
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct WaypointFeatures {
    pub death_waypoints: bool,
    pub spawn_waypoints: bool,
    pub home_waypoints: bool,
    pub portal_waypoints: bool,
    pub custom_waypoints: bool,
    pub temporary_waypoints: bool,
    pub shared_waypoints: bool,
    pub global_waypoints: bool,
    pub distance_display: bool,
    pub direction_display: bool,
    pub beam_display: bool,
    pub sound_alerts: bool,
    pub proximity_alerts: bool,
    pub auto_discovery: bool,
    pub coordinate_display: bool,
    pub waypoint_sorting: bool,
    pub waypoint_filtering: bool,
    pub waypoint_search: bool,
    pub waypoint_colors: bool,
    pub waypoint_icons: bool,
    pub waypoint_groups: bool,
}

impl Default for WaypointFeatures {
    fn default() -> Self {
        Self {
            death_waypoints: true,
            spawn_waypoints: true,
            home_waypoints: true,
            portal_waypoints: true,
            custom_waypoints: true,
            temporary_waypoints: true,
            shared_waypoints: true,
            global_waypoints: false,
            distance_display: true,
            direction_display: true,
            beam_display: true,
            sound_alerts: false,
            proximity_alerts: true,
            auto_discovery: true,
            coordinate_display: true,
            waypoint_sorting: true,
            waypoint_filtering: true,
            waypoint_search: true,
            waypoint_colors: true,
            waypoint_icons: true,
            waypoint_groups: true,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct WaypointDisplay {
    pub render_distance: f64,
    pub min_render_distance: f64,
    pub fade_near_distance: f64,
    pub fade_far_distance: f64,
    pub beam_height: f64,
    pub beam_width: f32,
    pub icon_size: f32,
    pub text_size: f32,
    pub show_distance: bool,
    pub show_y_level: bool,
    pub show_dimension: bool,
    pub compass_enabled: bool,
    pub hud_enabled: bool,
}

impl Default for WaypointDisplay {
    fn default() -> Self {
        Self {
            render_distance: 10000.0,
            min_render_distance: 4.0,
            fade_near_distance: 8.0,
            fade_far_distance: 1000.0,
            beam_height: 256.0,
            beam_width: 1.0,
            icon_size: 1.0,
            text_size: 1.0,
            show_distance: true,
            show_y_level: true,
            show_dimension: true,
            compass_enabled: true,
            hud_enabled: true,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct WaypointPermissions {
    pub allow_create: bool,
    pub allow_edit: bool,
    pub allow_delete: bool,
    pub allow_share: bool,
    pub allow_global: bool,
    pub allow_beam: bool,
    pub allow_sound: bool,
    pub allowed_dimensions: Vec<String>,
    pub max_name_length: usize,
    pub blacklisted_names: Vec<String>,
}

impl Default for WaypointPermissions {
    fn default() -> Self {
        Self {
            allow_create: true,
            allow_edit: true,
            allow_delete: true,
            allow_share: true,
            allow_global: false,
            allow_beam: true,
            allow_sound: true,
            allowed_dimensions: vec![
                "overworld".to_string(),
                "nether".to_string(),
                "end".to_string(),
            ],
            max_name_length: 32,
            blacklisted_names: Vec::new(),
        }
    }
}
