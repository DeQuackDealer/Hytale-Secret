# NPCAPI Reference

## Overview

The `NPCAPI` provides a comprehensive system for creating, managing, and controlling Non-Player Characters (NPCs) in Rubidium. It includes NPC definitions, behaviors, pathfinding integration, dialog systems, and lifecycle events.

**Package:** `rubidium.api.npc`

## Getting Started

```java
import rubidium.api.npc.NPCAPI;
import rubidium.api.npc.NPCAPI.*;
import rubidium.api.pathfinding.PathfindingAPI.Vec3i;

// Create and spawn an NPC
NPCDefinition shopkeeper = NPCAPI.create("shopkeeper")
    .displayName("§6Shopkeeper Bob")
    .type(NPCDefinition.NPCType.MERCHANT)
    .interactable(true)
    .build();

NPC npc = NPCAPI.spawn(shopkeeper, new Vec3i(100, 64, 200));
```

## Static Methods

### NPC Definition

#### `create(String id)`
Creates a new NPC definition builder.

```java
public static NPCDefinition.Builder create(String id)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `id` | `String` | Unique identifier for this NPC type |

**Returns:** `NPCDefinition.Builder` - A builder for configuring the NPC

**Example:**
```java
NPCDefinition guard = NPCAPI.create("city_guard")
    .displayName("§cCity Guard")
    .type(NPCDefinition.NPCType.GUARD)
    .behavior("guard")
    .hostile(true)
    .health(50.0)
    .moveSpeed(0.3)
    .build();
```

#### `register(NPCDefinition definition)`
Registers an NPC definition for spawning.

```java
public static NPCDefinition register(NPCDefinition definition)
```

#### `register(NPCDefinition.Builder builder)`
Builds and registers an NPC definition.

```java
public static NPCDefinition register(NPCDefinition.Builder builder)
```

#### `getDefinition(String id)`
Gets a registered NPC definition.

```java
public static Optional<NPCDefinition> getDefinition(String id)
```

---

### Quick Creation Methods

#### `villager(String id, String name)`
Creates a basic villager NPC.

```java
public static NPCDefinition villager(String id, String name)
```

**Example:**
```java
NPCDefinition farmer = NPCAPI.villager("farmer_john", "§aFarmer John");
```

#### `guard(String id, String name)`
Creates a guard NPC.

```java
public static NPCDefinition guard(String id, String name)
```

**Example:**
```java
NPCDefinition knight = NPCAPI.guard("castle_knight", "§cSir Galahad");
```

#### `merchant(String id, String name)`
Creates a merchant NPC.

```java
public static NPCDefinition merchant(String id, String name)
```

#### `questGiver(String id, String name)`
Creates a quest giver NPC.

```java
public static NPCDefinition questGiver(String id, String name)
```

---

### NPC Spawning

#### `spawn(String definitionId, Vec3i location)`
Spawns an NPC from a registered definition.

```java
public static NPC spawn(String definitionId, Vec3i location)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `definitionId` | `String` | The registered definition ID |
| `location` | `Vec3i` | The spawn location |

**Returns:** `NPC` - The spawned NPC instance

**Throws:** `IllegalArgumentException` if definition not found

**Example:**
```java
NPCAPI.register(NPCAPI.villager("bob", "Bob"));
NPC bob = NPCAPI.spawn("bob", new Vec3i(100, 64, 200));
```

#### `spawn(NPCDefinition definition, Vec3i location)`
Registers and spawns an NPC in one step.

```java
public static NPC spawn(NPCDefinition definition, Vec3i location)
```

**Example:**
```java
NPCDefinition def = NPCAPI.merchant("trader", "§6Wandering Trader");
NPC trader = NPCAPI.spawn(def, new Vec3i(50, 64, 50));
```

---

### NPC Management

#### `despawn(UUID npcId)`
Removes an NPC from the world.

```java
public static void despawn(UUID npcId)
```

#### `despawn(NPC npc)`
Removes an NPC from the world.

```java
public static void despawn(NPC npc)
```

#### `get(UUID id)`
Gets an NPC by its unique ID.

```java
public static Optional<NPC> get(UUID id)
```

#### `all()`
Gets all active NPCs.

```java
public static Collection<NPC> all()
```

#### `nearby(Vec3i center, double radius)`
Gets all NPCs within a radius of a location.

