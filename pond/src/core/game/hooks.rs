use async_trait::async_trait;
use serde::{Deserialize, Serialize};
use uuid::Uuid;

#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Serialize, Deserialize)]
pub enum HookPriority {
    Lowest = 0,
    Low = 1,
    Normal = 2,
    High = 3,
    Highest = 4,
    Monitor = 5,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum HookResult {
    Continue,
    Cancel,
    Modify(serde_json::Value),
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum GameHookEvent {
    PlayerPreJoin { player_id: Uuid, name: String, address: String },
    PlayerJoin { player_id: Uuid, name: String },
    PlayerLeave { player_id: Uuid, reason: String },
    PlayerChat { player_id: Uuid, message: String },
    PlayerCommand { player_id: Uuid, command: String },
    PlayerMove { player_id: Uuid, from: Position, to: Position },
    PlayerInteract { player_id: Uuid, target: InteractTarget },
    BlockPlace { player_id: Uuid, position: Position, block_type: String },
    BlockBreak { player_id: Uuid, position: Position },
    EntitySpawn { entity_id: Uuid, entity_type: String, position: Position },
    EntityDamage { entity_id: Uuid, damage: f32, source: DamageSource },
    WorldSave,
    ServerTick { tick: u64 },
    Custom { event_type: String, data: serde_json::Value },
}

#[derive(Debug, Clone, Copy, Serialize, Deserialize)]
pub struct Position {
    pub x: f64,
    pub y: f64,
    pub z: f64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum InteractTarget {
    Block(Position),
    Entity(Uuid),
    Item(String),
    Air,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum DamageSource {
    Player(Uuid),
    Entity(Uuid),
    Environment(String),
    Unknown,
}

#[async_trait]
pub trait GameHook: Send + Sync {
    fn name(&self) -> &str;
    fn priority(&self) -> HookPriority;
    fn handles(&self, event: &GameHookEvent) -> bool;
    async fn execute(&self, event: &GameHookEvent) -> HookResult;
}

pub struct HookRegistry {
    hooks: dashmap::DashMap<String, Vec<HookEntry>>,
}

struct HookEntry {
    priority: HookPriority,
    hook: std::sync::Arc<dyn GameHook>,
}

impl HookRegistry {
    pub fn new() -> Self {
        Self {
            hooks: dashmap::DashMap::new(),
        }
    }
    
    pub fn register(&self, event_type: &str, hook: std::sync::Arc<dyn GameHook>) {
        let entry = HookEntry {
            priority: hook.priority(),
            hook,
        };
        
        let mut hooks = self.hooks.entry(event_type.to_string()).or_insert_with(Vec::new);
        hooks.push(entry);
        hooks.sort_by_key(|e| e.priority);
    }
    
    pub fn unregister(&self, event_type: &str, hook_name: &str) {
        if let Some(mut hooks) = self.hooks.get_mut(event_type) {
            hooks.retain(|e| e.hook.name() != hook_name);
        }
    }
    
    pub async fn dispatch(&self, event: &GameHookEvent) -> HookResult {
        let event_type = event_type_name(event);
        
        if let Some(hooks) = self.hooks.get(event_type) {
            for entry in hooks.iter() {
                if entry.hook.handles(event) {
                    let result = entry.hook.execute(event).await;
                    if matches!(result, HookResult::Cancel) {
                        return result;
                    }
                    if let HookResult::Modify(_) = &result {
                        return result;
                    }
                }
            }
        }
        
        if let Some(hooks) = self.hooks.get("*") {
            for entry in hooks.iter() {
                if entry.hook.handles(event) {
                    let result = entry.hook.execute(event).await;
                    if matches!(result, HookResult::Cancel) {
                        return result;
                    }
                }
            }
        }
        
        HookResult::Continue
    }
}

impl Default for HookRegistry {
    fn default() -> Self {
        Self::new()
    }
}

fn event_type_name(event: &GameHookEvent) -> &'static str {
    match event {
        GameHookEvent::PlayerPreJoin { .. } => "player_pre_join",
        GameHookEvent::PlayerJoin { .. } => "player_join",
        GameHookEvent::PlayerLeave { .. } => "player_leave",
        GameHookEvent::PlayerChat { .. } => "player_chat",
        GameHookEvent::PlayerCommand { .. } => "player_command",
        GameHookEvent::PlayerMove { .. } => "player_move",
        GameHookEvent::PlayerInteract { .. } => "player_interact",
        GameHookEvent::BlockPlace { .. } => "block_place",
        GameHookEvent::BlockBreak { .. } => "block_break",
        GameHookEvent::EntitySpawn { .. } => "entity_spawn",
        GameHookEvent::EntityDamage { .. } => "entity_damage",
        GameHookEvent::WorldSave => "world_save",
        GameHookEvent::ServerTick { .. } => "server_tick",
        GameHookEvent::Custom { .. } => "custom",
    }
}
