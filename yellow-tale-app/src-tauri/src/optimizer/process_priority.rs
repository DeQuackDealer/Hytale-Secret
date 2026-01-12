use super::PriorityLevel;
use parking_lot::RwLock;
use std::collections::HashMap;

pub struct ProcessPriorityController {
    original_priorities: RwLock<HashMap<u32, i32>>,
}

impl ProcessPriorityController {
    pub fn new() -> Self {
        Self {
            original_priorities: RwLock::new(HashMap::new()),
        }
    }
    
    #[cfg(target_os = "windows")]
    pub fn set_priority(&self, process_id: u32, level: PriorityLevel) -> Result<(), String> {
        use windows::Win32::System::Threading::{
            OpenProcess, SetPriorityClass, GetPriorityClass,
            PROCESS_SET_INFORMATION, PROCESS_QUERY_INFORMATION,
            ABOVE_NORMAL_PRIORITY_CLASS, HIGH_PRIORITY_CLASS, 
            NORMAL_PRIORITY_CLASS, REALTIME_PRIORITY_CLASS,
            PROCESS_CREATION_FLAGS,
        };
        use windows::Win32::Foundation::CloseHandle;
        
        let priority_class = match level {
            PriorityLevel::Normal => NORMAL_PRIORITY_CLASS,
            PriorityLevel::AboveNormal => ABOVE_NORMAL_PRIORITY_CLASS,
            PriorityLevel::High => HIGH_PRIORITY_CLASS,
            PriorityLevel::Realtime => REALTIME_PRIORITY_CLASS,
        };
        
        unsafe {
            let handle = OpenProcess(
                PROCESS_SET_INFORMATION | PROCESS_QUERY_INFORMATION,
                false,
                process_id,
            ).map_err(|e| format!("Failed to open process: {}", e))?;
            
            let original = GetPriorityClass(handle);
            self.original_priorities.write().insert(process_id, original.0 as i32);
            
            let result = SetPriorityClass(handle, priority_class);
            let _ = CloseHandle(handle);
            
            if result.is_ok() {
                tracing::info!("Set process {} priority to {:?}", process_id, level);
                Ok(())
            } else {
                Err("Failed to set priority class".to_string())
            }
        }
    }
    
    #[cfg(not(target_os = "windows"))]
    pub fn set_priority(&self, process_id: u32, level: PriorityLevel) -> Result<(), String> {
        self.original_priorities.write().insert(process_id, 0);
        tracing::info!("Set process {} priority to {:?} (simulated on non-Windows)", process_id, level);
        Ok(())
    }
    
    #[cfg(target_os = "windows")]
    pub fn restore_priority(&self, process_id: u32) -> Result<(), String> {
        use windows::Win32::System::Threading::{
            OpenProcess, SetPriorityClass, PROCESS_SET_INFORMATION,
            PROCESS_CREATION_FLAGS,
        };
        use windows::Win32::Foundation::CloseHandle;
        
        let original = self.original_priorities.write().remove(&process_id);
        
        if let Some(priority) = original {
            unsafe {
                let handle = OpenProcess(
                    PROCESS_SET_INFORMATION,
                    false,
                    process_id,
                ).map_err(|e| format!("Failed to open process: {}", e))?;
                
                let result = SetPriorityClass(handle, PROCESS_CREATION_FLAGS(priority as u32));
                let _ = CloseHandle(handle);
                
                if result.is_ok() {
                    Ok(())
                } else {
                    Err("Failed to restore priority".to_string())
                }
            }
        } else {
            Ok(())
        }
    }
    
    #[cfg(not(target_os = "windows"))]
    pub fn restore_priority(&self, process_id: u32) -> Result<(), String> {
        self.original_priorities.write().remove(&process_id);
        tracing::info!("Restored process {} priority (simulated)", process_id);
        Ok(())
    }
    
    pub fn get_managed_processes(&self) -> Vec<u32> {
        self.original_priorities.read().keys().cloned().collect()
    }
}
