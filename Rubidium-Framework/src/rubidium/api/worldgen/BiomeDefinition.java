package rubidium.api.worldgen;

import rubidium.api.registry.ResourceId;
import java.util.*;

public class BiomeDefinition {
    
    private final ResourceId id;
    private final String displayName;
    private final float temperature;
    private final float humidity;
    private final int skyColor;
    private final int fogColor;
    private final int waterColor;
    private final int grassColor;
    private final int foliageColor;
    private final List<SpawnEntry> spawns;
    private final List<FeatureEntry> features;
    private final String surfaceBlock;
    private final String subsurfaceBlock;
    private final String underwaterBlock;
    
    private BiomeDefinition(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.temperature = builder.temperature;
        this.humidity = builder.humidity;
        this.skyColor = builder.skyColor;
        this.fogColor = builder.fogColor;
        this.waterColor = builder.waterColor;
        this.grassColor = builder.grassColor;
        this.foliageColor = builder.foliageColor;
        this.spawns = List.copyOf(builder.spawns);
        this.features = List.copyOf(builder.features);
        this.surfaceBlock = builder.surfaceBlock;
        this.subsurfaceBlock = builder.subsurfaceBlock;
        this.underwaterBlock = builder.underwaterBlock;
    }
    
    public ResourceId getId() { return id; }
    public String getDisplayName() { return displayName; }
    public float getTemperature() { return temperature; }
    public float getHumidity() { return humidity; }
    public int getSkyColor() { return skyColor; }
    public int getFogColor() { return fogColor; }
    public int getWaterColor() { return waterColor; }
    public int getGrassColor() { return grassColor; }
    public int getFoliageColor() { return foliageColor; }
    public List<SpawnEntry> getSpawns() { return spawns; }
    public List<FeatureEntry> getFeatures() { return features; }
    public String getSurfaceBlock() { return surfaceBlock; }
    public String getSubsurfaceBlock() { return subsurfaceBlock; }
    public String getUnderwaterBlock() { return underwaterBlock; }
    
    public static Builder builder(String id) {
        return new Builder(ResourceId.of(id));
    }
    
    public static Builder builder(ResourceId id) {
        return new Builder(id);
    }
    
    public static class Builder {
        private final ResourceId id;
        private String displayName;
        private float temperature = 0.5f;
        private float humidity = 0.5f;
        private int skyColor = 0x78A7FF;
        private int fogColor = 0xC0D8FF;
        private int waterColor = 0x3F76E4;
        private int grassColor = 0x91BD59;
        private int foliageColor = 0x77AB2F;
        private List<SpawnEntry> spawns = new ArrayList<>();
        private List<FeatureEntry> features = new ArrayList<>();
        private String surfaceBlock = "grass_block";
        private String subsurfaceBlock = "dirt";
        private String underwaterBlock = "sand";
        
        private Builder(ResourceId id) {
            this.id = id;
            this.displayName = id.path();
        }
        
        public Builder displayName(String name) { this.displayName = name; return this; }
        public Builder temperature(float temp) { this.temperature = temp; return this; }
        public Builder humidity(float hum) { this.humidity = hum; return this; }
        public Builder skyColor(int color) { this.skyColor = color; return this; }
        public Builder fogColor(int color) { this.fogColor = color; return this; }
        public Builder waterColor(int color) { this.waterColor = color; return this; }
        public Builder grassColor(int color) { this.grassColor = color; return this; }
        public Builder foliageColor(int color) { this.foliageColor = color; return this; }
        public Builder spawn(String entityId, int weight, int minGroup, int maxGroup) {
            this.spawns.add(new SpawnEntry(entityId, weight, minGroup, maxGroup));
            return this;
        }
        public Builder feature(String featureId, FeatureStep step) {
            this.features.add(new FeatureEntry(featureId, step));
            return this;
        }
        public Builder surface(String block) { this.surfaceBlock = block; return this; }
        public Builder subsurface(String block) { this.subsurfaceBlock = block; return this; }
        public Builder underwater(String block) { this.underwaterBlock = block; return this; }
        
        public BiomeDefinition build() {
            return new BiomeDefinition(this);
        }
    }
    
    public record SpawnEntry(String entityId, int weight, int minGroup, int maxGroup) {}
    public record FeatureEntry(String featureId, FeatureStep step) {}
    
    public enum FeatureStep {
        RAW_GENERATION, LAKES, LOCAL_MODIFICATIONS, UNDERGROUND_STRUCTURES,
        SURFACE_STRUCTURES, STRONGHOLDS, UNDERGROUND_ORES, UNDERGROUND_DECORATION,
        FLUID_SPRINGS, VEGETAL_DECORATION, TOP_LAYER_MODIFICATION
    }
}
