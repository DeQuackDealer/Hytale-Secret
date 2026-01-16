# Testing Rubidium Framework Locally

This guide explains how to set up and run a local Rubidium server for testing your plugins.

## Quick Start

### 1. Build the Framework

```bash
cd Rubidium-Framework
./gradlew shadowJar
```

This creates `build/libs/Rubidium-1.0.0.jar`

### 2. Run the Test Server

```bash
chmod +x run-server.sh
./run-server.sh
```

The script will:
- Build the framework if needed
- Create a `test-server/` directory
- Generate default configuration
- Start the server

### 3. Server Console Commands

Once running, you can use these commands:

| Command | Description |
|---------|-------------|
| `help` | List all available commands |
| `plugins` | List loaded plugins |
| `version` | Show server version |
| `reload` | Reload configuration |
| `stop` | Stop the server |

## Directory Structure

After running, your test server will have:

```
test-server/
├── rubidium.jar          # The server JAR
├── eula.txt              # EULA acceptance
├── config/
│   └── server.properties # Server configuration
├── plugins/              # Drop plugin JARs here
├── worlds/               # World data
└── logs/
    └── latest.log        # Server logs
```

## Testing Your Plugin

### 1. Build Your Plugin

For the example DinoMod plugin:

```bash
cd example-plugins/DinoMod
../../gradlew jar
```

### 2. Install the Plugin

Copy the JAR to the plugins folder:

```bash
cp build/libs/DinoMod-1.0.0.jar ../../test-server/plugins/
```

### 3. Restart the Server

The server will automatically load plugins from the `plugins/` folder.

## Configuration

Edit `test-server/config/server.properties`:

```properties
# Server Settings
server-name=My Test Server
server-port=25565
max-players=20

# Performance
view-distance=10
simulation-distance=8

# Features
enable-command-block=true
debug-mode=true
online-mode=false
spawn-protection=0
```

## Creating a Test Plugin

### Minimal Plugin Structure

```
my-plugin/
├── build.gradle.kts
├── plugin.yml
└── src/
    └── myplugin/
        └── MyPlugin.java
```

### plugin.yml

```yaml
name: MyPlugin
version: 1.0.0
main: myplugin.MyPlugin
author: YourName
description: My test plugin
```

### MyPlugin.java

```java
package myplugin;

import rubidium.api.RubidiumPlugin;
import rubidium.api.block.BlockAPI;
import rubidium.api.command.CommandAPI;
import rubidium.api.event.EventAPI;

public class MyPlugin extends RubidiumPlugin {
    
    @Override
    public void onEnable() {
        getLogger().info("MyPlugin enabled!");
        
        // Register a custom block
        BlockAPI.register(
            BlockAPI.create("myplugin:magic_stone")
                .displayName("Magic Stone")
                .hardness(5.0f)
                .lightLevel(10)
                .build()
        );
        
        // Register a command
        CommandAPI.register(
            CommandAPI.create("test")
                .description("Test command")
                .executor(ctx -> {
                    getLogger().info("Test command executed!");
                    return true;
                })
                .build()
        );
        
        // Register an event listener
        EventAPI.register(EventAPI.PlayerJoinEvent.class, event -> {
            getLogger().info("Player joined: " + event.getPlayer());
        });
    }
    
    @Override
    public void onDisable() {
        getLogger().info("MyPlugin disabled!");
    }
}
```

### build.gradle.kts

```kotlin
plugins {
    java
}

group = "com.myplugin"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    compileOnly(files("path/to/Rubidium-1.0.0.jar"))
}

tasks.jar {
    manifest {
        attributes("Plugin-Main" to "myplugin.MyPlugin")
    }
}
```

## API Examples

### Register Custom Items

```java
// Simple material
ItemAPI.register(ItemAPI.material("mymod:crystal"));

// Food item
ItemAPI.register(ItemAPI.food("mymod:apple", 6, 0.6f));

// Custom weapon
ItemAPI.register(ToolAPI.sword("mymod:crystal_sword", ToolAPI.ToolTier.DIAMOND));
```

### Register Custom Entities

```java
// Hostile mob
EntityAPI.register(EntityAPI.monster("mymod:shadow", 40, 8));

// Passive animal
EntityAPI.register(EntityAPI.animal("mymod:bunny", 10));

// Boss entity
EntityAPI.register(EntityAPI.boss("mymod:dragon", 500, 25));
```

### Register Crafting Recipes

```java
// Shaped recipe
RecipeAPI.register(
    RecipeAPI.shaped("mymod:crystal_sword", "mymod:crystal_sword")
        .pattern(" C ", " C ", " S ")
        .key('C', "mymod:crystal")
        .key('S', "stick")
        .build()
);
```

### Create Custom UI

```java
UIScreen shop = UIAPI.createScreen("shop", "Item Shop");
shop.add(UIAPI.label("title", "Welcome to the Shop").centered());
shop.add(UIAPI.button("buy", "Buy Item").on("click", e -> {
    // Handle purchase
}));
UIAPI.open(player, shop);
```

### Economy System

```java
// Give player money
EconomyAPI.deposit(playerId, 100.0);

// Check balance
double balance = EconomyAPI.getBalance(playerId);

// Transfer money
EconomyAPI.transfer(fromId, toId, 50.0);
```

## Debugging Tips

1. **Enable Debug Mode**: Set `debug-mode=true` in server.properties
2. **Check Logs**: View `test-server/logs/latest.log`
3. **Hot Reload**: Use `/reload` to reload configuration without restart
4. **Force Rebuild**: Run `./run-server.sh --rebuild`

## JVM Arguments

For better performance or debugging, modify `run-server.sh`:

```bash
# Debug mode
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar rubidium.jar

# More memory
java -Xms1G -Xmx4G -jar rubidium.jar

# GC tuning
java -XX:+UseZGC -Xms2G -Xmx2G -jar rubidium.jar
```
