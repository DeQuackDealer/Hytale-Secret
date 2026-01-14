package rubidium.essentials;

import java.util.*;

/**
 * Kit definition with items and cooldown.
 */
public class Kit {
    
    private final String name;
    private final String displayName;
    private final List<KitItem> items;
    private final long cooldownMs;
    private final String permission;
    
    public Kit(String name, String displayName, long cooldownMs, String permission) {
        this.name = name;
        this.displayName = displayName;
        this.items = new ArrayList<>();
        this.cooldownMs = cooldownMs;
        this.permission = permission;
    }
    
    public Kit addItem(String itemId, int amount) {
        items.add(new KitItem(itemId, amount, null));
        return this;
    }
    
    public Kit addItem(String itemId, int amount, Map<String, Object> nbt) {
        items.add(new KitItem(itemId, amount, nbt));
        return this;
    }
    
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public List<KitItem> getItems() { return items; }
    public long getCooldownMs() { return cooldownMs; }
    public String getPermission() { return permission; }
    
    public record KitItem(String itemId, int amount, Map<String, Object> nbt) {}
}
