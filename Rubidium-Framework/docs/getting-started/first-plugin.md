# Creating Your First Plugin

Learn how to create a basic Rubidium plugin from scratch.

This guide walks you through creating a simple plugin that responds to commands and sends messages to players.

## Project Structure

A typical Rubidium plugin has the following structure:

```
MyPlugin/
├── src/main/java/
│   └── com/example/myplugin/
│       ├── MyPlugin.java
│       ├── commands/
│       │   └── GreetCommand.java
│       └── listeners/
│           └── JoinListener.java
├── src/main/resources/
│   └── config.yml
├── libs/
│   └── HytaleServer.jar
├── build.gradle.kts
└── settings.gradle.kts
```

## Step 1: Create the Main Class

Every Rubidium plugin needs a main class that extends `RubidiumPlugin`:

```java
package com.example.myplugin;

import rubidium.api.plugin.RubidiumPlugin;
import rubidium.api.plugin.PluginInfo;
import rubidium.api.command.CommandAPI;
import rubidium.api.chat.ChatAPI;
import rubidium.api.event.EventAPI;

@PluginInfo(
    id = "my-plugin",
    name = "My Plugin",
    version = "1.0.0",
    author = "Your Name",
    description = "My first Rubidium plugin"
)
public class MyPlugin extends RubidiumPlugin {
    
    private static MyPlugin instance;
    
    @Override
    public void onLoad() {
        instance = this;
        getLogger().info("MyPlugin is loading...");
    }
    
    @Override
    public void onEnable() {
        getLogger().info("MyPlugin has been enabled!");
        
        registerCommands();
        registerEvents();
    }
    
    @Override
    public void onDisable() {
        getLogger().info("MyPlugin has been disabled!");
    }
    
    public static MyPlugin getInstance() {
        return instance;
    }
    
    private void registerCommands() {
        // We'll add commands here
    }
    
    private void registerEvents() {
        // We'll add event listeners here
    }
}
```

## Step 2: Understanding the Plugin Lifecycle

Rubidium plugins have a defined lifecycle:

| Method | When Called | Use For |
|--------|-------------|---------|
| `onLoad()` | Before enable, after JAR load | Early initialization, static references |
| `onEnable()` | Plugin starts | Register commands, listeners, load config |
| `onDisable()` | Plugin stops | Cleanup, save data, unregister listeners |

The lifecycle order is: `onLoad()` → `onEnable()` → (running) → `onDisable()`

## Step 3: Add a Simple Command

Commands are the primary way players interact with your plugin. Add this to your `registerCommands()` method:

```java
private void registerCommands() {
    // Simple greet command
    CommandAPI.register(CommandAPI.create("greet")
        .description("Greets a player")
        .usage("/greet [player]")
        .permission("myplugin.greet")
        .executor(ctx -> {
            if (!ctx.hasArg(0)) {
                ChatAPI.success(ctx.sender(), "Hello, World!");
            } else {
                String target = ctx.arg(0);
                ChatAPI.broadcast("&a" + target + " has been greeted!");
            }
            return true;
        })
        .build());
}
```

### Command Context Methods

The `CommandContext` provides useful methods for handling arguments:

```java
CommandAPI.register(CommandAPI.create("example")
    .executor(ctx -> {
        // Get arguments
        String arg0 = ctx.arg(0);                    // First argument (null if missing)
        String arg1 = ctx.arg(1, "default");         // With default value
        int count = ctx.argInt(0, 1);                // Parse as integer
        double amount = ctx.argDouble(1, 0.0);       // Parse as double
        
        // Check arguments
        if (!ctx.hasArg(0)) {
            ChatAPI.error(ctx.sender(), "Missing argument!");
            return false;
        }
        
        // Get argument count
        int argCount = ctx.argCount();
        
        // Join remaining arguments (for messages)
        String message = ctx.joinArgs(1);  // Join from index 1 onward
        
        return true;
    })
    .build());
```

### Command with Subcommands

```java
CommandAPI.register(CommandAPI.create("home")
    .description("Manage your homes")
    .subCommand("set", ctx -> {
        String homeName = ctx.arg(0, "home");
        ChatAPI.success(ctx.sender(), "Home '" + homeName + "' set!");
        return true;
    })
    .subCommand("delete", ctx -> {
        String homeName = ctx.arg(0);
        if (homeName == null) {
            ChatAPI.error(ctx.sender(), "Specify a home to delete");
            return false;
        }
        ChatAPI.success(ctx.sender(), "Home '" + homeName + "' deleted!");
        return true;
    })
    .subCommand("list", ctx -> {
        ChatAPI.info(ctx.sender(), "Your homes: home, spawn, base");
        return true;
    })
    .executor(ctx -> {
        // Default action when no subcommand
        ChatAPI.info(ctx.sender(), "Usage: /home <set|delete|list>");
        return true;
    })
    .build());
```

### Player-Only Commands

