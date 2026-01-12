use serde::{Deserialize, Serialize};
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum GameEvent {
    ServerStarting,
    ServerStarted { version: String },
    ServerStopping,
    ServerStopped,
    
    PlayerJoin(PlayerInfo),
    PlayerQuit { id: Uuid, reason: String },
    PlayerMove { id: Uuid, x: f64, y: f64, z: f64, yaw: f32, pitch: f32, on_ground: bool },
    PlayerChat { id: Uuid, message: String },
    PlayerCommand { id: Uuid, command: String, args: Vec<String> },
    
    PlayerAttack { attacker_id: Uuid, target_id: Uuid, damage: f64, distance: f64 },
    PlayerDamage { id: Uuid, damage: f64, source: DamageSource },
    PlayerDeath { id: Uuid, killer_id: Option<Uuid>, message: String },
    PlayerRespawn { id: Uuid, x: f64, y: f64, z: f64, world: String },
    
    WorldLoad(WorldInfo),
    WorldUnload { name: String },
    WorldTimeChange { world: String, time: i64 },
    WorldWeatherChange { world: String, weather: String },
    
    EntitySpawn { id: Uuid, entity_type: String, x: f64, y: f64, z: f64, world: String },
    EntityRemove { id: Uuid },
    EntityMove { id: Uuid, x: f64, y: f64, z: f64 },
    
    ChunkLoad { world: String, x: i32, z: i32 },
    ChunkUnload { world: String, x: i32, z: i32 },
    
    BlockChange { world: String, x: i32, y: i32, z: i32, block_type: String },
    BlockBreak { player_id: Uuid, world: String, x: i32, y: i32, z: i32, block_type: String },
    BlockPlace { player_id: Uuid, world: String, x: i32, y: i32, z: i32, block_type: String },
    
    TickComplete { tick: u64, duration_ms: f64 },
    TpsUpdate { tps: f64 },
    
    PluginMessage { channel: String, data: Vec<u8> },
    
    Custom { event_type: String, data: String },
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum GameCommand {
    Say(String),
    Kick { player: String, reason: String },
    Ban { player: String, reason: String, duration: Option<u64> },
    Teleport { player: String, x: f64, y: f64, z: f64 },
    SetTime { world: String, time: i64 },
    SetWeather { world: String, weather: String },
    Raw(String),
    
    SendTitle { player: String, title: String, subtitle: String },
    SendActionBar { player: String, message: String },
    PlaySound { player: String, sound: String, volume: f32, pitch: f32 },
    
    SetGameMode { player: String, mode: String },
    GiveItem { player: String, item: String, count: u32 },
    
    SaveWorld { world: String },
    LoadChunk { world: String, x: i32, z: i32 },
    UnloadChunk { world: String, x: i32, z: i32 },
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PlayerInfo {
    pub id: Uuid,
    pub name: String,
    pub display_name: Option<String>,
    pub x: f64,
    pub y: f64,
    pub z: f64,
    pub world: String,
    pub ip_address: Option<String>,
    pub client_brand: Option<String>,
    pub protocol_version: Option<i32>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct WorldInfo {
    pub name: String,
    pub dimension: String,
    pub spawn_x: f64,
    pub spawn_y: f64,
    pub spawn_z: f64,
    pub seed: Option<i64>,
    pub difficulty: String,
}

#[derive(Debug, Clone, Copy, Serialize, Deserialize)]
pub enum DamageSource {
    Player,
    Entity,
    Fall,
    Fire,
    Lava,
    Drowning,
    Explosion,
    Projectile,
    Magic,
    Void,
    Starvation,
    Other,
}

impl GameEvent {
    pub fn event_name(&self) -> &'static str {
        match self {
            GameEvent::ServerStarting => "server_starting",
            GameEvent::ServerStarted { .. } => "server_started",
            GameEvent::ServerStopping => "server_stopping",
            GameEvent::ServerStopped => "server_stopped",
            GameEvent::PlayerJoin(_) => "player_join",
            GameEvent::PlayerQuit { .. } => "player_quit",
            GameEvent::PlayerMove { .. } => "player_move",
            GameEvent::PlayerChat { .. } => "player_chat",
            GameEvent::PlayerCommand { .. } => "player_command",
            GameEvent::PlayerAttack { .. } => "player_attack",
            GameEvent::PlayerDamage { .. } => "player_damage",
            GameEvent::PlayerDeath { .. } => "player_death",
            GameEvent::PlayerRespawn { .. } => "player_respawn",
            GameEvent::WorldLoad(_) => "world_load",
            GameEvent::WorldUnload { .. } => "world_unload",
            GameEvent::WorldTimeChange { .. } => "world_time_change",
            GameEvent::WorldWeatherChange { .. } => "world_weather_change",
            GameEvent::EntitySpawn { .. } => "entity_spawn",
            GameEvent::EntityRemove { .. } => "entity_remove",
            GameEvent::EntityMove { .. } => "entity_move",
            GameEvent::ChunkLoad { .. } => "chunk_load",
            GameEvent::ChunkUnload { .. } => "chunk_unload",
            GameEvent::BlockChange { .. } => "block_change",
            GameEvent::BlockBreak { .. } => "block_break",
            GameEvent::BlockPlace { .. } => "block_place",
            GameEvent::TickComplete { .. } => "tick_complete",
            GameEvent::TpsUpdate { .. } => "tps_update",
            GameEvent::PluginMessage { .. } => "plugin_message",
            GameEvent::Custom { .. } => "custom",
        }
    }
}
