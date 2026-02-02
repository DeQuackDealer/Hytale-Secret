package rubidium.api.item;

import java.util.*;

public class PlayerInventory {
    
    public static final int INVENTORY_SIZE = 36;
    public static final int HOTBAR_SIZE = 9;
    public static final int ARMOR_SIZE = 4;
    
    private final UUID owner;
    private final ItemStack[] slots;
    private final ItemStack[] armor;
    private ItemStack offHand;
    private int selectedSlot = 0;
    
    public PlayerInventory(UUID owner) {
        this.owner = owner;
        this.slots = new ItemStack[INVENTORY_SIZE];
        this.armor = new ItemStack[ARMOR_SIZE];
    }
    
    public UUID getOwner() {
        return owner;
    }
    
    public boolean addItem(ItemStack item) {
        if (item == null || item.getAmount() <= 0) return false;
        
        int remaining = item.getAmount();
        
        for (int i = 0; i < INVENTORY_SIZE && remaining > 0; i++) {
            ItemStack slot = slots[i];
            if (slot != null && slot.isSimilar(item)) {
                int space = 64 - slot.getAmount();
                if (space > 0) {
                    int toAdd = Math.min(space, remaining);
                    slot.setAmount(slot.getAmount() + toAdd);
                    remaining -= toAdd;
                }
            }
        }
        
        for (int i = 0; i < INVENTORY_SIZE && remaining > 0; i++) {
            if (slots[i] == null) {
                ItemStack newStack = item.clone();
                int toAdd = Math.min(64, remaining);
                newStack.setAmount(toAdd);
                slots[i] = newStack;
                remaining -= toAdd;
            }
        }
        
        return remaining < item.getAmount();
    }
    
    public boolean removeItem(String itemType, int amount) {
        int toRemove = amount;
        
        for (int i = INVENTORY_SIZE - 1; i >= 0 && toRemove > 0; i--) {
            ItemStack slot = slots[i];
            if (slot != null && slot.getType().equals(itemType)) {
                int remove = Math.min(slot.getAmount(), toRemove);
                slot.setAmount(slot.getAmount() - remove);
                toRemove -= remove;
                
                if (slot.getAmount() <= 0) {
                    slots[i] = null;
                }
            }
        }
        
        return toRemove == 0;
    }
    
    public int countItem(String itemType) {
        int count = 0;
        for (ItemStack slot : slots) {
            if (slot != null && slot.getType().equals(itemType)) {
                count += slot.getAmount();
            }
        }
        return count;
    }
    
    public ItemStack getSlot(int index) {
        if (index < 0 || index >= INVENTORY_SIZE) return null;
        return slots[index];
    }
    
    public boolean setSlot(int index, ItemStack item) {
        if (index < 0 || index >= INVENTORY_SIZE) return false;
        slots[index] = item;
        return true;
    }
    
    public ItemStack getMainHand() {
        return slots[selectedSlot];
    }
    
    public void setSelectedSlot(int slot) {
        if (slot >= 0 && slot < HOTBAR_SIZE) {
            this.selectedSlot = slot;
        }
    }
    
    public int getSelectedSlot() {
        return selectedSlot;
    }
    
    public ItemStack getOffHand() {
        return offHand;
    }
    
    public void setOffHand(ItemStack item) {
        this.offHand = item;
    }
    
    public ItemStack[] getArmor() {
        return armor.clone();
    }
    
    public ItemStack getHelmet() {
        return armor[0];
    }
    
    public ItemStack getChestplate() {
        return armor[1];
    }
    
    public ItemStack getLeggings() {
        return armor[2];
    }
    
    public ItemStack getBoots() {
        return armor[3];
    }
    
    public void setArmor(ItemStack helmet, ItemStack chestplate, ItemStack leggings, ItemStack boots) {
        armor[0] = helmet;
        armor[1] = chestplate;
        armor[2] = leggings;
        armor[3] = boots;
    }
    
    public void setHelmet(ItemStack item) {
        armor[0] = item;
    }
    
    public void setChestplate(ItemStack item) {
        armor[1] = item;
    }
    
    public void setLeggings(ItemStack item) {
        armor[2] = item;
    }
    
    public void setBoots(ItemStack item) {
        armor[3] = item;
    }
    
    public void clear() {
        Arrays.fill(slots, null);
        Arrays.fill(armor, null);
        offHand = null;
    }
    
    public boolean isEmpty() {
        for (ItemStack slot : slots) {
            if (slot != null) return false;
        }
        for (ItemStack a : armor) {
            if (a != null) return false;
        }
        return offHand == null;
    }
    
    public int getFirstEmpty() {
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (slots[i] == null) return i;
        }
        return -1;
    }
    
    public List<ItemStack> getContents() {
        List<ItemStack> contents = new ArrayList<>();
        for (ItemStack slot : slots) {
            if (slot != null) {
                contents.add(slot.clone());
            }
        }
        return contents;
    }
    
    public ItemStack[] getHotbar() {
        ItemStack[] hotbar = new ItemStack[HOTBAR_SIZE];
        System.arraycopy(slots, 0, hotbar, 0, HOTBAR_SIZE);
        return hotbar;
    }
}
