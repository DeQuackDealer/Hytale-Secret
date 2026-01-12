use rubidium::{BootstrapOrchestrator, LoggingConfig, init_logging, AdminCli};
use rubidium::logging::config::development_config;
use std::path::PathBuf;
use std::io::{self, Write, BufRead};
use tracing::{info, error};

#[tokio::main]
async fn main() {
    let logging_config = if std::env::var("RUBIDIUM_PRODUCTION").is_ok() {
        rubidium::logging::config::production_config()
    } else {
        development_config()
    };
    init_logging(&logging_config);
    
    println!();
    println!("  ██████╗ ██╗   ██╗██████╗ ██╗██████╗ ██╗██╗   ██╗███╗   ███╗");
    println!("  ██╔══██╗██║   ██║██╔══██╗██║██╔══██╗██║██║   ██║████╗ ████║");
    println!("  ██████╔╝██║   ██║██████╔╝██║██║  ██║██║██║   ██║██╔████╔██║");
    println!("  ██╔══██╗██║   ██║██╔══██╗██║██║  ██║██║██║   ██║██║╚██╔╝██║");
    println!("  ██║  ██║╚██████╔╝██████╔╝██║██████╔╝██║╚██████╔╝██║ ╚═╝ ██║");
    println!("  ╚═╝  ╚═╝ ╚═════╝ ╚═════╝ ╚═╝╚═════╝ ╚═╝ ╚═════╝ ╚═╝     ╚═╝");
    println!();
    println!("  All-in-One Hytale Server Platform v{}", rubidium::VERSION);
    println!();

    let config_path = std::env::var("RUBIDIUM_CONFIG")
        .map(PathBuf::from)
        .unwrap_or_else(|_| PathBuf::from("rubidium.toml"));
    
    let server_jar = std::env::var("HYTALE_SERVER_JAR")
        .map(PathBuf::from)
        .unwrap_or_else(|_| PathBuf::from("hytaleserver.jar"));
    
    if !server_jar.exists() {
        error!("Server JAR not found: {:?}", server_jar);
        error!("");
        error!("Please place your Hytale server JAR in the current directory");
        error!("or set HYTALE_SERVER_JAR environment variable.");
        error!("");
        error!("Example:");
        error!("  export HYTALE_SERVER_JAR=/path/to/hytaleserver.jar");
        error!("  ./rubidium-server");
        std::process::exit(1);
    }

    info!("Configuration: {:?}", config_path);
    info!("Server JAR: {:?}", server_jar);

    let mut orchestrator = BootstrapOrchestrator::new(config_path, server_jar);
    
    match orchestrator.bootstrap().await {
        Ok(_) => {
            info!("Server started successfully!");
            
            let game_server = orchestrator.game_server().unwrap().clone();
            let anticheat = orchestrator.anticheat().unwrap().clone();
            let event_bus = orchestrator.event_bus().unwrap().clone();
            let session_manager = orchestrator.session_manager().unwrap().clone();
            
            let admin_cli = AdminCli::new(
                game_server.clone(),
                anticheat,
                event_bus,
                session_manager,
            );
            
            println!();
            println!("Type 'help' for available commands, or enter server commands directly.");
            println!();
            
            let stdin = io::stdin();
            let mut stdout = io::stdout();
            
            loop {
                print!("rubidium> ");
                stdout.flush().unwrap();
                
                let mut input = String::new();
                if stdin.lock().read_line(&mut input).is_err() {
                    break;
                }
                
                let input = input.trim();
                if input.is_empty() {
                    continue;
                }
                
                if input == "exit" || input == "quit" {
                    info!("Shutdown requested...");
                    break;
                }
                
                match admin_cli.execute(input).await {
                    Ok(output) => {
                        if !output.is_empty() {
                            println!("{}", output);
                        }
                    }
                    Err(e) => {
                        error!("Error: {}", e);
                    }
                }
                
                if game_server.status() == rubidium::ServerStatus::Offline {
                    info!("Server stopped.");
                    break;
                }
            }
        }
        Err(e) => {
            error!("Bootstrap failed: {}", e);
            error!("");
            error!("Check the logs above for more details.");
            std::process::exit(1);
        }
    }
}
