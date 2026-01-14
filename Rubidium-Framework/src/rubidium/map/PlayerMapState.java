package rubidium.map;

import java.util.UUID;

/**
 * Player-specific map state.
 */
public class PlayerMapState {
    
    private final UUID playerId;
    private double x, y, z;
    private float yaw;
    private boolean visible;
    private boolean minimapEnabled;
    private int zoomLevel;
    private MapMode mode;
    
    public PlayerMapState(UUID playerId) {
        this.playerId = playerId;
        this.visible = true;
        this.minimapEnabled = true;
        this.zoomLevel = 1;
        this.mode = MapMode.MINIMAP;
    }
    
    public UUID getPlayerId() { return playerId; }
    
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    
    public void updatePosition(double x, double y, double z, float yaw) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
    }
    
    public void updateFromLocation() {
    }
    
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    
    public boolean isMinimapEnabled() { return minimapEnabled; }
    public void setMinimapEnabled(boolean enabled) { this.minimapEnabled = enabled; }
    
    public int getZoomLevel() { return zoomLevel; }
    public void setZoomLevel(int level) { this.zoomLevel = Math.max(1, Math.min(5, level)); }
    
    public MapMode getMode() { return mode; }
    public void setMode(MapMode mode) { this.mode = mode; }
    
    public enum MapMode {
        MINIMAP,
        FULLSCREEN,
        HIDDEN
    }
}
