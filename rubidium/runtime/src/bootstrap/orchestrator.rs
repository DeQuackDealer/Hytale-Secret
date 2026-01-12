use super::phases::BootstrapPhase;
use super::diagnostics::{StartupReport, DiagnosticResult};
use crate::bridge::{GameServerBridge, GameServerConfig};
use crate::anticheat::AnticheatService;
use crate::core::config::ConfigManager;
use crate::core::plugins::PluginManager;
use crate::core::scheduler::Scheduler;
use crate::core::performance::PerformanceMonitor;
use crate::core::telemetry::TelemetryCollector;
use crate::events::EventBus;
use crate::features::{AdaptiveScheduler, WorldHeatmap, SessionManager};
use parking_lot::RwLock;
use std::future::Future;
use std::path::PathBuf;
use std::pin::Pin;
use std::sync::Arc;
use std::time::{Duration, Instant};
use tracing::{info, warn, error, debug};

type PhaseFuture<'a> = Pin<Box<dyn Future<Output = Result<(), String>> + 'a>>;

pub struct BootstrapOrchestrator {
    config_path: PathBuf,
    server_jar: PathBuf,
    
    config: Option<Arc<ConfigManager>>,
    game_server: Option<Arc<GameServerBridge>>,
    anticheat: Option<Arc<AnticheatService>>,
    plugins: Option<Arc<PluginManager>>,
    scheduler: Option<Arc<Scheduler>>,
    performance: Option<Arc<PerformanceMonitor>>,
    telemetry: Option<Arc<TelemetryCollector>>,
    event_bus: Option<Arc<EventBus>>,
    adaptive_scheduler: Option<Arc<AdaptiveScheduler>>,
    world_heatmap: Option<Arc<WorldHeatmap>>,
    session_manager: Option<Arc<SessionManager>>,
    
    current_phase: RwLock<BootstrapPhase>,
    start_time: Option<Instant>,
    report: RwLock<StartupReport>,
}

impl BootstrapOrchestrator {
    pub fn new(config_path: impl Into<PathBuf>, server_jar: impl Into<PathBuf>) -> Self {
        Self {
            config_path: config_path.into(),
            server_jar: server_jar.into(),
            config: None,
            game_server: None,
            anticheat: None,
            plugins: None,
            scheduler: None,
            performance: None,
            telemetry: None,
            event_bus: None,
            adaptive_scheduler: None,
            world_heatmap: None,
            session_manager: None,
            current_phase: RwLock::new(BootstrapPhase::Initializing),
            start_time: None,
            report: RwLock::new(StartupReport::new()),
        }
    }

    pub async fn bootstrap(&mut self) -> Result<(), String> {
        self.start_time = Some(Instant::now());
        info!("=== Rubidium Server Bootstrap ===");
        
        self.run_phase(BootstrapPhase::Configuration, |this| Box::pin(this.phase_configuration())).await?;
        self.run_phase(BootstrapPhase::Verification, |this| Box::pin(this.phase_verification())).await?;
        self.run_phase(BootstrapPhase::CoreServices, |this| Box::pin(this.phase_core_services())).await?;
        self.run_phase(BootstrapPhase::GameServer, |this| Box::pin(this.phase_game_server())).await?;
        self.run_phase(BootstrapPhase::EventSubscriptions, |this| Box::pin(this.phase_event_subscriptions())).await?;
        self.run_phase(BootstrapPhase::Plugins, |this| Box::pin(this.phase_plugins())).await?;
        self.run_phase(BootstrapPhase::Anticheat, |this| Box::pin(this.phase_anticheat())).await?;
        self.run_phase(BootstrapPhase::Ready, |this| Box::pin(this.phase_ready())).await?;
        
        let elapsed = self.start_time.unwrap().elapsed();
        self.report.write().total_time = elapsed;
        
        info!("=== Bootstrap Complete in {:.2}s ===", elapsed.as_secs_f64());
        self.print_report();
        
        Ok(())
    }

