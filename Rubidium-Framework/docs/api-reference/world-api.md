# WorldAPI Reference

## Overview

The `WorldAPI` provides world management functionality for Rubidium. It allows you to control world time, weather, blocks, particles, sounds, and world effects like explosions and lightning.

**Package:** `rubidium.api.world`

## Getting Started

```java
import rubidium.api.world.WorldAPI;
import rubidium.api.world.WorldAPI.Weather;
import rubidium.api.world.WorldAPI.WorldState;

// Get the singleton instance
WorldAPI world = WorldAPI.get();
```

## Public Methods

### Instance Access

#### `get()`
Returns the singleton instance of the WorldAPI.

```java
public static WorldAPI get()
```

**Returns:** `WorldAPI` - The singleton instance

---

### World Access

#### `getWorld(String name)`
Gets or creates a world state by name.

```java
public WorldState getWorld(String name)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `name` | `String` | The world name |

**Returns:** `WorldState` - The world state object

**Example:**
```java
WorldState overworld = WorldAPI.get().getWorld("world");
WorldState nether = WorldAPI.get().getWorld("world_nether");
```

#### `getWorldNames()`
Gets all registered world names.

```java
public Collection<String> getWorldNames()
```

**Returns:** `Collection<String>` - An unmodifiable set of world names

**Example:**
```java
for (String worldName : WorldAPI.get().getWorldNames()) {
    System.out.println("World: " + worldName);
}
```

---

### Time Management

#### `setTime(String worldName, long time)`
Sets the time in a world.

```java
public void setTime(String worldName, long time)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `worldName` | `String` | The world name |
| `time` | `long` | The time in ticks (0-24000) |

**Time Values:**
| Time | Ticks | Description |
|------|-------|-------------|
| Sunrise | 0 | Dawn begins |
| Noon | 6000 | Middle of day |
| Sunset | 12000 | Dusk begins |
| Midnight | 18000 | Middle of night |

**Example:**
```java
// Set to noon
WorldAPI.get().setTime("world", 6000);

// Set to midnight
WorldAPI.get().setTime("world", 18000);
```

#### `getTime(String worldName)`
Gets the current time in a world.

```java
public long getTime(String worldName)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `worldName` | `String` | The world name |

**Returns:** `long` - The current time in ticks

---

### Weather Management

#### `setWeather(String worldName, Weather weather)`
Sets the weather in a world.

```java
public void setWeather(String worldName, Weather weather)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `worldName` | `String` | The world name |
| `weather` | `Weather` | The weather type |

**Example:**
```java
// Set to rain
WorldAPI.get().setWeather("world", Weather.RAIN);

// Set to thunderstorm
WorldAPI.get().setWeather("world", Weather.THUNDER);

// Clear weather
WorldAPI.get().setWeather("world", Weather.CLEAR);
```

#### `getWeather(String worldName)`
Gets the current weather in a world.

```java
public Weather getWeather(String worldName)
```

**Returns:** `Weather` - The current weather type

---

### Block Manipulation

#### `setBlock(String worldName, int x, int y, int z, String blockType)`
Sets a block at the specified coordinates.

```java
public void setBlock(String worldName, int x, int y, int z, String blockType)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `worldName` | `String` | The world name |
| `x` | `int` | X coordinate |
| `y` | `int` | Y coordinate |
| `z` | `int` | Z coordinate |
| `blockType` | `String` | The block type identifier |

**Example:**
```java
// Place a diamond block
WorldAPI.get().setBlock("world", 100, 64, 100, "hytale:diamond_block");

// Remove a block (set to air)
WorldAPI.get().setBlock("world", 100, 64, 100, "hytale:air");
```

#### `getBlock(String worldName, int x, int y, int z)`
Gets the block type at the specified coordinates.

```java
public String getBlock(String worldName, int x, int y, int z)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `worldName` | `String` | The world name |
| `x` | `int` | X coordinate |
| `y` | `int` | Y coordinate |
| `z` | `int` | Z coordinate |

**Returns:** `String` - The block type identifier (defaults to "hytale:air" if unavailable)

