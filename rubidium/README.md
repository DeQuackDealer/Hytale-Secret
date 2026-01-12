# Rubidium - All-in-One Hytale Development Platform

Rubidium is a comprehensive development platform for Hytale server plugins, providing SDKs for both Java and C#, development tools inspired by Hytale DevKit, optimization APIs, and deep integration with Yellow Tale.

## Components

### Java SDK (`java-sdk/`)
Full-featured Java plugin development kit with:
- **RPAL (Rubidium Plugin API Layer)** - Core plugin abstraction layer
- **Annotations** - `@Plugin`, `@EventHandler`, `@Command`, `@Scheduled`
- **Event System** - Priority-based event handling with cancellation
- **Command Framework** - Annotation-driven command registration
- **Configuration API** - Type-safe YAML/TOML configuration
- **Optimization APIs** - Access to performance tuning features
- **Yellow Tale Integration** - Cosmetics, friends, matchmaking

### C# SDK (`csharp-sdk/`)
.NET-based plugin development for C# developers:
- **Plugin Base Classes** - `RubidiumPlugin`, `PluginModule`
- **Attribute System** - `[Plugin]`, `[EventListener]`, `[Command]`
- **Async/Await Support** - First-class async plugin development
- **Dependency Injection** - Built-in DI container
- **Hot Reload Support** - Live code updates during development

### DevKit Core (`devkit-core/`)
IDE-agnostic development tools:
- **Project Templates** - Java and C# starter projects
- **Code Generators** - Boilerplate generation for common patterns
- **Schema Validation** - Plugin manifest validation
- **Asset Pipeline** - Resource compilation and optimization
- **Debug Bridge** - Remote debugging support

### Gradle Plugin (`gradle-plugin/`)
Build automation for Java plugins:
- **Plugin Packaging** - Automatic JAR bundling with dependencies
- **Manifest Generation** - Auto-generated plugin.toml
- **Resource Processing** - Asset compilation
- **Hot Deploy** - Deploy to running server

## Quick Start

### Java Plugin

```java
package com.example.myplugin;

import com.yellowtale.rubidium.api.*;
import com.yellowtale.rubidium.annotations.*;

@Plugin(
    id = "my-plugin",
    name = "My First Plugin",
    version = "1.0.0",
    author = "Your Name"
)
public class MyPlugin extends RubidiumPlugin {
    
    @Override
    public void onEnable() {
        getLogger().info("Plugin enabled!");
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage("Welcome!");
    }
    
    @Command(name = "hello", permission = "myplugin.hello")
    public void helloCommand(CommandSender sender, String[] args) {
        sender.sendMessage("Hello, World!");
    }
}
```

### C# Plugin

```csharp
using Rubidium.Api;
using Rubidium.Plugin;

[Plugin("my-plugin", "My First Plugin", "1.0.0")]
public class MyPlugin : RubidiumPlugin
{
    public override async Task OnEnableAsync()
    {
        Logger.Info("Plugin enabled!");
    }
    
    [EventListener(Priority.Normal)]
    public void OnPlayerJoin(PlayerJoinEvent e)
    {
        e.Player.SendMessage("Welcome!");
    }
    
    [Command("hello", Permission = "myplugin.hello")]
    public void HelloCommand(ICommandSender sender, string[] args)
    {
        sender.SendMessage("Hello, World!");
    }
}
```

## Features

