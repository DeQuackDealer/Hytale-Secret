# AI Behavior API

## Overview

The AI Behavior API provides a flexible behavior tree and goal-based AI system for creating intelligent NPCs, mobs, and other entities. It supports standard behavior tree patterns including sequences, selectors, parallels, decorators, and built-in action nodes for common AI tasks.

## Package

```java
import rubidium.api.ai.AIBehaviorAPI;
import static rubidium.api.ai.AIBehaviorAPI.*;
```

---

## Core Concepts

### Behavior Trees

Behavior trees execute child nodes based on control flow nodes (sequence, selector, parallel) and return one of three statuses:

| Status | Description |
|--------|-------------|
| `SUCCESS` | Node completed successfully |
| `FAILURE` | Node failed to complete |
| `RUNNING` | Node is still executing, will continue next tick |

### Goal Selectors

Goal selectors manage prioritized goals that can start, continue, and stop based on conditions.

---

## Static Factory Methods

### Tree Management

| Method | Returns | Description |
|--------|---------|-------------|
| `createTree(String id)` | `BehaviorTree.Builder` | Creates a behavior tree builder |
| `registerTree(BehaviorTree tree)` | `BehaviorTree` | Registers a tree for reuse |
| `getTree(String id)` | `Optional<BehaviorTree>` | Retrieves a registered tree |
| `createGoalSelector(String id)` | `GoalSelector` | Creates and registers a goal selector |
| `getGoalSelector(String id)` | `Optional<GoalSelector>` | Retrieves a goal selector |

### Control Flow Nodes

| Method | Returns | Description |
|--------|---------|-------------|
| `sequence(BehaviorNode... children)` | `BehaviorNode` | Runs children in order, fails if any fails |
| `selector(BehaviorNode... children)` | `BehaviorNode` | Runs children until one succeeds |
| `parallel(BehaviorNode... children)` | `BehaviorNode` | Runs all children simultaneously |

### Decorator Nodes

| Method | Returns | Description |
|--------|---------|-------------|
| `condition(Predicate<AIContext> condition, BehaviorNode child)` | `BehaviorNode` | Runs child only if condition passes |
| `inverter(BehaviorNode child)` | `BehaviorNode` | Inverts child's success/failure |
| `repeater(BehaviorNode child, int times)` | `BehaviorNode` | Repeats child N times |

### Action Nodes

| Method | Returns | Description |
|--------|---------|-------------|
| `action(Consumer<AIContext> action)` | `BehaviorNode` | Executes custom action |
| `wait(int ticks)` | `BehaviorNode` | Waits for N ticks |
| `moveTo(Vec3i target)` | `BehaviorNode` | Moves entity to position |
| `moveToTarget()` | `BehaviorNode` | Moves entity to current target |
| `lookAt(Vec3i target)` | `BehaviorNode` | Looks at position |
| `lookAtTarget()` | `BehaviorNode` | Looks at current target |
| `attack()` | `BehaviorNode` | Attacks current target |
| `flee(double distance)` | `BehaviorNode` | Flees from target |
| `wander(int radius)` | `BehaviorNode` | Wanders randomly within radius |
| `patrol(List<Vec3i> points)` | `BehaviorNode` | Patrols between waypoints |

---

## BehaviorTree

### Builder Methods

```java
BehaviorTree tree = AIBehaviorAPI.createTree("guard-ai")
    .root(behaviorNode)
    .build();
```

### Instance Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `getId()` | `String` | Returns the tree identifier |
| `tick(AIContext context)` | `NodeStatus` | Executes one tick of the tree |
| `reset()` | `void` | Resets all nodes to initial state |

### Code Example

