use super::config::WaypointConfig;
use super::types::{Waypoint, WaypointGroup, WaypointHudInfo, WaypointType, WaypointVisibility};
use chrono::Utc;
use dashmap::DashMap;
use parking_lot::RwLock;
use std::sync::Arc;
use uuid::Uuid;

pub struct WaypointService {
    config: Arc<RwLock<WaypointConfig>>,
    waypoints: DashMap<Uuid, Waypoint>,
    owner_index: DashMap<Uuid, Vec<Uuid>>,
    dimension_index: DashMap<String, Vec<Uuid>>,
    groups: DashMap<Uuid, Vec<WaypointGroup>>,
    proximity_tracking: DashMap<Uuid, Vec<Uuid>>,
}

impl WaypointService {
    pub fn new(config: WaypointConfig) -> Self {
        Self {
            config: Arc::new(RwLock::new(config)),
            waypoints: DashMap::new(),
            owner_index: DashMap::new(),
            dimension_index: DashMap::new(),
            groups: DashMap::new(),
            proximity_tracking: DashMap::new(),
        }
    }

    pub fn create_waypoint(&self, waypoint: Waypoint) -> Result<Uuid, String> {
        let config = self.config.read();
        
        if !config.enabled {
            return Err("Waypoints are disabled".to_string());
        }

        if !config.permissions.allow_create {
            return Err("Creating waypoints is not allowed".to_string());
        }

        let owner_count = self.owner_index.get(&waypoint.owner_id)
            .map(|v| v.len())
            .unwrap_or(0);

        if owner_count >= config.max_waypoints_per_player as usize {
            return Err(format!("Maximum waypoints ({}) reached", config.max_waypoints_per_player));
        }

        if !config.permissions.allowed_dimensions.contains(&waypoint.dimension) {
            return Err("Waypoints not allowed in this dimension".to_string());
        }

        if waypoint.name.len() > config.permissions.max_name_length {
            return Err(format!("Waypoint name too long (max {} characters)", config.permissions.max_name_length));
        }

        if config.permissions.blacklisted_names.iter().any(|n| waypoint.name.to_lowercase().contains(&n.to_lowercase())) {
            return Err("Waypoint name contains blacklisted words".to_string());
        }

        match waypoint.waypoint_type {
            WaypointType::Death if !config.features.death_waypoints => {
                return Err("Death waypoints are disabled".to_string());
            }
            WaypointType::Spawn if !config.features.spawn_waypoints => {
                return Err("Spawn waypoints are disabled".to_string());
            }
            WaypointType::Home if !config.features.home_waypoints => {
                return Err("Home waypoints are disabled".to_string());
            }
            WaypointType::Portal if !config.features.portal_waypoints => {
                return Err("Portal waypoints are disabled".to_string());
            }
            WaypointType::Shared if !config.features.shared_waypoints => {
                return Err("Shared waypoints are disabled".to_string());
            }
            WaypointType::Global if !config.features.global_waypoints => {
                return Err("Global waypoints are disabled".to_string());
            }
            _ => {}
        }

        if waypoint.beam_enabled && (!config.permissions.allow_beam || !config.features.beam_display) {
            return Err("Beacon beams are not allowed".to_string());
        }

        if waypoint.sound_enabled && (!config.permissions.allow_sound || !config.features.sound_alerts) {
            return Err("Sound alerts are not allowed".to_string());
        }

        if waypoint.proximity_radius.is_some() && !config.features.proximity_alerts {
            return Err("Proximity alerts are not allowed".to_string());
        }

        if waypoint.temporary && !config.features.temporary_waypoints {
            return Err("Temporary waypoints are not allowed".to_string());
        }

        drop(config);

        let id = waypoint.id;
        let owner = waypoint.owner_id;
        let dimension = waypoint.dimension.clone();

        self.waypoints.insert(id, waypoint);
        
        self.owner_index.entry(owner)
            .or_insert_with(Vec::new)
            .push(id);
        
        self.dimension_index.entry(dimension)
            .or_insert_with(Vec::new)
            .push(id);

        Ok(id)
    }

