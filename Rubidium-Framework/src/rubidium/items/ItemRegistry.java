package rubidium.items;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for custom items.
 */
public class ItemRegistry {
    
    private static ItemRegistry instance;
    
    private final Map<String, CustomItem> items;
    private final Map<String, List<CustomItem>> categories;
    
    private ItemRegistry() {
        this.items = new ConcurrentHashMap<>();
        this.categories = new ConcurrentHashMap<>();
    }
    
    public static ItemRegistry getInstance() {
        if (instance == null) {
            instance = new ItemRegistry();
        }
        return instance;
    }
    
    public void register(CustomItem item) {
        items.put(item.getId(), item);
    }
    
    public void register(String category, CustomItem item) {
        items.put(item.getId(), item);
        categories.computeIfAbsent(category, k -> new ArrayList<>()).add(item);
    }
    
    public Optional<CustomItem> get(String id) {
        return Optional.ofNullable(items.get(id));
    }
    
    public Collection<CustomItem> getAll() {
        return items.values();
    }
    
    public List<CustomItem> getByCategory(String category) {
        return categories.getOrDefault(category, Collections.emptyList());
    }
    
    public void unregister(String id) {
        CustomItem item = items.remove(id);
        if (item != null) {
            categories.values().forEach(list -> list.remove(item));
        }
    }
    
    public boolean exists(String id) {
        return items.containsKey(id);
    }
}
