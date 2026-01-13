# Rubidium Architecture

> **Document Purpose**: Describes the overall system architecture, design patterns, and core principles of the Rubidium framework.

## Design Philosophy

### 1. API-Safe Design
Rubidium is designed to work without access to Hytale internals:
- No memory injection or bytecode manipulation
- No decompilation or reverse engineering
- Ready for official modding APIs when available
- Uses adapter pattern for game integration

### 2. Modular Architecture
Every component is designed for hot-reload capability:
- Runtime loading/unloading without server restart
- Isolated classloaders per module
- Clean dependency injection
- Graceful failure handling

### 3. Performance-First
Built for high-performance game servers:
- Lock-free data structures where possible
- Object pooling to reduce GC pressure
- Tick-budget management
- Async I/O for non-critical operations

## Architectural Layers

```
┌─────────────────────────────────────────────────────────────────┐
│                        Plugin Layer                              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐           │
│  │ Plugin A │ │ Plugin B │ │ Plugin C │ │ Plugin D │           │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘           │
├─────────────────────────────────────────────────────────────────┤
│                        Feature Layer                             │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐           │
│  │ QoL      │ │ Voice    │ │ Waypoint │ │ Economy  │           │
│  │ Features │ │ Chat     │ │ System   │ │ System   │           │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘           │
├─────────────────────────────────────────────────────────────────┤
│                        Core Layer                                │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐           │
│  │ Module   │ │ Event    │ │ Scheduler│ │ Config   │           │
│  │ Manager  │ │ Bus      │ │          │ │ Manager  │           │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘           │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐           │
│  │ Network  │ │ Lifecycle│ │ Metrics  │ │ Logging  │           │
│  │ Manager  │ │ Manager  │ │ Registry │ │ Manager  │           │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘           │
├─────────────────────────────────────────────────────────────────┤
│                        Adapter Layer                             │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                    Game Adapter Interface                    ││
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       ││
│  │  │ Player   │ │ World    │ │ Entity   │ │ Protocol │       ││
│  │  │ Adapter  │ │ Adapter  │ │ Adapter  │ │ Adapter  │       ││
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘       ││
│  └─────────────────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────────────────┤
│                        Game Runtime                              │
│                    (Hytale Server Process)                       │
└─────────────────────────────────────────────────────────────────┘
```

## Core Components

### RubidiumCore
Central orchestrator for all subsystems.

```java
public interface RubidiumCore {
    // Lifecycle
    void start();
    void stop();
    void reload();
    
    // Managers
    ModuleManager getModuleManager();
    LifecycleManager getLifecycleManager();
    ConfigManager getConfigManager();
    RubidiumScheduler getScheduler();
    EventBus getEventBus();
    NetworkManager getNetworkManager();
    QoLManager getQoLManager();
    MetricsRegistry getMetrics();
    
    // State
    boolean isRunning();
    LifecyclePhase getPhase();
}
```

### Lifecycle Phases

```
STOPPED ──► STARTING ──► RUNNING ──► STOPPING ──► STOPPED
                │                        ▲
                └──► RELOADING ──────────┘
```

| Phase | Description |
|-------|-------------|
| `STOPPED` | Framework not running |
| `STARTING` | Initializing subsystems |
| `RUNNING` | Normal operation |
| `STOPPING` | Graceful shutdown |
| `RELOADING` | Hot-reload in progress |

## Design Patterns

### 1. Adapter Pattern
Abstracts game-specific functionality:

```java
public interface GameAdapter {
    PlayerAdapter getPlayer(UUID id);
    WorldAdapter getWorld(String name);
    void broadcast(String message);
    double getCurrentTPS();
}

public interface PlayerAdapter {
    UUID getId();
    String getName();
    Vector3d getPosition();
    void sendMessage(String message);
    void teleport(Vector3d position);
}
```

### 2. Observer Pattern (Events)

```java
@EventHandler(priority = EventPriority.NORMAL)
public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    // Handle event
}
```

### 3. Strategy Pattern (Features)

```java
public abstract class QoLFeature {
    public abstract String getId();
    protected abstract void onEnable();
    protected abstract void onDisable();
    protected abstract void onTick();
}
```

### 4. Builder Pattern (Configuration)

