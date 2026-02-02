package rubidium.api.item;

import java.util.*;

public class ItemStack {
    
    private final String type;
    private int amount;
    private String displayName;
    private List<String> lore;
    private Map<String, Object> nbt;
    private Map<String, Integer> enchantments;
    private int durability;
    private int maxDurability;
    
    public ItemStack(String type) {
        this(type, 1);
    }
    
    public ItemStack(String type, int amount) {
        this.type = type;
        this.amount = Math.max(1, Math.min(64, amount));
        this.lore = new ArrayList<>();
        this.nbt = new HashMap<>();
        this.enchantments = new HashMap<>();
        this.durability = -1;
        this.maxDurability = -1;
    }
    
    public String getType() {
        return type;
    }
    
    public int getAmount() {
        return amount;
    }
    
    public void setAmount(int amount) {
        this.amount = Math.max(0, Math.min(64, amount));
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public ItemStack setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }
    
    public List<String> getLore() {
        return new ArrayList<>(lore);
    }
    
    public ItemStack setLore(List<String> lore) {
        this.lore = new ArrayList<>(lore);
        return this;
    }
    
    public ItemStack addLore(String line) {
        this.lore.add(line);
        return this;
    }
    
    public Map<String, Integer> getEnchantments() {
        return new HashMap<>(enchantments);
    }
    
    public ItemStack addEnchantment(String enchant, int level) {
        this.enchantments.put(enchant, level);
        return this;
    }
    
    public boolean hasEnchantment(String enchant) {
        return enchantments.containsKey(enchant);
    }
    
    public int getEnchantmentLevel(String enchant) {
        return enchantments.getOrDefault(enchant, 0);
    }
    
    public int getDurability() {
        return durability;
    }
    
    public ItemStack setDurability(int durability) {
        this.durability = durability;
        return this;
    }
    
    public int getMaxDurability() {
        return maxDurability;
    }
    
    public ItemStack setMaxDurability(int maxDurability) {
        this.maxDurability = maxDurability;
        return this;
    }
    
    public boolean isDamaged() {
        return durability > 0 && durability < maxDurability;
    }
    
    public Object getNBT(String key) {
        return nbt.get(key);
    }
    
    public ItemStack setNBT(String key, Object value) {
        this.nbt.put(key, value);
        return this;
    }
    
    public boolean hasNBT(String key) {
        return nbt.containsKey(key);
    }
    
    public Map<String, Object> getAllNBT() {
        return new HashMap<>(nbt);
    }
    
    public ItemStack clone() {
        ItemStack copy = new ItemStack(type, amount);
        copy.displayName = displayName;
        copy.lore = new ArrayList<>(lore);
        copy.nbt = new HashMap<>(nbt);
        copy.enchantments = new HashMap<>(enchantments);
        copy.durability = durability;
        copy.maxDurability = maxDurability;
        return copy;
    }
    
    public boolean isSimilar(ItemStack other) {
        if (other == null) return false;
        return type.equals(other.type) && 
               Objects.equals(displayName, other.displayName) &&
               lore.equals(other.lore) &&
               enchantments.equals(other.enchantments);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type).append(" x").append(amount);
        if (displayName != null) {
            sb.append(" [").append(displayName).append("]");
        }
        if (!enchantments.isEmpty()) {
            sb.append(" (enchanted)");
        }
        return sb.toString();
    }
}
