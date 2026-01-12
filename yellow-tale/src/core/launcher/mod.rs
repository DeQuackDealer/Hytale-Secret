//! Game Launcher Module
//! 
//! Provides process lifecycle control for game executables.
//! The game is treated as a black-box - no injection, patching, or hooking.
//! 
//! # Features
//! - Launch game with custom environment variables, working directory, and arguments
//! - Track process PID and state
//! - Detect crashes and clean exits
//! - Clean shutdown handling

use std::collections::HashMap;
use std::path::PathBuf;
use std::process::{Child, Command, Stdio};
use std::sync::Arc;
use tokio::sync::RwLock;
use serde::{Deserialize, Serialize};
use thiserror::Error;
use tracing::{info, warn, error};

#[derive(Error, Debug)]
pub enum LauncherError {
    #[error("Game executable not found: {0}")]
    ExecutableNotFound(PathBuf),
    
    #[error("Failed to launch game: {0}")]
    LaunchFailed(String),
    
    #[error("Game process not running")]
    ProcessNotRunning,
    
    #[error("IO error: {0}")]
    IoError(#[from] std::io::Error),
}

/// Configuration for launching a game process
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LaunchConfig {
    /// Path to the game executable
    pub executable_path: PathBuf,
    
    /// Working directory for the game process
    pub working_dir: Option<PathBuf>,
    
    /// Command line arguments
    pub args: Vec<String>,
    
    /// Environment variables to set
    pub env_vars: HashMap<String, String>,
    
    /// Whether to inherit parent environment
    pub inherit_env: bool,
}

impl Default for LaunchConfig {
    fn default() -> Self {
        Self {
            executable_path: PathBuf::new(),
            working_dir: None,
            args: Vec::new(),
            env_vars: HashMap::new(),
            inherit_env: true,
        }
    }
}

/// State of a launched game process
#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum ProcessState {
    /// Not yet launched
    Idle,
    /// Currently running with given PID
    Running { pid: u32 },
    /// Process exited cleanly with exit code
    Exited { code: i32 },
    /// Process crashed or was killed
    Crashed { reason: String },
}

/// Information about a launched process
#[derive(Debug)]
struct LaunchedProcess {
    child: Child,
    #[allow(dead_code)] // Kept for future use (restart, config export)
    config: LaunchConfig,
    state: ProcessState,
}

/// Service for managing game process lifecycle
pub struct LauncherService {
    /// Currently tracked process (if any)
    process: Arc<RwLock<Option<LaunchedProcess>>>,
}

impl LauncherService {
    /// Create a new launcher service
    pub fn new() -> Self {
        Self {
            process: Arc::new(RwLock::new(None)),
        }
    }
    
    /// Launch a game with the given configuration
    pub async fn launch(&self, config: LaunchConfig) -> Result<u32, LauncherError> {
        // Verify executable exists
        if !config.executable_path.exists() {
            return Err(LauncherError::ExecutableNotFound(config.executable_path.clone()));
        }
        
        info!("Launching game: {:?}", config.executable_path);
        
        // Build the command
        let mut cmd = Command::new(&config.executable_path);
        
        // Set working directory
        if let Some(ref work_dir) = config.working_dir {
            cmd.current_dir(work_dir);
        } else if let Some(parent) = config.executable_path.parent() {
            cmd.current_dir(parent);
        }
        
        // Add arguments
        cmd.args(&config.args);
        
        // Handle environment
        if !config.inherit_env {
            cmd.env_clear();
        }
        for (key, value) in &config.env_vars {
            cmd.env(key, value);
        }
        
        // Configure stdio
        cmd.stdout(Stdio::piped());
        cmd.stderr(Stdio::piped());
        
        // Spawn the process
        let child = cmd.spawn().map_err(|e| {
            error!("Failed to spawn game process: {}", e);
            LauncherError::LaunchFailed(e.to_string())
        })?;
        
        let pid = child.id();
        info!("Game launched with PID: {}", pid);
        
        // Store the process
        let mut process_guard = self.process.write().await;
        *process_guard = Some(LaunchedProcess {
            child,
            config,
            state: ProcessState::Running { pid },
        });
        
        Ok(pid)
    }
    
    /// Get the current state of the game process
    pub async fn get_state(&self) -> ProcessState {
        let process_guard = self.process.read().await;
        match &*process_guard {
            Some(proc) => proc.state.clone(),
            None => ProcessState::Idle,
        }
    }
    
    /// Check if the game process is still running and update state
    pub async fn poll_status(&self) -> ProcessState {
        let mut process_guard = self.process.write().await;
        
        if let Some(ref mut proc) = *process_guard {
            if let ProcessState::Running { pid: _ } = proc.state {
                // Try to check if process has exited
                match proc.child.try_wait() {
                    Ok(Some(status)) => {
                        if status.success() {
                            let code = status.code().unwrap_or(0);
                            info!("Game exited cleanly with code: {}", code);
                            proc.state = ProcessState::Exited { code };
                        } else {
                            let code = status.code().unwrap_or(-1);
                            warn!("Game exited with error code: {}", code);
                            proc.state = ProcessState::Crashed {
                                reason: format!("Exit code: {}", code),
                            };
                        }
                    }
                    Ok(None) => {
                        // Still running
                    }
                    Err(e) => {
                        error!("Error checking process status: {}", e);
                        proc.state = ProcessState::Crashed {
                            reason: e.to_string(),
                        };
                    }
                }
            }
            proc.state.clone()
        } else {
            ProcessState::Idle
        }
    }
    
    /// Request the game process to terminate gracefully
    pub async fn terminate(&self) -> Result<(), LauncherError> {
        let mut process_guard = self.process.write().await;
        
        if let Some(ref mut proc) = *process_guard {
            info!("Requesting game termination...");
            
            // Request graceful termination
            // Note: On Unix, we could send SIGTERM but that requires the nix crate
            // For simplicity, we use kill() which works cross-platform
            let _ = proc.child.kill();
            
            proc.state = ProcessState::Exited { code: 0 };
            Ok(())
        } else {
            Err(LauncherError::ProcessNotRunning)
        }
    }
    
    /// Force kill the game process
    pub async fn kill(&self) -> Result<(), LauncherError> {
        let mut process_guard = self.process.write().await;
        
        if let Some(ref mut proc) = *process_guard {
            warn!("Force killing game process...");
            proc.child.kill()?;
            proc.state = ProcessState::Crashed {
                reason: "Forcefully terminated".to_string(),
            };
            Ok(())
        } else {
            Err(LauncherError::ProcessNotRunning)
        }
    }
    
    /// Clear the stored process state
    pub async fn clear(&self) {
        let mut process_guard = self.process.write().await;
        *process_guard = None;
    }
}

impl Default for LauncherService {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    
    #[tokio::test]
    async fn test_launcher_initial_state() {
        let launcher = LauncherService::new();
        assert!(matches!(launcher.get_state().await, ProcessState::Idle));
    }
}
