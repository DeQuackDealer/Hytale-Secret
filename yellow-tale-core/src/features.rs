use serde::{Deserialize, Serialize};
use std::collections::HashMap;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FeatureGate {
    pub id: String,
    pub name: String,
    pub description: String,
    pub tier: FeatureTier,
    pub requires_game_api: bool,
    pub enabled: bool,
    pub category: FeatureCategory,
}

#[derive(Debug, Clone, Copy, Serialize, Deserialize, PartialEq, Eq)]
pub enum FeatureTier {
    Free,
    PremiumLauncher,
    PremiumPond,
}

#[derive(Debug, Clone, Copy, Serialize, Deserialize, PartialEq, Eq)]
pub enum FeatureCategory {
    Launcher,
    Performance,
    Social,
    Server,
    Cosmetics,
    Mods,
    Diagnostics,
}

pub struct FeatureManager {
    features: HashMap<String, FeatureGate>,
    premium_launcher: bool,
    premium_pond: bool,
    game_api_available: bool,
}

impl FeatureManager {
    pub fn new() -> Self {
        let mut manager = Self {
            features: HashMap::new(),
            premium_launcher: false,
            premium_pond: false,
            game_api_available: false,
        };
        
        manager.register_default_features();
        manager.recalculate();
        manager
    }
    
    fn register_default_features(&mut self) {
        let defaults = vec![
            FeatureGate {
                id: "launcher.core".to_string(),
                name: "Core Launcher".to_string(),
                description: "Basic launcher functionality".to_string(),
                tier: FeatureTier::Free,
                requires_game_api: false,
                enabled: true,
                category: FeatureCategory::Launcher,
            },
            FeatureGate {
                id: "launcher.profiles".to_string(),
                name: "Profile Management".to_string(),
                description: "Create and manage game profiles".to_string(),
                tier: FeatureTier::Free,
                requires_game_api: false,
                enabled: true,
                category: FeatureCategory::Launcher,
            },
            FeatureGate {
                id: "launcher.mods.basic".to_string(),
                name: "Basic Mod Management".to_string(),
                description: "Install and manage mods".to_string(),
                tier: FeatureTier::Free,
                requires_game_api: false,
                enabled: true,
                category: FeatureCategory::Mods,
            },
            FeatureGate {
                id: "launcher.mods.conflicts".to_string(),
                name: "Mod Conflict Detection".to_string(),
                description: "Detect conflicts between mods".to_string(),
                tier: FeatureTier::PremiumLauncher,
                requires_game_api: false,
                enabled: false,
                category: FeatureCategory::Mods,
            },
            FeatureGate {
                id: "launcher.performance.basic".to_string(),
                name: "Basic Performance Settings".to_string(),
                description: "RAM and JVM settings".to_string(),
                tier: FeatureTier::Free,
                requires_game_api: false,
                enabled: true,
                category: FeatureCategory::Performance,
            },
            FeatureGate {
                id: "launcher.performance.advanced".to_string(),
                name: "Advanced Performance Tuning".to_string(),
                description: "GC presets, smart JVM tuning".to_string(),
                tier: FeatureTier::PremiumLauncher,
                requires_game_api: false,
                enabled: false,
                category: FeatureCategory::Performance,
            },
            FeatureGate {
                id: "launcher.diagnostics.basic".to_string(),
                name: "Basic Diagnostics".to_string(),
                description: "RAM and FPS monitoring".to_string(),
                tier: FeatureTier::Free,
                requires_game_api: false,
                enabled: true,
                category: FeatureCategory::Diagnostics,
            },
            FeatureGate {
                id: "launcher.diagnostics.advanced".to_string(),
                name: "Advanced Diagnostics".to_string(),
                description: "Live graphs, plugin CPU usage".to_string(),
                tier: FeatureTier::PremiumLauncher,
                requires_game_api: false,
                enabled: false,
                category: FeatureCategory::Diagnostics,
            },
            FeatureGate {
                id: "launcher.sync".to_string(),
                name: "Profile Sync".to_string(),
                description: "Sync profiles across devices".to_string(),
                tier: FeatureTier::PremiumLauncher,
                requires_game_api: false,
                enabled: false,
                category: FeatureCategory::Launcher,
            },
            FeatureGate {
                id: "social.friends".to_string(),
                name: "Friends System".to_string(),
                description: "Add friends and see online status".to_string(),
                tier: FeatureTier::Free,
                requires_game_api: false,
                enabled: true,
                category: FeatureCategory::Social,
            },
            FeatureGate {
                id: "social.party".to_string(),
                name: "Party System".to_string(),
                description: "Create parties and join servers together".to_string(),
                tier: FeatureTier::Free,
                requires_game_api: true,
                enabled: false,
                category: FeatureCategory::Social,
            },
            FeatureGate {
                id: "cosmetics.local".to_string(),
                name: "Local Cosmetics".to_string(),
                description: "Cosmetics visible only to you".to_string(),
                tier: FeatureTier::Free,
                requires_game_api: true,
                enabled: false,
                category: FeatureCategory::Cosmetics,
            },
            FeatureGate {
                id: "cosmetics.server".to_string(),
                name: "Server Cosmetics".to_string(),
                description: "Cosmetics visible to others (server-approved)".to_string(),
                tier: FeatureTier::Free,
                requires_game_api: true,
                enabled: false,
                category: FeatureCategory::Cosmetics,
            },
            FeatureGate {
                id: "pond.core".to_string(),
                name: "Pond Core".to_string(),
                description: "Basic server integration".to_string(),
                tier: FeatureTier::Free,
                requires_game_api: true,
                enabled: false,
                category: FeatureCategory::Server,
            },
            FeatureGate {
                id: "pond.plugins".to_string(),
                name: "Plugin System".to_string(),
                description: "Load and manage Pond plugins".to_string(),
                tier: FeatureTier::Free,
                requires_game_api: true,
                enabled: false,
                category: FeatureCategory::Server,
            },
            FeatureGate {
                id: "pond.performance.basic".to_string(),
                name: "Basic Server Optimization".to_string(),
                description: "Standard tick scheduling".to_string(),
                tier: FeatureTier::Free,
                requires_game_api: true,
                enabled: false,
                category: FeatureCategory::Server,
            },
            FeatureGate {
                id: "pond.performance.advanced".to_string(),
                name: "Advanced Server Optimization".to_string(),
                description: "Tick budgeting, async scheduling".to_string(),
                tier: FeatureTier::PremiumPond,
                requires_game_api: true,
                enabled: false,
                category: FeatureCategory::Server,
            },
            FeatureGate {
                id: "pond.analytics".to_string(),
                name: "Server Analytics".to_string(),
                description: "Detailed performance analytics".to_string(),
                tier: FeatureTier::PremiumPond,
                requires_game_api: true,
                enabled: false,
                category: FeatureCategory::Server,
            },
            FeatureGate {
                id: "pond.hotreload".to_string(),
                name: "Plugin Hot Reload".to_string(),
                description: "Reload plugins without restart".to_string(),
                tier: FeatureTier::PremiumPond,
                requires_game_api: true,
                enabled: false,
                category: FeatureCategory::Server,
            },
        ];
        
        for feature in defaults {
            self.features.insert(feature.id.clone(), feature);
        }
    }
    
