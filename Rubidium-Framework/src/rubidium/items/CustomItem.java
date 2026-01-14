package rubidium.items;

import java.util.*;

/**
 * Custom item with behaviors, attributes, and NBT data.
 */
public class CustomItem {
    
    private final String id;
    private final String name;
    private final String material;
    private final int maxStack;
    private final String icon;
    private final List<String> lore;
    private final Map<String, Object> nbt;
    private final Map<String, Double> attributes;
    private final List<ItemBuilder.ItemEnchantment> enchantments;
    private final List<ItemBuilder.ItemBehavior> behaviors;
    private final String rarity;
    private final boolean glowing;
    private final boolean unbreakable;
    
    public CustomItem(String id, String name, String material, int maxStack, String icon,
                      List<String> lore, Map<String, Object> nbt, Map<String, Double> attributes,
                      List<ItemBuilder.ItemEnchantment> enchantments, 
                      List<ItemBuilder.ItemBehavior> behaviors,
                      String rarity, boolean glowing, boolean unbreakable) {
        this.id = id;
        this.name = name;
        this.material = material;
        this.maxStack = maxStack;
        this.icon = icon;
        this.lore = lore;
        this.nbt = nbt;
        this.attributes = attributes;
        this.enchantments = enchantments;
        this.behaviors = behaviors;
        this.rarity = rarity;
        this.glowing = glowing;
        this.unbreakable = unbreakable;
    }
    
    public void handleInteraction(ItemContext context) {
        for (ItemBuilder.ItemBehavior behavior : behaviors) {
            behavior.execute(context);
        }
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public String getMaterial() { return material; }
    public int getMaxStack() { return maxStack; }
    public String getIcon() { return icon; }
    public List<String> getLore() { return lore; }
    public Map<String, Object> getNbt() { return nbt; }
    public Map<String, Double> getAttributes() { return attributes; }
    public List<ItemBuilder.ItemEnchantment> getEnchantments() { return enchantments; }
    public String getRarity() { return rarity; }
    public boolean isGlowing() { return glowing; }
    public boolean isUnbreakable() { return unbreakable; }
    
    public String getRarityColor() {
        return switch (rarity.toUpperCase()) {
            case "COMMON" -> "§f";
            case "UNCOMMON" -> "§a";
            case "RARE" -> "§9";
            case "EPIC" -> "§5";
            case "LEGENDARY" -> "§6";
            case "MYTHIC" -> "§d";
            default -> "§7";
        };
    }
    
    public String getFormattedName() {
        return getRarityColor() + name;
    }
    
    public List<String> getFormattedLore() {
        List<String> formatted = new ArrayList<>();
        formatted.add("");
        for (String line : lore) {
            formatted.add("§7" + line);
        }
        formatted.add("");
        formatted.add("§8" + rarity);
        return formatted;
    }
}