```java
// Quick helper for player-only commands
CommandAPI.register(CommandAPI.playerOnly("heal", "myplugin.heal", (sender, args) -> {
    // sender is guaranteed to be a player
    ChatAPI.success(sender, "You have been healed!");
}));
```

## Step 4: Listen for Events

Handle game events using the Event API. Add this to your `registerEvents()` method:

```java
private void registerEvents() {
    // Lambda-based listener for player join
    EventAPI.register(EventAPI.PlayerJoinEvent.class, event -> {
        ChatAPI.broadcast("&aWelcome, " + event.getPlayer() + "!");
        event.setJoinMessage("&7" + event.getPlayer() + " joined the game");
    });
    
    // Lambda-based listener for player quit
    EventAPI.register(EventAPI.PlayerQuitEvent.class, event -> {
        event.setQuitMessage("&7" + event.getPlayer() + " left the game");
    });
    
    // Listen for chat with priority
    EventAPI.register(EventAPI.PlayerChatEvent.class, event -> {
        // Modify chat format
        event.setFormat("&7[Player] &f%s: %s");
    }, EventAPI.EventPriority.HIGH);
}
```

### Event Priorities

Events are processed in priority order:

| Priority | When to Use |
|----------|-------------|
| `LOWEST` | First to run, can be overridden |
| `LOW` | Early processing |
| `NORMAL` | Default, most listeners |
| `HIGH` | Late processing |
| `HIGHEST` | Last to run before monitor |
| `MONITOR` | Read-only observation, don't modify |

### Cancellable Events

Some events can be cancelled to prevent the action:

```java
EventAPI.register(EventAPI.BlockBreakEvent.class, event -> {
    // Prevent breaking blocks in spawn area
    if (isInSpawn(event.getBlock())) {
        event.setCancelled(true);
        ChatAPI.error(event.getPlayer(), "You cannot break blocks in spawn!");
    }
});
```

### Annotation-Based Listeners

For cleaner code with many listeners:

```java
public class MyListener {
    
    @EventAPI.EventListener
    public void onPlayerJoin(EventAPI.PlayerJoinEvent event) {
        ChatAPI.broadcast("&aWelcome!");
    }
    
    @EventAPI.EventListener(priority = EventAPI.EventPriority.HIGH)
    public void onChat(EventAPI.PlayerChatEvent event) {
        // High priority chat handling
    }
    
    @EventAPI.EventListener(ignoreCancelled = true)
    public void onBlockBreak(EventAPI.BlockBreakEvent event) {
        // Only runs if event wasn't cancelled
    }
}

// In your main class:
private void registerEvents() {
    EventAPI.registerListener(new MyListener());
}
```

## Step 5: Create Configuration

Create `src/main/resources/config.yml`:

```yaml
# MyPlugin Configuration

settings:
  welcome-message: "Welcome to the server!"
  enable-greetings: true
  max-homes: 5

messages:
  no-permission: "&cYou don't have permission!"
  player-not-found: "&cPlayer not found!"
  
features:
  announcements: true
  join-effects: false
```

Load configuration in your plugin:

```java
import rubidium.api.config.ConfigAPI;

public class MyPlugin extends RubidiumPlugin {
    
    private ConfigAPI.Config config;
    
    @Override
    public void onEnable() {
        try {
            // Load or create config with defaults
            config = ConfigAPI.loadOrCreate("myplugin", Map.of(
                "settings.welcome-message", "Welcome to the server!",
                "settings.enable-greetings", true,
                "settings.max-homes", 5
            ));
        } catch (IOException e) {
            getLogger().severe("Failed to load config: " + e.getMessage());
        }
        
        // Read values
        String welcomeMsg = config.getString("settings.welcome-message");
        boolean greetings = config.getBoolean("settings.enable-greetings", true);
        int maxHomes = config.getInt("settings.max-homes", 5);
        
        getLogger().info("Welcome message: " + welcomeMsg);
    }
    
    @Override
    public void onDisable() {
        // Save config changes
        try {
            config.save();
        } catch (IOException e) {
            getLogger().severe("Failed to save config: " + e.getMessage());
        }
    }
}
```

### Config API Methods

```java
// Reading values
String str = config.getString("path.to.key");
String strDefault = config.getString("path.to.key", "default");
int num = config.getInt("path.to.int");
long bigNum = config.getLong("path.to.long");
double decimal = config.getDouble("path.to.double");
boolean flag = config.getBoolean("path.to.bool", false);
List<String> list = config.getStringList("path.to.list");
Map<String, Object> section = config.getSection("settings");

// Writing values
config.set("settings.new-value", "hello");
config.set("settings.count", 42);

// Check existence
if (config.contains("settings.optional")) {
    // Key exists
}

// Get all keys
Set<String> keys = config.getKeys();
```

## Step 6: Using the Chat API

The Chat API provides various messaging methods:

