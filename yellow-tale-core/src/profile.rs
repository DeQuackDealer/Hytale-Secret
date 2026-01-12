use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::path::{Path, PathBuf};
use thiserror::Error;
use uuid::Uuid;
use chrono::{DateTime, Utc};

#[derive(Debug, Error)]
pub enum ProfileError {
    #[error("Profile not found: {0}")]
    NotFound(String),
    #[error("Profile already exists: {0}")]
    AlreadyExists(String),
    #[error("IO error: {0}")]
    IoError(#[from] std::io::Error),
    #[error("Serialization error: {0}")]
    SerializeError(String),
    #[error("Invalid profile: {0}")]
    InvalidProfile(String),
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Profile {
    pub id: Uuid,
    pub name: String,
    pub created_at: DateTime<Utc>,
    pub updated_at: DateTime<Utc>,
    pub game_version: Option<String>,
    pub performance: ProfilePerformance,
    pub mods: Vec<ModEntry>,
    pub metadata: HashMap<String, String>,
}

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct ProfilePerformance {
    pub ram_allocation_mb: Option<u32>,
    pub jvm_args_override: Vec<String>,
    pub gc_preset_override: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModEntry {
    pub id: String,
    pub name: String,
    pub version: String,
    pub enabled: bool,
    pub source: ModSource,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum ModSource {
    Local(PathBuf),
    Remote { url: String, hash: String },
    Registry { registry: String, package: String },
}

impl Profile {
    pub fn new(name: impl Into<String>) -> Self {
        let now = Utc::now();
        Self {
            id: Uuid::new_v4(),
            name: name.into(),
            created_at: now,
            updated_at: now,
            game_version: None,
            performance: ProfilePerformance::default(),
            mods: Vec::new(),
            metadata: HashMap::new(),
        }
    }
    
    pub fn with_version(mut self, version: impl Into<String>) -> Self {
        self.game_version = Some(version.into());
        self
    }
    
    pub fn add_mod(&mut self, entry: ModEntry) {
        self.mods.push(entry);
        self.updated_at = Utc::now();
    }
    
    pub fn remove_mod(&mut self, mod_id: &str) -> bool {
        let initial_len = self.mods.len();
        self.mods.retain(|m| m.id != mod_id);
        let removed = self.mods.len() < initial_len;
        if removed {
            self.updated_at = Utc::now();
        }
        removed
    }
    
    pub fn toggle_mod(&mut self, mod_id: &str) -> Option<bool> {
        if let Some(m) = self.mods.iter_mut().find(|m| m.id == mod_id) {
            m.enabled = !m.enabled;
            self.updated_at = Utc::now();
            Some(m.enabled)
        } else {
            None
        }
    }
}

pub struct ProfileManager {
    profiles_dir: PathBuf,
    profiles: HashMap<Uuid, Profile>,
    active_profile: Option<Uuid>,
}

impl ProfileManager {
    pub fn new(profiles_dir: PathBuf) -> Self {
        Self {
            profiles_dir,
            profiles: HashMap::new(),
            active_profile: None,
        }
    }
    
    pub fn load_all(&mut self) -> Result<(), ProfileError> {
        if !self.profiles_dir.exists() {
            std::fs::create_dir_all(&self.profiles_dir)?;
            return Ok(());
        }
        
        for entry in std::fs::read_dir(&self.profiles_dir)? {
            let entry = entry?;
            let path = entry.path();
            
            if path.extension().map(|e| e == "json").unwrap_or(false) {
                if let Ok(content) = std::fs::read_to_string(&path) {
                    if let Ok(profile) = serde_json::from_str::<Profile>(&content) {
                        self.profiles.insert(profile.id, profile);
                    }
                }
            }
        }
        
        Ok(())
    }
    
    pub fn create(&mut self, name: impl Into<String>) -> Result<&Profile, ProfileError> {
        let profile = Profile::new(name);
        let id = profile.id;
        
        self.save_profile(&profile)?;
        self.profiles.insert(id, profile);
        
        Ok(self.profiles.get(&id).unwrap())
    }
    
    pub fn get(&self, id: Uuid) -> Option<&Profile> {
        self.profiles.get(&id)
    }
    
    pub fn get_mut(&mut self, id: Uuid) -> Option<&mut Profile> {
        self.profiles.get_mut(&id)
    }
    
    pub fn get_by_name(&self, name: &str) -> Option<&Profile> {
        self.profiles.values().find(|p| p.name == name)
    }
    
    pub fn list(&self) -> Vec<&Profile> {
        self.profiles.values().collect()
    }
    
    pub fn delete(&mut self, id: Uuid) -> Result<(), ProfileError> {
        if let Some(profile) = self.profiles.remove(&id) {
            let path = self.profile_path(&profile);
            if path.exists() {
                std::fs::remove_file(path)?;
            }
            
            if self.active_profile == Some(id) {
                self.active_profile = None;
            }
            
            Ok(())
        } else {
            Err(ProfileError::NotFound(id.to_string()))
        }
    }
    
    pub fn clone_profile(&mut self, source_id: Uuid, new_name: impl Into<String>) -> Result<&Profile, ProfileError> {
        let source = self.profiles.get(&source_id)
            .ok_or_else(|| ProfileError::NotFound(source_id.to_string()))?
            .clone();
        
        let now = Utc::now();
        let cloned = Profile {
            id: Uuid::new_v4(),
            name: new_name.into(),
            created_at: now,
            updated_at: now,
            ..source
        };
        
        let id = cloned.id;
        self.save_profile(&cloned)?;
        self.profiles.insert(id, cloned);
        
        Ok(self.profiles.get(&id).unwrap())
    }
    
    pub fn set_active(&mut self, id: Uuid) -> Result<(), ProfileError> {
        if !self.profiles.contains_key(&id) {
            return Err(ProfileError::NotFound(id.to_string()));
        }
        self.active_profile = Some(id);
        Ok(())
    }
    
    pub fn active(&self) -> Option<&Profile> {
        self.active_profile.and_then(|id| self.profiles.get(&id))
    }
    
    pub fn save(&self, id: Uuid) -> Result<(), ProfileError> {
        let profile = self.profiles.get(&id)
            .ok_or_else(|| ProfileError::NotFound(id.to_string()))?;
        self.save_profile(profile)
    }
    
    pub fn save_all(&self) -> Result<(), ProfileError> {
        for profile in self.profiles.values() {
            self.save_profile(profile)?;
        }
        Ok(())
    }
    
    pub fn export(&self, id: Uuid, path: &Path) -> Result<(), ProfileError> {
        let profile = self.profiles.get(&id)
            .ok_or_else(|| ProfileError::NotFound(id.to_string()))?;
        
        let content = serde_json::to_string_pretty(profile)
            .map_err(|e| ProfileError::SerializeError(e.to_string()))?;
        
        std::fs::write(path, content)?;
        Ok(())
    }
    
    pub fn import(&mut self, path: &Path) -> Result<&Profile, ProfileError> {
        let content = std::fs::read_to_string(path)?;
        let mut profile: Profile = serde_json::from_str(&content)
            .map_err(|e| ProfileError::SerializeError(e.to_string()))?;
        
        profile.id = Uuid::new_v4();
        
        if self.get_by_name(&profile.name).is_some() {
            profile.name = format!("{} (imported)", profile.name);
        }
        
        let id = profile.id;
        self.save_profile(&profile)?;
        self.profiles.insert(id, profile);
        
        Ok(self.profiles.get(&id).unwrap())
    }
    
    fn profile_path(&self, profile: &Profile) -> PathBuf {
        self.profiles_dir.join(format!("{}.json", profile.id))
    }
    
    fn save_profile(&self, profile: &Profile) -> Result<(), ProfileError> {
        std::fs::create_dir_all(&self.profiles_dir)?;
        
        let path = self.profile_path(profile);
        let content = serde_json::to_string_pretty(profile)
            .map_err(|e| ProfileError::SerializeError(e.to_string()))?;
        
        std::fs::write(path, content)?;
        Ok(())
    }
}
