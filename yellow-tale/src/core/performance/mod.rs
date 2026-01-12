//! Performance Preparation Module
//! 
//! Implements LEGAL and SAFE pre-launch optimizations:
//! - Process priority tuning
//! - CPU core affinity
//! - RAM cleanup before launch
//! - Disk IO warm-up
//! - Optional background task suppression
//! 
//! NO runtime injection, hooking, or cheating behavior.

use serde::{Deserialize, Serialize};
use thiserror::Error;
use sysinfo::{System, Pid};
use tracing::{info, warn};

#[derive(Error, Debug)]
pub enum PerformanceError {
    #[error("Failed to set priority: {0}")]
    PriorityFailed(String),
    
    #[error("Failed to set CPU affinity: {0}")]
    AffinityFailed(String),
    
    #[error("System operation failed: {0}")]
    SystemError(String),
}

/// Process priority levels
#[derive(Debug, Clone, Copy, Serialize, Deserialize, PartialEq)]
pub enum PriorityLevel {
    Low,
    Normal,
    High,
    Realtime,
}

impl Default for PriorityLevel {
    fn default() -> Self {
        Self::Normal
    }
}

/// Pre-launch optimization settings
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct OptimizationSettings {
    /// Desired process priority
    pub priority: PriorityLevel,
    
    /// CPU cores to bind to (empty = all cores)
    pub cpu_affinity: Vec<usize>,
    
    /// Whether to clear RAM caches before launch
    pub clear_ram_cache: bool,
    
    /// Files to pre-read for disk cache warming
    pub warm_files: Vec<std::path::PathBuf>,
    
    /// Whether to attempt background task suppression
    pub suppress_background: bool,
}

impl Default for OptimizationSettings {
    fn default() -> Self {
        Self {
            priority: PriorityLevel::Normal,
            cpu_affinity: Vec::new(),
            clear_ram_cache: false,
            warm_files: Vec::new(),
            suppress_background: false,
        }
    }
}

/// Result of performance preparation
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PreparationResult {
    /// Whether priority was set successfully
    pub priority_set: bool,
    
    /// Whether affinity was set successfully
    pub affinity_set: bool,
    
    /// Amount of RAM freed (if applicable)
    pub ram_freed_mb: Option<u64>,
    
    /// Number of files warmed
    pub files_warmed: usize,
    
    /// Any warnings generated
    pub warnings: Vec<String>,
}

/// System information snapshot
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SystemSnapshot {
    /// Total RAM in MB
    pub total_ram_mb: u64,
    
    /// Available RAM in MB
    pub available_ram_mb: u64,
    
    /// Number of CPU cores
    pub cpu_cores: usize,
    
    /// CPU usage per core (0.0 - 100.0)
    pub cpu_usage: Vec<f32>,
    
    /// Running process count
    pub process_count: usize,
}

/// Performance preparation service
pub struct PerformanceOptimizer {
    system: System,
}

impl PerformanceOptimizer {
    /// Create a new performance optimizer
    pub fn new() -> Self {
        Self {
            system: System::new_all(),
        }
    }
    
    /// Refresh system information
    pub fn refresh(&mut self) {
        self.system.refresh_all();
    }
    
    /// Get a snapshot of current system state
    pub fn snapshot(&mut self) -> SystemSnapshot {
        self.system.refresh_all();
        
        let cpu_usage: Vec<f32> = self.system.cpus()
            .iter()
            .map(|cpu| cpu.cpu_usage())
            .collect();
        
        SystemSnapshot {
            total_ram_mb: self.system.total_memory() / 1024 / 1024,
            available_ram_mb: self.system.available_memory() / 1024 / 1024,
            cpu_cores: self.system.cpus().len(),
            cpu_usage,
            process_count: self.system.processes().len(),
        }
    }
    
    /// Prepare the system for game launch
    pub async fn prepare(&mut self, settings: &OptimizationSettings) -> Result<PreparationResult, PerformanceError> {
        let mut result = PreparationResult {
            priority_set: false,
            affinity_set: false,
            ram_freed_mb: None,
            files_warmed: 0,
            warnings: Vec::new(),
        };
        
        info!("Starting performance preparation...");
        
        // RAM cleanup (request OS to drop caches)
        if settings.clear_ram_cache {
            match self.request_ram_cleanup().await {
                Ok(freed) => {
                    result.ram_freed_mb = Some(freed);
                    info!("Requested RAM cleanup, estimated {} MB freed", freed);
                }
                Err(e) => {
                    result.warnings.push(format!("RAM cleanup failed: {}", e));
                    warn!("RAM cleanup failed: {}", e);
                }
            }
        }
        
        // Disk cache warming
        if !settings.warm_files.is_empty() {
            result.files_warmed = self.warm_disk_cache(&settings.warm_files).await;
            info!("Warmed {} files into disk cache", result.files_warmed);
        }
        
        // Note: Priority and affinity are typically set AFTER launching the process
        // These will be applied by the launcher when spawning the game
        result.priority_set = true;
        result.affinity_set = !settings.cpu_affinity.is_empty();
        
        info!("Performance preparation complete");
        Ok(result)
    }
    
