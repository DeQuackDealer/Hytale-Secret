# API Reference

> **Document Purpose**: Complete API reference for Rubidium framework integration.

## Package Overview

```
com.yellowtale.rubidium
├── api                          # Public API interfaces
│   ├── anticheat               # Anti-cheat detection
│   ├── command                 # Command system
│   ├── config                  # Configuration
│   ├── event                   # Event system
│   ├── player                  # Player abstractions
│   └── scheduler               # Task scheduling
├── core                         # Core implementation
│   ├── access                  # Access control
│   ├── config                  # Config management
│   ├── lifecycle               # Lifecycle management
│   ├── logging                 # Logging system
│   ├── metrics                 # Performance metrics
│   ├── module                  # Module system
│   ├── network                 # Network handling
│   ├── performance             # Performance budgets
│   └── scheduler               # Scheduler impl
├── chat                         # Chat system
├── economy                      # Economy system
├── party                        # Party system
├── permissions                  # Permission system
├── qol                          # QoL features
├── replay                       # Replay system
├── teleport                     # Teleportation
├── voice                        # Voice chat
└── waypoints                    # Waypoints
```

---

## Core API

### RubidiumCore

Entry point to the framework.

```java
public interface RubidiumCore {
    // Lifecycle
    void start();
    void stop();
    void reload();
    boolean isRunning();
    LifecyclePhase getPhase();
    
    // Managers
    ModuleManager getModuleManager();
    LifecycleManager getLifecycleManager();
    ConfigManager getConfigManager();
    RubidiumScheduler getScheduler();
    EventBus getEventBus();
    NetworkManager getNetworkManager();
    QoLManager getQoLManager();
    MetricsRegistry getMetrics();
    
    // Feature Managers
    VoiceChatManager getVoiceChat();
    WaypointManager getWaypoints();
    PartyManager getParties();
    EconomyManager getEconomy();
    TeleportManager getTeleportation();
    ChatManager getChat();
    PermissionManager getPermissions();
    
    // Factory
    static RubidiumCore create(Path dataDir, RubidiumLogger logger);
}
```

### Player

Player abstraction interface.

```java
public interface Player extends CommandSender {
    // Identity
    UUID getId();
    String getName();
    String getDisplayName();
    
    // Location
    Location getLocation();
    String getWorld();
    Vector3d getPosition();
    float getYaw();
    float getPitch();
    
    // State
    boolean isOnline();
    boolean isOperator();
    GameMode getGameMode();
    double getHealth();
    double getMaxHealth();
    boolean isFlying();
    boolean isSneaking();
    boolean isSprinting();
    
    // Actions
    void teleport(Location location);
    void sendMessage(String message);
    void sendMessage(Component component);
    void sendActionBar(String message);
    void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut);
    void playSound(String sound, float volume, float pitch);
    void kick(String reason);
    
    // Inventory
    PlayerInventory getInventory();
    ItemStack getItemInMainHand();
    ItemStack getItemInOffHand();
    
    // Data
    PlayerData getData();
    void setData(String key, Object value);
    <T> T getData(String key, Class<T> type);
}
```

### Location

Location representation.

```java
public record Location(
    String world,
    double x,
    double y,
    double z,
    float yaw,
    float pitch
) {
    // Constructors
    public static Location of(String world, double x, double y, double z);
    public static Location of(String world, Vector3d position);
    public static Location of(Player player);
    
    // Operations
    public Location add(double dx, double dy, double dz);
    public Location subtract(double dx, double dy, double dz);
    public Location setYaw(float yaw);
    public Location setPitch(float pitch);
    
    // Calculations
    public double distance(Location other);
    public double distanceSquared(Location other);
    public double horizontalDistance(Location other);
    public Vector3d toVector();
    public Location center(); // Center of block
    
    // Serialization
    public String serialize();
    public static Location deserialize(String data);
}
```

---

## Event System

### EventBus

Central event management.

