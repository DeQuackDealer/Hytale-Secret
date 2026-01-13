# Performance Optimization System

> **AI-Readable Documentation** - Structured reference for the Rubidium performance optimization system.

## Overview

The Performance system provides intelligent resource management to maintain server stability under varying load conditions. Inspired by performance saver plugins, it dynamically adjusts server parameters to prevent crashes while maintaining playability.

## Core Components

### PerformanceManager

**Package**: `com.yellowtale.rubidium.performance`

Central coordinator for all performance optimization features.

```java
public class PerformanceManager {
    // State
    private AtomicInteger currentTps;
    private AtomicInteger currentViewRadius;
    private volatile boolean cpuPressure;
    private volatile boolean memoryPressure;
    private volatile ServerState serverState;
    
    // Lifecycle
    void start()
    void stop()
    
    // Tick Recording
    void recordTick(long tickTimeNanos)
    
    // Updates
    void updatePlayerCount(int count)
    void updateChunkCount(long count)
    
    // Queries
    int getCurrentTps()
    int getTargetTps()
    int getCurrentViewRadius()
    boolean isCpuPressure()
    boolean isMemoryPressure()
    ServerState getServerState()
    PerformanceMetrics getMetrics()
    
    // Listeners
    void onViewRadiusChange(Consumer<Integer> listener)
    void onTpsChange(Consumer<Integer> listener)
    void onMetricsUpdate(Consumer<PerformanceMetrics> listener)
}
```

### Server States

| State | Description | TPS Target | View Radius |
|-------|-------------|------------|-------------|
| `NORMAL` | Healthy operation | 20 | Default (10) |
| `DEGRADED` | Resource pressure detected | 20 | Reduced dynamically |
| `EMPTY` | No players online | 5 | Minimum |

## Features

### 1. TPS Limiting

Maintains stable, predictable tick rate instead of fluctuating performance.

**Configuration**:
```java
PerformanceConfig.builder()
    .targetTps(20)           // Normal operation TPS
    .emptyServerTps(5)       // TPS when no players
    .enableTpsLimiting(true)
    .build();
```

**Behavior**:
- Lower but stable TPS provides better player experience than high but fluctuating TPS
- Network prediction works better with consistent timing
- Empty servers automatically reduce TPS to conserve resources

**TpsLimiter Class**:
```java
TpsLimiter limiter = new TpsLimiter(20);

// In tick loop
limiter.preTickWait();   // Sleep to maintain target TPS
doTick();
limiter.postTick();      // Record tick completion

// Adjust dynamically
limiter.setTargetTps(15); // Reduce when needed
```

### 2. Dynamic View Radius

Automatically adjusts player view distance based on resource pressure.

**Configuration**:
```java
PerformanceConfig.builder()
    .defaultViewRadius(10)      // Normal radius
    .minimumViewRadius(4)       // Emergency minimum
    .maximumViewRadius(16)      // Upper limit
    .viewRadiusStep(1)          // Adjustment step size
    .enableDynamicViewRadius(true)
    .build();
```

**Pressure Detection**:
- **CPU Pressure**: Detected when measured TPS drops below target - 2
- **Memory Pressure**: Detected when heap usage exceeds 85% or GC time spikes

**Behavior**:
1. When pressure detected → Reduce view radius by step
2. Continue reducing until pressure subsides or minimum reached
3. When recovered → Gradually increase back to default
4. Player overrides are capped by global limit

**ViewRadiusController**:
```java
ViewRadiusController controller = new ViewRadiusController(10, 4, 16);

// Global adjustments
controller.setGlobalViewRadius(8);
controller.decrease(2);
controller.increase(1);

// Per-player overrides (e.g., premium players)
controller.setPlayerOverride(playerId, 12);
int effective = controller.getViewRadius(playerId); // Min of override and global

// Listen to changes
controller.onRadiusChange((playerId, newRadius) -> {
    if (playerId == null) {
        // Global change - notify all players
    } else {
        // Specific player change
    }
});
```

### 3. Smart Garbage Collection

Triggers GC at optimal times to prevent memory-related crashes.

**Configuration**:
```java
PerformanceConfig.builder()
    .memoryPressureThreshold(0.85)  // 85% heap triggers pressure
    .memoryRecoveryThreshold(0.70)  // 70% heap = recovered
    .gcTimeThreshold(500)           // 500ms GC time = excessive
    .minGcInterval(30000)           // 30s minimum between GC calls
    .chunkUnloadThreshold(100)      // Chunks unloaded to trigger GC
    .enableSmartGc(true)
    .build();
```

**Triggers**:
1. **Chunk Unload Batch**: When many chunks unload at once
2. **Memory Pressure**: When heap usage is high
3. **Server Empty**: When all players leave

