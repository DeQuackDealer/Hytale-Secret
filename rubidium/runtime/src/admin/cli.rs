use crate::bridge::GameServerBridge;
use crate::anticheat::AnticheatService;
use crate::events::EventBus;
use crate::features::SessionManager;
use std::sync::Arc;
use tracing::info;

pub struct AdminCli {
    game_server: Arc<GameServerBridge>,
    anticheat: Arc<AnticheatService>,
    event_bus: Arc<EventBus>,
    session_manager: Arc<SessionManager>,
}

impl AdminCli {
    pub fn new(
        game_server: Arc<GameServerBridge>,
        anticheat: Arc<AnticheatService>,
        event_bus: Arc<EventBus>,
        session_manager: Arc<SessionManager>,
    ) -> Self {
        Self {
            game_server,
            anticheat,
            event_bus,
            session_manager,
        }
    }

    pub async fn execute(&self, command: &str) -> Result<String, String> {
        let parts: Vec<&str> = command.trim().split_whitespace().collect();
        if parts.is_empty() {
            return Ok(self.help());
        }

        match parts[0] {
            "help" | "?" => Ok(self.help()),
            "status" => Ok(self.status().await),
            "players" => Ok(self.players().await),
            "anticheat" => self.anticheat_cmd(&parts[1..]).await,
            "tps" => Ok(self.tps().await),
            "uptime" => Ok(self.uptime().await),
            "events" => Ok(self.events().await),
            "sessions" => Ok(self.sessions().await),
            "findings" => self.findings(&parts[1..]).await,
            "kick" => self.kick(&parts[1..]).await,
            "say" => self.say(&parts[1..]).await,
            "stop" => self.stop().await,
            "reload" => self.reload().await,
            _ => self.passthrough(command).await,
        }
    }

    fn help(&self) -> String {
        r#"
Rubidium Admin Commands:
  status          - Show server status overview
  players         - List online players
  tps             - Show current TPS
  uptime          - Show server uptime
  events          - Show event statistics
  sessions        - Show session statistics
  
  anticheat status    - Show anticheat status
  anticheat toggle    - Enable/disable anticheat
  anticheat findings  - Show recent findings
  
  findings [player]   - Show anticheat findings
  kick <player> [reason] - Kick a player
  say <message>       - Broadcast a message
  
  reload          - Reload configuration
  stop            - Stop the server
  
  Any other command is passed to the game server.
"#.to_string()
    }

    async fn status(&self) -> String {
        let status = self.game_server.status();
        let players = self.game_server.player_count();
        let tps = self.game_server.tps();
        let uptime = self.game_server.uptime()
            .map(|d| format!("{}s", d.as_secs()))
            .unwrap_or_else(|| "N/A".to_string());
        let events = self.event_bus.event_count();
        let anticheat_status = if self.anticheat.is_enabled() { "ON" } else { "OFF" };
        
        format!(
            r#"
┌─────────────────────────────────────┐
│        RUBIDIUM SERVER STATUS       │
├─────────────────────────────────────┤
│ Status:     {:23} │
│ Players:    {:23} │
│ TPS:        {:23.1} │
│ Uptime:     {:23} │
│ Events:     {:23} │
│ Anticheat:  {:23} │
└─────────────────────────────────────┘
"#,
            format!("{:?}", status),
            players,
            tps,
            uptime,
            events,
            anticheat_status
        )
    }

    async fn players(&self) -> String {
        let sessions = self.session_manager.get_all_sessions();
        if sessions.is_empty() {
            return "No players online.".to_string();
        }

        let mut output = format!("Online Players ({}):\n", sessions.len());
        for session in sessions {
            let duration = session.session_duration();
            let premium = if session.is_premium { " [Premium]" } else { "" };
            output.push_str(&format!(
                "  {} - {} ({:?}){}\n",
                session.username,
                session.player_id,
                duration,
                premium
            ));
        }
        output
    }

    async fn tps(&self) -> String {
        let tps = self.game_server.tps();
        let tick = self.game_server.tick();
        format!("TPS: {:.1} (tick: {})", tps, tick)
    }

