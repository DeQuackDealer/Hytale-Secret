package com.yellowtale.rubidium.waypoints;

import java.util.*;

public class Waypoint {
    private final UUID id;
    private String name;
    private String description;
    
    private String world;
    private double x;
    private double y;
    private double z;
    
    private final UUID owner;
    private WaypointVisibility visibility;
    private final Set<UUID> sharedWith;
    
    private WaypointCategory category;
    private int color;
    private WaypointIcon icon;
    private boolean showBeam;
    private boolean showDistance;
    
    private long createdAt;
    private long lastVisited;
    private int visitCount;
    private final Map<String, String> tags;
    
    public Waypoint(UUID id, String name, UUID owner) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.description = null;
        this.world = "world";
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.visibility = WaypointVisibility.PRIVATE;
        this.sharedWith = new HashSet<>();
        this.category = WaypointCategory.CUSTOM;
        this.color = 0xFFFFFF;
        this.icon = WaypointIcon.MARKER;
        this.showBeam = true;
        this.showDistance = true;
        this.createdAt = System.currentTimeMillis();
        this.lastVisited = 0;
        this.visitCount = 0;
        this.tags = new HashMap<>();
    }
    
    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getWorld() { return world; }
    public void setWorld(String world) { this.world = world; }
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    public double getZ() { return z; }
    public void setZ(double z) { this.z = z; }
    
    public UUID getOwner() { return owner; }
    public WaypointVisibility getVisibility() { return visibility; }
    public void setVisibility(WaypointVisibility visibility) { this.visibility = visibility; }
    public Set<UUID> getSharedWith() { return sharedWith; }
    
    public WaypointCategory getCategory() { return category; }
    public void setCategory(WaypointCategory category) { 
        this.category = category;
        if (category != null && this.color == 0xFFFFFF) {
            this.color = category.getColor();
        }
        if (category != null && this.icon == WaypointIcon.MARKER) {
            this.icon = category.getDefaultIcon();
        }
    }
    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
    public WaypointIcon getIcon() { return icon; }
    public void setIcon(WaypointIcon icon) { this.icon = icon; }
    public boolean isShowBeam() { return showBeam; }
    public void setShowBeam(boolean showBeam) { this.showBeam = showBeam; }
    public boolean isShowDistance() { return showDistance; }
    public void setShowDistance(boolean showDistance) { this.showDistance = showDistance; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getLastVisited() { return lastVisited; }
    public void setLastVisited(long lastVisited) { this.lastVisited = lastVisited; }
    public int getVisitCount() { return visitCount; }
    public void incrementVisitCount() { this.visitCount++; this.lastVisited = System.currentTimeMillis(); }
    public Map<String, String> getTags() { return tags; }
    
    public void setLocation(String world, double x, double y, double z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public WaypointManager.Location getLocation() {
        return new WaypointManager.Location(world, x, y, z);
    }
    
    public double distanceTo(double px, double py, double pz) {
        double dx = x - px;
        double dy = y - py;
        double dz = z - pz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    public double horizontalDistanceTo(double px, double pz) {
        double dx = x - px;
        double dz = z - pz;
        return Math.sqrt(dx * dx + dz * dz);
    }
    
    public boolean isOwnedBy(UUID playerId) {
        return owner != null && owner.equals(playerId);
    }
    
    public boolean isVisibleTo(UUID playerId) {
        if (visibility == WaypointVisibility.PUBLIC || visibility == WaypointVisibility.SERVER) {
            return true;
        }
        if (isOwnedBy(playerId)) {
            return true;
        }
        if (sharedWith.contains(playerId)) {
            return true;
        }
        return false;
    }
    
    public enum WaypointVisibility {
        PRIVATE,
        PARTY,
        TEAM,
        FACTION,
        PUBLIC,
        SERVER
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String name;
        private String description;
        private String world = "world";
        private double x, y, z;
        private UUID owner;
        private WaypointVisibility visibility = WaypointVisibility.PRIVATE;
        private WaypointCategory category = WaypointCategory.CUSTOM;
        private WaypointIcon icon = WaypointIcon.MARKER;
        private int color = 0xFFFFFF;
        private boolean showBeam = true;
        private boolean showDistance = true;
        
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder location(String world, double x, double y, double z) { 
            this.world = world; this.x = x; this.y = y; this.z = z; return this; 
        }
        public Builder location(WaypointManager.Location loc) {
            return location(loc.world(), loc.x(), loc.y(), loc.z());
        }
        public Builder owner(UUID owner) { this.owner = owner; return this; }
        public Builder visibility(WaypointVisibility visibility) { this.visibility = visibility; return this; }
        public Builder category(WaypointCategory category) { this.category = category; return this; }
        public Builder icon(WaypointIcon icon) { this.icon = icon; return this; }
        public Builder color(int color) { this.color = color; return this; }
        public Builder showBeam(boolean showBeam) { this.showBeam = showBeam; return this; }
        public Builder showDistance(boolean showDistance) { this.showDistance = showDistance; return this; }
        
        public Waypoint build() {
            Waypoint wp = new Waypoint(UUID.randomUUID(), name, owner);
            wp.setDescription(description);
            wp.setWorld(world);
            wp.setX(x);
            wp.setY(y);
            wp.setZ(z);
            wp.setVisibility(visibility);
            wp.setCategory(category);
            wp.setIcon(icon);
            wp.setColor(color);
            wp.setShowBeam(showBeam);
            wp.setShowDistance(showDistance);
            return wp;
        }
    }
}
