# Waypoint System

> **Document Purpose**: Complete reference for Rubidium's waypoint and navigation system.

## Overview

Rubidium's waypoint system provides:
- **Personal Waypoints**: Private markers saved per-player
- **Shared Waypoints**: Visible to party, team, or server
- **Categories**: Organize waypoints with colors and icons
- **Navigation**: Distance, direction, and compass integration
- **Map Integration**: Export to minimap mods

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     WaypointManager                              │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Core Components                                           │   │
│  │ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐     │   │
│  │ │ Waypoint │ │ Category │ │ Sharing  │ │ Navigation│     │   │
│  │ │ Storage  │ │ Manager  │ │ Manager  │ │ Engine   │     │   │
│  │ └──────────┘ └──────────┘ └──────────┘ └──────────┘     │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Renderers                                                 │   │
│  │ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐     │   │
│  │ │ HUD      │ │ Compass  │ │ Beam     │ │ Minimap  │     │   │
│  │ │ Renderer │ │ Renderer │ │ Renderer │ │ Export   │     │   │
│  │ └──────────┘ └──────────┘ └──────────┘ └──────────┘     │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

## Core Classes

### Waypoint

```java
package com.yellowtale.rubidium.waypoints;

public class Waypoint {
    // Identity
    private UUID id;
    private String name;
    private String description;
    
    // Location
    private String world;
    private double x;
    private double y;
    private double z;
    
    // Ownership
    private UUID owner;
    private WaypointVisibility visibility;
    private Set<UUID> sharedWith;
    
    // Appearance
    private WaypointCategory category;
    private int color;
    private WaypointIcon icon;
    private boolean showBeam;
    private boolean showDistance;
    
    // Metadata
    private long createdAt;
    private long lastVisited;
    private int visitCount;
    private Map<String, String> tags;
    
    public enum WaypointVisibility {
        PRIVATE,        // Only owner can see
        PARTY,          // Party members can see
        TEAM,           // Team members can see
        FACTION,        // Faction members can see
        PUBLIC,         // Everyone can see
        SERVER          // Server-managed waypoint
    }
}
```

### WaypointCategory

```java
public class WaypointCategory {
    private String id;
    private String name;
    private int color;
    private WaypointIcon defaultIcon;
    private boolean system;
    
    // Built-in categories
    public static final WaypointCategory HOME = 
        new WaypointCategory("home", "Home", 0x4CAF50, WaypointIcon.HOUSE, true);
    public static final WaypointCategory DEATH = 
        new WaypointCategory("death", "Death", 0xF44336, WaypointIcon.SKULL, true);
    public static final WaypointCategory SPAWN = 
        new WaypointCategory("spawn", "Spawn", 0x2196F3, WaypointIcon.STAR, true);
    public static final WaypointCategory POI = 
        new WaypointCategory("poi", "Point of Interest", 0xFFEB3B, WaypointIcon.FLAG, true);
    public static final WaypointCategory DUNGEON = 
        new WaypointCategory("dungeon", "Dungeon", 0x9C27B0, WaypointIcon.CAVE, true);
    public static final WaypointCategory RESOURCE = 
        new WaypointCategory("resource", "Resource", 0xFF9800, WaypointIcon.PICKAXE, true);
    public static final WaypointCategory CUSTOM = 
        new WaypointCategory("custom", "Custom", 0xFFFFFF, WaypointIcon.MARKER, false);
}
```

### WaypointIcon

```java
public enum WaypointIcon {
    // Buildings
    HOUSE, CASTLE, TOWER, TENT, VILLAGE,
    
    // Nature
    TREE, MOUNTAIN, CAVE, WATER, PORTAL,
    
    // Combat
    SKULL, SWORD, SHIELD, CROSSHAIR,
    
    // Resources
    PICKAXE, AXE, CHEST, DIAMOND, GOLD,
    
    // Navigation
    MARKER, FLAG, STAR, ARROW, COMPASS,
    
    // Social
    HEART, PLAYER, GROUP, SHOP, QUEST,
    
    // Status
    WARNING, INFO, QUESTION, CHECK, CROSS,
    
    // Custom (numeric IDs for user-uploaded)
    CUSTOM_0, CUSTOM_1, CUSTOM_2, CUSTOM_3, CUSTOM_4
}
```

### WaypointManager

