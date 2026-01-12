use chrono::{DateTime, Utc};
use std::fmt;

pub struct LogLine {
    pub timestamp: DateTime<Utc>,
    pub level: LogLevel,
    pub target: String,
    pub message: String,
    pub fields: Vec<(String, String)>,
}

#[derive(Debug, Clone, Copy)]
pub enum LogLevel {
    Trace,
    Debug,
    Info,
    Warn,
    Error,
}

impl LogLevel {
    pub fn as_str(&self) -> &'static str {
        match self {
            LogLevel::Trace => "TRACE",
            LogLevel::Debug => "DEBUG",
            LogLevel::Info => "INFO",
            LogLevel::Warn => "WARN",
            LogLevel::Error => "ERROR",
        }
    }

    pub fn color_code(&self) -> &'static str {
        match self {
            LogLevel::Trace => "\x1b[90m",
            LogLevel::Debug => "\x1b[36m",
            LogLevel::Info => "\x1b[32m",
            LogLevel::Warn => "\x1b[33m",
            LogLevel::Error => "\x1b[31m",
        }
    }
}

impl fmt::Display for LogLevel {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.as_str())
    }
}

pub fn format_pretty(line: &LogLine, colors: bool) -> String {
    let level_str = if colors {
        format!("{}{}{}",
                line.level.color_code(),
                line.level.as_str(),
                "\x1b[0m")
    } else {
        line.level.as_str().to_string()
    };

    let timestamp = line.timestamp.format("%Y-%m-%d %H:%M:%S%.3f");
    
    let mut output = format!(
        "{} {:5} [{}] {}",
        timestamp, level_str, line.target, line.message
    );

    if !line.fields.is_empty() {
        output.push_str(" {");
        for (i, (key, value)) in line.fields.iter().enumerate() {
            if i > 0 {
                output.push_str(", ");
            }
            output.push_str(&format!("{}={}", key, value));
        }
        output.push('}');
    }

    output
}

pub fn format_json(line: &LogLine) -> String {
    let mut fields_json = String::from("{");
    for (i, (key, value)) in line.fields.iter().enumerate() {
        if i > 0 {
            fields_json.push_str(",");
        }
        fields_json.push_str(&format!("\"{}\":\"{}\"", key, value));
    }
    fields_json.push('}');

    format!(
        r#"{{"timestamp":"{}","level":"{}","target":"{}","message":"{}","fields":{}}}"#,
        line.timestamp.to_rfc3339(),
        line.level.as_str(),
        line.target,
        line.message.replace('"', "\\\""),
        fields_json
    )
}

pub fn format_compact(line: &LogLine) -> String {
    format!(
        "{} {} {}",
        line.timestamp.format("%H:%M:%S"),
        line.level.as_str().chars().next().unwrap_or('?'),
        line.message
    )
}
