package dinomod;

import rubidium.api.RubidiumPlugin;
import rubidium.api.block.BlockAPI;
import rubidium.api.block.BlockDefinition;
import rubidium.api.entity.EntityAPI;
import rubidium.api.entity.EntityDefinition;
import rubidium.api.item.ItemAPI;
import rubidium.api.item.ItemDefinition;
import rubidium.api.tool.ToolAPI;
import rubidium.api.recipe.RecipeAPI;
import rubidium.api.particle.ParticleAPI;
import rubidium.api.sound.SoundAPI;
import rubidium.api.worldgen.WorldGenAPI;
import rubidium.api.structure.StructureAPI;
import rubidium.api.ui.UIAPI;

/**
 * DinoMod - Example plugin demonstrating all Rubidium APIs
 * 
 * This plugin adds dinosaurs, prehistoric blocks, tools, and biomes
 * to show how to use the Rubidium Framework APIs.
 */
public class DinoModPlugin extends RubidiumPlugin {
    
    @Override
    public void onEnable() {
        getLogger().info("DinoMod is loading...");
        
        registerBlocks();
        registerItems();
        registerEntities();
        registerRecipes();
        registerBiomes();
        registerStructures();
        registerEffects();
        
        getLogger().info("DinoMod loaded successfully!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("DinoMod disabled.");
    }
    
    private void registerBlocks() {
        // Fossil Ore - drops bones
        BlockAPI.register(
            BlockAPI.ore("dinomod:fossil_ore", "dinomod:bone_fragment", 1, 3)
        );
        
        // Amber Block - decorative, transparent
        BlockAPI.register(
            BlockAPI.create("dinomod:amber_block")
                .displayName("Amber Block")
                .material("glass")
                .transparent(true)
                .hardness(1.0f)
                .lightLevel(4)
                .dropsSelf()
                .build()
        );
        
        // Prehistoric Stone variants
        BlockAPI.register(BlockAPI.stone("dinomod:prehistoric_stone"));
        BlockAPI.register(BlockAPI.stone("dinomod:volcanic_rock"));
        
        // Dino Nest block
        BlockAPI.register(
            BlockAPI.create("dinomod:dino_nest")
                .displayName("Dinosaur Nest")
                .material("organic")
                .hardness(0.5f)
                .onInteract(ctx -> {
                    // Spawn baby dino when interacted with egg
                })
                .build()
        );
    }
    
    private void registerItems() {
        // Basic materials
        ItemAPI.register(ItemAPI.material("dinomod:bone_fragment"));
        ItemAPI.register(ItemAPI.material("dinomod:amber_shard"));
        ItemAPI.register(ItemAPI.material("dinomod:dino_scale"));
        ItemAPI.register(ItemAPI.material("dinomod:raptor_claw"));
        ItemAPI.register(ItemAPI.material("dinomod:rex_tooth"));
        
        // Food items
        ItemAPI.register(ItemAPI.food("dinomod:raw_dino_meat", 3, 0.3f));
        ItemAPI.register(ItemAPI.food("dinomod:cooked_dino_meat", 8, 0.8f));
        ItemAPI.register(ItemAPI.food("dinomod:dino_egg", 4, 0.5f));
        
        // Fossil tier tools
        ItemAPI.register(ToolAPI.pickaxe("dinomod:fossil_pickaxe", ToolAPI.ToolTier.FOSSIL));
        ItemAPI.register(ToolAPI.axe("dinomod:fossil_axe", ToolAPI.ToolTier.FOSSIL));
        ItemAPI.register(ToolAPI.sword("dinomod:fossil_sword", ToolAPI.ToolTier.FOSSIL));
        ItemAPI.register(ToolAPI.shovel("dinomod:fossil_shovel", ToolAPI.ToolTier.FOSSIL));
        
        // Special weapons
        ItemAPI.register(ToolAPI.spear("dinomod:bone_spear", 150, 6.0));
        ItemAPI.register(ToolAPI.hammer("dinomod:rex_hammer", 500, 12.0));
        
        // Rare items
        ItemAPI.register(
            ItemAPI.rare("dinomod:preserved_dna", ItemDefinition.Rarity.LEGENDARY,
                "Ancient DNA preserved in amber",
                "Can be used to clone dinosaurs")
        );
    }
    
    private void registerEntities() {
        // Velociraptor - fast, hostile pack hunter
        EntityAPI.register(
            EntityAPI.dinosaur("dinomod:velociraptor", 30, 6, true)
        );
        
        // T-Rex - boss-level predator
        EntityAPI.register(
            EntityAPI.boss("dinomod:tyrannosaurus", 300, 20)
        );
        
        // Triceratops - defensive herbivore
        EntityAPI.register(
            EntityAPI.create("dinomod:triceratops")
                .displayName("Triceratops")
                .type(EntityDefinition.EntityType.ANIMAL)
                .maxHealth(80)
                .attackDamage(10)
                .size(2.0, 2.5)
                .tag("dinosaur")
                .tag("herbivore")
                .ai(EntityDefinition.AITask.wander(5))
                .ai(EntityDefinition.AITask.flee(2, 16.0))
                .onDamage(ctx -> {
                    // Becomes aggressive when attacked
                })
                .build()
        );
        
        // Pteranodon - flying dinosaur
        EntityAPI.register(
            EntityAPI.create("dinomod:pteranodon")
                .displayName("Pteranodon")
                .type(EntityDefinition.EntityType.ANIMAL)
                .maxHealth(20)
                .movementSpeed(0.4)
                .size(2.0, 1.0)
                .tag("dinosaur")
                .tag("flying")
                .attribute("can_fly", true)
                .build()
        );
        
        // Brachiosaurus - gentle giant
        EntityAPI.register(
            EntityAPI.dinosaur("dinomod:brachiosaurus", 200, 4, false)
        );
        
        // Compsognathus - tiny pack animal
        EntityAPI.register(
            EntityAPI.create("dinomod:compsognathus")
                .displayName("Compsognathus")
                .type(EntityDefinition.EntityType.ANIMAL)
                .maxHealth(8)
                .attackDamage(2)
                .size(0.4, 0.3)
                .tameable()
                .tag("dinosaur")
                .tag("small")
                .build()
        );
    }
    
    private void registerRecipes() {
        // Fossil Pickaxe
        RecipeAPI.register(
            RecipeAPI.pickaxe("dinomod:craft_fossil_pickaxe", "dinomod:bone_fragment", "dinomod:fossil_pickaxe")
        );
        
        // Fossil Sword  
        RecipeAPI.register(
            RecipeAPI.sword("dinomod:craft_fossil_sword", "dinomod:bone_fragment", "dinomod:fossil_sword")
        );
        
        // Bone Spear
        RecipeAPI.register(
            RecipeAPI.shaped("dinomod:craft_bone_spear", "dinomod:bone_spear")
                .pattern("  B", " S ", "S  ")
                .key('B', "dinomod:bone_fragment")
                .key('S', "stick")
                .build()
        );
        
        // Rex Hammer
        RecipeAPI.register(
            RecipeAPI.shaped("dinomod:craft_rex_hammer", "dinomod:rex_hammer")
                .pattern("TTT", "TST", " S ")
                .key('T', "dinomod:rex_tooth")
                .key('S', "stick")
                .build()
        );
        
        // Cook dino meat
        RecipeAPI.register(
            RecipeAPI.smelting("dinomod:cook_dino_meat", "dinomod:cooked_dino_meat")
                .ingredient("dinomod:raw_dino_meat")
                .build()
        );
        
        // Amber block from shards
        RecipeAPI.register(
            RecipeAPI.block("dinomod:craft_amber_block", "dinomod:amber_shard", "dinomod:amber_block")
        );
    }
    
    private void registerBiomes() {
        // Prehistoric Jungle
        WorldGenAPI.register(
            WorldGenAPI.prehistoric("dinomod:prehistoric_jungle")
        );
        
        // Volcanic Wasteland
        WorldGenAPI.register(
            WorldGenAPI.volcanic("dinomod:volcanic_wasteland")
        );
        
        // Fossil Desert
        WorldGenAPI.register(
            WorldGenAPI.create("dinomod:fossil_desert")
                .displayName("Fossil Desert")
                .temperature(2.0f)
                .humidity(0.0f)
                .grassColor(0xC4A35A)
                .surface("sand")
                .subsurface("sandstone")
                .spawn("dinomod:velociraptor", 10, 1, 3)
                .spawn("dinomod:compsognathus", 15, 2, 5)
                .build()
        );
    }
    
    private void registerStructures() {
        // Dino Nest structures
        StructureAPI.register(
            StructureAPI.dinoNest("dinomod:raptor_nest", "dinomod:velociraptor")
                .biome("dinomod:prehistoric_jungle")
                .build()
        );
        
        StructureAPI.register(
            StructureAPI.dinoNest("dinomod:rex_nest", "dinomod:tyrannosaurus")
                .biome("dinomod:volcanic_wasteland")
                .spawnChance(0.001)
                .build()
        );
        
        // Fossil Site
        StructureAPI.register(
            StructureAPI.ruins("dinomod:fossil_site")
                .displayName("Fossil Excavation Site")
                .biome("dinomod:fossil_desert")
                .block(0, 0, 0, "dinomod:fossil_ore")
                .block(1, 0, 0, "dinomod:fossil_ore")
                .block(0, 0, 1, "dinomod:fossil_ore")
                .build()
        );
    }
    
    private void registerEffects() {
        // Particles
        ParticleAPI.register(ParticleAPI.dust("dinomod:amber_sparkle", 0xFFB300));
        ParticleAPI.register(ParticleAPI.smoke("dinomod:volcanic_smoke"));
        ParticleAPI.register(ParticleAPI.blood("dinomod:dino_blood"));
        
        // Sounds
        SoundAPI.register(SoundAPI.dinoRoar("dinomod:rex_roar", "sounds/rex_roar.ogg"));
        SoundAPI.register(SoundAPI.mob("dinomod:raptor_call", "sounds/raptor1.ogg", "sounds/raptor2.ogg"));
        SoundAPI.register(SoundAPI.ambient("dinomod:prehistoric_ambience", "sounds/jungle_ambience.ogg"));
    }
}
