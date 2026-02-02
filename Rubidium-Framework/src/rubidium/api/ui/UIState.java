package rubidium.api.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UIState {
    
    private final Map<String, Object> values = new ConcurrentHashMap<>();
    private final Map<String, Object> dirtyValues = new ConcurrentHashMap<>();
    private volatile boolean dirty = false;
    
    public void set(String key, Object value) {
        Object old = values.get(key);
        if (!java.util.Objects.equals(old, value)) {
            values.put(key, value);
            dirtyValues.put(key, value);
            dirty = true;
        }
    }
    
    public void setString(String key, String value) {
        set(key, value);
    }
    
    public void setBoolean(String key, boolean value) {
        set(key, value);
    }
    
    public void setInt(String key, int value) {
        set(key, value);
    }
    
    public void setFloat(String key, float value) {
        set(key, value);
    }
    
    public void setDouble(String key, double value) {
        set(key, value);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) values.get(key);
    }
    
    public String getString(String key) {
        Object val = values.get(key);
        return val != null ? val.toString() : null;
    }
    
    public String getString(String key, String defaultValue) {
        String val = getString(key);
        return val != null ? val : defaultValue;
    }
    
    public boolean getBoolean(String key) {
        Object val = values.get(key);
        return val instanceof Boolean ? (Boolean) val : false;
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        Object val = values.get(key);
        return val instanceof Boolean ? (Boolean) val : defaultValue;
    }
    
    public int getInt(String key) {
        Object val = values.get(key);
        return val instanceof Number ? ((Number) val).intValue() : 0;
    }
    
    public int getInt(String key, int defaultValue) {
        Object val = values.get(key);
        return val instanceof Number ? ((Number) val).intValue() : defaultValue;
    }
    
    public float getFloat(String key) {
        Object val = values.get(key);
        return val instanceof Number ? ((Number) val).floatValue() : 0f;
    }
    
    public float getFloat(String key, float defaultValue) {
        Object val = values.get(key);
        return val instanceof Number ? ((Number) val).floatValue() : defaultValue;
    }
    
    public double getDouble(String key) {
        Object val = values.get(key);
        return val instanceof Number ? ((Number) val).doubleValue() : 0.0;
    }
    
    public double getDouble(String key, double defaultValue) {
        Object val = values.get(key);
        return val instanceof Number ? ((Number) val).doubleValue() : defaultValue;
    }
    
    public boolean isDirty() {
        return dirty;
    }
    
    public Map<String, Object> getDirtyEntries() {
        return new HashMap<>(dirtyValues);
    }
    
    public void clearDirty() {
        dirtyValues.clear();
        dirty = false;
    }
    
    public void clear() {
        values.clear();
        dirtyValues.clear();
        dirty = false;
    }
    
    public boolean has(String key) {
        return values.containsKey(key);
    }
    
    public void remove(String key) {
        values.remove(key);
        dirtyValues.put(key, null);
        dirty = true;
    }
}
