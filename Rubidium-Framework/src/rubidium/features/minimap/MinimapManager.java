package rubidium.features.minimap;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MinimapManager {
    
    private static final Map<UUID, MinimapManager> instances = new ConcurrentHashMap<>();
    
    private final Map<UUID, Map<String, Waypoint>> playerWaypoints = new ConcurrentHashMap<>();
    private final Map<UUID, MinimapSettings> playerSettings = new ConcurrentHashMap<>();
    
    public static MinimapManager getOrCreate(UUID playerId) {
        return instances.computeIfAbsent(playerId, k -> new MinimapManager());
    }
    
    public static void remove(UUID playerId) {
        MinimapManager manager = instances.remove(playerId);
        if (manager != null) {
            manager.cleanupPlayer(playerId);
        }
    }
    
    public void addWaypoint(UUID playerId, Waypoint waypoint) {
        playerWaypoints.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .put(waypoint.getId(), waypoint);
    }
    
    public void removeWaypoint(UUID playerId, String waypointId) {
        Map<String, Waypoint> waypoints = playerWaypoints.get(playerId);
        if (waypoints != null) {
            waypoints.remove(waypointId);
        }
    }
    
    public Waypoint getWaypoint(UUID playerId, String waypointId) {
        Map<String, Waypoint> waypoints = playerWaypoints.get(playerId);
        return waypoints != null ? waypoints.get(waypointId) : null;
    }
    
    public Map<String, Waypoint> getWaypoints(UUID playerId) {
        return playerWaypoints.getOrDefault(playerId, Map.of());
    }
    
    public void setZoom(UUID playerId, float zoom) {
        getSettings(playerId).setZoom(zoom);
    }
    
    public float getZoom(UUID playerId) {
        return getSettings(playerId).getZoom();
    }
    
    public void setShowPlayers(UUID playerId, boolean show) {
        getSettings(playerId).setShowPlayers(show);
    }
    
    public boolean isShowingPlayers(UUID playerId) {
        return getSettings(playerId).isShowPlayers();
    }
    
    public void setShowWaypoints(UUID playerId, boolean show) {
        getSettings(playerId).setShowWaypoints(show);
    }
    
    public boolean isShowingWaypoints(UUID playerId) {
        return getSettings(playerId).isShowWaypoints();
    }
    
    private MinimapSettings getSettings(UUID playerId) {
        return playerSettings.computeIfAbsent(playerId, k -> new MinimapSettings());
    }
    
    public void cleanupPlayer(UUID playerId) {
        playerWaypoints.remove(playerId);
        playerSettings.remove(playerId);
    }
    
    public static class MinimapSettings {
        private float zoom = 1.0f;
        private boolean showPlayers = true;
        private boolean showWaypoints = true;
        
        public float getZoom() {
            return zoom;
        }
        
        public void setZoom(float zoom) {
            this.zoom = zoom;
        }
        
        public boolean isShowPlayers() {
            return showPlayers;
        }
        
        public void setShowPlayers(boolean showPlayers) {
            this.showPlayers = showPlayers;
        }
        
        public boolean isShowWaypoints() {
            return showWaypoints;
        }
        
        public void setShowWaypoints(boolean showWaypoints) {
            this.showWaypoints = showWaypoints;
        }
    }
}
