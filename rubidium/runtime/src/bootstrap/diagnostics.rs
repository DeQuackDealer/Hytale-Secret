use std::time::Duration;
use serde::{Serialize, Deserialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct StartupReport {
    pub phases: Vec<DiagnosticResult>,
    pub total_time: Duration,
    pub info: Vec<String>,
    pub warnings: Vec<String>,
    pub errors: Vec<String>,
}

impl StartupReport {
    pub fn new() -> Self {
        Self {
            phases: Vec::new(),
            total_time: Duration::ZERO,
            info: Vec::new(),
            warnings: Vec::new(),
            errors: Vec::new(),
        }
    }

    pub fn add_info(&mut self, message: impl Into<String>) {
        self.info.push(message.into());
    }

    pub fn add_warning(&mut self, message: impl Into<String>) {
        self.warnings.push(message.into());
    }

    pub fn add_error(&mut self, message: impl Into<String>) {
        self.errors.push(message.into());
    }

    pub fn is_success(&self) -> bool {
        self.errors.is_empty() && self.phases.iter().all(|p| p.success)
    }

    pub fn summary(&self) -> String {
        let success_count = self.phases.iter().filter(|p| p.success).count();
        let total_count = self.phases.len();
        format!(
            "{}/{} phases complete in {:.2}s",
            success_count,
            total_count,
            self.total_time.as_secs_f64()
        )
    }
}

impl Default for StartupReport {
    fn default() -> Self {
        Self::new()
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DiagnosticResult {
    pub name: String,
    pub success: bool,
    pub message: Option<String>,
    pub duration: Duration,
    pub level: DiagnosticLevel,
    pub details: Vec<String>,
}

impl DiagnosticResult {
    pub fn success(name: impl Into<String>, duration: Duration) -> Self {
        Self {
            name: name.into(),
            success: true,
            message: None,
            duration,
            level: DiagnosticLevel::Info,
            details: Vec::new(),
        }
    }

    pub fn failure(name: impl Into<String>, message: impl Into<String>, duration: Duration) -> Self {
        Self {
            name: name.into(),
            success: false,
            message: Some(message.into()),
            duration,
            level: DiagnosticLevel::Error,
            details: Vec::new(),
        }
    }

    pub fn warning(name: impl Into<String>, message: impl Into<String>, duration: Duration) -> Self {
        Self {
            name: name.into(),
            success: true,
            message: Some(message.into()),
            duration,
            level: DiagnosticLevel::Warning,
            details: Vec::new(),
        }
    }

    pub fn with_detail(mut self, detail: impl Into<String>) -> Self {
        self.details.push(detail.into());
        self
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum DiagnosticLevel {
    Info,
    Warning,
    Error,
    Critical,
}
