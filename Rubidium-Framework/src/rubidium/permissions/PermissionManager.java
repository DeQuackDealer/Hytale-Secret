package rubidium.permissions;

import rubidium.hytale.api.player.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LuckPerms-style permission system with groups, inheritance, contexts, and tracks.
 */
public class PermissionManager {
    
    private static PermissionManager instance;
    
    private final Map<String, PermissionGroup> groups;
    private final Map<UUID, PlayerPermissions> playerPermissions;
    private final Map<String, Track> tracks;
    
    private PermissionManager() {
        this.groups = new ConcurrentHashMap<>();
        this.playerPermissions = new ConcurrentHashMap<>();
        this.tracks = new ConcurrentHashMap<>();
        
        createDefaultGroup();
    }
    
    public static PermissionManager getInstance() {
        if (instance == null) {
            instance = new PermissionManager();
        }
        return instance;
    }
    
    private void createDefaultGroup() {
        PermissionGroup defaultGroup = new PermissionGroup("default", "Default", 0);
        defaultGroup.setDefault(true);
        groups.put("default", defaultGroup);
    }
    
    public void createGroup(String id, String displayName, int weight) {
        groups.put(id, new PermissionGroup(id, displayName, weight));
    }
    
    public Optional<PermissionGroup> getGroup(String id) {
        return Optional.ofNullable(groups.get(id));
    }
    
    public Collection<PermissionGroup> getGroups() {
        return groups.values();
    }
    
    public void deleteGroup(String id) {
        if (!"default".equals(id)) {
            groups.remove(id);
        }
    }
    
    public PlayerPermissions getPlayerPermissions(Player player) {
        return getPlayerPermissions(player.getUuid());
    }
    
    public PlayerPermissions getPlayerPermissions(UUID uuid) {
        return playerPermissions.computeIfAbsent(uuid, k -> {
            PlayerPermissions pp = new PlayerPermissions(k);
            pp.addGroup("default");
            return pp;
        });
    }
    
    public boolean hasPermission(Player player, String permission) {
        return hasPermission(player, permission, PermissionContext.empty());
    }
    
    public boolean hasPermission(Player player, String permission, PermissionContext context) {
        if (player.isOp()) return true;
        
        PlayerPermissions pp = getPlayerPermissions(player);
        
        if (pp.hasExplicitPermission(permission, context)) {
            return pp.getExplicitPermission(permission, context);
        }
        
        for (String groupId : pp.getGroups()) {
            PermissionGroup group = groups.get(groupId);
            if (group != null) {
                Boolean result = checkGroupPermission(group, permission, context, new HashSet<>());
                if (result != null) return result;
            }
        }
        
        return false;
    }
    
    private Boolean checkGroupPermission(PermissionGroup group, String permission, 
                                         PermissionContext context, Set<String> visited) {
        if (visited.contains(group.getId())) return null;
        visited.add(group.getId());
        
        if (group.hasExplicitPermission(permission, context)) {
            return group.getExplicitPermission(permission, context);
        }
        
        String[] parts = permission.split("\\.");
        StringBuilder wildcard = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) wildcard.append(".");
            wildcard.append(parts[i]);
            if (group.hasExplicitPermission(wildcard + ".*", context)) {
                return group.getExplicitPermission(wildcard + ".*", context);
            }
        }
        
        if (group.hasExplicitPermission("*", context)) {
            return group.getExplicitPermission("*", context);
        }
        
        for (String parentId : group.getParents()) {
            PermissionGroup parent = groups.get(parentId);
            if (parent != null) {
                Boolean result = checkGroupPermission(parent, permission, context, visited);
                if (result != null) return result;
            }
        }
        
        return null;
    }
    
    public void setPlayerPermission(Player player, String permission, boolean value) {
        setPlayerPermission(player, permission, value, PermissionContext.empty());
    }
    
    public void setPlayerPermission(Player player, String permission, boolean value, PermissionContext context) {
        getPlayerPermissions(player).setPermission(permission, value, context);
    }
    
    public void addPlayerToGroup(Player player, String groupId) {
        if (groups.containsKey(groupId)) {
            getPlayerPermissions(player).addGroup(groupId);
        }
    }
    
    public void removePlayerFromGroup(Player player, String groupId) {
        if (!"default".equals(groupId)) {
            getPlayerPermissions(player).removeGroup(groupId);
        }
    }
    
    public void createTrack(String id, String displayName, List<String> groupOrder) {
        tracks.put(id, new Track(id, displayName, groupOrder));
    }
    
    public Optional<Track> getTrack(String id) {
        return Optional.ofNullable(tracks.get(id));
    }
    
    public boolean promotePlayer(Player player, String trackId) {
        Track track = tracks.get(trackId);
        if (track == null) return false;
        
        PlayerPermissions pp = getPlayerPermissions(player);
        String currentGroup = track.getCurrentGroup(pp.getGroups());
        String nextGroup = track.getNextGroup(currentGroup);
        
        if (nextGroup != null) {
            if (currentGroup != null) {
                pp.removeGroup(currentGroup);
            }
            pp.addGroup(nextGroup);
            return true;
        }
        
        return false;
    }
    
    public boolean demotePlayer(Player player, String trackId) {
        Track track = tracks.get(trackId);
        if (track == null) return false;
        
        PlayerPermissions pp = getPlayerPermissions(player);
        String currentGroup = track.getCurrentGroup(pp.getGroups());
        String prevGroup = track.getPreviousGroup(currentGroup);
        
        if (prevGroup != null) {
            if (currentGroup != null) {
                pp.removeGroup(currentGroup);
            }
            pp.addGroup(prevGroup);
            return true;
        }
        
        return false;
    }
}
