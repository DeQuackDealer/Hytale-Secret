# InventoryAPI Reference

## Overview

The `InventoryAPI` provides comprehensive inventory management for players in Rubidium. It handles item registration, player inventory manipulation, armor management, and automatic synchronization with the Hytale runtime.

**Package:** `rubidium.api.item`

## Getting Started

```java
import rubidium.api.item.InventoryAPI;
import rubidium.api.item.InventoryAPI.ItemDefinition;

// Get the singleton instance
InventoryAPI inventory = InventoryAPI.get();
```

## Public Methods

### Instance Access

#### `get()`
Returns the singleton instance of the InventoryAPI.

```java
public static InventoryAPI get()
```

**Returns:** `InventoryAPI` - The singleton instance

**Example:**
```java
InventoryAPI api = InventoryAPI.get();
```

---

### Item Registration

#### `register(ItemDefinition definition)`
Registers a custom item definition to the item registry.

```java
public void register(ItemDefinition definition)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `definition` | `ItemDefinition` | The item definition to register |

**Example:**
```java
ItemDefinition customSword = new ItemDefinition(
    "mymod:epic_sword",  // Item ID
    "Epic Sword",        // Display name
    1,                   // Max stack size
    true,                // Has durability
    2000                 // Max durability
);
InventoryAPI.get().register(customSword);
```

#### `getDefinition(String itemId)`
Retrieves an item definition by its ID.

```java
public ItemDefinition getDefinition(String itemId)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `itemId` | `String` | The unique item identifier (e.g., "hytale:diamond_sword") |

**Returns:** `ItemDefinition` - The item definition, or `null` if not found

**Example:**
```java
ItemDefinition sword = InventoryAPI.get().getDefinition("hytale:diamond_sword");
if (sword != null) {
    System.out.println("Max stack: " + sword.getMaxStack());
}
```

#### `getAllDefinitions()`
Returns all registered item definitions.

```java
public Collection<ItemDefinition> getAllDefinitions()
```

**Returns:** `Collection<ItemDefinition>` - An unmodifiable collection of all item definitions

---

### Player Inventory Access

#### `getInventory(UUID playerId)`
Gets or creates a player's inventory.

```java
public PlayerInventory getInventory(UUID playerId)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `playerId` | `UUID` | The player's unique identifier |

**Returns:** `PlayerInventory` - The player's inventory instance

**Example:**
```java
UUID playerId = player.getUniqueId();
PlayerInventory inv = InventoryAPI.get().getInventory(playerId);
```

---

### Item Management

#### `giveItem(UUID playerId, ItemStack item)`
Gives an item stack to a player.

```java
public boolean giveItem(UUID playerId, ItemStack item)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `playerId` | `UUID` | The player's unique identifier |
| `item` | `ItemStack` | The item stack to give |

**Returns:** `boolean` - `true` if successful, `false` if inventory is full

**Example:**
```java
ItemStack diamonds = new ItemStack("hytale:diamond", 16);
boolean success = InventoryAPI.get().giveItem(playerId, diamonds);
if (success) {
    player.sendMessage("You received 16 diamonds!");
}
```

#### `giveItem(UUID playerId, String itemType, int amount)`
Convenience method to give items by type and amount.

```java
public boolean giveItem(UUID playerId, String itemType, int amount)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `playerId` | `UUID` | The player's unique identifier |
| `itemType` | `String` | The item type identifier |
| `amount` | `int` | The quantity to give |

**Returns:** `boolean` - `true` if successful, `false` if inventory is full

**Example:**
```java
InventoryAPI.get().giveItem(playerId, "hytale:golden_apple", 5);
```

#### `takeItem(UUID playerId, String itemType, int amount)`
Removes items from a player's inventory.

```java
public boolean takeItem(UUID playerId, String itemType, int amount)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `playerId` | `UUID` | The player's unique identifier |
| `itemType` | `String` | The item type to remove |
| `amount` | `int` | The quantity to remove |

**Returns:** `boolean` - `true` if successful, `false` if player doesn't have enough items

**Example:**
```java
if (InventoryAPI.get().takeItem(playerId, "hytale:emerald", 10)) {
    player.sendMessage("Purchase complete!");
} else {
    player.sendMessage("You don't have enough emeralds!");
}
```

