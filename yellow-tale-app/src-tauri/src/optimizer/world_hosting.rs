use serde::{Deserialize, Serialize};
use std::path::PathBuf;
use std::process::Child;
use parking_lot::RwLock;
use tokio::process::Command;
use std::net::{SocketAddr, UdpSocket};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct WorldHostConfig {
    pub world_path: PathBuf,
    pub port: u16,
    pub max_players: u32,
    pub memory_mb: u32,
    pub motd: String,
    pub password: Option<String>,
}

impl Default for WorldHostConfig {
    fn default() -> Self {
        Self {
            world_path: PathBuf::from("worlds/default"),
            port: 25565,
            max_players: 8,
            memory_mb: 2048,
            motd: "Yellow Tale Local Server".to_string(),
            password: None,
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum NatType {
    Open,
    Moderate,
    Strict,
    Unknown,
}

impl NatType {
    pub fn as_str(&self) -> &'static str {
        match self {
            Self::Open => "open",
            Self::Moderate => "moderate",
            Self::Strict => "strict",
            Self::Unknown => "unknown",
        }
    }
    
    pub fn hosting_recommendation(&self) -> &'static str {
        match self {
            Self::Open => "Your network is ideal for hosting. Players should be able to connect directly.",
            Self::Moderate => "Your network may require port forwarding for optimal connectivity.",
            Self::Strict => "Your network has strict NAT. Consider using a relay service or VPN for hosting.",
            Self::Unknown => "Could not determine NAT type. Try testing connectivity manually.",
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct NatInfo {
    pub nat_type: NatType,
    pub external_ip: Option<String>,
    pub internal_ip: Option<String>,
    pub upnp_available: bool,
    pub port_forwarding_needed: bool,
    pub recommendation: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct HostingStatus {
    pub running: bool,
    pub world_name: String,
    pub players_online: u32,
    pub max_players: u32,
    pub uptime_seconds: u64,
    pub port: u16,
}

pub struct LocalWorldHost {
    config: RwLock<WorldHostConfig>,
    process: RwLock<Option<u32>>,
    start_time: RwLock<Option<std::time::Instant>>,
}

impl LocalWorldHost {
    pub fn new() -> Self {
        Self {
            config: RwLock::new(WorldHostConfig::default()),
            process: RwLock::new(None),
            start_time: RwLock::new(None),
        }
    }
    
    pub fn set_config(&self, config: WorldHostConfig) {
        *self.config.write() = config;
    }
    
    pub fn get_config(&self) -> WorldHostConfig {
        self.config.read().clone()
    }
    
    pub async fn detect_nat(&self) -> Result<NatInfo, String> {
        let internal_ip = self.get_local_ip()
            .ok_or_else(|| "Failed to detect local IP address".to_string())?;
        
        let external_ip = self.get_external_ip().await
            .map_err(|e| format!("Failed to detect external IP: {}", e))?;
        
        let (nat_type, upnp) = self.test_nat_type(&external_ip, &internal_ip).await;
        
        let port_forwarding_needed = nat_type != NatType::Open;
        let recommendation = nat_type.hosting_recommendation().to_string();
        
        Ok(NatInfo {
            nat_type,
            external_ip: Some(external_ip),
            internal_ip: Some(internal_ip),
            upnp_available: upnp,
            port_forwarding_needed,
            recommendation,
        })
    }
    
    fn get_local_ip(&self) -> Option<String> {
        let socket = UdpSocket::bind("0.0.0.0:0").ok()?;
        socket.connect("8.8.8.8:80").ok()?;
        let addr = socket.local_addr().ok()?;
        Some(addr.ip().to_string())
    }
    
    async fn get_external_ip(&self) -> Result<String, String> {
        let client = reqwest::Client::new();
        let response = client
            .get("https://api.ipify.org")
            .timeout(std::time::Duration::from_secs(5))
            .send()
            .await
            .map_err(|e| format!("Network request failed: {}", e))?;
        
        response.text().await
            .map_err(|e| format!("Failed to read response: {}", e))
    }
    
    async fn test_nat_type(&self, external: &str, internal: &str) -> (NatType, bool) {
        let upnp_available = self.check_upnp().await;
        
        if upnp_available {
            return (NatType::Open, true);
        }
        
        if external == internal {
            (NatType::Open, false)
        } else {
            (NatType::Moderate, false)
        }
    }
    
    async fn check_upnp(&self) -> bool {
        false
    }
    
    pub async fn start_server(&self, game_executable: &PathBuf) -> Result<u32, String> {
        if self.is_running() {
            return Err("Server is already running".to_string());
        }
        
        let config = self.config.read().clone();
        
        if !game_executable.exists() {
            return Err("Game executable not found".to_string());
        }
        
        let output = Command::new(game_executable)
            .arg("--server")
            .arg("--port")
            .arg(config.port.to_string())
            .arg("--world")
            .arg(&config.world_path)
            .arg("--max-players")
            .arg(config.max_players.to_string())
            .spawn()
            .map_err(|e| format!("Failed to start server: {}", e))?;
        
        let pid = output.id().unwrap_or(0);
        *self.process.write() = Some(pid);
        *self.start_time.write() = Some(std::time::Instant::now());
        
        tracing::info!("Started local server on port {} with PID {}", config.port, pid);
        
        Ok(pid)
    }
    
    pub async fn stop_server(&self) -> Result<(), String> {
        let pid = self.process.write().take();
        
        if let Some(pid) = pid {
            #[cfg(target_os = "windows")]
            {
                use std::process::Command;
                let _ = Command::new("taskkill")
                    .args(["/PID", &pid.to_string(), "/F"])
                    .output();
            }
            
            #[cfg(not(target_os = "windows"))]
            {
                use std::process::Command;
                let _ = Command::new("kill")
                    .arg(pid.to_string())
                    .output();
            }
            
            *self.start_time.write() = None;
            tracing::info!("Stopped local server (PID {})", pid);
        }
        
        Ok(())
    }
    
    pub fn is_running(&self) -> bool {
        self.process.read().is_some()
    }
    
    pub fn get_status(&self) -> HostingStatus {
        let config = self.config.read();
        let running = self.is_running();
        
        let uptime = self.start_time.read()
            .map(|t| t.elapsed().as_secs())
            .unwrap_or(0);
        
        HostingStatus {
            running,
            world_name: config.world_path.file_name()
                .and_then(|n| n.to_str())
                .unwrap_or("Unknown")
                .to_string(),
            players_online: 0,
            max_players: config.max_players,
            uptime_seconds: uptime,
            port: config.port,
        }
    }
}
