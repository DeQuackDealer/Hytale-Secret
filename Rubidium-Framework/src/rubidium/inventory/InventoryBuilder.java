package rubidium.inventory;

import rubidium.api.player.Player;

import java.util.*;
import java.util.function.BiConsumer;

public class InventoryBuilder {
    
    private final String id;
    private String title;
    private int rows;
    private final Map<Integer, InventorySlot> slots;
    private BiConsumer<Player, InventoryClickEvent> globalClickHandler;
    private Runnable closeHandler;
    
    private InventoryBuilder(String id) {
        this.id = id;
        this.title = "Inventory";
        this.rows = 3;
        this.slots = new HashMap<>();
    }
    
    public static InventoryBuilder create(String id) {
        return new InventoryBuilder(id);
    }
    
    public InventoryBuilder title(String title) {
        this.title = title;
        return this;
    }
    
    public InventoryBuilder rows(int rows) {
        this.rows = Math.max(1, Math.min(6, rows));
        return this;
    }
    
    public InventoryBuilder setItem(int slot, ItemStack item) {
        return setItem(slot, item, null);
    }
    
    public InventoryBuilder setItem(int slot, ItemStack item, BiConsumer<Player, InventoryClickEvent> clickHandler) {
        slots.put(slot, new InventorySlot(item, clickHandler));
        return this;
    }
    
    public InventoryBuilder setItem(int row, int col, ItemStack item) {
        return setItem(row * 9 + col, item);
    }
    
    public InventoryBuilder setItem(int row, int col, ItemStack item, BiConsumer<Player, InventoryClickEvent> clickHandler) {
        return setItem(row * 9 + col, item, clickHandler);
    }
    
    public InventoryBuilder fillRow(int row, ItemStack item) {
        for (int col = 0; col < 9; col++) {
            setItem(row, col, item);
        }
        return this;
    }
    
    public InventoryBuilder fillBorder(ItemStack item) {
        for (int col = 0; col < 9; col++) {
            setItem(0, col, item);
            setItem(rows - 1, col, item);
        }
        for (int row = 1; row < rows - 1; row++) {
            setItem(row, 0, item);
            setItem(row, 8, item);
        }
        return this;
    }
    
    public InventoryBuilder fill(ItemStack item) {
        for (int slot = 0; slot < rows * 9; slot++) {
            if (!slots.containsKey(slot)) {
                setItem(slot, item);
            }
        }
        return this;
    }
    
    public InventoryBuilder onGlobalClick(BiConsumer<Player, InventoryClickEvent> handler) {
        this.globalClickHandler = handler;
        return this;
    }
    
    public InventoryBuilder onClose(Runnable handler) {
        this.closeHandler = handler;
        return this;
    }
    
    public CustomInventory build() {
        return new CustomInventory(id, title, rows, slots, globalClickHandler, closeHandler);
    }
    
    public void openFor(Player player) {
        build().open(player);
    }
    
    public record InventorySlot(rubidium.inventory.ItemStack item, BiConsumer<Player, InventoryClickEvent> clickHandler) {}
    
    public record InventoryClickEvent(int slot, ClickType clickType, boolean isShiftClick) {}
    public enum ClickType { LEFT, RIGHT, MIDDLE, SHIFT_LEFT, SHIFT_RIGHT, DROP, DOUBLE_CLICK }
}
