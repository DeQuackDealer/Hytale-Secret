# Rubidium Framework

**Production-Ready Modular Framework for Hytale**

Rubidium is a comprehensive, dual-edition framework designed specifically for Hytale. It provides server operators, plugin developers, and singleplayer modders with a robust foundation for building high-performance, feature-rich experiences.

---

## Editions

Rubidium is available in two editions:

| Feature | Rubidium Free | Rubidium Plus |
|---------|---------------|---------------|
| Core APIs | Yes | Yes |
| Event System | Yes | Yes |
| Command System | Yes | Yes |
| Player Management | Yes | Yes |
| World & Chunk API | Yes | Yes |
| Scheduler API | Yes | Yes |
| Configuration API | Yes | Yes |
| Teleport API | Yes | Yes |
| Pathfinding API | Yes | Yes |
| NPC API | Yes | Yes |
| Chat API | Yes | Yes |
| Voice Chat | No | Yes |
| Minimap & Waypoints | No | Yes |
| Admin UI Panel | No | Yes |
| Performance Stats HUD | No | Yes |
| Anti-Cheat API | No | Yes |
| Replay System | No | Yes |
| Priority Support | No | Yes |

---

## Requirements

- **Java**: 21 or higher
- **Hytale Server**: Compatible with official Hytale server API
- **Build Tool**: Gradle 8.0+ (for development)

---

## Installation

### For Server Operators

1. Download the appropriate JAR:
   - `rubidium.jar` - Free edition
   - `rubidium_plus.jar` - Plus edition (requires license)

2. Place the JAR in your Hytale server's `plugins/` directory

3. Start your server - Rubidium will initialize automatically

### For Singleplayer/Standalone

Rubidium supports standalone mode for singleplayer/LAN games:

**Option 1: Mod Directory**
1. Place the JAR in your game's `mods/` directory
2. The game will automatically load Rubidium

**Option 2: Manual Launch**
```bash
# Standalone mode (no Hytale server required for development)
java -cp "rubidium.jar" rubidium.RubidiumStandaloneEntry

# With Hytale server JAR for full functionality
java -cp "rubidium.jar:HytaleServer.jar" rubidium.RubidiumLauncher
```

**Note**: In standalone mode, Rubidium uses stub implementations for server-side features. When running with a real Hytale server, all features connect automatically via the Runtime Bridge.

---

## Operating Modes

### Server Mode (Default)
When loaded as a Hytale server plugin via `RubidiumHytaleEntry`:
- Full integration with Hytale's event system
- Multi-player support with per-player settings
- Server-wide configuration management
- Admin tools and moderation features

### Singleplayer/Standalone Mode
When launched via `RubidiumStandaloneEntry` or `RubidiumLauncher`:
- Works without a dedicated server
- Local player features (minimap, HUD, settings)
- Mod loading and management
- LAN hosting support

---

## Core Features

### Event System
Subscribe to and handle game events with priority-based processing:

```java
@EventHandler(priority = EventPriority.NORMAL)
public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    player.sendMessage("Welcome to the server!");
}
```

### Command System
Register commands with automatic tab completion:

```java
CommandAPI.register("heal", (sender, args) -> {
    if (sender instanceof Player player) {
        player.setHealth(player.getMaxHealth());
        player.sendMessage("You have been healed!");
    }
});
```

### Scheduler API
Run tasks synchronously or asynchronously:

```java
// Run after 20 ticks (1 second)
SchedulerAPI.runLater(() -> {
    broadcast("Server restarting in 5 minutes!");
}, 20);

// Run every 100 ticks
SchedulerAPI.runTimer(() -> {
    saveAllData();
}, 0, 100);

// Run async
SchedulerAPI.runAsync(() -> {
    return fetchFromDatabase();
}).thenAccept(result -> {
    processResult(result);
});
```

### Configuration API
Type-safe configuration with hot-reloading:

```java
ConfigAPI config = ConfigAPI.load(dataFolder, "config.yml");

int maxPlayers = config.getInt("server.max-players", 100);
boolean pvpEnabled = config.getBoolean("gameplay.pvp", true);
List<String> motd = config.getStringList("messages.motd");

config.set("server.last-restart", System.currentTimeMillis());
config.save();
```

### World & Chunk API
Query and manipulate the world:

```java
World world = WorldAPI.getOrCreate("main");
Chunk chunk = world.getChunkAt(0, 0);

// Check if chunk is loaded
if (chunk.isLoaded()) {
    // Work with chunk data
}

// Teleport player to world spawn
TeleportAPI.teleport(player.getUUID(), world.getSpawnLocation());
```

### Teleport API
Safe teleportation with warps and location history:

