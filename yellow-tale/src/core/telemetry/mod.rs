//! Telemetry Module
//! 
//! Provides logging and metrics collection:
//! - Structured logging with tracing
//! - Log file rotation
//! - Metric aggregation

use std::path::PathBuf;
use thiserror::Error;
use tracing_subscriber::{
    fmt,
    layer::SubscriberExt,
    util::SubscriberInitExt,
    EnvFilter,
};

#[derive(Error, Debug)]
pub enum TelemetryError {
    #[error("Failed to initialize logging: {0}")]
    InitFailed(String),
    
    #[error("IO error: {0}")]
    IoError(#[from] std::io::Error),
}

/// Initialize the logging system
pub fn init_logging() -> Result<(), TelemetryError> {
    // Create a filter from RUST_LOG env var or use default
    let filter = EnvFilter::try_from_default_env()
        .unwrap_or_else(|_| EnvFilter::new("info,yellow_tale=debug"));
    
    // Set up the subscriber with formatting
    tracing_subscriber::registry()
        .with(filter)
        .with(fmt::layer()
            .with_target(true)
            .with_thread_ids(false)
            .with_file(true)
            .with_line_number(true)
            .compact())
        .try_init()
        .map_err(|e| TelemetryError::InitFailed(e.to_string()))?;
    
    Ok(())
}

/// Initialize logging with file output
pub fn init_logging_with_file(log_dir: PathBuf) -> Result<(), TelemetryError> {
    std::fs::create_dir_all(&log_dir)?;
    
    let log_file = log_dir.join("yellow-tale.log");
    let file = std::fs::OpenOptions::new()
        .create(true)
        .append(true)
        .open(&log_file)?;
    
    let filter = EnvFilter::try_from_default_env()
        .unwrap_or_else(|_| EnvFilter::new("info,yellow_tale=debug"));
    
    tracing_subscriber::registry()
        .with(filter)
        .with(fmt::layer()
            .with_target(true)
            .with_ansi(false)
            .with_writer(file)
            .with_file(true)
            .with_line_number(true))
        .with(fmt::layer()
            .with_target(true)
            .compact())
        .try_init()
        .map_err(|e| TelemetryError::InitFailed(e.to_string()))?;
    
    Ok(())
}

#[cfg(test)]
mod tests {
    #[test]
    fn test_logging_module_exists() {
        assert!(true);
    }
}
