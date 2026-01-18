# Chat API

Send messages, broadcasts, and NPC speech with rich formatting.

The Chat API provides a comprehensive set of methods for sending messages to players, broadcasting to the server, and making NPCs and bots speak.

## Quick Start

```java
import rubidium.api.chat.ChatAPI;
import rubidium.api.player.Player;

// Send to one player
ChatAPI.sendTo(player, "Hello!");

// Broadcast to all
ChatAPI.broadcast("Server message!");

// Send as a bot
ChatAPI.sendAsBot("ServerBot", "Hello everyone!");
```

## Sending Messages

### To a Single Player

```java
// Basic message
ChatAPI.sendTo(player, "Hello!");

// By UUID
ChatAPI.sendTo(playerUUID, "Hello!");

// Formatted messages
ChatAPI.success(player, "Operation completed!");
ChatAPI.error(player, "Something went wrong!");
ChatAPI.info(player, "Did you know?");
ChatAPI.warning(player, "Be careful!");
ChatAPI.tip(player, "Pro tip: Use /help");
```

### Broadcast to All Players

```java
// Simple broadcast
ChatAPI.broadcast("Server announcement!");

// With filter
ChatAPI.broadcast("VIP message!", player -> player.hasPermission("vip"));

// World-specific
ChatAPI.broadcastWorld("world_nether", "Nether event starting!");
```

## Message Types

| Method | Prefix | Color |
|--------|--------|-------|
| `success(player, msg)` | "Success: " | Green |
| `error(player, msg)` | "Error: " | Red |
| `info(player, msg)` | "Info: " | Aqua |
| `warning(player, msg)` | "Warning: " | Yellow |
| `tip(player, msg)` | "Tip: " | Green |

## Bot and NPC Messages

### Bot Messages

```java
// Send as a bot
ChatAPI.sendAsBot("ServerBot", "Hello everyone!");

// With custom color
ChatAPI.sendAsBot("AdminBot", "Important message!", "&c");
```

### NPC Messages (Plus Only)

```java
import rubidium.api.npc.NPCAPI;

// Get or create an NPC
NPCAPI.NPC npc = NPCAPI.spawn("shopkeeper", location);

// Make NPC speak
ChatAPI.sendAsNPC(npc, "Welcome to my shop!");

// Or use the NPC's speak method
npc.speak("Would you like to buy something?");
```

## Private Messages

```java
// Whisper between players
ChatAPI.whisper(fromPlayer, toPlayer, "Hey, how are you?");
```

This sends:
- To sender: `[You -> PlayerName] Hey, how are you?`
- To receiver: `[SenderName -> You] Hey, how are you?`

## Announcements

```java
// Standard announcement
ChatAPI.announce("Double XP weekend starts now!");

// Server announcement (more prominent)
ChatAPI.announceServer("Server restart in 5 minutes!");
```

## Color Codes

Rubidium uses `&` color codes:

| Code | Color | Code | Format |
|------|-------|------|--------|
| `&0` | Black | `&l` | Bold |
| `&1` | Dark Blue | `&o` | Italic |
| `&2` | Dark Green | `&n` | Underline |
| `&3` | Dark Aqua | `&m` | Strikethrough |
| `&4` | Dark Red | `&k` | Obfuscated |
| `&5` | Dark Purple | `&r` | Reset |
| `&6` | Gold | | |
| `&7` | Gray | | |
| `&8` | Dark Gray | | |
| `&9` | Blue | | |
| `&a` | Green | | |
| `&b` | Aqua | | |
| `&c` | Red | | |
| `&d` | Light Purple | | |
| `&e` | Yellow | | |
| `&f` | White | | |

### Example

```java
ChatAPI.broadcast("&a&lWELCOME &fto &6&lMy Server!");
// Renders as: green bold "WELCOME" + white "to" + gold bold "My Server!"
```

## Events

The Chat API fires events that you can listen to:

```java
import rubidium.api.event.EventAPI;
import rubidium.api.chat.ChatAPI.PlayerChatEvent;
import rubidium.api.chat.ChatAPI.BotChatEvent;
import rubidium.api.chat.ChatAPI.NPCChatEvent;

// Listen for player chat
EventAPI.on(PlayerChatEvent.class, event -> {
    Player player = event.getPlayer();
    String message = event.getMessage();
    String formatted = event.getFormattedMessage();
    
    // Log or modify...
});

// Listen for bot chat
EventAPI.on(BotChatEvent.class, event -> {
    String botName = event.getBotName();
    String message = event.getMessage();
});

// Listen for NPC chat
EventAPI.on(NPCChatEvent.class, event -> {
    NPCAPI.NPC npc = event.getNpc();
    String message = event.getMessage();
});
```

## Player Chat

Send a message as if it came from a player:

```java
// This broadcasts the message with player format
ChatAPI.sendAsPlayer(player, "Hello everyone!");
// Output: <PlayerName> Hello everyone!
```

## Complete Example

```java
package com.example.chatplugin;

import rubidium.api.plugin.RubidiumPlugin;
import rubidium.api.plugin.PluginInfo;
import rubidium.api.command.CommandAPI;
import rubidium.api.chat.ChatAPI;
import rubidium.api.event.EventAPI;
import rubidium.api.chat.ChatAPI.PlayerChatEvent;

@PluginInfo(id = "chat-plugin", name = "Chat Plugin", version = "1.0.0")
public class ChatPlugin extends RubidiumPlugin {
    
    @Override
    public void onEnable() {
        // Welcome command
        CommandAPI.register(CommandAPI.command("welcome")
            .description("Welcome a player")
            .permission("chat.welcome")
            .handler(ctx -> {
                if (ctx.getArgs().length == 0) {
                    ChatAPI.error(ctx.getPlayer(), "Usage: /welcome <player>");
                    return CommandAPI.CommandResult.failure("Missing player");
                }
                
                String playerName = ctx.getArgs()[0];
                ChatAPI.broadcast("&a&l>>> &fPlease welcome &e" + playerName + " &fto the server! &a&l<<<");
                return CommandAPI.CommandResult.success();
            })
            .build());
        
        // Staff announcement command
        CommandAPI.register(CommandAPI.command("staffsay")
            .description("Send a staff message")
            .permission("chat.staff")
            .handler(ctx -> {
                if (ctx.getArgs().length == 0) {
                    ChatAPI.error(ctx.getPlayer(), "Usage: /staffsay <message>");
                    return CommandAPI.CommandResult.failure("Missing message");
                }
                
                String message = String.join(" ", ctx.getArgs());
                ChatAPI.sendAsBot("Staff", message, "&c");
                return CommandAPI.CommandResult.success();
            })
            .build());
        
        // Chat filter example
        EventAPI.on(PlayerChatEvent.class, event -> {
            String message = event.getMessage();
            if (message.contains("badword")) {
                ChatAPI.warning(event.getPlayer(), "Please keep chat family-friendly!");
            }
        });
        
        getLogger().info("Chat Plugin enabled!");
    }
}
```

## Best Practices

1. **Use appropriate message types** - Use `success()`, `error()`, etc. for consistent UX
2. **Color sparingly** - Too many colors can be hard to read
3. **Provide context** - Include relevant information in messages
4. **Handle NPC speech carefully** - Don't spam NPC messages
5. **Filter user input** - Always validate and sanitize player chat
