use super::process_manager::ProcessManager;
use super::console::ConsoleHandler;
use super::protocol::{GameEvent, GameCommand, PlayerInfo, WorldInfo};
use crate::abstraction::GameAdapter;
use crate::abstraction::entities::{EntityHandle, PlayerHandle, GameMode, BoundingBox};
use crate::abstraction::world::{WorldHandle, Dimension, Weather, BlockData};
use async_trait::async_trait;
use parking_lot::RwLock;
use std::collections::HashMap;
use std::path::PathBuf;
use std::sync::atomic::{AtomicBool, AtomicU64, Ordering};
use std::sync::Arc;
use tokio::sync::{broadcast, mpsc};
use tracing::{info, warn, error};
use uuid::Uuid;

#[derive(Debug, Clone)]
pub struct GameServerConfig {
    pub jar_path: PathBuf,
    pub working_dir: PathBuf,
    pub java_path: Option<PathBuf>,
    pub jvm_args: Vec<String>,
    pub server_args: Vec<String>,
    pub max_memory_mb: u32,
    pub min_memory_mb: u32,
    pub auto_restart: bool,
    pub restart_delay_secs: u32,
}

impl Default for GameServerConfig {
    fn default() -> Self {
        Self {
            jar_path: PathBuf::from("hytaleserver.jar"),
            working_dir: PathBuf::from("."),
            java_path: None,
            jvm_args: vec![
                "-XX:+UseG1GC".to_string(),
                "-XX:+ParallelRefProcEnabled".to_string(),
                "-XX:MaxGCPauseMillis=200".to_string(),
                "-XX:+UnlockExperimentalVMOptions".to_string(),
                "-XX:+DisableExplicitGC".to_string(),
            ],
            server_args: Vec::new(),
            max_memory_mb: 4096,
            min_memory_mb: 1024,
            auto_restart: true,
            restart_delay_secs: 10,
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ServerStatus {
    Offline,
    Starting,
    Loading,
    Running,
    Stopping,
    Crashed,
    Restarting,
}

pub struct GameServerBridge {
    config: RwLock<GameServerConfig>,
    process: Arc<ProcessManager>,
    console: Arc<ConsoleHandler>,
    status: RwLock<ServerStatus>,
    connected: AtomicBool,
    current_tick: AtomicU64,
    tps: RwLock<f64>,
    
    players: RwLock<HashMap<Uuid, Arc<ConnectedPlayer>>>,
    worlds: RwLock<HashMap<String, Arc<LoadedWorld>>>,
    
    event_tx: broadcast::Sender<GameEvent>,
    command_tx: mpsc::Sender<GameCommand>,
    
    start_time: RwLock<Option<std::time::Instant>>,
    version: RwLock<Option<String>>,
}

impl GameServerBridge {
    pub fn new(config: GameServerConfig) -> Self {
        let (event_tx, _) = broadcast::channel(10000);
        let (command_tx, command_rx) = mpsc::channel(1000);
        
        let console = Arc::new(ConsoleHandler::new());
        let process = Arc::new(ProcessManager::new(console.clone()));
        
        Self {
            config: RwLock::new(config),
            process,
            console,
            status: RwLock::new(ServerStatus::Offline),
            connected: AtomicBool::new(false),
            current_tick: AtomicU64::new(0),
            tps: RwLock::new(20.0),
            players: RwLock::new(HashMap::new()),
            worlds: RwLock::new(HashMap::new()),
            event_tx,
            command_tx,
            start_time: RwLock::new(None),
            version: RwLock::new(None),
        }
    }

    pub async fn start(&self) -> Result<(), String> {
        let config = self.config.read().clone();
        
        if !config.jar_path.exists() {
            return Err(format!("Server JAR not found: {:?}", config.jar_path));
        }
        
        info!("Starting game server from {:?}", config.jar_path);
        *self.status.write() = ServerStatus::Starting;
        *self.start_time.write() = Some(std::time::Instant::now());
        
        let java_path = config.java_path.clone()
            .unwrap_or_else(|| PathBuf::from("java"));
        
        let mut args = Vec::new();
        args.push(format!("-Xms{}M", config.min_memory_mb));
        args.push(format!("-Xmx{}M", config.max_memory_mb));
        args.extend(config.jvm_args.clone());
        args.push("-jar".to_string());
        args.push(config.jar_path.to_string_lossy().to_string());
        args.extend(config.server_args.clone());
        
        match self.process.spawn(&java_path, &args, &config.working_dir).await {
            Ok(_) => {
                info!("Game server process started");
                *self.status.write() = ServerStatus::Loading;
                self.connected.store(true, Ordering::SeqCst);
                
                self.wait_for_ready().await?;
                
                *self.status.write() = ServerStatus::Running;
                info!("Game server is now running");
                Ok(())
            }
            Err(e) => {
                error!("Failed to start game server: {}", e);
                *self.status.write() = ServerStatus::Crashed;
                Err(e)
            }
        }
    }

    async fn wait_for_ready(&self) -> Result<(), String> {
        let timeout = std::time::Duration::from_secs(120);
        let start = std::time::Instant::now();
        
        loop {
            if start.elapsed() > timeout {
                return Err("Timeout waiting for server to start".to_string());
            }
            
            if self.console.contains_pattern("Done").await {
                return Ok(());
            }
            
            if !self.process.is_running().await {
                return Err("Server process exited during startup".to_string());
            }
            
            tokio::time::sleep(std::time::Duration::from_millis(100)).await;
        }
    }

    pub async fn stop(&self) -> Result<(), String> {
        if *self.status.read() == ServerStatus::Offline {
            return Ok(());
        }
        
        info!("Stopping game server...");
        *self.status.write() = ServerStatus::Stopping;
        
        self.send_command("stop").await?;
        
        let timeout = std::time::Duration::from_secs(30);
        if !self.process.wait_for_exit(timeout).await {
            warn!("Server did not stop gracefully, forcing termination");
            self.process.kill().await?;
        }
        
        self.connected.store(false, Ordering::SeqCst);
        *self.status.write() = ServerStatus::Offline;
        self.players.write().clear();
        
        info!("Game server stopped");
        Ok(())
    }

    pub async fn send_command(&self, command: &str) -> Result<(), String> {
        if !self.connected.load(Ordering::Relaxed) {
            return Err("Not connected to game server".to_string());
        }
        
        self.console.send_input(command).await
    }

    pub fn subscribe_events(&self) -> broadcast::Receiver<GameEvent> {
        self.event_tx.subscribe()
    }

    pub fn emit_event(&self, event: GameEvent) {
        let _ = self.event_tx.send(event);
    }

    pub fn status(&self) -> ServerStatus {
        *self.status.read()
    }

    pub fn uptime(&self) -> Option<std::time::Duration> {
        self.start_time.read().map(|t| t.elapsed())
    }

    fn add_player(&self, info: PlayerInfo) -> Arc<ConnectedPlayer> {
        let player = Arc::new(ConnectedPlayer::new(info));
        self.players.write().insert(player.id, player.clone());
        player
    }

    fn remove_player(&self, id: Uuid) -> Option<Arc<ConnectedPlayer>> {
        self.players.write().remove(&id)
    }

    pub fn player_count(&self) -> usize {
        self.players.read().len()
    }

    pub fn tps(&self) -> f64 {
        *self.tps.read()
    }

    pub fn tick(&self) -> u64 {
        self.current_tick.load(Ordering::Relaxed)
    }

    pub async fn broadcast(&self, message: &str) {
        let _ = self.send_command(&format!("say {}", message)).await;
    }
}

#[async_trait]
impl GameAdapter for GameServerBridge {
    fn name(&self) -> &'static str { "HytaleServer" }
    
    fn version(&self) -> String {
        self.version.read().clone().unwrap_or_else(|| "unknown".to_string())
    }
    
    fn is_connected(&self) -> bool {
        self.connected.load(Ordering::Relaxed) && 
        *self.status.read() == ServerStatus::Running
    }

    async fn get_player(&self, id: Uuid) -> Option<Arc<dyn PlayerHandle>> {
        self.players.read().get(&id).map(|p| p.clone() as Arc<dyn PlayerHandle>)
    }

    async fn get_online_players(&self) -> Vec<Arc<dyn PlayerHandle>> {
        self.players.read().values()
            .map(|p| p.clone() as Arc<dyn PlayerHandle>)
            .collect()
    }

    async fn get_player_count(&self) -> usize {
        self.players.read().len()
    }

    async fn get_max_players(&self) -> usize {
        100
    }

    async fn get_world(&self, name: &str) -> Option<Arc<dyn WorldHandle>> {
        self.worlds.read().get(name).map(|w| w.clone() as Arc<dyn WorldHandle>)
    }

    async fn get_worlds(&self) -> Vec<Arc<dyn WorldHandle>> {
        self.worlds.read().values()
            .map(|w| w.clone() as Arc<dyn WorldHandle>)
            .collect()
    }

    async fn get_default_world(&self) -> Option<Arc<dyn WorldHandle>> {
        self.worlds.read().get("world").map(|w| w.clone() as Arc<dyn WorldHandle>)
    }

    async fn get_entity(&self, _id: Uuid) -> Option<Arc<dyn EntityHandle>> {
        None
    }

    async fn get_entities_in_world(&self, _world: &str) -> Vec<Arc<dyn EntityHandle>> {
        Vec::new()
    }

    async fn broadcast_message(&self, message: &str) {
        let _ = self.send_command(&format!("say {}", message)).await;
    }

    async fn execute_console_command(&self, command: &str) -> Result<String, String> {
        self.send_command(command).await?;
        Ok("Command sent".to_string())
    }

    async fn get_tps(&self) -> f64 {
        *self.tps.read()
    }

    async fn get_tick(&self) -> u64 {
        self.current_tick.load(Ordering::Relaxed)
    }
}

pub struct ConnectedPlayer {
    id: Uuid,
    name: RwLock<String>,
    display_name: RwLock<String>,
    x: RwLock<f64>,
    y: RwLock<f64>,
    z: RwLock<f64>,
    yaw: RwLock<f32>,
    pitch: RwLock<f32>,
    world: RwLock<String>,
    on_ground: AtomicBool,
    online: AtomicBool,
    ping: RwLock<i32>,
    game_mode: RwLock<GameMode>,
    health: RwLock<f64>,
    max_health: RwLock<f64>,
    food_level: RwLock<i32>,
    permissions: RwLock<Vec<String>>,
    is_op: AtomicBool,
    command_sender: Option<mpsc::Sender<String>>,
}

impl ConnectedPlayer {
    fn new(info: PlayerInfo) -> Self {
        Self {
            id: info.id,
            name: RwLock::new(info.name.clone()),
            display_name: RwLock::new(info.display_name.unwrap_or(info.name)),
            x: RwLock::new(info.x),
            y: RwLock::new(info.y),
            z: RwLock::new(info.z),
            yaw: RwLock::new(0.0),
            pitch: RwLock::new(0.0),
            world: RwLock::new(info.world),
            on_ground: AtomicBool::new(true),
            online: AtomicBool::new(true),
            ping: RwLock::new(0),
            game_mode: RwLock::new(GameMode::Survival),
            health: RwLock::new(20.0),
            max_health: RwLock::new(20.0),
            food_level: RwLock::new(20),
            permissions: RwLock::new(Vec::new()),
            is_op: AtomicBool::new(false),
            command_sender: None,
        }
    }

    pub fn update_position(&self, x: f64, y: f64, z: f64, yaw: f32, pitch: f32, on_ground: bool) {
        *self.x.write() = x;
        *self.y.write() = y;
        *self.z.write() = z;
        *self.yaw.write() = yaw;
        *self.pitch.write() = pitch;
        self.on_ground.store(on_ground, Ordering::Relaxed);
    }
}

#[async_trait]
impl EntityHandle for ConnectedPlayer {
    fn id(&self) -> Uuid { self.id }
    fn entity_type(&self) -> &'static str { "player" }
    fn world_name(&self) -> String { self.world.read().clone() }
    fn x(&self) -> f64 { *self.x.read() }
    fn y(&self) -> f64 { *self.y.read() }
    fn z(&self) -> f64 { *self.z.read() }
    fn yaw(&self) -> f32 { *self.yaw.read() }
    fn pitch(&self) -> f32 { *self.pitch.read() }
    fn is_valid(&self) -> bool { self.online.load(Ordering::Relaxed) }
    fn is_on_ground(&self) -> bool { self.on_ground.load(Ordering::Relaxed) }
    
    async fn teleport(&self, _x: f64, _y: f64, _z: f64) -> Result<(), String> {
        Ok(())
    }
    
    async fn teleport_with_rotation(&self, _x: f64, _y: f64, _z: f64, _yaw: f32, _pitch: f32) -> Result<(), String> {
        Ok(())
    }
    
    async fn remove(&self) -> Result<(), String> {
        Err("Cannot remove players".to_string())
    }
    
    fn bounding_box(&self) -> BoundingBox {
        BoundingBox::player(*self.x.read(), *self.y.read(), *self.z.read())
    }
}

#[async_trait]
impl PlayerHandle for ConnectedPlayer {
    fn player_name(&self) -> String { self.name.read().clone() }
    fn display_name(&self) -> String { self.display_name.read().clone() }
    fn is_online(&self) -> bool { self.online.load(Ordering::Relaxed) }
    fn ping(&self) -> i32 { *self.ping.read() }
    fn game_mode(&self) -> GameMode { *self.game_mode.read() }
    
    async fn send_message(&self, _message: &str) {}
    async fn send_title(&self, _title: &str, _subtitle: &str, _fade_in: i32, _stay: i32, _fade_out: i32) {}
    async fn send_action_bar(&self, _message: &str) {}
    async fn play_sound(&self, _sound: &str, _volume: f32, _pitch: f32) {}
    async fn kick(&self, _reason: &str) {}
    async fn set_game_mode(&self, mode: GameMode) { *self.game_mode.write() = mode; }
    
    fn has_permission(&self, permission: &str) -> bool {
        self.permissions.read().contains(&permission.to_string())
    }
    fn is_op(&self) -> bool { self.is_op.load(Ordering::Relaxed) }
    fn health(&self) -> f64 { *self.health.read() }
    fn max_health(&self) -> f64 { *self.max_health.read() }
    fn food_level(&self) -> i32 { *self.food_level.read() }
}

pub struct LoadedWorld {
    name: String,
    dimension: Dimension,
    spawn_x: f64,
    spawn_y: f64,
    spawn_z: f64,
    time: RwLock<i64>,
    weather: RwLock<Weather>,
    loaded_chunks: RwLock<usize>,
}

impl LoadedWorld {
    fn new(info: WorldInfo) -> Self {
        Self {
            name: info.name,
            dimension: Dimension::Overworld,
            spawn_x: info.spawn_x,
            spawn_y: info.spawn_y,
            spawn_z: info.spawn_z,
            time: RwLock::new(0),
            weather: RwLock::new(Weather::Clear),
            loaded_chunks: RwLock::new(0),
        }
    }
}

#[async_trait]
impl WorldHandle for LoadedWorld {
    fn name(&self) -> &str { &self.name }
    fn dimension(&self) -> Dimension { self.dimension }
    fn is_loaded(&self) -> bool { true }
    fn spawn_x(&self) -> f64 { self.spawn_x }
    fn spawn_y(&self) -> f64 { self.spawn_y }
    fn spawn_z(&self) -> f64 { self.spawn_z }
    
    async fn get_entities(&self) -> Vec<Arc<dyn EntityHandle>> { Vec::new() }
    async fn get_entities_in_radius(&self, _x: f64, _y: f64, _z: f64, _radius: f64) -> Vec<Arc<dyn EntityHandle>> { Vec::new() }
    async fn get_entity_count(&self) -> usize { 0 }
    
    async fn get_block_at(&self, _x: i32, _y: i32, _z: i32) -> Option<BlockData> { None }
    async fn set_block_at(&self, _x: i32, _y: i32, _z: i32, _block: BlockData) -> Result<(), String> { Ok(()) }
    
    async fn is_chunk_loaded(&self, _chunk_x: i32, _chunk_z: i32) -> bool { false }
    async fn load_chunk(&self, _chunk_x: i32, _chunk_z: i32) -> Result<(), String> { Ok(()) }
    async fn unload_chunk(&self, _chunk_x: i32, _chunk_z: i32) -> Result<(), String> { Ok(()) }
    async fn get_loaded_chunk_count(&self) -> usize { *self.loaded_chunks.read() }
    
    fn time(&self) -> i64 { *self.time.read() }
    async fn set_time(&self, time: i64) { *self.time.write() = time; }
    fn weather(&self) -> Weather { *self.weather.read() }
    async fn set_weather(&self, weather: Weather, _duration: i32) { *self.weather.write() = weather; }
}
