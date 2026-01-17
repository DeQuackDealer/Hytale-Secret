# Testing Rubidium Framework with Hytale

This guide explains how to set up and run a local Hytale test server with Rubidium installed.

## Quick Start

### 1. Run the Setup Script

```bash
cd Rubidium-Framework
chmod +x setupserver.sh
./setupserver.sh
```

This will:
- Check for Java 25+
- Create the `test-server/` directory structure
- Copy HytaleServer.jar from libs/
- Build Rubidium and install it to earlyplugins/
- Generate server configuration

### 2. Start the Test Server

```bash
cd test-server
./start.sh
```

The server will start on **localhost:5520** (UDP).

### 3. Stop the Server

Press `Ctrl+C` or run:
```bash
./stop.sh
```

## Directory Structure

After setup, your test server will have:

```
test-server/
├── HytaleServer.jar      # Official Hytale server
├── start.sh              # Start script with optimized JVM flags
├── stop.sh               # Stop script
├── config/
│   └── server.properties # Server configuration
├── earlyplugins/         # Plugin JARs go here
│   └── Rubidium-1.0.0.jar
├── worlds/               # World data
├── assets/               # Asset packs (optional)
└── logs/
    └── server.log        # Server logs
```

## Testing Your Plugin

### 1. Create Your Plugin

Create a new plugin project with this structure:

```
my-plugin/
├── build.gradle.kts
├── src/
│   └── myplugin/
│       └── MyPlugin.java
└── resources/
    └── manifest.json
```

### 2. manifest.json

```json
{
  "Main": "myplugin.MyPlugin",
  "Version": "1.0.0",
  "Name": "MyPlugin",
  "Description": "My awesome Hytale plugin",
  "IncludesAssetPack": false
}
```

### 3. MyPlugin.java (Using Rubidium APIs)

```java
package myplugin;

import rubidium.api.pathfinding.PathfindingAPI;
import rubidium.api.pathfinding.PathfindingAPI.*;
import rubidium.api.npc.NPCAPI;
import rubidium.api.scheduler.SchedulerAPI;
import rubidium.api.hologram.HologramAPI;

import java.util.logging.Logger;

public class MyPlugin {
    
    private static final Logger LOGGER = Logger.getLogger("MyPlugin");
    
    public MyPlugin() {
        LOGGER.info("MyPlugin loading...");
    }
    
    public void onEnable() {
        LOGGER.info("MyPlugin enabled!");
        
        // Create an NPC guard
        var guard = NPCAPI.guard("myplugin:town_guard", "Town Guard");
        var npc = NPCAPI.spawn(guard, Vec3i.of(100, 64, 100));
        
        // Create a hologram
        HologramAPI.text("welcome", Vec3i.of(0, 70, 0),
            "&6Welcome to the Server!",
            "&7Powered by Rubidium"
        );
        
        // Schedule a repeating task
        SchedulerAPI.runTimer("myplugin:check", () -> {
            LOGGER.info("Server tick check");
        }, 100, 200);
        
        // Example: Pathfinding
        var context = PathfindingAPI.createContext(pos -> true); // All passable
        var path = PathfindingAPI.findPath(
            Vec3i.of(0, 64, 0),
            Vec3i.of(10, 64, 10),
            context
        );
        
        if (path.success()) {
            LOGGER.info("Found path with " + path.length() + " nodes");
        }
    }
    
    public void onDisable() {
        LOGGER.info("MyPlugin disabled!");
        SchedulerAPI.cancel("myplugin:check");
    }
}
```

### 4. build.gradle.kts

```kotlin
plugins {
    java
    id("com.gradleup.shadow") version "9.3.1"
}

group = "com.myplugin"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

repositories {
    mavenCentral()
    flatDir { dirs("libs") }
}

dependencies {
    // Add both JARs to your libs/ folder
    compileOnly(files("libs/HytaleServer.jar"))
    compileOnly(files("libs/Rubidium-1.0.0.jar"))
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("MyPlugin")
    archiveClassifier.set("")
}

sourceSets {
    main {
        java { srcDirs("src") }
        resources { srcDirs("resources") }
    }
}
```

### 5. Build and Install

```bash
./gradlew shadowJar
cp build/libs/MyPlugin-1.0.0.jar /path/to/test-server/earlyplugins/
```

### 6. Restart Server

The server will automatically load plugins from `earlyplugins/`.

## Available Rubidium APIs

