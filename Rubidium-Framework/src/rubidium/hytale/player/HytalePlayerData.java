package rubidium.hytale.player;

import rubidium.api.player.PlayerData;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class HytalePlayerData implements PlayerData {
    
    private static final Logger logger = Logger.getLogger("Rubidium-HytalePlayerData");
    
    private final Object hytalePlayer;
    private final Map<String, Object> localData;
    private Object persistentDataHandle;
    
    public HytalePlayerData(Object hytalePlayer) {
        this.hytalePlayer = hytalePlayer;
        this.localData = new ConcurrentHashMap<>();
        extractPersistentData();
    }
    
    private void extractPersistentData() {
        try {
            Method m = hytalePlayer.getClass().getMethod("getPersistentData");
            this.persistentDataHandle = m.invoke(hytalePlayer);
        } catch (Exception e) {
            try {
                Method m = hytalePlayer.getClass().getMethod("getData");
                this.persistentDataHandle = m.invoke(hytalePlayer);
            } catch (Exception ex) {
                logger.fine("Using local data storage for player data");
            }
        }
    }
    
    @Override
    public void set(String key, Object value) {
        localData.put(key, value);
        setToPersistent(key, value);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        Object value = localData.get(key);
        if (value == null) {
            value = getFromPersistent(key);
        }
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }
    
    @Override
    public String getString(String key) {
        return getString(key, null);
    }
    
    @Override
    public String getString(String key, String defaultValue) {
        return get(key, String.class).orElse(defaultValue);
    }
    
    @Override
    public int getInt(String key) {
        return getInt(key, 0);
    }
    
    @Override
    public int getInt(String key, int defaultValue) {
        return get(key, Number.class).map(Number::intValue).orElse(defaultValue);
    }
    
    @Override
    public long getLong(String key) {
        return getLong(key, 0L);
    }
    
    @Override
    public long getLong(String key, long defaultValue) {
        return get(key, Number.class).map(Number::longValue).orElse(defaultValue);
    }
    
    @Override
    public double getDouble(String key) {
        return getDouble(key, 0.0);
    }
    
    @Override
    public double getDouble(String key, double defaultValue) {
        return get(key, Number.class).map(Number::doubleValue).orElse(defaultValue);
    }
    
    @Override
    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }
    
    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return get(key, Boolean.class).orElse(defaultValue);
    }
    
    @Override
    public boolean has(String key) {
        return localData.containsKey(key) || hasInPersistent(key);
    }
    
    @Override
    public void remove(String key) {
        localData.remove(key);
        removeFromPersistent(key);
    }
    
    @Override
    public void save() {
        if (persistentDataHandle != null) {
            try {
                Method m = persistentDataHandle.getClass().getMethod("save");
                m.invoke(persistentDataHandle);
            } catch (Exception e) {
                logger.fine("Persistent data save not available");
            }
        }
    }
    
    @Override
    public void reload() {
        localData.clear();
        extractPersistentData();
    }
    
    @SuppressWarnings("unchecked")
    private <T> T getFromPersistent(String key) {
        if (persistentDataHandle == null) return null;
        
        try {
            Method m = persistentDataHandle.getClass().getMethod("get", String.class);
            return (T) m.invoke(persistentDataHandle, key);
        } catch (Exception e) {
            return null;
        }
    }
    
    private void setToPersistent(String key, Object value) {
        if (persistentDataHandle == null) return;
        
        try {
            Method m = persistentDataHandle.getClass().getMethod("set", String.class, Object.class);
            m.invoke(persistentDataHandle, key, value);
        } catch (Exception ignored) {}
    }
    
    private boolean hasInPersistent(String key) {
        if (persistentDataHandle == null) return false;
        
        try {
            Method m = persistentDataHandle.getClass().getMethod("has", String.class);
            return (boolean) m.invoke(persistentDataHandle, key);
        } catch (Exception e) {
            try {
                Method m = persistentDataHandle.getClass().getMethod("containsKey", String.class);
                return (boolean) m.invoke(persistentDataHandle, key);
            } catch (Exception ex) {
                return false;
            }
        }
    }
    
    private void removeFromPersistent(String key) {
        if (persistentDataHandle == null) return;
        
        try {
            Method m = persistentDataHandle.getClass().getMethod("remove", String.class);
            m.invoke(persistentDataHandle, key);
        } catch (Exception ignored) {}
    }
}
