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

## Quality of Life (QoL) Features

Rubidium includes a comprehensive suite of toggleable QoL features for server operators. Each feature can be independently enabled or disabled at runtime with persistent state.

### Managing QoL Features

```
/qol list                    - List all features and their status
/qol info <feature-id>       - Show feature details
/qol enable <feature-id|all> - Enable a feature or all features
/qol disable <feature-id|all> - Disable a feature or all features
/qol toggle <feature-id>     - Toggle a feature on/off
/qol reload                  - Reload all feature configurations
```

### Available Features

| Feature ID | Name | Description |
|------------|------|-------------|
| `afk-detection` | AFK Detection | Detects inactive players with optional auto-kick |
| `chat-formatting` | Chat Formatting | Color codes, mentions, customizable format |
| `join-leave-messages` | Join/Leave Messages | Customizable player join/leave messages |
| `motd` | MOTD & Tab List | Server MOTD and tab header/footer |
| `command-cooldown` | Command Cooldowns | Rate limits commands to prevent spam |
| `maintenance-mode` | Maintenance Mode | Restricts access with bypass permissions |
| `staff-tools` | Staff Tools | Vanish, godmode, freeze, teleport |
| `lag-detection` | Lag Detection | TPS/memory monitoring with alerts |
| `auto-save` | Auto Save | Periodic world saves with announcements |
| `player-stats` | Player Statistics | Tracks playtime and join counts |

### Feature Configuration

Each feature has a configurable record pattern:

```java
// Example: Configure AFK detection
AfkDetectionFeature afk = qolManager.getFeature("afk-detection");
afk.setConfig(new AfkDetectionFeature.AfkConfig(
    Duration.ofMinutes(5),    // timeout before marking AFK
    true,                      // kick AFK players
    Duration.ofMinutes(30),    // kick after this AFK duration
    "{player} is now AFK",     // AFK message
    "{player} is no longer AFK", // return message
    true                       // broadcast AFK status
));
```

### Integrating with Core

```java
// Start the QoL tick loop (integrates with scheduler)
qolManager.startTickLoop(scheduler);

// Register default features
qolManager.registerFeature(new AfkDetectionFeature(logger));
qolManager.registerFeature(new ChatFormattingFeature(logger));
qolManager.registerFeature(new LagDetectionFeature(logger));
// ... register other features

// Enable features
qolManager.enableFeature("afk-detection");
qolManager.enableFeature("lag-detection");
```

### Staff Tools Commands

```
/vanish                      - Toggle invisibility
/godmode                     - Toggle invincibility
/freeze <player>             - Freeze/unfreeze a player
/tp <player>                 - Teleport to a player
/spectate <player>           - Spectate a player (no target = stop)
/staffmode                   - Show active staff modes
```

### Maintenance Mode Commands

```
/maintenance                 - Show maintenance status
/maintenance on [reason]     - Enable maintenance mode
/maintenance off             - Disable maintenance mode
/maintenance add <player>    - Add player to maintenance bypass
/maintenance remove <player> - Remove from bypass list
/maintenance list            - Show bypass list
```

---

## Moderator Replay System

Rubidium includes a powerful replay recording system for anti-cheat purposes. It continuously captures player state at configurable FPS, storing data in efficient ring buffers with delta compression.

### Features

- **Customizable FPS** - Record at 5, 10, 20, or 60 FPS depending on your needs
- **Ring Buffer Storage** - Memory-efficient circular buffers per player
- **Delta Compression** - Only stores changes between frames (typically 90%+ compression)
- **Async I/O** - All disk writes happen on background threads
- **Auto-Pruning** - Automatic cleanup based on retention period and storage quotas
- **Trigger-Based Recording** - Start recording on suspicious activity, combat, or reports
- **TPS-Aware** - Automatically drops frames when server is under load

### Configuration

