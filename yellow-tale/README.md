# Yellow Tale

A high-performance native launcher and orchestration platform for Hytale.

## Overview

Yellow Tale is a game launcher that sits **above** officially-moddable games like Hytale, enhancing orchestration, performance preparation, caching, diagnostics, and user experience - **without touching game internals**.

### Design Philosophy

- **Game as Black Box**: No memory injection, binary patching, or hooking
- **Legal & Safe**: Only pre-launch optimizations and external orchestration
- **Future-Proof**: Designed to survive game updates, mod loader changes, and engine rewrites
- **Compatible**: Works alongside official mod systems without replacing them

## Hytale Information

Yellow Tale is designed to work with [Hytale](https://hytale.com), launching in Early Access on **January 13, 2026**.

### Key Hytale Technical Details

| Aspect | Details |
|--------|---------|
| **Launch Date** | January 13, 2026 (Early Access) |
| **Price** | $20 |
| **Platform** | Windows PC only at launch (Mac/Linux planned) |
| **Engine** | Legacy Engine (C#/Java) |
| **Modding** | Server-side only - client cannot be modified |

### Hytale System Requirements

**Minimum (30+ FPS @ 1080p Low)**
- OS: Windows 10 64-bit (1809+) / Windows 11
- CPU: Intel Core i3-4130 / AMD FX-6300
- RAM: 8 GB (dedicated GPU) / 12 GB (integrated)
- GPU: Intel UHD 620 / Radeon Vega 3 / GTX 900 / RX 400
- Storage: 10-20 GB (SSD recommended)
- Graphics API: OpenGL 4.1+

**Recommended (60+ FPS @ 1080p High)**
- CPU: Intel Core i5-10400 / Ryzen 5 3600
- RAM: 16 GB
- GPU: Intel Iris Xe / Radeon 660M / GTX 900 / RX 400

### Hytale Modding Architecture

Hytale uses a **"One Community, One Client"** philosophy:
- All mods run **server-side only**
- Client automatically receives assets/logic when joining modded servers
- No client-side mod installations needed

**Four Modding Categories:**

1. **Java Plugins** - Programmatic server extensions (similar to Bukkit/Spigot)
2. **Data Assets (JSON)** - Define game content via configuration files
3. **Art Assets** - Models, textures, animations via Blockbench
4. **Visual Scripting** - Node-based logic editor (like Unreal Blueprints)

### Official Hytale Resources

- Official Website: [hytale.com](https://hytale.com)
- Official Blog: [hytale.com/news](https://hytale.com/news)
- Community Wiki: [hytalemodding.dev](https://hytalemodding.dev/en)
- GitHub Community: [github.com/HytaleModding](https://github.com/HytaleModding)

## Architecture

```
yellow-tale/
├─ core/
│  ├─ launcher/      # Process lifecycle control
│  ├─ profiles/      # Profile configs & migration
│  ├─ mods/          # Generic mod orchestration
│  ├─ cache/         # Deduplicated content cache
│  ├─ performance/   # Pre-launch optimization
│  ├─ diagnostics/   # Read-only metrics
│  ├─ sessions/      # Session orchestration
│  ├─ ipc/           # UI ↔ core interface (49 commands)
│  ├─ telemetry/     # Logs & metrics
│  ├─ util/          # Shared utilities
│  ├─ config/        # Configuration management
│  ├─ db/            # PostgreSQL database
│  ├─ users/         # User auth & accounts
│  ├─ friends/       # Social features
│  └─ relay/         # WebSocket relay tunneling
│
├─ config/
│  └─ defaults.toml  # Default configuration
│
└─ README.md
```

## Core Systems

### 1. Game Launcher
- Launch any executable with custom environment, working directory, and arguments
- Track process PID and state
- Detect crashes and clean exits
- Clean shutdown handling

### 2. Profile System
- Multiple user profiles with versioned TOML storage
- Each profile contains: executable path, enabled mods, performance preferences
- Automatic schema migration for future updates

### 3. Mod Orchestration
- Generic mod package management (install/remove/enable/disable)
- Version pinning and dependency graph resolution
- Compatible with official mod systems

### 4. User Authentication (Database-Backed)
- PostgreSQL database for persistent storage
- User signup with Argon2 password hashing
- Session token management with expiration
- Profile updates (display name, avatar)
- User search functionality

### 5. Friends System
- Send/accept/decline friend requests
- Friends list with online status
- User blocking functionality
- Mutual friend detection

### 6. Relay/Tunneling Server
- WebSocket-based relay server for P2P connections
- Session-based peer grouping
- Automatic host migration on disconnect
- Binary and JSON message support
- Latency-optimized connection handling

### 4. Smart Cache
- Content-addressed storage with SHA-256 hashing
- Deduplication across profiles
- LRU eviction strategy
- Background cache warming

### 5. Performance Preparation
**Legal & safe pre-launch optimizations only:**
- Process priority tuning
- CPU core affinity
- RAM cleanup before launch
- Disk IO warm-up

### 6. Diagnostics
- CPU/RAM/disk usage monitoring
- Process-specific metrics
- Exportable reports

### 7. Session Orchestration
- Invite code generation
- P2P connection attempt layer
- Relay interface (stub for future infrastructure)
- Session lifecycle tracking

### 8. IPC API
- JSON-based communication
- Versioned command schema
- Error-first responses
- Designed for Tauri UI integration

## Building

```bash
# Build the project
cargo build

# Run tests
cargo test

# Build for release
cargo build --release
```

## Configuration

Copy `config/defaults.toml` to your config directory and modify as needed:

```toml
[cache]
max_size_bytes = 10737418240  # 10 GB
enable_warming = true

[performance]
default_priority = "normal"
warm_disk_default = true

[session]
preferred_method = "hybrid"
max_relay_hops = 3
```

## IPC API

The IPC API uses JSON for communication between the UI and core:

```json
{
  "id": "uuid",
  "version": "1.0.0",
  "command": "launch_game",
  "params": {
    "executable_path": "/path/to/game",
    "args": ["--arg1", "--arg2"]
  }
}
```

Available commands:
- `get_version`, `get_status`
- `launch_game`, `get_game_state`, `terminate_game`
- `list_profiles`, `create_profile`, `update_profile`, `delete_profile`
- `list_mods`, `install_mod`, `remove_mod`, `enable_mod`, `disable_mod`
- `get_cache_stats`, `clear_cache`
- `collect_metrics`, `get_diagnostics_report`
- `create_session`, `join_session`, `leave_session`, `get_invite_code`

## Future Work

- **UI**: Tauri-based frontend (TypeScript/React)
- **Relay Infrastructure**: P2P relay server network
- **Hytale Integration**: When official APIs become available
- **Cross-Platform**: Mac and Linux support

## License

MIT License

## Disclaimer

Yellow Tale is a third-party launcher. It is not affiliated with, endorsed by, or connected to Hypixel Studios or Hytale. All game-related trademarks belong to their respective owners.