```java
public static Collection<NPC> nearby(Vec3i center, double radius)
```

**Example:**
```java
Collection<NPC> nearbyNPCs = NPCAPI.nearby(playerLocation, 10.0);
for (NPC npc : nearbyNPCs) {
    npc.lookAt(playerLocation);
}
```

---

### Behavior Registration

#### `registerBehavior(String id, NPCBehavior behavior)`
Registers a custom behavior.

```java
public static void registerBehavior(String id, NPCBehavior behavior)
```

**Example:**
```java
NPCAPI.registerBehavior("dance", new DanceBehavior());
```

#### `getBehavior(String id)`
Gets a registered behavior.

```java
public static Optional<NPCBehavior> getBehavior(String id)
```

---

## NPCDefinition Record

Defines the properties of an NPC type.

```java
public record NPCDefinition(
    String id,
    String displayName,
    NPCType type,
    String model,
    String skin,
    String defaultBehavior,
    List<DialogNode> dialog,
    Map<String, Object> attributes,
    boolean interactable,
    boolean invulnerable,
    boolean showNameTag,
    boolean hostile,
    double moveSpeed,
    double health
)
```

### NPCDefinition.Builder

```java
public Builder displayName(String name)
public Builder type(NPCType type)
public Builder model(String model)
public Builder skin(String skin)
public Builder behavior(String behavior)
public Builder dialog(DialogNode... nodes)
public Builder attribute(String key, Object value)
public Builder interactable(boolean v)
public Builder invulnerable(boolean v)
public Builder showNameTag(boolean v)
public Builder hostile(boolean v)
public Builder moveSpeed(double speed)
public Builder health(double health)
public NPCDefinition build()
```

**Example:**
```java
NPCDefinition boss = NPCAPI.create("dragon_boss")
    .displayName("§4§lAncient Dragon")
    .type(NPCDefinition.NPCType.BOSS)
    .health(500.0)
    .moveSpeed(0.4)
    .hostile(true)
    .invulnerable(false)
    .showNameTag(true)
    .attribute("fire_damage", 20.0)
    .attribute("attack_range", 5.0)
    .build();
```

### NPCType Enum

```java
public enum NPCType {
    VILLAGER,    // Friendly villager
    MERCHANT,    // Shop/trade NPC
    GUARD,       // Protective NPC
    QUEST_GIVER, // Quest NPC
    COMPANION,   // Follows player
    ENEMY,       // Hostile mob
    BOSS,        // Boss enemy
    CUSTOM       // Custom type
}
```

---

## NPC Class

Represents a spawned NPC instance.

### Properties

```java
public UUID getId()
public NPCDefinition getDefinition()
public Vec3i getLocation()
public Vec3i getHomeLocation()
public double getYaw()
public double getPitch()
public double getHealth()
public String getCurrentBehavior()
public Object getTarget()
```

### Setters

```java
public void setLocation(Vec3i loc)
public void setHomeLocation(Vec3i loc)
public void setRotation(double yaw, double pitch)
public void setHealth(double health)
public void setTarget(Object target)
public void setBehavior(String behaviorId)
public void setPatrolPoints(List<Vec3i> points)
```

### Movement

#### `moveTo(Vec3i target, PathfindingContext context)`
Initiates pathfinding to a target location.

```java
public void moveTo(Vec3i target, PathfindingContext context)
```

#### `moveToAsync(Vec3i target, PathfindingContext context, Consumer<PathResult> callback)`
Asynchronously finds and follows a path.

```java
public void moveToAsync(Vec3i target, PathfindingContext context, Consumer<PathResult> callback)
```

**Example:**
```java
npc.moveToAsync(destination, context, result -> {
    if (result.success()) {
        System.out.println("Path found with " + result.path().size() + " nodes");
    }
});
```

### Actions

#### `tick()`
Processes one tick of NPC behavior and movement.

```java
public boolean tick()
```

**Returns:** `boolean` - `true` if the NPC moved this tick

#### `lookAt(Vec3i target)`
Rotates the NPC to face a location.

```java
public void lookAt(Vec3i target)
```

#### `damage(double amount)`
Damages the NPC (respects invulnerability).

```java
public void damage(double amount)
```

#### `isDead()`
Checks if the NPC's health is zero or below.

```java
public boolean isDead()
```

#### `speak(String message)` / `say(String message)`
Makes the NPC send a chat message.

