# Advanced Optimizer API

## Overview

The Advanced Optimizer API provides high-performance memory management, task scheduling, batch processing, and caching utilities designed to minimize garbage collection pressure and maximize server tick performance.

## Package

```java
import rubidium.optimization.AdvancedOptimizer;
```

## Getting Started

```java
AdvancedOptimizer optimizer = AdvancedOptimizer.get();
optimizer.initialize();
```

---

## AdvancedOptimizer

Main optimization controller that manages all optimization subsystems.

### Static Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `get()` | `AdvancedOptimizer` | Returns the singleton instance |

### Instance Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `initialize()` | `void` | Initializes all optimization subsystems |
| `shutdown()` | `void` | Shuts down optimizer and logs statistics |
| `getMemoryPool()` | `MemoryPoolManager` | Returns the memory pool manager |
| `getTickScheduler()` | `TickScheduler` | Returns the tick scheduler |
| `getBatchProcessor()` | `BatchProcessor` | Returns the batch processor |
| `getObjectPooling()` | `ObjectPooling` | Returns the object pooling manager |
| `getLazyLoader()` | `LazyLoadManager` | Returns the lazy load manager |
| `getCacheManager()` | `CacheManager` | Returns the cache manager |
| `getOptimizationStats()` | `Map<String, Object>` | Returns optimization statistics |

### Code Example

```java
AdvancedOptimizer optimizer = AdvancedOptimizer.get();
optimizer.initialize();

// Access subsystems
MemoryPoolManager memPool = optimizer.getMemoryPool();
TickScheduler scheduler = optimizer.getTickScheduler();
CacheManager cache = optimizer.getCacheManager();

// Shutdown on server stop
optimizer.shutdown();
```

---

## MemoryPoolManager

Object pooling system to reduce garbage collection by reusing frequently allocated objects.

### Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `initialize()` | `void` | Initializes the memory pool |
| `cleanup()` | `void` | Clears all pools |
| `registerPool(Class<T> type, Supplier<T> factory)` | `void` | Registers a new object pool with custom factory |
| `acquire(Class<T> type)` | `T` | Acquires an object from the pool or creates new |
| `release(T object)` | `void` | Returns an object to its pool |
| `getReuseRatio()` | `double` | Returns ratio of reuses to total acquisitions |
| `getStats()` | `String` | Returns pool statistics string |

### Code Example

```java
MemoryPoolManager pool = optimizer.getMemoryPool();

// Register a custom pool
pool.registerPool(Vector3.class, Vector3::new);

// Acquire from pool instead of new
Vector3 vec = pool.acquire(Vector3.class);
vec.set(x, y, z);

// Use the object...

// Return to pool when done
pool.release(vec);

// Check efficiency
System.out.println("Reuse ratio: " + pool.getReuseRatio());
```

---

## TickScheduler

Tick-based task scheduling system integrated with the server tick loop.

### Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `start()` | `void` | Starts the scheduler (50ms tick rate) |
| `stop()` | `void` | Stops the scheduler |
| `scheduleOnce(String name, Runnable action, long delayTicks)` | `void` | Schedules a one-time task |
| `scheduleRepeating(String name, Runnable action, long intervalTicks)` | `void` | Schedules a repeating task |
| `cancel(String name)` | `void` | Cancels a scheduled task by name |
| `getCurrentTick()` | `long` | Returns the current tick count |

### Code Example

```java
TickScheduler scheduler = optimizer.getTickScheduler();

// Schedule a one-time task in 20 ticks (1 second)
scheduler.scheduleOnce("save-data", () -> {
    savePlayerData();
}, 20);

// Schedule repeating task every 100 ticks (5 seconds)
scheduler.scheduleRepeating("cleanup", () -> {
    cleanupExpiredEntities();
}, 100);

// Cancel a task
scheduler.cancel("cleanup");
```

---

## BatchProcessor

Batches multiple operations together for efficient processing.

### Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `start()` | `void` | Starts the batch processor (100ms flush interval) |
| `stop()` | `void` | Stops the processor |
| `registerQueue(String name, int batchSize, Consumer<List<T>> processor)` | `void` | Creates a new batch queue |
| `submit(String queueName, T item)` | `void` | Submits an item to a queue |

### Code Example

```java
BatchProcessor batch = optimizer.getBatchProcessor();

// Register a batch queue for database writes
batch.registerQueue("db-writes", 50, items -> {
    database.batchInsert(items);
});

// Submit items - they'll be processed in batches of 50
for (PlayerData data : playerDataList) {
    batch.submit("db-writes", data);
}
```

---

## ObjectPooling

Named object pools with custom reset functions.

### Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `createPool(String name, Supplier<T> factory, Consumer<T> reset, int initialSize)` | `void` | Creates a named pool |
| `borrow(String poolName)` | `T` | Borrows an object from a pool |
| `returnObject(String poolName, T object)` | `void` | Returns an object to its pool |

