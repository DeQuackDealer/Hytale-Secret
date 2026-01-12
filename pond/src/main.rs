use pond::Server;
use tracing::info;

#[tokio::main]
async fn main() {
    tracing_subscriber::fmt::init();
    
    info!("Starting Pond server...");
    
    let mut server = Server::new("pond.toml").await.expect("Failed to initialize server");
    
    if let Err(e) = server.start().await {
        tracing::error!("Server error: {}", e);
        std::process::exit(1);
    }
}
