package rubidium.waypoints;

import rubidium.core.logging.RubidiumLogger;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class WaypointManager {
    
    private final RubidiumLogger logger;
    private final Path dataDir;
    private WaypointConfig config;
    
    private final Map<UUID, List<Waypoint>> playerWaypoints;
    private final List<Waypoint> serverWaypoints;
    private final Map<UUID, UUID> activeWaypoints;
    private final Map<UUID, List<WaypointCategory>> playerCategories;
    
    private final List<Consumer<Waypoint>> createListeners;
    private final List<Consumer<Waypoint>> deleteListeners;
    private final List<Consumer<Waypoint>> updateListeners;
    
    public static final int MAX_WAYPOINTS_PER_PLAYER = 100;
    public static final int MAX_SHARED_WAYPOINTS = 50;
    public static final int MAX_NAME_LENGTH = 32;
    public static final int MAX_DESCRIPTION_LENGTH = 256;
    
    public WaypointManager(RubidiumLogger logger, Path dataDir) {
        this.logger = logger;
        this.dataDir = dataDir;
        this.config = WaypointConfig.defaults();
        this.playerWaypoints = new ConcurrentHashMap<>();
        this.serverWaypoints = new ArrayList<>();
        this.activeWaypoints = new ConcurrentHashMap<>();
        this.playerCategories = new ConcurrentHashMap<>();
        this.createListeners = new ArrayList<>();
        this.deleteListeners = new ArrayList<>();
        this.updateListeners = new ArrayList<>();
    }
    
    public Waypoint createWaypoint(UUID owner, String name, Location location) {
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Waypoint name too long (max " + MAX_NAME_LENGTH + ")");
        }
        
        List<Waypoint> waypoints = playerWaypoints.computeIfAbsent(owner, k -> new ArrayList<>());
        if (waypoints.size() >= MAX_WAYPOINTS_PER_PLAYER) {
            throw new IllegalStateException("Maximum waypoints reached (" + MAX_WAYPOINTS_PER_PLAYER + ")");
        }
        
        Waypoint waypoint = new Waypoint(UUID.randomUUID(), name, owner);
        waypoint.setWorld(location.world());
        waypoint.setX(location.x());
        waypoint.setY(location.y());
        waypoint.setZ(location.z());
        waypoint.setCreatedAt(System.currentTimeMillis());
        waypoint.setCategory(WaypointCategory.CUSTOM);
        
        waypoints.add(waypoint);
        
        for (Consumer<Waypoint> listener : createListeners) {
            listener.accept(waypoint);
        }
        
        logger.debug("Created waypoint '{}' for player {}", name, owner);
        return waypoint;
    }
    
    public void deleteWaypoint(UUID waypointId) {
        for (var entry : playerWaypoints.entrySet()) {
            List<Waypoint> waypoints = entry.getValue();
            Optional<Waypoint> found = waypoints.stream()
                .filter(w -> w.getId().equals(waypointId))
                .findFirst();
            
            if (found.isPresent()) {
                Waypoint waypoint = found.get();
                waypoints.remove(waypoint);
                
                activeWaypoints.values().removeIf(id -> id.equals(waypointId));
                
                for (Consumer<Waypoint> listener : deleteListeners) {
                    listener.accept(waypoint);
                }
                
                logger.debug("Deleted waypoint '{}'", waypoint.getName());
                return;
            }
        }
        
        serverWaypoints.removeIf(w -> w.getId().equals(waypointId));
    }
    
    public void updateWaypoint(UUID waypointId, WaypointUpdate update) {
        Optional<Waypoint> found = findWaypoint(waypointId);
        if (found.isEmpty()) return;
        
        Waypoint waypoint = found.get();
        
        if (update.name() != null) {
            waypoint.setName(update.name());
        }
        if (update.description() != null) {
            waypoint.setDescription(update.description());
        }
        if (update.category() != null) {
            waypoint.setCategory(update.category());
        }
        if (update.color() != null) {
            waypoint.setColor(update.color());
        }
        if (update.icon() != null) {
            waypoint.setIcon(update.icon());
        }
        if (update.showBeam() != null) {
            waypoint.setShowBeam(update.showBeam());
        }
        if (update.showDistance() != null) {
            waypoint.setShowDistance(update.showDistance());
        }
        if (update.visibility() != null) {
            waypoint.setVisibility(update.visibility());
        }
        
        for (Consumer<Waypoint> listener : updateListeners) {
            listener.accept(waypoint);
        }
    }
    
    public Optional<Waypoint> getWaypoint(UUID waypointId) {
        return findWaypoint(waypointId);
    }
    
    public List<Waypoint> getPlayerWaypoints(UUID playerId) {
        return new ArrayList<>(playerWaypoints.getOrDefault(playerId, Collections.emptyList()));
    }
    
    public List<Waypoint> getVisibleWaypoints(UUID playerId) {
        List<Waypoint> visible = new ArrayList<>();
        
        visible.addAll(getPlayerWaypoints(playerId));
        
        visible.addAll(serverWaypoints);
        
        for (var entry : playerWaypoints.entrySet()) {
            if (entry.getKey().equals(playerId)) continue;
            
            for (Waypoint wp : entry.getValue()) {
                if (wp.getSharedWith().contains(playerId)) {
                    visible.add(wp);
                } else if (wp.getVisibility() == Waypoint.WaypointVisibility.PUBLIC) {
                    visible.add(wp);
                }
            }
        }
        
        return visible;
    }
    
    public List<Waypoint> getNearbyWaypoints(Location center, double radius) {
        List<Waypoint> nearby = new ArrayList<>();
        double radiusSquared = radius * radius;
        
        for (List<Waypoint> waypoints : playerWaypoints.values()) {
            for (Waypoint wp : waypoints) {
                if (!wp.getWorld().equals(center.world())) continue;
                
                double dx = wp.getX() - center.x();
                double dy = wp.getY() - center.y();
                double dz = wp.getZ() - center.z();
                double distSquared = dx * dx + dy * dy + dz * dz;
                
                if (distSquared <= radiusSquared) {
                    nearby.add(wp);
                }
            }
        }
        
        for (Waypoint wp : serverWaypoints) {
            if (!wp.getWorld().equals(center.world())) continue;
            
            double dx = wp.getX() - center.x();
            double dy = wp.getY() - center.y();
            double dz = wp.getZ() - center.z();
            double distSquared = dx * dx + dy * dy + dz * dz;
            
            if (distSquared <= radiusSquared) {
                nearby.add(wp);
            }
        }
        
        return nearby;
    }
    
    public List<Waypoint> searchWaypoints(UUID playerId, String query) {
        String lowerQuery = query.toLowerCase();
        return getVisibleWaypoints(playerId).stream()
            .filter(wp -> wp.getName().toLowerCase().contains(lowerQuery) ||
                         (wp.getDescription() != null && wp.getDescription().toLowerCase().contains(lowerQuery)))
            .collect(Collectors.toList());
    }
    
    public void shareWaypoint(UUID waypointId, UUID targetPlayer) {
        findWaypoint(waypointId).ifPresent(wp -> {
            wp.getSharedWith().add(targetPlayer);
        });
    }
    
    public void unshareWaypoint(UUID waypointId, UUID targetPlayer) {
        findWaypoint(waypointId).ifPresent(wp -> {
            wp.getSharedWith().remove(targetPlayer);
        });
    }
    
    public void setVisibility(UUID waypointId, Waypoint.WaypointVisibility visibility) {
        findWaypoint(waypointId).ifPresent(wp -> {
            wp.setVisibility(visibility);
        });
    }
    
    public NavigationData getNavigation(UUID playerId, UUID waypointId) {
        return findWaypoint(waypointId)
            .map(wp -> NavigationData.calculate(getPlayerLocation(playerId), wp))
            .orElse(null);
    }
    
    public void setActiveWaypoint(UUID playerId, UUID waypointId) {
        activeWaypoints.put(playerId, waypointId);
    }
    
    public void clearActiveWaypoint(UUID playerId) {
        activeWaypoints.remove(playerId);
    }
    
    public Optional<UUID> getActiveWaypoint(UUID playerId) {
        return Optional.ofNullable(activeWaypoints.get(playerId));
    }
    
    public Waypoint createServerWaypoint(String name, Location location) {
        Waypoint waypoint = new Waypoint(UUID.randomUUID(), name, null);
        waypoint.setWorld(location.world());
        waypoint.setX(location.x());
        waypoint.setY(location.y());
        waypoint.setZ(location.z());
        waypoint.setCreatedAt(System.currentTimeMillis());
        waypoint.setVisibility(Waypoint.WaypointVisibility.SERVER);
        waypoint.setCategory(WaypointCategory.SPAWN);
        
        serverWaypoints.add(waypoint);
        
        logger.info("Created server waypoint '{}'", name);
        return waypoint;
    }
    
    public List<Waypoint> getServerWaypoints() {
        return new ArrayList<>(serverWaypoints);
    }
    
    public void onWaypointCreated(Consumer<Waypoint> listener) {
        createListeners.add(listener);
    }
    
    public void onWaypointDeleted(Consumer<Waypoint> listener) {
        deleteListeners.add(listener);
    }
    
    public void onWaypointUpdated(Consumer<Waypoint> listener) {
        updateListeners.add(listener);
    }
    
    private Optional<Waypoint> findWaypoint(UUID waypointId) {
        for (List<Waypoint> waypoints : playerWaypoints.values()) {
            for (Waypoint wp : waypoints) {
                if (wp.getId().equals(waypointId)) {
                    return Optional.of(wp);
                }
            }
        }
        
        return serverWaypoints.stream()
            .filter(wp -> wp.getId().equals(waypointId))
            .findFirst();
    }
    
    private Location getPlayerLocation(UUID playerId) {
        return new Location("world", 0, 0, 0);
    }
    
    public void setConfig(WaypointConfig config) {
        this.config = config;
    }
    
    public WaypointConfig getConfig() {
        return config;
    }
    
    public record Location(String world, double x, double y, double z) {
        public double distance(Location other) {
            if (!world.equals(other.world)) return -1;
            double dx = x - other.x;
            double dy = y - other.y;
            double dz = z - other.z;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }
    
    public record WaypointUpdate(
        String name,
        String description,
        WaypointCategory category,
        Integer color,
        WaypointIcon icon,
        Boolean showBeam,
        Boolean showDistance,
        Waypoint.WaypointVisibility visibility
    ) {
        public static WaypointUpdate name(String name) {
            return new WaypointUpdate(name, null, null, null, null, null, null, null);
        }
        
        public static WaypointUpdate description(String description) {
            return new WaypointUpdate(null, description, null, null, null, null, null, null);
        }
    }
}
