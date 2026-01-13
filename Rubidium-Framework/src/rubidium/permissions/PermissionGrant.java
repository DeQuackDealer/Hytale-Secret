package rubidium.permissions;

import java.util.UUID;

public record PermissionGrant(
    String permission,
    PermissionValue value,
    PermissionContext context,
    long grantedAt,
    Long expiresAt,
    UUID grantedBy,
    String reason
) {
    public boolean isExpired() {
        return expiresAt != null && expiresAt < System.currentTimeMillis();
    }
    
    public boolean isPermanent() {
        return expiresAt == null;
    }
    
    public long getRemainingTime() {
        if (expiresAt == null) return Long.MAX_VALUE;
        return Math.max(0, expiresAt - System.currentTimeMillis());
    }
}
