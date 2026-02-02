# EventAPI Reference

## Overview

The `EventAPI` provides a robust event system for Rubidium. It allows you to register event handlers, fire events, and create custom events. The system supports event priorities, cancellable events, and both lambda-based and annotation-based handler registration.

**Package:** `rubidium.api.event`

## Getting Started

```java
import rubidium.api.event.EventAPI;
import rubidium.api.event.EventAPI.*;

// Register a simple event handler
EventAPI.register(PlayerJoinEvent.class, event -> {
    System.out.println("Player joined: " + event.getJoinMessage());
});
```

## Static Methods

### Handler Registration (Lambda-Based)

#### `register(Class<T> eventClass, Consumer<T> handler)`
Registers an event handler with default priority.

```java
public static <T extends Event> void register(Class<T> eventClass, Consumer<T> handler)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `eventClass` | `Class<T>` | The event class to listen for |
| `handler` | `Consumer<T>` | The handler function |

**Example:**
```java
EventAPI.register(PlayerJoinEvent.class, event -> {
    event.setJoinMessage("§a" + event.getPlayer() + " has joined!");
});
```

#### `register(Class<T> eventClass, Consumer<T> handler, EventPriority priority)`
Registers an event handler with specified priority.

```java
public static <T extends Event> void register(Class<T> eventClass, Consumer<T> handler, EventPriority priority)
```

**Example:**
```java
EventAPI.register(PlayerChatEvent.class, event -> {
    // This runs first due to LOW priority
    event.setFormat("[%s] %s");
}, EventPriority.LOW);
```

#### `register(Class<T> eventClass, Consumer<T> handler, EventPriority priority, boolean ignoreCancelled)`
Registers a handler with full configuration.

```java
public static <T extends Event> void register(Class<T> eventClass, Consumer<T> handler, EventPriority priority, boolean ignoreCancelled)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `eventClass` | `Class<T>` | The event class to listen for |
| `handler` | `Consumer<T>` | The handler function |
| `priority` | `EventPriority` | When to run this handler |
| `ignoreCancelled` | `boolean` | If true, skip if event is cancelled |

**Example:**
```java
EventAPI.register(BlockBreakEvent.class, event -> {
    // Only runs if event wasn't cancelled
    logBlockBreak(event);
}, EventPriority.MONITOR, true);
```

---

### Handler Registration (Annotation-Based)

#### `registerListener(Object listener)`
Registers all `@EventListener` annotated methods in a class.

```java
public static void registerListener(Object listener)
```

**Example:**
```java
public class MyEventHandler {
    
    @EventListener
    public void onPlayerJoin(PlayerJoinEvent event) {
        System.out.println("Player joined!");
    }
    
    @EventListener(priority = EventPriority.HIGH)
    public void onChat(PlayerChatEvent event) {
        // Filter chat
    }
    
    @EventListener(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        // Log only uncancelled breaks
    }
}

// Register the listener
EventAPI.registerListener(new MyEventHandler());
```

---

### Handler Removal

#### `unregisterListener(Object listener)`
Removes all handlers registered by a listener object.

```java
public static void unregisterListener(Object listener)
```

**Example:**
```java
EventAPI.unregisterListener(myEventHandler);
```

#### `unregisterAll(Class<? extends Event> eventClass)`
Removes all handlers for a specific event type.

```java
public static void unregisterAll(Class<? extends Event> eventClass)
```

**Example:**
```java
EventAPI.unregisterAll(PlayerChatEvent.class);
```

---

### Event Firing

#### `fire(T event)`
Fires an event and runs all registered handlers.

```java
public static <T extends Event> T fire(T event)
```

**Returns:** `T` - The event after all handlers have processed it

**Example:**
```java
PlayerJoinEvent event = new PlayerJoinEvent(player, "Welcome!");
EventAPI.fire(event);
String finalMessage = event.getJoinMessage(); // May have been modified
```

#### `fireAndCheck(Cancellable event)`
Fires a cancellable event and returns whether it was cancelled.

```java
public static boolean fireAndCheck(Cancellable event)
```

**Returns:** `boolean` - `true` if the event was NOT cancelled

**Example:**
```java
BlockBreakEvent event = new BlockBreakEvent(player, block);
if (EventAPI.fireAndCheck(event)) {
    // Event was not cancelled, proceed with block break
    world.breakBlock(block);
}
```

---

## Enums

### EventPriority

Determines the order in which handlers are called.

```java
public enum EventPriority {
    LOWEST,   // First to run
    LOW,
    NORMAL,   // Default
    HIGH,
    HIGHEST,
    MONITOR   // Last to run (for logging, not modification)
}
```

**Priority Order:** LOWEST → LOW → NORMAL → HIGH → HIGHEST → MONITOR

---

## Annotations

### @EventListener