```java
public interface EventBus {
    // Registration
    <T extends Event> void subscribe(Class<T> eventType, Consumer<T> handler);
    <T extends Event> void subscribe(Class<T> eventType, Consumer<T> handler, EventPriority priority);
    void unsubscribe(Object listener);
    
    // Dispatch
    void dispatch(Event event);
    <T extends Event> T dispatchAndReturn(T event);
    CompletableFuture<Void> dispatchAsync(Event event);
    
    // Utilities
    void registerListener(Object listener); // Annotation-based
    int getListenerCount(Class<? extends Event> eventType);
}
```

### Event

Base event class.

```java
public abstract class Event {
    private final long timestamp = System.currentTimeMillis();
    private boolean async = false;
    
    public long getTimestamp();
    public boolean isAsync();
}

public interface Cancellable {
    boolean isCancelled();
    void setCancelled(boolean cancelled);
}
```

### Event Priorities

```java
public enum EventPriority {
    LOWEST(0),      // First to receive, can set initial state
    LOW(1),
    NORMAL(2),      // Default priority
    HIGH(3),
    HIGHEST(4),     // Last to receive, final modifications
    MONITOR(5);     // Read-only, for logging/metrics
}
```

### Common Events

```java
// Player Events
public class PlayerJoinEvent extends PlayerEvent { }
public class PlayerQuitEvent extends PlayerEvent { }
public class PlayerMoveEvent extends PlayerEvent implements Cancellable { }
public class PlayerChatEvent extends PlayerEvent implements Cancellable { }
public class PlayerDeathEvent extends PlayerEvent { }
public class PlayerDamageEvent extends PlayerEvent implements Cancellable { }
public class PlayerCommandEvent extends PlayerEvent implements Cancellable { }

// Economy Events
public class TransactionEvent extends Event implements Cancellable { }
public class BalanceChangeEvent extends Event { }
public class ShopPurchaseEvent extends Event implements Cancellable { }

// Party Events
public class PartyCreatedEvent extends Event { }
public class PartyDisbandedEvent extends Event { }
public class PartyMemberJoinedEvent extends Event { }
public class PartyMemberLeftEvent extends Event { }

// Teleport Events
public class TeleportRequestEvent extends Event implements Cancellable { }
public class PreTeleportEvent extends Event implements Cancellable { }
public class PostTeleportEvent extends Event { }
public class HomeSetEvent extends Event implements Cancellable { }

// Voice Events
public class VoiceStateChangeEvent extends Event { }
public class VoiceChannelJoinEvent extends Event { }
public class VoiceChannelLeaveEvent extends Event { }
```

---

## Scheduler API

### RubidiumScheduler

Task scheduling system.

```java
public interface RubidiumScheduler {
    // Immediate execution
    TaskHandle runTask(String owner, Runnable task);
    TaskHandle runTaskAsync(String owner, Runnable task);
    
    // Delayed execution
    TaskHandle runTaskLater(String owner, Runnable task, long delayTicks);
    TaskHandle runTaskLaterAsync(String owner, Runnable task, long delayTicks);
    
    // Repeating tasks
    TaskHandle runTaskTimer(String owner, Runnable task, long delayTicks, long periodTicks);
    TaskHandle runTaskTimerAsync(String owner, Runnable task, long delayTicks, long periodTicks);
    
    // Priority tasks
    TaskHandle runTask(String owner, Runnable task, TaskPriority priority);
    
    // Async with future
    <T> CompletableFuture<T> runAsync(String owner, Supplier<T> task);
    
    // Task management
    void cancelTask(TaskHandle handle);
    void cancelAllTasks(String owner);
    int getActiveTaskCount(String owner);
    
    // Tick info
    long getCurrentTick();
    double getCurrentTPS();
    double getAverageTPS();
}

public interface TaskHandle {
    int getId();
    String getOwner();
    boolean isRepeating();
    boolean isCancelled();
    void cancel();
}

public enum TaskPriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL
}
```

---

## Configuration API

### ConfigManager

Configuration management.

