use dashmap::DashMap;
use serde::{Deserialize, Serialize};
use std::sync::Arc;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FeatureConfig {
    pub features: std::collections::HashMap<String, FeatureDefinition>,
    pub profiles: std::collections::HashMap<String, Vec<String>>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FeatureDefinition {
    pub enabled: bool,
    pub requires_premium: bool,
    pub requires_game_api: bool,
    pub fallback: Option<String>,
    pub config: Option<serde_json::Value>,
}

impl Default for FeatureDefinition {
    fn default() -> Self {
        Self {
            enabled: true,
            requires_premium: false,
            requires_game_api: false,
            fallback: None,
            config: None,
        }
    }
}

pub struct FeatureManager {
    features: DashMap<String, FeatureState>,
    premium_enabled: std::sync::atomic::AtomicBool,
    game_api_available: std::sync::atomic::AtomicBool,
    listeners: tokio::sync::RwLock<Vec<Arc<dyn FeatureListener>>>,
}

#[derive(Debug, Clone)]
struct FeatureState {
    definition: FeatureDefinition,
    active: bool,
    using_fallback: bool,
}

#[async_trait::async_trait]
pub trait FeatureListener: Send + Sync {
    async fn on_feature_changed(&self, feature: &str, enabled: bool);
}

impl FeatureManager {
    pub fn new() -> Self {
        let manager = Self {
            features: DashMap::new(),
            premium_enabled: std::sync::atomic::AtomicBool::new(false),
            game_api_available: std::sync::atomic::AtomicBool::new(false),
            listeners: tokio::sync::RwLock::new(Vec::new()),
        };
        
        manager.register_default_features();
        manager
    }
    
    fn register_default_features(&self) {
        let defaults = vec![
            ("performance.cpu_allocation", FeatureDefinition { requires_premium: true, requires_game_api: false, ..Default::default() }),
            ("performance.ram_reservation", FeatureDefinition { requires_premium: true, requires_game_api: false, ..Default::default() }),
            ("performance.asset_streaming", FeatureDefinition { requires_premium: false, requires_game_api: true, ..Default::default() }),
            ("performance.texture_compression", FeatureDefinition { requires_premium: false, requires_game_api: false, ..Default::default() }),
            ("performance.shader_precompile", FeatureDefinition { requires_premium: true, requires_game_api: true, ..Default::default() }),
            ("diagnostics.network", FeatureDefinition { requires_premium: false, requires_game_api: false, ..Default::default() }),
            ("diagnostics.gpu_profiling", FeatureDefinition { requires_premium: true, requires_game_api: true, ..Default::default() }),
            ("diagnostics.memory_tracking", FeatureDefinition { requires_premium: true, requires_game_api: false, ..Default::default() }),
            ("mods.conflict_detection", FeatureDefinition { requires_premium: true, requires_game_api: true, ..Default::default() }),
            ("mods.auto_resolution", FeatureDefinition { requires_premium: true, requires_game_api: true, ..Default::default() }),
            ("mods.safe_mode", FeatureDefinition { requires_premium: false, requires_game_api: true, ..Default::default() }),
            ("connectivity.nat_traversal", FeatureDefinition { requires_premium: false, requires_game_api: false, ..Default::default() }),
            ("connectivity.connection_pooling", FeatureDefinition { requires_premium: false, requires_game_api: false, ..Default::default() }),
            ("connectivity.route_selection", FeatureDefinition { requires_premium: true, requires_game_api: false, ..Default::default() }),
            ("connectivity.session_handoff", FeatureDefinition { requires_premium: true, requires_game_api: true, ..Default::default() }),
            ("social.friend_activity", FeatureDefinition { requires_premium: false, requires_game_api: false, ..Default::default() }),
            ("social.player_presence", FeatureDefinition { requires_premium: false, requires_game_api: false, ..Default::default() }),
            ("social.queue_priority", FeatureDefinition { requires_premium: true, requires_game_api: true, ..Default::default() }),
            ("visual.upscaling", FeatureDefinition { requires_premium: true, requires_game_api: true, ..Default::default() }),
            ("visual.fsr", FeatureDefinition { requires_premium: true, requires_game_api: true, ..Default::default() }),
            ("launcher.auto_update", FeatureDefinition { requires_premium: false, requires_game_api: false, ..Default::default() }),
            ("launcher.profiles", FeatureDefinition { requires_premium: false, requires_game_api: false, ..Default::default() }),
            ("launcher.cloud_sync", FeatureDefinition { requires_premium: true, requires_game_api: false, ..Default::default() }),
        ];
        
        for (name, def) in defaults {
            let active = def.enabled && !def.requires_premium && !def.requires_game_api;
            self.features.insert(name.to_string(), FeatureState {
                definition: def,
                active,
                using_fallback: false,
            });
        }
    }
    
    pub fn set_premium(&self, enabled: bool) {
        self.premium_enabled.store(enabled, std::sync::atomic::Ordering::Relaxed);
        self.recalculate_all();
    }
    
    pub fn set_game_api_available(&self, available: bool) {
        self.game_api_available.store(available, std::sync::atomic::Ordering::Relaxed);
        self.recalculate_all();
    }
    
    fn recalculate_all(&self) {
        let premium = self.premium_enabled.load(std::sync::atomic::Ordering::Relaxed);
        let game_api = self.game_api_available.load(std::sync::atomic::Ordering::Relaxed);
        
        for mut entry in self.features.iter_mut() {
            let can_activate = entry.definition.enabled 
                && (!entry.definition.requires_premium || premium)
                && (!entry.definition.requires_game_api || game_api);
            
            let has_fallback = entry.definition.fallback.is_some();
            entry.active = can_activate;
            entry.using_fallback = !can_activate && has_fallback;
        }
    }
    
    pub fn is_enabled(&self, feature: &str) -> bool {
        self.features.get(feature).map(|f| f.active).unwrap_or(false)
    }
    
    pub fn is_available(&self, feature: &str) -> bool {
        self.features.contains_key(feature)
    }
    
    pub fn get_fallback(&self, feature: &str) -> Option<String> {
        self.features.get(feature)
            .filter(|f| f.using_fallback)
            .and_then(|f| f.definition.fallback.clone())
    }
    
    pub fn get_config<T: serde::de::DeserializeOwned>(&self, feature: &str) -> Option<T> {
        self.features.get(feature)
            .and_then(|f| f.definition.config.clone())
            .and_then(|v| serde_json::from_value(v).ok())
    }
    
    pub fn register(&self, name: &str, definition: FeatureDefinition) {
        let premium = self.premium_enabled.load(std::sync::atomic::Ordering::Relaxed);
        let game_api = self.game_api_available.load(std::sync::atomic::Ordering::Relaxed);
        
        let can_activate = definition.enabled 
            && (!definition.requires_premium || premium)
            && (!definition.requires_game_api || game_api);
        
        self.features.insert(name.to_string(), FeatureState {
            definition,
            active: can_activate,
            using_fallback: false,
        });
    }
    
    pub fn list_features(&self) -> Vec<FeatureStatus> {
        self.features.iter().map(|entry| {
            FeatureStatus {
                name: entry.key().clone(),
                enabled: entry.active,
                requires_premium: entry.definition.requires_premium,
                requires_game_api: entry.definition.requires_game_api,
                using_fallback: entry.using_fallback,
            }
        }).collect()
    }
    
    pub async fn add_listener(&self, listener: Arc<dyn FeatureListener>) {
        let mut listeners = self.listeners.write().await;
        listeners.push(listener);
    }
}

impl Default for FeatureManager {
    fn default() -> Self {
        Self::new()
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FeatureStatus {
    pub name: String,
    pub enabled: bool,
    pub requires_premium: bool,
    pub requires_game_api: bool,
    pub using_fallback: bool,
}

pub fn feature_guard<'a>(features: &'a FeatureManager, feature: &'a str) -> impl Fn() -> bool + 'a {
    move || features.is_enabled(feature)
}

#[macro_export]
macro_rules! when_feature {
    ($features:expr, $feature:expr, $body:block) => {
        if $features.is_enabled($feature) {
            $body
        }
    };
    ($features:expr, $feature:expr, $body:block else $fallback:block) => {
        if $features.is_enabled($feature) {
            $body
        } else {
            $fallback
        }
    };
}
