use serde::{Deserialize, Serialize};
use sysinfo::System;
use parking_lot::RwLock;
use std::collections::HashSet;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CpuInfo {
    pub core_id: usize,
    pub frequency_mhz: u64,
    pub vendor: String,
    pub brand: String,
    pub is_performance_core: bool,
}

pub struct CpuAffinityManager {
    detected_cores: RwLock<Vec<CpuInfo>>,
    performance_core_ids: RwLock<HashSet<usize>>,
}

impl CpuAffinityManager {
    pub fn new() -> Self {
        let manager = Self {
            detected_cores: RwLock::new(Vec::new()),
            performance_core_ids: RwLock::new(HashSet::new()),
        };
        manager.detect_cores();
        manager
    }
    
    pub fn detect_cores(&self) {
        let mut sys = System::new();
        sys.refresh_cpu_all();
        
        let mut cores = Vec::new();
        let mut performance_cores = HashSet::new();
        
        let cpus = sys.cpus();
        if cpus.is_empty() {
            return;
        }
        
        let avg_freq: u64 = cpus.iter().map(|c| c.frequency()).sum::<u64>() / cpus.len() as u64;
        
        for (i, cpu) in cpus.iter().enumerate() {
            let is_performance = cpu.frequency() >= avg_freq;
            
            let info = CpuInfo {
                core_id: i,
                frequency_mhz: cpu.frequency(),
                vendor: cpu.vendor_id().to_string(),
                brand: cpu.brand().to_string(),
                is_performance_core: is_performance,
            };
            
            if is_performance {
                performance_cores.insert(i);
            }
            
            cores.push(info);
        }
        
        *self.detected_cores.write() = cores;
        *self.performance_core_ids.write() = performance_cores;
    }
    
    pub fn get_cores(&self) -> Vec<CpuInfo> {
        self.detected_cores.read().clone()
    }
    
    pub fn get_performance_cores(&self) -> Vec<usize> {
        self.performance_core_ids.read().iter().cloned().collect()
    }
    
    pub fn get_core_count(&self) -> usize {
        self.detected_cores.read().len()
    }
    
    #[cfg(target_os = "windows")]
    pub fn set_affinity(&self, process_id: u32, performance_cores_only: bool) -> Result<Vec<usize>, String> {
        use windows::Win32::System::Threading::{
            OpenProcess, SetProcessAffinityMask, PROCESS_SET_INFORMATION, PROCESS_QUERY_INFORMATION,
        };
        use windows::Win32::Foundation::CloseHandle;
        
        let cores_to_use: Vec<usize> = if performance_cores_only {
            self.get_performance_cores()
        } else {
            (0..self.get_core_count()).collect()
        };
        
        if cores_to_use.is_empty() {
            return Err("No cores available".to_string());
        }
        
        let mut mask: usize = 0;
        for core in &cores_to_use {
            mask |= 1 << core;
        }
        
        unsafe {
            let handle = OpenProcess(
                PROCESS_SET_INFORMATION | PROCESS_QUERY_INFORMATION,
                false,
                process_id,
            ).map_err(|e| format!("Failed to open process: {}", e))?;
            
            let result = SetProcessAffinityMask(handle, mask);
            let _ = CloseHandle(handle);
            
            if result.is_ok() {
                Ok(cores_to_use)
            } else {
                Err("Failed to set affinity mask".to_string())
            }
        }
    }
    
    #[cfg(not(target_os = "windows"))]
    pub fn set_affinity(&self, _process_id: u32, performance_cores_only: bool) -> Result<Vec<usize>, String> {
        let cores_to_use: Vec<usize> = if performance_cores_only {
            self.get_performance_cores()
        } else {
            (0..self.get_core_count()).collect()
        };
        
        tracing::info!("CPU affinity would be set to cores: {:?} (simulated on non-Windows)", cores_to_use);
        Ok(cores_to_use)
    }
    
    pub fn set_affinity_mask(&self, process_id: u32, core_mask: &[usize]) -> Result<(), String> {
        if core_mask.is_empty() {
            return Err("Core mask cannot be empty".to_string());
        }
        
        #[cfg(target_os = "windows")]
        {
            use windows::Win32::System::Threading::{
                OpenProcess, SetProcessAffinityMask, PROCESS_SET_INFORMATION, PROCESS_QUERY_INFORMATION,
            };
            use windows::Win32::Foundation::CloseHandle;
            
            let mut mask: usize = 0;
            for core in core_mask {
                mask |= 1 << core;
            }
            
            unsafe {
                let handle = OpenProcess(
                    PROCESS_SET_INFORMATION | PROCESS_QUERY_INFORMATION,
                    false,
                    process_id,
                ).map_err(|e| format!("Failed to open process: {}", e))?;
                
                let result = SetProcessAffinityMask(handle, mask);
                let _ = CloseHandle(handle);
                
                if result.is_ok() {
                    Ok(())
                } else {
                    Err("Failed to set affinity mask".to_string())
                }
            }
        }
        
        #[cfg(not(target_os = "windows"))]
        {
            let _ = process_id;
            tracing::info!("CPU affinity mask set to {:?} (simulated)", core_mask);
            Ok(())
        }
    }
}
