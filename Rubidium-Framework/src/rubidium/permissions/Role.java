package rubidium.permissions;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Role {
    private final String id;
    private String name;
    private String prefix;
    private String suffix;
    private int priority;
    private int color;
    
    private final Set<Role> parents;
    private final Map<String, PermissionValue> permissions;
    private final Map<String, String> metadata;
    
    private boolean defaultRole;
    private RoleType type;
    
    public static final Role DEFAULT = new Role("default", "Player", "", "", 0, 0xAAAAAA, RoleType.DEFAULT, true);
    public static final Role VIP = new Role("vip", "VIP", "&a[VIP] ", "", 100, 0x55FF55, RoleType.DONATOR, false);
    public static final Role MODERATOR = new Role("mod", "Moderator", "&b[MOD] ", "", 500, 0x55FFFF, RoleType.STAFF, false);
    public static final Role ADMIN = new Role("admin", "Admin", "&c[ADMIN] ", "", 1000, 0xFF5555, RoleType.ADMIN, false);
    
    static {
        VIP.addParent(DEFAULT);
        MODERATOR.addParent(VIP);
        ADMIN.addParent(MODERATOR);
        ADMIN.setPermission("*", PermissionValue.TRUE);
    }
    
    public Role(String id, String name, String prefix, String suffix, int priority, int color, RoleType type, boolean defaultRole) {
        this.id = id;
        this.name = name;
        this.prefix = prefix;
        this.suffix = suffix;
        this.priority = priority;
        this.color = color;
        this.type = type;
        this.defaultRole = defaultRole;
        this.parents = ConcurrentHashMap.newKeySet();
        this.permissions = new ConcurrentHashMap<>();
        this.metadata = new ConcurrentHashMap<>();
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    public String getSuffix() { return suffix; }
    public void setSuffix(String suffix) { this.suffix = suffix; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
    public RoleType getType() { return type; }
    public void setType(RoleType type) { this.type = type; }
    public boolean isDefaultRole() { return defaultRole; }
    public void setDefaultRole(boolean defaultRole) { this.defaultRole = defaultRole; }
    
    public Set<Role> getParents() { return Collections.unmodifiableSet(parents); }
    public Map<String, PermissionValue> getPermissions() { return Collections.unmodifiableMap(permissions); }
    public Map<String, String> getMetadata() { return metadata; }
    
    public void addParent(Role parent) {
        if (!parent.equals(this)) {
            parents.add(parent);
        }
    }
    
    public void removeParent(Role parent) {
        parents.remove(parent);
    }
    
    public void setPermission(String permission, PermissionValue value) {
        permissions.put(permission, value);
    }
    
    public void unsetPermission(String permission) {
        permissions.remove(permission);
    }
    
    public PermissionValue getPermission(String permission) {
        return permissions.getOrDefault(permission, PermissionValue.UNDEFINED);
    }
    
    public boolean hasPermission(String permission) {
        return permissions.containsKey(permission) && permissions.get(permission) == PermissionValue.TRUE;
    }
    
    public String getColorHex() {
        return String.format("#%06X", color);
    }
    
    public String getFormattedPrefix() {
        return prefix != null ? translateColorCodes(prefix) : "";
    }
    
    public String getFormattedSuffix() {
        return suffix != null ? translateColorCodes(suffix) : "";
    }
    
    private String translateColorCodes(String text) {
        return text;
    }
    
    public enum RoleType {
        DEFAULT,
        DONATOR,
        STAFF,
        ADMIN,
        CUSTOM
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return id.equals(role.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    public static Builder builder(String id) {
        return new Builder(id);
    }
    
    public static class Builder {
        private final String id;
        private String name;
        private String prefix = "";
        private String suffix = "";
        private int priority = 0;
        private int color = 0xFFFFFF;
        private RoleType type = RoleType.CUSTOM;
        private boolean defaultRole = false;
        private final List<Role> parents = new ArrayList<>();
        private final Map<String, PermissionValue> permissions = new HashMap<>();
        
        public Builder(String id) {
            this.id = id;
            this.name = id;
        }
        
        public Builder name(String name) { this.name = name; return this; }
        public Builder prefix(String prefix) { this.prefix = prefix; return this; }
        public Builder suffix(String suffix) { this.suffix = suffix; return this; }
        public Builder priority(int priority) { this.priority = priority; return this; }
        public Builder color(int color) { this.color = color; return this; }
        public Builder type(RoleType type) { this.type = type; return this; }
        public Builder defaultRole(boolean defaultRole) { this.defaultRole = defaultRole; return this; }
        public Builder parent(Role parent) { this.parents.add(parent); return this; }
        public Builder permission(String perm, boolean value) { 
            this.permissions.put(perm, value ? PermissionValue.TRUE : PermissionValue.FALSE); 
            return this; 
        }
        
        public Role build() {
            Role role = new Role(id, name, prefix, suffix, priority, color, type, defaultRole);
            parents.forEach(role::addParent);
            permissions.forEach(role::setPermission);
            return role;
        }
    }
}
