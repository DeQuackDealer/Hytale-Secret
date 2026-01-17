package rubidium.api.item;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ItemRegistry {
    
    private static final Map<String, ItemType> items = new ConcurrentHashMap<>();
    
    private ItemRegistry() {}
    
    public static void register(ItemType itemType) {
        items.put(itemType.getId(), itemType);
    }
    
    public static Optional<ItemType> get(String id) {
        return Optional.ofNullable(items.get(id));
    }
    
    public static Collection<ItemType> getAll() {
        return Collections.unmodifiableCollection(items.values());
    }
    
    public static List<ItemType> search(String query) {
        String lowerQuery = query.toLowerCase();
        return items.values().stream()
            .filter(item -> item.getId().toLowerCase().contains(lowerQuery) ||
                           item.getName().toLowerCase().contains(lowerQuery))
            .toList();
    }
    
    public static int count() {
        return items.size();
    }
}
