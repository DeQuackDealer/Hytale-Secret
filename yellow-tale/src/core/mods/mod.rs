//! Mod Orchestration Module
//! 
//! Provides generic mod orchestration WITHOUT being a mod loader.
//! Mods are treated as opaque packages - this module handles:
//! - Install/remove/enable/disable
//! - Version pinning
//! - Dependency graph resolution
//! - Per-profile mod sets
//! 
//! This is compatible with official mod systems without replacing them.

use std::collections::{HashMap, HashSet};
use std::path::PathBuf;
use serde::{Deserialize, Serialize};
use thiserror::Error;
use semver::{Version, VersionReq};
use chrono::{DateTime, Utc};
use tracing::{info, warn};

#[derive(Error, Debug)]
pub enum ModError {
    #[error("Mod not found: {0}")]
    NotFound(String),
    
    #[error("Mod already installed: {0}")]
    AlreadyInstalled(String),
    
    #[error("Version conflict: {0}")]
    VersionConflict(String),
    
    #[error("Dependency not satisfied: {0} requires {1}")]
    DependencyNotSatisfied(String, String),
    
    #[error("Circular dependency detected: {0}")]
    CircularDependency(String),
    
    #[error("IO error: {0}")]
    IoError(#[from] std::io::Error),
}

/// Metadata about a mod package
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModMetadata {
    /// Unique identifier for the mod
    pub id: String,
    
    /// Human-readable name
    pub name: String,
    
    /// Version of the mod
    pub version: Version,
    
    /// Brief description
    pub description: Option<String>,
    
    /// Author(s)
    pub authors: Vec<String>,
    
    /// Dependencies on other mods (id -> version requirement)
    pub dependencies: HashMap<String, VersionReq>,
    
    /// Mods this is incompatible with
    pub conflicts: Vec<String>,
    
    /// When this mod was installed
    pub installed_at: DateTime<Utc>,
    
    /// Path to the mod package
    pub package_path: PathBuf,
}

/// State of a mod in a profile
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModState {
    /// The mod's metadata
    pub metadata: ModMetadata,
    
    /// Whether the mod is enabled
    pub enabled: bool,
    
    /// Pinned version (if any)
    pub pinned_version: Option<Version>,
}

/// Result of dependency resolution
#[derive(Debug, Clone)]
pub struct ResolutionResult {
    /// Mods in load order (dependencies first)
    pub load_order: Vec<String>,
    
    /// Any warnings generated during resolution
    pub warnings: Vec<String>,
}

/// Orchestrates mod installation, removal, and dependency management
pub struct ModOrchestrator {
    /// Directory where mods are stored
    mods_dir: PathBuf,
    
    /// All installed mods
    installed_mods: HashMap<String, ModState>,
}

impl ModOrchestrator {
    /// Create a new mod orchestrator
    pub fn new(mods_dir: PathBuf) -> Self {
        Self {
            mods_dir,
            installed_mods: HashMap::new(),
        }
    }
    
    /// Load mod index from disk
    pub async fn load_index(&mut self) -> Result<(), ModError> {
        if !self.mods_dir.exists() {
            tokio::fs::create_dir_all(&self.mods_dir).await?;
            info!("Created mods directory: {:?}", self.mods_dir);
        }
        
        let index_path = self.mods_dir.join("index.toml");
        if index_path.exists() {
            let content = tokio::fs::read_to_string(&index_path).await?;
            self.installed_mods = toml::from_str(&content)
                .map_err(|e| ModError::IoError(std::io::Error::new(
                    std::io::ErrorKind::InvalidData,
                    e.to_string()
                )))?;
            info!("Loaded {} mods from index", self.installed_mods.len());
        }
        
        Ok(())
    }
    
    /// Save mod index to disk
    async fn save_index(&self) -> Result<(), ModError> {
        let index_path = self.mods_dir.join("index.toml");
        let content = toml::to_string_pretty(&self.installed_mods)
            .map_err(|e| ModError::IoError(std::io::Error::new(
                std::io::ErrorKind::InvalidData,
                e.to_string()
            )))?;
        tokio::fs::write(&index_path, content).await?;
        Ok(())
    }
    
    /// Install a mod from a package path
    pub async fn install(&mut self, package_path: PathBuf, metadata: ModMetadata) -> Result<(), ModError> {
        if self.installed_mods.contains_key(&metadata.id) {
            return Err(ModError::AlreadyInstalled(metadata.id.clone()));
        }
        
        info!("Installing mod: {} v{}", metadata.name, metadata.version);
        
        // Copy package to mods directory
        let dest_path = self.mods_dir.join(&metadata.id);
        if package_path.is_dir() {
            // For directories, we'd do a recursive copy
            // Simplified for now
            tokio::fs::create_dir_all(&dest_path).await?;
        } else {
            tokio::fs::copy(&package_path, &dest_path).await?;
        }
        
        let state = ModState {
            metadata: ModMetadata {
                package_path: dest_path,
                ..metadata
            },
            enabled: true,
            pinned_version: None,
        };
        
        self.installed_mods.insert(state.metadata.id.clone(), state);
        self.save_index().await?;
        
        Ok(())
    }
    
