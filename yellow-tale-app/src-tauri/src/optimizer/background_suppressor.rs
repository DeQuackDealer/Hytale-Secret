use serde::{Deserialize, Serialize};
use sysinfo::{System, ProcessRefreshKind, Pid};
use parking_lot::RwLock;
use std::collections::HashMap;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SuppressedProcess {
    pub pid: u32,
    pub name: String,
    pub original_priority: Option<i32>,
    pub suppressed_at: chrono::DateTime<chrono::Utc>,
}

const SUPPRESSIBLE_PROCESSES: &[&str] = &[
    "discord",
    "spotify",
    "steam",
    "epicgameslauncher",
    "origin",
    "gog",
    "battlenet",
    "chrome",
    "firefox",
    "edge",
    "opera",
    "brave",
    "slack",
    "teams",
    "zoom",
    "skype",
    "telegram",
    "signal",
    "obs",
    "streamlabs",
    "nvidia",
    "geforce",
    "razer",
    "corsair",
    "logitech",
    "steelseries",
    "hwinfo",
    "afterburner",
    "rivatuner",
    "wallpaper",
    "rainmeter",
];

const PROTECTED_PROCESSES: &[&str] = &[
    "system",
    "csrss",
    "smss",
    "services",
    "lsass",
    "winlogon",
    "svchost",
    "dwm",
    "explorer",
    "taskmgr",
    "audiodg",
    "conhost",
    "wininit",
    "fontdrvhost",
    "securityhealthservice",
    "antimalware",
    "defender",
    "hytale",
    "yellow",
];

pub struct BackgroundSuppressor {
    suppressed: RwLock<HashMap<u32, SuppressedProcess>>,
    is_active: RwLock<bool>,
    whitelist: RwLock<Vec<String>>,
}

impl BackgroundSuppressor {
    pub fn new() -> Self {
        Self {
            suppressed: RwLock::new(HashMap::new()),
            is_active: RwLock::new(false),
            whitelist: RwLock::new(Vec::new()),
        }
    }
    
    pub fn add_to_whitelist(&self, process_name: String) {
        self.whitelist.write().push(process_name.to_lowercase());
    }
    
    pub fn remove_from_whitelist(&self, process_name: &str) {
        let lower = process_name.to_lowercase();
        self.whitelist.write().retain(|n| n != &lower);
    }
    
    pub fn get_whitelist(&self) -> Vec<String> {
        self.whitelist.read().clone()
    }
    
    fn should_suppress(&self, process_name: &str) -> bool {
        let lower = process_name.to_lowercase();
        
        if self.whitelist.read().iter().any(|w| lower.contains(w)) {
            return false;
        }
        
        if PROTECTED_PROCESSES.iter().any(|p| lower.contains(p)) {
            return false;
        }
        
        SUPPRESSIBLE_PROCESSES.iter().any(|p| lower.contains(p))
    }
    
    pub fn get_suppressible_processes(&self) -> Vec<(u32, String)> {
        let mut sys = System::new();
        sys.refresh_processes_specifics(ProcessRefreshKind::new());
        
        let mut suppressible = Vec::new();
        
        for (pid, process) in sys.processes() {
            let name = process.name().to_string_lossy().to_string();
            if self.should_suppress(&name) {
                suppressible.push((pid.as_u32(), name));
            }
        }
        
        suppressible
    }
    
    #[cfg(target_os = "windows")]
    pub async fn suppress(&self) -> Result<usize, String> {
        use windows::Win32::System::Threading::{
            OpenProcess, SetPriorityClass, GetPriorityClass,
            PROCESS_SET_INFORMATION, PROCESS_QUERY_INFORMATION,
            IDLE_PRIORITY_CLASS,
        };
        use windows::Win32::Foundation::CloseHandle;
        
        if *self.is_active.read() {
            return Ok(0);
        }
        
        *self.is_active.write() = true;
        
        let processes = self.get_suppressible_processes();
        let mut count = 0;
        
        for (pid, name) in processes {
            unsafe {
                if let Ok(handle) = OpenProcess(
                    PROCESS_SET_INFORMATION | PROCESS_QUERY_INFORMATION,
                    false,
                    pid,
                ) {
                    let original = GetPriorityClass(handle);
                    
                    if SetPriorityClass(handle, IDLE_PRIORITY_CLASS).is_ok() {
                        self.suppressed.write().insert(pid, SuppressedProcess {
                            pid,
                            name: name.clone(),
                            original_priority: Some(original.0 as i32),
                            suppressed_at: chrono::Utc::now(),
                        });
                        count += 1;
                        tracing::debug!("Suppressed process: {} (PID: {})", name, pid);
                    }
                    
                    let _ = CloseHandle(handle);
                }
            }
        }
        
        tracing::info!("Suppressed {} background processes", count);
        Ok(count)
    }
    
    #[cfg(not(target_os = "windows"))]
    pub async fn suppress(&self) -> Result<usize, String> {
        if *self.is_active.read() {
            return Ok(0);
        }
        
        *self.is_active.write() = true;
        
        let processes = self.get_suppressible_processes();
        let count = processes.len();
        
        for (pid, name) in processes {
            self.suppressed.write().insert(pid, SuppressedProcess {
                pid,
                name: name.clone(),
                original_priority: None,
                suppressed_at: chrono::Utc::now(),
            });
        }
        
        tracing::info!("Would suppress {} background processes (simulated)", count);
        Ok(count)
    }
    
    #[cfg(target_os = "windows")]
    pub async fn restore(&self) -> Result<(), String> {
        use windows::Win32::System::Threading::{
            OpenProcess, SetPriorityClass, PROCESS_SET_INFORMATION,
            PROCESS_CREATION_FLAGS,
        };
        use windows::Win32::Foundation::CloseHandle;
        
        if !*self.is_active.read() {
            return Ok(());
        }
        
        let suppressed = self.suppressed.write().drain().collect::<Vec<_>>();
        let mut restored = 0;
        
        for (pid, info) in suppressed {
            if let Some(original_priority) = info.original_priority {
                unsafe {
                    if let Ok(handle) = OpenProcess(
                        PROCESS_SET_INFORMATION,
                        false,
                        pid,
                    ) {
                        if SetPriorityClass(handle, PROCESS_CREATION_FLAGS(original_priority as u32)).is_ok() {
                            restored += 1;
                        }
                        let _ = CloseHandle(handle);
                    }
                }
            }
        }
        
        *self.is_active.write() = false;
        tracing::info!("Restored {} background processes", restored);
        Ok(())
    }
    
    #[cfg(not(target_os = "windows"))]
    pub async fn restore(&self) -> Result<(), String> {
        if !*self.is_active.read() {
            return Ok(());
        }
        
        let count = self.suppressed.read().len();
        self.suppressed.write().clear();
        *self.is_active.write() = false;
        
        tracing::info!("Restored {} background processes (simulated)", count);
        Ok(())
    }
    
    pub fn is_active(&self) -> bool {
        *self.is_active.read()
    }
    
    pub fn get_suppressed(&self) -> Vec<SuppressedProcess> {
        self.suppressed.read().values().cloned().collect()
    }
}
