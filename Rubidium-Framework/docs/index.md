# Rubidium Framework Documentation

> The comprehensive Java SDK for Hytale server plugin development

Welcome to the Rubidium Framework documentation. Rubidium is a powerful API library that extends the official Hytale server plugin system, providing developers with a rich set of tools to create engaging server experiences.

## Quick Links

| Resource | Description |
|----------|-------------|
| [Getting Started](./getting-started/installation.md) | Install and set up Rubidium |
| [First Plugin](./getting-started/first-plugin.md) | Create your first plugin |
| [API Reference](./api-reference/index.md) | Complete API documentation |
| [Guides](./guides/index.md) | Step-by-step tutorials |

## What is Rubidium?

Rubidium is a Java framework that provides:

- **Plugin System** - Modern plugin loading with lifecycle management
- **Command API** - Fluent command registration with subcommands and tab completion
- **Chat API** - Rich messaging with formatting, bots, and NPC speech
- **Event API** - Priority-based event handling with cancellation support
- **Config API** - YAML configuration with nested sections and type-safe access
- **Player API** - Comprehensive player management and state tracking
- **And much more...**

## Editions

Rubidium is available in two editions:

### Rubidium (Free)

The free edition includes essential APIs for plugin development:

| Feature | Description |
|---------|-------------|
| Performance Optimizations | Memory management, network optimization, thread pooling |
| Plugin System | Plugin loading and lifecycle management |
| Command API | Register and manage server commands |
| Chat API | Chat messaging and formatting |
| Event API | Event handling and listeners |
| Config API | Configuration file management |
| Player API | Basic player management |

### Rubidium Plus (Premium)

The premium edition includes everything in Free, plus advanced features:

| Feature | Description |
|---------|-------------|
| NPC API | Create and manage NPCs with AI behaviors |
| AI Behavior API | Advanced AI behavior trees and state machines |
| Pathfinding API | A* pathfinding and navigation meshes |
| World Generation API | Custom terrain and structure generation |
| Inventory API | Custom inventory UIs and item management |
| Economy API | Virtual currency and transactions |
| Particles API | Custom particle effects |
| Bossbar API | Boss health bars and progress displays |
| Scoreboard API | Custom scoreboards and objectives |
| Voice Chat | Proximity voice chat with push-to-talk |
| Minimap | In-game minimap with waypoints |
| HUD Editor | Drag-and-drop HUD customization |
| Admin Panel | Server administration interface |
| Replay System | Record and playback sessions |

[Compare Editions](./features/editions.md) | [Upgrade to Plus](https://rubidium.dev/plus)

## Quick Start

### Prerequisites

- Java Development Kit (JDK) 17 or later
- Gradle 8.0 or later (or use the included wrapper)
- IntelliJ IDEA or another Java IDE
- A Hytale server for testing

### Installation

Add Rubidium to your `build.gradle.kts`:

```kotlin
repositories {
    maven { url = uri("https://repo.rubidium.dev/releases") }
}

dependencies {
    compileOnly("com.rubidium:rubidium:1.0.0")
}
```

See the [Installation Guide](./getting-started/installation.md) for detailed setup instructions.

### Your First Plugin

```java
package com.example.myplugin;

import rubidium.api.plugin.RubidiumPlugin;
import rubidium.api.plugin.PluginInfo;
import rubidium.api.command.CommandAPI;
import rubidium.api.chat.ChatAPI;

@PluginInfo(
    id = "my-plugin",
    name = "My Plugin",
    version = "1.0.0"
)
public class MyPlugin extends RubidiumPlugin {
    
    @Override
    public void onEnable() {
        getLogger().info("Plugin enabled!");
        
        // Register a simple command
        CommandAPI.register(CommandAPI.create("hello")
            .description("Say hello")
            .executor(ctx -> {
                ChatAPI.success(ctx.sender(), "Hello, World!");
                return true;
            })
            .build());
    }
}
```

## Feature Detection

Rubidium provides runtime feature detection to gracefully handle edition differences:

```java
import rubidium.core.tier.FeatureRegistry;

// Check if running Plus edition
if (FeatureRegistry.isPremium()) {
    // Plus features available
}

// Check specific feature availability
if (FeatureRegistry.isEnabled("api.npc")) {
    // NPC API is available
}

// Safe feature execution
FeatureRegistry.withFeature("api.particles", () -> {
    // This code only runs if particles API is available
    ParticlesAPI.spawn(location, ParticleType.FLAME, 100);
});

// Require a feature (throws exception if unavailable)
try {
    FeatureRegistry.requireFeature("api.economy");
    // Economy API available
} catch (FeatureNotAvailableException e) {
    // Handle unavailable feature
}
```

## API Overview

### Command API

Create commands with a fluent builder:

```java
CommandAPI.register(CommandAPI.create("teleport")
    .description("Teleport to coordinates")
    .usage("/teleport <x> <y> <z>")
    .permission("myplugin.teleport")
    .playerOnly()
    .subCommand("home", ctx -> { /* handle /teleport home */ return true; })
    .subCommand("spawn", ctx -> { /* handle /teleport spawn */ return true; })
    .executor(ctx -> {
        double x = ctx.argDouble(0, 0);
        double y = ctx.argDouble(1, 64);
        double z = ctx.argDouble(2, 0);
        // Teleport logic
        return true;
    })
    .build());
```

### Chat API

Send messages in various formats:

```java
// Broadcast to all players
ChatAPI.broadcast("Server message");

// Send to specific player
ChatAPI.sendTo(player, "Private message");

// Styled messages
ChatAPI.success(player, "Operation completed!");
ChatAPI.error(player, "Something went wrong");
ChatAPI.warning(player, "Be careful!");
ChatAPI.tip(player, "Helpful hint");
ChatAPI.info(player, "Information");

// Bot and NPC messages
ChatAPI.sendAsBot("ServerBot", "Automated message");
ChatAPI.sendAsNPC("Shopkeeper", "Welcome to my store!");

// Whispers
ChatAPI.whisper(fromPlayer, toPlayer, "Secret message");
```

### Event API

Listen for and handle events:

```java
// Lambda-based listener
EventAPI.register(PlayerJoinEvent.class, event -> {
    ChatAPI.broadcast("Welcome, " + event.getPlayer().getName() + "!");
});

// With priority
EventAPI.register(PlayerChatEvent.class, event -> {
    // Modify chat
}, EventAPI.EventPriority.HIGH);

// Annotation-based listener
public class MyListener {
    @EventAPI.EventListener(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Handle event
    }
}

// Register annotation-based listener
EventAPI.registerListener(new MyListener());
```

## Community

- [Discord](https://discord.gg/rubidium) - Join our community
- [GitHub](https://github.com/DeQuackDealer/Hytale-Secret) - Report issues and contribute
- [Forums](https://forum.rubidium.dev) - Discuss and share

## License

Rubidium is available under the MIT License for the Free edition.
Rubidium Plus requires a commercial license.