| API | Description |
|-----|-------------|
| `PathfindingAPI` | A* pathfinding with async support |
| `NPCAPI` | Create and manage NPCs with behaviors |
| `AIBehaviorAPI` | Behavior trees and goal selectors |
| `WorldQueryAPI` | Raycasting, line-of-sight, spatial queries |
| `SchedulerAPI` | Task scheduling, cooldowns, task chains |
| `ScoreboardAPI` | Dynamic scoreboards and teams |
| `HologramAPI` | Floating text and item displays |
| `TeleportAPI` | Warps, TPA requests, back command |
| `BossBarAPI` | Custom boss bars |
| `InventoryAPI` | Custom GUI menus with click handlers |
| `ConfigAPI` | YAML configuration loading/saving |
| `MessageAPI` | Colors, placeholders, localization |

## API Examples

### Pathfinding

```java
// Create a pathfinding context
var context = PathfindingAPI.createContext(
    pos -> world.isPassable(pos),
    (from, to) -> world.getBlockCost(to)
);

// Find path synchronously
var path = PathfindingAPI.findPath(start, goal, context);

// Find path asynchronously
PathfindingAPI.findPathAsync(start, goal, context)
    .thenAccept(result -> {
        if (result.success()) {
            entity.followPath(result.path());
        }
    });
```

### NPCs with AI

```java
// Create NPC definition
var shopkeeper = NPCAPI.create("mymod:shopkeeper")
    .displayName("&6Shopkeeper")
    .type(NPCDefinition.NPCType.MERCHANT)
    .behavior("idle")
    .interactable(true)
    .dialog(
        DialogNode.withOptions("greeting", "Hello! Would you like to trade?",
            DialogOption.of("Yes, show me your wares", "trade"),
            DialogOption.of("No thanks", null)
        )
    )
    .build();

// Spawn NPC
var npc = NPCAPI.spawn(shopkeeper, location);

// Set patrol points
npc.setPatrolPoints(List.of(
    Vec3i.of(0, 64, 0),
    Vec3i.of(10, 64, 0),
    Vec3i.of(10, 64, 10),
    Vec3i.of(0, 64, 10)
));
npc.setBehavior("patrol");
```

### AI Behavior Trees

```java
var tree = AIBehaviorAPI.createTree("guard_ai")
    .root(AIBehaviorAPI.selector(
        // If enemy nearby, attack
        AIBehaviorAPI.condition(
            ctx -> ctx.target() != null,
            AIBehaviorAPI.sequence(
                AIBehaviorAPI.moveToTarget(),
                AIBehaviorAPI.attack()
            )
        ),
        // Otherwise patrol
        AIBehaviorAPI.patrol(patrolPoints)
    ))
    .build();

AIBehaviorAPI.registerTree(tree);
```

### Custom Inventories

```java
var menu = InventoryAPI.create("Shop", 3);
menu.setSlot(13, "diamond", ctx -> {
    // Handle click
    player.sendMessage("You clicked the diamond!");
});
menu.onClose(playerId -> {
    // Handle close
});
InventoryAPI.open(playerId, menu);
```

### Boss Bars

```java
var bar = BossBarAPI.create("boss_health")
    .title("&4Dragon Boss")
    .color(BossBarAPI.BarColor.RED)
    .progress(1.0f)
    .build();

BossBarAPI.register(bar);
BossBarAPI.showTo("boss_health", playerId);

// Update progress
bar.setProgress(boss.getHealth() / boss.getMaxHealth());
```

## Server Configuration

Edit `test-server/config/server.properties`:

```properties
# Server Settings
server-name=Rubidium Test Server
server-port=5520
max-players=20

# Performance
view-distance=8

# Authentication
auth-mode=offline

# Features
enable-command-block=true
spawn-protection=0
allow-flight=true
pvp=true
```

## JVM Arguments

The `start.sh` script includes optimized JVM flags. For debugging:

```bash
# Remote debugging
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
    -jar HytaleServer.jar --accept-early-plugins

# More memory
java -Xms2G -Xmx8G -jar HytaleServer.jar --accept-early-plugins

# AOT compilation (faster startup)
java -XX:AOTCache=HytaleServer.aot -jar HytaleServer.jar --accept-early-plugins
```

## Troubleshooting

1. **Plugin not loading**: Check `earlyplugins/` folder and manifest.json
2. **Port in use**: Change port in server.properties or use `--bind 0.0.0.0:5521`
3. **Out of memory**: Increase `-Xmx` in start.sh
4. **Authentication errors**: Use `--auth-mode offline` for local testing
