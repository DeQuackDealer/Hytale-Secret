# Rubidium

**Production-Ready Server Framework for Hytale**

Rubidium is a modular, runtime-reloadable, performance-focused server framework designed specifically for Hytale. Built with Java 21+ and modern software engineering principles, it provides server operators and developers with a comprehensive foundation for building high-performance, scalable multiplayer experiences.

---

## Why Rubidium?

Unlike traditional server modifications that rely on reverse engineering or memory injection, Rubidium is designed to be **API-safe** from day one. This means it will seamlessly integrate with official Hytale modding APIs when they become available, ensuring your server infrastructure remains stable and compliant with Hypixel Studios' guidelines.

### Design Principles

- **Modular** - Load, unload, and reload modules at runtime without server restarts
- **Performance-Focused** - Built-in budget management and metrics to maintain consistent tick rates
- **API-Safe** - No game internals access, ready for official modding APIs
- **Extensible** - Clean interfaces with default implementations and clear extension points

---

## Access Control

Rubidium includes a comprehensive access control system with public/private server modes.

### Server Modes

- **Public Mode** - Anyone can join (default). Use `/ban` to block players.
- **Private Mode** - Only whitelisted players can join. Use `/whitelist` to manage access.

### Commands

```
/accessmode <public|private>     - Switch between public and private mode
/ban <player> [reason]           - Ban a player (permanent)
/tempban <player> <duration>     - Temporary ban (1d, 12h, 30m, 1w)
/unban <player>                  - Remove a ban
/banlist                         - View all banned players

/whitelist add <player>          - Add player to whitelist
/whitelist remove <player>       - Remove from whitelist
/whitelist list                  - View whitelisted players
/whitelist on                    - Enable whitelist (private mode)
/whitelist off                   - Disable whitelist (public mode)
/whitelist clear                 - Clear all whitelisted players
```

### Duration Format

For temporary bans, use these duration formats:
- `30s` - 30 seconds
- `30m` - 30 minutes
- `12h` - 12 hours
- `7d` - 7 days
- `2w` - 2 weeks

---

## Modtale Integration

