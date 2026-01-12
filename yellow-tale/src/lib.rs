//! Yellow Tale - High-performance native launcher and orchestration platform
//! 
//! This library provides the core functionality for the Yellow Tale launcher,
//! designed to work with Hytale and other officially-moddable games.
//! 
//! # Architecture
//! 
//! Yellow Tale follows a strict separation of concerns:
//! - **core**: All system logic, file IO, networking, diagnostics
//! - **ipc**: JSON-based communication layer for UI integration
//! 
//! # Design Philosophy
//! 
//! - Treats games as black-box executables
//! - No memory injection, binary patching, or hooking
//! - Compatible with official mod systems without replacing them
//! - Future-proof and legally safe

pub mod core;

pub use core::*;

/// Library version following semver
pub const VERSION: &str = env!("CARGO_PKG_VERSION");

/// IPC API version for schema compatibility
pub const IPC_API_VERSION: &str = "1.0.0";
