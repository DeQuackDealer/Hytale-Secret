# Command API

Register and manage server commands with a fluent builder pattern.

The Command API provides a clean, type-safe way to register commands, handle arguments, manage permissions, and implement cooldowns.

## Quick Start

```java
import rubidium.api.command.CommandAPI;
import rubidium.api.chat.ChatAPI;

CommandAPI.register(CommandAPI.command("hello")
    .description("Says hello to the player")
    .handler(context -> {
        ChatAPI.success(context.getPlayer(), "Hello, World!");
        return CommandAPI.CommandResult.success();
    })
    .build());
```

## Command Builder

### Basic Command

```java
CommandAPI.register(CommandAPI.command("spawn")
    .description("Teleport to spawn")
    .permission("server.spawn")
    .handler(context -> {
        // Teleport logic
        return CommandAPI.CommandResult.success();
    })
    .build());
```

### Command with Arguments

```java
CommandAPI.register(CommandAPI.command("give")
    .description("Give items to a player")
    .usage("/give <player> <item> [amount]")
    .permission("admin.give")
    .handler(context -> {
        String[] args = context.getArgs();
        
        if (args.length < 2) {
            return CommandAPI.CommandResult.failure("Usage: /give <player> <item> [amount]");
        }
        
        String playerName = args[0];
        String item = args[1];
        int amount = args.length > 2 ? Integer.parseInt(args[2]) : 1;
        
        // Give item logic...
        
        return CommandAPI.CommandResult.success();
    })
    .build());
```

### Command with Cooldown

```java
CommandAPI.register(CommandAPI.command("heal")
    .description("Heal yourself")
    .cooldown(30) // 30 second cooldown
    .handler(context -> {
        context.getPlayer().setHealth(20);
        ChatAPI.success(context.getPlayer(), "You have been healed!");
        return CommandAPI.CommandResult.success();
    })
    .build());
```

### Admin-Only Command

```java
CommandAPI.register(CommandAPI.command("ban")
    .description("Ban a player")
    .permission("admin.ban")
    .adminOnly(true)
    .handler(context -> {
        // Ban logic
        return CommandAPI.CommandResult.success();
    })
    .build());
```

## Builder Methods

| Method | Description | Example |
|--------|-------------|---------|
| `command(name)` | Create new command | `CommandAPI.command("spawn")` |
| `description(desc)` | Set description | `.description("Teleport to spawn")` |
| `usage(usage)` | Set usage string | `.usage("/give <player> <item>")` |
| `permission(perm)` | Required permission | `.permission("server.spawn")` |
| `cooldown(seconds)` | Cooldown in seconds | `.cooldown(30)` |
| `aliases(...)` | Add aliases | `.aliases("sp", "home")` |
| `adminOnly(bool)` | Require admin | `.adminOnly(true)` |
| `handler(handler)` | Set handler | `.handler(ctx -> ...)` |
| `build()` | Build command | `.build()` |

## Command Context

The `CommandContext` provides access to command execution details:

```java
.handler(context -> {
    // Get the executing player
    Player player = context.getPlayer();
    
    // Get command arguments
    String[] args = context.getArgs();
    
    // Get specific argument with default
    String name = context.getArg(0, "default");
    
    // Get numeric argument
    int count = context.getIntArg(1, 1);
    
    // Check if player has permission
    boolean canAdmin = context.hasPermission("admin.full");
    
    return CommandAPI.CommandResult.success();
})
```

### Context Methods

| Method | Description | Return Type |
|--------|-------------|-------------|
| `getPlayer()` | Executing player | `Player` |
| `getArgs()` | All arguments | `String[]` |
| `getArg(index, default)` | Get argument | `String` |
| `getIntArg(index, default)` | Parse int argument | `int` |
| `hasPermission(perm)` | Check permission | `boolean` |
| `getSender()` | Command sender | `Object` |

## Command Results

Return appropriate results from your handler:

```java
// Success
return CommandAPI.CommandResult.success();

// Success with message
return CommandAPI.CommandResult.success("Item given!");

// Failure
return CommandAPI.CommandResult.failure("Player not found");

// Error with exception
return CommandAPI.CommandResult.error(exception);
```

## Subcommands