Rubidium integrates directly with [Modtale](https://modtale.net), the Hytale community mod repository. Server operators can browse, install, and update plugins directly from in-game commands.

### Setup

1. Set your Modtale API key as an environment variable:

```bash
export MODTALE_API_KEY="md_your_api_key_here"
```

Get your API key from [Modtale Developer Settings](https://modtale.net/dashboard/developer).

2. **For production use**, configure a proper JSON parser (Gson, Jackson, etc.):

```java
// Add Gson to your dependencies
ModtaleClient client = new ModtaleClient(logger, apiKey);
client.setJsonParser(new ModtaleClient.JsonParser() {
    private final Gson gson = new Gson();
    
    @Override
    public ModtaleClient.SearchResult parseSearchResult(String json) {
        return gson.fromJson(json, ModtaleClient.SearchResult.class);
    }
    
    @Override
    public ModtaleClient.ProjectDetails parseProjectDetails(String json) {
        return gson.fromJson(json, ModtaleClient.ProjectDetails.class);
    }
    
    @Override
    public List<String> parseStringArray(String json) {
        return gson.fromJson(json, new TypeToken<List<String>>(){}.getType());
    }
});
```

> **Note**: The built-in basic JSON parser is for testing only. Production deployments must use a proper JSON library.

### Commands

```
/plugins search <query>              - Search for plugins
/plugins browse [popular|newest]     - Browse plugin listings
/plugins info <project-id>           - View plugin details
/plugins install <project-id>        - Install latest version
/plugins install <id> <version>      - Install specific version
/plugins update <project-id>         - Update to latest version
/plugins uninstall <project-id>      - Remove a plugin
/plugins installed                   - List installed plugins
/plugins tags                        - Show available categories
```

### Example Usage

```
> /plugins search anticheat
Searching for: anticheat...
=== Search Results (3 total) ===
 - Super AntiCheat by ModDev123
   15420 downloads | 4.8 rating
   ID: 550e8400-e29b-41d4-a716-446655440000

> /plugins install 550e8400-e29b-41d4-a716-446655440000
Installing plugin...
Successfully installed plugin!
Plugin installed successfully. Restart to activate.
```

---

## Core Systems

### Module System

The heart of Rubidium. Modules can be loaded, unloaded, and reloaded at runtime without requiring a server restart.

- **Runtime Loading** - Load new modules while the server is running
- **Hot Reloading** - Update module code without server downtime
- **Dependency Resolution** - Automatic topological sorting ensures modules load in correct order
- **Isolated Classloaders** - Each module gets its own classloader for clean separation
- **Safe Unloading** - Proper cleanup with `onDisable` hooks and GC hints

```java
public class MyModule extends AbstractModule {
    @Override
    public String getId() {
        return "my_module";
    }

    @Override
    protected void doEnable() {
        logger.info("Module enabled!");
    }

    @Override
    protected void doDisable() {
        logger.info("Module disabled!");
    }
}
```

### Lifecycle Manager

Comprehensive lifecycle management that coordinates startup, shutdown, and reload operations across all subsystems.

- **Phase Transitions** - `STOPPED`, `STARTING`, `RUNNING`, `STOPPING`, `RELOADING`
- **Lifecycle Hooks** - `onLoad`, `onEnable`, `onDisable`, `onReload` for each module
- **Shutdown Hooks** - Register cleanup actions that execute in reverse order
- **Rollback on Failure** - If startup fails, already-started subsystems are cleanly stopped
- **Event Listeners** - Subscribe to lifecycle phase changes

```java
lifecycleManager.addShutdownHook("cleanup", () -> {
    // This runs during shutdown in reverse registration order
    saveAllData();
});
```

### Configuration System

A type-safe configuration system that supports hot-reloading, validation, and schema migration.

- **Typed Configs** - Define configurations as Java classes with getters/setters
- **Validation** - Built-in validators for ranges, required fields, patterns
- **Hot Reload** - File watching automatically reloads configs when changed
- **Schema Migration** - Upgrade old config formats to new versions
- **Reload Listeners** - Get notified when configs change

```java
public class MyConfig extends AbstractConfig {
    private int maxPlayers = 100;
    private boolean enablePvP = true;

    @Override
    public void load(Properties props) {
        maxPlayers = getInt(props, "max_players", 100);
        enablePvP = getBoolean(props, "enable_pvp", true);
    }

    @Override
    public List<String> validate() {
        return new ValidationBuilder()
            .requireRange("max_players", maxPlayers, 1, 1000)
            .build();
    }
}
```

### Tick-Based Scheduler

A powerful scheduler that integrates with the game's tick loop, supporting both synchronous and asynchronous task execution.

- **Tick Synchronization** - Tasks execute at precise tick intervals (20 TPS target)
- **Async Support** - Run heavy operations on worker threads with `CompletableFuture`
- **Priority Queues** - `LOW`, `NORMAL`, `HIGH`, `CRITICAL` task priorities
- **Deferred Execution** - Non-critical tasks can be deferred when tick budget is exceeded
- **Repeating Tasks** - Schedule tasks to run at fixed intervals

```java
// Run every 20 ticks (1 second at 20 TPS)
scheduler.runTaskTimer("my_module", () -> {
    updatePlayerStats();
}, 0, 20);

// Run async
scheduler.runAsync("my_module", () -> {
    return fetchDataFromDatabase();
}).thenAccept(data -> {
    processData(data);
});
```

### Performance Budget Manager

Keep your server running at a consistent tick rate with per-module performance budgets and automatic task deferral.

- **Per-Module Budgets** - Each module gets allocated time per tick
- **Time Tracking** - Measure exactly how long each module takes
- **Soft Limits** - Warn or defer when modules exceed their budget
- **Tick Overrun Detection** - Automatic reporting when ticks take too long
- **Budget Reset** - Fresh allocation each tick for fair scheduling

```java
// Start timing a module's execution
TimingContext ctx = performanceManager.startTiming("my_module");
try {
    doExpensiveWork();
} finally {
    ctx.stop();
}
```

### Metrics & Profiling

Comprehensive metrics collection for monitoring server health and performance.

- **Counters** - Track event counts (packets sent, tasks executed)
- **Gauges** - Monitor current values (player count, memory usage)
- **Histograms** - Record distributions with percentiles (P50, P95, P99)
- **Timers** - Measure operation durations
- **Tick Statistics** - Detailed tick timing with rolling averages
- **Memory Sampling** - Track heap usage over time

```java
// Increment a counter
metricsRegistry.counter("my_module.events.processed").increment();

// Record a value in a histogram
metricsRegistry.histogram("my_module.response_time").record(responseMs);

// Set a gauge
metricsRegistry.gauge("my_module.active_connections", () -> getConnectionCount());
```

### Structured Logging

A logging system designed for high-performance servers with async writing and structured formatting.

- **Async File Writing** - Logs are written on a background thread
- **Structured Format** - Use `{}` placeholders for efficient string formatting
- **Per-Module Loggers** - Each module gets a named logger
- **Log Levels** - `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`
- **Graceful Shutdown** - Queue is drained before closing

```java
RubidiumLogger logger = logManager.getLogger("MyModule");

logger.info("Player {} joined from {}", player.getName(), player.getAddress());
logger.debug("Processing {} items", items.size());
logger.error("Failed to save data", exception);
```

### Network Abstraction

A network layer that abstracts away protocol details while providing powerful traffic shaping.

- **Packet Batching** - Group multiple packets for efficient transmission
- **Priority Queues** - Critical packets (keepalive) go first
- **Bandwidth Limiting** - Cap total bytes per second
- **Packet Interceptors** - Inspect or modify packets before sending
- **Protocol Agnostic** - Ready for official Hytale protocols

```java
// Send a packet with priority
networkManager.send(connectionId, packet, PacketPriority.HIGH);

// Register a packet interceptor
networkManager.addInterceptor((packet, connection) -> {
    logPacket(packet);
    return packet; // or modify/drop
});
```

---

## Getting Started

### Requirements

- Java 21 or higher
- Gradle 8.0+

### Installation

Add Rubidium to your Gradle project:

```kotlin
dependencies {
    implementation("com.yellowtale:rubidium-sdk:1.0.0")
}
```

### Creating Your First Module

```java
package com.example.mymodule;

import com.yellowtale.rubidium.core.module.AbstractModule;
import com.yellowtale.rubidium.core.module.ModuleDescriptor;

public class MyFirstModule extends AbstractModule {

    @Override
    public String getId() {
        return "my_first_module";
    }

    @Override
    public String getName() {
        return "My First Module";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    protected void doEnable() {
        logger.info("Hello, Rubidium!");
        
        // Schedule a repeating task
        getScheduler().runTaskTimer(getId(), () -> {
            logger.debug("Tick!");
        }, 0, 20);
    }

    @Override
    protected void doDisable() {
        logger.info("Goodbye!");
    }
}
```

### Module Manifest

Create a `rubidium.module` file in your JAR's `META-INF` directory:

```properties
id=my_first_module
name=My First Module
version=1.0.0
main=com.example.mymodule.MyFirstModule
api_version=1.0
dependencies=
```

---

## Project Structure

```
src/main/java/com/yellowtale/rubidium/
├── annotations/          # @Plugin, @Command, @EventHandler, @Scheduled
├── api/                  # Public API for plugins
│   ├── anticheat/        # Anti-cheat service interfaces
│   ├── command/          # Command system
│   ├── config/           # Plugin configuration
│   ├── event/            # Event bus and player events
│   ├── logging/          # Logger interface
│   ├── player/           # Player abstraction
│   └── scheduler/        # Task scheduler interface
├── core/                 # Core framework implementation
│   ├── config/           # Configuration system
│   ├── lifecycle/        # Lifecycle management
│   ├── logging/          # Logging implementation
│   ├── metrics/          # Metrics collection
│   ├── module/           # Module system
│   ├── network/          # Network abstraction
│   ├── performance/      # Performance budgeting
│   ├── scheduler/        # Scheduler implementation
│   └── RubidiumCore.java # Main entry point
├── devkit/               # Development tools
├── integration/          # Yellow Tale launcher integration
└── optimization/         # Performance optimization utilities
```

---

## License

**Rubidium Proprietary License v1.0**

- You **can** run Rubidium on your servers
- You **cannot** redistribute, fork, or modify it
- You **cannot** resell or bundle it
- Cosmetics are licensed, not owned

See [LICENSE](LICENSE) for the full license text.

---

## Links

- **Website**: [Yellow Tale](https://yellowtale.com)
- **Documentation**: [Rubidium Docs](https://yellowtale.com/rubidium)
- **Issues**: [GitHub Issues](https://github.com/DeQuackDealer/Rubidium-HytalePlugin/issues)

---

**Built with passion for the Hytale community.**

*2026 Riley Liang (DeQuackDealer)*
