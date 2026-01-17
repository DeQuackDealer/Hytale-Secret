package rubidium.settings;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerSettings {
    
    private boolean optimizationsEnabled = true;
    private boolean voiceChatAllowed = true;
    private boolean minimapAllowed = true;
    private boolean statisticsAllowed = true;
    private boolean waypointsAllowed = true;
    
    private float voiceChatProximityRadius = 50.0f;
    private int maxWaypointsPerPlayer = 50;
    
    private final Set<UUID> owners = ConcurrentHashMap.newKeySet();
    private final Set<UUID> admins = ConcurrentHashMap.newKeySet();
    
    public ServerSettings() {
    }
    
    public boolean isOptimizationsEnabled() { return optimizationsEnabled; }
    public void setOptimizationsEnabled(boolean enabled) { this.optimizationsEnabled = enabled; }
    
    public boolean isVoiceChatAllowed() { return voiceChatAllowed; }
    public void setVoiceChatAllowed(boolean allowed) { this.voiceChatAllowed = allowed; }
    
    public boolean isMinimapAllowed() { return minimapAllowed; }
    public void setMinimapAllowed(boolean allowed) { this.minimapAllowed = allowed; }
    
    public boolean isStatisticsAllowed() { return statisticsAllowed; }
    public void setStatisticsAllowed(boolean allowed) { this.statisticsAllowed = allowed; }
    
    public boolean isWaypointsAllowed() { return waypointsAllowed; }
    public void setWaypointsAllowed(boolean allowed) { this.waypointsAllowed = allowed; }
    
    public float getVoiceChatProximityRadius() { return voiceChatProximityRadius; }
    public void setVoiceChatProximityRadius(float radius) { this.voiceChatProximityRadius = radius; }
    
    public int getMaxWaypointsPerPlayer() { return maxWaypointsPerPlayer; }
    public void setMaxWaypointsPerPlayer(int max) { this.maxWaypointsPerPlayer = max; }
    
    public void addOwner(UUID playerId) {
        owners.add(playerId);
        admins.add(playerId);
    }
    
    public void removeOwner(UUID playerId) {
        owners.remove(playerId);
    }
    
    public boolean isOwner(UUID playerId) {
        return owners.contains(playerId);
    }
    
    public void addAdmin(UUID playerId) {
        admins.add(playerId);
    }
    
    public void removeAdmin(UUID playerId) {
        if (!owners.contains(playerId)) {
            admins.remove(playerId);
        }
    }
    
    public boolean isAdmin(UUID playerId) {
        return admins.contains(playerId);
    }
    
    public Set<UUID> getOwners() {
        return Collections.unmodifiableSet(owners);
    }
    
    public Set<UUID> getAdmins() {
        return Collections.unmodifiableSet(admins);
    }
    
    public SettingsRegistry.PermissionLevel getPermissionLevel(UUID playerId) {
        if (owners.contains(playerId)) return SettingsRegistry.PermissionLevel.OWNER;
        if (admins.contains(playerId)) return SettingsRegistry.PermissionLevel.ADMIN;
        return SettingsRegistry.PermissionLevel.PLAYER;
    }
    
    public void save() {
        System.out.println("[Rubidium] Saving server settings...");
    }
    
    public void load() {
        System.out.println("[Rubidium] Loading server settings...");
    }
}
