use super::config::PresenceConfig;
use chrono::{DateTime, Utc};
use dashmap::DashMap;
use parking_lot::RwLock;
use serde::{Deserialize, Serialize};
use std::sync::Arc;
use uuid::Uuid;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum PresenceStatus {
    Online,
    Idle,
    Busy,
    DoNotDisturb,
    Invisible,
    Offline,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PlayerPresence {
    pub player_id: Uuid,
    pub player_name: String,
    pub status: PresenceStatus,
    pub activity: Option<String>,
    pub server: Option<String>,
    pub world: Option<String>,
    pub location: Option<(f64, f64, f64)>,
    pub party_id: Option<Uuid>,
    pub last_seen: DateTime<Utc>,
    pub last_activity: DateTime<Utc>,
}

impl PlayerPresence {
    pub fn new(player_id: Uuid, player_name: String) -> Self {
        let now = Utc::now();
        Self {
            player_id,
            player_name,
            status: PresenceStatus::Online,
            activity: None,
            server: None,
            world: None,
            location: None,
            party_id: None,
            last_seen: now,
            last_activity: now,
        }
    }
}

pub struct PresenceService {
    config: Arc<RwLock<PresenceConfig>>,
    presences: DashMap<Uuid, PlayerPresence>,
    friends: DashMap<Uuid, Vec<Uuid>>,
}

impl PresenceService {
    pub fn new(config: PresenceConfig) -> Self {
        Self {
            config: Arc::new(RwLock::new(config)),
            presences: DashMap::new(),
            friends: DashMap::new(),
        }
    }

    pub fn player_online(&self, player_id: Uuid, player_name: String) {
        let presence = PlayerPresence::new(player_id, player_name);
        self.presences.insert(player_id, presence);
    }

    pub fn player_offline(&self, player_id: Uuid) {
        if let Some(mut presence) = self.presences.get_mut(&player_id) {
            presence.status = PresenceStatus::Offline;
            presence.last_seen = Utc::now();
        }
    }

    pub fn update_activity(&self, player_id: Uuid, activity: String) {
        if let Some(mut presence) = self.presences.get_mut(&player_id) {
            presence.activity = Some(activity);
            presence.last_activity = Utc::now();
            presence.status = PresenceStatus::Online;
        }
    }

    pub fn update_location(&self, player_id: Uuid, world: String, x: f64, y: f64, z: f64) {
        let config = self.config.read();
        if !config.show_location {
            return;
        }
        drop(config);

        if let Some(mut presence) = self.presences.get_mut(&player_id) {
            presence.world = Some(world);
            presence.location = Some((x, y, z));
            presence.last_activity = Utc::now();
        }
    }

    pub fn set_status(&self, player_id: Uuid, status: PresenceStatus) {
        if let Some(mut presence) = self.presences.get_mut(&player_id) {
            presence.status = status;
        }
    }

    pub fn set_party(&self, player_id: Uuid, party_id: Option<Uuid>) {
        if let Some(mut presence) = self.presences.get_mut(&player_id) {
            presence.party_id = party_id;
        }
    }

    pub fn get_presence(&self, player_id: Uuid) -> Option<PlayerPresence> {
        self.presences.get(&player_id).map(|p| p.clone())
    }

    pub fn get_friend_presences(&self, player_id: Uuid) -> Vec<PlayerPresence> {
        self.friends.get(&player_id)
            .map(|friends| friends.iter()
                .filter_map(|id| self.get_presence(*id))
                .filter(|p| p.status != PresenceStatus::Invisible)
                .collect())
            .unwrap_or_default()
    }

    pub fn add_friend(&self, player_id: Uuid, friend_id: Uuid) {
        self.friends.entry(player_id)
            .or_insert_with(Vec::new)
            .push(friend_id);
    }

    pub fn remove_friend(&self, player_id: Uuid, friend_id: Uuid) {
        if let Some(mut friends) = self.friends.get_mut(&player_id) {
            friends.retain(|id| *id != friend_id);
        }
    }

    pub fn check_idle(&self) {
        let config = self.config.read();
        let idle_timeout = chrono::Duration::seconds(config.idle_timeout_secs as i64);
        drop(config);

        let now = Utc::now();
        for mut presence in self.presences.iter_mut() {
            if presence.status == PresenceStatus::Online {
                if now - presence.last_activity > idle_timeout {
                    presence.status = PresenceStatus::Idle;
                }
            }
        }
    }

    pub fn get_online_count(&self) -> usize {
        self.presences.iter()
            .filter(|p| p.status != PresenceStatus::Offline)
            .count()
    }

    pub fn config(&self) -> &Arc<RwLock<PresenceConfig>> {
        &self.config
    }
}