    pub fn set_premium_launcher(&mut self, premium: bool) {
        self.premium_launcher = premium;
        self.recalculate();
    }
    
    pub fn set_premium_pond(&mut self, premium: bool) {
        self.premium_pond = premium;
        self.recalculate();
    }
    
    pub fn set_game_api_available(&mut self, available: bool) {
        self.game_api_available = available;
        self.recalculate();
    }
    
    fn recalculate(&mut self) {
        for feature in self.features.values_mut() {
            let tier_ok = match feature.tier {
                FeatureTier::Free => true,
                FeatureTier::PremiumLauncher => self.premium_launcher,
                FeatureTier::PremiumPond => self.premium_pond,
            };
            
            let api_ok = !feature.requires_game_api || self.game_api_available;
            
            feature.enabled = tier_ok && api_ok;
        }
    }
    
    pub fn is_enabled(&self, feature_id: &str) -> bool {
        self.features.get(feature_id).map(|f| f.enabled).unwrap_or(false)
    }
    
    pub fn get(&self, feature_id: &str) -> Option<&FeatureGate> {
        self.features.get(feature_id)
    }
    
    pub fn list_all(&self) -> Vec<&FeatureGate> {
        self.features.values().collect()
    }
    
    pub fn list_enabled(&self) -> Vec<&FeatureGate> {
        self.features.values().filter(|f| f.enabled).collect()
    }
    
    pub fn list_by_category(&self, category: FeatureCategory) -> Vec<&FeatureGate> {
        self.features.values().filter(|f| f.category == category).collect()
    }
    
    pub fn list_by_tier(&self, tier: FeatureTier) -> Vec<&FeatureGate> {
        self.features.values().filter(|f| f.tier == tier).collect()
    }
    
    pub fn to_json(&self) -> serde_json::Value {
        let features: Vec<_> = self.features.values().collect();
        serde_json::json!({
            "premium_launcher": self.premium_launcher,
            "premium_pond": self.premium_pond,
            "game_api_available": self.game_api_available,
            "features": features,
        })
    }
}

impl Default for FeatureManager {
    fn default() -> Self {
        Self::new()
    }
}

#[macro_export]
macro_rules! when_feature {
    ($manager:expr, $feature:expr, $body:block) => {
        if $manager.is_enabled($feature) {
            $body
        }
    };
    ($manager:expr, $feature:expr, $body:block else $else_body:block) => {
        if $manager.is_enabled($feature) {
            $body
        } else {
            $else_body
        }
    };
}
