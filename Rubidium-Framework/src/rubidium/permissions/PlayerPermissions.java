package rubidium.permissions;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player permission data.
 */
public class PlayerPermissions {
    
    private final UUID playerId;
    private final Set<String> groups;
    private final Map<PermissionEntry, Boolean> permissions;
    private final Map<String, TemporaryPermission> temporaryPermissions;
    private String primaryGroup;
    private String prefix;
    private String suffix;
    
    public PlayerPermissions(UUID playerId) {
        this.playerId = playerId;
        this.groups = ConcurrentHashMap.newKeySet();
        this.permissions = new ConcurrentHashMap<>();
        this.temporaryPermissions = new ConcurrentHashMap<>();
        this.primaryGroup = "default";
    }
    
    public UUID getPlayerId() { return playerId; }
    
    public Set<String> getGroups() { return groups; }
    
    public void addGroup(String groupId) {
        groups.add(groupId);
    }
    
    public void removeGroup(String groupId) {
        groups.remove(groupId);
    }
    
    public boolean inGroup(String groupId) {
        return groups.contains(groupId);
    }
    
    public String getPrimaryGroup() { return primaryGroup; }
    
    public void setPrimaryGroup(String group) {
        this.primaryGroup = group;
    }
    
    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    
    public String getSuffix() { return suffix; }
    public void setSuffix(String suffix) { this.suffix = suffix; }
    
    public void setPermission(String permission, boolean value, PermissionContext context) {
        permissions.put(new PermissionEntry(permission, context), value);
    }
    
    public void unsetPermission(String permission, PermissionContext context) {
        permissions.remove(new PermissionEntry(permission, context));
    }
    
    public boolean hasExplicitPermission(String permission, PermissionContext context) {
        cleanExpiredTemporary();
        
        if (temporaryPermissions.containsKey(permission)) {
            return true;
        }
        
        return permissions.containsKey(new PermissionEntry(permission, context)) ||
               permissions.containsKey(new PermissionEntry(permission, PermissionContext.empty()));
    }
    
    public boolean getExplicitPermission(String permission, PermissionContext context) {
        cleanExpiredTemporary();
        
        TemporaryPermission temp = temporaryPermissions.get(permission);
        if (temp != null) {
            return temp.value();
        }
        
        Boolean value = permissions.get(new PermissionEntry(permission, context));
        if (value != null) return value;
        
        value = permissions.get(new PermissionEntry(permission, PermissionContext.empty()));
        return value != null && value;
    }
    
    public void setTemporaryPermission(String permission, boolean value, long durationMs) {
        long expiry = System.currentTimeMillis() + durationMs;
        temporaryPermissions.put(permission, new TemporaryPermission(permission, value, expiry));
    }
    
    public void removeTemporaryPermission(String permission) {
        temporaryPermissions.remove(permission);
    }
    
    private void cleanExpiredTemporary() {
        long now = System.currentTimeMillis();
        temporaryPermissions.entrySet().removeIf(e -> e.getValue().expiry() < now);
    }
    
    public record TemporaryPermission(String permission, boolean value, long expiry) {}
}
