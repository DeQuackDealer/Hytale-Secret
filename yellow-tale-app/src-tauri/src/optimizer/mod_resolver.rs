use serde::{Deserialize, Serialize};
use std::collections::{HashMap, HashSet, VecDeque};
use std::path::{Path, PathBuf};
use parking_lot::RwLock;
use tokio::fs;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModInfo {
    pub id: String,
    pub name: String,
    pub version: String,
    pub author: Option<String>,
    pub description: Option<String>,
    pub dependencies: Vec<ModDependency>,
    pub conflicts: Vec<String>,
    pub file_path: PathBuf,
    pub enabled: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModDependency {
    pub mod_id: String,
    pub version_requirement: Option<String>,
    pub optional: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModConflict {
    pub mod_a: String,
    pub mod_b: String,
    pub reason: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ResolutionResult {
    pub success: bool,
    pub load_order: Vec<String>,
    pub missing_dependencies: Vec<MissingDependency>,
    pub conflicts: Vec<ModConflict>,
    pub warnings: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MissingDependency {
    pub required_by: String,
    pub dependency_id: String,
    pub version_requirement: Option<String>,
    pub optional: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ContentProfile {
    pub id: String,
    pub name: String,
    pub description: Option<String>,
    pub enabled_mods: Vec<String>,
    pub mod_configs: HashMap<String, serde_json::Value>,
    pub created_at: chrono::DateTime<chrono::Utc>,
    pub last_used: Option<chrono::DateTime<chrono::Utc>>,
}

pub struct ModDependencyResolver {
    mods: RwLock<HashMap<String, ModInfo>>,
    profiles: RwLock<HashMap<String, ContentProfile>>,
    active_profile: RwLock<Option<String>>,
    mod_directory: RwLock<PathBuf>,
}

impl ModDependencyResolver {
    pub fn new() -> Self {
        let mod_dir = directories::ProjectDirs::from("com", "yellowtale", "YellowTale")
            .map(|dirs| dirs.data_dir().join("mods"))
            .unwrap_or_else(|| PathBuf::from("mods"));
        
        Self {
            mods: RwLock::new(HashMap::new()),
            profiles: RwLock::new(HashMap::new()),
            active_profile: RwLock::new(None),
            mod_directory: RwLock::new(mod_dir),
        }
    }
    
    pub fn set_mod_directory(&self, path: PathBuf) {
        *self.mod_directory.write() = path;
    }
    
    pub async fn scan_mods(&self) -> Result<Vec<ModInfo>, String> {
        let mod_dir = self.mod_directory.read().clone();
        
        if !mod_dir.exists() {
            fs::create_dir_all(&mod_dir).await
                .map_err(|e| format!("Failed to create mod directory: {}", e))?;
            return Ok(Vec::new());
        }
        
        let mut found_mods = Vec::new();
        let mut entries = fs::read_dir(&mod_dir).await
            .map_err(|e| format!("Failed to read mod directory: {}", e))?;
        
        while let Ok(Some(entry)) = entries.next_entry().await {
            let path = entry.path();
            
            if path.is_file() {
                if let Some(ext) = path.extension() {
                    if ext == "jar" || ext == "zip" || ext == "ytmod" {
                        if let Some(mod_info) = self.parse_mod_file(&path).await {
                            found_mods.push(mod_info);
                        }
                    }
                }
            } else if path.is_dir() {
                let manifest_path = path.join("mod.json");
                if manifest_path.exists() {
                    if let Some(mod_info) = self.parse_mod_manifest(&manifest_path).await {
                        found_mods.push(mod_info);
                    }
                }
            }
        }
        
        {
            let mut mods = self.mods.write();
            for mod_info in &found_mods {
                mods.insert(mod_info.id.clone(), mod_info.clone());
            }
        }
        
        tracing::info!("Scanned {} mods", found_mods.len());
        
        Ok(found_mods)
    }
    
    async fn parse_mod_file(&self, path: &Path) -> Option<ModInfo> {
        let file_name = path.file_stem()?.to_str()?;
        
        Some(ModInfo {
            id: file_name.to_lowercase().replace(' ', "_"),
            name: file_name.to_string(),
            version: "1.0.0".to_string(),
            author: None,
            description: None,
            dependencies: Vec::new(),
            conflicts: Vec::new(),
            file_path: path.to_path_buf(),
            enabled: true,
        })
    }
    
    async fn parse_mod_manifest(&self, manifest_path: &Path) -> Option<ModInfo> {
        let content = fs::read_to_string(manifest_path).await.ok()?;
        let json: serde_json::Value = serde_json::from_str(&content).ok()?;
        
        let id = json["id"].as_str()?.to_string();
        let name = json["name"].as_str().unwrap_or(&id).to_string();
        let version = json["version"].as_str().unwrap_or("1.0.0").to_string();
        
        let dependencies = json["dependencies"]
            .as_array()
            .map(|arr| {
                arr.iter()
                    .filter_map(|d| {
                        Some(ModDependency {
                            mod_id: d["id"].as_str()?.to_string(),
                            version_requirement: d["version"].as_str().map(|s| s.to_string()),
                            optional: d["optional"].as_bool().unwrap_or(false),
                        })
                    })
                    .collect()
            })
            .unwrap_or_default();
        
        let conflicts = json["conflicts"]
            .as_array()
            .map(|arr| {
                arr.iter()
                    .filter_map(|c| c.as_str().map(|s| s.to_string()))
                    .collect()
            })
            .unwrap_or_default();
        
        Some(ModInfo {
            id,
            name,
            version,
            author: json["author"].as_str().map(|s| s.to_string()),
            description: json["description"].as_str().map(|s| s.to_string()),
            dependencies,
            conflicts,
            file_path: manifest_path.parent()?.to_path_buf(),
            enabled: true,
        })
    }
    
    pub fn resolve_dependencies(&self, enabled_mod_ids: &[String]) -> ResolutionResult {
        let mods = self.mods.read();
        
        let mut missing = Vec::new();
        let mut conflicts = Vec::new();
        let mut warnings = Vec::new();
        
        let enabled_set: HashSet<&String> = enabled_mod_ids.iter().collect();
        
        for mod_id in enabled_mod_ids {
            if let Some(mod_info) = mods.get(mod_id) {
                for dep in &mod_info.dependencies {
                    if !enabled_set.contains(&dep.mod_id) && !mods.contains_key(&dep.mod_id) {
                        if dep.optional {
                            warnings.push(format!(
                                "Optional dependency '{}' for mod '{}' is not available",
                                dep.mod_id, mod_id
                            ));
                        } else {
                            missing.push(MissingDependency {
                                required_by: mod_id.clone(),
                                dependency_id: dep.mod_id.clone(),
                                version_requirement: dep.version_requirement.clone(),
                                optional: dep.optional,
                            });
                        }
                    }
                }
                
                for conflict_id in &mod_info.conflicts {
                    if enabled_set.contains(conflict_id) {
                        conflicts.push(ModConflict {
                            mod_a: mod_id.clone(),
                            mod_b: conflict_id.clone(),
                            reason: format!("Mod '{}' conflicts with '{}'", mod_id, conflict_id),
                        });
                    }
                }
            }
        }
        
        let load_order = self.topological_sort(enabled_mod_ids, &mods);
        
        let success = missing.iter().all(|m| m.optional) && conflicts.is_empty();
        
        ResolutionResult {
            success,
            load_order,
            missing_dependencies: missing,
            conflicts,
            warnings,
        }
    }
    
    fn topological_sort(&self, mod_ids: &[String], mods: &HashMap<String, ModInfo>) -> Vec<String> {
        let mut in_degree: HashMap<String, usize> = HashMap::new();
        let mut graph: HashMap<String, Vec<String>> = HashMap::new();
        
        for id in mod_ids {
            in_degree.entry(id.clone()).or_insert(0);
            graph.entry(id.clone()).or_insert_with(Vec::new);
        }
        
        for id in mod_ids {
            if let Some(mod_info) = mods.get(id) {
                for dep in &mod_info.dependencies {
                    if mod_ids.contains(&dep.mod_id) {
                        graph.entry(dep.mod_id.clone())
                            .or_insert_with(Vec::new)
                            .push(id.clone());
                        *in_degree.entry(id.clone()).or_insert(0) += 1;
                    }
                }
            }
        }
        
        let mut queue: VecDeque<String> = in_degree.iter()
            .filter(|(_, &deg)| deg == 0)
            .map(|(id, _)| id.clone())
            .collect();
        
        let mut result = Vec::new();
        
        while let Some(id) = queue.pop_front() {
            result.push(id.clone());
            
            if let Some(dependents) = graph.get(&id) {
                for dep in dependents {
                    if let Some(deg) = in_degree.get_mut(dep) {
                        *deg -= 1;
                        if *deg == 0 {
                            queue.push_back(dep.clone());
                        }
                    }
                }
            }
        }
        
        result
    }
    
    pub fn get_mods(&self) -> Vec<ModInfo> {
        self.mods.read().values().cloned().collect()
    }
    
    pub fn get_mod(&self, id: &str) -> Option<ModInfo> {
        self.mods.read().get(id).cloned()
    }
    
    pub fn set_mod_enabled(&self, id: &str, enabled: bool) -> Result<(), String> {
        let mut mods = self.mods.write();
        if let Some(mod_info) = mods.get_mut(id) {
            mod_info.enabled = enabled;
            Ok(())
        } else {
            Err("Mod not found".to_string())
        }
    }
    
    pub fn create_profile(&self, name: &str, enabled_mods: Vec<String>) -> ContentProfile {
        let profile = ContentProfile {
            id: uuid::Uuid::new_v4().to_string(),
            name: name.to_string(),
            description: None,
            enabled_mods,
            mod_configs: HashMap::new(),
            created_at: chrono::Utc::now(),
            last_used: None,
        };
        
        self.profiles.write().insert(profile.id.clone(), profile.clone());
        profile
    }
    
    pub fn get_profiles(&self) -> Vec<ContentProfile> {
        self.profiles.read().values().cloned().collect()
    }
    
    pub fn get_profile(&self, id: &str) -> Option<ContentProfile> {
        self.profiles.read().get(id).cloned()
    }
    
    pub fn activate_profile(&self, profile_id: &str) -> Result<ResolutionResult, String> {
        let profile = self.profiles.read()
            .get(profile_id)
            .cloned()
            .ok_or_else(|| "Profile not found".to_string())?;
        
        let result = self.resolve_dependencies(&profile.enabled_mods);
        
        if result.success {
            *self.active_profile.write() = Some(profile_id.to_string());
            
            if let Some(p) = self.profiles.write().get_mut(profile_id) {
                p.last_used = Some(chrono::Utc::now());
            }
        }
        
        Ok(result)
    }
    
    pub fn delete_profile(&self, id: &str) -> Result<(), String> {
        self.profiles.write().remove(id)
            .ok_or_else(|| "Profile not found".to_string())?;
        
        if self.active_profile.read().as_ref() == Some(&id.to_string()) {
            *self.active_profile.write() = None;
        }
        
        Ok(())
    }
}
