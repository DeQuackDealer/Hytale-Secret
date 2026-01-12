pub mod cpu_affinity;
pub mod process_priority;
pub mod memory;
pub mod background_suppressor;
pub mod hardware_presets;
pub mod frame_monitor;
pub mod asset_cache;
pub mod world_hosting;
pub mod save_snapshot;
pub mod mod_resolver;

use serde::{Deserialize, Serialize};
use std::sync::Arc;
use parking_lot::RwLock;

pub use cpu_affinity::CpuAffinityManager;
pub use process_priority::ProcessPriorityController;
pub use memory::MemoryOptimizer;
pub use background_suppressor::BackgroundSuppressor;
pub use hardware_presets::{HardwarePreset, HardwarePresetManager};
pub use frame_monitor::FrameMonitor;
pub use asset_cache::AssetCache;
pub use world_hosting::LocalWorldHost;
pub use save_snapshot::SaveSnapshotManager;
pub use mod_resolver::ModDependencyResolver;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct OptimizationConfig {
    pub cpu_affinity_enabled: bool,
    pub performance_cores_only: bool,
    pub priority_boost_enabled: bool,
    pub priority_level: PriorityLevel,
    pub memory_optimization_enabled: bool,
    pub target_memory_mb: u32,
    pub background_suppression_enabled: bool,
    pub frame_monitoring_enabled: bool,
    pub target_framerate: Option<u32>,
    pub asset_caching_enabled: bool,
    pub cache_size_mb: u32,
    pub shader_prewarming_enabled: bool,
    pub active_preset: Option<String>,
}

