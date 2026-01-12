pub mod core;

pub use core::game::{ServerAdapter, GameHook, HookPriority, WorldProvider};
pub use core::game::adapter::HytaleServerAdapter;
pub use core::server::Server;
pub use core::plugins::{Plugin, PluginManager, PluginMetadata};
pub use core::scheduler::{Scheduler, Task, TaskPriority};
pub use core::performance::PerformanceMonitor;
pub use core::assets::{AssetRegistry, Cosmetic, CosmeticScope};
pub use core::config::ConfigManager;
pub use core::telemetry::TelemetryCollector;
pub use core::integration::{
    LauncherBridge, ServerCapabilities, ConnectivityFeatures, 
    SyncCapabilities, PlayerActivity, PlayerStatus, QueueEntry,
    AssetPreloadManifest, NetworkOptimizationHints,
};