```java
// Create a warp
TeleportAPI.createWarp("spawn", new Vec3i(0, 64, 0));

// Teleport to warp
TeleportAPI.teleportToWarp(player.getUUID(), "spawn");

// Save and restore last location
TeleportAPI.saveLastLocation(player.getUUID(), player.getPosition());
TeleportAPI.teleportToLastLocation(player.getUUID());
```

### NPC API
Create and manage NPCs:

```java
// Define NPC type
NPCAPI.NPCDefinition merchant = new NPCAPI.NPCDefinition(
    "merchant",
    "Traveling Merchant",
    "villager",
    true,  // invulnerable
    false  // stationary
);
NPCAPI.registerDefinition(merchant);

// Spawn NPC
NPCAPI.NPC npc = NPCAPI.spawnNPC("merchant", new Vec3i(100, 64, 100));

// Handle interactions
NPCAPI.onInteract(npc.getId(), (player, clickType) -> {
    openShopUI(player);
});
```

### Pathfinding API
A* pathfinding for entities:

```java
PathfindingAPI.PathResult result = PathfindingAPI.findPath(
    start,
    end,
    PathfindingAPI.PathOptions.builder()
        .maxDistance(100)
        .avoidWater(true)
        .build()
);

if (result.isSuccess()) {
    List<Vec3i> path = result.getPath();
    entity.followPath(path);
}
```

### Chat API
Formatted chat with channels:

```java
ChatAPI.broadcast("Server announcement!", ChatAPI.Format.GOLD);
ChatAPI.sendTo(player, "Private message", ChatAPI.Format.ITALIC);

// Create chat channel
ChatAPI.createChannel("staff", ChatAPI.ChannelType.PRIVATE);
ChatAPI.addToChannel(player, "staff");
```

---

## Plus Edition Features

### Voice Chat (Plus)
Proximity-based voice communication:

```java
VoiceChatModule voiceChat = VoiceChatModule.get();

// Configure proximity radius
voiceChat.setProximityRadius(50.0);

// Mute/unmute player
voiceChat.setMuted(player.getUUID(), true);

// Check if player is speaking
boolean speaking = voiceChat.isSpeaking(player.getUUID());
```

### Minimap & Waypoints (Plus)
In-game minimap with custom waypoints:

```java
MinimapModule minimap = MinimapModule.get();

// Create waypoint
minimap.createWaypoint(player.getUUID(), "Home", position, 0x00FF00);

// Toggle minimap visibility
minimap.setVisible(player.getUUID(), true);

// Configure zoom level
minimap.setZoom(player.getUUID(), 2.0f);
```

### Admin UI Panel (Plus)
GUI-based server administration:

```java
AdminUIModule admin = AdminUIModule.get();

// Open admin panel for player
admin.openPanel(player, "players");

// Available panels:
// - players: Player management
// - worlds: World settings
// - permissions: Permission groups
// - server: Server control
// - chunks: Chunk protection
// - items: Item browser
// - teleport: Teleportation
// - bans: Ban management
```

### Anti-Cheat API (Plus)
Movement and combat validation:

```java
AnticheatAPI.setEnabled(true);

// Check player movement
List<Finding> findings = AnticheatAPI.checkMovement(player.getUUID(), snapshot);

for (Finding finding : findings) {
    if (finding.getSeverity() >= Finding.CRITICAL) {
        player.kick("Cheating detected: " + finding.getType());
    }
}

// Configure detection thresholds
AnticheatAPI.setThreshold("speed", 1.5);
AnticheatAPI.setThreshold("reach", 4.0);
```

### Performance Stats HUD (Plus)
Real-time performance monitoring:

```java
PerformanceStatsModule stats = PerformanceStatsModule.get();

// Show stats overlay
stats.showFor(player.getUUID());

// Get current metrics
double tps = stats.getCurrentTPS();
long memoryUsed = stats.getMemoryUsed();
int entityCount = stats.getEntityCount();
```

---

## Hytale UI System

Rubidium provides a native UI system using Hytale's `.ui` layout files:

### Creating UI Pages

```java
public class MySettingsPage extends RubidiumPage {
    @Override
    public String getLayoutFile() {
        return "rubidium/pages/my_settings.ui";
    }
    
    @Override
    protected void onOpen(Player player) {
        UIState state = getState(player);
        state.set("volume", 80);
        state.set("graphics", "high");
    }
    
    @Override
    protected void onEvent(Player player, String eventId, Map<String, Object> data) {
        if (eventId.equals("save_clicked")) {
            saveSettings(player);
        }
    }
}
```

### UI State Management

```java
UIState state = UIState.create();
state.set("playerName", player.getName());
state.set("health", player.getHealth());
state.set("items", inventoryList);

// Bind state to UI elements
UIBinder.bind(page, state);
```