**Example:**
```java
String blockType = WorldAPI.get().getBlock("world", 100, 64, 100);
if (blockType.equals("hytale:diamond_ore")) {
    // Found diamond ore!
}
```

---

### Sound Effects

#### `playSound(String worldName, double x, double y, double z, String sound, float volume, float pitch)`
Plays a sound at the specified location.

```java
public void playSound(String worldName, double x, double y, double z, String sound, float volume, float pitch)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `worldName` | `String` | The world name |
| `x` | `double` | X coordinate |
| `y` | `double` | Y coordinate |
| `z` | `double` | Z coordinate |
| `sound` | `String` | The sound identifier |
| `volume` | `float` | Volume (0.0 to 1.0) |
| `pitch` | `float` | Pitch (0.5 to 2.0, 1.0 is normal) |

**Example:**
```java
// Play explosion sound
WorldAPI.get().playSound("world", 100.5, 64.0, 200.5, "hytale:explosion", 1.0f, 1.0f);

// Play higher pitched sound
WorldAPI.get().playSound("world", 100.5, 64.0, 200.5, "hytale:ding", 0.8f, 1.5f);
```

---

### Particle Effects

#### `spawnParticle(String worldName, double x, double y, double z, String particle, int count)`
Spawns particles at the specified location.

```java
public void spawnParticle(String worldName, double x, double y, double z, String particle, int count)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `worldName` | `String` | The world name |
| `x` | `double` | X coordinate |
| `y` | `double` | Y coordinate |
| `z` | `double` | Z coordinate |
| `particle` | `String` | The particle type identifier |
| `count` | `int` | Number of particles to spawn |

**Example:**
```java
// Spawn smoke particles
WorldAPI.get().spawnParticle("world", 100.5, 65.0, 200.5, "hytale:smoke", 20);

// Spawn magic particles
WorldAPI.get().spawnParticle("world", 100.5, 65.0, 200.5, "hytale:magic", 50);
```

---

### World Effects

#### `createExplosion(String worldName, double x, double y, double z, float power, boolean fire, boolean breakBlocks)`
Creates an explosion at the specified location.

```java
public void createExplosion(String worldName, double x, double y, double z, float power, boolean fire, boolean breakBlocks)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `worldName` | `String` | The world name |
| `x` | `double` | X coordinate |
| `y` | `double` | Y coordinate |
| `z` | `double` | Z coordinate |
| `power` | `float` | Explosion power (4.0 = TNT) |
| `fire` | `boolean` | Whether to create fire |
| `breakBlocks` | `boolean` | Whether to break blocks |

**Example:**
```java
// Create TNT-like explosion
WorldAPI.get().createExplosion("world", 100.0, 64.0, 200.0, 4.0f, true, true);

// Create visual-only explosion (no damage or blocks broken)
WorldAPI.get().createExplosion("world", 100.0, 64.0, 200.0, 2.0f, false, false);
```

#### `strikeLightning(String worldName, double x, double y, double z, boolean damage)`
Strikes lightning at the specified location.

```java
public void strikeLightning(String worldName, double x, double y, double z, boolean damage)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `worldName` | `String` | The world name |
| `x` | `double` | X coordinate |
| `y` | `double` | Y coordinate |
| `z` | `double` | Z coordinate |
| `damage` | `boolean` | Whether the lightning deals damage |

**Example:**
```java
// Strike damaging lightning
WorldAPI.get().strikeLightning("world", 100.0, 64.0, 200.0, true);

// Strike visual-only lightning
WorldAPI.get().strikeLightning("world", 100.0, 64.0, 200.0, false);
```

---

### Player Queries

#### `getPlayersInWorld(String worldName)`
Gets all players currently in a world.

