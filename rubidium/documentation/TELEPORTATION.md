# Teleportation System

> **Document Purpose**: Complete reference for Rubidium's teleportation, homes, and warps system.

## Overview

Rubidium's teleportation system provides:
- **Player Teleportation**: Request-based player-to-player teleport
- **Homes**: Personal saved locations
- **Warps**: Server-wide named locations
- **Spawn**: Server spawn management
- **Back**: Return to previous location
- **Integration**: Works with permissions, cooldowns, and costs

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    TeleportManager                               │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Core Systems                                              │   │
│  │ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐     │   │
│  │ │ Request  │ │ Home     │ │ Warp     │ │ Spawn    │     │   │
│  │ │ Manager  │ │ Manager  │ │ Manager  │ │ Manager  │     │   │
│  │ └──────────┘ └──────────┘ └──────────┘ └──────────┘     │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Features                                                  │   │
│  │ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐     │   │
│  │ │ Cooldown │ │ Warmup   │ │ Cost     │ │ History  │     │   │
│  │ │ Manager  │ │ Manager  │ │ Manager  │ │ (Back)   │     │   │
│  │ └──────────┘ └──────────┘ └──────────┘ └──────────┘     │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

## Core Classes

### TeleportRequest

```java
package com.yellowtale.rubidium.teleport;

public class TeleportRequest {
    private UUID id;
    private UUID requester;
    private UUID target;
    private TeleportType type;
    private TeleportStatus status;
    private long createdAt;
    private long expiresAt;
    private Location destination;
    private String reason;
    
    public enum TeleportType {
        TPA,            // Teleport to player
        TPAHERE,        // Teleport player to you
        HOME,           // Teleport to home
        WARP,           // Teleport to warp
        SPAWN,          // Teleport to spawn
        BACK,           // Teleport to previous location
        PARTY,          // Party teleport
        ADMIN           // Admin teleport (no restrictions)
    }
    
    public enum TeleportStatus {
        PENDING,        // Waiting for acceptance
        ACCEPTED,       // Accepted, in warmup
        WARMING_UP,     // Warmup countdown
        EXECUTING,      // Teleporting
        COMPLETED,      // Successfully teleported
        CANCELLED,      // Cancelled by user
        DENIED,         // Denied by target
        EXPIRED,        // Request expired
        FAILED          // Teleport failed
    }
}
```

### Home

```java
public class Home {
    private UUID id;
    private UUID owner;
    private String name;
    private Location location;
    private long createdAt;
    private long lastUsed;
    private int useCount;
    private boolean isDefault;
    private HomeIcon icon;
    private Map<String, String> metadata;
    
    public enum HomeIcon {
        DEFAULT, BED, HOUSE, CASTLE, CAVE, MINE, FARM, SHOP, SECRET
    }
}
```

### Warp

```java
public class Warp {
    private String id;
    private String name;
    private String description;
    private Location location;
    private UUID creator;
    private long createdAt;
    private WarpCategory category;
    private WarpPermission permission;
    private boolean enabled;
    private boolean hidden;
    private int useCount;
    private WarpCost cost;
    
    public enum WarpCategory {
        GENERAL, SPAWN, HUB, DUNGEON, ARENA, SHOP, ADMIN
    }
    
    public record WarpPermission(
        String permission,
        List<String> allowedRoles,
        boolean requireAll
    ) {}
    
    public record WarpCost(
        String currency,
        long amount,
        boolean refundOnCancel
    ) {}
}
```

### TeleportManager