```java
public class WaypointManager {
    
    // Storage
    private WaypointStorage storage;
    private Map<UUID, List<Waypoint>> playerWaypoints;
    private List<Waypoint> serverWaypoints;
    
    // Configuration
    private WaypointConfig config;
    
    // Limits
    public static final int MAX_WAYPOINTS_PER_PLAYER = 100;
    public static final int MAX_SHARED_WAYPOINTS = 50;
    public static final int MAX_NAME_LENGTH = 32;
    public static final int MAX_DESCRIPTION_LENGTH = 256;
    
    // CRUD Operations
    public Waypoint createWaypoint(UUID owner, String name, Location location);
    public void updateWaypoint(UUID waypointId, WaypointUpdate update);
    public void deleteWaypoint(UUID waypointId);
    public Optional<Waypoint> getWaypoint(UUID waypointId);
    
    // Query Operations
    public List<Waypoint> getPlayerWaypoints(UUID playerId);
    public List<Waypoint> getVisibleWaypoints(UUID playerId);
    public List<Waypoint> getNearbyWaypoints(Location center, double radius);
    public List<Waypoint> searchWaypoints(UUID playerId, String query);
    
    // Sharing
    public void shareWaypoint(UUID waypointId, UUID targetPlayer);
    public void unshareWaypoint(UUID waypointId, UUID targetPlayer);
    public void setVisibility(UUID waypointId, WaypointVisibility visibility);
    
    // Navigation
    public NavigationData getNavigation(UUID playerId, UUID waypointId);
    public void setActiveWaypoint(UUID playerId, UUID waypointId);
    public void clearActiveWaypoint(UUID playerId);
}
```

## Navigation System

### NavigationData

```java
public record NavigationData(
    Waypoint waypoint,
    double distance,
    double horizontalDistance,
    double verticalDistance,
    double bearing,
    Direction compassDirection,
    String formattedDistance,
    String eta,
    boolean inRange,
    boolean sameWorld
) {
    public enum Direction {
        N, NE, E, SE, S, SW, W, NW
    }
    
    public static NavigationData calculate(Location from, Waypoint to) {
        if (!from.getWorld().equals(to.getWorld())) {
            return new NavigationData(to, -1, -1, -1, 0, null, "Different World", null, false, false);
        }
        
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double bearing = Math.toDegrees(Math.atan2(-dx, dz));
        if (bearing < 0) bearing += 360;
        
        Direction compass = getCompassDirection(bearing);
        String formatted = formatDistance(distance);
        
        return new NavigationData(
            to, distance, horizontalDist, dy, bearing, compass,
            formatted, null, distance <= 1000, true
        );
    }
}
```

### Compass Integration

```java
public class CompassRenderer {
    private static final int COMPASS_WIDTH = 180;
    
    public void renderCompass(Player player, List<Waypoint> visible) {
        double playerYaw = player.getLocation().getYaw();
        
        for (Waypoint wp : visible) {
            NavigationData nav = NavigationData.calculate(player.getLocation(), wp);
            if (!nav.sameWorld()) continue;
            
            double relativeAngle = normalizeAngle(nav.bearing() - playerYaw);
            
            if (Math.abs(relativeAngle) <= COMPASS_WIDTH / 2) {
                int xOffset = (int) (relativeAngle * (screenWidth / COMPASS_WIDTH));
                renderWaypointMarker(wp, xOffset, nav.distance());
            }
        }
    }
}
```

## Storage Format

### Player Waypoint File

```yaml
# /data/players/{uuid}/waypoints.yml
version: 1
categories:
  - id: "my-spots"
    name: "My Spots"
    color: "#FF6B6B"
    icon: "STAR"
    
waypoints:
  - id: "550e8400-e29b-41d4-a716-446655440000"
    name: "My Base"
    description: "Main base with storage"
    world: "world"
    x: 100.5
    y: 64.0
    z: -200.5
    category: "home"
    color: "#4CAF50"
    icon: "HOUSE"
    visibility: "PARTY"
    sharedWith:
      - "other-player-uuid"
    showBeam: true
    showDistance: true
    createdAt: 1704067200000
    lastVisited: 1704153600000
    visitCount: 42
    tags:
      type: "survival"
      biome: "plains"
```

### Server Waypoint File

```yaml
# /data/waypoints/server-waypoints.yml
version: 1
waypoints:
  - id: "spawn-main"
    name: "Spawn"
    description: "Server spawn point"
    world: "world"
    x: 0.0
    y: 100.0
    z: 0.0
    category: "spawn"
    visibility: "SERVER"
    permanent: true
    
  - id: "shop-district"
    name: "Shop District"
    description: "Player shops and trading"
    world: "world"
    x: 500.0
    y: 64.0
    z: 500.0
    category: "poi"
    visibility: "SERVER"
```

## Commands

### Basic Commands

```
/waypoint create <name>         - Create waypoint at current location
/waypoint delete <name>         - Delete a waypoint
/waypoint list [category]       - List your waypoints
/waypoint info <name>           - Show waypoint details
/waypoint edit <name> <field> <value> - Edit waypoint properties
/waypoint goto <name>           - Set as active navigation target
/waypoint stop                  - Clear active navigation
```

### Shorthand Alias

```
/wp add <name>                  - Create waypoint
/wp del <name>                  - Delete waypoint
/wp ls                          - List waypoints
/wp <name>                      - Navigate to waypoint
```

