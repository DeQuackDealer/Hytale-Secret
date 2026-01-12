//! Utility Module
//! 
//! Shared utilities used across the application:
//! - Path helpers
//! - Hash utilities
//! - Common types

use std::path::{Path, PathBuf};
use sha2::{Sha256, Digest};

/// Compute SHA-256 hash of data
pub fn sha256_hash(data: &[u8]) -> String {
    let mut hasher = Sha256::new();
    hasher.update(data);
    hex::encode(hasher.finalize())
}

/// Compute SHA-256 hash of a file
pub async fn sha256_file(path: &Path) -> std::io::Result<String> {
    let data = tokio::fs::read(path).await?;
    Ok(sha256_hash(&data))
}

/// Ensure a directory exists, creating it if necessary
pub async fn ensure_dir(path: &Path) -> std::io::Result<()> {
    if !path.exists() {
        tokio::fs::create_dir_all(path).await?;
    }
    Ok(())
}

/// Get a safe filename from a string
pub fn safe_filename(name: &str) -> String {
    name.chars()
        .map(|c| match c {
            '/' | '\\' | ':' | '*' | '?' | '"' | '<' | '>' | '|' => '_',
            c if c.is_control() => '_',
            c => c,
        })
        .collect()
}

/// Human-readable file size
pub fn human_size(bytes: u64) -> String {
    const UNITS: &[&str] = &["B", "KB", "MB", "GB", "TB"];
    let mut size = bytes as f64;
    let mut unit_idx = 0;
    
    while size >= 1024.0 && unit_idx < UNITS.len() - 1 {
        size /= 1024.0;
        unit_idx += 1;
    }
    
    if unit_idx == 0 {
        format!("{} B", bytes)
    } else {
        format!("{:.2} {}", size, UNITS[unit_idx])
    }
}

/// Get the application data directory
pub fn get_app_data_dir() -> PathBuf {
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

/// Get the application config directory
pub fn get_config_dir() -> PathBuf {
    get_app_data_dir().join("config")
}

/// Get the application cache directory
pub fn get_cache_dir() -> PathBuf {
    get_app_data_dir().join("cache")
}

/// Get the application logs directory
pub fn get_logs_dir() -> PathBuf {
    get_app_data_dir().join("logs")
}

#[cfg(test)]
mod tests {
    use super::*;
    
    #[test]
    fn test_sha256_hash() {
        let hash = sha256_hash(b"test");
        assert_eq!(hash.len(), 64);
    }
    
    #[test]
    fn test_safe_filename() {
        assert_eq!(safe_filename("hello/world"), "hello_world");
        assert_eq!(safe_filename("file:name"), "file_name");
        assert_eq!(safe_filename("normal.txt"), "normal.txt");
    }
    
    #[test]
    fn test_human_size() {
        assert_eq!(human_size(0), "0 B");
        assert_eq!(human_size(512), "512 B");
        assert_eq!(human_size(1024), "1.00 KB");
        assert_eq!(human_size(1536), "1.50 KB");
        assert_eq!(human_size(1048576), "1.00 MB");
    }
}
