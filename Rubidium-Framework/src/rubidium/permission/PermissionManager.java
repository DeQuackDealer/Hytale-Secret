package rubidium.permission;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionManager {
    
    private final Map<UUID, Set<String>> playerPermissions;
    private final Map<String, PermissionGroup> groups;
    private final Map<UUID, Set<String>> playerGroups;
    private String defaultGroup = "default";
    
    public PermissionManager() {
        this.playerPermissions = new ConcurrentHashMap<>();
        this.groups = new ConcurrentHashMap<>();
        this.playerGroups = new ConcurrentHashMap<>();
        
        createGroup("default");
    }
    
    public boolean hasPermission(UUID player, String permission) {
        if (permission == null || permission.isEmpty()) return true;
        
        Set<String> directPerms = playerPermissions.get(player);
        if (directPerms != null) {
            if (directPerms.contains("*")) return true;
            if (directPerms.contains(permission)) return true;
            if (hasWildcard(directPerms, permission)) return true;
        }
        
        Set<String> groupNames = playerGroups.getOrDefault(player, new HashSet<>());
        if (groupNames.isEmpty()) {
            groupNames = Set.of(defaultGroup);
        }
        
        for (String groupName : groupNames) {
            PermissionGroup group = groups.get(groupName);
            if (group != null && group.hasPermission(permission)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean hasWildcard(Set<String> permissions, String permission) {
        String[] parts = permission.split("\\.");
        StringBuilder builder = new StringBuilder();
        
        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) builder.append(".");
            builder.append(parts[i]);
            
            if (permissions.contains(builder.toString() + ".*")) {
                return true;
            }
        }
        
        return false;
    }
    
    public void addPermission(UUID player, String permission) {
        playerPermissions.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet())
            .add(permission);
    }
    
    public void removePermission(UUID player, String permission) {
        Set<String> perms = playerPermissions.get(player);
        if (perms != null) {
            perms.remove(permission);
        }
    }
    
    public Set<String> getPermissions(UUID player) {
        return new HashSet<>(playerPermissions.getOrDefault(player, Collections.emptySet()));
    }
    
    public void clearPermissions(UUID player) {
        playerPermissions.remove(player);
    }
    
    public void createGroup(String name) {
        groups.putIfAbsent(name, new PermissionGroup(name));
    }
    
    public void deleteGroup(String name) {
        groups.remove(name);
        
        for (Set<String> playerGroupSet : playerGroups.values()) {
            playerGroupSet.remove(name);
        }
    }
    
    public PermissionGroup getGroup(String name) {
        return groups.get(name);
    }
    
    public Collection<PermissionGroup> getGroups() {
        return Collections.unmodifiableCollection(groups.values());
    }
    
    public void addPlayerToGroup(UUID player, String groupName) {
        if (!groups.containsKey(groupName)) return;
        playerGroups.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet())
            .add(groupName);
    }
    
    public void removePlayerFromGroup(UUID player, String groupName) {
        Set<String> groupSet = playerGroups.get(player);
        if (groupSet != null) {
            groupSet.remove(groupName);
        }
    }
    
    public Set<String> getPlayerGroups(UUID player) {
        return new HashSet<>(playerGroups.getOrDefault(player, Collections.emptySet()));
    }
    
    public void setDefaultGroup(String name) {
        this.defaultGroup = name;
    }
    
    public String getDefaultGroup() {
        return defaultGroup;
    }
    
    public static class PermissionGroup {
        private final String name;
        private final Set<String> permissions;
        private final Set<String> inheritedGroups;
        private String prefix = "";
        private String suffix = "";
        private int priority = 0;
        
        public PermissionGroup(String name) {
            this.name = name;
            this.permissions = ConcurrentHashMap.newKeySet();
            this.inheritedGroups = ConcurrentHashMap.newKeySet();
        }
        
        public String getName() { return name; }
        
        public void addPermission(String permission) { permissions.add(permission); }
        public void removePermission(String permission) { permissions.remove(permission); }
        public Set<String> getPermissions() { return new HashSet<>(permissions); }
        
        public void addInheritance(String group) { inheritedGroups.add(group); }
        public void removeInheritance(String group) { inheritedGroups.remove(group); }
        public Set<String> getInheritedGroups() { return new HashSet<>(inheritedGroups); }
        
        public String getPrefix() { return prefix; }
        public void setPrefix(String prefix) { this.prefix = prefix; }
        
        public String getSuffix() { return suffix; }
        public void setSuffix(String suffix) { this.suffix = suffix; }
        
        public int getPriority() { return priority; }
        public void setPriority(int priority) { this.priority = priority; }
        
        public boolean hasPermission(String permission) {
            if (permissions.contains("*")) return true;
            if (permissions.contains(permission)) return true;
            
            String[] parts = permission.split("\\.");
            StringBuilder builder = new StringBuilder();
            
            for (int i = 0; i < parts.length - 1; i++) {
                if (i > 0) builder.append(".");
                builder.append(parts[i]);
                
                if (permissions.contains(builder.toString() + ".*")) {
                    return true;
                }
            }
            
            return false;
        }
    }
}
