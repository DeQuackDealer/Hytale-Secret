package rubidium.map;

import java.time.Instant;
import java.util.*;

/**
 * Waypoint marker on the map.
 */
public class Waypoint {
    
    private final String id;
    private final UUID ownerId;
    private String name;
    private String description;
    private double x, y, z;
    private String world;
    private WaypointIcon icon;
    private String color;
    private boolean visible;
    private boolean isPublic;
    private final Set<UUID> sharedWith;
    private final Instant createdAt;
    
    public Waypoint(String id, UUID ownerId, String name, double x, double y, double z, String world) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = world;
        this.icon = WaypointIcon.DEFAULT;
        this.color = "#FFFFFF";
        this.visible = true;
        this.isPublic = false;
        this.sharedWith = new HashSet<>();
        this.createdAt = Instant.now();
    }
    
    public String getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    
    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public String getWorld() { return world; }
    public void setWorld(String world) { this.world = world; }
    
    public WaypointIcon getIcon() { return icon; }
    public void setIcon(WaypointIcon icon) { this.icon = icon; }
    
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
    
    public Set<UUID> getSharedWith() { return Collections.unmodifiableSet(sharedWith); }
    
    public void shareWith(UUID playerId) { sharedWith.add(playerId); }
    public void unshareWith(UUID playerId) { sharedWith.remove(playerId); }
    
    public boolean canView(UUID playerId) {
        return isPublic || ownerId.equals(playerId) || sharedWith.contains(playerId);
    }
    
    public Instant getCreatedAt() { return createdAt; }
    
    public double distanceTo(double px, double py, double pz) {
        double dx = x - px;
        double dy = y - py;
        double dz = z - pz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    public double distance2DTo(double px, double pz) {
        double dx = x - px;
        double dz = z - pz;
        return Math.sqrt(dx * dx + dz * dz);
    }
    
    public enum WaypointIcon {
        DEFAULT,
        HOME,
        SPAWN,
        DEATH,
        QUEST,
        SHOP,
        DUNGEON,
        BOSS,
        PORTAL,
        FRIEND,
        PARTY,
        CUSTOM
    }
}
