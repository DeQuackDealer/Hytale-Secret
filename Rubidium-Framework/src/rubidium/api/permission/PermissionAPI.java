package rubidium.api.permission;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PermissionAPI {
    
    private static final Map<String, PermissionNode> permissions = new ConcurrentHashMap<>();
    private static final Map<String, PermissionGroup> groups = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<String>> playerPermissions = new ConcurrentHashMap<>();
    private static final Map<UUID, String> playerGroups = new ConcurrentHashMap<>();
    
    private PermissionAPI() {}
    
    public static PermissionNode register(String permission) {
        return register(permission, PermissionDefault.OP);
    }
    
    public static PermissionNode register(String permission, PermissionDefault defaultValue) {
        PermissionNode node = new PermissionNode(permission, "", defaultValue);
        permissions.put(permission, node);
        return node;
    }
    
    public static PermissionNode register(String permission, String description, PermissionDefault defaultValue) {
        PermissionNode node = new PermissionNode(permission, description, defaultValue);
        permissions.put(permission, node);
        return node;
    }
    
    public static Optional<PermissionNode> get(String permission) {
        return Optional.ofNullable(permissions.get(permission));
    }
    
    public static boolean hasPermission(UUID playerId, String permission) {
        Set<String> playerPerms = playerPermissions.get(playerId);
        if (playerPerms != null && playerPerms.contains(permission)) {
            return true;
        }
        
        String groupName = playerGroups.get(playerId);
        if (groupName != null) {
            PermissionGroup group = groups.get(groupName);
            if (group != null && group.hasPermission(permission)) {
                return true;
            }
        }
        
        if (hasWildcard(playerId, permission)) {
            return true;
        }
        
        PermissionNode node = permissions.get(permission);
        if (node != null) {
            return node.defaultValue() == PermissionDefault.TRUE;
        }
        
        return false;
    }
    
    private static boolean hasWildcard(UUID playerId, String permission) {
        Set<String> playerPerms = playerPermissions.get(playerId);
        if (playerPerms != null && playerPerms.contains("*")) {
            return true;
        }
        
        String[] parts = permission.split("\\.");
        StringBuilder wildcard = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) wildcard.append(".");
            wildcard.append(parts[i]);
            if (playerPerms != null && playerPerms.contains(wildcard + ".*")) {
                return true;
            }
        }
        
        return false;
    }
    
    public static void grant(UUID playerId, String permission) {
        playerPermissions.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet()).add(permission);
    }
    
    public static void revoke(UUID playerId, String permission) {
        Set<String> perms = playerPermissions.get(playerId);
        if (perms != null) {
            perms.remove(permission);
        }
    }
    
    public static void setGroup(UUID playerId, String groupName) {
        playerGroups.put(playerId, groupName);
    }
    
    public static Optional<String> getGroup(UUID playerId) {
        return Optional.ofNullable(playerGroups.get(playerId));
    }
    
    public static PermissionGroup createGroup(String name) {
        PermissionGroup group = new PermissionGroup(name);
        groups.put(name, group);
        return group;
    }
    
    public static Optional<PermissionGroup> getGroupByName(String name) {
        return Optional.ofNullable(groups.get(name));
    }
    
    public static Collection<PermissionGroup> allGroups() {
        return groups.values();
    }
    
    public static PermissionGroup defaultGroup() {
        return groups.computeIfAbsent("default", PermissionGroup::new);
    }
    
    public static PermissionGroup adminGroup() {
        PermissionGroup admin = groups.computeIfAbsent("admin", PermissionGroup::new);
        if (!admin.hasPermission("*")) {
            admin.grant("*");
        }
        return admin;
    }
    
    public enum PermissionDefault {
        TRUE,
        FALSE,
        OP,
        NOT_OP
    }
    
    public record PermissionNode(String permission, String description, PermissionDefault defaultValue) {}
    
    public static class PermissionGroup {
        private final String name;
        private final Set<String> permissions = ConcurrentHashMap.newKeySet();
        private String parent;
        private String prefix = "";
        private String suffix = "";
        private int weight = 0;
        
        public PermissionGroup(String name) {
            this.name = name;
        }
        
        public String getName() { return name; }
        public String getPrefix() { return prefix; }
        public String getSuffix() { return suffix; }
        public int getWeight() { return weight; }
        public Set<String> getPermissions() { return Set.copyOf(permissions); }
        
        public PermissionGroup grant(String permission) {
            permissions.add(permission);
            return this;
        }
        
        public PermissionGroup revoke(String permission) {
            permissions.remove(permission);
            return this;
        }
        
        public boolean hasPermission(String permission) {
            if (permissions.contains(permission) || permissions.contains("*")) {
                return true;
            }
            if (parent != null) {
                PermissionGroup parentGroup = groups.get(parent);
                if (parentGroup != null) {
                    return parentGroup.hasPermission(permission);
                }
            }
            return false;
        }
        
        public PermissionGroup parent(String parent) { this.parent = parent; return this; }
        public PermissionGroup prefix(String prefix) { this.prefix = prefix; return this; }
        public PermissionGroup suffix(String suffix) { this.suffix = suffix; return this; }
        public PermissionGroup weight(int weight) { this.weight = weight; return this; }
    }
}
