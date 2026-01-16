package rubidium.api.recipe;

import rubidium.api.registry.Registries;
import rubidium.api.registry.ResourceId;
import java.util.Collection;
import java.util.Optional;

public final class RecipeAPI {
    
    private RecipeAPI() {}
    
    public static RecipeDefinition register(RecipeDefinition recipe) {
        return Registries.RECIPES.register(recipe.getId(), recipe);
    }
    
    public static RecipeDefinition.Builder shaped(String id, String result) {
        return RecipeDefinition.shaped(id, result);
    }
    
    public static RecipeDefinition.Builder shapeless(String id, String result) {
        return RecipeDefinition.shapeless(id, result);
    }
    
    public static RecipeDefinition.Builder smelting(String id, String result) {
        return RecipeDefinition.smelting(id, result);
    }
    
    public static Optional<RecipeDefinition> get(String id) {
        return Registries.RECIPES.get(id);
    }
    
    public static Collection<RecipeDefinition> all() {
        return Registries.RECIPES.values();
    }
    
    public static RecipeDefinition sword(String id, String material, String result) {
        return shaped(id, result)
            .pattern(" M ", " M ", " S ")
            .key('M', material)
            .key('S', "stick")
            .build();
    }
    
    public static RecipeDefinition pickaxe(String id, String material, String result) {
        return shaped(id, result)
            .pattern("MMM", " S ", " S ")
            .key('M', material)
            .key('S', "stick")
            .build();
    }
    
    public static RecipeDefinition axe(String id, String material, String result) {
        return shaped(id, result)
            .pattern("MM ", "MS ", " S ")
            .key('M', material)
            .key('S', "stick")
            .build();
    }
    
    public static RecipeDefinition shovel(String id, String material, String result) {
        return shaped(id, result)
            .pattern(" M ", " S ", " S ")
            .key('M', material)
            .key('S', "stick")
            .build();
    }
    
    public static RecipeDefinition hoe(String id, String material, String result) {
        return shaped(id, result)
            .pattern("MM ", " S ", " S ")
            .key('M', material)
            .key('S', "stick")
            .build();
    }
    
    public static RecipeDefinition helmet(String id, String material, String result) {
        return shaped(id, result)
            .pattern("MMM", "M M")
            .key('M', material)
            .build();
    }
    
    public static RecipeDefinition chestplate(String id, String material, String result) {
        return shaped(id, result)
            .pattern("M M", "MMM", "MMM")
            .key('M', material)
            .build();
    }
    
    public static RecipeDefinition leggings(String id, String material, String result) {
        return shaped(id, result)
            .pattern("MMM", "M M", "M M")
            .key('M', material)
            .build();
    }
    
    public static RecipeDefinition boots(String id, String material, String result) {
        return shaped(id, result)
            .pattern("M M", "M M")
            .key('M', material)
            .build();
    }
    
    public static RecipeDefinition block(String id, String material, String result) {
        return shaped(id, result)
            .pattern("MMM", "MMM", "MMM")
            .key('M', material)
            .build();
    }
    
    public static RecipeDefinition unpack(String id, String block, String result, int count) {
        return shapeless(id, result).count(count).ingredient(block).build();
    }
}