Create commands with subcommands:

```java
CommandAPI.register(CommandAPI.command("party")
    .description("Party management")
    .subcommand("create", ctx -> {
        // Create party logic
        ChatAPI.success(ctx.getPlayer(), "Party created!");
        return CommandAPI.CommandResult.success();
    })
    .subcommand("invite", ctx -> {
        if (ctx.getArgs().length < 1) {
            return CommandAPI.CommandResult.failure("Usage: /party invite <player>");
        }
        // Invite logic
        return CommandAPI.CommandResult.success();
    })
    .subcommand("leave", ctx -> {
        // Leave logic
        return CommandAPI.CommandResult.success();
    })
    .handler(ctx -> {
        // Default handler (no subcommand)
        ChatAPI.info(ctx.getPlayer(), "Usage: /party <create|invite|leave>");
        return CommandAPI.CommandResult.success();
    })
    .build());
```

## Tab Completion

Add tab completion suggestions:

```java
CommandAPI.register(CommandAPI.command("gamemode")
    .description("Change game mode")
    .permission("admin.gamemode")
    .tabComplete((sender, args) -> {
        if (args.length == 1) {
            return List.of("survival", "creative", "adventure", "spectator");
        }
        return List.of();
    })
    .handler(context -> {
        // Handler logic
        return CommandAPI.CommandResult.success();
    })
    .build());
```

## Unregistering Commands

```java
// Unregister a single command
CommandAPI.unregister("mycommand");

// Unregister all commands from a plugin
CommandAPI.unregisterAll("my-plugin-id");
```

## Best Practices

### 1. Always Validate Arguments

```java
.handler(ctx -> {
    if (ctx.getArgs().length < 2) {
        ChatAPI.error(ctx.getPlayer(), "Not enough arguments!");
        return CommandAPI.CommandResult.failure("Missing arguments");
    }
    // Continue...
})
```

### 2. Use Permissions

```java
.permission("myplugin.admin.ban")
```

### 3. Provide Clear Usage Messages

```java
.usage("/ban <player> [reason]")
.description("Ban a player from the server")
```

### 4. Handle Errors Gracefully

```java
.handler(ctx -> {
    try {
        // Your logic
        return CommandAPI.CommandResult.success();
    } catch (NumberFormatException e) {
        ChatAPI.error(ctx.getPlayer(), "Invalid number format!");
        return CommandAPI.CommandResult.failure("Invalid number");
    } catch (Exception e) {
        return CommandAPI.CommandResult.error(e);
    }
})
```

## Complete Example

```java
package com.example.myplugin;

import rubidium.api.plugin.RubidiumPlugin;
import rubidium.api.plugin.PluginInfo;
import rubidium.api.command.CommandAPI;
import rubidium.api.chat.ChatAPI;
import rubidium.api.server.Server;

@PluginInfo(id = "teleport-plugin", name = "Teleport Plugin", version = "1.0.0")
public class TeleportPlugin extends RubidiumPlugin {
    
    @Override
    public void onEnable() {
        CommandAPI.register(CommandAPI.command("tp")
            .description("Teleport to a player")
            .usage("/tp <player>")
            .permission("teleport.use")
            .cooldown(5)
            .aliases("teleport", "goto")
            .tabComplete((sender, args) -> {
                if (args.length == 1) {
                    return Server.getOnlinePlayers().stream()
                        .map(p -> p.getName())
                        .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                        .toList();
                }
                return List.of();
            })
            .handler(ctx -> {
                if (ctx.getArgs().length == 0) {
                    ChatAPI.error(ctx.getPlayer(), "Usage: /tp <player>");
                    return CommandAPI.CommandResult.failure("Missing player");
                }
                
                String targetName = ctx.getArgs()[0];
                var target = Server.getPlayer(targetName);
                
                if (target.isEmpty()) {
                    ChatAPI.error(ctx.getPlayer(), "Player not found: " + targetName);
                    return CommandAPI.CommandResult.failure("Player not found");
                }
                
                // Teleport logic here...
                ChatAPI.success(ctx.getPlayer(), "Teleported to " + targetName);
                return CommandAPI.CommandResult.success();
            })
            .build());
    }
}
```
