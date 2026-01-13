package com.yellowtale.rubidium.permissions;

import com.yellowtale.rubidium.core.logging.RubidiumLogger;

import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class PermissionManager {
    
    private final RubidiumLogger logger;
    private final Path dataDir;
    
    private final Map<String, Role> roles;
    private final Map<UUID, PlayerPermissions> playerPermissions;
    private final Map<UUID, PermissionCache> permissionCache;
    private final Map<String, Permission> permissionRegistry;
    
    private final List<BiConsumer<UUID, Role>> roleAddListeners;
    private final List<BiConsumer<UUID, Role>> roleRemoveListeners;
    
    private Duration cacheExpiry = Duration.ofMinutes(5);
    
    public PermissionManager(RubidiumLogger logger, Path dataDir) {
        this.logger = logger;
        this.dataDir = dataDir;
        this.roles = new ConcurrentHashMap<>();
        this.playerPermissions = new ConcurrentHashMap<>();
        this.permissionCache = new ConcurrentHashMap<>();
        this.permissionRegistry = new ConcurrentHashMap<>();
        this.roleAddListeners = new ArrayList<>();
        this.roleRemoveListeners = new ArrayList<>();
        
        registerDefaultRoles();
    }
    
    private void registerDefaultRoles() {
        registerRole(Role.DEFAULT);
        registerRole(Role.VIP);
        registerRole(Role.MODERATOR);
        registerRole(Role.ADMIN);
    }
    
    public void registerRole(Role role) {
        roles.put(role.getId(), role);
        logger.debug("Registered role: {}", role.getName());
    }
    
    public void unregisterRole(String roleId) {
        Role role = roles.remove(roleId);
        if (role != null) {
            for (PlayerPermissions pp : playerPermissions.values()) {
                pp.removeRole(role);
            }
            invalidateAllCaches();
        }
    }
    
    public Optional<Role> getRole(String roleId) {
        return Optional.ofNullable(roles.get(roleId));
    }
    
    public List<Role> getAllRoles() {
        return new ArrayList<>(roles.values());
    }
    
    public Role getDefaultRole() {
        return roles.values().stream()
            .filter(Role::isDefaultRole)
            .findFirst()
            .orElse(Role.DEFAULT);
    }
    
    public void addRole(UUID playerId, Role role) {
        PlayerPermissions pp = getOrCreatePlayerPermissions(playerId);
        pp.addRole(role);
        invalidateCache(playerId);
        
        for (BiConsumer<UUID, Role> listener : roleAddListeners) {
            listener.accept(playerId, role);
        }
    }
    
    public void removeRole(UUID playerId, Role role) {
        PlayerPermissions pp = playerPermissions.get(playerId);
        if (pp != null) {
            pp.removeRole(role);
            invalidateCache(playerId);
            
            for (BiConsumer<UUID, Role> listener : roleRemoveListeners) {
                listener.accept(playerId, role);
            }
        }
    }
    
    public void setPrimaryRole(UUID playerId, Role role) {
        PlayerPermissions pp = getOrCreatePlayerPermissions(playerId);
        pp.setPrimaryRole(role);
        if (!pp.hasRole(role)) {
            pp.addRole(role);
        }
        invalidateCache(playerId);
    }
    
    public Set<Role> getPlayerRoles(UUID playerId) {
        PlayerPermissions pp = playerPermissions.get(playerId);
        if (pp == null) {
            return Set.of(getDefaultRole());
        }
        Set<Role> result = new HashSet<>(pp.getRoles());
        if (result.isEmpty()) {
            result.add(getDefaultRole());
        }
        return result;
    }
    
    public Role getPrimaryRole(UUID playerId) {
        PlayerPermissions pp = playerPermissions.get(playerId);
        if (pp != null && pp.getPrimaryRole() != null) {
            return pp.getPrimaryRole();
        }
        return getPlayerRoles(playerId).stream()
            .max(Comparator.comparingInt(Role::getPriority))
            .orElse(getDefaultRole());
    }
    
    public boolean hasPermission(UUID playerId, String permission) {
        return hasPermission(playerId, permission, PermissionContext.global());
    }
    
    public boolean hasPermission(UUID playerId, String permission, PermissionContext context) {
        PermissionCache cache = permissionCache.get(playerId);
        if (cache != null && !cache.isExpired(cacheExpiry)) {
            PermissionValue cached = cache.get(permission);
            if (cached != null) {
                return cached == PermissionValue.TRUE;
            }
        }
        
        PermissionValue value = resolve(playerId, permission, context);
        
        if (cache == null) {
            cache = new PermissionCache();
            permissionCache.put(playerId, cache);
        }
        cache.put(permission, value);
        
        return value == PermissionValue.TRUE;
    }
    
    public PermissionValue getPermissionValue(UUID playerId, String permission) {
        return resolve(playerId, permission, PermissionContext.global());
    }
    
    public void setPermission(UUID playerId, String permission, boolean value) {
        setPermission(playerId, permission, value, null);
    }
    
    public void setPermission(UUID playerId, String permission, boolean value, Duration duration) {
        PlayerPermissions pp = getOrCreatePlayerPermissions(playerId);
        Long expiresAt = duration != null ? System.currentTimeMillis() + duration.toMillis() : null;
        
        pp.setDirectPermission(permission, new PermissionGrant(
            permission,
            value ? PermissionValue.TRUE : PermissionValue.FALSE,
            PermissionContext.global(),
            System.currentTimeMillis(),
            expiresAt,
            null,
            null
        ));
        
        invalidateCache(playerId);
    }
    
    public void unsetPermission(UUID playerId, String permission) {
        PlayerPermissions pp = playerPermissions.get(playerId);
        if (pp != null) {
            pp.removeDirectPermission(permission);
            invalidateCache(playerId);
        }
    }
    
    public Map<String, PermissionGrant> getDirectPermissions(UUID playerId) {
        PlayerPermissions pp = playerPermissions.get(playerId);
        return pp != null ? pp.getDirectPermissions() : Collections.emptyMap();
    }
    
    public void setRolePermission(String roleId, String permission, boolean value) {
        Role role = roles.get(roleId);
        if (role != null) {
            role.setPermission(permission, value ? PermissionValue.TRUE : PermissionValue.FALSE);
            invalidateAllCaches();
        }
    }
    
    public void unsetRolePermission(String roleId, String permission) {
        Role role = roles.get(roleId);
        if (role != null) {
            role.unsetPermission(permission);
            invalidateAllCaches();
        }
    }
    
    public String getPrefix(UUID playerId) {
        PlayerPermissions pp = playerPermissions.get(playerId);
        if (pp != null && pp.getCustomPrefix() != null) {
            return pp.getCustomPrefix();
        }
        return getPrimaryRole(playerId).getPrefix();
    }
    
    public String getSuffix(UUID playerId) {
        PlayerPermissions pp = playerPermissions.get(playerId);
        if (pp != null && pp.getCustomSuffix() != null) {
            return pp.getCustomSuffix();
        }
        return getPrimaryRole(playerId).getSuffix();
    }
    
    public void setCustomPrefix(UUID playerId, String prefix) {
        PlayerPermissions pp = getOrCreatePlayerPermissions(playerId);
        pp.setCustomPrefix(prefix);
    }
    
    public void setCustomSuffix(UUID playerId, String suffix) {
        PlayerPermissions pp = getOrCreatePlayerPermissions(playerId);
        pp.setCustomSuffix(suffix);
    }
    
    public void invalidateCache(UUID playerId) {
        permissionCache.remove(playerId);
    }
    
    public void invalidateAllCaches() {
        permissionCache.clear();
    }
    
    public void onRoleAdded(BiConsumer<UUID, Role> listener) {
        roleAddListeners.add(listener);
    }
    
    public void onRoleRemoved(BiConsumer<UUID, Role> listener) {
        roleRemoveListeners.add(listener);
    }
    
    public void registerPermission(Permission permission) {
        permissionRegistry.put(permission.getNode(), permission);
    }
    
    private PermissionValue resolve(UUID playerId, String permission, PermissionContext context) {
        PlayerPermissions pp = playerPermissions.get(playerId);
        
        if (pp != null) {
            PermissionGrant direct = pp.getDirectPermission(permission);
            if (direct != null && direct.context().matches(context)) {
                if (direct.expiresAt() == null || direct.expiresAt() > System.currentTimeMillis()) {
                    return direct.value();
                }
            }
        }
        
        List<Role> playerRoles = getPlayerRoles(playerId).stream()
            .sorted(Comparator.comparingInt(Role::getPriority).reversed())
            .toList();
        
        for (Role role : playerRoles) {
            PermissionValue value = resolveRolePermission(role, permission, new HashSet<>());
            if (value != PermissionValue.UNDEFINED) {
                return value;
            }
        }
        
        PermissionValue wildcard = resolveWildcard(playerId, permission, context);
        if (wildcard != PermissionValue.UNDEFINED) {
            return wildcard;
        }
        
        Permission perm = permissionRegistry.get(permission);
        if (perm != null) {
            return perm.getDefaultValue() == Permission.PermissionDefault.TRUE 
                ? PermissionValue.TRUE 
                : PermissionValue.FALSE;
        }
        
        return PermissionValue.FALSE;
    }
    
    private PermissionValue resolveRolePermission(Role role, String permission, Set<String> visited) {
        if (visited.contains(role.getId())) {
            return PermissionValue.UNDEFINED;
        }
        visited.add(role.getId());
        
        PermissionValue value = role.getPermission(permission);
        if (value != PermissionValue.UNDEFINED) {
            return value;
        }
        
        PermissionValue wildcardValue = role.getPermission("*");
        if (wildcardValue != PermissionValue.UNDEFINED) {
            return wildcardValue;
        }
        
        for (Role parent : role.getParents()) {
            PermissionValue parentValue = resolveRolePermission(parent, permission, visited);
            if (parentValue != PermissionValue.UNDEFINED) {
                return parentValue;
            }
        }
        
        return PermissionValue.UNDEFINED;
    }
    
    private PermissionValue resolveWildcard(UUID playerId, String permission, PermissionContext context) {
        String[] parts = permission.split("\\.");
        StringBuilder builder = new StringBuilder();
        
        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) builder.append(".");
            builder.append(parts[i]);
            
            String wildcard = builder + ".*";
            if (hasDirectPermission(playerId, wildcard)) {
                return getDirectPermissionValue(playerId, wildcard);
            }
        }
        
        return PermissionValue.UNDEFINED;
    }
    
    private boolean hasDirectPermission(UUID playerId, String permission) {
        PlayerPermissions pp = playerPermissions.get(playerId);
        return pp != null && pp.hasDirectPermission(permission);
    }
    
    private PermissionValue getDirectPermissionValue(UUID playerId, String permission) {
        PlayerPermissions pp = playerPermissions.get(playerId);
        if (pp == null) return PermissionValue.UNDEFINED;
        PermissionGrant grant = pp.getDirectPermission(permission);
        return grant != null ? grant.value() : PermissionValue.UNDEFINED;
    }
    
    private PlayerPermissions getOrCreatePlayerPermissions(UUID playerId) {
        return playerPermissions.computeIfAbsent(playerId, id -> {
            PlayerPermissions pp = new PlayerPermissions(id);
            pp.addRole(getDefaultRole());
            return pp;
        });
    }
    
    public void setCacheExpiry(Duration cacheExpiry) {
        this.cacheExpiry = cacheExpiry;
    }
    
    private static class PermissionCache {
        private final Map<String, PermissionValue> cache = new ConcurrentHashMap<>();
        private final long createdAt = System.currentTimeMillis();
        
        void put(String permission, PermissionValue value) {
            cache.put(permission, value);
        }
        
        PermissionValue get(String permission) {
            return cache.get(permission);
        }
        
        boolean isExpired(Duration expiry) {
            return System.currentTimeMillis() - createdAt > expiry.toMillis();
        }
    }
}
