use serde::{Deserialize, Serialize};
use std::collections::HashMap;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ToggleConfig {
    pub features: HashMap<String, FeatureSettings>,
    pub role_defaults: HashMap<String, Vec<String>>,
    pub audit_enabled: bool,
}

impl Default for ToggleConfig {
    fn default() -> Self {
        let mut features = HashMap::new();
        
        features.insert("replay".to_string(), FeatureSettings::enabled("Replay System", "Record and playback gameplay"));
        features.insert("replay.capture".to_string(), FeatureSettings::enabled("Replay Capture", "Allow players to record"));
        features.insert("replay.playback".to_string(), FeatureSettings::enabled("Replay Playback", "Allow players to watch replays"));
        features.insert("replay.share".to_string(), FeatureSettings::disabled("Replay Sharing", "Allow sharing replays"));
        
        features.insert("mapping".to_string(), FeatureSettings::enabled("Mapping System", "Minimap and world map"));
        features.insert("mapping.minimap".to_string(), FeatureSettings::enabled("Minimap", "Show minimap HUD"));
        features.insert("mapping.worldmap".to_string(), FeatureSettings::enabled("World Map", "Full world map view"));
        features.insert("mapping.entities".to_string(), FeatureSettings::enabled("Entity Radar", "Show entities on map"));
        features.insert("mapping.cave_mode".to_string(), FeatureSettings::enabled("Cave Mode", "Underground map mode"));
        
        features.insert("waypoints".to_string(), FeatureSettings::enabled("Waypoints", "Location markers"));
        features.insert("waypoints.create".to_string(), FeatureSettings::enabled("Create Waypoints", "Allow creating waypoints"));
        features.insert("waypoints.death".to_string(), FeatureSettings::enabled("Death Waypoints", "Auto-create on death"));
        features.insert("waypoints.share".to_string(), FeatureSettings::enabled("Share Waypoints", "Allow sharing waypoints"));
        features.insert("waypoints.beam".to_string(), FeatureSettings::enabled("Waypoint Beams", "Beacon beam display"));
        features.insert("waypoints.sound".to_string(), FeatureSettings::disabled("Waypoint Sounds", "Audio alerts"));
        
        features.insert("social".to_string(), FeatureSettings::enabled("Social Features", "Friends and parties"));
        features.insert("social.parties".to_string(), FeatureSettings::enabled("Parties", "Party system"));
        features.insert("social.presence".to_string(), FeatureSettings::enabled("Presence", "Online status"));
        
        features.insert("cinema".to_string(), FeatureSettings::disabled("Cinematic Camera", "Advanced camera modes"));
        features.insert("events".to_string(), FeatureSettings::disabled("Events Calendar", "Server events"));

        let mut role_defaults = HashMap::new();
        role_defaults.insert("admin".to_string(), vec!["*".to_string()]);
        role_defaults.insert("moderator".to_string(), vec![
            "replay".to_string(),
            "mapping".to_string(),
            "waypoints".to_string(),
            "social".to_string(),
        ]);
        role_defaults.insert("player".to_string(), vec![
            "mapping.minimap".to_string(),
            "waypoints.create".to_string(),
            "waypoints.death".to_string(),
            "social.presence".to_string(),
        ]);

        Self {
            features,
            role_defaults,
            audit_enabled: true,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FeatureSettings {
    pub name: String,
    pub description: String,
    pub enabled: bool,
    pub premium_only: bool,
    pub requires_api: bool,
    pub parent: Option<String>,
    pub conflicts_with: Vec<String>,
}

impl FeatureSettings {
    pub fn enabled(name: &str, description: &str) -> Self {
        Self {
            name: name.to_string(),
            description: description.to_string(),
            enabled: true,
            premium_only: false,
            requires_api: false,
            parent: None,
            conflicts_with: Vec::new(),
        }
    }

    pub fn disabled(name: &str, description: &str) -> Self {
        Self {
            name: name.to_string(),
            description: description.to_string(),
            enabled: false,
            premium_only: false,
            requires_api: false,
            parent: None,
            conflicts_with: Vec::new(),
        }
    }

    pub fn premium(name: &str, description: &str) -> Self {
        Self {
            name: name.to_string(),
            description: description.to_string(),
            enabled: true,
            premium_only: true,
            requires_api: false,
            parent: None,
            conflicts_with: Vec::new(),
        }
    }
}
