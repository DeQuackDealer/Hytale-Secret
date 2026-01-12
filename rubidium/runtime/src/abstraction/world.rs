use super::entities::EntityHandle;
use async_trait::async_trait;
use std::sync::Arc;

#[async_trait]
pub trait WorldHandle: Send + Sync {
    fn name(&self) -> &str;
    fn dimension(&self) -> Dimension;
    fn is_loaded(&self) -> bool;
    
    fn spawn_x(&self) -> f64;
    fn spawn_y(&self) -> f64;
    fn spawn_z(&self) -> f64;
    
    async fn get_entities(&self) -> Vec<Arc<dyn EntityHandle>>;
    async fn get_entities_in_radius(&self, x: f64, y: f64, z: f64, radius: f64) -> Vec<Arc<dyn EntityHandle>>;
    async fn get_entity_count(&self) -> usize;
    
    async fn get_block_at(&self, x: i32, y: i32, z: i32) -> Option<BlockData>;
    async fn set_block_at(&self, x: i32, y: i32, z: i32, block: BlockData) -> Result<(), String>;
    
    async fn is_chunk_loaded(&self, chunk_x: i32, chunk_z: i32) -> bool;
    async fn load_chunk(&self, chunk_x: i32, chunk_z: i32) -> Result<(), String>;
    async fn unload_chunk(&self, chunk_x: i32, chunk_z: i32) -> Result<(), String>;
    async fn get_loaded_chunk_count(&self) -> usize;
    
    fn time(&self) -> i64;
    async fn set_time(&self, time: i64);
    
    fn weather(&self) -> Weather;
    async fn set_weather(&self, weather: Weather, duration: i32);
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Dimension {
    Overworld,
    Nether,
    End,
    Custom,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Weather {
    Clear,
    Rain,
    Thunder,
}

#[derive(Debug, Clone)]
pub struct BlockData {
    pub id: String,
    pub state: std::collections::HashMap<String, String>,
}

impl BlockData {
    pub fn new(id: impl Into<String>) -> Self {
        Self {
            id: id.into(),
            state: std::collections::HashMap::new(),
        }
    }

    pub fn with_state(mut self, key: impl Into<String>, value: impl Into<String>) -> Self {
        self.state.insert(key.into(), value.into());
        self
    }

    pub fn air() -> Self {
        Self::new("air")
    }

    pub fn is_air(&self) -> bool {
        self.id == "air" || self.id == "cave_air" || self.id == "void_air"
    }

    pub fn is_solid(&self) -> bool {
        !self.is_air()
    }
}
