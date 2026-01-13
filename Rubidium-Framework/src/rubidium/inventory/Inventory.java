package rubidium.inventory;

import java.util.*;
import java.util.function.Predicate;

public interface Inventory {
    
    int getSize();
    
    String getTitle();
    
    ItemStack getItem(int slot);
    
    void setItem(int slot, ItemStack item);
    
    ItemStack[] getContents();
    
    void setContents(ItemStack[] items);
    
    boolean contains(String type);
    
    boolean contains(ItemStack item);
    
    int first(String type);
    
    int first(ItemStack item);
    
    int firstEmpty();
    
    void remove(String type);
    
    void remove(ItemStack item);
    
    void clear();
    
    void clear(int slot);
    
    default boolean isEmpty() {
        for (ItemStack item : getContents()) {
            if (item != null && !item.isEmpty()) return false;
        }
        return true;
    }
    
    default int count(String type) {
        int count = 0;
        for (ItemStack item : getContents()) {
            if (item != null && item.getType().equals(type)) {
                count += item.getAmount();
            }
        }
        return count;
    }
    
    default boolean addItem(ItemStack item) {
        int remaining = item.getAmount();
        
        for (int i = 0; i < getSize() && remaining > 0; i++) {
            ItemStack current = getItem(i);
            if (current != null && current.isSimilar(item)) {
                int canAdd = current.getMaxStackSize() - current.getAmount();
                if (canAdd > 0) {
                    int toAdd = Math.min(canAdd, remaining);
                    current.setAmount(current.getAmount() + toAdd);
                    remaining -= toAdd;
                }
            }
        }
        
        for (int i = 0; i < getSize() && remaining > 0; i++) {
            ItemStack current = getItem(i);
            if (current == null || current.isEmpty()) {
                ItemStack newItem = item.clone();
                newItem.setAmount(Math.min(remaining, item.getMaxStackSize()));
                setItem(i, newItem);
                remaining -= newItem.getAmount();
            }
        }
        
        return remaining == 0;
    }
    
    default List<ItemStack> removeItem(String type, int amount) {
        List<ItemStack> removed = new ArrayList<>();
        int remaining = amount;
        
        for (int i = 0; i < getSize() && remaining > 0; i++) {
            ItemStack item = getItem(i);
            if (item != null && item.getType().equals(type)) {
                int toRemove = Math.min(item.getAmount(), remaining);
                
                ItemStack removedItem = item.clone();
                removedItem.setAmount(toRemove);
                removed.add(removedItem);
                
                item.setAmount(item.getAmount() - toRemove);
                if (item.getAmount() <= 0) {
                    clear(i);
                }
                
                remaining -= toRemove;
            }
        }
        
        return removed;
    }
    
    default List<ItemStack> find(Predicate<ItemStack> predicate) {
        List<ItemStack> found = new ArrayList<>();
        for (ItemStack item : getContents()) {
            if (item != null && predicate.test(item)) {
                found.add(item);
            }
        }
        return found;
    }
    
    default void forEach(java.util.function.Consumer<ItemStack> consumer) {
        for (ItemStack item : getContents()) {
            if (item != null && !item.isEmpty()) {
                consumer.accept(item);
            }
        }
    }
}
