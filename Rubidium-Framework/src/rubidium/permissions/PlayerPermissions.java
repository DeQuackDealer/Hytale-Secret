package rubidium.permissions;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerPermissions {
    private final UUID playerId;
    private final Set<Role> roles;
    private final Map<String, PermissionGrant> directPermissions;
    private Role primaryRole;
    private String customPrefix;
    private String customSuffix;
    
    public PlayerPermissions(UUID playerId) {
        this.playerId = playerId;
        this.roles = ConcurrentHashMap.newKeySet();
        this.directPermissions = new ConcurrentHashMap<>();
        this.primaryRole = null;
        this.customPrefix = null;
        this.customSuffix = null;
    }
    
    public UUID getPlayerId() { return playerId; }
    public Set<Role> getRoles() { return Collections.unmodifiableSet(roles); }
    public Map<String, PermissionGrant> getDirectPermissions() { return Collections.unmodifiableMap(directPermissions); }
    public Role getPrimaryRole() { return primaryRole; }
    public void setPrimaryRole(Role primaryRole) { this.primaryRole = primaryRole; }
    public String getCustomPrefix() { return customPrefix; }
    public void setCustomPrefix(String customPrefix) { this.customPrefix = customPrefix; }
    public String getCustomSuffix() { return customSuffix; }
    public void setCustomSuffix(String customSuffix) { this.customSuffix = customSuffix; }
    
    public void addRole(Role role) {
        roles.add(role);
        if (primaryRole == null || role.getPriority() > primaryRole.getPriority()) {
            primaryRole = role;
        }
    }
    
    public void removeRole(Role role) {
        roles.remove(role);
        if (role.equals(primaryRole)) {
            primaryRole = roles.stream()
                .max(Comparator.comparingInt(Role::getPriority))
                .orElse(null);
        }
    }
    
    public boolean hasRole(Role role) {
        return roles.contains(role);
    }
    
    public void setDirectPermission(String permission, PermissionGrant grant) {
        directPermissions.put(permission, grant);
    }
    
    public void removeDirectPermission(String permission) {
        directPermissions.remove(permission);
    }
    
    public PermissionGrant getDirectPermission(String permission) {
        PermissionGrant grant = directPermissions.get(permission);
        if (grant != null && grant.expiresAt() != null && grant.expiresAt() < System.currentTimeMillis()) {
            directPermissions.remove(permission);
            return null;
        }
        return grant;
    }
    
    public boolean hasDirectPermission(String permission) {
        return getDirectPermission(permission) != null;
    }
    
    public void cleanupExpiredPermissions() {
        long now = System.currentTimeMillis();
        directPermissions.entrySet().removeIf(entry -> {
            PermissionGrant grant = entry.getValue();
            return grant.expiresAt() != null && grant.expiresAt() < now;
        });
    }
}
