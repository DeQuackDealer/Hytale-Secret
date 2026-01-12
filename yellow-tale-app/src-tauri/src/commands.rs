use crate::state::{AppState, User, PerformanceSettings, EquippedCosmetics, CosmeticItem, HytaleInstallation, JavaInstallation};
use crate::optimizer::{
    OptimizationService, OptimizationConfig, OptimizationResult,
    SystemCapabilities, HardwarePreset, PriorityLevel,
    cpu_affinity::CpuInfo,
    memory::MemoryStats,
    frame_monitor::FrameStats,
    asset_cache::CacheStats,
    world_hosting::{WorldHostConfig, NatInfo, HostingStatus},
    save_snapshot::{Snapshot, SnapshotConfig},
    mod_resolver::{ModInfo, ContentProfile, ResolutionResult},
};
use std::path::PathBuf;
use serde::{Deserialize, Serialize};
use std::sync::Arc;
use tauri::State;
use tokio::sync::RwLock;
use uuid::Uuid;

type AppStateHandle = Arc<RwLock<AppState>>;
type OptimizerHandle = Arc<OptimizationService>;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SystemInfo {
    pub os: String,
    pub os_version: String,
    pub cpu: String,
    pub cpu_cores: u32,
    pub ram_total_mb: u64,
    pub ram_available_mb: u64,
    pub gpu: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ServerInfo {
    pub id: Uuid,
    pub name: String,
    pub address: String,
    pub port: u16,
    pub player_count: u32,
    pub max_players: u32,
    pub ping_ms: u32,
    pub version: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Friend {
    pub id: Uuid,
    pub username: String,
    pub display_name: Option<String>,
    pub avatar_url: Option<String>,
    pub online: bool,
    pub playing: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModProfile {
    pub id: String,
    pub name: String,
    pub mods: Vec<String>,
    pub active: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UpdateInfo {
    pub available: bool,
    pub version: Option<String>,
    pub changelog: Option<String>,
    pub download_url: Option<String>,
}

#[tauri::command]
pub async fn get_system_info() -> Result<SystemInfo, String> {
    use sysinfo::System;
    
    let mut sys = System::new_all();
    sys.refresh_all();
    
    let cpu_brand = sys.cpus().first()
        .map(|c| c.brand().to_string())
        .unwrap_or_default();
    
    Ok(SystemInfo {
        os: std::env::consts::OS.to_string(),
        os_version: System::long_os_version().unwrap_or_default(),
        cpu: cpu_brand,
        cpu_cores: sys.cpus().len() as u32,
        ram_total_mb: sys.total_memory() / 1024 / 1024,
        ram_available_mb: sys.available_memory() / 1024 / 1024,
        gpu: None,
    })
}

#[tauri::command]
pub async fn detect_hytale_installation(
    state: State<'_, AppStateHandle>,
    custom_path: Option<String>,
) -> Result<HytaleInstallation, String> {
    let base_path = if let Some(path) = custom_path {
        PathBuf::from(path)
    } else {
        #[cfg(target_os = "windows")]
        {
            if let Some(appdata) = dirs::data_dir() {
                appdata.join("Hytale")
            } else {
                return Err("Could not find AppData directory".to_string());
            }
        }
        #[cfg(target_os = "macos")]
        {
            if let Some(home) = dirs::home_dir() {
                home.join("Library").join("Application Support").join("Hytale")
            } else {
                return Err("Could not find home directory".to_string());
            }
        }
        #[cfg(target_os = "linux")]
        {
            if let Some(data) = dirs::data_dir() {
                data.join("Hytale")
            } else {
                return Err("Could not find data directory".to_string());
            }
        }
    };

    let install_path = base_path.join("install").join("release").join("package").join("game").join("latest");
    let client_path = install_path.join("Client");
    let server_path = install_path.join("Server");
    let assets_path = install_path.join("Assets.zip");
    let user_data_path = base_path.join("UserData");
    let packs_path = user_data_path.join("Packs");

    let valid = client_path.exists() || assets_path.exists();

    let installation = HytaleInstallation {
        path: base_path.to_string_lossy().to_string(),
        client_path: client_path.to_string_lossy().to_string(),
        server_path: server_path.to_string_lossy().to_string(),
        assets_path: assets_path.to_string_lossy().to_string(),
        user_data_path: user_data_path.to_string_lossy().to_string(),
        packs_path: packs_path.to_string_lossy().to_string(),
        version: None,
        valid,
    };

    if valid {
        let mut s = state.write().await;
        s.game_path = Some(base_path.to_string_lossy().to_string());
    }

    Ok(installation)
}

#[tauri::command]
pub async fn detect_java_installation(
    state: State<'_, AppStateHandle>,
    custom_path: Option<String>,
) -> Result<JavaInstallation, String> {
    use std::process::Command;

    let java_exe = if let Some(path) = custom_path {
        PathBuf::from(path)
    } else {
        let possible_paths = get_java_search_paths();
        
        let mut found_path = None;
        for path in possible_paths {
            if path.exists() {
                found_path = Some(path);
                break;
            }
        }
        
        if let Some(path) = found_path {
            path
        } else {
            if let Ok(output) = Command::new("java").arg("-version").output() {
                if output.status.success() {
                    PathBuf::from("java")
                } else {
                    return Ok(JavaInstallation {
                        path: String::new(),
                        version: String::new(),
                        vendor: String::new(),
                        is_temurin: false,
                        is_java_25: false,
                        valid: false,
                    });
                }
            } else {
                return Ok(JavaInstallation {
                    path: String::new(),
                    version: String::new(),
                    vendor: String::new(),
                    is_temurin: false,
                    is_java_25: false,
                    valid: false,
                });
            }
        }
    };

    let output = Command::new(&java_exe)
        .arg("-version")
        .output()
        .map_err(|e| format!("Failed to run Java: {}", e))?;

    let version_output = String::from_utf8_lossy(&output.stderr).to_string();
    
    let version = extract_java_version(&version_output);
    let vendor = extract_java_vendor(&version_output);
    let is_temurin = vendor.to_lowercase().contains("temurin") || 
                     vendor.to_lowercase().contains("adoptium") ||
                     version_output.to_lowercase().contains("temurin");
    let is_java_25 = version.starts_with("25");
    let valid = output.status.success();

    let installation = JavaInstallation {
        path: java_exe.to_string_lossy().to_string(),
        version,
        vendor,
        is_temurin,
        is_java_25,
        valid,
    };

    if valid {
        let mut s = state.write().await;
        s.java_path = Some(java_exe.to_string_lossy().to_string());
    }

    Ok(installation)
}

fn get_java_search_paths() -> Vec<PathBuf> {
    let mut paths = Vec::new();
    
    #[cfg(target_os = "windows")]
    {
        if let Ok(program_files) = std::env::var("ProgramFiles") {
            paths.push(PathBuf::from(&program_files).join("Eclipse Adoptium").join("jdk-25").join("bin").join("java.exe"));
            paths.push(PathBuf::from(&program_files).join("Eclipse Adoptium").join("jdk-25-lts").join("bin").join("java.exe"));
            paths.push(PathBuf::from(&program_files).join("Java").join("jdk-25").join("bin").join("java.exe"));
            paths.push(PathBuf::from(&program_files).join("Temurin").join("jdk-25").join("bin").join("java.exe"));
        }
        if let Ok(java_home) = std::env::var("JAVA_HOME") {
            paths.push(PathBuf::from(&java_home).join("bin").join("java.exe"));
        }
        if let Some(local) = dirs::data_local_dir() {
            paths.push(local.join("Programs").join("Eclipse Adoptium").join("jdk-25").join("bin").join("java.exe"));
        }
    }
    
    #[cfg(target_os = "macos")]
    {
        paths.push(PathBuf::from("/Library/Java/JavaVirtualMachines/temurin-25/Contents/Home/bin/java"));
        paths.push(PathBuf::from("/opt/homebrew/opt/openjdk@25/bin/java"));
        if let Ok(java_home) = std::env::var("JAVA_HOME") {
            paths.push(PathBuf::from(&java_home).join("bin").join("java"));
        }
    }
    
    #[cfg(target_os = "linux")]
    {
        paths.push(PathBuf::from("/usr/lib/jvm/temurin-25/bin/java"));
        paths.push(PathBuf::from("/usr/lib/jvm/java-25-openjdk/bin/java"));
        if let Ok(java_home) = std::env::var("JAVA_HOME") {
            paths.push(PathBuf::from(&java_home).join("bin").join("java"));
        }
    }
    
    paths
}

fn extract_java_version(output: &str) -> String {
    for line in output.lines() {
        if line.contains("version") {
            if let Some(start) = line.find('"') {
                if let Some(end) = line[start + 1..].find('"') {
                    return line[start + 1..start + 1 + end].to_string();
                }
            }
        }
    }
    String::new()
}

fn extract_java_vendor(output: &str) -> String {
    for line in output.lines() {
        if line.contains("Runtime") || line.contains("OpenJDK") || line.contains("Java(TM)") {
            return line.trim().to_string();
        }
    }
    output.lines().next().unwrap_or("Unknown").to_string()
}

#[tauri::command]
pub async fn set_game_path(
    state: State<'_, AppStateHandle>,
    path: String,
) -> Result<HytaleInstallation, String> {
    detect_hytale_installation(state, Some(path)).await
}

#[tauri::command]
pub async fn set_java_path(
    state: State<'_, AppStateHandle>,
    path: String,
) -> Result<JavaInstallation, String> {
    detect_java_installation(state, Some(path)).await
}

#[tauri::command]
pub async fn get_launcher_status(
    state: State<'_, AppStateHandle>,
) -> Result<LauncherStatus, String> {
    let s = state.read().await;
    
    Ok(LauncherStatus {
        game_path: s.game_path.clone(),
        java_path: s.java_path.clone(),
        game_running: s.game_running,
        user_logged_in: s.user.is_some(),
        ram_allocation_mb: s.performance.ram_allocation_mb,
    })
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LauncherStatus {
    pub game_path: Option<String>,
    pub java_path: Option<String>,
    pub game_running: bool,
    pub user_logged_in: bool,
    pub ram_allocation_mb: u32,
}

#[tauri::command]
pub async fn login(
    state: State<'_, AppStateHandle>,
    username: String,
    password: String,
) -> Result<User, String> {
    let api_url = {
        let s = state.read().await;
        s.api_url.clone()
    };
    
    let client = reqwest::Client::new();
    let res = client
        .post(format!("{}/api/v1/auth/login", api_url))
        .json(&serde_json::json!({
            "username": username,
            "password": password
        }))
        .send()
        .await
        .map_err(|e| e.to_string())?;
    
    let data: serde_json::Value = res.json().await.map_err(|e| e.to_string())?;
    
    if !data["success"].as_bool().unwrap_or(false) {
        return Err(data["error"].as_str().unwrap_or("Login failed").to_string());
    }
    
    let user_data = &data["data"]["user"];
    let token = data["data"]["token"].as_str().unwrap_or("").to_string();
    
    let user = User {
        id: Uuid::parse_str(user_data["id"].as_str().unwrap_or("")).unwrap_or_default(),
        username: user_data["username"].as_str().unwrap_or("").to_string(),
        display_name: user_data["display_name"].as_str().map(|s| s.to_string()),
        avatar_url: user_data["avatar_url"].as_str().map(|s| s.to_string()),
        premium: user_data["premium"].as_bool().unwrap_or(false),
        equipped_cosmetics: EquippedCosmetics::default(),
    };
    
    {
        let mut s = state.write().await;
        s.user = Some(user.clone());
        s.token = Some(token);
    }
    
    Ok(user)
}

#[tauri::command]
pub async fn logout(state: State<'_, AppStateHandle>) -> Result<(), String> {
    let (api_url, token) = {
        let s = state.read().await;
        (s.api_url.clone(), s.token.clone())
    };
    
    if let Some(token) = token {
        let client = reqwest::Client::new();
        let _ = client
            .post(format!("{}/api/v1/auth/logout", api_url))
            .json(&serde_json::json!({ "token": token }))
            .send()
            .await;
    }
    
    {
        let mut s = state.write().await;
        s.user = None;
        s.token = None;
    }
    
    Ok(())
}

#[tauri::command]
pub async fn get_user(state: State<'_, AppStateHandle>) -> Result<Option<User>, String> {
    let s = state.read().await;
    Ok(s.user.clone())
}

#[tauri::command]
pub async fn get_servers(state: State<'_, AppStateHandle>) -> Result<Vec<ServerInfo>, String> {
    let api_url = {
        let s = state.read().await;
        s.api_url.clone()
    };
    
    let client = reqwest::Client::new();
    let res = client
        .get(format!("{}/api/v1/servers", api_url))
        .send()
        .await
        .map_err(|e| e.to_string())?;
    
    let data: serde_json::Value = res.json().await.map_err(|e| e.to_string())?;
    
    if !data["success"].as_bool().unwrap_or(false) {
        return Ok(vec![]);
    }
    
    let servers: Vec<ServerInfo> = serde_json::from_value(data["data"].clone())
        .unwrap_or_default();
    
    Ok(servers)
}

#[tauri::command]
pub async fn launch_game(
    state: State<'_, AppStateHandle>,
    server_address: Option<String>,
) -> Result<(), String> {
    use std::process::Command;
    
    let (game_path, java_path, performance) = {
        let s = state.read().await;
        
        if s.game_running {
            return Err("Game is already running".to_string());
        }
        
        (s.game_path.clone(), s.java_path.clone(), s.performance.clone())
    };
    
    let game_path = game_path.ok_or("Hytale installation not found. Please set the game path in Settings.")?;
    let java_path = java_path.ok_or("Java installation not found. Please install Temurin Java 25 LTS.")?;
    
    let client_path = PathBuf::from(&game_path)
        .join("install")
        .join("release")
        .join("package")
        .join("game")
        .join("latest")
        .join("Client");
    
    if !client_path.exists() {
        return Err(format!("Hytale client not found at: {:?}", client_path));
    }
    
    let ram_mb = performance.ram_allocation_mb;
    let xmx = format!("-Xmx{}m", ram_mb);
    let xms = format!("-Xms{}m", ram_mb / 2);
    
    let mut jvm_args = vec![
        xmx,
        xms,
        "-XX:+UseG1GC".to_string(),
        "-XX:+ParallelRefProcEnabled".to_string(),
        "-XX:MaxGCPauseMillis=200".to_string(),
        "-XX:+UnlockExperimentalVMOptions".to_string(),
        "-XX:+DisableExplicitGC".to_string(),
        "-XX:G1NewSizePercent=30".to_string(),
        "-XX:G1MaxNewSizePercent=40".to_string(),
        "-XX:G1HeapRegionSize=8M".to_string(),
        "-XX:G1ReservePercent=20".to_string(),
        "-XX:G1HeapWastePercent=5".to_string(),
        "-XX:G1MixedGCCountTarget=4".to_string(),
        "-XX:InitiatingHeapOccupancyPercent=15".to_string(),
        "-XX:G1MixedGCLiveThresholdPercent=90".to_string(),
        "-XX:G1RSetUpdatingPauseTimePercent=5".to_string(),
        "-XX:SurvivorRatio=32".to_string(),
        "-XX:+PerfDisableSharedMem".to_string(),
        "-XX:MaxTenuringThreshold=1".to_string(),
    ];
    
    if let Some(fps) = performance.fps_limit {
        jvm_args.push(format!("-Dhytale.fpsLimit={}", fps));
    }
    
    let mut cmd = Command::new(&java_path);
    cmd.current_dir(&client_path);
    
    for arg in &jvm_args {
        cmd.arg(arg);
    }
    
    cmd.arg("-jar");
    cmd.arg("HytaleClient.jar");
    
    if let Some(server) = server_address {
        cmd.arg("--server");
        cmd.arg(server);
    }
    
    match cmd.spawn() {
        Ok(_child) => {
            let mut s = state.write().await;
            s.game_running = true;
            Ok(())
        }
        Err(e) => Err(format!("Failed to launch game: {}", e)),
    }
}

#[tauri::command]
pub async fn get_friends(state: State<'_, AppStateHandle>) -> Result<Vec<Friend>, String> {
    let (api_url, token) = {
        let s = state.read().await;
        (s.api_url.clone(), s.token.clone())
    };
    
    let token = token.ok_or("Not logged in")?;
    
    let client = reqwest::Client::new();
    let res = client
        .post(format!("{}/api/v1/friends", api_url))
        .json(&serde_json::json!({ "token": token }))
        .send()
        .await
        .map_err(|e| e.to_string())?;
    
    let data: serde_json::Value = res.json().await.map_err(|e| e.to_string())?;
    
    if !data["success"].as_bool().unwrap_or(false) {
        return Ok(vec![]);
    }
    
    let friends: Vec<Friend> = serde_json::from_value(data["data"]["friends"].clone())
        .unwrap_or_default();
    
    Ok(friends)
}

#[tauri::command]
pub async fn get_mod_profiles(state: State<'_, AppStateHandle>) -> Result<Vec<ModProfile>, String> {
    let (api_url, token) = {
        let s = state.read().await;
        (s.api_url.clone(), s.token.clone())
    };
    
    let token = token.ok_or("Not logged in")?;
    
    let client = reqwest::Client::new();
    let res = client
        .post(format!("{}/api/v1/mods/profiles", api_url))
        .json(&serde_json::json!({ "token": token }))
        .send()
        .await
        .map_err(|e| e.to_string())?;
    
    let data: serde_json::Value = res.json().await.map_err(|e| e.to_string())?;
    
    if !data["success"].as_bool().unwrap_or(false) {
        return Ok(vec![]);
    }
    
    let profiles: Vec<ModProfile> = serde_json::from_value(data["data"]["profiles"].clone())
        .unwrap_or_default();
    
    Ok(profiles)
}

#[tauri::command]
pub async fn check_for_updates(state: State<'_, AppStateHandle>) -> Result<UpdateInfo, String> {
    let api_url = {
        let s = state.read().await;
        s.api_url.clone()
    };
    
    let client = reqwest::Client::new();
    let res = client
        .get(format!("{}/api/v1/releases", api_url))
        .send()
        .await
        .map_err(|e| e.to_string())?;
    
    let data: serde_json::Value = res.json().await.map_err(|e| e.to_string())?;
    
    Ok(UpdateInfo {
        available: data["update_available"].as_bool().unwrap_or(false),
        version: data["latest_version"].as_str().map(|s| s.to_string()),
        changelog: data["changelog"].as_str().map(|s| s.to_string()),
        download_url: data["download_url"].as_str().map(|s| s.to_string()),
    })
}

#[tauri::command]
pub async fn get_performance_settings(
    state: State<'_, AppStateHandle>,
) -> Result<PerformanceSettings, String> {
    let s = state.read().await;
    Ok(s.performance.clone())
}

#[tauri::command]
pub async fn update_performance_settings(
    state: State<'_, AppStateHandle>,
    settings: PerformanceSettings,
) -> Result<(), String> {
    let mut s = state.write().await;
    s.performance = settings;
    Ok(())
}

#[tauri::command]
pub async fn get_feature_gates(state: State<'_, AppStateHandle>) -> Result<serde_json::Value, String> {
    let (api_url, token) = {
        let s = state.read().await;
        (s.api_url.clone(), s.token.clone())
    };
    
    let token = token.unwrap_or_default();
    
    let client = reqwest::Client::new();
    let res = client
        .post(format!("{}/api/v1/features", api_url))
        .json(&serde_json::json!({ "token": token }))
        .send()
        .await
        .map_err(|e| e.to_string())?;
    
    let data: serde_json::Value = res.json().await.map_err(|e| e.to_string())?;
    
    Ok(data)
}

#[tauri::command]
pub async fn get_cosmetics(state: State<'_, AppStateHandle>) -> Result<Vec<CosmeticItem>, String> {
    let (api_url, token) = {
        let s = state.read().await;
        (s.api_url.clone(), s.token.clone())
    };
    
    let token = token.ok_or("Not logged in")?;
    
    let client = reqwest::Client::new();
    let res = client
        .post(format!("{}/api/v1/cosmetics", api_url))
        .json(&serde_json::json!({ "token": token }))
        .send()
        .await
        .map_err(|e| e.to_string())?;
    
    let data: serde_json::Value = res.json().await.map_err(|e| e.to_string())?;
    
    if !data["success"].as_bool().unwrap_or(false) {
        return Ok(vec![]);
    }
    
    let cosmetics: Vec<CosmeticItem> = serde_json::from_value(data["data"]["items"].clone())
        .unwrap_or_default();
    
    {
        let mut s = state.write().await;
        s.owned_cosmetics = cosmetics.clone();
    }
    
    Ok(cosmetics)
}

#[tauri::command]
pub async fn equip_cosmetic(
    state: State<'_, AppStateHandle>,
    item_id: String,
    slot: String,
) -> Result<(), String> {
    let (api_url, token) = {
        let s = state.read().await;
        (s.api_url.clone(), s.token.clone())
    };
    
    let token = token.ok_or("Not logged in")?;
    
    let client = reqwest::Client::new();
    let res = client
        .post(format!("{}/api/v1/cosmetics/equip", api_url))
        .json(&serde_json::json!({ 
            "token": token,
            "item_id": item_id,
            "slot": slot
        }))
        .send()
        .await
        .map_err(|e| e.to_string())?;
    
    let data: serde_json::Value = res.json().await.map_err(|e| e.to_string())?;
    
    if !data["success"].as_bool().unwrap_or(false) {
        return Err(data["error"].as_str().unwrap_or("Failed to equip").to_string());
    }
    
    {
        let mut s = state.write().await;
        if let Some(ref mut user) = s.user {
            match slot.as_str() {
                "skin" => user.equipped_cosmetics.skin = Some(item_id),
                "emote_1" => user.equipped_cosmetics.emote_1 = Some(item_id),
                "emote_2" => user.equipped_cosmetics.emote_2 = Some(item_id),
                "emote_3" => user.equipped_cosmetics.emote_3 = Some(item_id),
                "emote_4" => user.equipped_cosmetics.emote_4 = Some(item_id),
                "cape" => user.equipped_cosmetics.cape = Some(item_id),
                "wings" => user.equipped_cosmetics.wings = Some(item_id),
                "aura" => user.equipped_cosmetics.aura = Some(item_id),
                _ => {}
            }
        }
    }
    
    Ok(())
}

#[tauri::command]
pub async fn unequip_cosmetic(
    state: State<'_, AppStateHandle>,
    slot: String,
) -> Result<(), String> {
    let (api_url, token) = {
        let s = state.read().await;
        (s.api_url.clone(), s.token.clone())
    };
    
    let token = token.ok_or("Not logged in")?;
    
    let client = reqwest::Client::new();
    let res = client
        .post(format!("{}/api/v1/cosmetics/unequip", api_url))
        .json(&serde_json::json!({ 
            "token": token,
            "slot": slot
        }))
        .send()
        .await
        .map_err(|e| e.to_string())?;
    
    let data: serde_json::Value = res.json().await.map_err(|e| e.to_string())?;
    
    if !data["success"].as_bool().unwrap_or(false) {
        return Err(data["error"].as_str().unwrap_or("Failed to unequip").to_string());
    }
    
    {
        let mut s = state.write().await;
        if let Some(ref mut user) = s.user {
            match slot.as_str() {
                "skin" => user.equipped_cosmetics.skin = None,
                "emote_1" => user.equipped_cosmetics.emote_1 = None,
                "emote_2" => user.equipped_cosmetics.emote_2 = None,
                "emote_3" => user.equipped_cosmetics.emote_3 = None,
                "emote_4" => user.equipped_cosmetics.emote_4 = None,
                "cape" => user.equipped_cosmetics.cape = None,
                "wings" => user.equipped_cosmetics.wings = None,
                "aura" => user.equipped_cosmetics.aura = None,
                _ => {}
            }
        }
    }
    
    Ok(())
}

#[tauri::command]
pub async fn get_equipped_cosmetics(state: State<'_, AppStateHandle>) -> Result<EquippedCosmetics, String> {
    let (api_url, token) = {
        let s = state.read().await;
        (s.api_url.clone(), s.token.clone())
    };
    
    let token = match token {
        Some(t) => t,
        None => return Ok(EquippedCosmetics::default()),
    };
    
    let client = reqwest::Client::new();
    let res = client
        .post(format!("{}/api/v1/cosmetics/equipped", api_url))
        .json(&serde_json::json!({ "token": token }))
        .send()
        .await
        .map_err(|e| e.to_string())?;
    
    let data: serde_json::Value = res.json().await.map_err(|e| e.to_string())?;
    
    if !data["success"].as_bool().unwrap_or(false) {
        let s = state.read().await;
        if let Some(ref user) = s.user {
            return Ok(user.equipped_cosmetics.clone());
        }
        return Ok(EquippedCosmetics::default());
    }
    
    let equipped_data = &data["data"]["equipped"];
    let mut equipped = EquippedCosmetics::default();
    
    if let Some(obj) = equipped_data.as_object() {
        for (slot, item_id) in obj {
            if let Some(id) = item_id.as_str() {
                match slot.as_str() {
                    "skin" => equipped.skin = Some(id.to_string()),
                    "emote_1" => equipped.emote_1 = Some(id.to_string()),
                    "emote_2" => equipped.emote_2 = Some(id.to_string()),
                    "emote_3" => equipped.emote_3 = Some(id.to_string()),
                    "emote_4" => equipped.emote_4 = Some(id.to_string()),
                    "cape" => equipped.cape = Some(id.to_string()),
                    "wings" => equipped.wings = Some(id.to_string()),
                    "aura" => equipped.aura = Some(id.to_string()),
                    _ => {}
                }
            }
        }
    }
    
    {
        let mut s = state.write().await;
        if let Some(ref mut user) = s.user {
            user.equipped_cosmetics = equipped.clone();
        }
    }
    
    Ok(equipped)
}

#[tauri::command]
pub async fn get_user_public_cosmetics(
    state: State<'_, AppStateHandle>,
    user_id: String,
) -> Result<EquippedCosmetics, String> {
    let api_url = {
        let s = state.read().await;
        s.api_url.clone()
    };
    
    let user_uuid = uuid::Uuid::parse_str(&user_id).map_err(|e| e.to_string())?;
    
    let client = reqwest::Client::new();
    let res = client
        .post(format!("{}/api/v1/cosmetics/user", api_url))
        .json(&serde_json::json!({ "user_id": user_uuid }))
        .send()
        .await
        .map_err(|e| e.to_string())?;
    
    let data: serde_json::Value = res.json().await.map_err(|e| e.to_string())?;
    
    if !data["success"].as_bool().unwrap_or(false) {
        return Err(data["error"].as_str().unwrap_or("Failed to fetch user cosmetics").to_string());
    }
    
    let equipped_data = &data["data"]["equipped"];
    let mut equipped = EquippedCosmetics::default();
    
    if let Some(obj) = equipped_data.as_object() {
        for (slot, item_id) in obj {
            if let Some(id) = item_id.as_str() {
                match slot.as_str() {
                    "skin" => equipped.skin = Some(id.to_string()),
                    "emote_1" => equipped.emote_1 = Some(id.to_string()),
                    "emote_2" => equipped.emote_2 = Some(id.to_string()),
                    "emote_3" => equipped.emote_3 = Some(id.to_string()),
                    "emote_4" => equipped.emote_4 = Some(id.to_string()),
                    "cape" => equipped.cape = Some(id.to_string()),
                    "wings" => equipped.wings = Some(id.to_string()),
                    "aura" => equipped.aura = Some(id.to_string()),
                    _ => {}
                }
            }
        }
    }
    
    Ok(equipped)
}

#[tauri::command]
pub async fn detect_system_capabilities(
    optimizer: State<'_, OptimizerHandle>,
) -> Result<SystemCapabilities, String> {
    Ok(optimizer.detect_capabilities())
}

#[tauri::command]
pub async fn get_optimization_config(
    optimizer: State<'_, OptimizerHandle>,
) -> Result<OptimizationConfig, String> {
    Ok(optimizer.get_config())
}

#[tauri::command]
pub async fn update_optimization_config(
    optimizer: State<'_, OptimizerHandle>,
    config: OptimizationConfig,
) -> Result<(), String> {
    optimizer.update_config(config);
    Ok(())
}

#[tauri::command]
pub async fn get_hardware_presets(
    optimizer: State<'_, OptimizerHandle>,
) -> Result<Vec<HardwarePreset>, String> {
    Ok(optimizer.get_available_presets())
}

#[tauri::command]
pub async fn apply_hardware_preset(
    optimizer: State<'_, OptimizerHandle>,
    preset_name: String,
) -> Result<OptimizationConfig, String> {
    optimizer.apply_preset(&preset_name)?;
    Ok(optimizer.get_config())
}

#[tauri::command]
pub async fn get_recommended_preset(
    optimizer: State<'_, OptimizerHandle>,
) -> Result<Option<String>, String> {
    let _ = optimizer.detect_capabilities();
    Ok(optimizer.get_recommended_preset())
}

#[tauri::command]
pub async fn get_cpu_cores(
    optimizer: State<'_, OptimizerHandle>,
) -> Result<Vec<CpuInfo>, String> {
    Ok(optimizer.cpu_affinity().get_cores())
}

#[tauri::command]
pub async fn get_memory_stats(
    optimizer: State<'_, OptimizerHandle>,
) -> Result<MemoryStats, String> {
    Ok(optimizer.memory_optimizer().refresh_stats())
}

#[tauri::command]
pub async fn get_frame_stats(
    optimizer: State<'_, OptimizerHandle>,
) -> Result<FrameStats, String> {
    Ok(optimizer.frame_monitor().get_stats())
}

#[tauri::command]
pub async fn get_cache_stats(
    optimizer: State<'_, OptimizerHandle>,
) -> Result<CacheStats, String> {
    Ok(optimizer.asset_cache().get_stats())
}

#[tauri::command]
pub async fn get_suppressible_processes(
    optimizer: State<'_, OptimizerHandle>,
) -> Result<Vec<(u32, String)>, String> {
    Ok(optimizer.background_suppressor().get_suppressible_processes())
}

#[tauri::command]
pub async fn optimize_for_launch(
    optimizer: State<'_, OptimizerHandle>,
    process_id: u32,
) -> Result<OptimizationResult, String> {
    optimizer.optimize_for_launch(process_id).await
}

#[tauri::command]
pub async fn restore_after_exit(
    optimizer: State<'_, OptimizerHandle>,
) -> Result<(), String> {
    optimizer.restore_after_exit().await
}

#[tauri::command]
pub async fn detect_nat(
    optimizer: State<'_, OptimizerHandle>,
) -> Result<NatInfo, String> {
    optimizer.world_host().detect_nat().await
}

#[tauri::command]
pub async fn get_world_host_config(
    optimizer: State<'_, OptimizerHandle>,
) -> Result<WorldHostConfig, String> {
    Ok(optimizer.world_host().get_config())
}

#[tauri::command]
pub async fn set_world_host_config(
    optimizer: State<'_, OptimizerHandle>,
    config: WorldHostConfig,
) -> Result<(), String> {
    optimizer.world_host().set_config(config);
    Ok(())
}

#[tauri::command]
pub async fn start_local_server(
    optimizer: State<'_, OptimizerHandle>,
    game_executable: String,
) -> Result<u32, String> {
    let path = PathBuf::from(game_executable);
    optimizer.world_host().start_server(&path).await
}

#[tauri::command]
pub async fn stop_local_server(
    optimizer: State<'_, OptimizerHandle>,
) -> Result<(), String> {
    optimizer.world_host().stop_server().await
}

#[tauri::command]
pub async fn get_hosting_status(
    optimizer: State<'_, OptimizerHandle>,
) -> Result<HostingStatus, String> {
    Ok(optimizer.world_host().get_status())
}

#[tauri::command]
pub async fn initialize_snapshots(
    optimizer: State<'_, OptimizerHandle>,
) -> Result<(), String> {
    optimizer.snapshot_manager().initialize().await
}

#[tauri::command]
pub async fn get_snapshot_config(
    optimizer: State<'_, OptimizerHandle>,
) -> Result<SnapshotConfig, String> {
    Ok(optimizer.snapshot_manager().get_config())
}

#[tauri::command]
pub async fn set_snapshot_config(
    optimizer: State<'_, OptimizerHandle>,
    config: SnapshotConfig,
) -> Result<(), String> {
    optimizer.snapshot_manager().set_config(config);
    Ok(())
}

#[tauri::command]
pub async fn create_snapshot(
    optimizer: State<'_, OptimizerHandle>,
    world_path: String,
    name: String,
    description: Option<String>,
) -> Result<Snapshot, String> {
    let path = PathBuf::from(world_path);
    optimizer.snapshot_manager().create_snapshot(&path, &name, description).await
}

#[tauri::command]
pub async fn restore_snapshot(
    optimizer: State<'_, OptimizerHandle>,
    snapshot_id: String,
    target_path: String,
) -> Result<(), String> {
    let path = PathBuf::from(target_path);
    optimizer.snapshot_manager().restore_snapshot(&snapshot_id, &path).await
}

#[tauri::command]
pub async fn get_snapshots(
    optimizer: State<'_, OptimizerHandle>,
) -> Result<Vec<Snapshot>, String> {
    if let Err(e) = optimizer.snapshot_manager().initialize().await {
        tracing::warn!("Failed to initialize snapshots: {}", e);
    }
    Ok(optimizer.snapshot_manager().get_snapshots())
}

#[tauri::command]
pub async fn delete_snapshot(
    optimizer: State<'_, OptimizerHandle>,
    snapshot_id: String,
) -> Result<(), String> {
    optimizer.snapshot_manager().delete_snapshot(&snapshot_id).await
}

#[tauri::command]
pub async fn scan_mods(
    optimizer: State<'_, OptimizerHandle>,
) -> Result<Vec<ModInfo>, String> {
    optimizer.mod_resolver().scan_mods().await.map_err(|e| format!("Failed to scan mods: {}", e))
}

#[tauri::command]
pub async fn get_installed_mods(
    optimizer: State<'_, OptimizerHandle>,
) -> Result<Vec<ModInfo>, String> {
    let mods = optimizer.mod_resolver().get_mods();
    if mods.is_empty() {
        if let Err(e) = optimizer.mod_resolver().scan_mods().await {
            tracing::warn!("Auto-scan failed: {}", e);
        }
    }
    Ok(optimizer.mod_resolver().get_mods())
}

#[tauri::command]
pub async fn get_mod_info(
    optimizer: State<'_, OptimizerHandle>,
    mod_id: String,
) -> Result<Option<ModInfo>, String> {
    Ok(optimizer.mod_resolver().get_mod(&mod_id))
}

#[tauri::command]
pub async fn set_mod_enabled(
    optimizer: State<'_, OptimizerHandle>,
    mod_id: String,
    enabled: bool,
) -> Result<(), String> {
    optimizer.mod_resolver().set_mod_enabled(&mod_id, enabled)
}

#[tauri::command]
pub async fn resolve_mod_dependencies(
    optimizer: State<'_, OptimizerHandle>,
    mod_ids: Vec<String>,
) -> Result<ResolutionResult, String> {
    Ok(optimizer.mod_resolver().resolve_dependencies(&mod_ids))
}

#[tauri::command]
pub async fn create_mod_profile(
    optimizer: State<'_, OptimizerHandle>,
    name: String,
    enabled_mods: Vec<String>,
) -> Result<ContentProfile, String> {
    Ok(optimizer.mod_resolver().create_profile(&name, enabled_mods))
}

#[tauri::command]
pub async fn get_local_mod_profiles(
    optimizer: State<'_, OptimizerHandle>,
) -> Result<Vec<ContentProfile>, String> {
    Ok(optimizer.mod_resolver().get_profiles())
}

#[tauri::command]
pub async fn activate_mod_profile(
    optimizer: State<'_, OptimizerHandle>,
    profile_id: String,
) -> Result<ResolutionResult, String> {
    optimizer.mod_resolver().activate_profile(&profile_id)
}

#[tauri::command]
pub async fn delete_mod_profile(
    optimizer: State<'_, OptimizerHandle>,
    profile_id: String,
) -> Result<(), String> {
    optimizer.mod_resolver().delete_profile(&profile_id)
}

#[tauri::command]
pub async fn set_mod_directory(
    optimizer: State<'_, OptimizerHandle>,
    path: String,
) -> Result<(), String> {
    optimizer.mod_resolver().set_mod_directory(PathBuf::from(path));
    Ok(())
}