    /// Remove an installed mod
    pub async fn remove(&mut self, mod_id: &str) -> Result<(), ModError> {
        let state = self.installed_mods.remove(mod_id)
            .ok_or_else(|| ModError::NotFound(mod_id.to_string()))?;
        
        info!("Removing mod: {}", state.metadata.name);
        
        // Check if other mods depend on this
        for (id, other) in &self.installed_mods {
            if other.metadata.dependencies.contains_key(mod_id) {
                warn!("Mod {} depends on {}", id, mod_id);
            }
        }
        
        // Remove from filesystem
        if state.metadata.package_path.exists() {
            if state.metadata.package_path.is_dir() {
                tokio::fs::remove_dir_all(&state.metadata.package_path).await?;
            } else {
                tokio::fs::remove_file(&state.metadata.package_path).await?;
            }
        }
        
        self.save_index().await?;
        Ok(())
    }
    
    /// Enable a mod
    pub async fn enable(&mut self, mod_id: &str) -> Result<(), ModError> {
        let state = self.installed_mods.get_mut(mod_id)
            .ok_or_else(|| ModError::NotFound(mod_id.to_string()))?;
        
        state.enabled = true;
        info!("Enabled mod: {}", state.metadata.name);
        
        self.save_index().await?;
        Ok(())
    }
    
    /// Disable a mod
    pub async fn disable(&mut self, mod_id: &str) -> Result<(), ModError> {
        let state = self.installed_mods.get_mut(mod_id)
            .ok_or_else(|| ModError::NotFound(mod_id.to_string()))?;
        
        state.enabled = false;
        info!("Disabled mod: {}", state.metadata.name);
        
        self.save_index().await?;
        Ok(())
    }
    
    /// Pin a mod to a specific version
    pub async fn pin_version(&mut self, mod_id: &str, version: Version) -> Result<(), ModError> {
        let state = self.installed_mods.get_mut(mod_id)
            .ok_or_else(|| ModError::NotFound(mod_id.to_string()))?;
        
        state.pinned_version = Some(version.clone());
        info!("Pinned {} to version {}", state.metadata.name, version);
        
        self.save_index().await?;
        Ok(())
    }
    
    /// Resolve dependencies and generate load order
    pub fn resolve_dependencies(&self, enabled_mod_ids: &[String]) -> Result<ResolutionResult, ModError> {
        let mut load_order = Vec::new();
        let mut resolved = HashSet::new();
        let mut warnings = Vec::new();
        
        // Topological sort with cycle detection
        fn visit(
            mod_id: &str,
            mods: &HashMap<String, ModState>,
            resolved: &mut HashSet<String>,
            visiting: &mut HashSet<String>,
            load_order: &mut Vec<String>,
            warnings: &mut Vec<String>,
        ) -> Result<(), ModError> {
            if resolved.contains(mod_id) {
                return Ok(());
            }
            
            if visiting.contains(mod_id) {
                return Err(ModError::CircularDependency(mod_id.to_string()));
            }
            
            visiting.insert(mod_id.to_string());
            
            if let Some(state) = mods.get(mod_id) {
                // Visit dependencies first
                for (dep_id, version_req) in &state.metadata.dependencies {
                    if let Some(dep_state) = mods.get(dep_id) {
                        // Check version requirement
                        if !version_req.matches(&dep_state.metadata.version) {
                            return Err(ModError::DependencyNotSatisfied(
                                mod_id.to_string(),
                                format!("{} {}", dep_id, version_req),
                            ));
                        }
                        visit(dep_id, mods, resolved, visiting, load_order, warnings)?;
                    } else {
                        return Err(ModError::DependencyNotSatisfied(
                            mod_id.to_string(),
                            dep_id.clone(),
                        ));
                    }
                }
                
                // Check for conflicts
                for conflict_id in &state.metadata.conflicts {
                    if resolved.contains(conflict_id) {
                        warnings.push(format!(
                            "Mod {} conflicts with {} but both are enabled",
                            mod_id, conflict_id
                        ));
                    }
                }
            }
            
            visiting.remove(mod_id);
            resolved.insert(mod_id.to_string());
            load_order.push(mod_id.to_string());
            
            Ok(())
        }
        
        let mut visiting = HashSet::new();
        
        for mod_id in enabled_mod_ids {
            if let Some(state) = self.installed_mods.get(mod_id) {
                if state.enabled {
                    visit(
                        mod_id,
                        &self.installed_mods,
                        &mut resolved,
                        &mut visiting,
                        &mut load_order,
                        &mut warnings,
                    )?;
                }
            }
        }
        
        Ok(ResolutionResult { load_order, warnings })
    }
    
    /// List all installed mods
    pub fn list(&self) -> Vec<&ModState> {
        self.installed_mods.values().collect()
    }
    
    /// Get a specific mod by ID
    pub fn get(&self, mod_id: &str) -> Option<&ModState> {
        self.installed_mods.get(mod_id)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::path::PathBuf;
    
    #[test]
    fn test_mod_orchestrator_new() {
        let orchestrator = ModOrchestrator::new(PathBuf::from("/tmp/mods"));
        assert!(orchestrator.list().is_empty());
    }
}