    async fn run_phase<F>(&mut self, phase: BootstrapPhase, f: F) -> Result<(), String>
    where
        F: for<'a> FnOnce(&'a mut Self) -> PhaseFuture<'a>,
    {
        let phase_start = Instant::now();
        *self.current_phase.write() = phase;
        info!("[{:?}] Starting...", phase);
        
        let result = f(self).await;
        let elapsed = phase_start.elapsed();
        
        let diagnostic = match &result {
            Ok(_) => {
                info!("[{:?}] Complete ({:.2}ms)", phase, elapsed.as_secs_f64() * 1000.0);
                DiagnosticResult::success(format!("{:?}", phase), elapsed)
            }
            Err(e) => {
                error!("[{:?}] Failed: {}", phase, e);
                DiagnosticResult::failure(format!("{:?}", phase), e.clone(), elapsed)
            }
        };
        
        self.report.write().phases.push(diagnostic);
        result
    }

    async fn phase_configuration(&mut self) -> Result<(), String> {
        debug!("Loading configuration from {:?}", self.config_path);
        
        if !self.config_path.exists() {
            return Err(format!("Config file not found: {:?}", self.config_path));
        }
        
        let config = Arc::new(ConfigManager::new(
            self.config_path.to_string_lossy().as_ref()
        )?);
        
        self.report.write().add_info("Configuration loaded successfully");
        self.config = Some(config);
        Ok(())
    }

    async fn phase_verification(&mut self) -> Result<(), String> {
        debug!("Verifying server JAR: {:?}", self.server_jar);
        
        if !self.server_jar.exists() {
            return Err(format!("Server JAR not found: {:?}", self.server_jar));
        }
        
        let metadata = std::fs::metadata(&self.server_jar)
            .map_err(|e| format!("Cannot read JAR file: {}", e))?;
        
        let size_mb = metadata.len() as f64 / (1024.0 * 1024.0);
        self.report.write().add_info(format!("Server JAR: {:.2} MB", size_mb));
        
        if let Ok(output) = std::process::Command::new("java")
            .arg("-version")
            .output()
        {
            let version = String::from_utf8_lossy(&output.stderr);
            if let Some(line) = version.lines().next() {
                self.report.write().add_info(format!("Java: {}", line));
            }
        } else {
            self.report.write().add_warning("Java version check failed");
        }
        
        Ok(())
    }

    async fn phase_core_services(&mut self) -> Result<(), String> {
        debug!("Initializing core services");
        
        let telemetry = Arc::new(TelemetryCollector::new());
        let performance = Arc::new(PerformanceMonitor::new(telemetry.clone()));
        let scheduler = Arc::new(Scheduler::new(performance.clone()));
        let event_bus = Arc::new(EventBus::new());
        
        let adaptive_scheduler = Arc::new(AdaptiveScheduler::new(50.0));
        let world_heatmap = Arc::new(WorldHeatmap::new(256));
        let session_manager = Arc::new(SessionManager::new(Duration::from_secs(3600)));
        
        self.telemetry = Some(telemetry);
        self.performance = Some(performance);
        self.scheduler = Some(scheduler);
        self.event_bus = Some(event_bus);
        self.adaptive_scheduler = Some(adaptive_scheduler);
        self.world_heatmap = Some(world_heatmap);
        self.session_manager = Some(session_manager);
        
        self.report.write().add_info("Core services initialized");
        Ok(())
    }

    async fn phase_game_server(&mut self) -> Result<(), String> {
        debug!("Starting game server");
        
        let game_config = GameServerConfig {
            jar_path: self.server_jar.clone(),
            working_dir: self.server_jar.parent()
                .unwrap_or(&PathBuf::from("."))
                .to_path_buf(),
            ..Default::default()
        };
        
        let game_server = Arc::new(GameServerBridge::new(game_config));
        game_server.start().await?;
        
        self.game_server = Some(game_server);
        self.report.write().add_info("Game server started");
        Ok(())
    }

    async fn phase_event_subscriptions(&mut self) -> Result<(), String> {
        debug!("Setting up event subscriptions");
        
        let event_bus = self.event_bus.as_ref().unwrap();
        let game_server = self.game_server.as_ref().unwrap();
        
        let mut receiver = game_server.subscribe_events();
        let event_bus_clone = event_bus.clone();
        let session_manager = self.session_manager.as_ref().unwrap().clone();
        let world_heatmap = self.world_heatmap.as_ref().unwrap().clone();
        
        tokio::spawn(async move {
            while let Ok(event) = receiver.recv().await {
                event_bus_clone.emit(event.clone()).await;
                
                match &event {
                    crate::bridge::GameEvent::PlayerJoin(info) => {
                        session_manager.create_session(info.id, info.name.clone());
                    }
                    crate::bridge::GameEvent::PlayerQuit { id, .. } => {
                        session_manager.remove_session(*id);
                    }
                    crate::bridge::GameEvent::PlayerMove { x, z, .. } => {
                        world_heatmap.record_player_position(*x, *z, "world");
                    }
                    crate::bridge::GameEvent::BlockChange { x, z, world, .. } |
                    crate::bridge::GameEvent::BlockBreak { x, z, world, .. } |
                    crate::bridge::GameEvent::BlockPlace { x, z, world, .. } => {
                        world_heatmap.record_block_change(*x as f64, *z as f64, world);
                    }
                    _ => {}
                }
            }
        });
        
        self.report.write().add_info("Event subscriptions configured");
        Ok(())
    }

    async fn phase_plugins(&mut self) -> Result<(), String> {
        debug!("Loading plugins");
        
        let config = self.config.as_ref().unwrap();
        let plugins = Arc::new(PluginManager::new(config.clone()));
        
        if let Err(e) = plugins.load_all().await {
            self.report.write().add_warning(format!("Plugin loading: {}", e));
        }
        
        let count = plugins.count();
        self.plugins = Some(plugins);
        self.report.write().add_info(format!("{} plugins loaded", count));
        Ok(())
    }

    async fn phase_anticheat(&mut self) -> Result<(), String> {
        debug!("Initializing anticheat");
        
        let anticheat_config = crate::anticheat::AnticheatConfig::default();
        let anticheat = Arc::new(AnticheatService::new(anticheat_config));
        
        let event_bus = self.event_bus.as_ref().unwrap();
        let anticheat_clone = anticheat.clone();
        let mut receiver = event_bus.subscribe();
        
        tokio::spawn(async move {
            while let Ok(event) = receiver.recv().await {
                match event {
                    crate::bridge::GameEvent::PlayerMove { id, x, y, z, yaw, pitch, on_ground } => {
                        let snapshot = crate::abstraction::MovementSnapshot::new(x, y, z, yaw, pitch);
                        anticheat_clone.process_movement(id, snapshot);
                    }
                    crate::bridge::GameEvent::PlayerAttack { attacker_id, target_id, damage, distance } => {
                        let snapshot = crate::abstraction::CombatSnapshot::attack(target_id, distance, 0.0);
                        anticheat_clone.process_combat(attacker_id, snapshot);
                    }
                    _ => {}
                }
            }
        });
        
        self.anticheat = Some(anticheat);
        self.report.write().add_info("Anticheat initialized with 25% sampling");
        Ok(())
    }

    async fn phase_ready(&mut self) -> Result<(), String> {
        debug!("Finalizing startup");
        
        self.scheduler.as_ref().unwrap().start().await;
        self.performance.as_ref().unwrap().start_monitoring().await;
        
        let player_count = self.game_server.as_ref().unwrap().player_count();
        self.report.write().add_info(format!("Server ready with {} players", player_count));
        
        Ok(())
    }

    fn print_report(&self) {
        let report = self.report.read();
        
        info!("");
        info!("┌─────────────────────────────────────────┐");
        info!("│         RUBIDIUM STARTUP REPORT         │");
        info!("├─────────────────────────────────────────┤");
        
        for phase in &report.phases {
            let status = if phase.success { "✓" } else { "✗" };
            let time = phase.duration.as_millis();
            info!("│ {} {:25} {:8}ms │", status, phase.name, time);
        }
        
        info!("├─────────────────────────────────────────┤");
        info!("│ Total startup time: {:18.2}s │", report.total_time.as_secs_f64());
        info!("└─────────────────────────────────────────┘");
        
        if !report.warnings.is_empty() {
            info!("");
            info!("Warnings:");
            for warning in &report.warnings {
                warn!("  - {}", warning);
            }
        }
        
        info!("");
        for info_line in &report.info {
            info!("  {}", info_line);
        }
    }

    pub fn game_server(&self) -> Option<&Arc<GameServerBridge>> {
        self.game_server.as_ref()
    }

    pub fn anticheat(&self) -> Option<&Arc<AnticheatService>> {
        self.anticheat.as_ref()
    }

    pub fn event_bus(&self) -> Option<&Arc<EventBus>> {
        self.event_bus.as_ref()
    }

    pub fn session_manager(&self) -> Option<&Arc<SessionManager>> {
        self.session_manager.as_ref()
    }
}
