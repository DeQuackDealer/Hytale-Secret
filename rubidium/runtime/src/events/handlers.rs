use crate::bridge::GameEvent;
use tracing::{info, debug};

pub fn log_player_events(event: GameEvent) {
    match event {
        GameEvent::PlayerJoin(info) => {
            info!("[+] {} joined the server", info.name);
        }
        GameEvent::PlayerQuit { id, reason } => {
            info!("[-] Player {} left: {}", id, reason);
        }
        GameEvent::PlayerChat { id, message } => {
            debug!("[Chat] {}: {}", id, message);
        }
        _ => {}
    }
}

pub fn log_world_events(event: GameEvent) {
    match event {
        GameEvent::WorldLoad(info) => {
            info!("[World] Loaded: {} ({} spawn at {},{},{})", 
                  info.name, info.dimension, info.spawn_x, info.spawn_y, info.spawn_z);
        }
        GameEvent::WorldUnload { name } => {
            info!("[World] Unloaded: {}", name);
        }
        _ => {}
    }
}

pub fn log_performance_events(event: GameEvent) {
    match event {
        GameEvent::TpsUpdate { tps } => {
            if tps < 18.0 {
                tracing::warn!("[Performance] TPS dropped to {:.1}", tps);
            }
        }
        GameEvent::TickComplete { tick, duration_ms } => {
            if duration_ms > 50.0 {
                debug!("[Performance] Tick {} took {:.1}ms (lag)", tick, duration_ms);
            }
        }
        _ => {}
    }
}
