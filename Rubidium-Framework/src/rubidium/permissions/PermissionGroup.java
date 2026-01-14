package rubidium.permissions;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Permission group with inheritance and weight.
 */
public class PermissionGroup {
    
    private final String id;
    private String displayName;
    private int weight;
    private String prefix;
    private String suffix;
    private boolean isDefault;
    private final Set<String> parents;
    private final Map<PermissionEntry, Boolean> permissions;
    
    public PermissionGroup(String id, String displayName, int weight) {
        this.id = id;
        this.displayName = displayName;
        this.weight = weight;
        this.prefix = "";
        this.suffix = "";
        this.isDefault = false;
        this.parents = ConcurrentHashMap.newKeySet();
        this.permissions = new ConcurrentHashMap<>();
    }
    
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String name) { this.displayName = name; }
    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }
    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    public String getSuffix() { return suffix; }
    public void setSuffix(String suffix) { this.suffix = suffix; }
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
    
    public Set<String> getParents() { return parents; }
    
    public void addParent(String parentId) {
        parents.add(parentId);
    }
    
    public void removeParent(String parentId) {
        parents.remove(parentId);
    }
    
    public void setPermission(String permission, boolean value) {
        setPermission(permission, value, PermissionContext.empty());
    }
    
    public void setPermission(String permission, boolean value, PermissionContext context) {
        permissions.put(new PermissionEntry(permission, context), value);
    }
    
    public void unsetPermission(String permission) {
        unsetPermission(permission, PermissionContext.empty());
    }
    
    public void unsetPermission(String permission, PermissionContext context) {
        permissions.remove(new PermissionEntry(permission, context));
    }
    
    public boolean hasExplicitPermission(String permission, PermissionContext context) {
        return permissions.containsKey(new PermissionEntry(permission, context)) ||
               permissions.containsKey(new PermissionEntry(permission, PermissionContext.empty()));
    }
    
    public boolean getExplicitPermission(String permission, PermissionContext context) {
        Boolean value = permissions.get(new PermissionEntry(permission, context));
        if (value != null) return value;
        
        value = permissions.get(new PermissionEntry(permission, PermissionContext.empty()));
        return value != null && value;
    }
    
    public Map<PermissionEntry, Boolean> getPermissions() {
        return Collections.unmodifiableMap(permissions);
    }
}
