package com.yellowtale.rubidium.permissions;

public enum PermissionValue {
    TRUE,
    FALSE,
    UNDEFINED;
    
    public static PermissionValue of(boolean value) {
        return value ? TRUE : FALSE;
    }
    
    public boolean toBoolean() {
        return this == TRUE;
    }
    
    public boolean isDefined() {
        return this != UNDEFINED;
    }
}
