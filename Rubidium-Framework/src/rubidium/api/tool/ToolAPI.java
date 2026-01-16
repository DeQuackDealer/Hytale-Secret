package rubidium.api.tool;

import rubidium.api.item.ItemAPI;
import rubidium.api.item.ItemDefinition;
import rubidium.api.registry.Registries;

public final class ToolAPI {
    
    private ToolAPI() {}
    
    public static ItemDefinition pickaxe(String id, ToolTier tier) {
        return ItemAPI.create(id)
            .type(ItemDefinition.ItemType.TOOL)
            .displayName(tier.name + " Pickaxe")
            .maxStackSize(1)
            .durability(tier.durability)
            .attribute("mining_speed", tier.speed)
            .attribute("attack_damage", tier.damage + 1)
            .attribute("attack_speed", 1.2)
            .tag("tool")
            .tag("pickaxe")
            .tag("tier_" + tier.name.toLowerCase())
            .build();
    }
    
    public static ItemDefinition axe(String id, ToolTier tier) {
        return ItemAPI.create(id)
            .type(ItemDefinition.ItemType.TOOL)
            .displayName(tier.name + " Axe")
            .maxStackSize(1)
            .durability(tier.durability)
            .attribute("mining_speed", tier.speed)
            .attribute("attack_damage", tier.damage + 6)
            .attribute("attack_speed", 0.9)
            .tag("tool")
            .tag("axe")
            .tag("tier_" + tier.name.toLowerCase())
            .build();
    }
    
    public static ItemDefinition shovel(String id, ToolTier tier) {
        return ItemAPI.create(id)
            .type(ItemDefinition.ItemType.TOOL)
            .displayName(tier.name + " Shovel")
            .maxStackSize(1)
            .durability(tier.durability)
            .attribute("mining_speed", tier.speed)
            .attribute("attack_damage", tier.damage + 1.5)
            .attribute("attack_speed", 1.0)
            .tag("tool")
            .tag("shovel")
            .tag("tier_" + tier.name.toLowerCase())
            .build();
    }
    
    public static ItemDefinition hoe(String id, ToolTier tier) {
        return ItemAPI.create(id)
            .type(ItemDefinition.ItemType.TOOL)
            .displayName(tier.name + " Hoe")
            .maxStackSize(1)
            .durability(tier.durability)
            .attribute("attack_damage", 1)
            .attribute("attack_speed", tier.speed)
            .tag("tool")
            .tag("hoe")
            .tag("tier_" + tier.name.toLowerCase())
            .build();
    }
    
    public static ItemDefinition sword(String id, ToolTier tier) {
        return ItemAPI.create(id)
            .type(ItemDefinition.ItemType.WEAPON)
            .displayName(tier.name + " Sword")
            .maxStackSize(1)
            .durability(tier.durability)
            .attribute("attack_damage", tier.damage + 4)
            .attribute("attack_speed", 1.6)
            .tag("weapon")
            .tag("sword")
            .tag("tier_" + tier.name.toLowerCase())
            .build();
    }
    
    public static ItemDefinition bow(String id, int durability, double drawSpeed, double damage) {
        return ItemAPI.create(id)
            .type(ItemDefinition.ItemType.WEAPON)
            .maxStackSize(1)
            .durability(durability)
            .attribute("draw_speed", drawSpeed)
            .attribute("arrow_damage", damage)
            .tag("weapon")
            .tag("bow")
            .tag("ranged")
            .build();
    }
    
    public static ItemDefinition crossbow(String id, int durability, double damage) {
        return ItemAPI.create(id)
            .type(ItemDefinition.ItemType.WEAPON)
            .maxStackSize(1)
            .durability(durability)
            .attribute("arrow_damage", damage)
            .tag("weapon")
            .tag("crossbow")
            .tag("ranged")
            .build();
    }
    
    public static ItemDefinition spear(String id, int durability, double damage) {
        return ItemAPI.create(id)
            .type(ItemDefinition.ItemType.WEAPON)
            .maxStackSize(1)
            .durability(durability)
            .attribute("attack_damage", damage)
            .attribute("attack_speed", 1.1)
            .attribute("reach", 4.0)
            .tag("weapon")
            .tag("spear")
            .tag("polearm")
            .build();
    }
    
    public static ItemDefinition hammer(String id, int durability, double damage) {
        return ItemAPI.create(id)
            .type(ItemDefinition.ItemType.WEAPON)
            .maxStackSize(1)
            .durability(durability)
            .attribute("attack_damage", damage)
            .attribute("attack_speed", 0.6)
            .attribute("knockback", 2.0)
            .tag("weapon")
            .tag("hammer")
            .tag("blunt")
            .build();
    }
    
    public static ItemDefinition shield(String id, int durability, double blockStrength) {
        return ItemAPI.create(id)
            .type(ItemDefinition.ItemType.ARMOR)
            .maxStackSize(1)
            .durability(durability)
            .attribute("block_strength", blockStrength)
            .tag("shield")
            .tag("offhand")
            .build();
    }
    
    public enum ToolTier {
        WOOD("Wood", 59, 2.0, 0),
        STONE("Stone", 131, 4.0, 1),
        IRON("Iron", 250, 6.0, 2),
        GOLD("Gold", 32, 12.0, 0),
        DIAMOND("Diamond", 1561, 8.0, 3),
        NETHERITE("Netherite", 2031, 9.0, 4),
        BONE("Bone", 150, 5.0, 1),
        OBSIDIAN("Obsidian", 2000, 6.0, 4),
        AMBER("Amber", 800, 7.0, 2),
        FOSSIL("Fossil", 500, 5.5, 2);
        
        public final String name;
        public final int durability;
        public final double speed;
        public final double damage;
        
        ToolTier(String name, int durability, double speed, double damage) {
            this.name = name;
            this.durability = durability;
            this.speed = speed;
            this.damage = damage;
        }
    }
}
