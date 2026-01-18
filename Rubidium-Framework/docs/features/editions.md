# Rubidium Editions

Compare Rubidium Free and Rubidium Plus editions.

## Edition Comparison

| Feature | Rubidium (Free) | Rubidium Plus |
|---------|:---------------:|:-------------:|
| **Core Features** | | |
| Performance Optimizations | ✅ | ✅ |
| Plugin System | ✅ | ✅ |
| Command API | ✅ | ✅ |
| Chat API | ✅ | ✅ |
| Event API | ✅ | ✅ |
| Config API | ✅ | ✅ |
| Player API | ✅ | ✅ |
| Server API | ✅ | ✅ |
| **Advanced APIs** | | |
| NPC API | ❌ | ✅ |
| AI Behavior API | ❌ | ✅ |
| Pathfinding API | ❌ | ✅ |
| World Generation API | ❌ | ✅ |
| Inventory API | ❌ | ✅ |
| Economy API | ❌ | ✅ |
| Particles API | ❌ | ✅ |
| Bossbar API | ❌ | ✅ |
| Scoreboard API | ❌ | ✅ |
| **Premium Features** | | |
| Voice Chat System | ❌ | ✅ |
| Minimap with Waypoints | ❌ | ✅ |
| Performance Statistics Overlay | ❌ | ✅ |
| HUD Editor | ❌ | ✅ |
| Admin Panel | ❌ | ✅ |
| Replay System | ❌ | ✅ |
| Hytale UI Integration | ❌ | ✅ |
| **Support** | | |
| Community Support | ✅ | ✅ |
| Priority Support | ❌ | ✅ |
| Direct Developer Access | ❌ | ✅ |
| **Pricing** | | |
| Cost | Free | $29.99/server |

## Rubidium (Free Edition)

The free edition is perfect for getting started with Hytale plugin development.

### Included Features

- **Performance Optimizations**: Memory management, network optimization, thread pooling
- **Plugin System**: Modern plugin loading with lifecycle management
- **Command API**: Full-featured command registration with arguments, permissions, cooldowns
- **Chat API**: Messaging, broadcasts, formatting, bot messages
- **Event API**: Complete event handling and custom events
- **Config API**: YAML, JSON, TOML configuration support
- **Player API**: Player management and data storage
- **Server API**: Server utilities and scheduling

### Use Cases

- Learning Hytale plugin development
- Simple server plugins
- Hobby projects
- Testing and prototyping

### Installation

```kotlin
dependencies {
    compileOnly("com.rubidium:rubidium:1.0.0")
}
```

## Rubidium Plus (Premium Edition)

The premium edition unlocks the full power of Rubidium with advanced features.

### All Free Features Plus:

- **NPC API**: Create intelligent NPCs with dialog, behaviors, and interactions
- **AI Behavior API**: Behavior trees, state machines, and goal-based AI
- **Pathfinding API**: A* pathfinding with navigation meshes
- **World Generation API**: Custom terrain, structures, and biomes
- **Inventory API**: Custom inventory UIs and item management
- **Economy API**: Virtual currencies, shops, and transactions
- **Particles API**: Custom particle effects and animations
- **Bossbar API**: Boss health bars and progress displays
- **Scoreboard API**: Custom scoreboards and objectives

### Premium Features:

- **Voice Chat**: Proximity voice chat with push-to-talk
- **Minimap**: In-game minimap with waypoints and markers
- **Performance Overlay**: Real-time FPS, TPS, memory statistics
- **HUD Editor**: Drag-and-drop HUD customization
- **Admin Panel**: Web-based server administration
- **Replay System**: Record and playback game sessions
- **Hytale UI Integration**: Native CustomUIPage support

### Use Cases

- Professional server networks
- Minigame servers
- RPG and adventure servers
- Commercial server projects

### Installation

```kotlin
dependencies {
    compileOnly("com.rubidium:rubidium-plus:1.0.0")
}
```

## Feature Detection

Check which edition is running at runtime:

```java
import rubidium.core.tier.FeatureRegistry;
import rubidium.core.tier.ProductTier;

// Check current edition
ProductTier tier = FeatureRegistry.getCurrentTier();
System.out.println("Running " + tier.getDisplayName());

// Check if premium
if (FeatureRegistry.isPremium()) {
    // Plus features available
}

// Check specific feature
if (FeatureRegistry.isEnabled("api.npc")) {
    // NPC API is available
    NPCAPI.spawn("guard", location);
}
```

## Graceful Degradation

Write plugins that work with both editions:

```java
public class MyPlugin extends RubidiumPlugin {
    
    @Override
    public void onEnable() {
        // Core features always work
        registerCommands();
        
        // Premium features with fallback
        FeatureRegistry.withFeature("api.npc", () -> {
            // This only runs in Plus edition
            spawnNPCs();
        });
        
        // Or check and notify
        if (!FeatureRegistry.isEnabled("feature.minimap")) {
            getLogger().info("Minimap disabled (requires Rubidium Plus)");
        }
    }
}
```

## Upgrading

To upgrade from Free to Plus:

1. Purchase Rubidium Plus license
2. Replace `rubidium.jar` with `rubidium_plus.jar`
3. Add license key to your server configuration
4. Restart your server

All existing plugins will continue to work with access to new features.

## License

- **Rubidium (Free)**: MIT License - free for any use
- **Rubidium Plus**: Commercial license - per-server pricing

For commercial licensing inquiries, contact: licensing@rubidium.dev
