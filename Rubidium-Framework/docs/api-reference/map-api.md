# Map API

The Map API provides BetterMap-style world exploration features including persistent exploration tracking, waypoints, player radar, and location overlays.

## Overview

The Map API enhances the default Hytale map experience by:
- **Persistent Exploration:** Remembers areas you've visited across sessions
- **Waypoint System:** Create, share, and teleport to saved locations
- **Compass Radar:** See nearby players on your compass
- **Location Overlay:** Display coordinates and direction on screen
- **Shared Mapping:** Optionally share exploration with other players
- **Customizable Zoom:** Set your own min/max zoom levels

## Getting Started

```java
import rubidium.api.map.MapAPI;

MapAPI map = MapAPI.get();
```

## Core Features

### Recording Exploration

As players move, the map tracks explored areas:

```java
// Update player position (automatically records exploration)
map.updatePlayerPosition(playerId, "world", x, y, z, yaw);

// Manually record exploration
map.recordExploration(playerId, "world", chunkX, chunkZ);

// Check if an area is explored
boolean explored = map.isExplored(playerId, "world", chunkX, chunkZ);
```

### Waypoints

Create and manage location markers:

```java
// Create a personal waypoint
MapAPI.Waypoint waypoint = map.createWaypoint(
    playerId, 
    "My Base",      // Name
    "world",        // World
    100.0, 64.0, -50.0  // Coordinates
);

// Customize waypoint
waypoint.setColor(0xFF0000);  // Red
waypoint.setName("Home Base");

// Create a global waypoint (visible to all)
MapAPI.Waypoint global = map.createGlobalWaypoint(
    "Spawn Point",
    "world",
    0.0, 64.0, 0.0,
    0x00FF00  // Green
);

// Get player's waypoints
List<MapAPI.Waypoint> myWaypoints = map.getPlayerWaypoints(playerId);

// Get all visible waypoints (personal + global)
List<MapAPI.Waypoint> visible = map.getAllVisibleWaypoints(playerId, "world");

// Delete a waypoint
map.deleteWaypoint(playerId, waypoint.getId());

// Share waypoint with another player
map.shareWaypoint(fromPlayerId, toPlayerId, waypoint.getId());
```

### Waypoint Properties

```java
MapAPI.Waypoint wp = map.createWaypoint(player, "Test", "world", 0, 64, 0);

// Getters
UUID id = wp.getId();
UUID owner = wp.getOwner();
String name = wp.getName();
String world = wp.getWorldName();
double x = wp.getX();
double y = wp.getY();
double z = wp.getZ();
int color = wp.getColor();
boolean isGlobal = wp.isGlobal();
UUID sharedFrom = wp.getSharedFrom();

// Calculate distance
double distance = wp.distanceTo(playerX, playerY, playerZ);

// Copy waypoint
MapAPI.Waypoint copy = wp.copy();
```

### Player Radar

Locate nearby players:

```java
// Get nearby players within range
List<MapAPI.PlayerRadarEntry> nearby = map.getRadarEntries(
    playerId, 
    "world", 
    playerX, playerZ, 
    100.0  // Range
);

for (MapAPI.PlayerRadarEntry entry : nearby) {
    UUID otherId = entry.getPlayerId();
    double distance = entry.getDistance();
    double angle = entry.getAngle();          // Radians
    double angleDeg = entry.getAngleDegrees(); // Degrees
}
```

### Location Overlay

Display player coordinates:

```java
MapAPI.LocationOverlay loc = map.getLocationOverlay(playerId);

String world = loc.getWorld();
double x = loc.getX();
double y = loc.getY();
double z = loc.getZ();
float yaw = loc.getYaw();

// Formatted display
String coords = loc.getFormattedCoords();  // "X: 100.0 Y: 64.0 Z: -50.0"
String direction = loc.getDirection();      // "North", "South", "East", "West"
```

### Zoom Settings

Allow players to customize their map zoom:

```java
// Set player's zoom limits
map.setPlayerZoom(playerId, 10.0f, 256.0f);

// Get current settings
MapAPI.PlayerMapData playerMap = map.getPlayerMap(playerId);
float minZoom = playerMap.getMinZoom();
float maxZoom = playerMap.getMaxZoom();
```

## Configuration

### MapConfig

```java
MapAPI.MapConfig config = map.getConfig();

// Exploration settings
config.setExplorationRadius(16);        // Chunks around player to mark explored
config.setUpdateRateMs(500);            // Update frequency

// Map quality (affects performance)
config.setMapQuality(MapAPI.MapQuality.MEDIUM);
// LOW:    30,000 chunks, 8x8 resolution
// MEDIUM: 10,000 chunks, 16x16 resolution
// HIGH:   3,000 chunks, 32x32 resolution

// Shared exploration mode
config.setSharedExploration(true);      // All players share one map

// Chunk loading limits
config.setMaxChunksToLoad(10000);

// Radar settings
config.setRadarEnabled(true);
config.setRadarRange(-1);               // -1 = infinite

// Auto-save interval
config.setAutoSaveInterval(5);          // Minutes

// World whitelist
config.addAllowedWorld("custom_world");
config.removeAllowedWorld("world");
boolean allowed = config.isWorldAllowed("world");
```

