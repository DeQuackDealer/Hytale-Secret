package rubidium.hytale.player;

import rubidium.api.player.PlayerInventory;

import java.lang.reflect.Method;
import java.util.logging.Logger;

public class HytalePlayerInventory implements PlayerInventory {
    
    private static final Logger logger = Logger.getLogger("Rubidium-HytaleInventory");
    
    private final Object hytalePlayer;
    private Object inventoryHandle;
    
    public HytalePlayerInventory(Object hytalePlayer) {
        this.hytalePlayer = hytalePlayer;
        extractInventory();
    }
    
    private void extractInventory() {
        try {
            Method m = hytalePlayer.getClass().getMethod("getInventory");
            this.inventoryHandle = m.invoke(hytalePlayer);
        } catch (Exception e) {
            try {
                var field = hytalePlayer.getClass().getDeclaredField("inventory");
                field.setAccessible(true);
                this.inventoryHandle = field.get(hytalePlayer);
            } catch (Exception ex) {
                logger.warn("Could not extract inventory handle");
            }
        }
    }
    
    @Override
    public Object getItem(int slot) {
        if (inventoryHandle == null) return null;
        
        try {
            Method m = inventoryHandle.getClass().getMethod("getItem", int.class);
            return m.invoke(inventoryHandle, slot);
        } catch (Exception e) {
            try {
                Method m = inventoryHandle.getClass().getMethod("get", int.class);
                return m.invoke(inventoryHandle, slot);
            } catch (Exception ex) {
                logger.warn("Failed to get item at slot " + slot);
            }
        }
        return null;
    }
    
    @Override
    public void setItem(int slot, Object item) {
        if (inventoryHandle == null) return;
        
        try {
            Method m = inventoryHandle.getClass().getMethod("setItem", int.class, Object.class);
            m.invoke(inventoryHandle, slot, item);
        } catch (Exception e) {
            try {
                Method m = inventoryHandle.getClass().getMethod("set", int.class, Object.class);
                m.invoke(inventoryHandle, slot, item);
            } catch (Exception ex) {
                logger.warn("Failed to set item at slot " + slot);
            }
        }
    }
    
    @Override
    public void clear() {
        if (inventoryHandle == null) return;
        
        try {
            Method m = inventoryHandle.getClass().getMethod("clear");
            m.invoke(inventoryHandle);
        } catch (Exception e) {
            logger.warn("Failed to clear inventory");
        }
    }
    
    @Override
    public void clear(int slot) {
        setItem(slot, null);
    }
    
    @Override
    public int getSize() {
        if (inventoryHandle == null) return 36;
        
        try {
            Method m = inventoryHandle.getClass().getMethod("getSize");
            return (int) m.invoke(inventoryHandle);
        } catch (Exception e) {
            try {
                Method m = inventoryHandle.getClass().getMethod("size");
                return (int) m.invoke(inventoryHandle);
            } catch (Exception ex) {
                return 36;
            }
        }
    }
    
    @Override
    public Object getItemInMainHand() {
        if (inventoryHandle == null) return null;
        
        try {
            Method m = inventoryHandle.getClass().getMethod("getItemInMainHand");
            return m.invoke(inventoryHandle);
        } catch (Exception e) {
            try {
                Method m = inventoryHandle.getClass().getMethod("getHeldItem");
                return m.invoke(inventoryHandle);
            } catch (Exception ex) {
                return getItem(0);
            }
        }
    }
    
    @Override
    public void setItemInMainHand(Object item) {
        if (inventoryHandle == null) return;
        
        try {
            Method m = inventoryHandle.getClass().getMethod("setItemInMainHand", Object.class);
            m.invoke(inventoryHandle, item);
        } catch (Exception e) {
            try {
                Method m = inventoryHandle.getClass().getMethod("setHeldItem", Object.class);
                m.invoke(inventoryHandle, item);
            } catch (Exception ex) {
                setItem(0, item);
            }
        }
    }
    
    @Override
    public Object getItemInOffHand() {
        if (inventoryHandle == null) return null;
        
        try {
            Method m = inventoryHandle.getClass().getMethod("getItemInOffHand");
            return m.invoke(inventoryHandle);
        } catch (Exception e) {
            return getItem(40);
        }
    }
    
    @Override
    public void setItemInOffHand(Object item) {
        if (inventoryHandle == null) return;
        
        try {
            Method m = inventoryHandle.getClass().getMethod("setItemInOffHand", Object.class);
            m.invoke(inventoryHandle, item);
        } catch (Exception e) {
            setItem(40, item);
        }
    }
    
    @Override
    public Object[] getContents() {
        int size = getSize();
        Object[] contents = new Object[size];
        for (int i = 0; i < size; i++) {
            contents[i] = getItem(i);
        }
        return contents;
    }
    
    @Override
    public void setContents(Object[] items) {
        for (int i = 0; i < items.length && i < getSize(); i++) {
            setItem(i, items[i]);
        }
    }
    
    @Override
    public int firstEmpty() {
        int size = getSize();
        for (int i = 0; i < size; i++) {
            if (getItem(i) == null) {
                return i;
            }
        }
        return -1;
    }
    
    @Override
    public boolean contains(Object item) {
        int size = getSize();
        for (int i = 0; i < size; i++) {
            Object slotItem = getItem(i);
            if (slotItem != null && slotItem.equals(item)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void addItem(Object... items) {
        for (Object item : items) {
            int slot = firstEmpty();
            if (slot != -1) {
                setItem(slot, item);
            }
        }
    }
    
    @Override
    public void removeItem(Object... items) {
        for (Object item : items) {
            int size = getSize();
            for (int i = 0; i < size; i++) {
                Object slotItem = getItem(i);
                if (slotItem != null && slotItem.equals(item)) {
                    setItem(i, null);
                    break;
                }
            }
        }
    }
    
    public Object getHandle() {
        return inventoryHandle;
    }
}
