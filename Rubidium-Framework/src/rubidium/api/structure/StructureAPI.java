package rubidium.api.structure;

import rubidium.api.registry.Registries;
import rubidium.api.registry.ResourceId;
import java.util.Collection;
import java.util.Optional;

public final class StructureAPI {
    
    private StructureAPI() {}
    
    public static StructureDefinition register(StructureDefinition structure) {
        return Registries.STRUCTURES.register(structure.getId(), structure);
    }
    
    public static StructureDefinition.Builder create(String id) {
        return StructureDefinition.builder(id);
    }
    
    public static StructureDefinition.Builder create(String namespace, String path) {
        return StructureDefinition.builder(ResourceId.of(namespace, path));
    }
    
    public static Optional<StructureDefinition> get(String id) {
        return Registries.STRUCTURES.get(id);
    }
    
    public static StructureDefinition getOrThrow(String id) {
        return Registries.STRUCTURES.getOrThrow(ResourceId.parse(id));
    }
    
    public static Collection<StructureDefinition> all() {
        return Registries.STRUCTURES.values();
    }
    
    public static boolean exists(String id) {
        return Registries.STRUCTURES.contains(ResourceId.parse(id));
    }
    
    public static StructureDefinition.Builder house(String id) {
        return create(id)
            .size(7, 5, 7)
            .spawnChance(0.005)
            .heightRange(60, 100);
    }
    
    public static StructureDefinition.Builder dungeon(String id) {
        return create(id)
            .size(15, 8, 15)
            .spawnChance(0.002)
            .heightRange(10, 50)
            .generateAir(true);
    }
    
    public static StructureDefinition.Builder tower(String id) {
        return create(id)
            .size(9, 20, 9)
            .spawnChance(0.001)
            .heightRange(60, 100);
    }
    
    public static StructureDefinition.Builder ruins(String id) {
        return create(id)
            .size(12, 6, 12)
            .spawnChance(0.003)
            .heightRange(50, 120);
    }
    
    public static StructureDefinition.Builder dinoNest(String id, String dinoType) {
        return create(id)
            .displayName(dinoType + " Nest")
            .size(5, 2, 5)
            .spawnChance(0.01)
            .heightRange(60, 100)
            .entity(2.5, 1, 2.5, dinoType);
    }
}