    pub fn delete_waypoint(&self, waypoint_id: Uuid, requester_id: Uuid) -> Result<(), String> {
        let config = self.config.read();
        if !config.permissions.allow_delete {
            return Err("Deleting waypoints is not allowed".to_string());
        }
        drop(config);

        let waypoint = self.waypoints.get(&waypoint_id)
            .ok_or("Waypoint not found")?;
        
        if waypoint.owner_id != requester_id {
            return Err("You can only delete your own waypoints".to_string());
        }

        let owner = waypoint.owner_id;
        let dimension = waypoint.dimension.clone();
        drop(waypoint);

        self.waypoints.remove(&waypoint_id);

        if let Some(mut ids) = self.owner_index.get_mut(&owner) {
            ids.retain(|id| *id != waypoint_id);
        }

        if let Some(mut ids) = self.dimension_index.get_mut(&dimension) {
            ids.retain(|id| *id != waypoint_id);
        }

        Ok(())
    }

    pub fn update_waypoint<F>(&self, waypoint_id: Uuid, requester_id: Uuid, updater: F) -> Result<(), String>
    where
        F: FnOnce(&mut Waypoint),
    {
        let config = self.config.read();
        if !config.permissions.allow_edit {
            return Err("Editing waypoints is not allowed".to_string());
        }
        drop(config);

        let mut waypoint = self.waypoints.get_mut(&waypoint_id)
            .ok_or("Waypoint not found")?;
        
        if waypoint.owner_id != requester_id {
            return Err("You can only edit your own waypoints".to_string());
        }

        updater(&mut waypoint);
        waypoint.updated_at = Utc::now();

        Ok(())
    }

    pub fn get_waypoint(&self, waypoint_id: Uuid) -> Option<Waypoint> {
        self.waypoints.get(&waypoint_id).map(|w| w.clone())
    }

    pub fn get_player_waypoints(&self, player_id: Uuid) -> Vec<Waypoint> {
        self.owner_index.get(&player_id)
            .map(|ids| ids.iter()
                .filter_map(|id| self.waypoints.get(id).map(|w| w.clone()))
                .collect())
            .unwrap_or_default()
    }

    pub fn get_visible_waypoints(&self, player_id: Uuid, dimension: &str) -> Vec<Waypoint> {
        self.dimension_index.get(dimension)
            .map(|ids| ids.iter()
                .filter_map(|id| self.waypoints.get(id))
                .filter(|w| w.is_visible_to(player_id))
                .map(|w| w.clone())
                .collect())
            .unwrap_or_default()
    }

    pub fn get_hud_waypoints(&self, player_id: Uuid, player_x: f64, player_y: f64, player_z: f64, player_yaw: f32, dimension: &str) -> Vec<WaypointHudInfo> {
        let config = self.config.read();
        if !config.display.hud_enabled {
            return Vec::new();
        }
        
        let render_distance = config.display.render_distance;
        let min_render = config.display.min_render_distance;
        let fade_near = config.display.fade_near_distance;
        let fade_far = config.display.fade_far_distance;
        drop(config);

        self.get_visible_waypoints(player_id, dimension)
            .into_iter()
            .filter_map(|wp| {
                let distance = wp.horizontal_distance_to(player_x, player_z);
                
                if distance > render_distance || distance < min_render {
                    return None;
                }

                let alpha = if distance < fade_near {
                    ((distance - min_render) / (fade_near - min_render)) as f32
                } else if distance > fade_far {
                    (1.0 - (distance - fade_far) / (render_distance - fade_far)) as f32
                } else {
                    1.0
                };

                if wp.visibility == WaypointVisibility::FadeWithDistance && alpha < 0.1 {
                    return None;
                }

                let direction = wp.direction_from(player_x, player_z);
                let relative_direction = (direction - player_yaw as f64 + 360.0) % 360.0;
                
                let on_screen = relative_direction > 270.0 || relative_direction < 90.0;
                
                let screen_x = ((relative_direction - 180.0) / 180.0) as f32;
                let screen_y = ((wp.y - player_y) / distance * 0.5) as f32;

                Some(WaypointHudInfo {
                    waypoint: wp,
                    distance,
                    direction: relative_direction,
                    screen_x,
                    screen_y,
                    on_screen,
                    alpha,
                })
            })
            .collect()
    }

    pub fn share_waypoint(&self, waypoint_id: Uuid, requester_id: Uuid, share: bool) -> Result<(), String> {
        let config = self.config.read();
        if !config.features.shared_waypoints || !config.permissions.allow_share {
            return Err("Sharing waypoints is not allowed".to_string());
        }
        drop(config);

        self.update_waypoint(waypoint_id, requester_id, |wp| {
            wp.shared = share;
        })
    }

