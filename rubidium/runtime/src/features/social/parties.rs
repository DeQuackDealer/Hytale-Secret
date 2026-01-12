use super::config::PartyConfig;
use chrono::{DateTime, Utc};
use dashmap::DashMap;
use parking_lot::RwLock;
use serde::{Deserialize, Serialize};
use std::sync::Arc;
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Party {
    pub id: Uuid,
    pub name: Option<String>,
    pub leader_id: Uuid,
    pub members: Vec<Uuid>,
    pub is_public: bool,
    pub created_at: DateTime<Utc>,
    pub settings: PartySettings,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PartySettings {
    pub waypoint_sharing: bool,
    pub location_sharing: bool,
    pub party_chat: bool,
    pub allow_invites: bool,
}

impl Default for PartySettings {
    fn default() -> Self {
        Self {
            waypoint_sharing: true,
            location_sharing: true,
            party_chat: true,
            allow_invites: true,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PartyInvite {
    pub id: Uuid,
    pub party_id: Uuid,
    pub from_id: Uuid,
    pub to_id: Uuid,
    pub created_at: DateTime<Utc>,
    pub expires_at: DateTime<Utc>,
}

pub struct PartyService {
    config: Arc<RwLock<PartyConfig>>,
    parties: DashMap<Uuid, Party>,
    player_parties: DashMap<Uuid, Uuid>,
    invites: DashMap<Uuid, PartyInvite>,
    player_invites: DashMap<Uuid, Vec<Uuid>>,
}

impl PartyService {
    pub fn new(config: PartyConfig) -> Self {
        Self {
            config: Arc::new(RwLock::new(config)),
            parties: DashMap::new(),
            player_parties: DashMap::new(),
            invites: DashMap::new(),
            player_invites: DashMap::new(),
        }
    }

    pub fn create_party(&self, leader_id: Uuid, name: Option<String>) -> Result<Uuid, String> {
        let config = self.config.read();
        if !config.enabled {
            return Err("Parties are disabled".to_string());
        }
        drop(config);

        if self.player_parties.contains_key(&leader_id) {
            return Err("You are already in a party".to_string());
        }

        let party_id = Uuid::new_v4();
        let party = Party {
            id: party_id,
            name,
            leader_id,
            members: vec![leader_id],
            is_public: false,
            created_at: Utc::now(),
            settings: PartySettings::default(),
        };

        self.parties.insert(party_id, party);
        self.player_parties.insert(leader_id, party_id);

        Ok(party_id)
    }

    pub fn invite_player(&self, party_id: Uuid, from_id: Uuid, to_id: Uuid) -> Result<Uuid, String> {
        let config = self.config.read();
        let timeout = config.invite_timeout_secs;
        drop(config);

        let party = self.parties.get(&party_id)
            .ok_or("Party not found")?;

        if !party.settings.allow_invites && party.leader_id != from_id {
            return Err("Only the party leader can invite".to_string());
        }

        if !party.members.contains(&from_id) {
            return Err("You are not in this party".to_string());
        }

        drop(party);

        if self.player_parties.contains_key(&to_id) {
            return Err("Player is already in a party".to_string());
        }

        let invite_id = Uuid::new_v4();
        let now = Utc::now();
        let invite = PartyInvite {
            id: invite_id,
            party_id,
            from_id,
            to_id,
            created_at: now,
            expires_at: now + chrono::Duration::seconds(timeout as i64),
        };

        self.invites.insert(invite_id, invite);
        self.player_invites.entry(to_id)
            .or_insert_with(Vec::new)
            .push(invite_id);

        Ok(invite_id)
    }

    pub fn accept_invite(&self, invite_id: Uuid, player_id: Uuid) -> Result<Uuid, String> {
        let invite = self.invites.remove(&invite_id)
            .ok_or("Invite not found or expired")?.1;

        if invite.to_id != player_id {
            self.invites.insert(invite_id, invite);
            return Err("This invite is not for you".to_string());
        }

        if Utc::now() > invite.expires_at {
            return Err("Invite has expired".to_string());
        }

        self.join_party(invite.party_id, player_id)
    }

    pub fn decline_invite(&self, invite_id: Uuid, player_id: Uuid) -> Result<(), String> {
        let invite = self.invites.get(&invite_id)
            .ok_or("Invite not found")?;

        if invite.to_id != player_id {
            return Err("This invite is not for you".to_string());
        }

        drop(invite);
        self.invites.remove(&invite_id);

        if let Some(mut invites) = self.player_invites.get_mut(&player_id) {
            invites.retain(|id| *id != invite_id);
        }

        Ok(())
    }

    pub fn join_party(&self, party_id: Uuid, player_id: Uuid) -> Result<Uuid, String> {
        let config = self.config.read();
        let max_size = config.max_party_size as usize;
        drop(config);

        if self.player_parties.contains_key(&player_id) {
            return Err("You are already in a party".to_string());
        }

        let mut party = self.parties.get_mut(&party_id)
            .ok_or("Party not found")?;

        if party.members.len() >= max_size {
            return Err("Party is full".to_string());
        }

        party.members.push(player_id);
        drop(party);

        self.player_parties.insert(player_id, party_id);

        Ok(party_id)
    }

    pub fn leave_party(&self, player_id: Uuid) -> Result<(), String> {
        let party_id = self.player_parties.remove(&player_id)
            .ok_or("You are not in a party")?.1;

        let mut party = self.parties.get_mut(&party_id)
            .ok_or("Party not found")?;

        party.members.retain(|id| *id != player_id);

        if party.members.is_empty() {
            drop(party);
            self.parties.remove(&party_id);
        } else if party.leader_id == player_id {
            party.leader_id = party.members[0];
        }

        Ok(())
    }

    pub fn kick_player(&self, party_id: Uuid, leader_id: Uuid, target_id: Uuid) -> Result<(), String> {
        let party = self.parties.get(&party_id)
            .ok_or("Party not found")?;

        if party.leader_id != leader_id {
            return Err("Only the party leader can kick members".to_string());
        }

        if target_id == leader_id {
            return Err("You cannot kick yourself".to_string());
        }

        drop(party);

        self.player_parties.remove(&target_id);

        if let Some(mut party) = self.parties.get_mut(&party_id) {
            party.members.retain(|id| *id != target_id);
        }

        Ok(())
    }

    pub fn transfer_leadership(&self, party_id: Uuid, leader_id: Uuid, new_leader_id: Uuid) -> Result<(), String> {
        let mut party = self.parties.get_mut(&party_id)
            .ok_or("Party not found")?;

        if party.leader_id != leader_id {
            return Err("Only the party leader can transfer leadership".to_string());
        }

        if !party.members.contains(&new_leader_id) {
            return Err("Target player is not in the party".to_string());
        }

        party.leader_id = new_leader_id;

        Ok(())
    }

    pub fn disband_party(&self, party_id: Uuid, leader_id: Uuid) -> Result<(), String> {
        let party = self.parties.get(&party_id)
            .ok_or("Party not found")?;

        if party.leader_id != leader_id {
            return Err("Only the party leader can disband the party".to_string());
        }

        let members = party.members.clone();
        drop(party);

        for member in members {
            self.player_parties.remove(&member);
        }

        self.parties.remove(&party_id);

        Ok(())
    }

    pub fn get_party(&self, party_id: Uuid) -> Option<Party> {
        self.parties.get(&party_id).map(|p| p.clone())
    }

    pub fn get_player_party(&self, player_id: Uuid) -> Option<Party> {
        let party_id = self.player_parties.get(&player_id)?;
        self.get_party(*party_id)
    }

    pub fn get_pending_invites(&self, player_id: Uuid) -> Vec<PartyInvite> {
        let now = Utc::now();
        self.player_invites.get(&player_id)
            .map(|ids| ids.iter()
                .filter_map(|id| self.invites.get(id))
                .filter(|i| i.expires_at > now)
                .map(|i| i.clone())
                .collect())
            .unwrap_or_default()
    }

    pub fn cleanup_expired_invites(&self) {
        let now = Utc::now();
        let expired: Vec<_> = self.invites.iter()
            .filter(|i| i.expires_at < now)
            .map(|i| i.id)
            .collect();

        for id in expired {
            self.invites.remove(&id);
        }
    }

    pub fn config(&self) -> &Arc<RwLock<PartyConfig>> {
        &self.config
    }
}
