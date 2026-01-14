package rubidium.teleport;

import rubidium.api.player.Player;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class TeleportManager {
    
    private final Map<UUID, TeleportRequest> pendingRequests;
    private final Map<UUID, Location> lastLocations;
    private final ScheduledExecutorService scheduler;
    
    private int requestTimeout = 60;
    private int teleportDelay = 3;
    private boolean cancelOnMove = true;
    private boolean cancelOnDamage = true;
    
    public TeleportManager() {
        this.pendingRequests = new ConcurrentHashMap<>();
        this.lastLocations = new ConcurrentHashMap<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Rubidium-Teleport");
            t.setDaemon(true);
            return t;
        });
        
        startCleanupTask();
    }
    
    public void teleport(Player player, double x, double y, double z) {
        teleport(player, x, y, z, player.getYaw(), player.getPitch());
    }
    
    public void teleport(Player player, double x, double y, double z, float yaw, float pitch) {
        saveLastLocation(player);
        player.teleport(x, y, z, yaw, pitch);
    }
    
    public void teleport(Player player, Location location) {
        teleport(player, location.x(), location.y(), location.z(), location.yaw(), location.pitch());
    }
    
    public void teleportDelayed(Player player, Location target, Consumer<TeleportResult> callback) {
        if (teleportDelay <= 0) {
            teleport(player, target);
            callback.accept(TeleportResult.ok());
            return;
        }
        
        UUID playerId = player.getUUID();
        Location startLocation = getLocation(player);
        
        TeleportTask task = new TeleportTask(playerId, target, startLocation, callback);
        
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            Player p = getPlayer(playerId);
            if (p == null || !p.isOnline()) {
                callback.accept(TeleportResult.failure("Player offline"));
                return;
            }
            
            teleport(p, target);
            callback.accept(TeleportResult.ok());
        }, teleportDelay, TimeUnit.SECONDS);
        
        task.setFuture(future);
        pendingRequests.put(playerId, new TeleportRequest(playerId, playerId, TeleportType.DELAYED, task));
    }
    
    public void requestTeleport(Player requester, Player target) {
        UUID requesterId = requester.getUUID();
        UUID targetId = target.getUUID();
        
        if (pendingRequests.containsKey(targetId)) {
            return;
        }
        
        TeleportRequest request = new TeleportRequest(requesterId, targetId, TeleportType.TPA, null);
        pendingRequests.put(targetId, request);
        
        scheduler.schedule(() -> {
            TeleportRequest pending = pendingRequests.get(targetId);
            if (pending != null && pending.requesterId().equals(requesterId)) {
                pendingRequests.remove(targetId);
            }
        }, requestTimeout, TimeUnit.SECONDS);
    }
    
    public void requestTeleportHere(Player requester, Player target) {
        UUID requesterId = requester.getUUID();
        UUID targetId = target.getUUID();
        
        if (pendingRequests.containsKey(targetId)) {
            return;
        }
        
        TeleportRequest request = new TeleportRequest(requesterId, targetId, TeleportType.TPAHERE, null);
        pendingRequests.put(targetId, request);
        
        scheduler.schedule(() -> {
            TeleportRequest pending = pendingRequests.get(targetId);
            if (pending != null && pending.requesterId().equals(requesterId)) {
                pendingRequests.remove(targetId);
            }
        }, requestTimeout, TimeUnit.SECONDS);
    }
    
    public boolean acceptRequest(Player target) {
        TeleportRequest request = pendingRequests.remove(target.getUUID());
        if (request == null) return false;
        
        Player requester = getPlayer(request.requesterId());
        if (requester == null || !requester.isOnline()) return false;
        
        if (request.type() == TeleportType.TPA) {
            teleport(requester, getLocation(target));
        } else if (request.type() == TeleportType.TPAHERE) {
            teleport(target, getLocation(requester));
        }
        
        return true;
    }
    
    public boolean denyRequest(Player target) {
        return pendingRequests.remove(target.getUUID()) != null;
    }
    
    public boolean cancelRequest(Player requester) {
        UUID requesterId = requester.getUUID();
        
        for (Iterator<Map.Entry<UUID, TeleportRequest>> it = pendingRequests.entrySet().iterator(); it.hasNext();) {
            Map.Entry<UUID, TeleportRequest> entry = it.next();
            if (entry.getValue().requesterId().equals(requesterId)) {
                it.remove();
                return true;
            }
        }
        
        return false;
    }
    
    public boolean teleportBack(Player player) {
        Location last = lastLocations.get(player.getUUID());
        if (last == null) return false;
        
        teleport(player, last);
        return true;
    }
    
    public void saveLastLocation(Player player) {
        lastLocations.put(player.getUUID(), getLocation(player));
    }
    
    public Optional<Location> getLastLocation(UUID playerId) {
        return Optional.ofNullable(lastLocations.get(playerId));
    }
    
    public void onPlayerMove(Player player, double newX, double newY, double newZ) {
        if (!cancelOnMove) return;
        
        TeleportRequest request = pendingRequests.get(player.getUUID());
        if (request != null && request.task() != null) {
            Location start = request.task().startLocation();
            double distance = Math.sqrt(
                Math.pow(newX - start.x(), 2) +
                Math.pow(newY - start.y(), 2) +
                Math.pow(newZ - start.z(), 2)
            );
            
            if (distance > 0.5) {
                cancelTeleport(player.getUUID(), "Moved during teleport");
            }
        }
    }
    
    public void onPlayerDamage(Player player) {
        if (!cancelOnDamage) return;
        
        if (pendingRequests.containsKey(player.getUUID())) {
            cancelTeleport(player.getUUID(), "Took damage during teleport");
        }
    }
    
    private void cancelTeleport(UUID playerId, String reason) {
        TeleportRequest request = pendingRequests.remove(playerId);
        if (request != null && request.task() != null) {
            request.task().future().cancel(false);
            request.task().callback().accept(TeleportResult.failure(reason));
        }
    }
    
    public void setRequestTimeout(int seconds) {
        this.requestTimeout = seconds;
    }
    
    public void setTeleportDelay(int seconds) {
        this.teleportDelay = seconds;
    }
    
    public void setCancelOnMove(boolean cancel) {
        this.cancelOnMove = cancel;
    }
    
    public void setCancelOnDamage(boolean cancel) {
        this.cancelOnDamage = cancel;
    }
    
    private void startCleanupTask() {
        scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            pendingRequests.entrySet().removeIf(entry -> 
                now - entry.getValue().createdAt() > requestTimeout * 1000L
            );
        }, 60, 60, TimeUnit.SECONDS);
    }
    
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    private Location getLocation(Player player) {
        return new Location(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch(), player.getWorld());
    }
    
    private Player getPlayer(UUID playerId) {
        return null;
    }
    
    public record Location(double x, double y, double z, float yaw, float pitch, String world) {}
    
    public record TeleportRequest(UUID requesterId, UUID targetId, TeleportType type, TeleportTask task, long createdAt) {
        public TeleportRequest(UUID requesterId, UUID targetId, TeleportType type, TeleportTask task) {
            this(requesterId, targetId, type, task, System.currentTimeMillis());
        }
    }
    
    public record TeleportResult(boolean succeeded, String message) {
        public boolean success() {
            return succeeded;
        }
        
        public static TeleportResult ok() {
            return new TeleportResult(true, "Teleported successfully");
        }
        
        public static TeleportResult failure(String reason) {
            return new TeleportResult(false, reason);
        }
    }
    
    public enum TeleportType {
        TPA, TPAHERE, DELAYED, INSTANT
    }
    
    public static class TeleportTask {
        private final UUID playerId;
        private final Location target;
        private final Location startLocation;
        private final Consumer<TeleportResult> callback;
        private ScheduledFuture<?> future;
        
        public TeleportTask(UUID playerId, Location target, Location startLocation, Consumer<TeleportResult> callback) {
            this.playerId = playerId;
            this.target = target;
            this.startLocation = startLocation;
            this.callback = callback;
        }
        
        public UUID playerId() { return playerId; }
        public Location target() { return target; }
        public Location startLocation() { return startLocation; }
        public Consumer<TeleportResult> callback() { return callback; }
        public ScheduledFuture<?> future() { return future; }
        
        public void setFuture(ScheduledFuture<?> future) {
            this.future = future;
        }
    }
}