impl Default for OptimizationConfig {
    fn default() -> Self {
        Self {
            cpu_affinity_enabled: true,
            performance_cores_only: true,
            priority_boost_enabled: true,
            priority_level: PriorityLevel::High,
            memory_optimization_enabled: true,
            target_memory_mb: 4096,
            background_suppression_enabled: false,
            frame_monitoring_enabled: true,
            target_framerate: None,
            asset_caching_enabled: true,
            cache_size_mb: 2048,
            shader_prewarming_enabled: true,
            active_preset: None,
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum PriorityLevel {
    Normal,
    AboveNormal,
    High,
    Realtime,
}

impl PriorityLevel {
    pub fn as_str(&self) -> &'static str {
        match self {
            Self::Normal => "normal",
            Self::AboveNormal => "above_normal",
            Self::High => "high",
            Self::Realtime => "realtime",
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SystemCapabilities {
    pub cpu_cores: usize,
    pub performance_cores: usize,
    pub efficiency_cores: usize,
    pub total_memory_mb: u64,
    pub available_memory_mb: u64,
    pub gpu_name: Option<String>,
    pub gpu_memory_mb: Option<u64>,
    pub has_ssd: bool,
    pub os_version: String,
}

pub struct OptimizationService {
    config: Arc<RwLock<OptimizationConfig>>,
    capabilities: Arc<RwLock<Option<SystemCapabilities>>>,
    cpu_affinity: CpuAffinityManager,
    priority_controller: ProcessPriorityController,
    memory_optimizer: MemoryOptimizer,
    background_suppressor: BackgroundSuppressor,
    preset_manager: HardwarePresetManager,
    frame_monitor: FrameMonitor,
    asset_cache: AssetCache,
    world_host: LocalWorldHost,
    snapshot_manager: SaveSnapshotManager,
    mod_resolver: ModDependencyResolver,
}

impl OptimizationService {
    pub fn new() -> Self {
        Self {
            config: Arc::new(RwLock::new(OptimizationConfig::default())),
            capabilities: Arc::new(RwLock::new(None)),
            cpu_affinity: CpuAffinityManager::new(),
            priority_controller: ProcessPriorityController::new(),
            memory_optimizer: MemoryOptimizer::new(),
            background_suppressor: BackgroundSuppressor::new(),
            preset_manager: HardwarePresetManager::new(),
            frame_monitor: FrameMonitor::new(),
            asset_cache: AssetCache::new(),
            world_host: LocalWorldHost::new(),
            snapshot_manager: SaveSnapshotManager::new(),
            mod_resolver: ModDependencyResolver::new(),
        }
    }
    
    pub fn detect_capabilities(&self) -> SystemCapabilities {
        use sysinfo::System;
        
        let mut sys = System::new_all();
        sys.refresh_all();
        
        let cpu_cores = sys.cpus().len();
        let total_memory_mb = sys.total_memory() / 1024 / 1024;
        let available_memory_mb = sys.available_memory() / 1024 / 1024;
        
        let capabilities = SystemCapabilities {
            cpu_cores,
            performance_cores: cpu_cores,
            efficiency_cores: 0,
            total_memory_mb,
            available_memory_mb,
            gpu_name: None,
            gpu_memory_mb: None,
            has_ssd: true,
            os_version: System::long_os_version().unwrap_or_default(),
        };
        
        *self.capabilities.write() = Some(capabilities.clone());
        capabilities
    }
    
    pub fn get_config(&self) -> OptimizationConfig {
        self.config.read().clone()
    }
    
    pub fn update_config(&self, config: OptimizationConfig) {
        *self.config.write() = config;
    }
    
    pub fn apply_preset(&self, preset_name: &str) -> Result<(), String> {
        let preset = self.preset_manager.get_preset(preset_name)
            .ok_or_else(|| format!("Preset '{}' not found", preset_name))?;
        
        let mut config = self.config.write();
        config.cpu_affinity_enabled = preset.cpu_affinity_enabled;
        config.performance_cores_only = preset.performance_cores_only;
        config.priority_boost_enabled = preset.priority_boost_enabled;
        config.priority_level = preset.priority_level;
        config.memory_optimization_enabled = preset.memory_optimization_enabled;
        config.target_memory_mb = preset.target_memory_mb;
        config.background_suppression_enabled = preset.background_suppression_enabled;
        config.frame_monitoring_enabled = preset.frame_monitoring_enabled;
        config.target_framerate = preset.target_framerate;
        config.active_preset = Some(preset_name.to_string());
        
        Ok(())
    }
    
    pub fn get_available_presets(&self) -> Vec<HardwarePreset> {
        self.preset_manager.get_all_presets()
    }
    
    pub fn get_recommended_preset(&self) -> Option<String> {
        let caps = self.capabilities.read();
        caps.as_ref().map(|c| self.preset_manager.recommend_preset(c))
    }
    
    pub async fn optimize_for_launch(&self, process_id: u32) -> Result<OptimizationResult, String> {
        let config = self.config.read().clone();
        let mut results = OptimizationResult::default();
        
        if config.cpu_affinity_enabled {
            match self.cpu_affinity.set_affinity(process_id, config.performance_cores_only) {
                Ok(cores) => {
                    results.cpu_affinity_set = true;
                    results.cores_assigned = cores;
                }
                Err(e) => {
                    tracing::warn!("Failed to set CPU affinity: {}", e);
                }
            }
        }
        
        if config.priority_boost_enabled {
            match self.priority_controller.set_priority(process_id, config.priority_level) {
                Ok(_) => {
                    results.priority_set = true;
                    results.priority_level = Some(config.priority_level);
                }
                Err(e) => {
                    tracing::warn!("Failed to set process priority: {}", e);
                }
            }
        }
        
        if config.memory_optimization_enabled {
            match self.memory_optimizer.optimize(config.target_memory_mb).await {
                Ok(freed) => {
                    results.memory_optimized = true;
                    results.memory_freed_mb = freed;
                }
                Err(e) => {
                    tracing::warn!("Failed to optimize memory: {}", e);
                }
            }
        }
        
        if config.background_suppression_enabled {
            match self.background_suppressor.suppress().await {
                Ok(count) => {
                    results.background_suppressed = true;
                    results.processes_suppressed = count;
                }
                Err(e) => {
                    tracing::warn!("Failed to suppress background processes: {}", e);
                }
            }
        }
        
        Ok(results)
    }
    
    pub async fn restore_after_exit(&self) -> Result<(), String> {
        self.background_suppressor.restore().await
    }
    
    pub fn cpu_affinity(&self) -> &CpuAffinityManager {
        &self.cpu_affinity
    }
    
    pub fn priority_controller(&self) -> &ProcessPriorityController {
        &self.priority_controller
    }
    
    pub fn memory_optimizer(&self) -> &MemoryOptimizer {
        &self.memory_optimizer
    }
    
    pub fn background_suppressor(&self) -> &BackgroundSuppressor {
        &self.background_suppressor
    }
    
    pub fn frame_monitor(&self) -> &FrameMonitor {
        &self.frame_monitor
    }
    
    pub fn asset_cache(&self) -> &AssetCache {
        &self.asset_cache
    }
    
    pub fn world_host(&self) -> &LocalWorldHost {
        &self.world_host
    }
    
    pub fn snapshot_manager(&self) -> &SaveSnapshotManager {
        &self.snapshot_manager
    }
    
    pub fn mod_resolver(&self) -> &ModDependencyResolver {
        &self.mod_resolver
    }
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct OptimizationResult {
    pub cpu_affinity_set: bool,
    pub cores_assigned: Vec<usize>,
    pub priority_set: bool,
    pub priority_level: Option<PriorityLevel>,
    pub memory_optimized: bool,
    pub memory_freed_mb: u64,
    pub background_suppressed: bool,
    pub processes_suppressed: usize,
}