### Plugin Development
- Multi-language support (Java 17+, C# 10+)
- Hot-reloadable plugins
- Dependency injection
- Event-driven architecture
- Command framework
- Configuration management
- Localization support

### Optimization
- Server tick optimization hooks
- Memory management APIs
- Entity budget control
- Chunk loading optimization
- Network packet batching
- Performance profiling

### Yellow Tale Integration
- Cosmetic verification
- Friend list sync
- Premium feature gates
- Session management
- Analytics bridge

### DevKit Tools
- IntelliJ IDEA integration
- Visual Studio / Rider support
- Project wizards
- Code templates
- Live reload
- Remote debugging

### Anticheat System (`runtime/src/anticheat/`)
Lightweight, event-driven anticheat with minimal RAM usage:
- **Movement Checks** - Speed, fly, no-fall, teleport detection
- **Combat Checks** - CPS limits, reach detection, killaura detection
- **Packet Checks** - Flood protection, keepalive analysis, malformed packet handling
- **Statistical Sampling** - Only samples 25% of events by default (configurable)
- **Ring Buffer Findings** - Fixed-size storage, no memory growth
- **Modular Detection** - Enable/disable individual checks at runtime

### Runtime (`runtime/`)
The Rubidium server runtime (formerly Pond):
- **Plugin Loader** - Hot-reloadable plugins with dependency resolution
- **Task Scheduler** - Priority-based tick scheduling with adaptive throttling
- **Performance Monitor** - Real-time TPS tracking and entity budgeting
- **Session Manager** - Player session tracking with Yellow Tale integration
- **World Heatmaps** - Lightweight activity tracking for optimization

## Architecture

Rubidium follows a standard SDK/Runtime separation pattern:

```
┌─────────────────────────────────────────────────────────────┐
│                    Your Plugin Code                         │
│  (Uses SDK interfaces - compiles against rubidium-api.jar)  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     Rubidium SDK                            │
│  - Java SDK (rubidium-api.jar) - API interfaces/contracts   │
│  - C# SDK (Rubidium.SDK.dll) - API interfaces/contracts     │
│  - DevKit tools for project scaffolding                     │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                  Rubidium Server Runtime                    │
│  - Provides concrete implementations of all SDK interfaces  │
│  - EventBus, CommandManager, Scheduler implementations      │
│  - Anticheat with movement, combat, and packet detection    │
│  - Optimization engine with tick optimizer                  │
│  - Yellow Tale API client and session management            │
│  - Plugin loader, hot-reload, sandboxing                    │
│  - Abstraction layer for future Hytale API compatibility    │
└─────────────────────────────────────────────────────────────┘
```

**SDK (this package)**: Provides API contracts (interfaces, base classes, annotations/attributes) that plugins compile against. This allows plugins to be developed independently of the runtime.

**Rubidium Runtime**: Provides the actual implementations. When plugins are loaded at runtime, the server injects real implementations of `EventBus`, `CommandManager`, `TaskScheduler`, `OptimizationContext`, and `YellowTaleAPI`.

This separation enables:
- Plugins can be compiled without the full server
- API stability across server versions
- Multiple runtime implementations possible
- Clear dependency boundaries

## API Abstraction (Hytale Compatibility)

All game interactions go through abstraction layers (`GameAdapter`, `EntityHandle`, `WorldHandle`) to ensure:
- No direct coupling to game internals
- Future Hytale API can be swapped in via adapter implementations
- Feature flags for API-dependent functionality (`hytale-api` feature)

## Anticheat Details

The anticheat system is designed for minimal resource usage:

| Check Type | RAM Usage | CPU Impact |
|------------|-----------|------------|
| Movement | ~20 snapshots/player | Event-driven |
| Combat | ~20 snapshots/player | Event-driven |
| Packet | Stats only (no history) | Per-packet |
| Findings | Fixed 1000-entry ring | Write-only |

**Key Design Decisions:**
- Statistical sampling (25% by default) reduces CPU by 4x
- Ring buffers prevent memory growth
- No per-tick polling - all event-driven
- Lazy evaluation - checks only run when needed
- Configurable thresholds for different server sizes

## Requirements

- Java 17+ for Java SDK
- .NET 7+ for C# SDK
- Gradle 8+ for build tooling
- Rubidium Server Runtime - for running plugins

## Building

### Java SDK
```bash
cd java-sdk
./gradlew build publish
```

### C# SDK
```bash
cd csharp-sdk
dotnet build
dotnet pack
```

### Gradle Plugin
```bash
cd gradle-plugin
./gradlew build publishToMavenLocal
```

## License

MIT License - Yellow Tale Team
