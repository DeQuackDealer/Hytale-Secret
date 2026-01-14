package rubidium.items;

import java.util.*;

/**
 * Fluent builder for creating custom items with behaviors and attributes.
 */
public class ItemBuilder {
    
    private String id;
    private String name;
    private String material;
    private int maxStack = 64;
    private String icon;
    private final List<String> lore = new ArrayList<>();
    private final Map<String, Object> nbt = new HashMap<>();
    private final Map<String, Double> attributes = new HashMap<>();
    private final List<ItemEnchantment> enchantments = new ArrayList<>();
    private final List<ItemBehavior> behaviors = new ArrayList<>();
    private String rarity = "COMMON";
    private boolean glowing = false;
    private boolean unbreakable = false;
    
    public ItemBuilder(String id) {
        this.id = id;
    }
    
    public static ItemBuilder create(String id) {
        return new ItemBuilder(id);
    }
    
    public ItemBuilder name(String name) { this.name = name; return this; }
    public ItemBuilder material(String material) { this.material = material; return this; }
    public ItemBuilder maxStack(int stack) { this.maxStack = stack; return this; }
    public ItemBuilder icon(String icon) { this.icon = icon; return this; }
    public ItemBuilder rarity(String rarity) { this.rarity = rarity; return this; }
    public ItemBuilder glowing(boolean glowing) { this.glowing = glowing; return this; }
    public ItemBuilder unbreakable(boolean unbreakable) { this.unbreakable = unbreakable; return this; }
    
    public ItemBuilder lore(String... lines) {
        lore.addAll(Arrays.asList(lines));
        return this;
    }
    
    public ItemBuilder lore(String line) {
        lore.add(line);
        return this;
    }
    
    public ItemBuilder nbt(String key, Object value) {
        nbt.put(key, value);
        return this;
    }
    
    public ItemBuilder attribute(String attribute, double value) {
        attributes.put(attribute, value);
        return this;
    }
    
    public ItemBuilder damage(double damage) {
        return attribute("attack_damage", damage);
    }
    
    public ItemBuilder attackSpeed(double speed) {
        return attribute("attack_speed", speed);
    }
    
    public ItemBuilder armor(double armor) {
        return attribute("armor", armor);
    }
    
    public ItemBuilder armorToughness(double toughness) {
        return attribute("armor_toughness", toughness);
    }
    
    public ItemBuilder speed(double speed) {
        return attribute("movement_speed", speed);
    }
    
    public ItemBuilder health(double health) {
        return attribute("max_health", health);
    }
    
    public ItemBuilder enchant(String enchantment, int level) {
        enchantments.add(new ItemEnchantment(enchantment, level));
        return this;
    }
    
    public ItemBuilder behavior(ItemBehavior behavior) {
        behaviors.add(behavior);
        return this;
    }
    
    public ItemBuilder onRightClick(ItemAction action) {
        return behavior(new RightClickBehavior(action));
    }
    
    public ItemBuilder onLeftClick(ItemAction action) {
        return behavior(new LeftClickBehavior(action));
    }
    
    public ItemBuilder onUse(ItemAction action) {
        return behavior(new UseBehavior(action));
    }
    
    public CustomItem build() {
        return new CustomItem(id, name, material, maxStack, icon, lore, nbt, 
            attributes, enchantments, behaviors, rarity, glowing, unbreakable);
    }
    
    public record ItemEnchantment(String id, int level) {}
    
    public interface ItemBehavior {
        void execute(ItemContext context);
    }
    
    public interface ItemAction {
        void execute(ItemContext context);
    }
    
    public record RightClickBehavior(ItemAction action) implements ItemBehavior {
        @Override
        public void execute(ItemContext context) {
            if (context.getAction() == ItemContext.Action.RIGHT_CLICK) {
                action.execute(context);
            }
        }
    }
    
    public record LeftClickBehavior(ItemAction action) implements ItemBehavior {
        @Override
        public void execute(ItemContext context) {
            if (context.getAction() == ItemContext.Action.LEFT_CLICK) {
                action.execute(context);
            }
        }
    }
    
    public record UseBehavior(ItemAction action) implements ItemBehavior {
        @Override
        public void execute(ItemContext context) {
            if (context.getAction() == ItemContext.Action.USE) {
                action.execute(context);
            }
        }
    }
}
