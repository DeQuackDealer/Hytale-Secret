use serde::{Deserialize, Serialize};
use sysinfo::System;
use parking_lot::RwLock;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MemoryStats {
    pub total_mb: u64,
    pub used_mb: u64,
    pub available_mb: u64,
    pub usage_percent: f32,
    pub swap_total_mb: u64,
    pub swap_used_mb: u64,
}

pub struct MemoryOptimizer {
    stats: RwLock<Option<MemoryStats>>,
    target_available_mb: RwLock<u64>,
}

impl MemoryOptimizer {
    pub fn new() -> Self {
        Self {
            stats: RwLock::new(None),
            target_available_mb: RwLock::new(4096),
        }
    }
    
    pub fn refresh_stats(&self) -> MemoryStats {
        let mut sys = System::new();
        sys.refresh_memory();
        
        let total = sys.total_memory() / 1024 / 1024;
        let used = sys.used_memory() / 1024 / 1024;
        let available = sys.available_memory() / 1024 / 1024;
        let swap_total = sys.total_swap() / 1024 / 1024;
        let swap_used = sys.used_swap() / 1024 / 1024;
        
        let stats = MemoryStats {
            total_mb: total,
            used_mb: used,
            available_mb: available,
            usage_percent: (used as f32 / total as f32) * 100.0,
            swap_total_mb: swap_total,
            swap_used_mb: swap_used,
        };
        
        *self.stats.write() = Some(stats.clone());
        stats
    }
    
    pub fn get_stats(&self) -> Option<MemoryStats> {
        self.stats.read().clone()
    }
    
    pub fn set_target(&self, target_mb: u64) {
        *self.target_available_mb.write() = target_mb;
    }
    
    pub async fn optimize(&self, target_mb: u32) -> Result<u64, String> {
        self.set_target(target_mb as u64);
        
        let before = self.refresh_stats();
        
        #[cfg(target_os = "windows")]
        {
            self.trim_working_sets().await?;
        }
        
        tokio::time::sleep(tokio::time::Duration::from_millis(100)).await;
        
        let after = self.refresh_stats();
        
        let freed = if after.available_mb > before.available_mb {
            after.available_mb - before.available_mb
        } else {
            0
        };
        
        tracing::info!(
            "Memory optimization: before={}MB available, after={}MB available, freed={}MB",
            before.available_mb, after.available_mb, freed
        );
        
        Ok(freed)
    }
    
    #[cfg(target_os = "windows")]
    async fn trim_working_sets(&self) -> Result<(), String> {
        use sysinfo::{System, ProcessRefreshKind};
        use windows::Win32::System::Threading::{
            OpenProcess, PROCESS_QUERY_INFORMATION, PROCESS_SET_QUOTA,
        };
        use windows::Win32::System::ProcessStatus::EmptyWorkingSet;
        use windows::Win32::Foundation::CloseHandle;
        
        let mut sys = System::new();
        sys.refresh_processes_specifics(ProcessRefreshKind::new());
        
        let current_pid = std::process::id();
        let mut trimmed = 0;
        
        for (pid, process) in sys.processes() {
            let pid_u32 = pid.as_u32();
            
            if pid_u32 == current_pid || pid_u32 == 0 || pid_u32 == 4 {
                continue;
            }
            
            let name = process.name().to_string_lossy().to_lowercase();
            if name.contains("system") || name.contains("csrss") || 
               name.contains("smss") || name.contains("services") ||
               name.contains("lsass") || name.contains("winlogon") {
                continue;
            }
            
            unsafe {
                if let Ok(handle) = OpenProcess(
                    PROCESS_QUERY_INFORMATION | PROCESS_SET_QUOTA,
                    false,
                    pid_u32,
                ) {
                    let _ = EmptyWorkingSet(handle);
                    let _ = CloseHandle(handle);
                    trimmed += 1;
                }
            }
        }
        
        tracing::debug!("Trimmed working sets for {} processes", trimmed);
        Ok(())
    }
    
    pub fn is_memory_pressure_high(&self) -> bool {
        if let Some(stats) = self.stats.read().as_ref() {
            stats.usage_percent > 85.0
        } else {
            false
        }
    }
    
    pub fn get_recommended_allocation(&self) -> u64 {
        let stats = self.refresh_stats();
        
        let base_allocation = (stats.total_mb as f64 * 0.5) as u64;
        let max_allocation = stats.available_mb.saturating_sub(2048);
        
        base_allocation.min(max_allocation).max(2048)
    }
}
