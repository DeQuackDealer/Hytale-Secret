package rubidium.api.block;

import rubidium.api.registry.Registries;
import rubidium.api.registry.ResourceId;
import java.util.Collection;
import java.util.Optional;

public final class BlockAPI {
    
    private BlockAPI() {}
    
    public static BlockDefinition register(BlockDefinition block) {
        return Registries.BLOCKS.register(block.getId(), block);
    }
    
    public static BlockDefinition register(BlockDefinition.Builder builder) {
        return register(builder.build());
    }
    
    public static BlockDefinition.Builder create(String id) {
        return BlockDefinition.builder(id);
    }
    
    public static BlockDefinition.Builder create(String namespace, String path) {
        return BlockDefinition.builder(ResourceId.of(namespace, path));
    }
    
    public static Optional<BlockDefinition> get(String id) {
        return Registries.BLOCKS.get(id);
    }
    
    public static BlockDefinition getOrThrow(String id) {
        return Registries.BLOCKS.getOrThrow(ResourceId.parse(id));
    }
    
    public static Collection<BlockDefinition> all() {
        return Registries.BLOCKS.values();
    }
    
    public static boolean exists(String id) {
        return Registries.BLOCKS.contains(ResourceId.parse(id));
    }
    
    public static void unregister(String id) {
        Registries.BLOCKS.unregister(ResourceId.parse(id));
    }
    
    public static BlockDefinition stone(String id) {
        return create(id).material("stone").hardness(1.5f).resistance(6.0f).dropsSelf().build();
    }
    
    public static BlockDefinition wood(String id) {
        return create(id).material("wood").hardness(2.0f).resistance(3.0f).dropsSelf().build();
    }
    
    public static BlockDefinition ore(String id, String dropItem, int minDrop, int maxDrop) {
        return create(id)
            .material("stone")
            .hardness(3.0f)
            .resistance(3.0f)
            .drops(new BlockDefinition.ItemDrop(dropItem, minDrop, maxDrop))
            .build();
    }
    
    public static BlockDefinition light(String id, int level) {
        return create(id).lightLevel(level).dropsSelf().build();
    }
    
    public static BlockDefinition glass(String id) {
        return create(id).material("glass").transparent(true).hardness(0.3f).build();
    }
}
