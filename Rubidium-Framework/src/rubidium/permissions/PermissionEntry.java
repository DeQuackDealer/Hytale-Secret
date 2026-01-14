package rubidium.permissions;

import java.util.Objects;

/**
 * Permission entry with context for storage.
 */
public record PermissionEntry(String permission, PermissionContext context) {
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PermissionEntry that = (PermissionEntry) o;
        return Objects.equals(permission, that.permission) && 
               Objects.equals(context, that.context);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(permission, context);
    }
}