#### `hasItem(UUID playerId, String itemType, int amount)`
Checks if a player has at least the specified amount of an item.

```java
public boolean hasItem(UUID playerId, String itemType, int amount)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `playerId` | `UUID` | The player's unique identifier |
| `itemType` | `String` | The item type to check |
| `amount` | `int` | The minimum required quantity |

**Returns:** `boolean` - `true` if player has enough items

**Example:**
```java
if (InventoryAPI.get().hasItem(playerId, "hytale:diamond", 5)) {
    // Player can afford the item
}
```

#### `countItem(UUID playerId, String itemType)`
Counts the total amount of a specific item type in a player's inventory.

```java
public int countItem(UUID playerId, String itemType)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `playerId` | `UUID` | The player's unique identifier |
| `itemType` | `String` | The item type to count |

**Returns:** `int` - The total count of the item

**Example:**
```java
int diamondCount = InventoryAPI.get().countItem(playerId, "hytale:diamond");
player.sendMessage("You have " + diamondCount + " diamonds.");
```

---

### Slot Management

#### `setSlot(UUID playerId, int slot, ItemStack item)`
Sets an item in a specific inventory slot.

```java
public boolean setSlot(UUID playerId, int slot, ItemStack item)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `playerId` | `UUID` | The player's unique identifier |
| `slot` | `int` | The slot index (0-35 for main inventory) |
| `item` | `ItemStack` | The item to place (or `null` to clear) |

**Returns:** `boolean` - `true` if successful

**Example:**
```java
ItemStack sword = new ItemStack("hytale:diamond_sword", 1);
InventoryAPI.get().setSlot(playerId, 0, sword); // Set in first hotbar slot
```

#### `getSlot(UUID playerId, int slot)`
Gets the item in a specific inventory slot.

```java
public ItemStack getSlot(UUID playerId, int slot)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `playerId` | `UUID` | The player's unique identifier |
| `slot` | `int` | The slot index |

**Returns:** `ItemStack` - The item in the slot, or `null` if empty

#### `clearSlot(UUID playerId, int slot)`
Clears a specific inventory slot.

```java
public void clearSlot(UUID playerId, int slot)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `playerId` | `UUID` | The player's unique identifier |
| `slot` | `int` | The slot index to clear |

#### `clearInventory(UUID playerId)`
Clears all items from a player's inventory.

```java
public void clearInventory(UUID playerId)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `playerId` | `UUID` | The player's unique identifier |

**Example:**
```java
InventoryAPI.get().clearInventory(playerId);
player.sendMessage("Your inventory has been cleared!");
```

---

### Equipment Access

#### `getMainHand(UUID playerId)`
Gets the item in the player's main hand.

```java
public ItemStack getMainHand(UUID playerId)
```

**Returns:** `ItemStack` - The main hand item, or `null` if empty

#### `getOffHand(UUID playerId)`
Gets the item in the player's off hand.

```java
public ItemStack getOffHand(UUID playerId)
```

**Returns:** `ItemStack` - The off hand item, or `null` if empty

#### `getArmor(UUID playerId)`
Gets the player's armor pieces.

```java
public ItemStack[] getArmor(UUID playerId)
```

**Returns:** `ItemStack[]` - Array of armor items [helmet, chestplate, leggings, boots]

#### `setArmor(UUID playerId, ItemStack helmet, ItemStack chestplate, ItemStack leggings, ItemStack boots)`
Sets all armor pieces for a player.

```java
public void setArmor(UUID playerId, ItemStack helmet, ItemStack chestplate, ItemStack leggings, ItemStack boots)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `playerId` | `UUID` | The player's unique identifier |
| `helmet` | `ItemStack` | Helmet item (or `null`) |
| `chestplate` | `ItemStack` | Chestplate item (or `null`) |
| `leggings` | `ItemStack` | Leggings item (or `null`) |
| `boots` | `ItemStack` | Boots item (or `null`) |

**Example:**
```java
InventoryAPI.get().setArmor(playerId,
    new ItemStack("hytale:diamond_helmet", 1),
    new ItemStack("hytale:diamond_chestplate", 1),
    new ItemStack("hytale:diamond_leggings", 1),
    new ItemStack("hytale:diamond_boots", 1)
);
```

---

### Lifecycle Events

#### `onPlayerJoin(UUID playerId)`
Called when a player joins to load their inventory from Hytale.

```java
public void onPlayerJoin(UUID playerId)
```

#### `onPlayerQuit(UUID playerId)`
Called when a player leaves to save their inventory to Hytale.

```java
public void onPlayerQuit(UUID playerId)
```

---

## Inner Classes

### ItemDefinition

Represents the definition of an item type.

```java
public static class ItemDefinition {
    public ItemDefinition(String id, String name, int maxStack, boolean hasDurability, int maxDurability)
    
