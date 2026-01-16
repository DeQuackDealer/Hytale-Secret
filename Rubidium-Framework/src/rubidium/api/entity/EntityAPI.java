package rubidium.api.entity;

import rubidium.api.registry.Registries;
import rubidium.api.registry.ResourceId;
import java.util.Collection;
import java.util.Optional;

public final class EntityAPI {
    
    private EntityAPI() {}
    
    public static EntityDefinition register(EntityDefinition entity) {
        return Registries.ENTITIES.register(entity.getId(), entity);
    }
    
    public static EntityDefinition.Builder create(String id) {
        return EntityDefinition.builder(id);
    }
    
    public static EntityDefinition.Builder create(String namespace, String path) {
        return EntityDefinition.builder(ResourceId.of(namespace, path));
    }
    
    public static Optional<EntityDefinition> get(String id) {
        return Registries.ENTITIES.get(id);
    }
    
    public static EntityDefinition getOrThrow(String id) {
        return Registries.ENTITIES.getOrThrow(ResourceId.parse(id));
    }
    
    public static Collection<EntityDefinition> all() {
        return Registries.ENTITIES.values();
    }
    
    public static boolean exists(String id) {
        return Registries.ENTITIES.contains(ResourceId.parse(id));
    }
    
    public static void unregister(String id) {
        Registries.ENTITIES.unregister(ResourceId.parse(id));
    }
    
    public static EntityDefinition monster(String id, double health, double damage) {
        return create(id)
            .type(EntityDefinition.EntityType.MONSTER)
            .maxHealth(health)
            .attackDamage(damage)
            .hostile()
            .ai(EntityDefinition.AITask.wander(5))
            .ai(EntityDefinition.AITask.attack(2, 16.0))
            .ai(EntityDefinition.AITask.lookAtPlayer(8))
            .build();
    }
    
    public static EntityDefinition animal(String id, double health) {
        return create(id)
            .type(EntityDefinition.EntityType.ANIMAL)
            .maxHealth(health)
            .ai(EntityDefinition.AITask.wander(5))
            .ai(EntityDefinition.AITask.flee(1, 8.0))
            .ai(EntityDefinition.AITask.lookAtPlayer(8))
            .build();
    }
    
    public static EntityDefinition npc(String id, String displayName) {
        return create(id)
            .type(EntityDefinition.EntityType.NPC)
            .displayName(displayName)
            .maxHealth(20)
            .ai(EntityDefinition.AITask.lookAtPlayer(1))
            .build();
    }
    
    public static EntityDefinition boss(String id, double health, double damage) {
        return create(id)
            .type(EntityDefinition.EntityType.MONSTER)
            .displayName(id.toUpperCase())
            .maxHealth(health)
            .attackDamage(damage)
            .size(2.0, 4.0)
            .hostile()
            .tag("boss")
            .ai(EntityDefinition.AITask.attack(1, 32.0))
            .build();
    }
    
    public static EntityDefinition dinosaur(String id, double health, double damage, boolean hostile) {
        EntityDefinition.Builder builder = create(id)
            .type(hostile ? EntityDefinition.EntityType.MONSTER : EntityDefinition.EntityType.ANIMAL)
            .maxHealth(health)
            .attackDamage(damage)
            .tag("dinosaur")
            .ai(EntityDefinition.AITask.wander(5));
        
        if (hostile) {
            builder.hostile().ai(EntityDefinition.AITask.attack(2, 24.0));
        } else {
            builder.ai(EntityDefinition.AITask.flee(2, 12.0));
        }
        
        return builder.build();
    }
}
