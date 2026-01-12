use dashmap::DashMap;
use serde::{Deserialize, Serialize};
use tracing::info;
use uuid::Uuid;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum CosmeticScope {
    Session,
    Server,
    Event,
    Permanent,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum CosmeticType {
    Skin,
    Cape,
    Hat,
    Particle,
    Emote,
    Mount,
    Pet,
    Trail,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Cosmetic {
    pub id: Uuid,
    pub name: String,
    pub cosmetic_type: CosmeticType,
    pub scope: CosmeticScope,
    pub creator_id: Option<Uuid>,
    pub asset_hash: String,
    pub metadata: CosmeticMetadata,
    pub approved: bool,
    pub enabled: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CosmeticMetadata {
    pub file_size_bytes: u64,
    pub dimensions: Option<(u32, u32)>,
    pub animated: bool,
    pub frame_count: Option<u32>,
    pub tags: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CosmeticOwnership {
    pub user_id: Uuid,
    pub cosmetic_id: Uuid,
    pub acquired_at: chrono::DateTime<chrono::Utc>,
    pub expires_at: Option<chrono::DateTime<chrono::Utc>>,
    pub source: String,
}

pub struct AssetRegistry {
    cosmetics: DashMap<Uuid, Cosmetic>,
    ownership: DashMap<Uuid, Vec<CosmeticOwnership>>,
    approval_rules: DashMap<String, ApprovalRule>,
    allowed_types: DashMap<CosmeticType, bool>,
}

#[derive(Debug, Clone)]
pub struct ApprovalRule {
    pub name: String,
    pub max_file_size: u64,
    pub allowed_extensions: Vec<String>,
    pub require_manual_review: bool,
}

impl AssetRegistry {
    pub fn new() -> Self {
        let registry = Self {
            cosmetics: DashMap::new(),
            ownership: DashMap::new(),
            approval_rules: DashMap::new(),
            allowed_types: DashMap::new(),
        };
        
        for cosmetic_type in [
            CosmeticType::Skin,
            CosmeticType::Cape,
            CosmeticType::Hat,
            CosmeticType::Particle,
            CosmeticType::Emote,
            CosmeticType::Mount,
            CosmeticType::Pet,
            CosmeticType::Trail,
        ] {
            registry.allowed_types.insert(cosmetic_type, true);
        }
        
        registry.approval_rules.insert("default".to_string(), ApprovalRule {
            name: "default".to_string(),
            max_file_size: 5 * 1024 * 1024,
            allowed_extensions: vec!["png".to_string(), "json".to_string()],
            require_manual_review: true,
        });
        
        registry
    }
    
    pub fn register_cosmetic(&self, cosmetic: Cosmetic) -> Result<Uuid, String> {
        if !self.allowed_types.get(&cosmetic.cosmetic_type).map(|v| *v).unwrap_or(false) {
            return Err(format!("Cosmetic type {:?} is not allowed", cosmetic.cosmetic_type));
        }
        
        self.validate_cosmetic(&cosmetic)?;
        
        let id = cosmetic.id;
        self.cosmetics.insert(id, cosmetic);
        info!("Registered cosmetic: {}", id);
        Ok(id)
    }
    
    fn validate_cosmetic(&self, cosmetic: &Cosmetic) -> Result<(), String> {
        let rule = self.approval_rules.get("default")
            .map(|r| r.clone())
            .unwrap_or(ApprovalRule {
                name: "default".to_string(),
                max_file_size: 5 * 1024 * 1024,
                allowed_extensions: vec![],
                require_manual_review: true,
            });
        
        if cosmetic.metadata.file_size_bytes > rule.max_file_size {
            return Err(format!(
                "File size {} exceeds maximum {}",
                cosmetic.metadata.file_size_bytes,
                rule.max_file_size
            ));
        }
        
        Ok(())
    }
    
    pub fn get_cosmetic(&self, id: Uuid) -> Option<Cosmetic> {
        self.cosmetics.get(&id).map(|c| c.clone())
    }
    
    pub fn list_cosmetics(&self, cosmetic_type: Option<CosmeticType>) -> Vec<Cosmetic> {
        self.cosmetics.iter()
            .filter(|c| {
                c.approved && c.enabled && 
                cosmetic_type.map(|t| c.cosmetic_type == t).unwrap_or(true)
            })
            .map(|c| c.clone())
            .collect()
    }
    
    pub fn grant_ownership(&self, user_id: Uuid, cosmetic_id: Uuid, source: String, expires: Option<chrono::DateTime<chrono::Utc>>) -> Result<(), String> {
        if !self.cosmetics.contains_key(&cosmetic_id) {
            return Err("Cosmetic not found".to_string());
        }
        
        let ownership = CosmeticOwnership {
            user_id,
            cosmetic_id,
            acquired_at: chrono::Utc::now(),
            expires_at: expires,
            source,
        };
        
        self.ownership.entry(user_id)
            .or_insert_with(Vec::new)
            .push(ownership);
        
        info!("Granted cosmetic {} to user {}", cosmetic_id, user_id);
        Ok(())
    }
    
    pub fn check_ownership(&self, user_id: Uuid, cosmetic_id: Uuid) -> bool {
        self.ownership.get(&user_id)
            .map(|list| {
                list.iter().any(|o| {
                    o.cosmetic_id == cosmetic_id &&
                    o.expires_at.map(|exp| exp > chrono::Utc::now()).unwrap_or(true)
                })
            })
            .unwrap_or(false)
    }
    
    pub fn get_user_cosmetics(&self, user_id: Uuid) -> Vec<Cosmetic> {
        let now = chrono::Utc::now();
        
        self.ownership.get(&user_id)
            .map(|list| {
                list.iter()
                    .filter(|o| o.expires_at.map(|exp| exp > now).unwrap_or(true))
                    .filter_map(|o| self.cosmetics.get(&o.cosmetic_id).map(|c| c.clone()))
                    .collect()
            })
            .unwrap_or_default()
    }
    
    pub fn set_cosmetic_type_enabled(&self, cosmetic_type: CosmeticType, enabled: bool) {
        self.allowed_types.insert(cosmetic_type, enabled);
    }
    
    pub fn approve_cosmetic(&self, id: Uuid) -> Result<(), String> {
        let mut cosmetic = self.cosmetics.get_mut(&id)
            .ok_or("Cosmetic not found")?;
        cosmetic.approved = true;
        info!("Approved cosmetic: {}", id);
        Ok(())
    }
    
    pub fn set_cosmetic_enabled(&self, id: Uuid, enabled: bool) -> Result<(), String> {
        let mut cosmetic = self.cosmetics.get_mut(&id)
            .ok_or("Cosmetic not found")?;
        cosmetic.enabled = enabled;
        Ok(())
    }
    
    pub fn validate_asset_manifest(&self, manifest: &AssetManifest) -> ValidationResult {
        let mut valid = Vec::new();
        let mut invalid = Vec::new();
        
        for asset_id in &manifest.cosmetic_ids {
            if self.cosmetics.get(asset_id)
                .map(|c| c.approved && c.enabled)
                .unwrap_or(false)
            {
                valid.push(*asset_id);
            } else {
                invalid.push(*asset_id);
            }
        }
        
        ValidationResult { valid, invalid }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AssetManifest {
    pub user_id: Uuid,
    pub cosmetic_ids: Vec<Uuid>,
    pub profile_id: Option<Uuid>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ValidationResult {
    pub valid: Vec<Uuid>,
    pub invalid: Vec<Uuid>,
}
