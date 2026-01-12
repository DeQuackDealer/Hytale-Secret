use super::{PriorityLevel, SystemCapabilities};
use serde::{Deserialize, Serialize};
use parking_lot::RwLock;
use std::collections::HashMap;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct HardwarePreset {
    pub name: String,
    pub description: String,
    pub min_cpu_cores: usize,
    pub min_ram_mb: u64,
    pub recommended_for: Vec<String>,
    
    pub cpu_affinity_enabled: bool,
    pub performance_cores_only: bool,
    pub priority_boost_enabled: bool,
    pub priority_level: PriorityLevel,
    pub memory_optimization_enabled: bool,
    pub target_memory_mb: u32,
    pub background_suppression_enabled: bool,
    pub frame_monitoring_enabled: bool,
    pub target_framerate: Option<u32>,
    pub shader_prewarming_enabled: bool,
    pub asset_cache_size_mb: u32,
}

impl Default for HardwarePreset {
    fn default() -> Self {
        Self {
            name: "balanced".to_string(),
            description: "Balanced performance and resource usage".to_string(),
            min_cpu_cores: 4,
            min_ram_mb: 8192,
            recommended_for: vec!["mid-range".to_string()],
            cpu_affinity_enabled: true,
            performance_cores_only: false,
            priority_boost_enabled: true,
            priority_level: PriorityLevel::AboveNormal,
            memory_optimization_enabled: true,
            target_memory_mb: 4096,
            background_suppression_enabled: false,
            frame_monitoring_enabled: true,
            target_framerate: None,
            shader_prewarming_enabled: true,
            asset_cache_size_mb: 2048,
        }
    }
}

pub struct HardwarePresetManager {
    presets: RwLock<HashMap<String, HardwarePreset>>,
}

impl HardwarePresetManager {
    pub fn new() -> Self {
        let manager = Self {
            presets: RwLock::new(HashMap::new()),
        };
        manager.initialize_defaults();
        manager
    }
    
