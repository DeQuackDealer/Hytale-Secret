# API Reference

Complete documentation for all Rubidium APIs.

## Free Edition APIs

These APIs are available in both Free and Plus editions.

| API | Description | Package |
|-----|-------------|---------|
| [Command API](./command-api.md) | Register and handle commands | `rubidium.api.command` |
| [Chat API](./chat-api.md) | Send messages and broadcasts | `rubidium.api.chat` |
| [Event API](./event-api.md) | Handle and fire events | `rubidium.api.event` |
| [Config API](./config-api.md) | Configuration file management | `rubidium.api.config` |
| [Plugin API](./plugin-api.md) | Plugin lifecycle management | `rubidium.api.plugin` |
| [Player API](./player-api.md) | Player management | `rubidium.api.player` |
| [Server API](./server-api.md) | Server utilities | `rubidium.api.server` |

## Plus Edition APIs

These APIs are only available in Rubidium Plus.

| API | Description | Package |
|-----|-------------|---------|
| [NPC API](./npc-api.md) | Create and manage NPCs | `rubidium.api.npc` |
| [AI Behavior API](./ai-api.md) | AI behavior trees | `rubidium.api.ai` |
| [Pathfinding API](./pathfinding-api.md) | A* navigation | `rubidium.api.pathfinding` |
| [World Gen API](./worldgen-api.md) | Custom terrain generation | `rubidium.api.worldgen` |
| [Inventory API](./inventory-api.md) | Custom inventories | `rubidium.api.inventory` |
| [Economy API](./economy-api.md) | Virtual currency | `rubidium.api.economy` |
| [Particles API](./particles-api.md) | Particle effects | `rubidium.api.particles` |
| [Bossbar API](./bossbar-api.md) | Boss health bars | `rubidium.api.bossbar` |
| [Scoreboard API](./scoreboard-api.md) | Custom scoreboards | `rubidium.api.scoreboard` |

## Feature Registry

Check which features are available at runtime:

```java
import rubidium.core.tier.FeatureRegistry;
import rubidium.core.tier.ProductTier;

// Check current edition
if (FeatureRegistry.isPremium()) {
    // Plus features available
}

// Check specific feature
if (FeatureRegistry.isEnabled("api.npc")) {
    // NPC API is available
}

// Safe feature usage
FeatureRegistry.withFeature("api.particles", () -> {
    // This code only runs if particles API is available
    ParticlesAPI.spawn(location, ParticleType.FLAME, 100);
});
```

## Core Classes

### ProductTier

```java
public enum ProductTier {
    FREE("Rubidium", "rubidium.jar"),
    PLUS("Rubidium Plus", "rubidium_plus.jar");
    
    public String getDisplayName();
    public String getJarName();
    public boolean isPremium();
    public boolean isFree();
}
```

### Feature

```java
public record Feature(
    String id,
    String name,
    ProductTier tier,
    String description
) {
    public boolean isEnabled();
    public boolean isPremiumOnly();
}
```

## Quick Reference

### Common Imports

```java
// Core APIs
import rubidium.api.command.CommandAPI;
import rubidium.api.chat.ChatAPI;
import rubidium.api.event.EventAPI;
import rubidium.api.config.ConfigAPI;

// Plugin base
import rubidium.api.plugin.RubidiumPlugin;
import rubidium.api.plugin.PluginInfo;

// Plus APIs
import rubidium.api.npc.NPCAPI;
import rubidium.api.ai.AIBehaviorAPI;
import rubidium.api.pathfinding.PathfindingAPI;
```

### Common Patterns

```java
// Register a command
CommandAPI.register(CommandAPI.command("name")
    .description("Description")
    .permission("perm.node")
    .handler(ctx -> CommandAPI.CommandResult.success())
    .build());

// Listen for events
EventAPI.on(SomeEvent.class, event -> {
    // Handle event
});

// Send messages
ChatAPI.broadcast("Message to all");
ChatAPI.sendTo(player, "Private message");
ChatAPI.sendAsBot("BotName", "Bot says this");
```
