use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CinemaConfig {
    pub enabled: bool,
    pub max_path_duration_secs: u32,
    pub max_keyframes: u32,
    pub smooth_interpolation: bool,
    pub allow_player_focus: bool,
    pub allow_time_control: bool,
    pub permissions: CinemaPermissions,
}

impl Default for CinemaConfig {
    fn default() -> Self {
        Self {
            enabled: false,
            max_path_duration_secs: 300,
            max_keyframes: 100,
            smooth_interpolation: true,
            allow_player_focus: true,
            allow_time_control: false,
            permissions: CinemaPermissions::default(),
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CinemaPermissions {
    pub allow_create: bool,
    pub allow_save: bool,
    pub allow_share: bool,
    pub allowed_roles: Vec<String>,
}

impl Default for CinemaPermissions {
    fn default() -> Self {
        Self {
            allow_create: true,
            allow_save: true,
            allow_share: false,
            allowed_roles: vec!["admin".to_string(), "moderator".to_string()],
        }
    }
}
