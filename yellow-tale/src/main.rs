//! Yellow Tale Launcher - Entry Point
//! 
//! This binary serves as the main entry point for the Yellow Tale launcher.
//! It initializes the core systems and provides a CLI interface.

use yellow_tale::core::{
    config::AppConfig,
    telemetry,
    db::Database,
    users::UserService,
    friends::FriendsService,
};
use tracing::{info, warn};
use std::path::PathBuf;
use std::sync::Arc;

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    telemetry::init_logging()?;
    
    info!("Yellow Tale v{} starting...", yellow_tale::VERSION);
    info!("IPC API Version: {}", yellow_tale::IPC_API_VERSION);
    
    let data_dir = get_data_dir();
    tokio::fs::create_dir_all(&data_dir).await.ok();
    
    let config_path = get_config_path();
    let config = match AppConfig::load(&config_path).await {
        Ok(cfg) => {
            info!("Configuration loaded from {:?}", config_path);
            cfg
        }
        Err(e) => {
            info!("Using default configuration: {}", e);
            let default_config = AppConfig::default();
            if let Err(save_err) = default_config.save(&config_path).await {
                info!("Could not save default config: {}", save_err);
            }
            default_config
        }
    };
    
    info!("Initializing core systems...");
    
    let db = match Database::connect().await {
        Ok(db) => {
            info!("Database connected");
            if let Err(e) = db.run_migrations().await {
                warn!("Migration warning: {}", e);
            }
            Some(Arc::new(db))
        }
        Err(e) => {
            warn!("Database not available (offline mode): {}", e);
            None
        }
    };
    
    let (user_service, friends_service) = if let Some(ref db) = db {
        let user_svc = UserService::new(db.pool().clone());
        let friends_svc = FriendsService::new(db.pool().clone());
        info!("User and Friends services initialized");
        (Some(user_svc), Some(friends_svc))
    } else {
        (None, None)
    };
    
    let launcher = yellow_tale::core::launcher::LauncherService::new();
    info!("Launcher service initialized");
    
    let profiles_dir = data_dir.join("profiles");
    let mut profile_manager = yellow_tale::core::profiles::ProfileManager::new(profiles_dir);
    if let Err(e) = profile_manager.load_all().await {
        info!("Could not load profiles: {}", e);
    }
    info!("Profile manager initialized ({} profiles loaded)", profile_manager.list().len());
    
    let cache_dir = data_dir.join("cache");
    let mut cache_manager = yellow_tale::core::cache::CacheManager::new(
        cache_dir,
        config.cache.max_size_bytes,
    );
    if let Err(e) = cache_manager.init().await {
        info!("Could not initialize cache: {}", e);
    }
    let cache_stats = cache_manager.stats();
    info!("Cache manager initialized ({} entries, {} bytes)", 
          cache_stats.entry_count, cache_stats.total_size);
    
    let session_orchestrator = yellow_tale::core::sessions::SessionOrchestrator::new();
    info!("Session orchestrator initialized");
    
    let mut diagnostics = yellow_tale::core::diagnostics::DiagnosticsCollector::new();
    let system_info = diagnostics.get_system_info();
    info!("Diagnostics collector initialized");
    info!("System: {} {} | {} cores | {} MB RAM", 
          system_info.os_name, system_info.os_version,
          system_info.cpu_cores, system_info.total_ram_mb);
    
    let mut ipc_server = yellow_tale::core::ipc::IpcServer::new(
        launcher,
        profile_manager,
        cache_manager,
        session_orchestrator,
        diagnostics,
    ).with_services(user_service, friends_service);
    
    info!("Yellow Tale initialized successfully!");
    
    ipc_server.status().await;
    
    info!("Running self-test...");
    
    let test_request = yellow_tale::core::ipc::IpcRequest {
        id: uuid::Uuid::new_v4(),
        version: yellow_tale::IPC_API_VERSION.to_string(),
        command: "get_version".to_string(),
        params: serde_json::json!({}),
    };
    
    let response = ipc_server.handle(test_request).await;
    if response.success {
        info!("IPC self-test passed: {:?}", response.data);
    }
    
    let metrics_request = yellow_tale::core::ipc::IpcRequest {
        id: uuid::Uuid::new_v4(),
        version: yellow_tale::IPC_API_VERSION.to_string(),
        command: "collect_metrics".to_string(),
        params: serde_json::json!({}),
    };
    
    let metrics_response = ipc_server.handle(metrics_request).await;
    if metrics_response.success {
        if let Some(data) = &metrics_response.data {
            info!("Current CPU usage: {}%", 
                  data.get("cpu_usage").and_then(|v| v.as_f64()).unwrap_or(0.0));
        }
    }
    
    if db.is_some() {
        info!("Database: Connected | Users & Friends: Ready | Relay: Standby");
    } else {
        info!("Database: Offline | Users & Friends: Unavailable | Relay: Standby");
    }
    
    info!("Yellow Tale ready. Awaiting commands...");
    info!("Note: In production, this would start the IPC listener for Tauri UI");
    
    Ok(())
}

/// Get the path to the configuration file
fn get_config_path() -> PathBuf {
    get_data_dir().join("config").join("config.toml")
}

/// Get the application data directory
fn get_data_dir() -> PathBuf {
    // Use standard OS-specific data directories
    #[cfg(target_os = "windows")]
    {
        std::env::var("APPDATA")
            .map(PathBuf::from)
            .unwrap_or_else(|_| PathBuf::from("."))
            .join("YellowTale")
    }
    
    #[cfg(target_os = "macos")]
    {
        std::env::var("HOME")
            .map(PathBuf::from)
            .unwrap_or_else(|_| PathBuf::from("."))
            .join("Library")
            .join("Application Support")
            .join("YellowTale")
    }
    
    #[cfg(target_os = "linux")]
    {
        std::env::var("XDG_DATA_HOME")
            .map(PathBuf::from)
            .unwrap_or_else(|_| {
                std::env::var("HOME")
                    .map(PathBuf::from)
                    .unwrap_or_else(|_| PathBuf::from("."))
                    .join(".local")
                    .join("share")
            })
            .join("yellow-tale")
    }
    
    #[cfg(not(any(target_os = "windows", target_os = "macos", target_os = "linux")))]
    {
        PathBuf::from(".").join("yellow-tale-data")
    }
}