```java
public interface ConfigManager {
    // Load/save
    <T extends Config> T loadConfig(String name, Class<T> type);
    void saveConfig(String name, Config config);
    void reloadConfig(String name);
    void reloadAll();
    
    // Access
    <T extends Config> T getConfig(String name, Class<T> type);
    boolean hasConfig(String name);
    
    // Watching
    void watch(String name, ConfigReloadListener listener);
    void unwatch(String name, ConfigReloadListener listener);
}

public interface Config {
    void load(Properties props);
    Properties save();
    List<String> validate();
    void setDefaults();
}

@FunctionalInterface
public interface ConfigReloadListener {
    void onReload(Config newConfig);
}
```

---

## Module API

### Module

Module lifecycle interface.

```java
public interface Module {
    // Identity
    String getId();
    String getName();
    String getVersion();
    String getDescription();
    List<String> getAuthors();
    
    // Dependencies
    List<PluginDependency> getDependencies();
    
    // State
    ModuleState getState();
    boolean isEnabled();
    
    // Lifecycle (called by framework)
    void onLoad(ModuleContext context);
    void onEnable();
    void onDisable();
    void onReload();
}

public abstract class AbstractModule implements Module {
    protected RubidiumLogger logger;
    protected ConfigManager configManager;
    protected RubidiumScheduler scheduler;
    protected EventBus eventBus;
    protected Path dataDir;
    
    // Template methods
    protected abstract void doEnable();
    protected abstract void doDisable();
    protected void doReload() { } // Optional override
}

public enum ModuleState {
    UNLOADED,
    LOADING,
    LOADED,
    ENABLING,
    ENABLED,
    DISABLING,
    DISABLED,
    ERROR
}
```

---

## Command API

### CommandManager

Command registration and handling.

```java
public interface CommandManager {
    // Registration
    void register(String name, CommandExecutor executor);
    void register(String name, CommandExecutor executor, String permission);
    void register(String name, CommandExecutor executor, String permission, String... aliases);
    void unregister(String name);
    
    // Tab completion
    void setTabCompleter(String name, TabCompleter completer);
    
    // Execution
    boolean dispatch(CommandSender sender, String commandLine);
}

@FunctionalInterface
public interface CommandExecutor {
    void execute(CommandSender sender, String[] args);
}

@FunctionalInterface
public interface TabCompleter {
    List<String> complete(CommandSender sender, String[] args);
}

public interface CommandSender {
    void sendMessage(String message);
    void sendMessage(Component component);
    boolean hasPermission(String permission);
    String getName();
    boolean isPlayer();
    Optional<Player> asPlayer();
}
```

### Annotation-Based Commands

```java
@Command(name = "example", permission = "example.use", aliases = {"ex"})
public class ExampleCommand {
    
    @Subcommand("create")
    @Permission("example.create")
    @Description("Create something")
    public void create(Player player, @Arg("name") String name) {
        // Implementation
    }
    
    @Subcommand("delete")
    @Permission("example.delete")
    public void delete(Player player, @Arg("name") String name, @Flag("force") boolean force) {
        // Implementation
    }
    
    @TabComplete("create")
    public List<String> completeCreate(Player player, String partial) {
        return suggestions.stream()
            .filter(s -> s.startsWith(partial))
            .toList();
    }
}
```

---

## Economy API

### EconomyManager

Economy operations.

```java
public interface EconomyManager {
    // Balance
    long getBalance(UUID playerId);
    long getBalance(UUID playerId, String currency);
    boolean hasBalance(UUID playerId, long amount);
    boolean hasBalance(UUID playerId, String currency, long amount);
    
    // Transactions
    Transaction deposit(UUID playerId, long amount, String reason);
    Transaction deposit(UUID playerId, String currency, long amount, String reason);
    Transaction withdraw(UUID playerId, long amount, String reason);
    Transaction withdraw(UUID playerId, String currency, long amount, String reason);
    Transaction transfer(UUID from, UUID to, long amount, String description);
    Transaction transfer(UUID from, UUID to, String currency, long amount, String description);
    
    // Currency
    Currency getPrimaryCurrency();
    Optional<Currency> getCurrency(String id);
    List<Currency> getCurrencies();
    
    // Formatting
    String format(long amount);
    String format(String currency, long amount);
    
    // History
    List<Transaction> getHistory(UUID playerId, int limit);
}
```

