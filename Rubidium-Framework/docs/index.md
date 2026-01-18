# Rubidium Framework Documentation

> The comprehensive Java SDK for Hytale server plugin development

Welcome to the Rubidium Framework documentation. Rubidium is a powerful API library that extends the official Hytale server plugin system, providing developers with a rich set of tools to create engaging server experiences.

## Quick Links

| Resource | Description |
|----------|-------------|
| [Getting Started](./getting-started/installation.md) | Install and set up Rubidium |
| [API Reference](./api-reference/index.md) | Complete API documentation |
| [Guides](./guides/index.md) | Step-by-step tutorials |
| [Features](./features/index.md) | Feature overview |

## What is Rubidium?

Rubidium is a Java framework that provides:

- **Plugin System** - Modern plugin loading with lifecycle management
- **Command API** - Fluent command registration and argument parsing
- **Chat API** - Rich messaging with formatting and NPC speech
- **Event API** - Flexible event handling and listeners
- **And much more...**

## Editions

### Rubidium (Free)

The free edition includes essential APIs for plugin development:

- Performance optimizations
- Plugin system
- Command API
- Chat API
- Event API
- Config API
- Player API

### Rubidium Plus (Premium)

The premium edition includes everything in Free, plus advanced features:

- NPC API with AI behaviors
- Pathfinding API
- Voice Chat system
- Minimap with waypoints
- HUD Editor
- Admin Panel
- World Generation API
- Economy API
- Particles API
- Replay System
- And more...

[Compare Editions](./features/editions.md)

## Prerequisites

Before you begin, make sure you have:

- Java Development Kit (JDK) 17 or later
- Gradle 8.0 or later (or use the included wrapper)
- IntelliJ IDEA or another Java IDE
- A Hytale server for testing

## Installation

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

## Your First Plugin

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
        
        CommandAPI.register(CommandAPI.command("hello")
            .description("Say hello")
            .handler(ctx -> {
                ChatAPI.success(ctx.getPlayer(), "Hello, World!");
                return CommandAPI.CommandResult.success();
            })
            .build());
    }
}
```

## Community

- [Discord](https://discord.gg/rubidium) - Join our community
- [GitHub](https://github.com/yellow-tale/rubidium) - Report issues and contribute
- [Forums](https://forum.rubidium.dev) - Discuss and share

## License

Rubidium is available under the MIT License for the Free edition.
Rubidium Plus requires a commercial license.
