use crate::core::config::ConfigManager;
use async_trait::async_trait;
use dashmap::DashMap;
use serde::{Deserialize, Serialize};
use std::sync::Arc;
use tracing::{info, warn, error};
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PluginMetadata {
    pub id: String,
    pub name: String,
    pub version: String,
    pub author: String,
    pub description: String,
    pub dependencies: Vec<PluginDependency>,
    pub api_version: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PluginDependency {
    pub id: String,
    pub version: String,
    pub optional: bool,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum PluginState {
    Discovered,
    Loading,
    Enabled,
    Disabled,
    Failed,
    Unloading,
}

#[async_trait]
pub trait Plugin: Send + Sync {
    fn metadata(&self) -> &PluginMetadata;
    async fn on_enable(&mut self) -> Result<(), String>;
    async fn on_disable(&mut self) -> Result<(), String>;
    async fn on_tick(&mut self);
    async fn on_reload(&mut self) -> Result<(), String>;
}

pub struct PluginInstance {
    pub metadata: PluginMetadata,
    pub state: PluginState,
    pub load_order: i32,
    pub error: Option<String>,
}

pub struct PluginManager {
    plugins: DashMap<String, PluginInstance>,
    config: Arc<ConfigManager>,
    plugins_dir: String,
}

impl PluginManager {
    pub fn new(config: Arc<ConfigManager>) -> Self {
        let plugins_dir = config.get_string("plugins.directory").unwrap_or_else(|| "plugins".to_string());
        
        Self {
            plugins: DashMap::new(),
            config,
            plugins_dir,
        }
    }
    
    pub async fn load_all(&self) -> Result<(), String> {
        info!("Discovering plugins in: {}", self.plugins_dir);
        
        let plugin_dirs = match std::fs::read_dir(&self.plugins_dir) {
            Ok(entries) => entries,
            Err(e) => {
                if e.kind() == std::io::ErrorKind::NotFound {
                    info!("No plugins directory found, creating...");
                    std::fs::create_dir_all(&self.plugins_dir).map_err(|e| e.to_string())?;
                    return Ok(());
                }
                return Err(e.to_string());
            }
        };
        
        let mut discovered = Vec::new();
        
        for entry in plugin_dirs.flatten() {
            let path = entry.path();
            if path.is_dir() {
                let config_path = path.join("plugin.toml");
                if config_path.exists() {
                    match self.load_plugin_metadata(&config_path) {
                        Ok(metadata) => {
                            info!("Discovered plugin: {} v{}", metadata.name, metadata.version);
                            discovered.push(metadata);
                        }
                        Err(e) => {
                            warn!("Failed to load plugin config {:?}: {}", config_path, e);
                        }
                    }
                }
            }
        }
        
        let ordered = self.resolve_load_order(discovered)?;
        
        for (order, metadata) in ordered.into_iter().enumerate() {
            let instance = PluginInstance {
                metadata: metadata.clone(),
                state: PluginState::Discovered,
                load_order: order as i32,
                error: None,
            };
            self.plugins.insert(metadata.id.clone(), instance);
        }
        
        for entry in self.plugins.iter() {
            let id = entry.key().clone();
            drop(entry);
            if let Err(e) = self.enable_plugin(&id).await {
                error!("Failed to enable plugin {}: {}", id, e);
            }
        }
        
        info!("Loaded {} plugins", self.plugins.len());
        Ok(())
    }
    
    fn load_plugin_metadata(&self, path: &std::path::Path) -> Result<PluginMetadata, String> {
        let content = std::fs::read_to_string(path).map_err(|e| e.to_string())?;
        toml::from_str(&content).map_err(|e| e.to_string())
    }
    
    fn resolve_load_order(&self, plugins: Vec<PluginMetadata>) -> Result<Vec<PluginMetadata>, String> {
        let mut ordered = plugins.clone();
        ordered.sort_by(|a, b| {
            let a_deps_b = a.dependencies.iter().any(|d| d.id == b.id);
            let b_deps_a = b.dependencies.iter().any(|d| d.id == a.id);
            
            if a_deps_b && b_deps_a {
                std::cmp::Ordering::Equal
            } else if a_deps_b {
                std::cmp::Ordering::Greater
            } else if b_deps_a {
                std::cmp::Ordering::Less
            } else {
                a.id.cmp(&b.id)
            }
        });
        Ok(ordered)
    }
    
    pub async fn enable_plugin(&self, id: &str) -> Result<(), String> {
        let mut instance = self.plugins.get_mut(id).ok_or("Plugin not found")?;
        
        if instance.state == PluginState::Enabled {
            return Ok(());
        }
        
        for dep in &instance.metadata.dependencies {
            if !dep.optional {
                if !self.plugins.contains_key(&dep.id) {
                    return Err(format!("Missing required dependency: {}", dep.id));
                }
                let dep_instance = self.plugins.get(&dep.id).unwrap();
                if dep_instance.state != PluginState::Enabled {
                    return Err(format!("Dependency {} is not enabled", dep.id));
                }
            }
        }
        
        instance.state = PluginState::Loading;
        info!("Enabling plugin: {}", instance.metadata.name);
        
        instance.state = PluginState::Enabled;
        info!("Plugin {} enabled successfully", instance.metadata.name);
        
        Ok(())
    }
    
    pub async fn disable_plugin(&self, id: &str) -> Result<(), String> {
        let mut instance = self.plugins.get_mut(id).ok_or("Plugin not found")?;
        
        if instance.state != PluginState::Enabled {
            return Ok(());
        }
        
        for other in self.plugins.iter() {
            if other.state == PluginState::Enabled {
                for dep in &other.metadata.dependencies {
                    if dep.id == id && !dep.optional {
                        return Err(format!("Plugin {} depends on this plugin", other.metadata.name));
                    }
                }
            }
        }
        
        instance.state = PluginState::Unloading;
        info!("Disabling plugin: {}", instance.metadata.name);
        
        instance.state = PluginState::Disabled;
        info!("Plugin {} disabled", instance.metadata.name);
        
        Ok(())
    }
    
    pub async fn unload_all(&self) {
        let mut ids: Vec<String> = self.plugins.iter()
            .map(|e| (e.load_order, e.key().clone()))
            .collect::<Vec<_>>()
            .into_iter()
            .rev()
            .map(|(_, id)| id)
            .collect();
        
        ids.sort_by(|a, b| {
            let a_order = self.plugins.get(a).map(|p| p.load_order).unwrap_or(0);
            let b_order = self.plugins.get(b).map(|p| p.load_order).unwrap_or(0);
            b_order.cmp(&a_order)
        });
        
        for id in ids {
            let _ = self.disable_plugin(&id).await;
        }
        
        self.plugins.clear();
    }
    
    pub async fn reload_configs(&self) {
        for entry in self.plugins.iter() {
            if entry.state == PluginState::Enabled {
                info!("Reloading config for plugin: {}", entry.metadata.name);
            }
        }
    }
    
    pub fn list_plugins(&self) -> Vec<PluginMetadata> {
        self.plugins.iter().map(|e| e.metadata.clone()).collect()
    }
    
    pub fn get_plugin_state(&self, id: &str) -> Option<PluginState> {
        self.plugins.get(id).map(|p| p.state)
    }
}
