package rubidium.api.teleport;

import rubidium.api.pathfinding.PathfindingAPI.Vec3i;
import rubidium.api.event.EventAPI;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public final class TeleportAPI {
    
    private static final Map<String, Warp> warps = new ConcurrentHashMap<>();
    private static final Map<UUID, TeleportRequest> pendingRequests = new ConcurrentHashMap<>();
    private static final Map<UUID, Vec3i> lastLocations = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    private static int defaultWarmup = 0;
    private static int requestTimeout = 60;
    
    private TeleportAPI() {}
    
    public static void teleport(UUID playerId, Vec3i destination) {
        teleport(playerId, destination, 0, null);
    }
    
    public static void teleport(UUID playerId, Vec3i destination, int warmupTicks, Consumer<Boolean> callback) {
        TeleportEvent event = new TeleportEvent(playerId, destination, warmupTicks);
        EventAPI.fire(event);
        
        if (event.isCancelled()) {
            if (callback != null) callback.accept(false);
            return;
        }
        
        if (warmupTicks > 0) {
            scheduler.schedule(() -> {
                performTeleport(playerId, destination);
                if (callback != null) callback.accept(true);
            }, warmupTicks * 50L, TimeUnit.MILLISECONDS);
        } else {
            performTeleport(playerId, destination);
            if (callback != null) callback.accept(true);
        }
    }
    
    private static void performTeleport(UUID playerId, Vec3i destination) {
    }
    
    public static void teleportToPlayer(UUID playerId, UUID targetId) {
    }
    
    public static void teleportPlayerHere(UUID targetId, UUID playerId) {
    }
    
    public static TeleportRequest sendRequest(UUID fromId, UUID toId, RequestType type) {
        TeleportRequest request = new TeleportRequest(
            UUID.randomUUID(),
            fromId,
            toId,
            type,
            System.currentTimeMillis() + requestTimeout * 1000L
        );
        pendingRequests.put(request.id(), request);
        
        scheduler.schedule(() -> {
            pendingRequests.remove(request.id());
        }, requestTimeout, TimeUnit.SECONDS);
        
        return request;
    }
    
    public static boolean acceptRequest(UUID requestId) {
        TeleportRequest request = pendingRequests.remove(requestId);
        if (request == null || request.isExpired()) return false;
        
        if (request.type() == RequestType.TPA) {
            teleportToPlayer(request.from(), request.to());
        } else {
            teleportPlayerHere(request.from(), request.to());
        }
        return true;
    }
    
    public static boolean denyRequest(UUID requestId) {
        return pendingRequests.remove(requestId) != null;
    }
    
    public static List<TeleportRequest> getPendingRequests(UUID playerId) {
        return pendingRequests.values().stream()
            .filter(r -> r.to().equals(playerId) && !r.isExpired())
            .toList();
    }
    
    public static void createWarp(String name, Vec3i location) {
        createWarp(name, location, null, null);
    }
    
    public static void createWarp(String name, Vec3i location, String permission, String displayName) {
        warps.put(name.toLowerCase(), new Warp(name, displayName != null ? displayName : name, location, permission));
    }
    
    public static boolean warpTo(UUID playerId, String warpName) {
        Warp warp = warps.get(warpName.toLowerCase());
        if (warp == null) return false;
        
        teleport(playerId, warp.location());
        return true;
    }
    
    public static Optional<Warp> getWarp(String name) {
        return Optional.ofNullable(warps.get(name.toLowerCase()));
    }
    
    public static Collection<Warp> getAllWarps() {
        return warps.values();
    }
    
    public static void deleteWarp(String name) {
        warps.remove(name.toLowerCase());
    }
    
    public static void saveLastLocation(UUID playerId, Vec3i location) {
        lastLocations.put(playerId, location);
    }
    
    public static Optional<Vec3i> getLastLocation(UUID playerId) {
        return Optional.ofNullable(lastLocations.get(playerId));
    }
    
    public static boolean back(UUID playerId) {
        Vec3i last = lastLocations.get(playerId);
        if (last == null) return false;
        
        teleport(playerId, last);
        return true;
    }
    
    public static void setDefaultWarmup(int ticks) {
        defaultWarmup = ticks;
    }
    
    public static void setRequestTimeout(int seconds) {
        requestTimeout = seconds;
    }
    
    public static int getDefaultWarmup() {
        return defaultWarmup;
    }
    
    public record Warp(String name, String displayName, Vec3i location, String permission) {
        public boolean hasPermission() {
            return permission != null && !permission.isEmpty();
        }
    }
    
    public record TeleportRequest(UUID id, UUID from, UUID to, RequestType type, long expiresAt) {
        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
        
        public long getRemainingSeconds() {
            return Math.max(0, (expiresAt - System.currentTimeMillis()) / 1000);
        }
    }
    
    public enum RequestType {
        TPA,
        TPA_HERE
    }
    
    public static class TeleportEvent extends EventAPI.CancellableEvent {
        private final UUID playerId;
        private Vec3i destination;
        private int warmupTicks;
        
        public TeleportEvent(UUID playerId, Vec3i destination, int warmupTicks) {
            this.playerId = playerId;
            this.destination = destination;
            this.warmupTicks = warmupTicks;
        }
        
        public UUID getPlayerId() { return playerId; }
        public Vec3i getDestination() { return destination; }
        public int getWarmupTicks() { return warmupTicks; }
        public void setDestination(Vec3i dest) { this.destination = dest; }
        public void setWarmupTicks(int ticks) { this.warmupTicks = ticks; }
    }
}
