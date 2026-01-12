use super::entities::{EntityHandle, PlayerHandle};
use super::world::WorldHandle;
use async_trait::async_trait;
use uuid::Uuid;
use std::sync::Arc;

#[async_trait]
pub trait GameAdapter: Send + Sync {
    fn name(&self) -> &'static str;
    fn version(&self) -> String;
    fn is_connected(&self) -> bool;

    async fn get_player(&self, id: Uuid) -> Option<Arc<dyn PlayerHandle>>;
    async fn get_online_players(&self) -> Vec<Arc<dyn PlayerHandle>>;
    async fn get_player_count(&self) -> usize;
    async fn get_max_players(&self) -> usize;

    async fn get_world(&self, name: &str) -> Option<Arc<dyn WorldHandle>>;
    async fn get_worlds(&self) -> Vec<Arc<dyn WorldHandle>>;
    async fn get_default_world(&self) -> Option<Arc<dyn WorldHandle>>;

    async fn get_entity(&self, id: Uuid) -> Option<Arc<dyn EntityHandle>>;
    async fn get_entities_in_world(&self, world: &str) -> Vec<Arc<dyn EntityHandle>>;

    async fn broadcast_message(&self, message: &str);
    async fn execute_console_command(&self, command: &str) -> Result<String, String>;

    async fn get_tps(&self) -> f64;
    async fn get_tick(&self) -> u64;
}

pub struct StubGameAdapter {
    name: &'static str,
}

impl StubGameAdapter {
    pub fn new() -> Self {
        Self { name: "Stub" }
    }
}

impl Default for StubGameAdapter {
    fn default() -> Self {
        Self::new()
    }
}

#[async_trait]
impl GameAdapter for StubGameAdapter {
    fn name(&self) -> &'static str { self.name }
    fn version(&self) -> String { "1.0.0".to_string() }
    fn is_connected(&self) -> bool { false }

    async fn get_player(&self, _id: Uuid) -> Option<Arc<dyn PlayerHandle>> { None }
    async fn get_online_players(&self) -> Vec<Arc<dyn PlayerHandle>> { Vec::new() }
    async fn get_player_count(&self) -> usize { 0 }
    async fn get_max_players(&self) -> usize { 100 }

    async fn get_world(&self, _name: &str) -> Option<Arc<dyn WorldHandle>> { None }
    async fn get_worlds(&self) -> Vec<Arc<dyn WorldHandle>> { Vec::new() }
    async fn get_default_world(&self) -> Option<Arc<dyn WorldHandle>> { None }

    async fn get_entity(&self, _id: Uuid) -> Option<Arc<dyn EntityHandle>> { None }
    async fn get_entities_in_world(&self, _world: &str) -> Vec<Arc<dyn EntityHandle>> { Vec::new() }

    async fn broadcast_message(&self, _message: &str) {}
    async fn execute_console_command(&self, _command: &str) -> Result<String, String> {
        Err("Stub adapter - no game connected".to_string())
    }

    async fn get_tps(&self) -> f64 { 20.0 }
    async fn get_tick(&self) -> u64 { 0 }
}