### Overlay System

```java
// Create HUD overlay
RubidiumOverlay overlay = new RubidiumOverlay("stats_overlay");
overlay.addWidget(new StatsWidget());
overlay.show(player);
```

---

## Commands

### Core Commands
```
/rubidium                    - Show framework info
/rubidium reload             - Reload configuration
/rubidium version            - Show version info

/settings                    - Open settings UI
/toggle <feature>            - Toggle a feature on/off
```

### Admin Commands (Plus)
```
/admin                       - Open admin panel
/adminstick                  - Toggle admin interaction mode
/giveadmin <player>          - Grant admin access
/removeadmin <player>        - Revoke admin access
```

### Teleport Commands
```
/warp <name>                 - Teleport to warp
/setwarp <name>              - Create warp at current location
/delwarp <name>              - Delete a warp
/back                        - Return to last location
```

### HUD Commands
```
/hud                         - Open HUD editor
/hud toggle <widget>         - Toggle widget visibility
/hud reset                   - Reset HUD layout
```

---

## Plugin Development

### Creating a Rubidium Plugin

```java
@Plugin(
    id = "my-plugin",
    name = "My Plugin",
    version = "1.0.0",
    author = "YourName"
)
public class MyPlugin extends RubidiumPlugin {
    
    @Override
    public void onEnable() {
        getLogger().info("Plugin enabled!");
        
        // Register events
        EventAPI.register(this, new MyEventListener());
        
        // Register commands
        CommandAPI.register("mycommand", this::handleCommand);
    }
    
    @Override
    public void onDisable() {
        getLogger().info("Plugin disabled!");
    }
    
    private void handleCommand(CommandSender sender, String[] args) {
        sender.sendMessage("Hello from MyPlugin!");
    }
}
```

### Plugin Manifest

Create `META-INF/rubidium.plugin`:

```properties
id=my-plugin
name=My Plugin
version=1.0.0
main=com.example.MyPlugin
api-version=1.0
dependencies=
```

---

## Runtime Bridge

Rubidium connects to the real Hytale server at runtime using a reflection-based bridge:

### How It Works

1. **Detection**: On startup, `HytaleRuntimeBridge` attempts to locate Hytale server classes
2. **Connection**: If found, it hooks into the real event system, command registry, and ECS
3. **Fallback**: In development/test mode, stub implementations are used

### ECS Integration

```java
// The bridge extracts real Hytale ECS references for each player:
// - Ref<Entity>: Player entity reference
// - EntityStore: Component storage for health, position, etc.

// Teleportation uses real Hytale Teleport component
HytaleRuntimeBridge.get().teleportPlayer(playerId, x, y, z);

// Health uses EntityStatMap component
double health = HytaleRuntimeBridge.get().getPlayerHealth(playerId);
```

---

## Testing

Rubidium includes comprehensive test suites:

### Running Tests

```bash
# Unit tests (17 tests)
java -cp "build/classes/java/main:build/resources/main" rubidium.test.RubidiumTestHarness

# Integration tests (5 tests)
java -cp "build/classes/java/main:build/resources/main" rubidium.test.IntegrationTest

# API tests (58 tests)
java -cp "build/classes/java/main:build/resources/main" rubidium.test.APIComprehensiveTest

# Standalone mode tests
java -cp "build/classes/java/main:build/resources/main" rubidium.test.RubidiumTestHarness --standalone
```

### Test Expectations

| Test Suite | Expected Result | Notes |
|------------|-----------------|-------|
| Unit Tests | 17/17 pass | Core functionality |
| Integration Tests | 4/5 pass | 1 expected failure in stub mode* |
| API Tests | 58/58 pass | All API functionality |
| Standalone Tests | All pass | Singleplayer mode |

*The "UI packets sent" test fails in development/stub mode because there's no real Hytale server to send packets to. This test passes when running against a real Hytale server.

---

## Building from Source

```bash
# Clone the repository
git clone https://github.com/yellowtale/rubidium-framework.git
cd rubidium-framework

# Build Free edition
./gradlew rubidiumFreeJar

# Build Plus edition
./gradlew rubidiumPlusJar
```

### Build Outputs

- `build/libs/rubidium.jar` - Free edition (~10.6 MB)
- `build/libs/rubidium_plus.jar` - Plus edition (~10.8 MB)

---

## Project Structure