    fn initialize_defaults(&self) {
        let mut presets = self.presets.write();
        
        presets.insert("ultra_performance".to_string(), HardwarePreset {
            name: "ultra_performance".to_string(),
            description: "Maximum performance for high-end systems. Uses aggressive optimizations.".to_string(),
            min_cpu_cores: 8,
            min_ram_mb: 16384,
            recommended_for: vec!["high-end".to_string(), "gaming".to_string()],
            cpu_affinity_enabled: true,
            performance_cores_only: true,
            priority_boost_enabled: true,
            priority_level: PriorityLevel::High,
            memory_optimization_enabled: true,
            target_memory_mb: 8192,
            background_suppression_enabled: true,
            frame_monitoring_enabled: true,
            target_framerate: None,
            shader_prewarming_enabled: true,
            asset_cache_size_mb: 4096,
        });
        
        presets.insert("performance".to_string(), HardwarePreset {
            name: "performance".to_string(),
            description: "High performance with some resource consideration.".to_string(),
            min_cpu_cores: 6,
            min_ram_mb: 12288,
            recommended_for: vec!["mid-high".to_string()],
            cpu_affinity_enabled: true,
            performance_cores_only: true,
            priority_boost_enabled: true,
            priority_level: PriorityLevel::AboveNormal,
            memory_optimization_enabled: true,
            target_memory_mb: 6144,
            background_suppression_enabled: false,
            frame_monitoring_enabled: true,
            target_framerate: None,
            shader_prewarming_enabled: true,
            asset_cache_size_mb: 3072,
        });
        
        presets.insert("balanced".to_string(), HardwarePreset::default());
        
        presets.insert("efficiency".to_string(), HardwarePreset {
            name: "efficiency".to_string(),
            description: "Balanced performance with lower resource usage. Good for multitasking.".to_string(),
            min_cpu_cores: 4,
            min_ram_mb: 8192,
            recommended_for: vec!["mid-range".to_string(), "laptop".to_string()],
            cpu_affinity_enabled: false,
            performance_cores_only: false,
            priority_boost_enabled: false,
            priority_level: PriorityLevel::Normal,
            memory_optimization_enabled: true,
            target_memory_mb: 3072,
            background_suppression_enabled: false,
            frame_monitoring_enabled: false,
            target_framerate: Some(60),
            shader_prewarming_enabled: true,
            asset_cache_size_mb: 1024,
        });
        
        presets.insert("low_spec".to_string(), HardwarePreset {
            name: "low_spec".to_string(),
            description: "Optimized for lower-end hardware. Reduces resource usage significantly.".to_string(),
            min_cpu_cores: 2,
            min_ram_mb: 4096,
            recommended_for: vec!["low-end".to_string(), "older hardware".to_string()],
            cpu_affinity_enabled: false,
            performance_cores_only: false,
            priority_boost_enabled: false,
            priority_level: PriorityLevel::Normal,
            memory_optimization_enabled: true,
            target_memory_mb: 2048,
            background_suppression_enabled: false,
            frame_monitoring_enabled: false,
            target_framerate: Some(30),
            shader_prewarming_enabled: false,
            asset_cache_size_mb: 512,
        });
        
        presets.insert("streaming".to_string(), HardwarePreset {
            name: "streaming".to_string(),
            description: "Optimized for streaming/recording. Leaves resources for capture software.".to_string(),
            min_cpu_cores: 6,
            min_ram_mb: 16384,
            recommended_for: vec!["streaming".to_string(), "content creation".to_string()],
            cpu_affinity_enabled: true,
            performance_cores_only: false,
            priority_boost_enabled: true,
            priority_level: PriorityLevel::AboveNormal,
            memory_optimization_enabled: false,
            target_memory_mb: 4096,
            background_suppression_enabled: false,
            frame_monitoring_enabled: true,
            target_framerate: Some(60),
            shader_prewarming_enabled: true,
            asset_cache_size_mb: 2048,
        });
        
        presets.insert("battery_saver".to_string(), HardwarePreset {
            name: "battery_saver".to_string(),
            description: "Minimal resource usage for laptops on battery power.".to_string(),
            min_cpu_cores: 2,
            min_ram_mb: 4096,
            recommended_for: vec!["laptop".to_string(), "mobile".to_string()],
            cpu_affinity_enabled: false,
            performance_cores_only: false,
            priority_boost_enabled: false,
            priority_level: PriorityLevel::Normal,
            memory_optimization_enabled: true,
            target_memory_mb: 2048,
            background_suppression_enabled: false,
            frame_monitoring_enabled: false,
            target_framerate: Some(30),
            shader_prewarming_enabled: false,
            asset_cache_size_mb: 256,
        });
    }
    
    pub fn get_preset(&self, name: &str) -> Option<HardwarePreset> {
        self.presets.read().get(name).cloned()
    }
    
    pub fn get_all_presets(&self) -> Vec<HardwarePreset> {
        self.presets.read().values().cloned().collect()
    }
    
    pub fn add_custom_preset(&self, preset: HardwarePreset) {
        self.presets.write().insert(preset.name.clone(), preset);
    }
    
    pub fn remove_preset(&self, name: &str) -> bool {
        self.presets.write().remove(name).is_some()
    }
    
    pub fn recommend_preset(&self, capabilities: &SystemCapabilities) -> String {
        let presets = self.presets.read();
        
        if capabilities.cpu_cores >= 8 && capabilities.total_memory_mb >= 16384 {
            return "ultra_performance".to_string();
        }
        
        if capabilities.cpu_cores >= 6 && capabilities.total_memory_mb >= 12288 {
            return "performance".to_string();
        }
        
        if capabilities.cpu_cores >= 4 && capabilities.total_memory_mb >= 8192 {
            return "balanced".to_string();
        }
        
        if capabilities.cpu_cores >= 4 && capabilities.total_memory_mb >= 6144 {
            return "efficiency".to_string();
        }
        
        "low_spec".to_string()
    }
    
    pub fn get_preset_for_use_case(&self, use_case: &str) -> Option<String> {
        let presets = self.presets.read();
        
        for (name, preset) in presets.iter() {
            if preset.recommended_for.iter().any(|r| r.to_lowercase() == use_case.to_lowercase()) {
                return Some(name.clone());
            }
        }
        
        None
    }
}
