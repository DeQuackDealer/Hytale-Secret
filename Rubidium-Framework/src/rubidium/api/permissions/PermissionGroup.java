package rubidium.api.permissions;

import java.util.*;

public class PermissionGroup {
    
    private final String name;
    private final Set<String> permissions = new HashSet<>();
    private final Set<UUID> members = new HashSet<>();
    private String prefix = "";
    private String suffix = "";
    private int priority = 0;
    
    public PermissionGroup(String name) {
        this.name = name;
    }
    
    public String getName() { return name; }
    
    public Set<String> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }
    
    public void addPermission(String permission) {
        permissions.add(permission);
    }
    
    public void removePermission(String permission) {
        permissions.remove(permission);
    }
    
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
    
    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
    }
    
    public void addMember(UUID uuid) {
        members.add(uuid);
    }
    
    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }
    
    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }
    
    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    
    public String getSuffix() { return suffix; }
    public void setSuffix(String suffix) { this.suffix = suffix; }
    
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
}
