use serde::{Deserialize, Serialize};
use std::path::PathBuf;
use std::time::Duration;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ReplayConfig {
    pub enabled: bool,
    pub capture_radius: f64,
    pub capture_height: f64,
    pub max_duration_secs: u64,
    pub storage_path: PathBuf,
    pub compression_enabled: bool,
    pub compression_level: u32,
    pub retention_days: u32,
    pub max_storage_gb: f64,
    pub tick_sample_rate: u32,
    pub entity_capture: EntityCaptureConfig,
    pub block_capture: BlockCaptureConfig,
    pub audio_capture: bool,
    pub permissions: ReplayPermissions,
}

impl Default for ReplayConfig {
    fn default() -> Self {
        Self {
            enabled: true,
            capture_radius: 64.0,
            capture_height: 32.0,
            max_duration_secs: 3600,
            storage_path: PathBuf::from("replays"),
            compression_enabled: true,
            compression_level: 6,
            retention_days: 30,
            max_storage_gb: 50.0,
            tick_sample_rate: 1,
            entity_capture: EntityCaptureConfig::default(),
            block_capture: BlockCaptureConfig::default(),
            audio_capture: false,
            permissions: ReplayPermissions::default(),
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct EntityCaptureConfig {
    pub players: bool,
    pub mobs: bool,
    pub projectiles: bool,
    pub particles: bool,
    pub items: bool,
}

impl Default for EntityCaptureConfig {
    fn default() -> Self {
        Self {
            players: true,
            mobs: true,
            projectiles: true,
            particles: true,
            items: true,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct BlockCaptureConfig {
    pub block_changes: bool,
    pub block_interactions: bool,
    pub explosions: bool,
    pub fluid_flow: bool,
}

impl Default for BlockCaptureConfig {
    fn default() -> Self {
        Self {
            block_changes: true,
            block_interactions: true,
            explosions: true,
            fluid_flow: false,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ReplayPermissions {
    pub allow_self_recording: bool,
    pub allow_self_playback: bool,
    pub allow_share_replays: bool,
    pub admin_can_view_all: bool,
    pub require_consent_for_others: bool,
    pub allowed_roles: Vec<String>,
}

impl Default for ReplayPermissions {
    fn default() -> Self {
        Self {
            allow_self_recording: true,
            allow_self_playback: true,
            allow_share_replays: false,
            admin_can_view_all: true,
            require_consent_for_others: true,
            allowed_roles: vec!["player".to_string()],
        }
    }
}