```java
ModeratorReplayFeature replay = new ModeratorReplayFeature(logger, dataDir);
replay.setConfig(new ModeratorReplayFeature.ReplayConfig(
    20,                          // targetFps: frames per second
    64,                          // captureRadius: blocks around player
    Duration.ofMinutes(5),       // bufferDuration: rolling buffer size
    Duration.ofSeconds(30),      // segmentDuration: chunk size for storage
    false,                       // continuousMode: always record everyone
    true,                        // recordOnSuspicion: anti-cheat triggers
    true,                        // recordOnCombat: record during fights
    true,                        // recordOnReport: record when reported
    dataDir.resolve("replays"),  // storageDir
    10L * 1024 * 1024 * 1024,    // maxStorageBytes: 10 GB total
    512L * 1024 * 1024,          // maxStoragePerPlayerBytes: 512 MB per player
    Duration.ofDays(7),          // retentionPeriod
    2,                           // compressionWorkers
    15.0                         // minTpsThreshold: pause if TPS drops below
));
```

### Commands

```
/replay record <player> [reason]  - Start recording a player manually
/replay stop <player>             - Stop recording a player
/replay list [player]             - List active and saved sessions
/replay info <session-id>         - Show session details
/replay review <session-id>       - Load a replay for review
/replay purge <player>            - Delete all replays for a player
/replay status                    - Show system status and metrics
/replay config <key> [value]      - View or modify configuration
```

### Integration with Anti-Cheat

```java
// Trigger recording when suspicious activity is detected
antiCheatService.onViolation((player, violation) -> {
    replayFeature.onSuspiciousActivity(player, violation.getReason());
});

// Trigger recording when combat starts
combatService.onCombatStart((attacker, defender) -> {
    replayFeature.onCombatStart(attacker, defender);
});

// Trigger recording when a player is reported
reportService.onPlayerReported((reported, reporter, reason) -> {
    replayFeature.onPlayerReport(reported, reporter, reason);
});
```

### Data Captured Per Frame

Each frame captures:
- Position (x, y, z) with sub-block precision
- Rotation (yaw, pitch)
- Velocity vector
- Movement state (sprinting, sneaking, swimming, flying, gliding)
- Health and armor values
- Status effects (as bitmask)
- Held item slot and ID
- Actions performed (attacks, block breaks, etc.)
- Target entity for combat events

### Storage Format

Replays are stored in `.rbx` binary format:
- **Segment Header** - Player UUID, timestamps, tick rate, checksum
- **Keyframe** - Full player state (first frame)
- **Deltas** - Bit-packed changes from previous frame
- **Compression** - DEFLATE compression for ~90% size reduction

### Performance Optimizations