---

## Permission API

### PermissionManager

Permission operations.

```java
public interface PermissionManager {
    // Checks
    boolean hasPermission(UUID playerId, String permission);
    boolean hasPermission(UUID playerId, String permission, PermissionContext context);
    
    // Roles
    void addRole(UUID playerId, Role role);
    void removeRole(UUID playerId, Role role);
    Set<Role> getRoles(UUID playerId);
    Role getPrimaryRole(UUID playerId);
    
    // Direct permissions
    void setPermission(UUID playerId, String permission, boolean value);
    void setPermission(UUID playerId, String permission, boolean value, Duration duration);
    void unsetPermission(UUID playerId, String permission);
    
    // Prefix/Suffix
    String getPrefix(UUID playerId);
    String getSuffix(UUID playerId);
    
    // Role management
    Optional<Role> getRole(String id);
    List<Role> getAllRoles();
    void setRolePermission(String roleId, String permission, boolean value);
}
```

---

## Teleport API

### TeleportManager

Teleportation operations.

```java
public interface TeleportManager {
    // Teleportation
    TeleportResult teleport(UUID playerId, Location destination);
    TeleportResult teleportToPlayer(UUID playerId, UUID targetId);
    TeleportResult teleportToHome(UUID playerId, String homeName);
    TeleportResult teleportToWarp(UUID playerId, String warpName);
    TeleportResult teleportToSpawn(UUID playerId);
    TeleportResult teleportBack(UUID playerId);
    
    // Requests
    TeleportRequest requestTpa(UUID requester, UUID target);
    TeleportRequest requestTpaHere(UUID requester, UUID target);
    void acceptRequest(UUID requestId);
    void denyRequest(UUID requestId);
    
    // Homes
    Home setHome(UUID playerId, String name, Location location);
    void deleteHome(UUID playerId, String name);
    List<Home> getHomes(UUID playerId);
    Optional<Home> getHome(UUID playerId, String name);
    int getMaxHomes(UUID playerId);
    
    // Warps
    List<Warp> getWarps();
    List<Warp> getAccessibleWarps(UUID playerId);
    Optional<Warp> getWarp(String name);
}

public record TeleportResult(
    boolean success,
    String message,
    TeleportType type,
    Location from,
    Location to
) {}
```

---

## Party API

### PartyManager

Party operations.

```java
public interface PartyManager {
    // Party lifecycle
    Party createParty(UUID leader, String name);
    void disbandParty(UUID partyId);
    
    // Membership
    void addMember(UUID partyId, UUID playerId);
    void removeMember(UUID partyId, UUID playerId);
    void kickMember(UUID partyId, UUID playerId, String reason);
    
    // Invites
    PartyInvite invitePlayer(UUID partyId, UUID inviter, UUID invitee);
    void acceptInvite(UUID inviteId);
    void declineInvite(UUID inviteId);
    
    // Queries
    Optional<Party> getParty(UUID partyId);
    Optional<Party> getPlayerParty(UUID playerId);
    boolean isInParty(UUID playerId);
    boolean isPartyLeader(UUID playerId);
    boolean areInSameParty(UUID player1, UUID player2);
    
    // Events
    void onPartyCreated(Consumer<Party> callback);
    void onMemberJoined(BiConsumer<Party, UUID> callback);
    void onMemberLeft(BiConsumer<Party, UUID> callback);
}
```

---

## Voice API

### VoiceChatManager

Voice chat operations.

