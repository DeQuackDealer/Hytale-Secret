package rubidium.api.item;

import rubidium.core.HytaleRuntimeBridge;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class InventoryAPI {
    
    private static final Logger LOGGER = Logger.getLogger("Rubidium-Inventory");
    private static InventoryAPI instance;
    
    private final Map<UUID, PlayerInventory> inventories = new ConcurrentHashMap<>();
    private final Map<String, ItemDefinition> itemRegistry = new ConcurrentHashMap<>();
    
    private InventoryAPI() {
        registerDefaultItems();
    }
    
    public static InventoryAPI get() {
        if (instance == null) {
            instance = new InventoryAPI();
        }
        return instance;
    }
    
    private void registerDefaultItems() {
        register(new ItemDefinition("hytale:diamond_sword", "Diamond Sword", 1, true, 1561));
        register(new ItemDefinition("hytale:iron_sword", "Iron Sword", 1, true, 250));
        register(new ItemDefinition("hytale:stone_sword", "Stone Sword", 1, true, 131));
        register(new ItemDefinition("hytale:wooden_sword", "Wooden Sword", 1, true, 59));
        register(new ItemDefinition("hytale:diamond_pickaxe", "Diamond Pickaxe", 1, true, 1561));
        register(new ItemDefinition("hytale:iron_pickaxe", "Iron Pickaxe", 1, true, 250));
        register(new ItemDefinition("hytale:diamond", "Diamond", 64, false, -1));
        register(new ItemDefinition("hytale:iron_ingot", "Iron Ingot", 64, false, -1));
        register(new ItemDefinition("hytale:gold_ingot", "Gold Ingot", 64, false, -1));
        register(new ItemDefinition("hytale:emerald", "Emerald", 64, false, -1));
        register(new ItemDefinition("hytale:apple", "Apple", 64, false, -1));
        register(new ItemDefinition("hytale:golden_apple", "Golden Apple", 64, false, -1));
        register(new ItemDefinition("hytale:bread", "Bread", 64, false, -1));
        register(new ItemDefinition("hytale:cooked_beef", "Steak", 64, false, -1));
        register(new ItemDefinition("hytale:bow", "Bow", 1, true, 384));
        register(new ItemDefinition("hytale:arrow", "Arrow", 64, false, -1));
        register(new ItemDefinition("hytale:shield", "Shield", 1, true, 336));
        register(new ItemDefinition("hytale:ender_pearl", "Ender Pearl", 16, false, -1));
        register(new ItemDefinition("hytale:potion", "Potion", 1, false, -1));
        register(new ItemDefinition("hytale:torch", "Torch", 64, false, -1));
        LOGGER.info("[Rubidium] Registered " + itemRegistry.size() + " default items");
    }
    
    public void register(ItemDefinition definition) {
        itemRegistry.put(definition.getId(), definition);
    }
    
    public ItemDefinition getDefinition(String itemId) {
        return itemRegistry.get(itemId);
    }
    
    public Collection<ItemDefinition> getAllDefinitions() {
        return Collections.unmodifiableCollection(itemRegistry.values());
    }
    
    public PlayerInventory getInventory(UUID playerId) {
        return inventories.computeIfAbsent(playerId, PlayerInventory::new);
    }
    
    public boolean giveItem(UUID playerId, ItemStack item) {
        PlayerInventory inv = getInventory(playerId);
        boolean success = inv.addItem(item);
        
        if (success) {
            syncToHytale(playerId, inv);
            LOGGER.info("[Rubidium] Gave " + item + " to player " + playerId);
        }
        
        return success;
    }
    
    public boolean giveItem(UUID playerId, String itemType, int amount) {
        return giveItem(playerId, new ItemStack(itemType, amount));
    }
    
    public boolean takeItem(UUID playerId, String itemType, int amount) {
        PlayerInventory inv = getInventory(playerId);
        boolean success = inv.removeItem(itemType, amount);
        
        if (success) {
            syncToHytale(playerId, inv);
            LOGGER.info("[Rubidium] Took " + amount + "x " + itemType + " from player " + playerId);
        }
        
        return success;
    }
    
    public boolean hasItem(UUID playerId, String itemType, int amount) {
        return getInventory(playerId).countItem(itemType) >= amount;
    }
    
    public int countItem(UUID playerId, String itemType) {
        return getInventory(playerId).countItem(itemType);
    }
    
    public boolean setSlot(UUID playerId, int slot, ItemStack item) {
        PlayerInventory inv = getInventory(playerId);
        boolean success = inv.setSlot(slot, item);
        
        if (success) {
            syncToHytale(playerId, inv);
        }
        
        return success;
    }
    
    public ItemStack getSlot(UUID playerId, int slot) {
        return getInventory(playerId).getSlot(slot);
    }
    
    public void clearInventory(UUID playerId) {
        PlayerInventory inv = getInventory(playerId);
        inv.clear();
        syncToHytale(playerId, inv);
        LOGGER.info("[Rubidium] Cleared inventory for player " + playerId);
    }
    
    public void clearSlot(UUID playerId, int slot) {
        setSlot(playerId, slot, null);
    }
    
    public ItemStack getMainHand(UUID playerId) {
        return getInventory(playerId).getMainHand();
    }
    
    public ItemStack getOffHand(UUID playerId) {
        return getInventory(playerId).getOffHand();
    }
    
    public ItemStack[] getArmor(UUID playerId) {
        return getInventory(playerId).getArmor();
    }
    
    public void setArmor(UUID playerId, ItemStack helmet, ItemStack chestplate, ItemStack leggings, ItemStack boots) {
        PlayerInventory inv = getInventory(playerId);
        inv.setArmor(helmet, chestplate, leggings, boots);
        syncToHytale(playerId, inv);
    }
    
    private void syncToHytale(UUID playerId, PlayerInventory inv) {
        if (HytaleRuntimeBridge.get().isHytaleAvailable()) {
            try {
                HytaleRuntimeBridge.get().syncInventory(playerId, inv);
            } catch (Exception e) {
                LOGGER.warning("[Rubidium] Failed to sync inventory to Hytale: " + e.getMessage());
            }
        }
    }
    
    public void onPlayerJoin(UUID playerId) {
        if (HytaleRuntimeBridge.get().isHytaleAvailable()) {
            try {
                Object hytaleInv = HytaleRuntimeBridge.get().loadInventory(playerId);
                if (hytaleInv instanceof PlayerInventory) {
                    inventories.put(playerId, (PlayerInventory) hytaleInv);
                }
            } catch (Exception e) {
                LOGGER.warning("[Rubidium] Failed to load inventory from Hytale: " + e.getMessage());
            }
        }
    }
    
    public void onPlayerQuit(UUID playerId) {
        PlayerInventory inv = inventories.remove(playerId);
        if (inv != null && HytaleRuntimeBridge.get().isHytaleAvailable()) {
            try {
                HytaleRuntimeBridge.get().saveInventory(playerId, inv);
            } catch (Exception e) {
                LOGGER.warning("[Rubidium] Failed to save inventory to Hytale: " + e.getMessage());
            }
        }
    }
    
    public static class ItemDefinition {
        private final String id;
        private final String name;
        private final int maxStack;
        private final boolean hasDurability;
        private final int maxDurability;
        
        public ItemDefinition(String id, String name, int maxStack, boolean hasDurability, int maxDurability) {
            this.id = id;
            this.name = name;
            this.maxStack = maxStack;
            this.hasDurability = hasDurability;
            this.maxDurability = maxDurability;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public int getMaxStack() { return maxStack; }
        public boolean hasDurability() { return hasDurability; }
        public int getMaxDurability() { return maxDurability; }
    }
}