```java
ReplayConfig config = ReplayConfig.builder()
    .targetFps(20)
    .captureRadius(64)
    .bufferDuration(Duration.ofMinutes(5))
    .compressionLevel(6)
    .build();
```

### 5. Factory Pattern (Modules)

```java
ModuleContainer container = ModuleFactory.create(
    moduleClass,
    context,
    dependencies
);
```

## Thread Model

```
┌─────────────────────────────────────────────────────────────────┐
│                         Main Thread                              │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Game Tick Loop (20 TPS)                                  │   │
│  │  ├── Scheduler.tick()                                     │   │
│  │  ├── QoLManager.tick()                                    │   │
│  │  ├── NetworkManager.processInbound()                      │   │
│  │  └── EventBus.dispatchSync()                              │   │
│  └──────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────┤
│                       Worker Threads                             │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐           │
│  │ Async    │ │ Config   │ │ Replay   │ │ Voice    │           │
│  │ Tasks    │ │ Watcher  │ │ Storage  │ │ Encoder  │           │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘           │
├─────────────────────────────────────────────────────────────────┤
│                        I/O Threads                               │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                        │
│  │ Network  │ │ Database │ │ File I/O │                        │
│  │ Netty    │ │ Pool     │ │ Pool     │                        │
│  └──────────┘ └──────────┘ └──────────┘                        │
└─────────────────────────────────────────────────────────────────┘
```

### Thread Safety Guidelines

1. **Main Thread Only**
   - Player state modifications
   - World mutations
   - Synchronous events

2. **Thread-Safe**
   - Configuration reads
   - Metrics updates
   - Logging

3. **Background Only**
   - File I/O
   - Network I/O
   - Compression
   - Database queries

## Memory Management

### Object Pooling

```java
public class ObjectPool<T> {
    private final Queue<T> pool;
    private final Supplier<T> factory;
    private final Consumer<T> reset;
    
    public T acquire() {
        T obj = pool.poll();
        return obj != null ? obj : factory.get();
    }
    
    public void release(T obj) {
        reset.accept(obj);
        pool.offer(obj);
    }
}
```

### Buffer Management

| Buffer Type | Usage | Size Limit |
|-------------|-------|------------|
| Replay Buffer | Per-player ring buffer | 5 minutes @ 20 FPS |
| Packet Buffer | Network I/O | 64 KB per connection |
| Audio Buffer | Voice chat encoding | 20ms frames |
| Config Cache | Hot-reload cache | Unlimited |

## Error Handling

### Exception Hierarchy

```
RubidiumException
├── ConfigException
│   └── ConfigValidationException
├── ModuleException
│   ├── ModuleLoadException
│   └── ModuleDependencyException
├── NetworkException
│   └── PacketException
└── FeatureException
```

### Recovery Strategies

| Error Type | Strategy |
|------------|----------|
| Config validation | Fall back to defaults |
| Module load failure | Skip module, log error |
| Network error | Reconnect with backoff |
| Task exception | Log and continue |
| Critical error | Graceful shutdown |

## Performance Budgets

```java
public class PerformanceBudgetManager {
    // Target: 50ms per tick (20 TPS)
    private static final long TICK_BUDGET_MS = 50;
    
    // Allocation per system
    private static final double SCHEDULER_BUDGET = 0.30;  // 15ms
    private static final double QOL_BUDGET = 0.20;        // 10ms
    private static final double NETWORK_BUDGET = 0.25;    // 12.5ms
    private static final double EVENTS_BUDGET = 0.15;     // 7.5ms
    private static final double RESERVE_BUDGET = 0.10;    // 5ms
}
```

## Configuration Hierarchy

```
rubidium/
├── config/
│   ├── rubidium.yml         # Core configuration
│   ├── modules.yml          # Module settings
│   ├── qol-features.yml     # QoL feature settings
│   ├── voice.yml            # Voice chat settings
│   ├── waypoints.yml        # Waypoint settings
│   ├── economy.yml          # Economy settings
│   ├── permissions.yml      # Permission definitions
│   └── chat.yml             # Chat system settings
├── data/
│   ├── players/             # Per-player data
│   ├── waypoints/           # Waypoint storage
│   ├── replays/             # Replay recordings
│   └── economy/             # Transaction logs
└── modules/
    └── *.jar                # Loaded modules
```
