package rubidium.api.registry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class Registry<T> {
    
    private static final Logger logger = Logger.getLogger("Rubidium");
    
    private final String name;
    private final Map<ResourceId, T> entries = new ConcurrentHashMap<>();
    private final List<Consumer<T>> registerCallbacks = new ArrayList<>();
    
    public Registry(String name) {
        this.name = name;
    }
    
    public T register(ResourceId id, T entry) {
        if (entries.containsKey(id)) {
            logger.warning("Overwriting " + name + " registry entry: " + id);
        }
        entries.put(id, entry);
        registerCallbacks.forEach(cb -> cb.accept(entry));
        logger.fine("Registered " + name + ": " + id);
        return entry;
    }
    
    public T register(String path, T entry) {
        return register(ResourceId.of(path), entry);
    }
    
    public Optional<T> get(ResourceId id) {
        return Optional.ofNullable(entries.get(id));
    }
    
    public Optional<T> get(String id) {
        return get(ResourceId.parse(id));
    }
    
    public T getOrThrow(ResourceId id) {
        return get(id).orElseThrow(() -> 
            new NoSuchElementException(name + " not found: " + id));
    }
    
    public boolean contains(ResourceId id) {
        return entries.containsKey(id);
    }
    
    public Set<ResourceId> keys() {
        return Collections.unmodifiableSet(entries.keySet());
    }
    
    public Collection<T> values() {
        return Collections.unmodifiableCollection(entries.values());
    }
    
    public int size() {
        return entries.size();
    }
    
    public void unregister(ResourceId id) {
        entries.remove(id);
    }
    
    public void clear() {
        entries.clear();
    }
    
    public void onRegister(Consumer<T> callback) {
        registerCallbacks.add(callback);
    }
    
    public String getName() {
        return name;
    }
}