```java
public List<UUID> getPlayersInWorld(String worldName)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `worldName` | `String` | The world name |

**Returns:** `List<UUID>` - List of player UUIDs in the world

**Example:**
```java
List<UUID> players = WorldAPI.get().getPlayersInWorld("world");
for (UUID playerId : players) {
    // Do something with each player
}
```

#### `getPlayerCount(String worldName)`
Gets the number of players in a world.

```java
public int getPlayerCount(String worldName)
```

**Returns:** `int` - The number of players in the world

**Example:**
```java
int count = WorldAPI.get().getPlayerCount("world");
System.out.println("Players in world: " + count);
```

---

## Enums

### Weather

```java
public enum Weather {
    CLEAR,    // Clear skies
    RAIN,     // Raining
    THUNDER,  // Thunderstorm
    SNOW      // Snowing
}
```

---

## Inner Classes

### WorldState

Represents the state of a world.

```java
public static class WorldState {
    public WorldState(String name)
    
    // Getters
    public String getName()
    public long getTime()
    public Weather getWeather()
    public boolean isPvpEnabled()
    public int getSpawnX()
    public int getSpawnY()
    public int getSpawnZ()
    
    // Setters
    public void setTime(long time)      // Time wraps at 24000
    public void setWeather(Weather weather)
    public void setPvpEnabled(boolean enabled)
    public void setSpawn(int x, int y, int z)
    
    // Utilities
    public boolean isDay()              // Returns true if time is 0-12000
    public boolean isNight()            // Returns true if time is 12000-24000
}
```

**Example:**
```java
WorldState world = WorldAPI.get().getWorld("world");

// Check time of day
if (world.isNight()) {
    // Spawn monsters
}

// Update spawn point
world.setSpawn(0, 100, 0);

// Disable PvP
world.setPvpEnabled(false);
```

---

## Default Worlds

The following worlds are pre-registered:

| World Name | Description |
|------------|-------------|
| `world` | The main overworld |
| `world_nether` | The Nether dimension |
| `world_the_end` | The End dimension |

---

## Best Practices

1. **Use world names consistently** - Use the exact world name strings ("world", "world_nether", etc.).

2. **Time is in ticks** - Remember that time values are in ticks (0-24000) where 6000 is noon.

3. **Explosion power scaling** - Power 4.0 is equivalent to TNT. Use lower values for smaller explosions.

4. **Consider performance** - Spawning many particles or creating many explosions can impact performance.

5. **Check player count** - Use `getPlayerCount()` before performing expensive world operations.

---

## Complete Example

```java
import rubidium.api.world.WorldAPI;
import rubidium.api.world.WorldAPI.Weather;
import rubidium.api.world.WorldAPI.WorldState;

public class WorldManager {
    private final WorldAPI world = WorldAPI.get();
    
    public void createDramaticEntrance(double x, double y, double z) {
        String worldName = "world";
        
        // Set dramatic weather
        world.setWeather(worldName, Weather.THUNDER);
        world.setTime(worldName, 18000); // Midnight
        
        // Strike lightning at the entrance point
        world.strikeLightning(worldName, x, y, z, false);
        
        // Spawn particles
        world.spawnParticle(worldName, x, y + 1, z, "hytale:magic", 100);
        
        // Play dramatic sound
        world.playSound(worldName, x, y, z, "hytale:thunder", 1.0f, 0.8f);
    }
    
    public void buildPlatform(int centerX, int y, int centerZ, int radius, String blockType) {
        String worldName = "world";
        
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                world.setBlock(worldName, x, y, z, blockType);
            }
        }
    }
    
    public void dayNightCycle() {
        WorldState state = world.getWorld("world");
        long currentTime = state.getTime();
        
        // Advance time by 100 ticks
        state.setTime(currentTime + 100);
        
        // Check if it just became night
        if (state.isNight() && currentTime < 12000) {
            // Night just started - spawn events
            System.out.println("Night has fallen!");
        }
    }
    
    public void teleportAllPlayersToSpawn() {
        WorldState state = world.getWorld("world");
        List<UUID> players = world.getPlayersInWorld("world");
        
        int spawnX = state.getSpawnX();
        int spawnY = state.getSpawnY();
        int spawnZ = state.getSpawnZ();
        
        for (UUID playerId : players) {
            // Teleport player to spawn (using TeleportAPI)
            // TeleportAPI.get().teleport(playerId, "world", spawnX, spawnY, spawnZ);
        }
    }
}
```
