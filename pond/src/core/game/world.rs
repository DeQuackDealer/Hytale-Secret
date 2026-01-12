use async_trait::async_trait;
use serde::{Deserialize, Serialize};
use uuid::Uuid;

#[derive(Debug, Clone, Copy, Hash, PartialEq, Eq, Serialize, Deserialize)]
pub struct ChunkPosition {
    pub x: i32,
    pub z: i32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ChunkData {
    pub position: ChunkPosition,
    pub blocks: Vec<u16>,
    pub height_map: Vec<u8>,
    pub biomes: Vec<u8>,
    pub entities: Vec<Uuid>,
    pub modified: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct EntityData {
    pub id: Uuid,
    pub entity_type: String,
    pub position: (f64, f64, f64),
    pub rotation: (f32, f32),
    pub velocity: (f64, f64, f64),
    pub nbt: Option<serde_json::Value>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct BlockData {
    pub block_type: u16,
    pub state: u8,
    pub nbt: Option<serde_json::Value>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum WorldError {
    ChunkNotLoaded,
    ChunkLoadFailed(String),
    SaveFailed(String),
    InvalidPosition,
    Unknown(String),
}

impl std::fmt::Display for WorldError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::ChunkNotLoaded => write!(f, "Chunk not loaded"),
            Self::ChunkLoadFailed(e) => write!(f, "Chunk load failed: {}", e),
            Self::SaveFailed(e) => write!(f, "Save failed: {}", e),
            Self::InvalidPosition => write!(f, "Invalid position"),
            Self::Unknown(e) => write!(f, "Unknown error: {}", e),
        }
    }
}

impl std::error::Error for WorldError {}

#[async_trait]
pub trait WorldProvider: Send + Sync {
    async fn load_chunk(&self, pos: ChunkPosition) -> Result<ChunkData, WorldError>;
    async fn unload_chunk(&self, pos: ChunkPosition) -> Result<(), WorldError>;
    async fn save_chunk(&self, chunk: &ChunkData) -> Result<(), WorldError>;
    async fn is_chunk_loaded(&self, pos: ChunkPosition) -> bool;
    
    async fn get_block(&self, x: i32, y: i32, z: i32) -> Result<BlockData, WorldError>;
    async fn set_block(&self, x: i32, y: i32, z: i32, block: BlockData) -> Result<(), WorldError>;
    
    async fn spawn_entity(&self, entity: EntityData) -> Result<Uuid, WorldError>;
    async fn remove_entity(&self, id: Uuid) -> Result<(), WorldError>;
    async fn get_entity(&self, id: Uuid) -> Result<EntityData, WorldError>;
    async fn update_entity(&self, entity: &EntityData) -> Result<(), WorldError>;
    
    async fn get_entities_in_chunk(&self, pos: ChunkPosition) -> Vec<EntityData>;
    async fn get_entities_in_radius(&self, center: (f64, f64, f64), radius: f64) -> Vec<EntityData>;
    
    async fn save_all(&self) -> Result<(), WorldError>;
}

pub struct StubWorldProvider {
    chunks: dashmap::DashMap<ChunkPosition, ChunkData>,
    entities: dashmap::DashMap<Uuid, EntityData>,
}

impl StubWorldProvider {
    pub fn new() -> Self {
        Self {
            chunks: dashmap::DashMap::new(),
            entities: dashmap::DashMap::new(),
        }
    }
}

impl Default for StubWorldProvider {
    fn default() -> Self {
        Self::new()
    }
}

#[async_trait]
impl WorldProvider for StubWorldProvider {
    async fn load_chunk(&self, pos: ChunkPosition) -> Result<ChunkData, WorldError> {
        if let Some(chunk) = self.chunks.get(&pos) {
            return Ok(chunk.clone());
        }
        
        let chunk = ChunkData {
            position: pos,
            blocks: vec![0; 16 * 16 * 256],
            height_map: vec![64; 256],
            biomes: vec![1; 256],
            entities: vec![],
            modified: false,
        };
        self.chunks.insert(pos, chunk.clone());
        Ok(chunk)
    }
    
    async fn unload_chunk(&self, pos: ChunkPosition) -> Result<(), WorldError> {
        self.chunks.remove(&pos);
        Ok(())
    }
    
    async fn save_chunk(&self, chunk: &ChunkData) -> Result<(), WorldError> {
        self.chunks.insert(chunk.position, chunk.clone());
        Ok(())
    }
    
    async fn is_chunk_loaded(&self, pos: ChunkPosition) -> bool {
        self.chunks.contains_key(&pos)
    }
    
    async fn get_block(&self, x: i32, y: i32, z: i32) -> Result<BlockData, WorldError> {
        let chunk_pos = ChunkPosition { x: x >> 4, z: z >> 4 };
        if !self.chunks.contains_key(&chunk_pos) {
            return Err(WorldError::ChunkNotLoaded);
        }
        
        Ok(BlockData {
            block_type: 0,
            state: 0,
            nbt: None,
        })
    }
    
    async fn set_block(&self, x: i32, y: i32, z: i32, _block: BlockData) -> Result<(), WorldError> {
        let chunk_pos = ChunkPosition { x: x >> 4, z: z >> 4 };
        if !self.chunks.contains_key(&chunk_pos) {
            return Err(WorldError::ChunkNotLoaded);
        }
        
        if let Some(mut chunk) = self.chunks.get_mut(&chunk_pos) {
            chunk.modified = true;
        }
        
        let _ = y;
        Ok(())
    }
    
    async fn spawn_entity(&self, entity: EntityData) -> Result<Uuid, WorldError> {
        let id = entity.id;
        self.entities.insert(id, entity);
        Ok(id)
    }
    
    async fn remove_entity(&self, id: Uuid) -> Result<(), WorldError> {
        self.entities.remove(&id);
        Ok(())
    }
    
    async fn get_entity(&self, id: Uuid) -> Result<EntityData, WorldError> {
        self.entities.get(&id)
            .map(|e| e.clone())
            .ok_or(WorldError::Unknown("Entity not found".to_string()))
    }
    
    async fn update_entity(&self, entity: &EntityData) -> Result<(), WorldError> {
        self.entities.insert(entity.id, entity.clone());
        Ok(())
    }
    
    async fn get_entities_in_chunk(&self, pos: ChunkPosition) -> Vec<EntityData> {
        self.entities.iter()
            .filter(|e| {
                let (x, _, z) = e.position;
                ChunkPosition { x: (x as i32) >> 4, z: (z as i32) >> 4 } == pos
            })
            .map(|e| e.clone())
            .collect()
    }
    
    async fn get_entities_in_radius(&self, center: (f64, f64, f64), radius: f64) -> Vec<EntityData> {
        let r2 = radius * radius;
        self.entities.iter()
            .filter(|e| {
                let (x, y, z) = e.position;
                let dx = x - center.0;
                let dy = y - center.1;
                let dz = z - center.2;
                dx*dx + dy*dy + dz*dz <= r2
            })
            .map(|e| e.clone())
            .collect()
    }
    
    async fn save_all(&self) -> Result<(), WorldError> {
        Ok(())
    }
}
