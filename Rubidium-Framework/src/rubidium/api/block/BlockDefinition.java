package rubidium.api.block;

import rubidium.api.registry.ResourceId;
import java.util.*;
import java.util.function.Consumer;

public class BlockDefinition {
    
    private final ResourceId id;
    private final String displayName;
    private final float hardness;
    private final float resistance;
    private final String material;
    private final boolean solid;
    private final boolean transparent;
    private final int lightLevel;
    private final Set<String> tags;
    private final Map<String, Object> properties;
    private final List<ItemDrop> drops;
    private final Consumer<BlockBreakContext> onBreak;
    private final Consumer<BlockInteractContext> onInteract;
    private final Consumer<BlockTickContext> onTick;
    
    private BlockDefinition(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.hardness = builder.hardness;
        this.resistance = builder.resistance;
        this.material = builder.material;
        this.solid = builder.solid;
        this.transparent = builder.transparent;
        this.lightLevel = builder.lightLevel;
        this.tags = Set.copyOf(builder.tags);
        this.properties = Map.copyOf(builder.properties);
        this.drops = List.copyOf(builder.drops);
        this.onBreak = builder.onBreak;
        this.onInteract = builder.onInteract;
        this.onTick = builder.onTick;
    }
    
    public ResourceId getId() { return id; }
    public String getDisplayName() { return displayName; }
    public float getHardness() { return hardness; }
    public float getResistance() { return resistance; }
    public String getMaterial() { return material; }
    public boolean isSolid() { return solid; }
    public boolean isTransparent() { return transparent; }
    public int getLightLevel() { return lightLevel; }
    public Set<String> getTags() { return tags; }
    public Map<String, Object> getProperties() { return properties; }
    public List<ItemDrop> getDrops() { return drops; }
    
    public void handleBreak(BlockBreakContext ctx) {
        if (onBreak != null) onBreak.accept(ctx);
    }
    
    public void handleInteract(BlockInteractContext ctx) {
        if (onInteract != null) onInteract.accept(ctx);
    }
    
    public void handleTick(BlockTickContext ctx) {
        if (onTick != null) onTick.accept(ctx);
    }
    
    public static Builder builder(String id) {
        return new Builder(ResourceId.of(id));
    }
    
    public static Builder builder(ResourceId id) {
        return new Builder(id);
    }
    
    public static class Builder {
        private final ResourceId id;
        private String displayName;
        private float hardness = 1.0f;
        private float resistance = 1.0f;
        private String material = "stone";
        private boolean solid = true;
        private boolean transparent = false;
        private int lightLevel = 0;
        private Set<String> tags = new HashSet<>();
        private Map<String, Object> properties = new HashMap<>();
        private List<ItemDrop> drops = new ArrayList<>();
        private Consumer<BlockBreakContext> onBreak;
        private Consumer<BlockInteractContext> onInteract;
        private Consumer<BlockTickContext> onTick;
        
        private Builder(ResourceId id) {
            this.id = id;
            this.displayName = id.path();
        }
        
        public Builder displayName(String name) { this.displayName = name; return this; }
        public Builder hardness(float hardness) { this.hardness = hardness; return this; }
        public Builder resistance(float resistance) { this.resistance = resistance; return this; }
        public Builder material(String material) { this.material = material; return this; }
        public Builder solid(boolean solid) { this.solid = solid; return this; }
        public Builder transparent(boolean transparent) { this.transparent = transparent; return this; }
        public Builder lightLevel(int level) { this.lightLevel = level; return this; }
        public Builder tag(String tag) { this.tags.add(tag); return this; }
        public Builder tags(String... tags) { this.tags.addAll(Arrays.asList(tags)); return this; }
        public Builder property(String key, Object value) { this.properties.put(key, value); return this; }
        public Builder drops(ItemDrop drop) { this.drops.add(drop); return this; }
        public Builder dropsSelf() { this.drops.add(new ItemDrop(id.toString(), 1, 1)); return this; }
        public Builder onBreak(Consumer<BlockBreakContext> handler) { this.onBreak = handler; return this; }
        public Builder onInteract(Consumer<BlockInteractContext> handler) { this.onInteract = handler; return this; }
        public Builder onTick(Consumer<BlockTickContext> handler) { this.onTick = handler; return this; }
        
        public BlockDefinition build() {
            return new BlockDefinition(this);
        }
    }
    
    public record ItemDrop(String itemId, int minCount, int maxCount) {}
    public record BlockBreakContext(Object player, int x, int y, int z, String world) {}
    public record BlockInteractContext(Object player, int x, int y, int z, String world, String hand) {}
    public record BlockTickContext(int x, int y, int z, String world, long tick) {}
}
