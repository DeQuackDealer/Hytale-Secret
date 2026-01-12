use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SocialConfig {
    pub enabled: bool,
    pub presence: PresenceConfig,
    pub parties: PartyConfig,
    pub permissions: SocialPermissions,
}

impl Default for SocialConfig {
    fn default() -> Self {
        Self {
            enabled: true,
            presence: PresenceConfig::default(),
            parties: PartyConfig::default(),
            permissions: SocialPermissions::default(),
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PresenceConfig {
    pub enabled: bool,
    pub show_activity: bool,
    pub show_server: bool,
    pub show_location: bool,
    pub idle_timeout_secs: u32,
    pub update_interval_ms: u32,
}

impl Default for PresenceConfig {
    fn default() -> Self {
        Self {
            enabled: true,
            show_activity: true,
            show_server: true,
            show_location: false,
            idle_timeout_secs: 300,
            update_interval_ms: 5000,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PartyConfig {
    pub enabled: bool,
    pub max_party_size: u32,
    pub invite_timeout_secs: u32,
    pub allow_public_parties: bool,
    pub party_chat: bool,
    pub waypoint_sharing: bool,
    pub location_sharing: bool,
}

impl Default for PartyConfig {
    fn default() -> Self {
        Self {
            enabled: true,
            max_party_size: 8,
            invite_timeout_secs: 60,
            allow_public_parties: true,
            party_chat: true,
            waypoint_sharing: true,
            location_sharing: true,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SocialPermissions {
    pub allow_presence: bool,
    pub allow_parties: bool,
    pub allow_party_invite: bool,
    pub allow_party_create: bool,
}

impl Default for SocialPermissions {
    fn default() -> Self {
        Self {
            allow_presence: true,
            allow_parties: true,
            allow_party_invite: true,
            allow_party_create: true,
        }
    }
}
