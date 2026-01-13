package rubidium.permissions;

import java.util.Map;

public record PermissionContext(
    String world,
    String server,
    Map<String, String> conditions
) {
    public static PermissionContext global() {
        return new PermissionContext(null, null, Map.of());
    }
    
    public static PermissionContext world(String world) {
        return new PermissionContext(world, null, Map.of());
    }
    
    public static PermissionContext server(String server) {
        return new PermissionContext(null, server, Map.of());
    }
    
    public static PermissionContext with(String key, String value) {
        return new PermissionContext(null, null, Map.of(key, value));
    }
    
    public boolean matches(PermissionContext required) {
        if (required.world() != null && !required.world().equals(this.world())) {
            return false;
        }
        if (required.server() != null && !required.server().equals(this.server())) {
            return false;
        }
        for (var entry : required.conditions().entrySet()) {
            if (!entry.getValue().equals(this.conditions().get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }
    
    public boolean isGlobal() {
        return world == null && server == null && conditions.isEmpty();
    }
}
