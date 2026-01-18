# Creating Your First Plugin

Learn how to create a basic Rubidium plugin from scratch.

This guide walks you through creating a simple plugin that responds to commands and sends messages to players.

## Project Structure

A typical Rubidium plugin has the following structure:

```
MyPlugin/
├── src/main/java/
│   └── com/example/myplugin/
│       └── MyPlugin.java
├── src/main/resources/
│   └── rubidium.yml
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

@PluginInfo(
    id = "my-plugin",
    name = "My Plugin",
    version = "1.0.0",
    author = "Your Name",
    description = "My first Rubidium plugin"
)
public class MyPlugin extends RubidiumPlugin {
    
    @Override
    public void onEnable() {
        getLogger().info("MyPlugin has been enabled!");
        
        registerCommands();
    }
    
    @Override
    public void onDisable() {
        getLogger().info("MyPlugin has been disabled!");
    }
    
    private void registerCommands() {
        // Register a simple greet command
        CommandAPI.register(CommandAPI.command("greet")
            .description("Greets a player")
            .usage("/greet [player]")
            .permission("myplugin.greet")
            .handler(context -> {
                if (context.getArgs().length == 0) {
                    ChatAPI.success(context.getPlayer(), "Hello, " + context.getPlayer().getName() + "!");
                } else {
                    String target = context.getArgs()[0];
                    ChatAPI.broadcast("&a" + context.getPlayer().getName() + " greets " + target + "!");
                }
                return CommandAPI.CommandResult.success();
            })
            .build());
    }
}
```

## Step 2: Understanding the Plugin Lifecycle

Rubidium plugins have a defined lifecycle:

| Method | When Called | Use For |
|--------|-------------|---------|
| `onLoad()` | Before enable | Early initialization |
| `onEnable()` | Plugin starts | Register commands, listeners |
| `onDisable()` | Plugin stops | Cleanup, save data |

## Step 3: Add Command Handling

Commands are the primary way players interact with your plugin:

```java
CommandAPI.register(CommandAPI.command("heal")
    .description("Heals the player")
    .permission("myplugin.heal")
    .cooldown(10) // 10 second cooldown
    .handler(context -> {
        // Heal the player
        context.getPlayer().setHealth(20);
        ChatAPI.success(context.getPlayer(), "You have been healed!");
        return CommandAPI.CommandResult.success();
    })
    .build());
```

### Command with Arguments

```java
CommandAPI.register(CommandAPI.command("give")
    .description("Give items to a player")
    .usage("/give <player> <item> [amount]")
    .permission("myplugin.give")
    .handler(context -> {
        if (context.getArgs().length < 2) {
            ChatAPI.error(context.getPlayer(), "Usage: /give <player> <item> [amount]");
            return CommandAPI.CommandResult.failure("Invalid arguments");
        }
        
        String playerName = context.getArgs()[0];
        String item = context.getArgs()[1];
        int amount = context.getArgs().length > 2 
            ? Integer.parseInt(context.getArgs()[2]) 
            : 1;
        
        // Give the item logic here...
        ChatAPI.success(context.getPlayer(), "Gave " + amount + "x " + item + " to " + playerName);
        return CommandAPI.CommandResult.success();
    })
    .build());
```

## Step 4: Listen for Events

Handle game events using the Event API:

```java
import rubidium.api.event.EventAPI;
import rubidium.api.event.player.PlayerJoinEvent;
import rubidium.api.event.player.PlayerQuitEvent;

@Override
public void onEnable() {
    getLogger().info("Plugin enabled!");
    
    // Listen for player join
    EventAPI.on(PlayerJoinEvent.class, event -> {
        ChatAPI.broadcast("&aWelcome, " + event.getPlayer().getName() + "!");
    });
    
    // Listen for player quit
    EventAPI.on(PlayerQuitEvent.class, event -> {
        ChatAPI.broadcast("&7" + event.getPlayer().getName() + " has left the game.");
    });
}
```

## Step 5: Create Configuration

Create a `rubidium.yml` in your resources folder:

```yaml
# MyPlugin Configuration

# General settings
settings:
  welcome-message: "Welcome to the server!"
  enable-greetings: true

# Command settings
commands:
  greet:
    cooldown: 5
    
# Messages
messages:
  no-permission: "&cYou don't have permission to do that!"
  player-not-found: "&cPlayer not found!"
```

Load configuration in your plugin:

```java
import rubidium.api.config.ConfigAPI;

@Override
public void onEnable() {
    // Load configuration
    ConfigAPI.Config config = ConfigAPI.load(getDataFolder(), "config.yml");
    
    String welcomeMessage = config.getString("settings.welcome-message", "Welcome!");
    boolean enableGreetings = config.getBoolean("settings.enable-greetings", true);
    
    getLogger().info("Welcome message: " + welcomeMessage);
}
```

## Step 6: Build and Test

1. Build your plugin:
   ```bash
   ./gradlew shadowJar
   ```

2. Copy `build/libs/MyPlugin.jar` to your server's `plugins` folder

3. Start the server and test your commands

## Complete Example

Here's a complete example plugin:

```java
package com.example.myplugin;

import rubidium.api.plugin.RubidiumPlugin;
import rubidium.api.plugin.PluginInfo;
import rubidium.api.command.CommandAPI;
import rubidium.api.chat.ChatAPI;
import rubidium.api.event.EventAPI;
import rubidium.api.event.player.PlayerJoinEvent;
import rubidium.api.config.ConfigAPI;

@PluginInfo(
    id = "my-plugin",
    name = "My Plugin",
    version = "1.0.0",
    author = "Your Name",
    description = "A complete example plugin"
)
public class MyPlugin extends RubidiumPlugin {
    
    private ConfigAPI.Config config;
    
    @Override
    public void onEnable() {
        // Load config
        config = ConfigAPI.load(getDataFolder(), "config.yml");
        
        // Register commands
        registerCommands();
        
        // Register event listeners
        registerEvents();
        
        getLogger().info("MyPlugin v" + getVersion() + " enabled!");
    }
    
    private void registerCommands() {
        CommandAPI.register(CommandAPI.command("hello")
            .description("Says hello")
            .handler(ctx -> {
                ChatAPI.sendAsBot("MyPlugin", "Hello, " + ctx.getPlayer().getName() + "!");
                return CommandAPI.CommandResult.success();
            })
            .build());
    }
    
    private void registerEvents() {
        EventAPI.on(PlayerJoinEvent.class, event -> {
            String msg = config.getString("settings.welcome-message", "Welcome!");
            ChatAPI.tip(event.getPlayer(), msg);
        });
    }
    
    @Override
    public void onDisable() {
        getLogger().info("MyPlugin disabled!");
    }
}
```

## Next Steps

Now that you've created your first plugin:

- [Command API Reference](../api-reference/command-api.md) - Advanced command features
- [Event API Reference](../api-reference/event-api.md) - All available events
- [Chat API Reference](../api-reference/chat-api.md) - Messaging features
- [NPC API Guide](../guides/npcs.md) - Create NPCs (Plus only)