```java
public class TeleportManager {
    
    // Configuration
    private TeleportConfig config;
    
    // Sub-managers
    private RequestManager requests;
    private HomeManager homes;
    private WarpManager warps;
    private SpawnManager spawns;
    private LocationHistory history;
    
    // Request Management
    public TeleportRequest requestTeleport(UUID requester, UUID target, TeleportType type);
    public void acceptRequest(UUID requestId);
    public void denyRequest(UUID requestId);
    public void cancelRequest(UUID requestId);
    public List<TeleportRequest> getPendingRequests(UUID playerId);
    
    // Direct Teleport
    public TeleportResult teleport(UUID playerId, Location destination, TeleportType type);
    public TeleportResult teleportToPlayer(UUID playerId, UUID targetId);
    public TeleportResult teleportToHome(UUID playerId, String homeName);
    public TeleportResult teleportToWarp(UUID playerId, String warpName);
    public TeleportResult teleportToSpawn(UUID playerId);
    public TeleportResult teleportBack(UUID playerId);
    
    // Home Management
    public Home setHome(UUID playerId, String name, Location location);
    public void deleteHome(UUID playerId, String name);
    public List<Home> getHomes(UUID playerId);
    public Optional<Home> getHome(UUID playerId, String name);
    public Home getDefaultHome(UUID playerId);
    
    // Warp Management
    public Warp createWarp(String name, Location location, UUID creator);
    public void deleteWarp(String name);
    public List<Warp> getWarps();
    public List<Warp> getAccessibleWarps(UUID playerId);
    public Optional<Warp> getWarp(String name);
    
    // Spawn Management
    public void setSpawn(String name, Location location);
    public Location getSpawn(String name);
    public Location getDefaultSpawn();
}
```

## Teleport Configuration

```java
public record TeleportConfig(
    // Requests
    Duration requestTimeout,
    boolean allowTpaToggle,
    
    // Warmup
    boolean enableWarmup,
    Duration warmupDuration,
    boolean cancelOnMove,
    boolean cancelOnDamage,
    double moveThreshold,
    
    // Cooldown
    boolean enableCooldown,
    Duration cooldownDuration,
    boolean cooldownPerType,
    
    // Costs
    boolean enableCosts,
    Map<TeleportType, TeleportCost> costs,
    
    // Homes
    int defaultMaxHomes,
    Map<String, Integer> homesByPermission,
    boolean allowHomeInNether,
    boolean allowHomeInEnd,
    
    // Safety
    boolean safeLocationCheck,
    int maxSearchRadius,
    boolean preventTeleportToUnsafe
) {
    public static TeleportConfig defaults() {
        return new TeleportConfig(
            Duration.ofSeconds(30),
            true,
            true,
            Duration.ofSeconds(3),
            true,
            true,
            0.5,
            true,
            Duration.ofSeconds(10),
            true,
            true,
            Map.of(
                TeleportType.HOME, new TeleportCost("gold", 0),
                TeleportType.WARP, new TeleportCost("gold", 50),
                TeleportType.TPA, new TeleportCost("gold", 10)
            ),
            3,
            Map.of("vip", 5, "premium", 10, "admin", 100),
            true,
            true,
            true,
            10,
            true
        );
    }
}
```

## Teleport Process

### Warmup System

```java
public class WarmupManager {
    private Map<UUID, WarmupState> activeWarmups = new ConcurrentHashMap<>();
    
    public CompletableFuture<TeleportResult> startWarmup(
        UUID playerId, 
        Location destination,
        TeleportType type
    ) {
        TeleportConfig config = teleportManager.getConfig();
        if (!config.enableWarmup() || type == TeleportType.ADMIN) {
            return CompletableFuture.completedFuture(
                executeInstantTeleport(playerId, destination, type)
            );
        }
        
        WarmupState state = new WarmupState(
            playerId,
            destination,
            type,
            getPlayerLocation(playerId),
            System.currentTimeMillis(),
            config.warmupDuration().toMillis()
        );
        
        activeWarmups.put(playerId, state);
        
        return state.getFuture();
    }
    
    public void tick() {
        long now = System.currentTimeMillis();
        
        for (var entry : activeWarmups.entrySet()) {
            UUID playerId = entry.getKey();
            WarmupState state = entry.getValue();
            
            // Check cancellation conditions
            if (shouldCancel(playerId, state)) {
                state.cancel("Teleport cancelled");
                activeWarmups.remove(playerId);
                continue;
            }
            
            // Check completion
            if (now >= state.getStartTime() + state.getDurationMs()) {
                TeleportResult result = executeTeleport(playerId, state.getDestination(), state.getType());
                state.complete(result);
                activeWarmups.remove(playerId);
            }
        }
    }
    
    private boolean shouldCancel(UUID playerId, WarmupState state) {
        TeleportConfig config = teleportManager.getConfig();
        Location current = getPlayerLocation(playerId);
        
        // Movement check
        if (config.cancelOnMove()) {
            double distance = current.distance(state.getStartLocation());
            if (distance > config.moveThreshold()) {
                return true;
            }
        }
        
        // Damage check
        if (config.cancelOnDamage() && wasRecentlyDamaged(playerId)) {
            return true;
        }
        
        return false;
    }
}
```