1. **Object Pooling** - `ReplayFramePool` reuses frame objects to reduce GC pressure
2. **Ring Buffers** - Lock-free `ReplayBuffer` with atomic operations
3. **Delta Encoding** - Only changed fields are stored (typically 2-10 bytes per frame)
4. **Variable-Length Integers** - Timestamps and small values use VarInt encoding
5. **Quantized Values** - Positions stored as fixed-point (1/4096 precision)
6. **Background Compression** - All compression runs on dedicated worker threads
7. **TPS Throttling** - Recording pauses automatically when server TPS drops

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
│   ├── access/           # Access control (whitelist, bans)
│   ├── config/           # Configuration system
│   ├── lifecycle/        # Lifecycle management
│   ├── logging/          # Logging implementation
│   ├── metrics/          # Metrics collection
│   ├── module/           # Module system
│   ├── network/          # Network abstraction
│   ├── performance/      # Performance budgeting
│   ├── scheduler/        # Scheduler implementation
│   └── RubidiumCore.java # Main entry point
├── integration/          # External integrations
│   └── modtale/          # Modtale plugin marketplace
├── qol/                  # Quality of Life features
│   ├── features/         # Individual QoL features
│   │   ├── AfkDetectionFeature.java
│   │   ├── AutoSaveFeature.java
│   │   ├── ChatFormattingFeature.java
│   │   ├── CommandCooldownFeature.java
│   │   ├── JoinLeaveMessagesFeature.java
│   │   ├── LagDetectionFeature.java
│   │   ├── MaintenanceModeFeature.java
│   │   ├── MotdFeature.java
│   │   ├── PlayerStatsFeature.java
│   │   └── StaffToolsFeature.java
│   ├── QoLFeature.java   # Base feature class
│   ├── QoLManager.java   # Feature registry
│   └── QoLCommands.java  # /qol commands
├── replay/               # Moderator Replay System
│   ├── ReplayFrame.java           # Per-tick player state capture
│   ├── ReplayDelta.java           # Delta-compressed frame changes
│   ├── ReplaySegment.java         # Chunk of frames with compression
│   ├── ReplaySession.java         # Full recording session
│   ├── ReplayBuffer.java          # Lock-free ring buffer
│   ├── ReplayFramePool.java       # Object pool for frames
│   ├── ReplayStorageWorker.java   # Async compression & I/O
│   ├── ReplayMetrics.java         # Performance metrics
│   ├── ModeratorReplayFeature.java # QoL feature integration
│   └── ReplayCommands.java        # /replay commands
├── voice/                # Voice Chat System
│   ├── VoiceChatManager.java    # Core voice management
│   ├── VoiceChannel.java        # Channel definitions
│   ├── VoiceState.java          # Player voice state
│   ├── VoiceConfig.java         # Voice configuration
│   ├── ProximityManager.java    # 3D spatial audio
│   └── VoiceMetrics.java        # Performance metrics
├── waypoints/            # Waypoint System
│   ├── WaypointManager.java     # Waypoint CRUD operations
│   ├── Waypoint.java            # Waypoint entity
│   ├── WaypointCategory.java    # Built-in categories
│   ├── WaypointIcon.java        # Icon definitions
│   ├── WaypointConfig.java      # Configuration
│   └── NavigationData.java      # Navigation calculations
├── party/                # Party System
│   ├── PartyManager.java        # Party lifecycle management
│   ├── Party.java               # Party entity
│   ├── PartySettings.java       # Configurable settings
│   └── PartyInvite.java         # Invite handling
├── economy/              # Economy System
│   ├── EconomyManager.java      # Balance & transactions
│   ├── Currency.java            # Multi-currency support
│   ├── Account.java             # Player accounts
│   ├── Transaction.java         # Transaction records
│   └── EconomyConfig.java       # Configuration
├── permissions/          # Permission System
│   ├── PermissionManager.java   # Permission checks & roles
│   ├── Role.java                # Role definitions
│   ├── Permission.java          # Permission nodes
│   ├── PermissionValue.java     # TRUE/FALSE/UNDEFINED
│   ├── PermissionContext.java   # Context-based permissions
│   ├── PlayerPermissions.java   # Per-player data
│   └── PermissionGrant.java     # Direct permission grants
├── devkit/               # Development tools
└── optimization/         # Performance optimization utilities
```

---

## Voice Chat System

Rubidium includes a full-featured voice chat system with proximity audio and channel support.

### Features

- **Proximity Chat** - 3D spatial audio that attenuates with distance
- **Channels** - Global, party, team, and private voice channels
- **Admin Controls** - Server mute, priority speaker, and moderation
- **Quality Settings** - Opus codec with configurable bitrate (16-128 kbps)

### Commands

```
/voice toggle               - Enable/disable voice chat
/voice mute                 - Mute your microphone
/voice deafen               - Deafen yourself
/voice volume <0-200>       - Set output volume
/voice channel join <name>  - Join a voice channel
/voice channel leave        - Leave current channel
/voice admin mute <player>  - Server mute a player
```

### Integration

```java
// Party voice chat auto-join
partyManager.onPartyCreated((party) -> {
    VoiceChannel channel = voiceChat.createChannel("party-" + party.getId(), ChannelType.PARTY);
    party.setVoiceChannel(channel);
});
```

---

## Waypoint System

A comprehensive waypoint and navigation system for players.

### Features

- **Personal Waypoints** - Private markers saved per-player
- **Sharing** - Share waypoints with party, team, or everyone
- **Categories** - Home, death, spawn, dungeon, resource, quest, shop, custom
- **Navigation** - Distance, direction, compass integration
- **Visual Beams** - Configurable light beams at waypoint locations

### Commands

```
/waypoint create <name>     - Create waypoint at current location
/waypoint delete <name>     - Delete a waypoint
/waypoint list              - List your waypoints
/waypoint goto <name>       - Set as navigation target
/waypoint share <name> <player> - Share with another player
/wp <name>                  - Shorthand for navigation
```

### Built-in Categories

| Category | Icon | Color | Use Case |
|----------|------|-------|----------|
| HOME | House | Green | Player bases |
| DEATH | Skull | Red | Death locations |
| SPAWN | Star | Blue | Spawn points |
| POI | Flag | Yellow | Points of interest |
| DUNGEON | Cave | Purple | Dungeons |
| RESOURCE | Pickaxe | Orange | Mining spots |
| QUEST | Quest | Pink | Quest objectives |
| SHOP | Shop | Cyan | Shops |

---

## Party System

A party/group coordination system for multiplayer gameplay.

### Features

- **Party Management** - Create, invite, kick, promote, transfer leadership
- **XP Sharing** - Configurable XP distribution among nearby members
- **Loot Distribution** - Free-for-all, round-robin, need-before-greed, leader decides
- **Friendly Fire** - Configurable damage between party members
- **Integration** - Voice chat and waypoint sharing for party members

### Commands

```
/party create [name]        - Create a new party
/party invite <player>      - Invite a player
/party accept               - Accept party invite
/party leave                - Leave your party
/party kick <player>        - Kick a member
/party promote <player>     - Promote to moderator
/party settings             - View/modify party settings
/p <message>                - Send party chat message
```

### Settings

```java
PartySettings settings = PartySettings.defaults()
    .withMaxMembers(8)
    .withShareXP(true)
    .withFriendlyFire(false)
    .withLootDistribution(LootDistribution.NEED_BEFORE_GREED)
    .withVoiceChat(true)
    .withWaypointSharing(true);