```java
// Create a guard AI behavior tree
BehaviorTree guardAI = AIBehaviorAPI.createTree("guard-patrol")
    .root(
        selector(
            // Priority 1: Attack if enemy nearby
            sequence(
                condition(ctx -> ctx.target() != null, 
                    sequence(
                        lookAtTarget(),
                        moveToTarget(),
                        attack()
                    )
                )
            ),
            // Priority 2: Patrol waypoints
            patrol(List.of(
                new Vec3i(0, 64, 0),
                new Vec3i(10, 64, 0),
                new Vec3i(10, 64, 10),
                new Vec3i(0, 64, 10)
            ))
        )
    )
    .build();

AIBehaviorAPI.registerTree(guardAI);
```

---

## AIContext

Context object passed to behavior nodes containing entity state and blackboard storage.

### Record Fields

| Field | Type | Description |
|-------|------|-------------|
| `entity` | `Object` | The entity being controlled |
| `position` | `Vec3i` | Current entity position |
| `target` | `Object` | Current target (nullable) |
| `blackboard` | `Map<String, Object>` | Shared data storage |
| `pathContext` | `PathfindingContext` | Pathfinding configuration |

### Factory Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `AIContext.create(Object entity, Vec3i position)` | `AIContext` | Creates a basic context |
| `withTarget(Object target)` | `AIContext` | Returns context with target set |
| `withPathContext(PathfindingContext ctx)` | `AIContext` | Returns context with pathfinding |

### Blackboard Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `get(String key)` | `<T>` | Gets a blackboard value |
| `get(String key, T defaultValue)` | `<T>` | Gets with default |
| `set(String key, Object value)` | `void` | Sets a blackboard value |

### Code Example

```java
// Create context for an NPC
AIContext context = AIContext.create(npc, npc.getBlockPosition())
    .withTarget(nearestPlayer)
    .withPathContext(pathfindingConfig);

// Use blackboard for shared state
context.set("home_position", spawnPoint);
context.set("aggro_range", 16.0);

// Access in behavior nodes
action(ctx -> {
    Vec3i home = ctx.get("home_position");
    double range = ctx.get("aggro_range", 10.0);
})
```

---

## GoalSelector

Priority-based goal system for complex AI behaviors.

### Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `getId()` | `String` | Returns the selector identifier |
| `addGoal(int priority, Goal goal)` | `void` | Adds a goal with priority (lower = higher priority) |
| `removeGoal(Goal goal)` | `void` | Removes a goal |
| `tick(AIContext context)` | `void` | Processes goals for one tick |
| `getActiveGoal()` | `Optional<Goal>` | Returns the currently active goal |

### Goal Interface

```java
public interface Goal {
    boolean canStart(AIContext context);           // Can this goal start?
    default boolean canContinue(AIContext context) // Can this goal continue?
        { return canStart(context); }
    default void start() {}                        // Called when goal starts
    void tick(AIContext context);                  // Called each tick while active
    default void stop() {}                         // Called when goal stops
}
```

### Code Example

```java
GoalSelector selector = AIBehaviorAPI.createGoalSelector("zombie-goals");

// Priority 0 (highest): Panic when on fire
selector.addGoal(0, new Goal() {
    @Override
    public boolean canStart(AIContext ctx) {
        return ((Entity) ctx.entity()).isOnFire();
    }
    
    @Override
    public void tick(AIContext ctx) {
        // Run to water
        findNearestWater(ctx);
    }
});

// Priority 1: Attack players
selector.addGoal(1, new Goal() {
    @Override
    public boolean canStart(AIContext ctx) {
        return ctx.target() != null;
    }
    
    @Override
    public void start() {
        playAggroSound();
    }
    
    @Override
    public void tick(AIContext ctx) {
        moveTowardTarget(ctx);
        if (inRange(ctx)) {
            attackTarget(ctx);
        }
    }
    
    @Override
    public void stop() {
        playIdleSound();
    }
});

// Priority 2 (lowest): Wander randomly
selector.addGoal(2, new WanderGoal(10));

// Tick the selector each game tick
selector.tick(context);
```

---

## Control Flow Patterns

### Sequence (AND logic)

Runs children in order. Fails immediately if any child fails.

