package com.yellowtale.rubidium.api.player;

public interface PlayerInventory {
    
    Object getItem(int slot);
    
    void setItem(int slot, Object item);
    
    Object getItemInMainHand();
    
    void setItemInMainHand(Object item);
    
    Object getItemInOffHand();
    
    void setItemInOffHand(Object item);
    
    Object[] getContents();
    
    void setContents(Object[] items);
    
    int getSize();
    
    void clear();
    
    void clear(int slot);
    
    int firstEmpty();
    
    boolean contains(Object item);
    
    void addItem(Object... items);
    
    void removeItem(Object... items);
}
