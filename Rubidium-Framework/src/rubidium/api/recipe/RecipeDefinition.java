package rubidium.api.recipe;

import rubidium.api.registry.ResourceId;
import java.util.*;

public class RecipeDefinition {
    
    private final ResourceId id;
    private final RecipeType type;
    private final String result;
    private final int resultCount;
    private final List<Ingredient> ingredients;
    private final String[] pattern;
    private final Map<Character, String> key;
    private final String group;
    
    private RecipeDefinition(Builder builder) {
        this.id = builder.id;
        this.type = builder.type;
        this.result = builder.result;
        this.resultCount = builder.resultCount;
        this.ingredients = List.copyOf(builder.ingredients);
        this.pattern = builder.pattern;
        this.key = builder.key != null ? Map.copyOf(builder.key) : Map.of();
        this.group = builder.group;
    }
    
    public ResourceId getId() { return id; }
    public RecipeType getType() { return type; }
    public String getResult() { return result; }
    public int getResultCount() { return resultCount; }
    public List<Ingredient> getIngredients() { return ingredients; }
    public String[] getPattern() { return pattern; }
    public Map<Character, String> getKey() { return key; }
    public String getGroup() { return group; }
    
    public static Builder shaped(String id, String result) {
        return new Builder(ResourceId.of(id), RecipeType.SHAPED, result);
    }
    
    public static Builder shapeless(String id, String result) {
        return new Builder(ResourceId.of(id), RecipeType.SHAPELESS, result);
    }
    
    public static Builder smelting(String id, String result) {
        return new Builder(ResourceId.of(id), RecipeType.SMELTING, result);
    }
    
    public enum RecipeType {
        SHAPED, SHAPELESS, SMELTING, SMOKING, BLASTING, CAMPFIRE, STONECUTTING, SMITHING
    }
    
    public static class Builder {
        private final ResourceId id;
        private final RecipeType type;
        private final String result;
        private int resultCount = 1;
        private List<Ingredient> ingredients = new ArrayList<>();
        private String[] pattern;
        private Map<Character, String> key = new HashMap<>();
        private String group;
        
        private Builder(ResourceId id, RecipeType type, String result) {
            this.id = id;
            this.type = type;
            this.result = result;
        }
        
        public Builder count(int count) { this.resultCount = count; return this; }
        public Builder pattern(String... rows) { this.pattern = rows; return this; }
        public Builder key(char symbol, String itemId) { this.key.put(symbol, itemId); return this; }
        public Builder ingredient(String itemId) { this.ingredients.add(new Ingredient(itemId, 1)); return this; }
        public Builder ingredient(String itemId, int count) { this.ingredients.add(new Ingredient(itemId, count)); return this; }
        public Builder group(String group) { this.group = group; return this; }
        
        public RecipeDefinition build() {
            return new RecipeDefinition(this);
        }
    }
    
    public record Ingredient(String itemId, int count) {}
}