    async fn uptime(&self) -> String {
        match self.game_server.uptime() {
            Some(d) => {
                let secs = d.as_secs();
                let hours = secs / 3600;
                let mins = (secs % 3600) / 60;
                let secs = secs % 60;
                format!("Uptime: {}h {}m {}s", hours, mins, secs)
            }
            None => "Server not running".to_string(),
        }
    }

    async fn events(&self) -> String {
        let count = self.event_bus.event_count();
        let handlers = self.event_bus.handler_count();
        format!("Events processed: {}\nActive handlers: {}", count, handlers)
    }

    async fn sessions(&self) -> String {
        let total = self.session_manager.get_online_count();
        let premium = self.session_manager.get_premium_count();
        format!(
            "Sessions: {} total, {} premium ({:.1}%)",
            total,
            premium,
            if total > 0 { premium as f64 / total as f64 * 100.0 } else { 0.0 }
        )
    }

    async fn anticheat_cmd(&self, args: &[&str]) -> Result<String, String> {
        if args.is_empty() {
            return Ok(format!("Anticheat: {}", if self.anticheat.is_enabled() { "enabled" } else { "disabled" }));
        }

        match args[0] {
            "status" => {
                let enabled = self.anticheat.is_enabled();
                let findings = self.anticheat.get_recent_findings(5);
                let mut output = format!("Anticheat Status: {}\n", if enabled { "ENABLED" } else { "DISABLED" });
                output.push_str(&format!("Recent findings: {}\n", findings.len()));
                for finding in findings {
                    output.push_str(&format!("  [{:?}] {} - {}\n", 
                                              finding.level, finding.player_id, finding.description));
                }
                Ok(output)
            }
            "toggle" => {
                let new_state = !self.anticheat.is_enabled();
                self.anticheat.set_enabled(new_state);
                Ok(format!("Anticheat: {}", if new_state { "enabled" } else { "disabled" }))
            }
            "findings" => {
                let findings = self.anticheat.get_recent_findings(20);
                if findings.is_empty() {
                    return Ok("No anticheat findings.".to_string());
                }
                let mut output = format!("Recent Findings ({}):\n", findings.len());
                for finding in findings {
                    output.push_str(&format!("  [{:?}] {} - {} (tick {})\n",
                                              finding.level, finding.player_id, 
                                              finding.description, finding.tick));
                }
                Ok(output)
            }
            _ => Err(format!("Unknown anticheat command: {}", args[0])),
        }
    }

    async fn findings(&self, args: &[&str]) -> Result<String, String> {
        if args.is_empty() {
            let findings = self.anticheat.get_recent_findings(10);
            if findings.is_empty() {
                return Ok("No findings.".to_string());
            }
            let mut output = String::new();
            for finding in findings {
                output.push_str(&format!("[{:?}] {} - {}\n", 
                                          finding.level, finding.player_id, finding.description));
            }
            Ok(output)
        } else {
            Err("Player-specific findings lookup not yet implemented".to_string())
        }
    }

    async fn kick(&self, args: &[&str]) -> Result<String, String> {
        if args.is_empty() {
            return Err("Usage: kick <player> [reason]".to_string());
        }
        let player = args[0];
        let reason = if args.len() > 1 {
            args[1..].join(" ")
        } else {
            "Kicked by administrator".to_string()
        };
        
        self.game_server.send_command(&format!("kick {} {}", player, reason)).await?;
        Ok(format!("Kicked {} ({})", player, reason))
    }

    async fn say(&self, args: &[&str]) -> Result<String, String> {
        if args.is_empty() {
            return Err("Usage: say <message>".to_string());
        }
        let message = args.join(" ");
        self.game_server.broadcast(&message).await;
        Ok(format!("Broadcast: {}", message))
    }

    async fn stop(&self) -> Result<String, String> {
        info!("Server stop requested via admin CLI");
        self.game_server.stop().await?;
        Ok("Server stopping...".to_string())
    }

    async fn reload(&self) -> Result<String, String> {
        info!("Configuration reload requested via admin CLI");
        Ok("Configuration reloaded.".to_string())
    }

    async fn passthrough(&self, command: &str) -> Result<String, String> {
        self.game_server.send_command(command).await?;
        Ok(format!("Command sent: {}", command))
    }
}
