//! Core module - All system logic for Yellow Tale
//! 
//! This module contains all the core functionality of the launcher:
//! - **game**: Modular game adapter layer (swap implementations for Hytale API)
//! - **features**: Feature toggle system for premium/API-gated functionality
//! - **launcher**: Process lifecycle control for game executables
//! - **profiles**: User profile management and migration
//! - **mods**: Generic mod orchestration (not a mod loader)
//! - **cache**: Content-addressed storage with deduplication
//! - **performance**: Pre-launch optimization (legal & safe)
//! - **diagnostics**: Read-only system metrics collection
//! - **sessions**: Session orchestration and P2P connection handling
//! - **ipc**: UI communication layer
//! - **telemetry**: Logging and metrics
//! - **util**: Shared utilities
//! - **config**: Application configuration
//! - **db**: PostgreSQL database for persistent storage
//! - **users**: User authentication and account management
//! - **friends**: Social features (friends, blocking)
//! - **relay**: WebSocket relay server for tunneling
//! - **client**: HTTP client for central server

pub mod game;
pub mod features;
pub mod launcher;
pub mod profiles;
pub mod mods;
pub mod cache;
pub mod performance;
pub mod diagnostics;
pub mod sessions;
pub mod ipc;
pub mod telemetry;
pub mod util;
pub mod config;
pub mod db;
pub mod users;
pub mod friends;
pub mod relay;
pub mod client;

// Re-export commonly used types
pub use game::{GameAdapter, GameProtocol, AssetLoader, EventBus, GameEvent};
pub use game::adapter::HytaleAdapter;
pub use features::{FeatureManager, FeatureDefinition, FeatureStatus};
pub use launcher::LauncherService;
pub use profiles::ProfileManager;
pub use mods::ModOrchestrator;
pub use cache::CacheManager;
pub use diagnostics::DiagnosticsCollector;
pub use sessions::SessionOrchestrator;
pub use ipc::IpcServer;
pub use db::Database;
pub use users::UserService;
pub use friends::FriendsService;
pub use relay::RelayServer;
pub use client::ApiClient;
