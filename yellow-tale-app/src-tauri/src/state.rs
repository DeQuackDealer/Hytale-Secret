use serde::{Deserialize, Serialize};
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct User {
    pub id: Uuid,
    pub username: String,
    pub display_name: Option<String>,
    pub avatar_url: Option<String>,
    pub premium: bool,
    pub equipped_cosmetics: EquippedCosmetics,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct EquippedCosmetics {
    pub skin: Option<String>,
    pub emote_1: Option<String>,
    pub emote_2: Option<String>,
    pub emote_3: Option<String>,
    pub emote_4: Option<String>,
    pub cape: Option<String>,
    pub wings: Option<String>,
    pub aura: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CosmeticItem {
    pub id: String,
    pub name: String,
    pub description: String,
    pub category: String,
    pub thumbnail_url: Option<String>,
    pub rarity: String,
    pub equipped: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PerformanceSettings {
    pub ram_allocation_mb: u32,
    pub use_dedicated_gpu: bool,
    pub fps_limit: Option<u32>,
    pub vsync: bool,
    pub render_distance: u32,
    pub texture_quality: String,
}

impl Default for PerformanceSettings {
    fn default() -> Self {
        Self {
            ram_allocation_mb: 4096,
            use_dedicated_gpu: true,
            fps_limit: None,
            vsync: true,
            render_distance: 12,
            texture_quality: "high".to_string(),
        }
    }
}

pub struct AppState {
    pub user: Option<User>,
    pub token: Option<String>,
    pub api_url: String,
    pub game_path: Option<String>,
    pub java_path: Option<String>,
    pub game_running: bool,
    pub performance: PerformanceSettings,
    pub owned_cosmetics: Vec<CosmeticItem>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct HytaleInstallation {
    pub path: String,
    pub client_path: String,
    pub server_path: String,
    pub assets_path: String,
    pub user_data_path: String,
    pub packs_path: String,
    pub version: Option<String>,
    pub valid: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct JavaInstallation {
    pub path: String,
    pub version: String,
    pub vendor: String,
    pub is_temurin: bool,
    pub is_java_25: bool,
    pub valid: bool,
}

impl AppState {
    pub fn new() -> Self {
        Self {
            user: None,
            token: None,
            api_url: std::env::var("API_URL")
                .unwrap_or_else(|_| "https://yellowtale.com".to_string()),
            game_path: None,
            java_path: None,
            game_running: false,
            performance: PerformanceSettings::default(),
            owned_cosmetics: Vec::new(),
        }
    }
}

impl Default for AppState {
    fn default() -> Self {
        Self::new()
    }
}
