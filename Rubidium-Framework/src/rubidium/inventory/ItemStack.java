package rubidium.inventory;

import rubidium.nbt.NBTTag;

import java.util.*;

public class ItemStack {
    
    private String type;
    private int amount;
    private int maxStackSize;
    private String displayName;
    private List<String> lore;
    private Map<String, Integer> enchantments;
    private NBTTag.CompoundTag customData;
    private boolean unbreakable;
    private int durability;
    private int maxDurability;
    
    public ItemStack(String type) {
        this(type, 1);
    }
    
    public ItemStack(String type, int amount) {
        this.type = type;
        this.amount = amount;
        this.maxStackSize = 64;
        this.lore = new ArrayList<>();
        this.enchantments = new HashMap<>();
        this.customData = new NBTTag.CompoundTag();
        this.unbreakable = false;
        this.durability = 0;
        this.maxDurability = 0;
    }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = Math.min(amount, maxStackSize); }
    
    public int getMaxStackSize() { return maxStackSize; }
    public void setMaxStackSize(int maxStackSize) { this.maxStackSize = maxStackSize; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public List<String> getLore() { return new ArrayList<>(lore); }
    public void setLore(List<String> lore) { this.lore = new ArrayList<>(lore); }
    public void addLore(String line) { this.lore.add(line); }
    public void clearLore() { this.lore.clear(); }
    
    public boolean hasDisplayName() { return displayName != null && !displayName.isEmpty(); }
    public boolean hasLore() { return !lore.isEmpty(); }
    
    public Map<String, Integer> getEnchantments() { return new HashMap<>(enchantments); }
    public void addEnchantment(String enchantment, int level) { enchantments.put(enchantment, level); }
    public void removeEnchantment(String enchantment) { enchantments.remove(enchantment); }
    public int getEnchantmentLevel(String enchantment) { return enchantments.getOrDefault(enchantment, 0); }
    public boolean hasEnchantment(String enchantment) { return enchantments.containsKey(enchantment); }
    public boolean hasEnchantments() { return !enchantments.isEmpty(); }
    
    public NBTTag.CompoundTag getCustomData() { return customData; }
    public void setCustomData(NBTTag.CompoundTag customData) { this.customData = customData; }
    
    public boolean isUnbreakable() { return unbreakable; }
    public void setUnbreakable(boolean unbreakable) { this.unbreakable = unbreakable; }
    
    public int getDurability() { return durability; }
    public void setDurability(int durability) { this.durability = durability; }
    
    public int getMaxDurability() { return maxDurability; }
    public void setMaxDurability(int maxDurability) { this.maxDurability = maxDurability; }
    
    public boolean isDamageable() { return maxDurability > 0; }
    
    public boolean isEmpty() {
        return type == null || type.equals("air") || amount <= 0;
    }
    
    public boolean isSimilar(ItemStack other) {
        if (other == null) return false;
        return Objects.equals(type, other.type) &&
               Objects.equals(displayName, other.displayName) &&
               Objects.equals(lore, other.lore) &&
               Objects.equals(enchantments, other.enchantments);
    }
    
    public ItemStack clone() {
        ItemStack clone = new ItemStack(type, amount);
        clone.maxStackSize = maxStackSize;
        clone.displayName = displayName;
        clone.lore = new ArrayList<>(lore);
        clone.enchantments = new HashMap<>(enchantments);
        clone.customData = (NBTTag.CompoundTag) customData.copy();
        clone.unbreakable = unbreakable;
        clone.durability = durability;
        clone.maxDurability = maxDurability;
        return clone;
    }
    
    public static Builder builder(String type) {
        return new Builder(type);
    }
    
    public static class Builder {
        private final ItemStack item;
        
        public Builder(String type) {
            this.item = new ItemStack(type);
        }
        
        public Builder amount(int amount) {
            item.setAmount(amount);
            return this;
        }
        
        public Builder displayName(String displayName) {
            item.setDisplayName(displayName);
            return this;
        }
        
        public Builder lore(String... lines) {
            item.setLore(Arrays.asList(lines));
            return this;
        }
        
        public Builder lore(List<String> lines) {
            item.setLore(lines);
            return this;
        }
        
        public Builder enchant(String enchantment, int level) {
            item.addEnchantment(enchantment, level);
            return this;
        }
        
        public Builder unbreakable(boolean unbreakable) {
            item.setUnbreakable(unbreakable);
            return this;
        }
        
        public Builder customData(NBTTag.CompoundTag data) {
            item.setCustomData(data);
            return this;
        }
        
        public ItemStack build() {
            return item;
        }
    }
}