```
src/rubidium/
├── api/                      # Public APIs
│   ├── anticheat/            # Anti-cheat interfaces
│   ├── block/                # Block manipulation
│   ├── chat/                 # Chat system
│   ├── command/              # Command system
│   ├── config/               # Configuration
│   ├── economy/              # Economy system
│   ├── entity/               # Entity management
│   ├── event/                # Event system
│   ├── hologram/             # Holograms
│   ├── inventory/            # Inventory management
│   ├── item/                 # Item system
│   ├── npc/                  # NPC system
│   ├── particle/             # Particle effects
│   ├── pathfinding/          # A* pathfinding
│   ├── permission/           # Permissions
│   ├── player/               # Player abstraction
│   ├── recipe/               # Crafting recipes
│   ├── scheduler/            # Task scheduling
│   ├── scoreboard/           # Scoreboards
│   ├── server/               # Server management
│   ├── sound/                # Sound system
│   ├── structure/            # Structure generation
│   ├── teleport/             # Teleportation
│   ├── ui/                   # UI system
│   └── world/                # World management
├── core/                     # Core framework
│   ├── access/               # Access control
│   ├── config/               # Config implementation
│   ├── network/              # Network layer
│   ├── scheduler/            # Scheduler implementation
│   ├── tier/                 # Edition management
│   └── HytaleRuntimeBridge.java
├── features/                 # Feature modules
│   ├── minimap/              # Minimap (Plus)
│   └── voicechat/            # Voice chat (Plus)
├── hytale/                   # Hytale integration
│   ├── adapter/              # Player/event adapters
│   ├── api/                  # Plugin loader
│   └── ui/                   # UI pages
├── admin/                    # Admin panel (Plus)
├── anticheat/                # Anti-cheat (Plus)
├── minimap/                  # Minimap module (Plus)
├── voicechat/                # Voice chat module (Plus)
├── stats/                    # Performance stats (Plus)
├── settings/                 # Settings management
├── hud/                      # HUD system
├── ui/                       # UI components
├── test/                     # Test harness
├── RubidiumHytaleEntry.java  # Server entry point
├── RubidiumStandaloneEntry.java  # Standalone entry
└── RubidiumLauncher.java     # Desktop launcher
```

---

## API Reference

### Core Classes

| Class | Description |
|-------|-------------|
| `RubidiumBootstrap` | Framework initialization |
| `HytaleRuntimeBridge` | Runtime Hytale connection |
| `FeatureRegistry` | Edition feature management |
| `PlayerEventHandler` | Player event processing |

### API Classes

| Class | Description |
|-------|-------------|
| `EventAPI` | Event registration and firing |
| `CommandAPI` | Command registration |
| `SchedulerAPI` | Task scheduling |
| `ConfigAPI` | Configuration management |
| `TeleportAPI` | Teleportation system |
| `PathfindingAPI` | A* pathfinding |
| `NPCAPI` | NPC management |
| `ChatAPI` | Chat system |
| `AnticheatAPI` | Anti-cheat (Plus) |
| `UIAPI` | UI management |

### Player Interface

```java
interface Player {
    UUID getUUID();
    String getName();
    void sendMessage(String message);
    void teleport(double x, double y, double z);
    double getHealth();
    void setHealth(double health);
    double getMaxHealth();
    boolean isOnline();
    boolean hasPermission(String permission);
    PlayerInventory getInventory();
    PlayerData getData();
}
```

---

## License

Rubidium Framework is proprietary software.

- **Free Edition**: Available for non-commercial use
- **Plus Edition**: Requires valid license key

For licensing inquiries, contact: licensing@yellowtale.com

---

## Support

- **Documentation**: https://docs.rubidium.dev
- **Discord**: https://discord.gg/rubidium
- **Issues**: https://github.com/yellowtale/rubidium-framework/issues

---

## Implementation Status

All features listed in this README are implemented and tested. The framework includes:

| Category | Status | Test Coverage |
|----------|--------|---------------|
| Core APIs (Event, Command, Scheduler) | Complete | 58 tests |
| Player Management | Complete | Tested |
| World & Chunk API | Complete | Tested |
| Teleport API | Complete | Tested |
| NPC API | Complete | Tested |
| Pathfinding API | Complete | Tested |
| Chat API | Complete | Tested |
| UI System | Complete | 7 tests |
| Voice Chat (Plus) | Complete | Tested |
| Minimap (Plus) | Complete | Tested |
| Admin UI (Plus) | Complete | 8 panels |
| Anti-Cheat (Plus) | Complete | Tested |
| Performance Stats (Plus) | Complete | Tested |
| Runtime Bridge | Complete | Tested |

**Known Limitations**:
- UI packet tracking requires a real Hytale server (fails in stub mode)
- Some features require Hytale server runtime for full functionality
- Standalone mode uses stub implementations for server-side features

---

## Credits

Developed by Yellow Tale & Pond for the Hytale community.

Special thanks to:
- Hypixel Studios for creating Hytale
- The Hytale modding community for feedback and testing
