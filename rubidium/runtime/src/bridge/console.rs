use parking_lot::RwLock;
use std::collections::VecDeque;
use tokio::sync::broadcast;
use tracing::{info, warn, error};
use chrono::{DateTime, Utc};

#[derive(Debug, Clone)]
pub struct ConsoleLine {
    pub content: String,
    pub timestamp: DateTime<Utc>,
    pub level: ConsoleLevel,
    pub source: ConsoleSource,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ConsoleLevel {
    Info,
    Warn,
    Error,
    Debug,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ConsoleSource {
    Server,
    Plugin,
    Rubidium,
}

pub struct ConsoleHandler {
    history: RwLock<VecDeque<ConsoleLine>>,
    max_history: usize,
    line_tx: broadcast::Sender<ConsoleLine>,
    patterns: RwLock<Vec<(String, bool)>>,
}

impl ConsoleHandler {
    pub fn new() -> Self {
        Self::with_capacity(10000)
    }

    pub fn with_capacity(max_history: usize) -> Self {
        let (line_tx, _) = broadcast::channel(1000);
        Self {
            history: RwLock::new(VecDeque::with_capacity(max_history)),
            max_history,
            line_tx,
            patterns: RwLock::new(Vec::new()),
        }
    }

    pub async fn append_output(&self, content: &str) {
        let level = self.detect_level(content);
        self.append_line(content, level, ConsoleSource::Server).await;
    }

    pub async fn append_error(&self, content: &str) {
        self.append_line(content, ConsoleLevel::Error, ConsoleSource::Server).await;
    }

    pub async fn append_rubidium(&self, content: &str, level: ConsoleLevel) {
        self.append_line(content, level, ConsoleSource::Rubidium).await;
    }

    async fn append_line(&self, content: &str, level: ConsoleLevel, source: ConsoleSource) {
        let line = ConsoleLine {
            content: content.to_string(),
            timestamp: Utc::now(),
            level,
            source,
        };

        self.check_patterns(&line.content).await;

        let mut history = self.history.write();
        if history.len() >= self.max_history {
            history.pop_front();
        }
        history.push_back(line.clone());

        let _ = self.line_tx.send(line);

        match level {
            ConsoleLevel::Info => info!("[Server] {}", content),
            ConsoleLevel::Warn => warn!("[Server] {}", content),
            ConsoleLevel::Error => error!("[Server] {}", content),
            ConsoleLevel::Debug => {}
        }
    }

    fn detect_level(&self, content: &str) -> ConsoleLevel {
        let lower = content.to_lowercase();
        if lower.contains("error") || lower.contains("exception") || lower.contains("failed") {
            ConsoleLevel::Error
        } else if lower.contains("warn") {
            ConsoleLevel::Warn
        } else {
            ConsoleLevel::Info
        }
    }

    async fn check_patterns(&self, content: &str) {
        let mut patterns = self.patterns.write();
        for (pattern, found) in patterns.iter_mut() {
            if content.contains(pattern.as_str()) {
                *found = true;
            }
        }
    }

    pub fn add_pattern(&self, pattern: &str) {
        self.patterns.write().push((pattern.to_string(), false));
    }

    pub async fn contains_pattern(&self, pattern: &str) -> bool {
        let patterns = self.patterns.read();
        patterns.iter()
            .find(|(p, _)| p == pattern)
            .map(|(_, found)| *found)
            .unwrap_or_else(|| {
                drop(patterns);
                self.add_pattern(pattern);
                false
            })
    }

    pub async fn send_input(&self, input: &str) -> Result<(), String> {
        info!("[Console] > {}", input);
        Ok(())
    }

    pub fn subscribe(&self) -> broadcast::Receiver<ConsoleLine> {
        self.line_tx.subscribe()
    }

    pub fn get_history(&self, count: usize) -> Vec<ConsoleLine> {
        let history = self.history.read();
        history.iter()
            .rev()
            .take(count)
            .cloned()
            .collect()
    }

    pub fn search(&self, query: &str) -> Vec<ConsoleLine> {
        let history = self.history.read();
        history.iter()
            .filter(|line| line.content.contains(query))
            .cloned()
            .collect()
    }

    pub fn clear(&self) {
        self.history.write().clear();
    }
}

impl Default for ConsoleHandler {
    fn default() -> Self {
        Self::new()
    }
}
