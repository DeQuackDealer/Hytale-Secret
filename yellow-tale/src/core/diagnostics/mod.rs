//! Diagnostics Module
//! 
//! Provides READ-ONLY system metrics collection:
//! - CPU usage
//! - GPU usage (when observable)
//! - RAM usage
//! - Disk IO
//! - Frame-time variance (if observable externally)
//! - Exportable logs
//! 
//! All metrics are exposed via IPC.

use std::collections::VecDeque;
use std::path::PathBuf;
use serde::{Deserialize, Serialize};
use thiserror::Error;
use sysinfo::{System, Disks, Pid};
use chrono::{DateTime, Utc};
use tracing::info;

#[derive(Error, Debug)]
pub enum DiagnosticsError {
    #[error("Process not found: {0}")]
    ProcessNotFound(u32),
    
    #[error("Failed to export: {0}")]
    ExportFailed(String),
    
    #[error("IO error: {0}")]
    IoError(#[from] std::io::Error),
}

/// A single metrics sample
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MetricsSample {
    /// When this sample was taken
    pub timestamp: DateTime<Utc>,
    
    /// Overall CPU usage (0.0 - 100.0)
    pub cpu_usage: f32,
    
    /// Per-core CPU usage
    pub cpu_per_core: Vec<f32>,
    
    /// Used RAM in MB
    pub ram_used_mb: u64,
    
    /// Total RAM in MB
    pub ram_total_mb: u64,
    
    /// Disk read bytes since last sample
    pub disk_read_bytes: u64,
    
    /// Disk write bytes since last sample
    pub disk_write_bytes: u64,
}

/// Process-specific metrics
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ProcessMetrics {
    /// Process ID
    pub pid: u32,
    
    /// Process name
    pub name: String,
    
    /// CPU usage by this process
    pub cpu_usage: f32,
    
    /// Memory used by this process in MB
    pub memory_mb: u64,
    
    /// Process status
    pub status: String,
}

/// Diagnostics report for export
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DiagnosticsReport {
    /// Report generation time
    pub generated_at: DateTime<Utc>,
    
    /// Yellow Tale version
    pub launcher_version: String,
    
    /// System information
    pub system_info: SystemInfo,
    
    /// Recent metrics samples
    pub metrics_history: Vec<MetricsSample>,
    
    /// Game process metrics (if running)
    pub game_metrics: Option<ProcessMetrics>,
    
    /// Recent log entries
    pub recent_logs: Vec<LogEntry>,
}

/// System information
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SystemInfo {
    /// Operating system name
    pub os_name: String,
    
    /// OS version
    pub os_version: String,
    
    /// CPU model
    pub cpu_model: String,
    
    /// Number of CPU cores
    pub cpu_cores: usize,
    
    /// Total RAM in MB
    pub total_ram_mb: u64,
    
    /// Disk information
    pub disks: Vec<DiskInfo>,
}

/// Information about a disk
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DiskInfo {
    /// Mount point / drive letter
    pub mount_point: String,
    
    /// Total space in GB
    pub total_gb: u64,
    
    /// Available space in GB
    pub available_gb: u64,
    
    /// Filesystem type
    pub fs_type: String,
}

/// A log entry
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LogEntry {
    /// When this was logged
    pub timestamp: DateTime<Utc>,
    
    /// Log level
    pub level: String,
    
    /// Log message
    pub message: String,
    
    /// Optional source module
    pub source: Option<String>,
}

/// Collector for system diagnostics
pub struct DiagnosticsCollector {
    system: System,
    disks: Disks,
    
    /// History of metrics samples
    metrics_history: VecDeque<MetricsSample>,
    
    /// Maximum history length
    max_history: usize,
    
    /// Recent log entries
    recent_logs: VecDeque<LogEntry>,
    
    /// Maximum log entries to keep
    max_logs: usize,
    
    /// PID of game process (if tracking)
    tracked_pid: Option<u32>,
}

impl DiagnosticsCollector {
    /// Create a new diagnostics collector
    pub fn new() -> Self {
        Self {
            system: System::new_all(),
            disks: Disks::new_with_refreshed_list(),
            metrics_history: VecDeque::new(),
            max_history: 3600, // Keep 1 hour at 1 sample/second
            recent_logs: VecDeque::new(),
            max_logs: 1000,
            tracked_pid: None,
        }
    }
    
    /// Set the game process to track
    pub fn track_process(&mut self, pid: u32) {
        self.tracked_pid = Some(pid);
        info!("Now tracking process PID: {}", pid);
    }
    