```java
public interface VoiceChatManager {
    // State
    void setMuted(UUID playerId, boolean muted);
    void setDeafened(UUID playerId, boolean deafened);
    boolean isMuted(UUID playerId);
    boolean isDeafened(UUID playerId);
    boolean isSpeaking(UUID playerId);
    
    // Volume
    void setInputVolume(UUID playerId, float volume);
    void setOutputVolume(UUID playerId, float volume);
    
    // Channels
    VoiceChannel createChannel(String name, ChannelType type);
    void deleteChannel(String channelId);
    void joinChannel(UUID playerId, VoiceChannel channel);
    void leaveChannel(UUID playerId, VoiceChannel channel);
    Optional<VoiceChannel> getCurrentChannel(UUID playerId);
    List<VoiceChannel> getChannels();
    
    // Admin
    void serverMute(UUID playerId, boolean muted);
    void setPrioritySpeaker(UUID playerId, boolean priority);
}
```

---

## Waypoint API

### WaypointManager

Waypoint operations.

```java
public interface WaypointManager {
    // CRUD
    Waypoint createWaypoint(UUID owner, String name, Location location);
    void deleteWaypoint(UUID waypointId);
    void updateWaypoint(UUID waypointId, WaypointUpdate update);
    
    // Queries
    Optional<Waypoint> getWaypoint(UUID waypointId);
    List<Waypoint> getPlayerWaypoints(UUID playerId);
    List<Waypoint> getVisibleWaypoints(UUID playerId);
    List<Waypoint> getNearbyWaypoints(Location center, double radius);
    
    // Sharing
    void shareWaypoint(UUID waypointId, UUID targetPlayer);
    void unshareWaypoint(UUID waypointId, UUID targetPlayer);
    
    // Navigation
    NavigationData getNavigation(UUID playerId, UUID waypointId);
    void setActiveWaypoint(UUID playerId, UUID waypointId);
    void clearActiveWaypoint(UUID playerId);
}
```

---

## Utility Classes

### Vector3d

3D vector mathematics.

```java
public record Vector3d(double x, double y, double z) {
    // Constants
    public static final Vector3d ZERO = new Vector3d(0, 0, 0);
    public static final Vector3d UP = new Vector3d(0, 1, 0);
    public static final Vector3d DOWN = new Vector3d(0, -1, 0);
    
    // Operations
    public Vector3d add(Vector3d other);
    public Vector3d subtract(Vector3d other);
    public Vector3d multiply(double scalar);
    public Vector3d divide(double scalar);
    public Vector3d normalize();
    public Vector3d cross(Vector3d other);
    public double dot(Vector3d other);
    public double length();
    public double lengthSquared();
    public double distance(Vector3d other);
}
```

### Duration Utilities

```java
public class DurationParser {
    public static Duration parse(String input) {
        // Formats: "30s", "5m", "2h", "1d", "1w"
        // Combined: "1d12h30m"
    }
    
    public static String format(Duration duration) {
        // Returns human-readable format
    }
}
```

---

## Best Practices

### Event Handling

```java
// DO: Use specific event types
eventBus.subscribe(PlayerJoinEvent.class, this::onJoin);

// DON'T: Subscribe to base Event class
eventBus.subscribe(Event.class, this::onAnyEvent); // Bad

// DO: Use appropriate priority
eventBus.subscribe(PlayerChatEvent.class, this::formatChat, EventPriority.HIGH);
eventBus.subscribe(PlayerChatEvent.class, this::logChat, EventPriority.MONITOR);
```

### Task Scheduling

```java
// DO: Use owner string for cleanup
scheduler.runTaskTimer("my-module", this::tick, 0, 20);

// DON'T: Use anonymous tasks that can't be cancelled
scheduler.runTaskTimer("", () -> tick(), 0, 20); // Bad

// DO: Clean up on disable
public void onDisable() {
    scheduler.cancelAllTasks("my-module");
}
```

### Permission Checks

```java
// DO: Check permissions before operations
if (permissions.hasPermission(player.getId(), "economy.pay")) {
    processPayment(player, target, amount);
}

// DO: Use context when relevant
if (permissions.hasPermission(player.getId(), "build.place", PermissionContext.world(player.getWorld()))) {
    allowBlockPlace();
}
```