### Code Example

```java
ObjectPooling pools = optimizer.getObjectPooling();

// Create a pool with reset function
pools.createPool("particles", 
    () -> new ParticleEffect(),
    particle -> particle.reset(),
    100  // pre-allocate 100 objects
);

// Borrow and use
ParticleEffect effect = pools.borrow("particles");
effect.setType(ParticleType.EXPLOSION);
effect.play(location);

// Return when done
pools.returnObject("particles", effect);
```

---

## LazyLoadManager

Deferred initialization for expensive resources.

### Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `register(String key, Supplier<T> loader)` | `void` | Registers a lazy loader |
| `get(String key)` | `T` | Gets or loads the resource |
| `unload(String key)` | `void` | Unloads a resource |
| `isLoaded(String key)` | `boolean` | Checks if a resource is loaded |

### Code Example

```java
LazyLoadManager lazy = optimizer.getLazyLoader();

// Register expensive resources
lazy.register("world-border-mesh", () -> generateComplexMesh());
lazy.register("npc-dialogue-tree", () -> loadDialogueFromDatabase());

// Resources load only when first accessed
Mesh mesh = lazy.get("world-border-mesh");

// Check if loaded
if (lazy.isLoaded("npc-dialogue-tree")) {
    // Resource is ready
}

// Unload when no longer needed
lazy.unload("world-border-mesh");
```

---

## CacheManager

TTL-based caching with automatic eviction.

### Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `createCache(String name, long ttlMs, int maxSize)` | `void` | Creates a named cache |
| `put(String cacheName, K key, V value)` | `void` | Stores a value in cache |
| `get(String cacheName, K key)` | `V` | Retrieves a cached value (null if expired/missing) |
| `getOrCompute(String cacheName, K key, Function<K, V> computer)` | `V` | Gets cached value or computes and caches it |
| `invalidate(String cacheName, Object key)` | `void` | Removes a specific entry |
| `clear(String cacheName)` | `void` | Clears all entries in a cache |

### Code Example

```java
CacheManager cache = optimizer.getCacheManager();

// Create a cache with 5-minute TTL and max 1000 entries
cache.createCache("player-profiles", 300_000, 1000);

// Store a value
cache.put("player-profiles", playerId, profile);

// Retrieve (returns null if expired)
PlayerProfile cached = cache.get("player-profiles", playerId);

// Get or compute pattern
PlayerProfile profile = cache.getOrCompute("player-profiles", playerId, 
    id -> database.loadProfile(id)
);

// Invalidate on update
cache.invalidate("player-profiles", playerId);

// Clear entire cache
cache.clear("player-profiles");
```

---

## Performance Considerations

### Memory Pooling

- **Default max pool size**: 1000 objects per type
- **Best for**: Frequently created short-lived objects (vectors, events, packets)
- **Avoid for**: Objects with complex state that's expensive to reset

### Tick Scheduler

- **Tick rate**: 50ms (20 TPS)
- **Best for**: Game logic that should sync with server ticks
- **Thread safety**: Tasks run on scheduler threads, synchronize shared state

### Batch Processing

- **Flush interval**: 100ms
- **Best for**: Database writes, network broadcasts, logging
- **Latency**: Items may wait up to batch size + flush interval before processing

### Caching

- **Eviction strategy**: TTL-based with LRU fallback when max size reached
- **Thread safety**: All operations are thread-safe (ConcurrentHashMap-based)
- **Memory**: Monitor cache sizes to avoid memory pressure

---

## Statistics

```java
Map<String, Object> stats = optimizer.getOptimizationStats();
// Returns:
// - memoryPoolStats: "Allocations: X, Reuses: Y, Ratio: Z%"
// - memoryReuseRatio: Percentage as string
// - currentTick: Current scheduler tick
// - optimizationsSaved: Total optimized operations count
// - memoryReclaimedMB: Memory reclaimed in megabytes
// - enabled: Whether optimizer is active
```

---

## Best Practices

1. **Pool frequently allocated objects** - Vectors, events, packets benefit most
2. **Use batch processing for I/O** - Database and network operations
3. **Cache expensive computations** - Path calculations, permission checks
4. **Lazy load large resources** - Load on demand, unload when not needed
5. **Monitor statistics** - Track reuse ratios and adjust pool sizes

```java
// Example: Full optimization setup for a minigame
AdvancedOptimizer opt = AdvancedOptimizer.get();
opt.initialize();

// Pool game objects
opt.getMemoryPool().registerPool(Arrow.class, Arrow::new);

// Cache team lookups
opt.getCacheManager().createCache("team-cache", 60_000, 500);

// Batch score updates
opt.getBatchProcessor().registerQueue("scores", 20, 
    scores -> leaderboard.batchUpdate(scores));

// Schedule periodic cleanup
opt.getTickScheduler().scheduleRepeating("arena-cleanup", 
    () -> arena.cleanupItems(), 200);
```