**ChunkLoadMonitor**:
```java
ChunkLoadMonitor monitor = new ChunkLoadMonitor(100);

// Track chunk lifecycle
monitor.onChunkLoad();
monitor.onChunkUnload();
monitor.onBulkUnload(50);

// Or set directly from world data
monitor.setCurrentChunks(worldChunkCount);

// Listen for GC suggestions
monitor.onGcSuggestion(chunkCount -> {
    logger.info("GC suggested, {} chunks loaded", chunkCount);
    System.gc();
});

// Get stats
ChunkLoadStats stats = monitor.getStats();
stats.current();     // Currently loaded
stats.peak();        // Peak since reset
stats.getChurnRate(); // Unload/total ratio
```

## Metrics

The `PerformanceMetrics` record provides comprehensive server health data:

```java
PerformanceMetrics metrics = performanceManager.getMetrics();

// TPS
metrics.currentTps();      // Actual measured TPS
metrics.targetTps();       // Target TPS
metrics.getTpsPercent();   // TPS as percentage of target

// Memory
metrics.heapUsed();        // Bytes used
metrics.heapMax();         // Max heap size
metrics.getHeapUsagePercent();
metrics.formatHeapUsed();  // "1.2 GB"

// Status
metrics.cpuPressure();
metrics.memoryPressure();
metrics.serverState();
metrics.isHealthy();
metrics.getStatusSummary(); // "HEALTHY", "CPU_PRESSURE", etc.

// Context
metrics.playerCount();
metrics.chunkCount();
metrics.viewRadius();
```

## Integration Example

```java
public class ServerTickLoop {
    private final PerformanceManager perf;
    private final TpsLimiter limiter;
    private final ViewRadiusController viewRadius;
    private final ChunkLoadMonitor chunks;
    
    public void initialize() {
        PerformanceConfig config = PerformanceConfig.builder()
            .targetTps(20)
            .emptyServerTps(5)
            .defaultViewRadius(10)
            .minimumViewRadius(4)
            .build();
        
        perf = new PerformanceManager(logger);
        perf.setConfig(config);
        
        limiter = new TpsLimiter(config.targetTps());
        viewRadius = new ViewRadiusController(
            config.defaultViewRadius(),
            config.minimumViewRadius(),
            config.maximumViewRadius()
        );
        chunks = new ChunkLoadMonitor(config.chunkUnloadThreshold());
        
        // Wire up listeners
        perf.onViewRadiusChange(viewRadius::setGlobalViewRadius);
        perf.onTpsChange(limiter::setTargetTps);
        chunks.onGcSuggestion(count -> System.gc());
        
        perf.start();
    }
    
    public void runTickLoop() {
        while (running) {
            try {
                limiter.preTickWait();
                
                long start = System.nanoTime();
                doTick();
                long elapsed = System.nanoTime() - start;
                
                perf.recordTick(elapsed);
                limiter.postTick();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private void doTick() {
        // Update performance manager with current state
        perf.updatePlayerCount(getOnlinePlayerCount());
        perf.updateChunkCount(getLoadedChunkCount());
        
        // Apply current view radius to players
        int radius = viewRadius.getGlobalViewRadius();
        for (Player player : getOnlinePlayers()) {
            int effective = viewRadius.getViewRadius(player.getId());
            player.setViewDistance(effective);
        }
        
        // Regular tick logic...
    }
}
```

## Configuration Reference

| Parameter | Default | Description |
|-----------|---------|-------------|
| `targetTps` | 20 | Normal operation TPS |
| `emptyServerTps` | 5 | TPS when no players online |
| `defaultViewRadius` | 10 | Normal view distance (chunks) |
| `minimumViewRadius` | 4 | Emergency minimum radius |
| `maximumViewRadius` | 16 | Upper radius limit |
| `viewRadiusStep` | 1 | Chunks to adjust per step |
| `memoryPressureThreshold` | 0.85 | Heap % to trigger pressure |
| `memoryRecoveryThreshold` | 0.70 | Heap % to clear pressure |
| `gcTimeThreshold` | 500 | GC time (ms) considered excessive |
| `minGcInterval` | 30000 | Minimum ms between GC calls |
| `chunkUnloadThreshold` | 100 | Unloads to trigger GC suggestion |
| `enableTpsLimiting` | true | Enable TPS control |
| `enableDynamicViewRadius` | true | Enable auto view distance |
| `enableSmartGc` | true | Enable intelligent GC |

## Best Practices

1. **Start Conservative**: Begin with defaults, tune based on your hardware
2. **Monitor Metrics**: Log metrics regularly to understand server behavior
3. **Test Under Load**: Simulate high player counts to verify protection works
4. **Premium Overrides**: Use `ViewRadiusController.setPlayerOverride()` for donors
5. **Avoid Over-Tuning**: Let the system adapt; manual intervention often hurts