    public String getId()
    public String getName()
    public int getMaxStack()
    public boolean hasDurability()
    public int getMaxDurability()
}
```

---

## Default Registered Items

The following items are pre-registered:

| Item ID | Name | Max Stack | Has Durability | Max Durability |
|---------|------|-----------|----------------|----------------|
| `hytale:diamond_sword` | Diamond Sword | 1 | Yes | 1561 |
| `hytale:iron_sword` | Iron Sword | 1 | Yes | 250 |
| `hytale:stone_sword` | Stone Sword | 1 | Yes | 131 |
| `hytale:wooden_sword` | Wooden Sword | 1 | Yes | 59 |
| `hytale:diamond_pickaxe` | Diamond Pickaxe | 1 | Yes | 1561 |
| `hytale:iron_pickaxe` | Iron Pickaxe | 1 | Yes | 250 |
| `hytale:diamond` | Diamond | 64 | No | - |
| `hytale:iron_ingot` | Iron Ingot | 64 | No | - |
| `hytale:gold_ingot` | Gold Ingot | 64 | No | - |
| `hytale:emerald` | Emerald | 64 | No | - |
| `hytale:apple` | Apple | 64 | No | - |
| `hytale:golden_apple` | Golden Apple | 64 | No | - |
| `hytale:bread` | Bread | 64 | No | - |
| `hytale:cooked_beef` | Steak | 64 | No | - |
| `hytale:bow` | Bow | 1 | Yes | 384 |
| `hytale:arrow` | Arrow | 64 | No | - |
| `hytale:shield` | Shield | 1 | Yes | 336 |
| `hytale:ender_pearl` | Ender Pearl | 16 | No | - |
| `hytale:potion` | Potion | 1 | No | - |
| `hytale:torch` | Torch | 64 | No | - |

---

## Best Practices

1. **Always check return values** - Methods like `giveItem()` and `takeItem()` return boolean indicating success.

2. **Use hasItem() before takeItem()** - Check if the player has enough items before attempting to take them.

3. **Register custom items early** - Register your item definitions during plugin initialization.

4. **Use the convenience methods** - `giveItem(UUID, String, int)` is simpler than creating ItemStack objects manually.

5. **Thread safety** - The InventoryAPI uses ConcurrentHashMap internally and is thread-safe for most operations.

---

## Complete Example

```java
import rubidium.api.item.InventoryAPI;
import rubidium.api.item.InventoryAPI.ItemDefinition;

public class ShopSystem {
    private final InventoryAPI inventory = InventoryAPI.get();
    
    public void initialize() {
        // Register custom items
        inventory.register(new ItemDefinition(
            "mymod:magic_wand", "Magic Wand", 1, true, 500
        ));
    }
    
    public boolean purchaseItem(UUID playerId, String itemId, int price) {
        // Check if player can afford it
        if (!inventory.hasItem(playerId, "hytale:emerald", price)) {
            return false;
        }
        
        // Take payment
        if (!inventory.takeItem(playerId, "hytale:emerald", price)) {
            return false;
        }
        
        // Give item
        if (!inventory.giveItem(playerId, itemId, 1)) {
            // Refund if inventory is full
            inventory.giveItem(playerId, "hytale:emerald", price);
            return false;
        }
        
        return true;
    }
    
    public void giveStarterKit(UUID playerId) {
        inventory.clearInventory(playerId);
        
        // Weapons
        inventory.giveItem(playerId, "hytale:iron_sword", 1);
        inventory.giveItem(playerId, "hytale:bow", 1);
        inventory.giveItem(playerId, "hytale:arrow", 64);
        
        // Food
        inventory.giveItem(playerId, "hytale:bread", 16);
        
        // Tools
        inventory.giveItem(playerId, "hytale:iron_pickaxe", 1);
        inventory.giveItem(playerId, "hytale:torch", 64);
    }
}
```
