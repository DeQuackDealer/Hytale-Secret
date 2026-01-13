package com.yellowtale.rubidium.permissions;

import java.util.HashSet;
import java.util.Set;

public class Permission {
    private final String node;
    private String description;
    private PermissionDefault defaultValue;
    private final Set<String> children;
    
    public Permission(String node) {
        this(node, null, PermissionDefault.FALSE);
    }
    
    public Permission(String node, String description, PermissionDefault defaultValue) {
        this.node = node;
        this.description = description;
        this.defaultValue = defaultValue;
        this.children = new HashSet<>();
    }
    
    public String getNode() { return node; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public PermissionDefault getDefaultValue() { return defaultValue; }
    public void setDefaultValue(PermissionDefault defaultValue) { this.defaultValue = defaultValue; }
    public Set<String> getChildren() { return children; }
    
    public void addChild(String child) {
        children.add(child);
    }
    
    public void removeChild(String child) {
        children.remove(child);
    }
    
    public boolean isGrantedByDefault() {
        return defaultValue == PermissionDefault.TRUE;
    }
    
    public enum PermissionDefault {
        TRUE,
        FALSE,
        OP,
        NOT_OP
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Permission that = (Permission) o;
        return node.equals(that.node);
    }
    
    @Override
    public int hashCode() {
        return node.hashCode();
    }
}
