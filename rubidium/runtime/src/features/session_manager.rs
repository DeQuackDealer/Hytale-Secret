use dashmap::DashMap;
use std::time::Duration;
use uuid::Uuid;
use ahash::RandomState;
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PlayerSession {
    pub player_id: Uuid,
    pub session_id: Uuid,
    pub username: String,
    pub connected_at: u64,
    pub last_activity: u64,
    pub yellow_tale_session: Option<String>,
    pub is_premium: bool,
    pub premium_tier: Option<String>,
    pub cosmetics: Vec<String>,
    pub friends_online: Vec<Uuid>,
}

impl PlayerSession {
    pub fn new(player_id: Uuid, username: String) -> Self {
        let now = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap()
            .as_secs();
        
        Self {
            player_id,
            session_id: Uuid::new_v4(),
            username,
            connected_at: now,
            last_activity: now,
            yellow_tale_session: None,
            is_premium: false,
            premium_tier: None,
            cosmetics: Vec::new(),
            friends_online: Vec::new(),
        }
    }

    pub fn session_duration(&self) -> Duration {
        let now = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap()
            .as_secs();
        Duration::from_secs(now - self.connected_at)
    }

    pub fn update_activity(&mut self) {
        self.last_activity = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap()
            .as_secs();
    }
}

pub struct SessionManager {
    sessions: DashMap<Uuid, PlayerSession, RandomState>,
    username_index: DashMap<String, Uuid, RandomState>,
    session_timeout: Duration,
    yellow_tale_enabled: bool,
}

impl SessionManager {
    pub fn new(session_timeout: Duration) -> Self {
        Self {
            sessions: DashMap::with_hasher(RandomState::new()),
            username_index: DashMap::with_hasher(RandomState::new()),
            session_timeout,
            yellow_tale_enabled: true,
        }
    }

    pub fn create_session(&self, player_id: Uuid, username: String) -> PlayerSession {
        let session = PlayerSession::new(player_id, username.clone());
        self.sessions.insert(player_id, session.clone());
        self.username_index.insert(username.to_lowercase(), player_id);
        session
    }

    pub fn get_session(&self, player_id: Uuid) -> Option<PlayerSession> {
        self.sessions.get(&player_id).map(|s| s.clone())
    }

    pub fn get_session_by_username(&self, username: &str) -> Option<PlayerSession> {
        self.username_index
            .get(&username.to_lowercase())
            .and_then(|id| self.sessions.get(&id).map(|s| s.clone()))
    }

    pub fn update_session<F>(&self, player_id: Uuid, updater: F) -> bool
    where
        F: FnOnce(&mut PlayerSession),
    {
        if let Some(mut session) = self.sessions.get_mut(&player_id) {
            updater(&mut session);
            session.update_activity();
            true
        } else {
            false
        }
    }

    pub fn remove_session(&self, player_id: Uuid) -> Option<PlayerSession> {
        if let Some((_, session)) = self.sessions.remove(&player_id) {
            self.username_index.remove(&session.username.to_lowercase());
            Some(session)
        } else {
            None
        }
    }

    pub fn link_yellow_tale(&self, player_id: Uuid, yt_session: String) -> bool {
        self.update_session(player_id, |session| {
            session.yellow_tale_session = Some(yt_session);
        })
    }

    pub fn set_premium(&self, player_id: Uuid, is_premium: bool, tier: Option<String>) -> bool {
        self.update_session(player_id, |session| {
            session.is_premium = is_premium;
            session.premium_tier = tier;
        })
    }

    pub fn set_cosmetics(&self, player_id: Uuid, cosmetics: Vec<String>) -> bool {
        self.update_session(player_id, |session| {
            session.cosmetics = cosmetics;
        })
    }

    pub fn update_friends_online(&self, player_id: Uuid, friends: Vec<Uuid>) -> bool {
        self.update_session(player_id, |session| {
            session.friends_online = friends;
        })
    }

    pub fn get_online_count(&self) -> usize {
        self.sessions.len()
    }

    pub fn get_all_sessions(&self) -> Vec<PlayerSession> {
        self.sessions.iter().map(|e| e.value().clone()).collect()
    }

    pub fn get_premium_count(&self) -> usize {
        self.sessions.iter().filter(|e| e.value().is_premium).count()
    }

    pub fn cleanup_stale_sessions(&self) -> usize {
        let now = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap()
            .as_secs();
        
        let timeout_secs = self.session_timeout.as_secs();
        let mut removed = 0;

        self.sessions.retain(|_, session| {
            if now - session.last_activity > timeout_secs {
                self.username_index.remove(&session.username.to_lowercase());
                removed += 1;
                false
            } else {
                true
            }
        });

        removed
    }
}
