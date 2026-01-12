use serde::{Deserialize, Serialize};
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MovementSnapshot {
    pub x: f64,
    pub y: f64,
    pub z: f64,
    pub yaw: f32,
    pub pitch: f32,
    pub on_ground: bool,
    pub in_water: bool,
    pub is_gliding: bool,
    pub is_teleporting: bool,
    pub took_fall_damage: bool,
    pub timestamp: u64,
    pub world: String,
}

impl MovementSnapshot {
    pub fn new(x: f64, y: f64, z: f64, yaw: f32, pitch: f32) -> Self {
        Self {
            x,
            y,
            z,
            yaw,
            pitch,
            on_ground: true,
            in_water: false,
            is_gliding: false,
            is_teleporting: false,
            took_fall_damage: false,
            timestamp: std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .unwrap()
                .as_millis() as u64,
            world: "world".to_string(),
        }
    }

    pub fn distance_to(&self, other: &MovementSnapshot) -> f64 {
        let dx = self.x - other.x;
        let dy = self.y - other.y;
        let dz = self.z - other.z;
        (dx * dx + dy * dy + dz * dz).sqrt()
    }

    pub fn horizontal_distance_to(&self, other: &MovementSnapshot) -> f64 {
        let dx = self.x - other.x;
        let dz = self.z - other.z;
        (dx * dx + dz * dz).sqrt()
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CombatSnapshot {
    pub is_attack: bool,
    pub target_id: Option<Uuid>,
    pub distance_to_target: Option<f64>,
    pub angle_to_target: Option<f64>,
    pub damage_dealt: f64,
    pub weapon_type: Option<String>,
    pub was_critical: bool,
    pub timestamp: u64,
}

impl CombatSnapshot {
    pub fn attack(target_id: Uuid, distance: f64, angle: f64) -> Self {
        Self {
            is_attack: true,
            target_id: Some(target_id),
            distance_to_target: Some(distance),
            angle_to_target: Some(angle),
            damage_dealt: 0.0,
            weapon_type: None,
            was_critical: false,
            timestamp: std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .unwrap()
                .as_millis() as u64,
        }
    }

    pub fn miss() -> Self {
        Self {
            is_attack: true,
            target_id: None,
            distance_to_target: None,
            angle_to_target: None,
            damage_dealt: 0.0,
            weapon_type: None,
            was_critical: false,
            timestamp: std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .unwrap()
                .as_millis() as u64,
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum PacketType {
    Position,
    PositionLook,
    Look,
    Flying,
    KeepAlive,
    Chat,
    UseItem,
    Attack,
    Interact,
    Custom,
    Unknown,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PacketSnapshot {
    pub packet_type: PacketType,
    pub size_bytes: u32,
    pub is_malformed: bool,
    pub is_duplicate: bool,
    pub timestamp: u64,
}

impl PacketSnapshot {
    pub fn new(packet_type: PacketType, size_bytes: u32) -> Self {
        Self {
            packet_type,
            size_bytes,
            is_malformed: false,
            is_duplicate: false,
            timestamp: std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .unwrap()
                .as_millis() as u64,
        }
    }

    pub fn malformed(packet_type: PacketType) -> Self {
        Self {
            packet_type,
            size_bytes: 0,
            is_malformed: true,
            is_duplicate: false,
            timestamp: std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .unwrap()
                .as_millis() as u64,
        }
    }
}
