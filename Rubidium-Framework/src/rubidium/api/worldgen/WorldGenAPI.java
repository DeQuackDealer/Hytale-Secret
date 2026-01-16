package rubidium.api.worldgen;

import rubidium.api.registry.Registries;
import rubidium.api.registry.ResourceId;
import java.util.Collection;
import java.util.Optional;

public final class WorldGenAPI {
    
    private WorldGenAPI() {}
    
    public static BiomeDefinition register(BiomeDefinition biome) {
        return Registries.BIOMES.register(biome.getId(), biome);
    }
    
    public static BiomeDefinition.Builder create(String id) {
        return BiomeDefinition.builder(id);
    }
    
    public static Optional<BiomeDefinition> get(String id) {
        return Registries.BIOMES.get(id);
    }
    
    public static Collection<BiomeDefinition> all() {
        return Registries.BIOMES.values();
    }
    
    public static BiomeDefinition forest(String id) {
        return create(id)
            .temperature(0.7f)
            .humidity(0.8f)
            .grassColor(0x79C05A)
            .foliageColor(0x59AE30)
            .surface("grass_block")
            .subsurface("dirt")
            .build();
    }
    
    public static BiomeDefinition desert(String id) {
        return create(id)
            .temperature(2.0f)
            .humidity(0.0f)
            .skyColor(0x6EB1FF)
            .grassColor(0xBFB755)
            .foliageColor(0xAEA42A)
            .surface("sand")
            .subsurface("sandstone")
            .build();
    }
    
    public static BiomeDefinition jungle(String id) {
        return create(id)
            .temperature(0.95f)
            .humidity(0.9f)
            .grassColor(0x59C93C)
            .foliageColor(0x30BB0B)
            .surface("grass_block")
            .subsurface("dirt")
            .build();
    }
    
    public static BiomeDefinition tundra(String id) {
        return create(id)
            .temperature(0.0f)
            .humidity(0.5f)
            .grassColor(0x80B497)
            .foliageColor(0x60A17B)
            .surface("snow_block")
            .subsurface("dirt")
            .build();
    }
    
    public static BiomeDefinition swamp(String id) {
        return create(id)
            .temperature(0.8f)
            .humidity(0.9f)
            .waterColor(0x617B64)
            .grassColor(0x6A7039)
            .foliageColor(0x6A7039)
            .fogColor(0xC0D8C0)
            .surface("grass_block")
            .subsurface("dirt")
            .underwater("clay")
            .build();
    }
    
    public static BiomeDefinition prehistoric(String id) {
        return create(id)
            .displayName("Prehistoric " + id)
            .temperature(0.9f)
            .humidity(0.7f)
            .grassColor(0x4A7023)
            .foliageColor(0x2D5016)
            .skyColor(0x7BA4DB)
            .fogColor(0x9ABFDB)
            .surface("grass_block")
            .subsurface("dirt")
            .build();
    }
    
    public static BiomeDefinition volcanic(String id) {
        return create(id)
            .temperature(2.0f)
            .humidity(0.0f)
            .skyColor(0xFF6B35)
            .fogColor(0x3D1308)
            .grassColor(0x2D2D2D)
            .foliageColor(0x1A1A1A)
            .surface("basalt")
            .subsurface("blackite")
            .build();
    }
}
