use chrono::{DateTime, Utc};
use dashmap::DashMap;
use serde::{Deserialize, Serialize};
use std::sync::atomic::{AtomicU64, Ordering};
use uuid::Uuid;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum MarkerType {
    Custom,
    Death,
    Spawn,
    Home,
    Portal,
    Structure,
    Poi,
    Player,
    Shared,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MapMarker {
    pub id: Uuid,
    pub owner_id: Uuid,
    pub marker_type: MarkerType,
    pub name: String,
    pub x: f64,
    pub y: f64,
    pub z: f64,
    pub dimension: String,
    pub color: u32,
    pub icon: String,
    pub visible: bool,
    pub shared: bool,
    pub shared_with: Vec<Uuid>,
    pub created_at: DateTime<Utc>,
    pub updated_at: DateTime<Utc>,
    pub temporary: bool,
    pub expires_at: Option<DateTime<Utc>>,
    pub distance_visible: bool,
    pub beam_visible: bool,
    pub metadata: Option<String>,
}

impl MapMarker {
    pub fn new(owner_id: Uuid, name: String, x: f64, y: f64, z: f64, dimension: String) -> Self {
        let now = Utc::now();
        Self {
            id: Uuid::new_v4(),
            owner_id,
            marker_type: MarkerType::Custom,
            name,
            x, y, z,
            dimension,
            color: 0xFFFFFF,
            icon: "default".to_string(),
            visible: true,
            shared: false,
            shared_with: Vec::new(),
            created_at: now,
            updated_at: now,
            temporary: false,
            expires_at: None,
            distance_visible: true,
            beam_visible: false,
            metadata: None,
        }
    }

    pub fn death(owner_id: Uuid, x: f64, y: f64, z: f64, dimension: String) -> Self {
        let mut marker = Self::new(owner_id, "Death".to_string(), x, y, z, dimension);
        marker.marker_type = MarkerType::Death;
        marker.color = 0xFF0000;
        marker.icon = "skull".to_string();
        marker.temporary = true;
        marker
    }

    pub fn spawn(owner_id: Uuid, x: f64, y: f64, z: f64, dimension: String) -> Self {
        let mut marker = Self::new(owner_id, "Spawn".to_string(), x, y, z, dimension);
        marker.marker_type = MarkerType::Spawn;
        marker.color = 0x00FF00;
        marker.icon = "bed".to_string();
        marker
    }

    pub fn home(owner_id: Uuid, x: f64, y: f64, z: f64, dimension: String) -> Self {
        let mut marker = Self::new(owner_id, "Home".to_string(), x, y, z, dimension);
        marker.marker_type = MarkerType::Home;
        marker.color = 0x00FFFF;
        marker.icon = "home".to_string();
        marker
    }

    pub fn distance_to(&self, x: f64, y: f64, z: f64) -> f64 {
        let dx = self.x - x;
        let dy = self.y - y;
        let dz = self.z - z;
        (dx * dx + dy * dy + dz * dz).sqrt()
    }

    pub fn horizontal_distance_to(&self, x: f64, z: f64) -> f64 {
        let dx = self.x - x;
        let dz = self.z - z;
        (dx * dx + dz * dz).sqrt()
    }

    pub fn direction_to(&self, x: f64, z: f64) -> f64 {
        let dx = self.x - x;
        let dz = self.z - z;
        dz.atan2(dx).to_degrees()
    }
}

pub struct MarkerRegistry {
    markers: DashMap<Uuid, MapMarker>,
    owner_index: DashMap<Uuid, Vec<Uuid>>,
    dimension_index: DashMap<String, Vec<Uuid>>,
    marker_counter: AtomicU64,
}

impl MarkerRegistry {
    pub fn new() -> Self {
        Self {
            markers: DashMap::new(),
            owner_index: DashMap::new(),
            dimension_index: DashMap::new(),
            marker_counter: AtomicU64::new(0),
        }
    }

    pub fn add_marker(&self, marker: MapMarker) -> Uuid {
        let id = marker.id;
        let owner = marker.owner_id;
        let dimension = marker.dimension.clone();
        
        self.markers.insert(id, marker);
        
        self.owner_index.entry(owner)
            .or_insert_with(Vec::new)
            .push(id);
        
        self.dimension_index.entry(dimension)
            .or_insert_with(Vec::new)
            .push(id);
        
        self.marker_counter.fetch_add(1, Ordering::Relaxed);
        
        id
    }

    pub fn remove_marker(&self, marker_id: Uuid) -> Option<MapMarker> {
        let marker = self.markers.remove(&marker_id)?.1;
        
        if let Some(mut markers) = self.owner_index.get_mut(&marker.owner_id) {
            markers.retain(|id| *id != marker_id);
        }
        
        if let Some(mut markers) = self.dimension_index.get_mut(&marker.dimension) {
            markers.retain(|id| *id != marker_id);
        }
        
        Some(marker)
    }

    pub fn get_marker(&self, marker_id: Uuid) -> Option<MapMarker> {
        self.markers.get(&marker_id).map(|m| m.clone())
    }

    pub fn update_marker<F>(&self, marker_id: Uuid, updater: F) -> bool
    where
        F: FnOnce(&mut MapMarker),
    {
        if let Some(mut marker) = self.markers.get_mut(&marker_id) {
            updater(&mut marker);
            marker.updated_at = Utc::now();
            true
        } else {
            false
        }
    }

    pub fn get_player_markers(&self, player_id: Uuid) -> Vec<MapMarker> {
        self.owner_index.get(&player_id)
            .map(|ids| ids.iter()
                .filter_map(|id| self.markers.get(id).map(|m| m.clone()))
                .collect())
            .unwrap_or_default()
    }

    pub fn get_visible_markers(&self, player_id: Uuid, dimension: &str) -> Vec<MapMarker> {
        self.dimension_index.get(dimension)
            .map(|ids| ids.iter()
                .filter_map(|id| self.markers.get(id))
                .filter(|m| m.visible && (m.owner_id == player_id || m.shared || m.shared_with.contains(&player_id)))
                .map(|m| m.clone())
                .collect())
            .unwrap_or_default()
    }

    pub fn get_all_markers_in_dimension(&self, dimension: &str) -> Vec<MapMarker> {
        self.dimension_index.get(dimension)
            .map(|ids| ids.iter()
                .filter_map(|id| self.markers.get(id).map(|m| m.clone()))
                .collect())
            .unwrap_or_default()
    }

    pub fn get_nearby_markers(&self, x: f64, z: f64, dimension: &str, radius: f64) -> Vec<MapMarker> {
        self.get_all_markers_in_dimension(dimension)
            .into_iter()
            .filter(|m| m.horizontal_distance_to(x, z) <= radius)
            .collect()
    }

    pub fn set_marker_visibility(&self, marker_id: Uuid, visible: bool) -> bool {
        self.update_marker(marker_id, |m| m.visible = visible)
    }

    pub fn share_marker(&self, marker_id: Uuid, share: bool) -> bool {
        self.update_marker(marker_id, |m| m.shared = share)
    }

    pub fn share_with_player(&self, marker_id: Uuid, player_id: Uuid) -> bool {
        self.update_marker(marker_id, |m| {
            if !m.shared_with.contains(&player_id) {
                m.shared_with.push(player_id);
            }
        })
    }

    pub fn unshare_with_player(&self, marker_id: Uuid, player_id: Uuid) -> bool {
        self.update_marker(marker_id, |m| {
            m.shared_with.retain(|id| *id != player_id);
        })
    }

    pub fn cleanup_expired(&self) {
        let now = Utc::now();
        let expired: Vec<_> = self.markers.iter()
            .filter(|m| m.expires_at.map(|e| e < now).unwrap_or(false))
            .map(|m| m.id)
            .collect();
        
        for id in expired {
            self.remove_marker(id);
        }
    }

    pub fn count(&self) -> usize {
        self.markers.len()
    }

    pub fn count_for_player(&self, player_id: Uuid) -> usize {
        self.owner_index.get(&player_id)
            .map(|ids| ids.len())
            .unwrap_or(0)
    }
}

impl Default for MarkerRegistry {
    fn default() -> Self {
        Self::new()
    }
}
