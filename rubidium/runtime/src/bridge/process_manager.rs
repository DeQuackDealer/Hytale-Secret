use super::console::ConsoleHandler;
use parking_lot::RwLock;
use std::path::Path;
use std::process::Stdio;
use std::sync::Arc;
use tokio::io::{AsyncBufReadExt, AsyncWriteExt, BufReader};
use tokio::process::{Child, Command};
use tokio::sync::mpsc;
use tracing::info;

pub struct ProcessManager {
    child: RwLock<Option<Child>>,
    stdin_tx: RwLock<Option<mpsc::Sender<String>>>,
    console: Arc<ConsoleHandler>,
    pid: RwLock<Option<u32>>,
}

impl ProcessManager {
    pub fn new(console: Arc<ConsoleHandler>) -> Self {
        Self {
            child: RwLock::new(None),
            stdin_tx: RwLock::new(None),
            console,
            pid: RwLock::new(None),
        }
    }

    pub async fn spawn(
        &self,
        program: &Path,
        args: &[String],
        working_dir: &Path,
    ) -> Result<(), String> {
        if self.is_running().await {
            return Err("Process already running".to_string());
        }

        info!("Spawning process: {:?} {:?}", program, args);

        let mut cmd = Command::new(program);
        cmd.args(args)
            .current_dir(working_dir)
            .stdin(Stdio::piped())
            .stdout(Stdio::piped())
            .stderr(Stdio::piped())
            .kill_on_drop(true);

        let mut child = cmd.spawn()
            .map_err(|e| format!("Failed to spawn process: {}", e))?;

        let pid = child.id();
        *self.pid.write() = pid;
        info!("Process started with PID: {:?}", pid);

        let stdout = child.stdout.take()
            .ok_or("Failed to capture stdout")?;
        let stderr = child.stderr.take()
            .ok_or("Failed to capture stderr")?;
        let stdin = child.stdin.take()
            .ok_or("Failed to capture stdin")?;

        let (stdin_tx, mut stdin_rx) = mpsc::channel::<String>(100);
        *self.stdin_tx.write() = Some(stdin_tx);

        let console_clone = self.console.clone();
        tokio::spawn(async move {
            let reader = BufReader::new(stdout);
            let mut lines = reader.lines();
            while let Ok(Some(line)) = lines.next_line().await {
                console_clone.append_output(&line).await;
            }
        });

        let console_clone = self.console.clone();
        tokio::spawn(async move {
            let reader = BufReader::new(stderr);
            let mut lines = reader.lines();
            while let Ok(Some(line)) = lines.next_line().await {
                console_clone.append_error(&line).await;
            }
        });

        tokio::spawn(async move {
            let mut stdin = stdin;
            while let Some(input) = stdin_rx.recv().await {
                if stdin.write_all(format!("{}\n", input).as_bytes()).await.is_err() {
                    break;
                }
                let _ = stdin.flush().await;
            }
        });

        *self.child.write() = Some(child);
        Ok(())
    }

    pub async fn is_running(&self) -> bool {
        if let Some(ref mut child) = *self.child.write() {
            match child.try_wait() {
                Ok(None) => true,
                Ok(Some(_)) => false,
                Err(_) => false,
            }
        } else {
            false
        }
    }

    pub async fn wait_for_exit(&self, timeout: std::time::Duration) -> bool {
        let start = std::time::Instant::now();
        
        loop {
            if start.elapsed() > timeout {
                return false;
            }
            
            if !self.is_running().await {
                return true;
            }
            
            tokio::time::sleep(std::time::Duration::from_millis(100)).await;
        }
    }

    pub async fn kill(&self) -> Result<(), String> {
        if let Some(ref mut child) = *self.child.write() {
            child.kill().await
                .map_err(|e| format!("Failed to kill process: {}", e))?;
        }
        *self.child.write() = None;
        *self.stdin_tx.write() = None;
        *self.pid.write() = None;
        Ok(())
    }

    pub async fn send_input(&self, input: &str) -> Result<(), String> {
        if let Some(ref tx) = *self.stdin_tx.read() {
            tx.send(input.to_string()).await
                .map_err(|e| format!("Failed to send input: {}", e))
        } else {
            Err("Process not running".to_string())
        }
    }

    pub fn pid(&self) -> Option<u32> {
        *self.pid.read()
    }
}