### Safe Location Finding

```java
public class SafeLocationFinder {
    
    public Optional<Location> findSafeLocation(Location target, int maxRadius) {
        // Check target first
        if (isSafe(target)) {
            return Optional.of(target);
        }
        
        // Spiral search outward
        for (int radius = 1; radius <= maxRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                    
                    // Check column
                    for (int dy = -radius; dy <= radius; dy++) {
                        Location check = target.add(dx, dy, dz);
                        if (isSafe(check)) {
                            return Optional.of(check.center());
                        }
                    }
                }
            }
        }
        
        return Optional.empty();
    }
    
    private boolean isSafe(Location loc) {
        Block feet = getBlock(loc);
        Block head = getBlock(loc.add(0, 1, 0));
        Block ground = getBlock(loc.add(0, -1, 0));
        
        return !feet.isSolid() 
            && !head.isSolid() 
            && ground.isSolid()
            && !isHazardous(feet)
            && !isHazardous(head)
            && !isHazardous(ground);
    }
    
    private boolean isHazardous(Block block) {
        return block.isLava() 
            || block.isFire() 
            || block.isCactus()
            || block.isMagmaBlock();
    }
}
```

## Commands

### TPA Commands

```
/tpa <player>                   - Request teleport to player
/tpahere <player>               - Request player teleport to you
/tpaccept [player]              - Accept teleport request
/tpdeny [player]                - Deny teleport request
/tpacancel                      - Cancel your pending request
/tpatoggle                      - Toggle receiving TPA requests
```

### Home Commands

```
/home [name]                    - Teleport to home (default if no name)
/sethome [name]                 - Set home at current location
/delhome <name>                 - Delete a home
/homes                          - List your homes
/home rename <old> <new>        - Rename a home
/home icon <name> <icon>        - Set home icon
/home default <name>            - Set default home
```

### Warp Commands

```
/warp <name>                    - Teleport to warp
/warps                          - List available warps
/warp info <name>               - Show warp information
```

### Warp Admin Commands

```
/setwarp <name>                 - Create warp at location
/delwarp <name>                 - Delete a warp
/warp enable <name>             - Enable a warp
/warp disable <name>            - Disable a warp
/warp permission <name> <perm>  - Set warp permission
/warp cost <name> <amount>      - Set warp cost
/warp category <name> <cat>     - Set warp category
```

### Spawn Commands

```
/spawn [name]                   - Teleport to spawn
/setspawn [name]                - Set spawn location
/spawns                         - List spawn points
```

### Utility Commands

```
/back                           - Return to previous location
/top                            - Teleport to highest block
/tp <player>                    - Teleport to player (admin)
/tphere <player>                - Teleport player to you (admin)
/tppos <x> <y> <z> [world]      - Teleport to coordinates
/tpoffline <player>             - Set offline player's spawn
```

## Events

