use serde::{Deserialize, Serialize};
use std::path::PathBuf;
use tracing_subscriber::{
    fmt,
    layer::SubscriberExt,
    util::SubscriberInitExt,
    EnvFilter,
};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LoggingConfig {
    pub level: String,
    pub format: LogFormat,
    pub console: bool,
    pub file: Option<FileLogConfig>,
    pub colors: bool,
    pub include_target: bool,
    pub include_thread: bool,
    pub include_file_line: bool,
}

impl Default for LoggingConfig {
    fn default() -> Self {
        Self {
            level: "info".to_string(),
            format: LogFormat::Pretty,
            console: true,
            file: None,
            colors: true,
            include_target: true,
            include_thread: false,
            include_file_line: false,
        }
    }
}

#[derive(Debug, Clone, Copy, Serialize, Deserialize)]
pub enum LogFormat {
    Pretty,
    Compact,
    Json,
    Full,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FileLogConfig {
    pub path: PathBuf,
    pub rotation: LogRotation,
    pub max_files: usize,
    pub max_size_mb: usize,
}

#[derive(Debug, Clone, Copy, Serialize, Deserialize)]
pub enum LogRotation {
    Daily,
    Hourly,
    Size,
    Never,
}

pub fn init_logging(config: &LoggingConfig) {
    let filter = EnvFilter::try_from_default_env()
        .unwrap_or_else(|_| EnvFilter::new(&config.level));

    let subscriber = tracing_subscriber::registry().with(filter);

    match config.format {
        LogFormat::Pretty => {
            let layer = fmt::layer()
                .with_ansi(config.colors)
                .with_target(config.include_target)
                .with_thread_ids(config.include_thread)
                .with_file(config.include_file_line)
                .with_line_number(config.include_file_line);
            subscriber.with(layer).init();
        }
        LogFormat::Compact => {
            let layer = fmt::layer()
                .compact()
                .with_ansi(config.colors)
                .with_target(config.include_target);
            subscriber.with(layer).init();
        }
        LogFormat::Json => {
            let layer = fmt::layer()
                .json()
                .with_current_span(true);
            subscriber.with(layer).init();
        }
        LogFormat::Full => {
            let layer = fmt::layer()
                .with_ansi(config.colors)
                .with_target(true)
                .with_thread_ids(true)
                .with_file(true)
                .with_line_number(true);
            subscriber.with(layer).init();
        }
    }
}

pub fn production_config() -> LoggingConfig {
    LoggingConfig {
        level: "info".to_string(),
        format: LogFormat::Json,
        console: true,
        file: Some(FileLogConfig {
            path: PathBuf::from("logs/rubidium.log"),
            rotation: LogRotation::Daily,
            max_files: 7,
            max_size_mb: 100,
        }),
        colors: false,
        include_target: true,
        include_thread: true,
        include_file_line: false,
    }
}

pub fn development_config() -> LoggingConfig {
    LoggingConfig {
        level: "debug".to_string(),
        format: LogFormat::Pretty,
        console: true,
        file: None,
        colors: true,
        include_target: true,
        include_thread: false,
        include_file_line: true,
    }
}