## Map Quality Settings

| Quality | Max Chunks | Resolution | Use Case |
|---------|------------|------------|----------|
| LOW | 30,000 | 8x8 | Large servers, limited RAM |
| MEDIUM | 10,000 | 16x16 | Balanced (default) |
| HIGH | 3,000 | 32x32 | Small servers, high detail |

## Player Map Data

Access per-player map information:

```java
MapAPI.PlayerMapData playerData = map.getPlayerMap(playerId);

// Exploration stats
int exploredChunks = playerData.getExploredChunkCount("world");

// Current position
String currentWorld = playerData.getCurrentWorld();
double lastX = playerData.getLastX();
double lastY = playerData.getLastY();
double lastZ = playerData.getLastZ();
float lastYaw = playerData.getLastYaw();

// Zoom settings
float minZoom = playerData.getMinZoom();
float maxZoom = playerData.getMaxZoom();

// Waypoints
List<MapAPI.Waypoint> waypoints = playerData.getWaypoints();
MapAPI.Waypoint specific = playerData.getWaypoint(waypointId);
```

## World Map Data (Shared Mode)

When shared exploration is enabled:

```java
MapAPI.WorldMapData worldData = map.getWorldMap("world");

// Check exploration
boolean explored = worldData.isExplored(chunkX, chunkZ);

// Find who explored a chunk
UUID explorer = worldData.getExplorer(chunkX, chunkZ);

// Total explored chunks
int total = worldData.getTotalExploredChunks();
```

## Complete Example

```java
import rubidium.RubidiumPlugin;
import rubidium.api.map.MapAPI;
import rubidium.api.command.CommandAPI;
import rubidium.api.event.EventAPI;
import rubidium.api.event.PlayerMoveEvent;

public class MapPlugin extends RubidiumPlugin {
    
    private MapAPI map;
    
    @Override
    protected void setup() {
        map = MapAPI.get();
        
        // Configure map settings
        MapAPI.MapConfig config = map.getConfig();
        config.setExplorationRadius(8);
        config.setRadarEnabled(true);
        config.setSharedExploration(false);
        
        registerCommands();
    }
    
    @Override
    protected void start() {
        // Track player movement
        EventAPI.get().on(PlayerMoveEvent.class, event -> {
            map.updatePlayerPosition(
                event.getPlayer().getUniqueId(),
                event.getWorld(),
                event.getX(), event.getY(), event.getZ(),
                event.getYaw()
            );
        });
    }
    
    private void registerCommands() {
        // Waypoint command
        CommandAPI.create("waypoint")
            .aliases("wp")
            .handler(ctx -> {
                UUID player = ctx.getPlayer().getUniqueId();
                String action = ctx.getArgOrDefault(0, "list");
                
                switch (action) {
                    case "create":
                        String name = ctx.getArg(1);
                        double x = ctx.getPlayer().getX();
                        double y = ctx.getPlayer().getY();
                        double z = ctx.getPlayer().getZ();
                        map.createWaypoint(player, name, 
                            ctx.getPlayer().getWorld(), x, y, z);
                        ctx.reply("Waypoint '" + name + "' created!");
                        break;
                        
                    case "list":
                        for (MapAPI.Waypoint wp : map.getPlayerWaypoints(player)) {
                            ctx.reply("- " + wp.getName() + " (" + 
                                (int)wp.getX() + ", " + (int)wp.getZ() + ")");
                        }
                        break;
                }
            })
            .register();
        
        // Location command
        CommandAPI.create("location")
            .aliases("loc", "coords")
            .handler(ctx -> {
                UUID player = ctx.getPlayer().getUniqueId();
                MapAPI.LocationOverlay loc = map.getLocationOverlay(player);
                ctx.reply(loc.getFormattedCoords() + " - Facing " + loc.getDirection());
            })
            .register();
        
        // Radar command
        CommandAPI.create("radar")
            .handler(ctx -> {
                UUID player = ctx.getPlayer().getUniqueId();
                MapAPI.LocationOverlay loc = map.getLocationOverlay(player);
                
                var nearby = map.getRadarEntries(player, 
                    loc.getWorld(), loc.getX(), loc.getZ(), 100);
                
                if (nearby.isEmpty()) {
                    ctx.reply("No players nearby");
                } else {
                    for (var entry : nearby) {
                        ctx.reply("Player " + entry.getPlayerId() + 
                            " - " + (int)entry.getDistance() + " blocks away");
                    }
                }
            })
            .register();
    }
    
    @Override
    protected void shutdown() {
        map.saveAllData();
    }
}
```

## Credits

Map API features inspired by [BetterMap](https://www.curseforge.com/hytale/mods/bettermap) by Paralaxe and Theobosse (Ninesliced team).
