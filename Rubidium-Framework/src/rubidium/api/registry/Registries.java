package rubidium.api.registry;

import rubidium.api.block.BlockDefinition;
import rubidium.api.entity.EntityDefinition;
import rubidium.api.item.ItemDefinition;
import rubidium.api.structure.StructureDefinition;
import rubidium.api.recipe.RecipeDefinition;
import rubidium.api.particle.ParticleDefinition;
import rubidium.api.sound.SoundDefinition;
import rubidium.api.worldgen.BiomeDefinition;

public final class Registries {
    
    private Registries() {}
    
    public static final Registry<BlockDefinition> BLOCKS = new Registry<>("Block");
    public static final Registry<ItemDefinition> ITEMS = new Registry<>("Item");
    public static final Registry<EntityDefinition> ENTITIES = new Registry<>("Entity");
    public static final Registry<StructureDefinition> STRUCTURES = new Registry<>("Structure");
    public static final Registry<RecipeDefinition> RECIPES = new Registry<>("Recipe");
    public static final Registry<ParticleDefinition> PARTICLES = new Registry<>("Particle");
    public static final Registry<SoundDefinition> SOUNDS = new Registry<>("Sound");
    public static final Registry<BiomeDefinition> BIOMES = new Registry<>("Biome");
}