Marks a method as an event handler.

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventListener {
    EventPriority priority() default EventPriority.NORMAL;
    boolean ignoreCancelled() default false;
}
```

**Requirements:**
- Method must be `public`
- Method must have exactly one parameter
- Parameter must be a subclass of `Event`

---

## Base Classes

### Event

Base class for all events.

```java
public abstract static class Event {
    protected Event()
    protected Event(String name)
    
    public String getName()           // Event class name
    public long getTimestamp()        // When the event was created
    public boolean isAsync()          // Whether event is async
    protected void setAsync(boolean async)
}
```

### Cancellable

Interface for events that can be cancelled.

```java
public interface Cancellable {
    boolean isCancelled();
    void setCancelled(boolean cancelled);
}
```

### CancellableEvent

Base class for cancellable events.

```java
public abstract static class CancellableEvent extends Event implements Cancellable {
    @Override
    public boolean isCancelled()
    
    @Override
    public void setCancelled(boolean cancelled)
}
```

---

## Built-in Events

### PlayerJoinEvent

Fired when a player joins the server.

```java
public static class PlayerJoinEvent extends Event {
    public Object getPlayer()
    public String getJoinMessage()
    public void setJoinMessage(String msg)
}
```

**Example:**
```java
EventAPI.register(PlayerJoinEvent.class, event -> {
    event.setJoinMessage("§a+ " + event.getPlayer());
});
```

### PlayerQuitEvent

Fired when a player leaves the server.

```java
public static class PlayerQuitEvent extends Event {
    public Object getPlayer()
    public String getQuitMessage()
    public void setQuitMessage(String msg)
}
```

### PlayerChatEvent (Cancellable)

Fired when a player sends a chat message.

```java
public static class PlayerChatEvent extends CancellableEvent {
    public Object getPlayer()
    public String getMessage()
    public void setMessage(String msg)
    public String getFormat()
    public void setFormat(String format)
}
```

**Example:**
```java
EventAPI.register(PlayerChatEvent.class, event -> {
    // Censor bad words
    String message = event.getMessage();
    if (containsBadWord(message)) {
        event.setCancelled(true);
    }
});
```

### BlockBreakEvent (Cancellable)

Fired when a player breaks a block.

```java
public static class BlockBreakEvent extends CancellableEvent {
    public Object getPlayer()
    public Object getBlock()
    public boolean shouldDropItems()
    public void setDropItems(boolean drop)
}
```

**Example:**
```java
EventAPI.register(BlockBreakEvent.class, event -> {
    // Prevent breaking in spawn area
    if (isInSpawn(event.getBlock())) {
        event.setCancelled(true);
    }
});
```

### EntityDamageEvent (Cancellable)

Fired when an entity takes damage.

```java
public static class EntityDamageEvent extends CancellableEvent {
    public Object getEntity()
    public Object getDamager()
    public double getDamage()
    public void setDamage(double damage)
    public DamageCause getCause()
    
    public enum DamageCause {
        ATTACK, FALL, FIRE, LAVA, DROWN, 
        EXPLOSION, PROJECTILE, MAGIC, VOID, CUSTOM
    }
}
```

**Example:**
```java
EventAPI.register(EntityDamageEvent.class, event -> {
    // Reduce fall damage by 50%
    if (event.getCause() == DamageCause.FALL) {
        event.setDamage(event.getDamage() * 0.5);
    }
});
```

---

## Creating Custom Events

### Simple Event

```java
public class LevelUpEvent extends EventAPI.Event {
    private final UUID playerId;
    private final int oldLevel;
    private final int newLevel;
    
    public LevelUpEvent(UUID playerId, int oldLevel, int newLevel) {
        this.playerId = playerId;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }
    
    public UUID getPlayerId() { return playerId; }
    public int getOldLevel() { return oldLevel; }
    public int getNewLevel() { return newLevel; }
}

// Fire the event
LevelUpEvent event = new LevelUpEvent(playerId, 4, 5);
EventAPI.fire(event);
```

### Cancellable Event

```java
public class PlayerTeleportEvent extends EventAPI.CancellableEvent {
    private final UUID playerId;
    private final Location from;
    private Location to;
    
    public PlayerTeleportEvent(UUID playerId, Location from, Location to) {
        this.playerId = playerId;
        this.from = from;
        this.to = to;
    }
    
    public UUID getPlayerId() { return playerId; }
    public Location getFrom() { return from; }
    public Location getTo() { return to; }
    public void setTo(Location to) { this.to = to; }
}