    pub fn share_with_player(&self, waypoint_id: Uuid, requester_id: Uuid, target_id: Uuid) -> Result<(), String> {
        let config = self.config.read();
        if !config.features.shared_waypoints || !config.permissions.allow_share {
            return Err("Sharing waypoints is not allowed".to_string());
        }
        drop(config);

        self.update_waypoint(waypoint_id, requester_id, |wp| {
            if !wp.shared_with.contains(&target_id) {
                wp.shared_with.push(target_id);
            }
        })
    }

    pub fn set_beam(&self, waypoint_id: Uuid, requester_id: Uuid, enabled: bool) -> Result<(), String> {
        let config = self.config.read();
        if enabled && (!config.permissions.allow_beam || !config.features.beam_display) {
            return Err("Beacon beams are not allowed".to_string());
        }
        drop(config);

        self.update_waypoint(waypoint_id, requester_id, |wp| {
            wp.beam_enabled = enabled;
        })
    }

    pub fn set_sound(&self, waypoint_id: Uuid, requester_id: Uuid, enabled: bool) -> Result<(), String> {
        let config = self.config.read();
        if enabled && (!config.permissions.allow_sound || !config.features.sound_alerts) {
            return Err("Sound alerts are not allowed".to_string());
        }
        drop(config);

        self.update_waypoint(waypoint_id, requester_id, |wp| {
            wp.sound_enabled = enabled;
        })
    }

    pub fn set_proximity(&self, waypoint_id: Uuid, requester_id: Uuid, radius: Option<f64>) -> Result<(), String> {
        let config = self.config.read();
        if radius.is_some() && !config.features.proximity_alerts {
            return Err("Proximity alerts are not allowed".to_string());
        }
        drop(config);

        self.update_waypoint(waypoint_id, requester_id, |wp| {
            wp.proximity_radius = radius;
        })
    }

    pub fn check_proximity(&self, player_id: Uuid, player_x: f64, player_z: f64, dimension: &str) -> Vec<Waypoint> {
        let config = self.config.read();
        if !config.features.proximity_alerts {
            return Vec::new();
        }
        drop(config);

        let mut triggered = Vec::new();
        let mut already_triggered = self.proximity_tracking.entry(player_id)
            .or_insert_with(Vec::new);

        for wp in self.get_visible_waypoints(player_id, dimension) {
            if let Some(radius) = wp.proximity_radius {
                let distance = wp.horizontal_distance_to(player_x, player_z);
                
                if distance <= radius {
                    if !already_triggered.contains(&wp.id) {
                        already_triggered.push(wp.id);
                        triggered.push(wp);
                    }
                } else {
                    already_triggered.retain(|id| *id != wp.id);
                }
            }
        }

        triggered
    }

    pub fn record_visit(&self, waypoint_id: Uuid) {
        if let Some(mut wp) = self.waypoints.get_mut(&waypoint_id) {
            wp.record_visit();
        }
    }

    pub fn create_group(&self, player_id: Uuid, group: WaypointGroup) {
        self.groups.entry(player_id)
            .or_insert_with(Vec::new)
            .push(group);
    }

    pub fn get_groups(&self, player_id: Uuid) -> Vec<WaypointGroup> {
        self.groups.get(&player_id)
            .map(|g| g.clone())
            .unwrap_or_default()
    }

    pub fn cleanup_expired(&self) {
        let now = Utc::now();
        let expired: Vec<_> = self.waypoints.iter()
            .filter(|w| w.expires_at.map(|e| e < now).unwrap_or(false))
            .map(|w| (w.id, w.owner_id))
            .collect();

        for (id, owner) in expired {
            self.waypoints.remove(&id);
            if let Some(mut ids) = self.owner_index.get_mut(&owner) {
                ids.retain(|i| *i != id);
            }
        }
    }

    pub fn config(&self) -> &Arc<RwLock<WaypointConfig>> {
        &self.config
    }

    pub fn is_enabled(&self) -> bool {
        self.config.read().enabled
    }

    pub fn set_enabled(&self, enabled: bool) {
        self.config.write().enabled = enabled;
    }
}
