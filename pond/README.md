# Pond - Server-Side Modular Platform for Hytale

Pond is a high-performance, modular server-side platform designed to run alongside Hytale servers. It provides plugin support, performance optimization, cosmetic management, and launcher integration without modifying game clients.

## Features

### Core Systems

- **Server Lifecycle** - Clean start/stop/reload with graceful shutdown
- **Plugin System** - Hot-reloadable plugins with dependency resolution
- **Task Scheduler** - Priority-based tick scheduling with adaptive throttling
- **Performance Monitor** - Real-time TPS tracking and entity budgeting
- **Asset Registry** - Server-approved cosmetic management
- **Configuration** - Live-reloadable TOML configuration
- **Telemetry** - Structured performance logging

### Premium Features (Gated)

- Advanced tick scheduling algorithms
- Adaptive entity throttling
- Dynamic chunk activation
- Memory pooling & reuse
- Server load prediction
- Runtime plugin hot-loading
- Live configuration reloads
- Real-time performance dashboards
- Safe plugin sandboxing

## Project Structure

```
pond/
├── src/
│   ├── lib.rs              # Library exports
│   ├── main.rs             # Server entry point
│   └── core/
│       ├── server.rs       # Server lifecycle
│       ├── plugins.rs      # Plugin system
│       ├── scheduler.rs    # Task scheduling
│       ├── performance.rs  # Performance monitoring
│       ├── assets.rs       # Cosmetic registry
│       ├── config.rs       # Configuration management
│       ├── telemetry.rs    # Metrics collection
│       └── integration.rs  # Yellow Tale bridge
├── plugins/
│   └── example-plugin/     # Example plugin
├── pond.toml               # Server configuration
├── Cargo.toml
└── README.md
```

## Quick Start

```bash
cd pond
cargo build --release
./target/release/pond-server
```

## Configuration

Edit `pond.toml` to configure the server:

```toml
[server]
name = "My Pond Server"
port = 25565
max_players = 100
tick_rate = 20

[plugins]
directory = "plugins"
auto_load = true
hot_reload = true

[performance]
tick_budget_ms = 50.0
adaptive_throttling = true
```

## Plugin Development

Create a new plugin by adding a directory under `plugins/` with a `plugin.toml`:

```toml
[plugin]
id = "my-plugin"
name = "My Plugin"
version = "1.0.0"
author = "Your Name"
description = "What it does"
api_version = "1.0.0"

[[dependencies]]
id = "another-plugin"
version = "1.0.0"
optional = false
```

## Yellow Tale Integration

Pond can advertise its capabilities to Yellow Tale launchers:

- Server capability discovery
- Asset manifest validation
- Cosmetic ownership verification
- Performance hints for client optimization

Enable in `pond.toml`:

```toml
[integration]
enabled = true
launcher_api_port = 25566
advertise_capabilities = true
```

## Design Philosophy

- **Game-agnostic** - No game-specific APIs or assumptions
- **No client injection** - Server-side only
- **Graceful degradation** - Premium features are optional
- **Performance-first** - Adaptive throttling prevents lag
- **Stable under load** - Fault-isolated plugins

## License

Copyright (c) 2026 Yellow Tale Team
