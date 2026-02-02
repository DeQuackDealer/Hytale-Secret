# API Reference

Complete documentation for all Rubidium APIs.

This reference covers all APIs available in the Rubidium Framework, organized by edition availability.

## Free Edition APIs

These APIs are available in both Free and Plus editions.

| API | Description | Package |
|-----|-------------|---------|
| [Command API](#command-api) | Register and handle commands | `rubidium.api.command` |
| [Chat API](#chat-api) | Send messages and broadcasts | `rubidium.api.chat` |
| [Event API](#event-api) | Handle and fire events | `rubidium.api.event` |
| [Config API](#config-api) | Configuration file management | `rubidium.api.config` |
| [Player API](#player-api) | Player management | `rubidium.api.player` |
| [Server API](#server-api) | Server utilities | `rubidium.api.server` |

## Plus Edition APIs

These APIs require Rubidium Plus.

| API | Description | Package |
|-----|-------------|---------|
| [NPC API](./npc-api.md) | Create and manage NPCs | `rubidium.api.npc` |
| [AI Behavior API](./ai-api.md) | AI behavior trees | `rubidium.api.ai` |
| [Pathfinding API](./pathfinding-api.md) | A* navigation | `rubidium.api.pathfinding` |
| [World Gen API](./worldgen-api.md) | Custom terrain generation | `rubidium.api.worldgen` |
| [Inventory API](./inventory-api.md) | Custom inventories | `rubidium.api.inventory` |
| [Economy API](./economy-api.md) | Virtual currency | `rubidium.api.economy` |
| [Particles API](./particles-api.md) | Particle effects | `rubidium.api.particle` |
| [Bossbar API](./bossbar-api.md) | Boss health bars | `rubidium.api.bossbar` |
| [Scoreboard API](./scoreboard-api.md) | Custom scoreboards | `rubidium.api.scoreboard` |

---

## Command API

The Command API provides a fluent builder for registering and managing server commands.

### Creating Commands

```java
import rubidium.api.command.CommandAPI;
import rubidium.api.command.CommandAPI.CommandContext;
import rubidium.api.command.CommandAPI.CommandDefinition;

// Basic command
CommandAPI.register(CommandAPI.create("ping")
    .description("Check server latency")
    .executor(ctx -> {
        // Handle command
        return true;
    })
    .build());
```

### CommandDefinition.Builder Methods

| Method | Description |
|--------|-------------|
| `create(String name)` | Create a new command builder |
| `.description(String)` | Set command description |
| `.usage(String)` | Set usage string |
| `.permission(String)` | Set required permission |
| `.alias(String)` | Add a single alias |
| `.aliases(String...)` | Add multiple aliases |
| `.playerOnly()` | Restrict to players only |
| `.executor(Function<CommandContext, Boolean>)` | Set command handler |
| `.tabCompleter(Function<CommandContext, List<String>>)` | Set tab completion |
| `.subCommand(String, Function<CommandContext, Boolean>)` | Add subcommand |
| `.subCommand(String, String, Function<CommandContext, Boolean>)` | Add subcommand with permission |
| `.build()` | Build the command definition |

### CommandContext Methods

```java
public record CommandContext(Object sender, String label, String[] args) {
    String arg(int index);                          // Get argument or null
    String arg(int index, String defaultValue);     // Get argument with default
    int argInt(int index, int defaultValue);        // Parse as integer
    double argDouble(int index, double defaultValue); // Parse as double
    boolean hasArg(int index);                      // Check if argument exists
    int argCount();                                 // Get argument count
    String joinArgs(int startIndex);                // Join args from index
}
```

### Complete Example

```java
CommandAPI.register(CommandAPI.create("teleport")
    .description("Teleport to coordinates or players")
    .usage("/teleport <x> <y> <z> | /teleport <player>")
    .permission("server.teleport")
    .aliases("tp", "goto")
    .playerOnly()
    .subCommand("here", "server.teleport.others", ctx -> {
        // /teleport here <player>
        return true;
    })
    .subCommand("spawn", ctx -> {
        // /teleport spawn
        return true;
    })
    .executor(ctx -> {
        if (ctx.argCount() == 1) {
            String target = ctx.arg(0);
            // Teleport to player
        } else if (ctx.argCount() >= 3) {
            double x = ctx.argDouble(0, 0);
            double y = ctx.argDouble(1, 64);
            double z = ctx.argDouble(2, 0);
            // Teleport to coordinates
        }
        return true;
    })
    .tabCompleter(ctx -> {
        if (ctx.argCount() == 1) {
            return List.of("spawn", "home", "PlayerName");
        }
        return List.of();
    })
    .build());
```

### Helper Methods

```java
// Simple command (no player restriction)
CommandAPI.register(CommandAPI.simple("status", "server.status", (sender, args) -> {
    // sender can be console or player
}));

// Player-only command
CommandAPI.register(CommandAPI.playerOnly("heal", "player.heal", (sender, args) -> {
    // sender is guaranteed to be a player
}));

// Get existing command
Optional<CommandDefinition> cmd = CommandAPI.get("teleport");

// Get all commands
Collection<CommandDefinition> all = CommandAPI.all();

// Execute a command programmatically
boolean success = CommandAPI.execute(sender, "/teleport spawn");
```

---

## Chat API

The Chat API provides methods for sending messages to players.

### Import

```java
import rubidium.api.chat.ChatAPI;
```

### Broadcast Methods

```java
// Broadcast to all online players
ChatAPI.broadcast("Message to everyone");

// Broadcast with filter
ChatAPI.broadcast("VIP message", player -> player.hasPermission("vip"));

// Broadcast to specific world
ChatAPI.broadcastWorld("world_hub", "Hub announcement");
```

### Player Messages

```java
// Send to specific player
ChatAPI.sendTo(player, "Direct message");
ChatAPI.sendTo(playerId, "By UUID");

// Styled messages with prefixes
ChatAPI.success(player, "Operation completed!");   // &aSuccess: &7message
ChatAPI.error(player, "Something went wrong");     // &cError: &7message
ChatAPI.warning(player, "Be careful!");            // &eWarning: &7message
ChatAPI.tip(player, "Helpful hint");               // &aTip: &7message
ChatAPI.info(player, "Information");               // &bInfo: &7message
```

### Special Messages

```java
// Announcements
ChatAPI.announce("Important announcement!");      // &6[Announcement] &f
ChatAPI.announceServer("Server message");         // &c[Server] &f

// Bot messages
ChatAPI.sendAsBot("BotName", "Automated message");           // &7[BotName] &f
ChatAPI.sendAsBot("BotName", "Message", "&b");               // Custom color

// NPC messages (Plus only)
ChatAPI.sendAsNPC(npc, "What can I help you with?");         // &e[NPCName] &f
ChatAPI.sendAsNPC("Shopkeeper", "Welcome!");

// Player chat (triggers events)
ChatAPI.sendAsPlayer(player, "Hello everyone!");

// Private messages (whispers)
ChatAPI.whisper(fromPlayer, toPlayer, "Secret message");
```

### Color Codes

Rubidium supports standard color codes with `&`:

| Code | Color | Code | Format |
|------|-------|------|--------|
| `&0` | Black | `&l` | Bold |
| `&1` | Dark Blue | `&m` | Strikethrough |
| `&2` | Dark Green | `&n` | Underline |
| `&3` | Dark Aqua | `&o` | Italic |
| `&4` | Dark Red | `&r` | Reset |
| `&5` | Purple | | |
| `&6` | Gold | | |
| `&7` | Gray | | |
| `&8` | Dark Gray | | |
| `&9` | Blue | | |
| `&a` | Green | | |
| `&b` | Aqua | | |
| `&c` | Red | | |
| `&d` | Pink | | |
| `&e` | Yellow | | |
| `&f` | White | | |

---

## Event API

The Event API provides event handling with priorities and cancellation.

### Import

```java
import rubidium.api.event.EventAPI;
import rubidium.api.event.EventAPI.EventPriority;
import rubidium.api.event.EventAPI.EventListener;
```

### Lambda Registration

```java
// Basic registration (NORMAL priority)
EventAPI.register(EventAPI.PlayerJoinEvent.class, event -> {
    event.setJoinMessage("Welcome!");
});

// With priority
EventAPI.register(EventAPI.PlayerChatEvent.class, event -> {
    event.setFormat("[Chat] %s: %s");
}, EventPriority.HIGH);

// With priority and ignore cancelled
EventAPI.register(EventAPI.BlockBreakEvent.class, event -> {
    // Only runs if not cancelled
}, EventPriority.NORMAL, true);
```

### Annotation Registration

```java
public class MyListeners {
    
    @EventListener
    public void onJoin(EventAPI.PlayerJoinEvent event) {
        // Default NORMAL priority
    }
    
    @EventListener(priority = EventPriority.HIGH)
    public void onChat(EventAPI.PlayerChatEvent event) {
        // High priority
    }
    
    @EventListener(ignoreCancelled = true)
    public void onBreak(EventAPI.BlockBreakEvent event) {
        // Skipped if cancelled
    }
}

// Register listener class
EventAPI.registerListener(new MyListeners());

// Unregister listener
EventAPI.unregisterListener(myListenerInstance);
```

### Event Priorities

| Priority | Order | Use Case |
|----------|-------|----------|
| `LOWEST` | First | Set defaults, can be overridden |
| `LOW` | Second | Early processing |
| `NORMAL` | Third | Standard handlers (default) |
| `HIGH` | Fourth | Late processing |
| `HIGHEST` | Fifth | Final modifications |
| `MONITOR` | Last | Read-only observation, logging |

### Firing Events

```java
// Fire an event
EventAPI.PlayerJoinEvent event = new EventAPI.PlayerJoinEvent(player, "joined");
EventAPI.fire(event);

// Fire and check if cancelled
boolean allowed = EventAPI.fireAndCheck(cancellableEvent);
if (allowed) {
    // Event was not cancelled
}
```

### Built-in Events

| Event | Cancellable | Description |
|-------|-------------|-------------|
| `PlayerJoinEvent` | No | Player joins server |
| `PlayerQuitEvent` | No | Player leaves server |
| `PlayerChatEvent` | Yes | Player sends chat |
| `BlockBreakEvent` | Yes | Block broken |
| `EntityDamageEvent` | Yes | Entity takes damage |

### Custom Events

```java
// Extend Event for non-cancellable
public class MyCustomEvent extends EventAPI.Event {
    private final String data;
    
    public MyCustomEvent(String data) {
        this.data = data;
    }
    
    public String getData() { return data; }
}

// Extend CancellableEvent for cancellable
public class MyCancellableEvent extends EventAPI.CancellableEvent {
    // ...
}

// Fire custom event
EventAPI.fire(new MyCustomEvent("test"));
```

---

## Config API

The Config API provides YAML configuration file management.

### Import

```java
import rubidium.api.config.ConfigAPI;
import rubidium.api.config.ConfigAPI.Config;
```

### Loading Configuration

```java
// Load existing config
Config config = ConfigAPI.load("myconfig");

// Load or create with defaults
Config config = ConfigAPI.loadOrCreate("myconfig", Map.of(
    "settings.enabled", true,
    "settings.max-players", 100,
    "messages.welcome", "Hello!"
));
```

### Reading Values

```java
// Strings
String str = config.getString("path.to.key");
String strDefault = config.getString("path.to.key", "default");

// Numbers
int num = config.getInt("count");
int numDefault = config.getInt("count", 10);
long bigNum = config.getLong("timestamp");
double decimal = config.getDouble("multiplier", 1.0);

// Booleans
boolean flag = config.getBoolean("enabled");
boolean flagDefault = config.getBoolean("enabled", false);

// Collections
List<String> list = config.getStringList("items");
Map<String, Object> section = config.getSection("settings");
Set<String> keys = config.getKeys();
```

### Writing Values

```java
// Set values (supports dot notation for nested keys)
config.set("settings.new-value", "hello");
config.set("settings.count", 42);
config.set("settings.enabled", true);

// Save changes to file
config.save();
```

### Config Builder

```java
// Create defaults using builder
Map<String, Object> defaults = ConfigAPI.builder()
    .set("version", 1)
    .set("settings.enabled", true)
    .set("settings.max-players", 100)
    .section("messages", Map.of(
        "welcome", "Welcome to the server!",
        "goodbye", "See you later!"
    ))
    .build();

Config config = ConfigAPI.loadOrCreate("myconfig", defaults);
```

### Global Operations

```java
// Set config root directory
ConfigAPI.setConfigRoot(Paths.get("plugins/myplugin/config"));

// Get loaded config by name
Optional<Config> config = ConfigAPI.get("myconfig");

// Save all loaded configs
ConfigAPI.saveAll();

// Reload all configs from disk
ConfigAPI.reloadAll();
```

---

## Player API

The Player API provides player management and state access.

### Import

```java
import rubidium.api.player.Player;
import rubidium.api.server.Server;
```

### Getting Players

```java
// Get by UUID
Optional<Player> player = Server.getPlayer(uuid);

// Get all online
Collection<Player> online = Server.getOnlinePlayers();
```

### Player Interface

```java
public interface Player extends CommandSender {
    // Identity
    UUID getUUID();
    String getName();
    String getDisplayName();
    void setDisplayName(String displayName);
    
    // State
    boolean isOnline();
    void kick(String reason);
    String getWorld();
    int getPing();
    String getAddress();
    
    // Time tracking
    long getFirstPlayed();
    long getLastPlayed();
    boolean hasPlayedBefore();
    
    // Position
    Location getLocation();
    double getX();
    double getY();
    double getZ();
    float getYaw();
    float getPitch();
    
    // Teleportation
    void teleport(double x, double y, double z);
    void teleport(double x, double y, double z, float yaw, float pitch);
    void teleport(Location location);
    
    // Health and state
    double getHealth();
    void setHealth(double health);
    double getMaxHealth();
    int getFoodLevel();
    void setFoodLevel(int level);
    
    // Movement state
    boolean isFlying();
    void setFlying(boolean flying);
    boolean isGliding();
    boolean isSprinting();
    boolean isSneaking();
    boolean isSwimming();
    boolean isOnGround();
    
    // Velocity
    double getVelX();
    double getVelY();
    double getVelZ();
    
    // Game mode
    GameMode getGameMode();
    void setGameMode(GameMode mode);
    
    // Operator status
    boolean isOp();
    void setOp(boolean op);
    
    // UI
    void showTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut);
    void showActionBar(String message);
    void playSound(String sound, float volume, float pitch);
    
    // Data
    PlayerInventory getInventory();
    PlayerData getData();
    void sendPacket(Object packet);
}
```

### Location Record

```java
public record Location(double x, double y, double z, float yaw, float pitch) {
    double distanceTo(Location other);
    double distanceSquared(Location other);
}
```

---

## Feature Registry

Check and manage feature availability at runtime.

### Import

```java
import rubidium.core.tier.FeatureRegistry;
import rubidium.core.tier.ProductTier;
```

### Checking Features

```java
// Check edition
ProductTier tier = FeatureRegistry.getCurrentTier();
boolean isPremium = FeatureRegistry.isPremium();

// Check specific feature
boolean hasNPC = FeatureRegistry.isEnabled("api.npc");
boolean needsPlus = FeatureRegistry.requiresPremium("api.npc");
```

### Safe Feature Usage

```java
// Run code only if feature available
FeatureRegistry.withFeature("api.particles", () -> {
    // This only runs if particles API is available
    ParticlesAPI.spawn(location, ParticleType.FLAME, 100);
});

// With return value and fallback
String result = FeatureRegistry.withFeature("api.economy", 
    () -> EconomyAPI.getBalance(player).toString(),
    "Economy unavailable"
);

// Require feature (throws exception if unavailable)
try {
    FeatureRegistry.requireFeature("api.npc");
    // NPC API is available
} catch (FeatureNotAvailableException e) {
    // Handle gracefully
}
```

### Feature IDs

**Free Features:**
- `optimization.memory`, `optimization.network`, `optimization.threading`
- `api.command`, `api.chat`, `api.event`, `api.config`, `api.plugin`, `api.player`

**Plus Features:**
- `api.npc`, `api.ai`, `api.pathfinding`, `api.worldgen`
- `api.inventory`, `api.economy`, `api.particles`, `api.bossbar`, `api.scoreboard`
- `feature.voicechat`, `feature.minimap`, `feature.statistics`
- `feature.hudeditor`, `feature.adminpanel`, `feature.replay`
- `hytale.ui`, `hytale.hud`

### Listing Features

```java
// Get all features
List<Feature> all = FeatureRegistry.getAllFeatures();

// Get by tier
List<Feature> free = FeatureRegistry.getFreeFeatures();
List<Feature> plus = FeatureRegistry.getPlusFeatures();
```

---

## Common Patterns

### Plugin Setup

```java
@PluginInfo(id = "my-plugin", name = "MyPlugin", version = "1.0.0")
public class MyPlugin extends RubidiumPlugin {
    
    @Override
    public void onEnable() {
        // Load config
        ConfigAPI.Config config = ConfigAPI.load("config");
        
        // Register commands
        CommandAPI.register(CommandAPI.create("mycommand")
            .executor(ctx -> { /* ... */ return true; })
            .build());
        
        // Register events
        EventAPI.register(EventAPI.PlayerJoinEvent.class, this::onPlayerJoin);
    }
    
    private void onPlayerJoin(EventAPI.PlayerJoinEvent event) {
        ChatAPI.tip(event.getPlayer(), "Welcome!");
    }
}
```

### Feature-Safe Code

```java
public void doSomething(Player player) {
    // Always works
    ChatAPI.info(player, "Processing...");
    
    // Only if Plus
    FeatureRegistry.withFeature("api.particles", () -> {
        // Show fancy particles
    });
}
```