    /// Stop tracking the game process
    pub fn untrack_process(&mut self) {
        self.tracked_pid = None;
    }
    
    /// Collect a metrics sample
    pub fn collect_sample(&mut self) -> MetricsSample {
        self.system.refresh_all();
        
        let cpu_per_core: Vec<f32> = self.system.cpus()
            .iter()
            .map(|cpu| cpu.cpu_usage())
            .collect();
        
        let cpu_usage = if cpu_per_core.is_empty() {
            0.0
        } else {
            cpu_per_core.iter().sum::<f32>() / cpu_per_core.len() as f32
        };
        
        let sample = MetricsSample {
            timestamp: Utc::now(),
            cpu_usage,
            cpu_per_core,
            ram_used_mb: self.system.used_memory() / 1024 / 1024,
            ram_total_mb: self.system.total_memory() / 1024 / 1024,
            disk_read_bytes: 0, // Would need to track delta
            disk_write_bytes: 0,
        };
        
        // Store in history
        self.metrics_history.push_back(sample.clone());
        while self.metrics_history.len() > self.max_history {
            self.metrics_history.pop_front();
        }
        
        sample
    }
    
    /// Get metrics for the tracked process
    pub fn get_process_metrics(&mut self) -> Option<ProcessMetrics> {
        let pid = self.tracked_pid?;
        self.system.refresh_processes(sysinfo::ProcessesToUpdate::All);
        
        let process = self.system.process(Pid::from_u32(pid))?;
        
        Some(ProcessMetrics {
            pid,
            name: process.name().to_string_lossy().to_string(),
            cpu_usage: process.cpu_usage(),
            memory_mb: process.memory() / 1024 / 1024,
            status: format!("{:?}", process.status()),
        })
    }
    
    /// Get system information
    pub fn get_system_info(&mut self) -> SystemInfo {
        self.system.refresh_all();
        self.disks.refresh();
        
        let cpu_model = self.system.cpus()
            .first()
            .map(|cpu| cpu.brand().to_string())
            .unwrap_or_else(|| "Unknown".to_string());
        
        let disks: Vec<DiskInfo> = self.disks.iter()
            .map(|disk| DiskInfo {
                mount_point: disk.mount_point().to_string_lossy().to_string(),
                total_gb: disk.total_space() / 1024 / 1024 / 1024,
                available_gb: disk.available_space() / 1024 / 1024 / 1024,
                fs_type: disk.file_system().to_string_lossy().to_string(),
            })
            .collect();
        
        SystemInfo {
            os_name: System::name().unwrap_or_else(|| "Unknown".to_string()),
            os_version: System::os_version().unwrap_or_else(|| "Unknown".to_string()),
            cpu_model,
            cpu_cores: self.system.cpus().len(),
            total_ram_mb: self.system.total_memory() / 1024 / 1024,
            disks,
        }
    }
    
    /// Add a log entry
    pub fn log(&mut self, level: &str, message: String, source: Option<String>) {
        let entry = LogEntry {
            timestamp: Utc::now(),
            level: level.to_string(),
            message,
            source,
        };
        
        self.recent_logs.push_back(entry);
        while self.recent_logs.len() > self.max_logs {
            self.recent_logs.pop_front();
        }
    }
    
    /// Get recent metrics history
    pub fn get_history(&self, count: usize) -> Vec<MetricsSample> {
        self.metrics_history
            .iter()
            .rev()
            .take(count)
            .cloned()
            .collect()
    }
    
    /// Generate a full diagnostics report
    pub fn generate_report(&mut self) -> DiagnosticsReport {
        DiagnosticsReport {
            generated_at: Utc::now(),
            launcher_version: crate::VERSION.to_string(),
            system_info: self.get_system_info(),
            metrics_history: self.metrics_history.iter().cloned().collect(),
            game_metrics: self.get_process_metrics(),
            recent_logs: self.recent_logs.iter().cloned().collect(),
        }
    }
    
    /// Export diagnostics report to a file
    pub async fn export_report(&mut self, path: PathBuf) -> Result<(), DiagnosticsError> {
        let report = self.generate_report();
        let content = serde_json::to_string_pretty(&report)
            .map_err(|e| DiagnosticsError::ExportFailed(e.to_string()))?;
        
        tokio::fs::write(&path, content).await?;
        info!("Exported diagnostics report to {:?}", path);
        
        Ok(())
    }
}

impl Default for DiagnosticsCollector {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    
    #[test]
    fn test_collect_sample() {
        let mut collector = DiagnosticsCollector::new();
        let sample = collector.collect_sample();
        assert!(sample.ram_total_mb > 0);
    }
}
