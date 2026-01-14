package rubidium.ui;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Reactive state store for UI data binding.
 */
public class StateStore {
    
    private final Map<String, Object> globalState = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Object>> playerState = new ConcurrentHashMap<>();
    private final Map<String, List<Consumer<Object>>> subscribers = new ConcurrentHashMap<>();
    
    public void set(String key, Object value) {
        Object oldValue = globalState.put(key, value);
        if (!Objects.equals(oldValue, value)) {
            notifySubscribers(key, value);
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        Object value = globalState.get(key);
        return value != null ? (T) value : defaultValue;
    }
    
    public <T> T get(String key) {
        return get(key, null);
    }
    
    public void setPlayerState(UUID playerId, String key, Object value) {
        playerState.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
            .put(key, value);
        notifySubscribers(playerId + ":" + key, value);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getPlayerState(UUID playerId, String key, T defaultValue) {
        Map<String, Object> state = playerState.get(playerId);
        if (state == null) return defaultValue;
        Object value = state.get(key);
        return value != null ? (T) value : defaultValue;
    }
    
    public void removePlayerState(UUID playerId) {
        playerState.remove(playerId);
    }
    
    public void subscribe(String key, Consumer<Object> listener) {
        subscribers.computeIfAbsent(key, k -> new ArrayList<>()).add(listener);
    }
    
    public void unsubscribe(String key, Consumer<Object> listener) {
        List<Consumer<Object>> subs = subscribers.get(key);
        if (subs != null) {
            subs.remove(listener);
        }
    }
    
    private void notifySubscribers(String key, Object value) {
        List<Consumer<Object>> subs = subscribers.get(key);
        if (subs != null) {
            for (Consumer<Object> sub : subs) {
                try {
                    sub.accept(value);
                } catch (Exception ignored) {}
            }
        }
    }
    
    public void clear() {
        globalState.clear();
        playerState.clear();
    }
}
