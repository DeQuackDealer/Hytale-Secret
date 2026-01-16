package rubidium.api.item;

import rubidium.api.registry.Registries;
import rubidium.api.registry.ResourceId;
import java.util.Collection;
import java.util.Optional;

public final class ItemAPI {
    
    private ItemAPI() {}
    
    public static ItemDefinition register(ItemDefinition item) {
        return Registries.ITEMS.register(item.getId(), item);
    }
    
    public static ItemDefinition.Builder create(String id) {
        return ItemDefinition.builder(id);
    }
    
    public static ItemDefinition.Builder create(String namespace, String path) {
        return ItemDefinition.builder(ResourceId.of(namespace, path));
    }
    
    public static Optional<ItemDefinition> get(String id) {
        return Registries.ITEMS.get(id);
    }
    
    public static ItemDefinition getOrThrow(String id) {
        return Registries.ITEMS.getOrThrow(ResourceId.parse(id));
    }
    
    public static Collection<ItemDefinition> all() {
        return Registries.ITEMS.values();
    }
    
    public static boolean exists(String id) {
        return Registries.ITEMS.contains(ResourceId.parse(id));
    }
    
    public static ItemDefinition material(String id) {
        return create(id).type(ItemDefinition.ItemType.MATERIAL).build();
    }
    
    public static ItemDefinition food(String id, int nutrition, float saturation) {
        return create(id)
            .type(ItemDefinition.ItemType.FOOD)
            .maxStackSize(64)
            .attribute("nutrition", nutrition)
            .attribute("saturation", saturation)
            .build();
    }
    
    public static ItemDefinition tool(String id, int durability, double miningSpeed) {
        return create(id)
            .type(ItemDefinition.ItemType.TOOL)
            .maxStackSize(1)
            .durability(durability)
            .attribute("mining_speed", miningSpeed)
            .build();
    }
    
    public static ItemDefinition weapon(String id, int durability, double attackDamage, double attackSpeed) {
        return create(id)
            .type(ItemDefinition.ItemType.WEAPON)
            .maxStackSize(1)
            .durability(durability)
            .attribute("attack_damage", attackDamage)
            .attribute("attack_speed", attackSpeed)
            .build();
    }
    
    public static ItemDefinition armor(String id, int durability, double defense, String slot) {
        return create(id)
            .type(ItemDefinition.ItemType.ARMOR)
            .maxStackSize(1)
            .durability(durability)
            .attribute("defense", defense)
            .attribute("slot", slot.hashCode())
            .tag("armor")
            .tag("armor_" + slot)
            .build();
    }
    
    public static ItemDefinition rare(String id, ItemDefinition.Rarity rarity, String... loreLines) {
        return create(id)
            .rarity(rarity)
            .maxStackSize(1)
            .lore(loreLines)
            .build();
    }
}