```java
sequence(
    moveTo(targetPos),      // First move
    wait(20),               // Then wait 1 second
    action(ctx -> attack()) // Then attack
)
```

### Selector (OR logic)

Tries children in order until one succeeds.

```java
selector(
    condition(ctx -> hasRangedWeapon(ctx), rangedAttack()),
    condition(ctx -> hasMeleeWeapon(ctx), meleeAttack()),
    flee(10.0)  // Fallback if no weapons
)
```

### Parallel (Concurrent execution)

Runs all children simultaneously. Fails if any child fails.

```java
parallel(
    lookAtTarget(),    // Keep looking at target
    moveToTarget(),    // While moving toward it
    action(ctx -> playWalkAnimation())
)
```

---

## Decorator Patterns

### Conditional Execution

```java
condition(
    ctx -> ctx.get("health") < 50,
    flee(20.0)  // Only flee if low health
)
```

### Inversion

```java
inverter(
    condition(ctx -> isEnemyNear(ctx), wait(1))
)
// Returns SUCCESS if enemy is NOT near
```

### Repetition

```java
repeater(
    sequence(
        wander(5),
        wait(40)
    ),
    3  // Wander 3 times
)
```

---

## Built-in Action Nodes

### Movement

```java
// Move to specific position
moveTo(new Vec3i(100, 64, 200))

// Move to current target
moveToTarget()

// Patrol waypoints
patrol(List.of(pos1, pos2, pos3, pos4))

// Random wandering
wander(10)  // 10 block radius

// Flee from target
flee(15.0)  // 15 block distance
```

### Looking

```java
// Look at position
lookAt(new Vec3i(100, 70, 200))

// Look at target
lookAtTarget()
```

### Combat

```java
// Attack current target
attack()
```

### Utility

```java
// Wait for ticks
wait(20)  // 1 second at 20 TPS

// Custom action
action(ctx -> {
    Entity entity = (Entity) ctx.entity();
    entity.playSound(Sound.ENTITY_ZOMBIE_AMBIENT);
})
```

---

## Performance Considerations

- **Tick Rate**: Behavior trees should be ticked once per game tick (20 TPS)
- **Pathfinding**: MoveTo nodes use async pathfinding to avoid blocking
- **Goal Selection**: Goals are sorted by priority on add; O(n) selection per tick
- **Blackboard**: Use for caching expensive calculations between ticks
- **Node Reuse**: Register and reuse trees rather than creating new instances

### Optimization Tips

```java
// Cache expensive checks in blackboard
action(ctx -> {
    if (!ctx.blackboard().containsKey("path_cached")) {
        ctx.set("path_cached", calculatePath(ctx));
    }
})

// Use conditions to skip expensive subtrees
selector(
    condition(ctx -> ctx.target() == null, idle()),  // Fast check first
    condition(ctx -> isTargetValid(ctx), attack())   // Expensive check second
)
```

---

## Complete Example

```java
// Create a shopkeeper NPC AI
BehaviorTree shopkeeperAI = AIBehaviorAPI.createTree("shopkeeper")
    .root(
        selector(
            // If player is interacting, open shop
            condition(ctx -> ctx.get("interacting_player") != null,
                sequence(
                    lookAtTarget(),
                    action(ctx -> openShopUI(ctx)),
                    wait(100)  // Keep shop open
                )
            ),
            // If player nearby, wave
            condition(ctx -> isPlayerNearby(ctx, 8),
                sequence(
                    lookAtTarget(),
                    action(ctx -> playWaveAnimation(ctx)),
                    wait(60)
                )
            ),
            // Otherwise, idle animations
            sequence(
                repeater(
                    sequence(
                        action(ctx -> playIdleAnimation(ctx)),
                        wait(40)
                    ),
                    5
                ),
                wander(3)
            )
        )
    )
    .build();

// Use in entity tick
public void tick() {
    AIContext context = AIContext.create(this, this.getBlockPosition())
        .withTarget(getNearestPlayer(16.0));
    
    shopkeeperAI.tick(context);
}
```
