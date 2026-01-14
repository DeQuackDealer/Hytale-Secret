package rubidium.permissions;

import java.util.*;

/**
 * Context for contextual permissions (world, server, etc).
 */
public class PermissionContext {
    
    private static final PermissionContext EMPTY = new PermissionContext(Map.of());
    
    private final Map<String, String> values;
    
    private PermissionContext(Map<String, String> values) {
        this.values = values;
    }
    
    public static PermissionContext empty() {
        return EMPTY;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static PermissionContext of(String key, String value) {
        return builder().add(key, value).build();
    }
    
    public Optional<String> get(String key) {
        return Optional.ofNullable(values.get(key));
    }
    
    public boolean has(String key) {
        return values.containsKey(key);
    }
    
    public boolean isEmpty() {
        return values.isEmpty();
    }
    
    public Map<String, String> getValues() {
        return Collections.unmodifiableMap(values);
    }
    
    public boolean matches(PermissionContext other) {
        if (this.isEmpty() || other.isEmpty()) return true;
        
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String otherValue = other.values.get(entry.getKey());
            if (otherValue != null && !otherValue.equals(entry.getValue())) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PermissionContext that = (PermissionContext) o;
        return Objects.equals(values, that.values);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(values);
    }
    
    public static class Builder {
        private final Map<String, String> values = new HashMap<>();
        
        public Builder add(String key, String value) {
            values.put(key, value);
            return this;
        }
        
        public Builder world(String world) {
            return add("world", world);
        }
        
        public Builder server(String server) {
            return add("server", server);
        }
        
        public PermissionContext build() {
            return new PermissionContext(Map.copyOf(values));
        }
    }
}
