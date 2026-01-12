use crate::core::{
    plugins::PluginManager,
    scheduler::Scheduler,
    performance::PerformanceMonitor,
    assets::AssetRegistry,
    config::ConfigManager,
    telemetry::TelemetryCollector,
    integration::LauncherBridge,
};
use std::sync::Arc;
use tokio::sync::RwLock;
use tracing::{info, warn, error};

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ServerState {
    Stopped,
    Starting,
    Running,
    Stopping,
    Crashed,
}

pub struct Server {
    state: Arc<RwLock<ServerState>>,
    config: Arc<ConfigManager>,
    plugins: Arc<PluginManager>,
    scheduler: Arc<Scheduler>,
    performance: Arc<PerformanceMonitor>,
    assets: Arc<AssetRegistry>,
    telemetry: Arc<TelemetryCollector>,
    launcher_bridge: Arc<LauncherBridge>,
}

impl Server {
    pub async fn new(config_path: &str) -> Result<Self, String> {
        info!("Initializing Pond server from config: {}", config_path);
        
        let config = Arc::new(ConfigManager::new(config_path)?);
        let telemetry = Arc::new(TelemetryCollector::new());
        let performance = Arc::new(PerformanceMonitor::new(telemetry.clone()));
        let scheduler = Arc::new(Scheduler::new(performance.clone()));
        let assets = Arc::new(AssetRegistry::new());
        let plugins = Arc::new(PluginManager::new(config.clone()));
        let launcher_bridge = Arc::new(LauncherBridge::new(assets.clone()));
        
        Ok(Self {
            state: Arc::new(RwLock::new(ServerState::Stopped)),
            config,
            plugins,
            scheduler,
            performance,
            assets,
            telemetry,
            launcher_bridge,
        })
    }
    
    pub async fn start(&mut self) -> Result<(), String> {
        {
            let mut state = self.state.write().await;
            if *state != ServerState::Stopped {
                return Err("Server is not in stopped state".to_string());
            }
            *state = ServerState::Starting;
        }
        
        info!("Starting Pond server...");
        
        if let Err(e) = self.plugins.load_all().await {
            error!("Failed to load plugins: {}", e);
            *self.state.write().await = ServerState::Crashed;
            return Err(e);
        }
        
        self.scheduler.start().await;
        self.performance.start_monitoring().await;
        self.launcher_bridge.start().await;
        
        *self.state.write().await = ServerState::Running;
        info!("Pond server is now running");
        
        self.run_main_loop().await
    }
    
    async fn run_main_loop(&self) -> Result<(), String> {
        let mut interval = tokio::time::interval(tokio::time::Duration::from_millis(50));
        
        loop {
            interval.tick().await;
            
            let state = *self.state.read().await;
            if state == ServerState::Stopping {
                break;
            }
            
            self.scheduler.tick().await;
            self.telemetry.record_tick().await;
        }
        
        self.shutdown().await
    }
    
    pub async fn shutdown(&self) -> Result<(), String> {
        info!("Shutting down Pond server...");
        *self.state.write().await = ServerState::Stopping;
        
        self.launcher_bridge.stop().await;
        self.performance.stop_monitoring().await;
        self.scheduler.stop().await;
        self.plugins.unload_all().await;
        
        *self.state.write().await = ServerState::Stopped;
        info!("Pond server stopped");
        Ok(())
    }
    
    pub async fn reload_config(&self) -> Result<(), String> {
        warn!("Reloading configuration...");
        self.config.reload()?;
        self.plugins.reload_configs().await;
        Ok(())
    }
    
    pub async fn state(&self) -> ServerState {
        *self.state.read().await
    }
    
    pub fn plugins(&self) -> &Arc<PluginManager> {
        &self.plugins
    }
    
    pub fn scheduler(&self) -> &Arc<Scheduler> {
        &self.scheduler
    }
    
    pub fn assets(&self) -> &Arc<AssetRegistry> {
        &self.assets
    }
    
    pub fn telemetry(&self) -> &Arc<TelemetryCollector> {
        &self.telemetry
    }
}
