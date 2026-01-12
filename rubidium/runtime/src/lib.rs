pub mod core;
pub mod anticheat;
pub mod abstraction;
pub mod features;
pub mod bridge;
pub mod bootstrap;
pub mod events;
pub mod admin;
pub mod logging;

pub use core::server::Server;
pub use core::config::ConfigManager;
pub use core::scheduler::{Scheduler, Task, TaskPriority};
pub use core::performance::PerformanceMonitor;
pub use core::plugins::PluginManager;

pub use anticheat::AnticheatService;
pub use abstraction::GameAdapter;

pub use bridge::{GameServerBridge, GameServerConfig, ServerStatus, GameEvent, GameCommand};
pub use bootstrap::{BootstrapOrchestrator, BootstrapPhase, StartupReport};
pub use events::EventBus;
pub use admin::{AdminCli, HealthCheck, HealthStatus};
pub use logging::{LoggingConfig, init_logging};

pub use features::{
    ReplayCapture, ReplayStorage, ReplayPlayer, ReplayCamera, ReplayConfig,
    CaptureFrame, PlaybackState, PlaybackSpeed, CameraMode,
    MappingConfig, MapMode, MinimapService, WorldMapService, 
    MapMarker, MarkerType, MarkerRegistry, MappingCoordinator, MapData,
    WaypointConfig, WaypointService, Waypoint, WaypointVisibility, WaypointIcon,
    FeatureToggleRegistry, FeatureToggle, FeatureStatus, ToggleConfig,
    SocialConfig, PresenceService, PlayerPresence, PresenceStatus,
    PartyService, Party, PartyInvite,
    CinemaConfig, CinemaService, CameraPath, PathKeyframe,
};

pub const VERSION: &str = env!("CARGO_PKG_VERSION");
pub const NAME: &str = "Rubidium";
