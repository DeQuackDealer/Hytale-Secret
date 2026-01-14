package rubidium.permissions;

/**
 * Context key definition for typed contexts.
 */
public class ContextKey<T> {
    
    private final String key;
    private final Class<T> type;
    
    public ContextKey(String key, Class<T> type) {
        this.key = key;
        this.type = type;
    }
    
    public String getKey() { return key; }
    public Class<T> getType() { return type; }
}
