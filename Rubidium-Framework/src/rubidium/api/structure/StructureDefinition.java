package rubidium.api.structure;

import rubidium.api.registry.ResourceId;
import java.util.*;

public class StructureDefinition {
    
    private final ResourceId id;
    private final String displayName;
    private final int sizeX, sizeY, sizeZ;
    private final List<BlockEntry> blocks;
    private final List<EntityEntry> entities;
    private final Set<String> biomes;
    private final double spawnChance;
    private final int minY, maxY;
    private final boolean generateAir;
    
    private StructureDefinition(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.sizeX = builder.sizeX;
        this.sizeY = builder.sizeY;
        this.sizeZ = builder.sizeZ;
        this.blocks = List.copyOf(builder.blocks);
        this.entities = List.copyOf(builder.entities);
        this.biomes = Set.copyOf(builder.biomes);
        this.spawnChance = builder.spawnChance;
        this.minY = builder.minY;
        this.maxY = builder.maxY;
        this.generateAir = builder.generateAir;
    }
    
    public ResourceId getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getSizeX() { return sizeX; }
    public int getSizeY() { return sizeY; }
    public int getSizeZ() { return sizeZ; }
    public List<BlockEntry> getBlocks() { return blocks; }
    public List<EntityEntry> getEntities() { return entities; }
    public Set<String> getBiomes() { return biomes; }
    public double getSpawnChance() { return spawnChance; }
    public int getMinY() { return minY; }
    public int getMaxY() { return maxY; }
    public boolean shouldGenerateAir() { return generateAir; }
    
    public static Builder builder(String id) {
        return new Builder(ResourceId.of(id));
    }
    
    public static Builder builder(ResourceId id) {
        return new Builder(id);
    }
    
    public static class Builder {
        private final ResourceId id;
        private String displayName;
        private int sizeX = 16, sizeY = 16, sizeZ = 16;
        private List<BlockEntry> blocks = new ArrayList<>();
        private List<EntityEntry> entities = new ArrayList<>();
        private Set<String> biomes = new HashSet<>();
        private double spawnChance = 0.01;
        private int minY = 0, maxY = 256;
        private boolean generateAir = false;
        
        private Builder(ResourceId id) {
            this.id = id;
            this.displayName = id.path();
        }
        
        public Builder displayName(String name) { this.displayName = name; return this; }
        public Builder size(int x, int y, int z) { this.sizeX = x; this.sizeY = y; this.sizeZ = z; return this; }
        public Builder block(int x, int y, int z, String blockId) { 
            this.blocks.add(new BlockEntry(x, y, z, blockId, Map.of())); 
            return this; 
        }
        public Builder block(int x, int y, int z, String blockId, Map<String, Object> state) { 
            this.blocks.add(new BlockEntry(x, y, z, blockId, state)); 
            return this; 
        }
        public Builder entity(double x, double y, double z, String entityId) {
            this.entities.add(new EntityEntry(x, y, z, entityId, Map.of()));
            return this;
        }
        public Builder biome(String biome) { this.biomes.add(biome); return this; }
        public Builder allBiomes() { this.biomes.clear(); return this; }
        public Builder spawnChance(double chance) { this.spawnChance = chance; return this; }
        public Builder heightRange(int min, int max) { this.minY = min; this.maxY = max; return this; }
        public Builder generateAir(boolean gen) { this.generateAir = gen; return this; }
        
        public StructureDefinition build() {
            return new StructureDefinition(this);
        }
    }
    
    public record BlockEntry(int x, int y, int z, String blockId, Map<String, Object> state) {}
    public record EntityEntry(double x, double y, double z, String entityId, Map<String, Object> nbt) {}
}
