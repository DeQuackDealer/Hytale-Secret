use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum WaypointVisibility {
    Visible,
    Hidden,
    FadeWithDistance,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum WaypointIcon {
    Default,
    Star,
    Diamond,
    Circle,
    Square,
    Triangle,
    Heart,
    Skull,
    Home,
    Bed,
    Flag,
    Pin,
    Cross,
    Chest,
    Portal,
    Cave,
    Mountain,
    Water,
    Village,
    Custom(u32),
}

impl Default for WaypointIcon {
    fn default() -> Self {
        Self::Default
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum WaypointType {
    Custom,
    Death,
    Spawn,
    Home,
    Portal,
    Discovery,
    Shared,
    Global,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Waypoint {
    pub id: Uuid,
    pub owner_id: Uuid,
    pub name: String,
    pub description: Option<String>,
    pub x: f64,
    pub y: f64,
    pub z: f64,
    pub dimension: String,
    pub waypoint_type: WaypointType,
    pub icon: WaypointIcon,
    pub color: u32,
    pub visibility: WaypointVisibility,
    pub beam_enabled: bool,
    pub beam_color: Option<u32>,
    pub sound_enabled: bool,
    pub proximity_radius: Option<f64>,
    pub group: Option<String>,
    pub sort_order: i32,
    pub temporary: bool,
    pub expires_at: Option<DateTime<Utc>>,
    pub shared: bool,
    pub shared_with: Vec<Uuid>,
    pub global: bool,
    pub created_at: DateTime<Utc>,
    pub updated_at: DateTime<Utc>,
    pub last_visited: Option<DateTime<Utc>>,
    pub visit_count: u32,
    pub tags: Vec<String>,
}

impl Waypoint {
    pub fn new(owner_id: Uuid, name: String, x: f64, y: f64, z: f64, dimension: String) -> Self {
        let now = Utc::now();
        Self {
            id: Uuid::new_v4(),
            owner_id,
            name,
            description: None,
            x, y, z,
            dimension,
            waypoint_type: WaypointType::Custom,
            icon: WaypointIcon::Default,
            color: 0xFFFFFF,
            visibility: WaypointVisibility::Visible,
            beam_enabled: false,
            beam_color: None,
            sound_enabled: false,
            proximity_radius: None,
            group: None,
            sort_order: 0,
            temporary: false,
            expires_at: None,
            shared: false,
            shared_with: Vec::new(),
            global: false,
            created_at: now,
            updated_at: now,
            last_visited: None,
            visit_count: 0,
            tags: Vec::new(),
        }
    }

    pub fn death(owner_id: Uuid, x: f64, y: f64, z: f64, dimension: String) -> Self {
        let mut wp = Self::new(owner_id, "Death Point".to_string(), x, y, z, dimension);
        wp.waypoint_type = WaypointType::Death;
        wp.icon = WaypointIcon::Skull;
        wp.color = 0xFF0000;
        wp.beam_enabled = true;
        wp.beam_color = Some(0xFF0000);
        wp.temporary = true;
        wp
    }

    pub fn spawn(owner_id: Uuid, x: f64, y: f64, z: f64, dimension: String) -> Self {
        let mut wp = Self::new(owner_id, "Spawn Point".to_string(), x, y, z, dimension);
        wp.waypoint_type = WaypointType::Spawn;
        wp.icon = WaypointIcon::Bed;
        wp.color = 0x00FF00;
        wp
    }

    pub fn home(owner_id: Uuid, x: f64, y: f64, z: f64, dimension: String) -> Self {
        let mut wp = Self::new(owner_id, "Home".to_string(), x, y, z, dimension);
        wp.waypoint_type = WaypointType::Home;
        wp.icon = WaypointIcon::Home;
        wp.color = 0x00FFFF;
        wp.beam_enabled = true;
        wp
    }

    pub fn portal(owner_id: Uuid, name: String, x: f64, y: f64, z: f64, dimension: String) -> Self {
        let mut wp = Self::new(owner_id, name, x, y, z, dimension);
        wp.waypoint_type = WaypointType::Portal;
        wp.icon = WaypointIcon::Portal;
        wp.color = 0x9932CC;
        wp
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

    pub fn direction_from(&self, x: f64, z: f64) -> f64 {
        let dx = self.x - x;
        let dz = self.z - z;
        (dz.atan2(dx).to_degrees() + 360.0) % 360.0
    }

    pub fn is_visible_to(&self, player_id: Uuid) -> bool {
        if self.visibility == WaypointVisibility::Hidden {
            return false;
        }
        
        if self.global {
            return true;
        }
        
        if self.owner_id == player_id {
            return true;
        }
        
        if self.shared {
            return true;
        }
        
        self.shared_with.contains(&player_id)
    }

    pub fn record_visit(&mut self) {
        self.last_visited = Some(Utc::now());
        self.visit_count += 1;
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct WaypointGroup {
    pub name: String,
    pub color: u32,
    pub collapsed: bool,
    pub visible: bool,
}

impl WaypointGroup {
    pub fn new(name: String, color: u32) -> Self {
        Self {
            name,
            color,
            collapsed: false,
            visible: true,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct WaypointHudInfo {
    pub waypoint: Waypoint,
    pub distance: f64,
    pub direction: f64,
    pub screen_x: f32,
    pub screen_y: f32,
    pub on_screen: bool,
    pub alpha: f32,
}