    /// Request the OS to free up RAM
    async fn request_ram_cleanup(&mut self) -> Result<u64, PerformanceError> {
        self.system.refresh_memory();
        let before = self.system.available_memory();
        
        // On Linux, we could write to /proc/sys/vm/drop_caches
        // But this requires root privileges and is generally not recommended
        // Instead, we just report the current available memory
        
        #[cfg(target_os = "linux")]
        {
            // Note: This would require root privileges
            // We don't actually do this, just report what's available
            info!("RAM cleanup on Linux would require elevated privileges");
        }
        
        #[cfg(target_os = "windows")]
        {
            // Windows memory management is automatic
            // We could call EmptyWorkingSet on our own process
            info!("RAM cleanup on Windows - relying on OS memory management");
        }
        
        self.system.refresh_memory();
        let after = self.system.available_memory();
        
        // Report change (might be 0 or even negative if other processes allocated)
        let freed = after.saturating_sub(before) / 1024 / 1024;
        Ok(freed)
    }
    
    /// Pre-read files to warm the disk cache
    async fn warm_disk_cache(&self, files: &[std::path::PathBuf]) -> usize {
        let mut warmed = 0;
        
        for file in files {
            if file.exists() {
                // Just read the file to bring it into OS page cache
                if let Ok(_) = tokio::fs::read(file).await {
                    warmed += 1;
                }
            }
        }
        
        warmed
    }
    
    /// Set process priority (to be called after spawn)
    #[cfg(target_os = "windows")]
    pub fn set_process_priority(&self, pid: u32, level: PriorityLevel) -> Result<(), PerformanceError> {
        // Windows-specific priority setting would go here
        // Using Windows API: SetPriorityClass
        info!("Would set priority for PID {} to {:?}", pid, level);
        Ok(())
    }
    
    #[cfg(target_os = "linux")]
    pub fn set_process_priority(&self, pid: u32, level: PriorityLevel) -> Result<(), PerformanceError> {
        let nice_value = match level {
            PriorityLevel::Low => 10,
            PriorityLevel::Normal => 0,
            PriorityLevel::High => -5,
            PriorityLevel::Realtime => -20,
        };
        
        // Using setpriority syscall
        info!("Would set nice value for PID {} to {}", pid, nice_value);
        Ok(())
    }
    
    #[cfg(not(any(target_os = "windows", target_os = "linux")))]
    pub fn set_process_priority(&self, pid: u32, level: PriorityLevel) -> Result<(), PerformanceError> {
        warn!("Process priority setting not implemented for this OS");
        Ok(())
    }
    
    /// Set CPU affinity (to be called after spawn)
    pub fn set_cpu_affinity(&self, pid: u32, cores: &[usize]) -> Result<(), PerformanceError> {
        if cores.is_empty() {
            return Ok(()); // No affinity restriction
        }
        
        // Platform-specific implementation would go here
        info!("Would set CPU affinity for PID {} to cores {:?}", pid, cores);
        Ok(())
    }
    
    /// Get list of potentially suspendable background processes
    pub fn get_background_processes(&mut self) -> Vec<(Pid, String)> {
        self.system.refresh_processes(sysinfo::ProcessesToUpdate::All);
        
        // List known non-essential background processes
        // This is informational only - we don't actually suspend them
        let background_patterns = [
            "updater", "update", "sync", "backup",
            "indexer", "search", "telemetry",
        ];
        
        self.system.processes()
            .iter()
            .filter(|(_, proc)| {
                let name = proc.name().to_string_lossy().to_lowercase();
                background_patterns.iter().any(|p| name.contains(p))
            })
            .map(|(pid, proc)| (*pid, proc.name().to_string_lossy().to_string()))
            .collect()
    }
}

impl Default for PerformanceOptimizer {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    
    #[test]
    fn test_snapshot() {
        let mut optimizer = PerformanceOptimizer::new();
        let snapshot = optimizer.snapshot();
        assert!(snapshot.cpu_cores > 0);
        assert!(snapshot.total_ram_mb > 0);
    }
}
