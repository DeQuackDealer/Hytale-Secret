package rubidium.api.item;

import rubidium.api.registry.ResourceId;
import java.util.*;
import java.util.function.Consumer;

public class ItemDefinition {
    
    private final ResourceId id;
    private final String displayName;
    private final ItemType type;
    private final int maxStackSize;
    private final int durability;
    private final Rarity rarity;
    private final Set<String> tags;
    private final Map<String, Double> attributes;
    private final List<String> lore;
    private final Consumer<ItemUseContext> onUse;
    private final Consumer<ItemInteractContext> onInteract;
    private final Consumer<ItemBreakContext> onBreak;
    
    private ItemDefinition(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.type = builder.type;
        this.maxStackSize = builder.maxStackSize;
        this.durability = builder.durability;
        this.rarity = builder.rarity;
        this.tags = Set.copyOf(builder.tags);
        this.attributes = Map.copyOf(builder.attributes);
        this.lore = List.copyOf(builder.lore);
        this.onUse = builder.onUse;
        this.onInteract = builder.onInteract;
        this.onBreak = builder.onBreak;
    }
    
    public ResourceId getId() { return id; }
    public String getDisplayName() { return displayName; }
    public ItemType getType() { return type; }
    public int getMaxStackSize() { return maxStackSize; }
    public int getDurability() { return durability; }
    public Rarity getRarity() { return rarity; }
    public Set<String> getTags() { return tags; }
    public Map<String, Double> getAttributes() { return attributes; }
    public List<String> getLore() { return lore; }
    
    public void handleUse(ItemUseContext ctx) { if (onUse != null) onUse.accept(ctx); }
    public void handleInteract(ItemInteractContext ctx) { if (onInteract != null) onInteract.accept(ctx); }
    public void handleBreak(ItemBreakContext ctx) { if (onBreak != null) onBreak.accept(ctx); }
    
    public static Builder builder(String id) {
        return new Builder(ResourceId.of(id));
    }
    
    public static Builder builder(ResourceId id) {
        return new Builder(id);
    }
    
    public enum ItemType {
        MATERIAL, TOOL, WEAPON, ARMOR, FOOD, BLOCK, MISC
    }
    
    public enum Rarity {
        COMMON, UNCOMMON, RARE, EPIC, LEGENDARY
    }
    
    public static class Builder {
        private final ResourceId id;
        private String displayName;
        private ItemType type = ItemType.MISC;
        private int maxStackSize = 64;
        private int durability = 0;
        private Rarity rarity = Rarity.COMMON;
        private Set<String> tags = new HashSet<>();
        private Map<String, Double> attributes = new HashMap<>();
        private List<String> lore = new ArrayList<>();
        private Consumer<ItemUseContext> onUse;
        private Consumer<ItemInteractContext> onInteract;
        private Consumer<ItemBreakContext> onBreak;
        
        private Builder(ResourceId id) {
            this.id = id;
            this.displayName = id.path();
        }
        
        public Builder displayName(String name) { this.displayName = name; return this; }
        public Builder type(ItemType type) { this.type = type; return this; }
        public Builder maxStackSize(int size) { this.maxStackSize = size; return this; }
        public Builder durability(int durability) { this.durability = durability; return this; }
        public Builder rarity(Rarity rarity) { this.rarity = rarity; return this; }
        public Builder tag(String tag) { this.tags.add(tag); return this; }
        public Builder attribute(String key, double value) { this.attributes.put(key, value); return this; }
        public Builder lore(String... lines) { this.lore.addAll(Arrays.asList(lines)); return this; }
        public Builder onUse(Consumer<ItemUseContext> h) { this.onUse = h; return this; }
        public Builder onInteract(Consumer<ItemInteractContext> h) { this.onInteract = h; return this; }
        public Builder onBreak(Consumer<ItemBreakContext> h) { this.onBreak = h; return this; }
        
        public ItemDefinition build() {
            return new ItemDefinition(this);
        }
    }
    
    public record ItemUseContext(Object player, Object item, String hand) {}
    public record ItemInteractContext(Object player, Object item, Object target, String hand) {}
    public record ItemBreakContext(Object player, Object item) {}
}