```

---

## Economy System

A multi-currency economy system with ACID-compliant transactions.

### Features

- **Multi-Currency** - Support for multiple currency types (gold, gems, tokens)
- **Secure Transactions** - Thread-safe with deadlock prevention
- **Account System** - Player, shop, guild, and escrow accounts
- **Transaction Logging** - Complete audit trail for all transactions
- **Formatting** - Customizable currency display formats

### Commands

```
/balance [player]           - Check balance
/pay <player> <amount>      - Send money to player
/eco give <player> <amount> - Give money (admin)
/eco take <player> <amount> - Take money (admin)
/eco history <player>       - View transaction history
```

### Integration

```java
// Check balance
long balance = economy.getBalance(playerId, "gold");

// Transfer money
Transaction tx = economy.transfer(from, to, "gold", 100, "Payment for sword");
if (tx.isSuccessful()) {
    player.sendMessage("Payment sent!");
}

// Quest reward
economy.deposit(playerId, "gold", 500, "Quest: Dragon Slayer completed");
```

---

## Permission System

A hierarchical role-based permission system with caching.

### Features

- **Hierarchical Roles** - Role inheritance with priority ordering
- **Wildcard Support** - Use `*` for all permissions
- **Context-Based** - Permissions can be world or server specific
- **Temporary Permissions** - Time-limited permission grants
- **Prefix/Suffix** - Customizable chat prefixes and suffixes
- **Caching** - O(1) cached permission lookups

### Built-in Roles

| Role | Priority | Prefix | Inherits From |
|------|----------|--------|---------------|
| Default | 0 | - | - |
| VIP | 100 | `[VIP]` | Default |
| Moderator | 500 | `[MOD]` | VIP |
| Admin | 1000 | `[ADMIN]` | Moderator |

### Commands

```
/perm check <player> <perm> - Check if player has permission
/perm set <player> <perm> <true|false> - Set player permission
/role add <player> <role>   - Add role to player
/role remove <player> <role> - Remove role from player
/role setprimary <player> <role> - Set primary role
```

### Integration

```java
// Check permission
if (permissions.hasPermission(playerId, "economy.shop.create")) {
    // Allow shop creation
}

// Get prefix for chat
String prefix = permissions.getPrefix(playerId);
String formatted = prefix + playerName + ": " + message;

// Add temporary permission
permissions.setPermission(playerId, "fly.enabled", true, Duration.ofHours(1));
```

---

## Documentation

Comprehensive AI-readable documentation is available in `/rubidium/documentation/`:

- **INDEX.md** - Documentation navigation and overview
- **ARCHITECTURE.md** - System architecture and design patterns
- **VOICE_CHAT.md** - Voice chat system reference
- **WAYPOINTS.md** - Waypoint system reference
- **PARTY_SYSTEM.md** - Party system reference
- **ECONOMY.md** - Economy system reference
- **TELEPORTATION.md** - Teleportation system reference
- **CHAT_SYSTEM.md** - Chat system reference
- **PERMISSIONS.md** - Permission system reference
- **COMMANDS.md** - Complete command reference
- **API_REFERENCE.md** - Full API documentation

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
