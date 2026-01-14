package rubidium.map;

import rubidium.hytale.api.player.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Minimap {
    
    private final Map<UUID, MinimapState> playerStates = new ConcurrentHashMap<>();
    private final WaypointManager waypointManager;
    private final ZoneManager zoneManager;
    
    private MinimapConfig config;
    
    public Minimap(WaypointManager waypointManager, ZoneManager zoneManager) {
        this.waypointManager = waypointManager;
        this.zoneManager = zoneManager;
        this.config = MinimapConfig.defaults();
    }
    
    public void onPlayerJoin(Player player) {
        MinimapState state = new MinimapState(player.getUuid());
        playerStates.put(player.getUuid(), state);
        
        sendMinimapUpdate(player);
    }
    
    public void onPlayerQuit(UUID playerId) {
        playerStates.remove(playerId);
    }
    
    public void onPlayerMove(Player player) {
        MinimapState state = playerStates.get(player.getUuid());
        if (state == null) return;
        
        var loc = player.getLocation();
        state.updatePosition(loc.getX(), loc.getY(), loc.getZ());
        state.updateRotation(loc.getYaw());
        
        if (state.shouldUpdate()) {
            sendMinimapUpdate(player);
            state.markUpdated();
        }
    }
    
    public void setMinimapEnabled(UUID playerId, boolean enabled) {
        MinimapState state = playerStates.get(playerId);
        if (state != null) {
            state.setEnabled(enabled);
        }
    }
    
    public void setZoomLevel(UUID playerId, float zoom) {
        MinimapState state = playerStates.get(playerId);
        if (state != null) {
            state.setZoom(Math.max(0.5f, Math.min(4.0f, zoom)));
        }
    }
    
    public void setRotationLocked(UUID playerId, boolean locked) {
        MinimapState state = playerStates.get(playerId);
        if (state != null) {
            state.setRotationLocked(locked);
        }
    }
    
    public void toggleWaypointVisibility(UUID playerId, String waypointId, boolean visible) {
        MinimapState state = playerStates.get(playerId);
        if (state != null) {
            if (visible) {
                state.showWaypoint(waypointId);
            } else {
                state.hideWaypoint(waypointId);
            }
        }
    }
    
    private void sendMinimapUpdate(Player player) {
        MinimapState state = playerStates.get(player.getUuid());
        if (state == null || !state.isEnabled()) return;
        
        List<Waypoint> visibleWaypoints = getVisibleWaypoints(player);
        List<MinimapEntity> nearbyEntities = getNearbyEntities(player, state.getZoom());
        List<Zone> zones = zoneManager.getZonesAt(
            state.getX(), state.getY(), state.getZ(), player.getWorld().getName()
        );
        
        MinimapUpdatePacket packet = new MinimapUpdatePacket(
            state.getX(),
            state.getY(),
            state.getZ(),
            state.getRotation(),
            state.getZoom(),
            state.isRotationLocked(),
            visibleWaypoints,
            nearbyEntities,
            zones
        );
        
        player.sendPacket(packet);
    }
    
    private List<Waypoint> getVisibleWaypoints(Player player) {
        List<Waypoint> all = new ArrayList<>();
        all.addAll(waypointManager.getPlayerWaypoints(player.getUuid()));
        all.addAll(waypointManager.getPublicWaypoints());
        
        MinimapState state = playerStates.get(player.getUuid());
        if (state == null) return all;
        
        return all.stream()
            .filter(w -> state.isWaypointVisible(w.getId()))
            .toList();
    }
    
    private List<MinimapEntity> getNearbyEntities(Player player, float zoom) {
        return Collections.emptyList();
    }
    
    public void setConfig(MinimapConfig config) {
        this.config = config;
    }
    
    public MinimapConfig getConfig() {
        return config;
    }
    
    public record MinimapConfig(
        int size,
        float defaultZoom,
        boolean showPlayerNames,
        boolean showMobIcons,
        boolean showWaypoints,
        boolean showZoneBorders,
        boolean showCoordinates,
        boolean showBiome,
        int updateIntervalMs,
        float iconScale
    ) {
        public static MinimapConfig defaults() {
            return new MinimapConfig(
                128,
                1.0f,
                true,
                true,
                true,
                true,
                true,
                true,
                100,
                1.0f
            );
        }
    }
    
    public record MinimapUpdatePacket(
        double x, double y, double z,
        float rotation,
        float zoom,
        boolean rotationLocked,
        List<Waypoint> waypoints,
        List<MinimapEntity> entities,
        List<Zone> zones
    ) {}
    
    public record MinimapEntity(
        UUID id,
        String type,
        double x, double z,
        String name,
        String icon
    ) {}
    
    public static class MinimapState {
        private final UUID playerId;
        private double x, y, z;
        private float rotation;
        private float zoom = 1.0f;
        private boolean enabled = true;
        private boolean rotationLocked = false;
        private long lastUpdate = 0;
        private final Set<String> hiddenWaypoints = new HashSet<>();
        
        public MinimapState(UUID playerId) {
            this.playerId = playerId;
        }
        
        public UUID getPlayerId() { return playerId; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public float getRotation() { return rotation; }
        public float getZoom() { return zoom; }
        public boolean isEnabled() { return enabled; }
        public boolean isRotationLocked() { return rotationLocked; }
        
        public void updatePosition(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        public void updateRotation(float rotation) {
            this.rotation = rotation;
        }
        
        public void setZoom(float zoom) { this.zoom = zoom; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public void setRotationLocked(boolean locked) { this.rotationLocked = locked; }
        
        public void showWaypoint(String id) { hiddenWaypoints.remove(id); }
        public void hideWaypoint(String id) { hiddenWaypoints.add(id); }
        public boolean isWaypointVisible(String id) { return !hiddenWaypoints.contains(id); }
        
        public boolean shouldUpdate() {
            return System.currentTimeMillis() - lastUpdate > 100;
        }
        
        public void markUpdated() {
            lastUpdate = System.currentTimeMillis();
        }
    }
}