```java
import rubidium.api.chat.ChatAPI;

// Broadcast to all players
ChatAPI.broadcast("Message to everyone");
ChatAPI.broadcast("Filtered message", player -> player.hasPermission("see.message"));

// Send to specific player
ChatAPI.sendTo(player, "Private message");
ChatAPI.sendTo(playerId, "By UUID");

// Styled messages
ChatAPI.success(player, "Operation completed!");  // Green prefix
ChatAPI.error(player, "Something went wrong");    // Red prefix
ChatAPI.warning(player, "Be careful!");           // Yellow prefix
ChatAPI.tip(player, "Helpful hint");              // Green tip
ChatAPI.info(player, "Information");              // Blue info

// Announcements
ChatAPI.announce("Important announcement!");      // Yellow [Announcement]
ChatAPI.announceServer("Server restarting...");   // Red [Server]

// Bot messages
ChatAPI.sendAsBot("ServerBot", "Automated message");
ChatAPI.sendAsBot("CustomBot", "Message", "&b");  // With custom color

// NPC messages (Plus only)
ChatAPI.sendAsNPC("Shopkeeper", "Welcome to my store!");

// Private messaging
ChatAPI.whisper(fromPlayer, toPlayer, "Secret message");

// World-specific broadcast
ChatAPI.broadcastWorld("world_hub", "Hub announcement!");
```

## Step 7: Build and Test

1. Build your plugin:
   ```bash
   ./gradlew shadowJar
   ```

2. Copy `build/libs/MyPlugin.jar` to your server's `plugins` folder

3. Start the server and check the console:
   ```
   [MyPlugin] MyPlugin is loading...
   [MyPlugin] MyPlugin has been enabled!
   ```

4. Test your commands in-game:
   - `/greet` - Should say "Hello, World!"
   - `/greet PlayerName` - Should broadcast a greeting

## Complete Example

Here's a complete working plugin:

```java
package com.example.myplugin;

import rubidium.api.plugin.RubidiumPlugin;
import rubidium.api.plugin.PluginInfo;
import rubidium.api.command.CommandAPI;
import rubidium.api.chat.ChatAPI;
import rubidium.api.event.EventAPI;
import rubidium.api.config.ConfigAPI;

import java.io.IOException;
import java.util.Map;

@PluginInfo(
    id = "my-plugin",
    name = "My Plugin",
    version = "1.0.0",
    author = "Your Name",
    description = "A complete example plugin"
)
public class MyPlugin extends RubidiumPlugin {
    
    private static MyPlugin instance;
    private ConfigAPI.Config config;
    
    @Override
    public void onLoad() {
        instance = this;
    }
    
    @Override
    public void onEnable() {
        // Load configuration
        loadConfig();
        
        // Register commands
        registerCommands();
        
        // Register event listeners
        registerEvents();
        
        getLogger().info("MyPlugin v" + getVersion() + " enabled!");
    }
    
    private void loadConfig() {
        try {
            config = ConfigAPI.loadOrCreate("myplugin", Map.of(
                "settings.welcome-message", "Welcome to the server!",
                "settings.enable-greetings", true
            ));
        } catch (IOException e) {
            getLogger().severe("Config error: " + e.getMessage());
        }
    }
    
    private void registerCommands() {
        // Hello command
        CommandAPI.register(CommandAPI.create("hello")
            .description("Says hello")
            .executor(ctx -> {
                ChatAPI.sendAsBot("MyPlugin", "Hello, " + ctx.sender() + "!");
                return true;
            })
            .build());
        
        // Greet command with argument
        CommandAPI.register(CommandAPI.create("greet")
            .description("Greet someone")
            .usage("/greet <name>")
            .executor(ctx -> {
                String name = ctx.arg(0, "World");
                ChatAPI.broadcast("&aHello, " + name + "!");
                return true;
            })
            .build());
    }
    
    private void registerEvents() {
        // Welcome message on join
        EventAPI.register(EventAPI.PlayerJoinEvent.class, event -> {
            if (config.getBoolean("settings.enable-greetings", true)) {
                String msg = config.getString("settings.welcome-message", "Welcome!");
                ChatAPI.tip(event.getPlayer(), msg);
            }
        });
    }
    
    @Override
    public void onDisable() {
        try {
            if (config != null) config.save();
        } catch (IOException e) {
            getLogger().severe("Failed to save config");
        }
        getLogger().info("MyPlugin disabled!");
    }
    
    public static MyPlugin getInstance() {
        return instance;
    }
}
```

## Next Steps

Now that you've created your first plugin:

- **[Command API Reference](../api-reference/command-api.md)** - Advanced command features
- **[Event API Reference](../api-reference/event-api.md)** - All available events
- **[Chat API Reference](../api-reference/chat-api.md)** - Messaging features
- **[Config API Reference](../api-reference/config-api.md)** - Configuration management
- **[NPC API Guide](../guides/npcs.md)** - Create NPCs (Plus only)
- **[Economy API Guide](../guides/economy.md)** - Virtual currency (Plus only)
