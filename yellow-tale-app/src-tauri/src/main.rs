#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use std::sync::Arc;
use tokio::sync::RwLock;

mod commands;
mod state;
mod optimizer;
mod future;

use state::AppState;
use optimizer::OptimizationService;

pub type AppStateHandle = Arc<RwLock<AppState>>;
pub type OptimizerHandle = Arc<OptimizationService>;

fn main() {
    tracing_subscriber::fmt::init();
    
    let state = Arc::new(RwLock::new(AppState::new()));
    let optimizer = Arc::new(OptimizationService::new());
    
    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_fs::init())
        .plugin(tauri_plugin_process::init())
        .manage(state)
        .manage(optimizer)
        .invoke_handler(tauri::generate_handler![
            commands::get_system_info,
            commands::login,
            commands::logout,
            commands::get_user,
            commands::get_servers,
            commands::launch_game,
            commands::get_friends,
            commands::get_mod_profiles,
            commands::check_for_updates,
            commands::get_performance_settings,
            commands::update_performance_settings,
            commands::get_feature_gates,
            commands::get_cosmetics,
            commands::equip_cosmetic,
            commands::unequip_cosmetic,
            commands::get_equipped_cosmetics,
            commands::get_user_public_cosmetics,
            commands::detect_system_capabilities,
            commands::get_optimization_config,
            commands::update_optimization_config,
            commands::get_hardware_presets,
            commands::apply_hardware_preset,
            commands::get_recommended_preset,
            commands::get_cpu_cores,
            commands::get_memory_stats,
            commands::get_frame_stats,
            commands::get_cache_stats,
            commands::get_suppressible_processes,
            commands::optimize_for_launch,
            commands::restore_after_exit,
            commands::detect_nat,
            commands::get_world_host_config,
            commands::set_world_host_config,
            commands::start_local_server,
            commands::stop_local_server,
            commands::get_hosting_status,
            commands::initialize_snapshots,
            commands::get_snapshot_config,
            commands::set_snapshot_config,
            commands::create_snapshot,
            commands::restore_snapshot,
            commands::get_snapshots,
            commands::delete_snapshot,
            commands::scan_mods,
            commands::get_installed_mods,
            commands::get_mod_info,
            commands::set_mod_enabled,
            commands::resolve_mod_dependencies,
            commands::create_mod_profile,
            commands::get_local_mod_profiles,
            commands::activate_mod_profile,
            commands::delete_mod_profile,
            commands::set_mod_directory,
            commands::detect_hytale_installation,
            commands::detect_java_installation,
            commands::set_game_path,
            commands::set_java_path,
            commands::get_launcher_status,
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
