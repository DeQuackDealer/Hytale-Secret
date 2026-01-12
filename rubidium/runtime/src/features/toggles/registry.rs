use super::config::{ToggleConfig, FeatureSettings};
use chrono::{DateTime, Utc};
use dashmap::DashMap;
use parking_lot::RwLock;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::sync::Arc;
use uuid::Uuid;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum FeatureStatus {
    Enabled,
    Disabled,
    PremiumOnly,
    RequiresApi,
    Conflicted,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FeatureToggle {
    pub id: String,
    pub name: String,
    pub description: String,
    pub status: FeatureStatus,
    pub enabled_at: Option<DateTime<Utc>>,
    pub disabled_at: Option<DateTime<Utc>>,
    pub changed_by: Option<Uuid>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FeatureAuditEntry {
    pub feature_id: String,
    pub action: String,
    pub actor: Uuid,
    pub timestamp: DateTime<Utc>,
    pub old_status: FeatureStatus,
    pub new_status: FeatureStatus,
    pub reason: Option<String>,
}

pub struct FeatureToggleRegistry {
    config: Arc<RwLock<ToggleConfig>>,
    toggles: DashMap<String, FeatureToggle>,
    player_overrides: DashMap<Uuid, HashMap<String, bool>>,
    role_cache: DashMap<Uuid, Vec<String>>,
    audit_log: RwLock<Vec<FeatureAuditEntry>>,
}

impl FeatureToggleRegistry {
    pub fn new(config: ToggleConfig) -> Self {
        let registry = Self {
            config: Arc::new(RwLock::new(config.clone())),
            toggles: DashMap::new(),
            player_overrides: DashMap::new(),
            role_cache: DashMap::new(),
            audit_log: RwLock::new(Vec::new()),
        };

        for (id, settings) in &config.features {
            registry.register_feature(id.clone(), settings.clone());
        }

        registry
    }

    pub fn register_feature(&self, id: String, settings: FeatureSettings) {
        let status = if !settings.enabled {
            FeatureStatus::Disabled
        } else if settings.premium_only {
            FeatureStatus::PremiumOnly
        } else if settings.requires_api {
            FeatureStatus::RequiresApi
        } else {
            FeatureStatus::Enabled
        };

        let toggle = FeatureToggle {
            id: id.clone(),
            name: settings.name,
            description: settings.description,
            status,
            enabled_at: if settings.enabled { Some(Utc::now()) } else { None },
            disabled_at: if !settings.enabled { Some(Utc::now()) } else { None },
            changed_by: None,
        };

        self.toggles.insert(id, toggle);
    }

    pub fn is_enabled(&self, feature_id: &str) -> bool {
        if let Some(toggle) = self.toggles.get(feature_id) {
            if toggle.status != FeatureStatus::Enabled {
                return false;
            }
        } else {
            return false;
        }
        
        if let Some(parent_id) = self.get_parent_id(feature_id) {
            if !self.is_enabled(&parent_id) {
                return false;
            }
        }
        
        true
    }
    
    fn get_parent_id(&self, feature_id: &str) -> Option<String> {
        if let Some(last_dot) = feature_id.rfind('.') {
            Some(feature_id[..last_dot].to_string())
        } else {
            None
        }
    }

    pub fn is_enabled_for(&self, feature_id: &str, player_id: Uuid) -> bool {
        if let Some(overrides) = self.player_overrides.get(&player_id) {
            if let Some(&override_value) = overrides.get(feature_id) {
                return override_value;
            }
        }

        if let Some(roles) = self.role_cache.get(&player_id) {
            let config = self.config.read();
            for role in roles.iter() {
                if let Some(allowed) = config.role_defaults.get(role) {
                    if allowed.contains(&"*".to_string()) {
                        return true;
                    }
                    if allowed.contains(&feature_id.to_string()) {
                        return true;
                    }
                }
            }
        }

        self.is_enabled(feature_id)
    }

    pub fn set_enabled(&self, feature_id: &str, enabled: bool, actor: Uuid, reason: Option<String>) -> Result<(), String> {
        let mut toggle = self.toggles.get_mut(feature_id)
            .ok_or("Feature not found")?;
        
        let old_status = toggle.status;
        let new_status = if enabled { FeatureStatus::Enabled } else { FeatureStatus::Disabled };
        
        if old_status == new_status {
            return Ok(());
        }

        toggle.status = new_status;
        toggle.changed_by = Some(actor);
        
        if enabled {
            toggle.enabled_at = Some(Utc::now());
        } else {
            toggle.disabled_at = Some(Utc::now());
        }

        let config = self.config.read();
        if config.audit_enabled {
            drop(config);
            self.audit_log.write().push(FeatureAuditEntry {
                feature_id: feature_id.to_string(),
                action: if enabled { "enable".to_string() } else { "disable".to_string() },
                actor,
                timestamp: Utc::now(),
                old_status,
                new_status,
                reason: reason.clone(),
            });
        }

        drop(toggle);
        
        if !enabled {
            self.cascade_disable(feature_id, actor, reason);
        }

        Ok(())
    }
    
    fn cascade_disable(&self, parent_id: &str, actor: Uuid, reason: Option<String>) {
        let prefix = format!("{}.", parent_id);
        let children: Vec<_> = self.toggles.iter()
            .filter(|t| t.id.starts_with(&prefix))
            .map(|t| t.id.clone())
            .collect();
        
        for child_id in children {
            let _ = self.set_child_disabled(&child_id, actor, reason.clone());
        }
    }
    
    fn set_child_disabled(&self, feature_id: &str, actor: Uuid, reason: Option<String>) -> Result<(), String> {
        if let Some(mut toggle) = self.toggles.get_mut(feature_id) {
            if toggle.status == FeatureStatus::Enabled {
                let old_status = toggle.status;
                toggle.status = FeatureStatus::Disabled;
                toggle.disabled_at = Some(Utc::now());
                toggle.changed_by = Some(actor);
                
                let config = self.config.read();
                if config.audit_enabled {
                    drop(config);
                    self.audit_log.write().push(FeatureAuditEntry {
                        feature_id: feature_id.to_string(),
                        action: "disable_cascade".to_string(),
                        actor,
                        timestamp: Utc::now(),
                        old_status,
                        new_status: FeatureStatus::Disabled,
                        reason,
                    });
                }
            }
        }
        Ok(())
    }

    pub fn toggle(&self, feature_id: &str, actor: Uuid) -> Result<bool, String> {
        let current = self.is_enabled(feature_id);
        self.set_enabled(feature_id, !current, actor, None)?;
        Ok(!current)
    }

    pub fn set_player_override(&self, player_id: Uuid, feature_id: &str, enabled: bool) {
        self.player_overrides.entry(player_id)
            .or_insert_with(HashMap::new)
            .insert(feature_id.to_string(), enabled);
    }

    pub fn remove_player_override(&self, player_id: Uuid, feature_id: &str) {
        if let Some(mut overrides) = self.player_overrides.get_mut(&player_id) {
            overrides.remove(feature_id);
        }
    }

    pub fn set_player_roles(&self, player_id: Uuid, roles: Vec<String>) {
        self.role_cache.insert(player_id, roles);
    }

    pub fn get_feature(&self, feature_id: &str) -> Option<FeatureToggle> {
        self.toggles.get(feature_id).map(|t| t.clone())
    }

    pub fn list_features(&self) -> Vec<FeatureToggle> {
        self.toggles.iter().map(|t| t.clone()).collect()
    }

    pub fn list_enabled(&self) -> Vec<String> {
        self.toggles.iter()
            .filter(|t| t.status == FeatureStatus::Enabled)
            .map(|t| t.id.clone())
            .collect()
    }

    pub fn list_disabled(&self) -> Vec<String> {
        self.toggles.iter()
            .filter(|t| t.status == FeatureStatus::Disabled)
            .map(|t| t.id.clone())
            .collect()
    }

    pub fn get_audit_log(&self, limit: usize) -> Vec<FeatureAuditEntry> {
        let log = self.audit_log.read();
        log.iter().rev().take(limit).cloned().collect()
    }

    pub fn get_feature_audit(&self, feature_id: &str, limit: usize) -> Vec<FeatureAuditEntry> {
        let log = self.audit_log.read();
        log.iter()
            .filter(|e| e.feature_id == feature_id)
            .rev()
            .take(limit)
            .cloned()
            .collect()
    }

    pub fn bulk_set(&self, feature_ids: &[&str], enabled: bool, actor: Uuid) -> Vec<Result<(), String>> {
        feature_ids.iter()
            .map(|id| self.set_enabled(id, enabled, actor, None))
            .collect()
    }

    pub fn get_status_summary(&self) -> FeatureSummary {
        let total = self.toggles.len();
        let enabled = self.toggles.iter().filter(|t| t.status == FeatureStatus::Enabled).count();
        let disabled = self.toggles.iter().filter(|t| t.status == FeatureStatus::Disabled).count();
        let premium = self.toggles.iter().filter(|t| t.status == FeatureStatus::PremiumOnly).count();

        FeatureSummary { total, enabled, disabled, premium }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FeatureSummary {
    pub total: usize,
    pub enabled: usize,
    pub disabled: usize,
    pub premium: usize,
}

#[macro_export]
macro_rules! when_feature {
    ($registry:expr, $feature:expr, $block:block) => {
        if $registry.is_enabled($feature) {
            $block
        }
    };
    ($registry:expr, $feature:expr, $player:expr, $block:block) => {
        if $registry.is_enabled_for($feature, $player) {
            $block
        }
    };
}