// Fire and check
PlayerTeleportEvent event = new PlayerTeleportEvent(playerId, from, to);
if (EventAPI.fireAndCheck(event)) {
    // Teleport was allowed, use potentially modified destination
    teleportPlayer(playerId, event.getTo());
}
```

---

## Best Practices

1. **Use appropriate priority** - LOWEST/LOW for setup, NORMAL for logic, HIGH/HIGHEST for final decisions, MONITOR for logging only.

2. **Don't modify in MONITOR** - MONITOR priority is for observation, not modification.

3. **Check cancelled state** - Use `ignoreCancelled = true` if you only care about uncancelled events.

4. **Use fireAndCheck for cancellables** - It's cleaner than manually checking `isCancelled()`.

5. **Unregister on disable** - Clean up listeners when your plugin disables.

6. **Keep handlers fast** - Long-running handlers block other handlers.

---

## Complete Example

```java
import rubidium.api.event.EventAPI;
import rubidium.api.event.EventAPI.*;

public class MyPlugin {
    private MyEventHandler eventHandler;
    
    public void onEnable() {
        eventHandler = new MyEventHandler();
        EventAPI.registerListener(eventHandler);
        
        // Lambda-based registration
        EventAPI.register(PlayerJoinEvent.class, this::handleJoin, EventPriority.HIGH);
    }
    
    public void onDisable() {
        EventAPI.unregisterListener(eventHandler);
    }
    
    private void handleJoin(PlayerJoinEvent event) {
        event.setJoinMessage("§6★ §e" + event.getPlayer() + " §6has arrived!");
    }
}

class MyEventHandler {
    
    @EventListener(priority = EventPriority.LOW)
    public void formatChat(PlayerChatEvent event) {
        String rank = getRank(event.getPlayer());
        event.setFormat(rank + " %s: %s");
    }
    
    @EventListener(priority = EventPriority.HIGH)
    public void filterChat(PlayerChatEvent event) {
        if (containsBadWords(event.getMessage())) {
            event.setCancelled(true);
            sendMessage(event.getPlayer(), "§cWatch your language!");
        }
    }
    
    @EventListener(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void logChat(PlayerChatEvent event) {
        // Only log messages that weren't cancelled
        log("[CHAT] " + event.getPlayer() + ": " + event.getMessage());
    }
    
    @EventListener
    public void onBlockBreak(BlockBreakEvent event) {
        // Give double drops on weekends
        if (isWeekend()) {
            giveExtraDrops(event.getPlayer(), event.getBlock());
        }
    }
    
    @EventListener(priority = EventPriority.LOWEST)
    public void protectSpawn(BlockBreakEvent event) {
        if (isInSpawn(event.getBlock())) {
            event.setCancelled(true);
        }
    }
    
    @EventListener
    public void onDamage(EntityDamageEvent event) {
        // Implement damage reduction for armor
        if (event.getCause() == EntityDamageEvent.DamageCause.ATTACK) {
            double armor = getArmorValue(event.getEntity());
            double reduction = armor * 0.04; // 4% per armor point
            event.setDamage(event.getDamage() * (1 - reduction));
        }
    }
    
    private String getRank(Object player) { return "§7[Player]"; }
    private boolean containsBadWords(String msg) { return false; }
    private void sendMessage(Object player, String msg) {}
    private void log(String msg) { System.out.println(msg); }
    private boolean isWeekend() { return false; }
    private void giveExtraDrops(Object player, Object block) {}
    private boolean isInSpawn(Object block) { return false; }
    private double getArmorValue(Object entity) { return 0; }
}
```

### Custom Event System Example

```java
public class QuestSystem {
    
    // Custom events
    public static class QuestStartEvent extends EventAPI.CancellableEvent {
        private final UUID playerId;
        private final String questId;
        
        public QuestStartEvent(UUID playerId, String questId) {
            this.playerId = playerId;
            this.questId = questId;
        }
        
        public UUID getPlayerId() { return playerId; }
        public String getQuestId() { return questId; }
    }
    
    public static class QuestCompleteEvent extends EventAPI.Event {
        private final UUID playerId;
        private final String questId;
        private int bonusReward = 0;
        
        public QuestCompleteEvent(UUID playerId, String questId) {
            this.playerId = playerId;
            this.questId = questId;
        }
        
        public UUID getPlayerId() { return playerId; }
        public String getQuestId() { return questId; }
        public int getBonusReward() { return bonusReward; }
        public void addBonusReward(int amount) { this.bonusReward += amount; }
    }
    
    public boolean startQuest(UUID playerId, String questId) {
        QuestStartEvent event = new QuestStartEvent(playerId, questId);
        if (EventAPI.fireAndCheck(event)) {
            // Quest start was allowed
            doStartQuest(playerId, questId);
            return true;
        }
        return false;
    }
    
    public void completeQuest(UUID playerId, String questId) {
        QuestCompleteEvent event = new QuestCompleteEvent(playerId, questId);
        EventAPI.fire(event);
        
        // Apply rewards including any bonuses added by handlers
        int baseReward = getQuestReward(questId);
        int totalReward = baseReward + event.getBonusReward();
        giveReward(playerId, totalReward);
    }
    
    private void doStartQuest(UUID playerId, String questId) {}
    private int getQuestReward(String questId) { return 100; }
    private void giveReward(UUID playerId, int amount) {}
}
```