```java
public void speak(String message)
public void say(String message)
```

### Data Storage

```java
public void setData(String key, Object value)
public <T> T getData(String key)
public <T> T getData(String key, T defaultValue)
```

**Example:**
```java
npc.setData("kills", 5);
int kills = npc.getData("kills", 0);
```

### Patrol

#### `getNextPatrolPoint()`
Gets the next patrol waypoint.

```java
public Vec3i getNextPatrolPoint()
```

---

## Dialog System

### DialogNode Record

```java
public record DialogNode(
    String id,
    String text,
    List<DialogOption> options
) {
    public static DialogNode simple(String id, String text)
    public static DialogNode withOptions(String id, String text, DialogOption... options)
}
```

### DialogOption Record

```java
public record DialogOption(
    String text,
    String nextNodeId,
    String action
) {
    public static DialogOption of(String text, String nextNodeId)
    public static DialogOption withAction(String text, String action)
}
```

**Example:**
```java
NPCDefinition questGiver = NPCAPI.create("old_wizard")
    .displayName("§5Wizard Merlin")
    .type(NPCDefinition.NPCType.QUEST_GIVER)
    .dialog(
        DialogNode.withOptions("start", "Greetings, adventurer! Do you seek a quest?",
            DialogOption.of("Yes, I seek adventure!", "quest_offer"),
            DialogOption.of("No, just passing through.", "goodbye")
        ),
        DialogNode.withOptions("quest_offer", "Find the Crystal of Power in the Dark Cave.",
            DialogOption.withAction("I accept this quest!", "start_crystal_quest"),
            DialogOption.of("That sounds too dangerous.", "goodbye")
        ),
        DialogNode.simple("goodbye", "Farewell, traveler. Safe journeys!")
    )
    .build();
```

---

## Built-in Behaviors

### IdleBehavior
NPC stands still and does nothing.

### WanderBehavior
NPC randomly wanders within a radius of its home location.

```java
new WanderBehavior(int radius)
```

### FollowBehavior
NPC follows its current target.

```java
new FollowBehavior(double maxDistance)
```

### GuardBehavior
NPC attacks hostile entities within range.

```java
new GuardBehavior(double aggroRange)
```

### PatrolBehavior
NPC moves between patrol points.

---

## Creating Custom Behaviors

```java
public class DanceBehavior implements NPCBehavior {
    private int tickCounter = 0;
    
    @Override
    public void tick(NPC npc) {
        tickCounter++;
        
        // Rotate every 10 ticks
        if (tickCounter % 10 == 0) {
            double currentYaw = npc.getYaw();
            npc.setRotation(currentYaw + 45, 0);
        }
    }
    
    @Override
    public void onStart(NPC npc) {
        npc.say("Time to dance!");
    }
    
    @Override
    public void onStop(NPC npc) {
        npc.say("Dance over!");
    }
}

// Register and use
NPCAPI.registerBehavior("dance", new DanceBehavior());

NPCDefinition dancer = NPCAPI.create("dancer")
    .displayName("§dDancing Dan")
    .behavior("dance")
    .build();
```

---

## Events

### NPCSpawnEvent

Fired when an NPC is spawned.

```java
public static class NPCSpawnEvent extends EventAPI.Event {
    public NPC getNpc()
}
```

### NPCDespawnEvent

Fired when an NPC is despawned.

```java
public static class NPCDespawnEvent extends EventAPI.Event {
    public NPC getNpc()
}
```

### NPCInteractEvent (Cancellable)

Fired when a player interacts with an NPC.

```java
public static class NPCInteractEvent extends EventAPI.CancellableEvent {
    public NPC getNpc()
    public Object getPlayer()
}
```

**Example:**
```java
EventAPI.register(NPCInteractEvent.class, event -> {
    NPC npc = event.getNpc();
    if (npc.getDefinition().type() == NPCDefinition.NPCType.MERCHANT) {
        openShopGUI(event.getPlayer(), npc);
    }
});
```

---

## Best Practices

1. **Register definitions once** - Register NPC definitions during initialization, then spawn instances as needed.

2. **Use appropriate types** - NPCType helps categorize and handle NPCs correctly.

3. **Clean up NPCs** - Despawn NPCs when they're no longer needed to save resources.

4. **Use data storage** - Store NPC-specific state using `setData()`/`getData()`.

