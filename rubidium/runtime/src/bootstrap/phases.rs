use std::fmt;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum BootstrapPhase {
    Initializing,
    Configuration,
    Verification,
    CoreServices,
    GameServer,
    EventSubscriptions,
    Plugins,
    Anticheat,
    Ready,
    Failed,
}

impl BootstrapPhase {
    pub fn description(&self) -> &'static str {
        match self {
            BootstrapPhase::Initializing => "Preparing bootstrap sequence",
            BootstrapPhase::Configuration => "Loading configuration files",
            BootstrapPhase::Verification => "Verifying server JAR and dependencies",
            BootstrapPhase::CoreServices => "Starting core services",
            BootstrapPhase::GameServer => "Launching game server process",
            BootstrapPhase::EventSubscriptions => "Wiring event subscriptions",
            BootstrapPhase::Plugins => "Loading plugins",
            BootstrapPhase::Anticheat => "Initializing anticheat system",
            BootstrapPhase::Ready => "Server ready",
            BootstrapPhase::Failed => "Bootstrap failed",
        }
    }

    pub fn is_critical(&self) -> bool {
        matches!(self, 
            BootstrapPhase::Configuration |
            BootstrapPhase::Verification |
            BootstrapPhase::GameServer
        )
    }

    pub fn order(&self) -> u8 {
        match self {
            BootstrapPhase::Initializing => 0,
            BootstrapPhase::Configuration => 1,
            BootstrapPhase::Verification => 2,
            BootstrapPhase::CoreServices => 3,
            BootstrapPhase::GameServer => 4,
            BootstrapPhase::EventSubscriptions => 5,
            BootstrapPhase::Plugins => 6,
            BootstrapPhase::Anticheat => 7,
            BootstrapPhase::Ready => 8,
            BootstrapPhase::Failed => 255,
        }
    }
}

impl fmt::Display for BootstrapPhase {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.description())
    }
}