### Category Commands

```
/waypoint category create <name> <color> - Create custom category
/waypoint category delete <name>         - Delete custom category
/waypoint category list                  - List all categories
/waypoint category set <waypoint> <cat>  - Set waypoint category
```

### Sharing Commands

```
/waypoint share <name> <player> - Share waypoint with player
/waypoint unshare <name> <player> - Remove share
/waypoint visibility <name> <level> - Set visibility level
/waypoint shared                - List shared waypoints
```

### Admin Commands

```
/waypoint admin create <name> <x> <y> <z> [world] - Create server waypoint
/waypoint admin delete <name>   - Delete server waypoint
/waypoint admin list            - List all server waypoints
/waypoint admin player <player> - View player's waypoints
/waypoint admin clear <player>  - Clear player's waypoints
/waypoint admin import <file>   - Import waypoints from file
/waypoint admin export <file>   - Export waypoints to file
```

## Integration Examples

### Death Waypoint

```java
eventBus.subscribe(PlayerDeathEvent.class, (event) -> {
    Player player = event.getPlayer();
    Location deathLoc = event.getLocation();
    
    // Remove old death waypoint
    waypointManager.getPlayerWaypoints(player.getId())
        .stream()
        .filter(wp -> wp.getCategory().equals(WaypointCategory.DEATH))
        .forEach(wp -> waypointManager.deleteWaypoint(wp.getId()));
    
    // Create new death waypoint
    Waypoint deathWp = waypointManager.createWaypoint(
        player.getId(),
        "Death Location",
        deathLoc
    );
    deathWp.setCategory(WaypointCategory.DEATH);
    deathWp.setShowBeam(true);
    
    player.sendMessage("Death location saved as waypoint");
});
```

### Party Waypoints

```java
partyManager.onWaypointPing((party, player, location, message) -> {
    Waypoint pingWp = Waypoint.builder()
        .name(player.getName() + "'s Ping")
        .location(location)
        .owner(player.getId())
        .visibility(WaypointVisibility.PARTY)
        .category(WaypointCategory.POI)
        .icon(WaypointIcon.CROSSHAIR)
        .temporary(Duration.ofMinutes(5))
        .build();
    
    waypointManager.createWaypoint(pingWp);
    
    for (UUID member : party.getMembers()) {
        if (!member.equals(player.getId())) {
            Player memberPlayer = playerManager.getPlayer(member);
            memberPlayer.sendMessage(player.getName() + " pinged: " + message);
        }
    }
});
```

### Quest Integration

```java
questManager.onObjectiveActivated((player, objective) -> {
    if (objective.hasLocation()) {
        Waypoint questWp = Waypoint.builder()
            .name(objective.getName())
            .description(objective.getDescription())
            .location(objective.getLocation())
            .owner(player.getId())
            .category(new WaypointCategory("quest", "Quest", 0xE91E63, WaypointIcon.QUEST))
            .visibility(WaypointVisibility.PRIVATE)
            .showBeam(true)
            .build();
        
        waypointManager.createWaypoint(questWp);
        waypointManager.setActiveWaypoint(player.getId(), questWp.getId());
    }
});

questManager.onObjectiveCompleted((player, objective) -> {
    waypointManager.deleteWaypointByTag(player.getId(), "quest", objective.getId());
});
```

## Rendering Options

### HUD Display Modes

| Mode | Description |
|------|-------------|
| `OFF` | No waypoint display |
| `MINIMAL` | Distance only |
| `COMPACT` | Icon + distance |
| `FULL` | Icon + name + distance |
| `DETAILED` | Icon + name + distance + category |

### Beam Settings

```java
public record BeamSettings(
    boolean enabled,
    double maxDistance,
    int color,
    float alpha,
    float width,
    boolean throughBlocks,
    boolean animate
) {
    public static BeamSettings defaults() {
        return new BeamSettings(
            true, 256.0, 0xFFFFFF, 0.5f, 1.0f, true, true
        );
    }
}
```

## Performance

### Optimization Strategies

1. **Spatial Indexing**: R-tree for nearby waypoint queries
2. **Distance Culling**: Hide waypoints beyond render distance
3. **Update Throttling**: Navigation updates every 5 ticks
4. **Lazy Loading**: Load waypoints on demand per chunk region

### Memory Usage

| Component | Per Waypoint |
|-----------|--------------|
| Core data | ~200 bytes |
| Metadata | ~100 bytes |
| Shared list | ~16 bytes per share |
| Tags | ~50 bytes per tag |

### Limits

| Limit | Default | Configurable |
|-------|---------|--------------|
| Max waypoints per player | 100 | Yes |
| Max shared waypoints | 50 | Yes |
| Max server waypoints | 1000 | Yes |
| Max categories per player | 20 | Yes |
| Minimum distance between | 1 block | Yes |