5. **Tick efficiently** - Only call `tick()` on NPCs that need updates.

6. **Handle events** - Use NPCInteractEvent for player interactions.

---

## Complete Example

```java
import rubidium.api.npc.NPCAPI;
import rubidium.api.npc.NPCAPI.*;
import rubidium.api.event.EventAPI;
import rubidium.api.pathfinding.PathfindingAPI.Vec3i;

public class NPCManager {
    
    public void initialize() {
        // Register NPC definitions
        registerNPCs();
        
        // Register event handlers
        EventAPI.register(NPCInteractEvent.class, this::handleInteract);
        EventAPI.register(NPCSpawnEvent.class, event -> {
            System.out.println("NPC spawned: " + event.getNpc().getDefinition().displayName());
        });
        
        // Register custom behavior
        NPCAPI.registerBehavior("flee", new FleeBehavior());
    }
    
    private void registerNPCs() {
        // Shopkeeper
        NPCAPI.register(NPCAPI.create("general_store")
            .displayName("§6General Store")
            .type(NPCDefinition.NPCType.MERCHANT)
            .interactable(true)
            .invulnerable(true)
            .showNameTag(true)
        );
        
        // Quest giver with dialog
        NPCAPI.register(NPCAPI.create("quest_master")
            .displayName("§eQuest Master")
            .type(NPCDefinition.NPCType.QUEST_GIVER)
            .dialog(
                DialogNode.withOptions("start", "Welcome! Ready for adventure?",
                    DialogOption.of("Show me available quests", "quest_list"),
                    DialogOption.of("Maybe later", "goodbye")
                ),
                DialogNode.simple("quest_list", "Check your quest log for available missions!"),
                DialogNode.simple("goodbye", "Come back when you're ready!")
            )
        );
        
        // Patrolling guard
        NPCAPI.register(NPCAPI.create("patrol_guard")
            .displayName("§cTown Guard")
            .type(NPCDefinition.NPCType.GUARD)
            .behavior("patrol")
            .hostile(true)
            .health(40.0)
        );
    }
    
    public void spawnTownNPCs() {
        // Spawn shopkeeper
        NPC shop = NPCAPI.spawn("general_store", new Vec3i(100, 64, 100));
        shop.lookAt(new Vec3i(100, 64, 110)); // Face the street
        
        // Spawn quest master
        NPC quest = NPCAPI.spawn("quest_master", new Vec3i(120, 64, 100));
        
        // Spawn patrolling guard
        NPC guard = NPCAPI.spawn("patrol_guard", new Vec3i(90, 64, 90));
        guard.setPatrolPoints(List.of(
            new Vec3i(90, 64, 90),
            new Vec3i(110, 64, 90),
            new Vec3i(110, 64, 110),
            new Vec3i(90, 64, 110)
        ));
    }
    
    private void handleInteract(NPCInteractEvent event) {
        NPC npc = event.getNpc();
        NPCDefinition.NPCType type = npc.getDefinition().type();
        
        switch (type) {
            case MERCHANT -> openShop(event.getPlayer(), npc);
            case QUEST_GIVER -> openQuestDialog(event.getPlayer(), npc);
            default -> npc.say("Hello there!");
        }
    }
    
    public void updateNPCs() {
        for (NPC npc : NPCAPI.all()) {
            boolean moved = npc.tick();
            
            // Check if NPC died
            if (npc.isDead()) {
                NPCAPI.despawn(npc);
            }
        }
    }
    
    public void cleanupNearbyNPCs(Vec3i location, double radius) {
        for (NPC npc : NPCAPI.nearby(location, radius)) {
            NPCAPI.despawn(npc);
        }
    }
    
    private void openShop(Object player, NPC npc) {
        // Open shop GUI
    }
    
    private void openQuestDialog(Object player, NPC npc) {
        // Start dialog system
    }
}

class FleeBehavior implements NPCBehavior {
    private final double fleeDistance = 20.0;
    
    @Override
    public void tick(NPC npc) {
        Object target = npc.getTarget();
        if (target != null) {
            // Run away from target
            Vec3i myLoc = npc.getLocation();
            Vec3i homeLoc = npc.getHomeLocation();
            
            // Move toward home when fleeing
            npc.moveTo(homeLoc, null);
        }
    }
    
    @Override
    public void onStart(NPC npc) {
        npc.say("I'm getting out of here!");
    }
}
```
