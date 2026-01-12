use async_trait::async_trait;
use uuid::Uuid;

#[async_trait]
pub trait EntityHandle: Send + Sync {
    fn id(&self) -> Uuid;
    fn entity_type(&self) -> &'static str;
    fn world_name(&self) -> String;
    
    fn x(&self) -> f64;
    fn y(&self) -> f64;
    fn z(&self) -> f64;
    fn yaw(&self) -> f32;
    fn pitch(&self) -> f32;
    
    fn is_valid(&self) -> bool;
    fn is_on_ground(&self) -> bool;
    
    async fn teleport(&self, x: f64, y: f64, z: f64) -> Result<(), String>;
    async fn teleport_with_rotation(&self, x: f64, y: f64, z: f64, yaw: f32, pitch: f32) -> Result<(), String>;
    async fn remove(&self) -> Result<(), String>;
    
    fn bounding_box(&self) -> BoundingBox;
    fn distance_to(&self, other: &dyn EntityHandle) -> f64 {
        let dx = self.x() - other.x();
        let dy = self.y() - other.y();
        let dz = self.z() - other.z();
        (dx * dx + dy * dy + dz * dz).sqrt()
    }
}

#[async_trait]
pub trait PlayerHandle: EntityHandle {
    fn player_name(&self) -> String;
    fn display_name(&self) -> String;
    fn is_online(&self) -> bool;
    fn ping(&self) -> i32;
    fn game_mode(&self) -> GameMode;
    
    async fn send_message(&self, message: &str);
    async fn send_title(&self, title: &str, subtitle: &str, fade_in: i32, stay: i32, fade_out: i32);
    async fn send_action_bar(&self, message: &str);
    async fn play_sound(&self, sound: &str, volume: f32, pitch: f32);
    
    async fn kick(&self, reason: &str);
    async fn set_game_mode(&self, mode: GameMode);
    
    fn has_permission(&self, permission: &str) -> bool;
    fn is_op(&self) -> bool;
    
    fn health(&self) -> f64;
    fn max_health(&self) -> f64;
    fn food_level(&self) -> i32;
}

#[derive(Debug, Clone, Copy, PartialEq)]
pub struct BoundingBox {
    pub min_x: f64,
    pub min_y: f64,
    pub min_z: f64,
    pub max_x: f64,
    pub max_y: f64,
    pub max_z: f64,
}

impl BoundingBox {
    pub fn new(min_x: f64, min_y: f64, min_z: f64, max_x: f64, max_y: f64, max_z: f64) -> Self {
        Self { min_x, min_y, min_z, max_x, max_y, max_z }
    }

    pub fn from_position(x: f64, y: f64, z: f64, width: f64, height: f64) -> Self {
        let half_width = width / 2.0;
        Self {
            min_x: x - half_width,
            min_y: y,
            min_z: z - half_width,
            max_x: x + half_width,
            max_y: y + height,
            max_z: z + half_width,
        }
    }

    pub fn player(x: f64, y: f64, z: f64) -> Self {
        Self::from_position(x, y, z, 0.6, 1.8)
    }

    pub fn intersects(&self, other: &BoundingBox) -> bool {
        self.min_x <= other.max_x && self.max_x >= other.min_x &&
        self.min_y <= other.max_y && self.max_y >= other.min_y &&
        self.min_z <= other.max_z && self.max_z >= other.min_z
    }

    pub fn contains_point(&self, x: f64, y: f64, z: f64) -> bool {
        x >= self.min_x && x <= self.max_x &&
        y >= self.min_y && y <= self.max_y &&
        z >= self.min_z && z <= self.max_z
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum GameMode {
    Survival,
    Creative,
    Adventure,
    Spectator,
}