```java
public class TeleportRequestEvent extends Event implements Cancellable {
    private UUID requester;
    private UUID target;
    private TeleportType type;
    private boolean cancelled;
    private String cancelReason;
}

public class TeleportAcceptEvent extends Event implements Cancellable {
    private TeleportRequest request;
    private boolean cancelled;
}

public class PreTeleportEvent extends Event implements Cancellable {
    private UUID playerId;
    private Location from;
    private Location to;
    private TeleportType type;
    private boolean cancelled;
    private String cancelReason;
}

public class PostTeleportEvent extends Event {
    private UUID playerId;
    private Location from;
    private Location to;
    private TeleportType type;
    private Duration warmupTime;
    private long cost;
}

public class HomeSetEvent extends Event implements Cancellable {
    private UUID playerId;
    private String homeName;
    private Location location;
    private boolean isNew;
    private boolean cancelled;
}

public class WarpCreatedEvent extends Event {
    private Warp warp;
    private UUID creator;
}
```

## Integration Examples

### Combat Tag Integration

```java
combatManager.onCombatTag((player, attacker) -> {
    // Cancel any pending teleports
    teleportManager.cancelAllRequests(player.getId());
    teleportManager.cancelWarmup(player.getId());
});

teleportManager.onPreTeleport((event) -> {
    if (combatManager.isInCombat(event.getPlayerId())) {
        event.setCancelled(true);
        event.setCancelReason("Cannot teleport while in combat");
    }
});
```

### Economy Integration

```java
teleportManager.onPreTeleport((event) -> {
    TeleportCost cost = teleportManager.getCost(event.getType());
    if (cost != null && cost.amount() > 0) {
        if (!economy.hasBalance(event.getPlayerId(), cost.currency(), cost.amount())) {
            event.setCancelled(true);
            event.setCancelReason("Insufficient funds. Cost: " + cost.format());
        }
    }
});

teleportManager.onPostTeleport((event) -> {
    TeleportCost cost = teleportManager.getCost(event.getType());
    if (cost != null && cost.amount() > 0) {
        economy.withdraw(
            event.getPlayerId(),
            cost.currency(),
            cost.amount(),
            "Teleport: " + event.getType()
        );
    }
});
```

### Death Back Location

```java
eventBus.subscribe(PlayerDeathEvent.class, (event) -> {
    teleportManager.saveBackLocation(
        event.getPlayer().getId(),
        event.getLocation(),
        "death"
    );
});
```

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `teleport.tpa` | Use /tpa | All |
| `teleport.tpahere` | Use /tpahere | All |
| `teleport.home` | Use /home | All |
| `teleport.home.multiple` | Have multiple homes | All |
| `teleport.home.set` | Set homes | All |
| `teleport.warp` | Use /warp | All |
| `teleport.warp.create` | Create warps | Staff |
| `teleport.spawn` | Use /spawn | All |
| `teleport.back` | Use /back | All |
| `teleport.tp` | Use /tp (admin) | Staff |
| `teleport.bypass.cooldown` | Bypass cooldowns | VIP |
| `teleport.bypass.warmup` | Bypass warmup | VIP |
| `teleport.bypass.cost` | Bypass costs | VIP |
| `teleport.bypass.combat` | Teleport in combat | Staff |

## Configuration

```yaml
# teleport.yml
requests:
  timeout: 30s
  allow_toggle: true

warmup:
  enabled: true
  duration: 3s
  cancel_on_move: true
  cancel_on_damage: true
  move_threshold: 0.5

cooldown:
  enabled: true
  duration: 10s
  per_type: true
  
  type_cooldowns:
    TPA: 15s
    HOME: 5s
    WARP: 10s
    SPAWN: 30s

costs:
  enabled: true
  currency: "gold"
  
  type_costs:
    TPA: 10
    TPAHERE: 10
    HOME: 0
    WARP: 50
    SPAWN: 0
    BACK: 25

homes:
  default_max: 3
  
  max_by_permission:
    "homes.vip": 5
    "homes.premium": 10
    "homes.unlimited": 1000
  
  allow_nether: true
  allow_end: true

warps:
  default_category: "general"
  hide_restricted: true

safety:
  check_destination: true
  max_search_radius: 10
  prevent_unsafe: true
```
