package rubidium.map;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Manages player waypoints.
 */
public class WaypointManager {
    
    private static final Logger logger = Logger.getLogger("Rubidium-Waypoints");
    
    private final Map<UUID, Map<String, Waypoint>> playerWaypoints = new ConcurrentHashMap<>();
    private final Map<String, Waypoint> publicWaypoints = new ConcurrentHashMap<>();
    
    private int maxWaypointsPerPlayer = 50;
    
    public void loadWaypoints() {
        logger.info("Loading waypoints...");
    }
    
    public void saveWaypoints() {
        logger.info("Saving waypoints...");
    }
    
    public Waypoint createWaypoint(UUID ownerId, String name, double x, double y, double z, String world) {
        Map<String, Waypoint> waypoints = playerWaypoints.computeIfAbsent(ownerId, k -> new ConcurrentHashMap<>());
        
        if (waypoints.size() >= maxWaypointsPerPlayer) {
            throw new IllegalStateException("Maximum waypoint limit reached");
        }
        
        String id = UUID.randomUUID().toString().substring(0, 8);
        Waypoint waypoint = new Waypoint(id, ownerId, name, x, y, z, world);
        waypoints.put(id, waypoint);
        
        logger.fine("Created waypoint: " + name + " for " + ownerId);
        return waypoint;
    }
    
    public void deleteWaypoint(UUID ownerId, String waypointId) {
        Map<String, Waypoint> waypoints = playerWaypoints.get(ownerId);
        if (waypoints != null) {
            Waypoint removed = waypoints.remove(waypointId);
            if (removed != null && removed.isPublic()) {
                publicWaypoints.remove(waypointId);
            }
        }
    }
    
    public Optional<Waypoint> getWaypoint(UUID ownerId, String waypointId) {
        Map<String, Waypoint> waypoints = playerWaypoints.get(ownerId);
        return waypoints != null ? Optional.ofNullable(waypoints.get(waypointId)) : Optional.empty();
    }
    
    public List<Waypoint> getPlayerWaypoints(UUID playerId) {
        Map<String, Waypoint> waypoints = playerWaypoints.get(playerId);
        if (waypoints == null) return List.of();
        return new ArrayList<>(waypoints.values());
    }
    
    public List<Waypoint> getPublicWaypoints() {
        return new ArrayList<>(publicWaypoints.values());
    }
    
    public List<Waypoint> getVisibleWaypoints(UUID playerId) {
        List<Waypoint> visible = new ArrayList<>(getPlayerWaypoints(playerId));
        
        for (Waypoint wp : publicWaypoints.values()) {
            if (!wp.getOwnerId().equals(playerId)) {
                visible.add(wp);
            }
        }
        
        for (Map<String, Waypoint> otherWaypoints : playerWaypoints.values()) {
            for (Waypoint wp : otherWaypoints.values()) {
                if (wp.getSharedWith().contains(playerId)) {
                    visible.add(wp);
                }
            }
        }
        
        return visible;
    }
    
    public void shareWaypoint(String waypointId, UUID targetPlayerId) {
        for (Map<String, Waypoint> waypoints : playerWaypoints.values()) {
            Waypoint wp = waypoints.get(waypointId);
            if (wp != null) {
                wp.shareWith(targetPlayerId);
                return;
            }
        }
    }
    
    public void makePublic(UUID ownerId, String waypointId) {
        Map<String, Waypoint> waypoints = playerWaypoints.get(ownerId);
        if (waypoints != null) {
            Waypoint wp = waypoints.get(waypointId);
            if (wp != null) {
                wp.setPublic(true);
                publicWaypoints.put(waypointId, wp);
            }
        }
    }
    
    public void makePrivate(UUID ownerId, String waypointId) {
        Map<String, Waypoint> waypoints = playerWaypoints.get(ownerId);
        if (waypoints != null) {
            Waypoint wp = waypoints.get(waypointId);
            if (wp != null) {
                wp.setPublic(false);
                publicWaypoints.remove(waypointId);
            }
        }
    }
    
    public List<Waypoint> searchWaypoints(UUID playerId, String query) {
        String lowerQuery = query.toLowerCase();
        return getVisibleWaypoints(playerId).stream()
            .filter(wp -> wp.getName().toLowerCase().contains(lowerQuery))
            .collect(Collectors.toList());
    }
    
    public List<Waypoint> getNearbyWaypoints(UUID playerId, double x, double z, double radius) {
        return getVisibleWaypoints(playerId).stream()
            .filter(wp -> wp.distance2DTo(x, z) <= radius)
            .sorted(Comparator.comparingDouble(wp -> wp.distance2DTo(x, z)))
            .collect(Collectors.toList());
    }
    
    public int getTotalWaypointCount() {
        int count = 0;
        for (Map<String, Waypoint> waypoints : playerWaypoints.values()) {
            count += waypoints.size();
        }
        return count;
    }
    
    public void setMaxWaypointsPerPlayer(int max) {
        this.maxWaypointsPerPlayer = max;
    }
}
