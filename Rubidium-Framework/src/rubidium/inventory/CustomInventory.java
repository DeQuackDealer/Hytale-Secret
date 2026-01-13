package rubidium.inventory;

import rubidium.api.player.Player;
import rubidium.inventory.InventoryBuilder.*;

import java.util.*;
import java.util.function.BiConsumer;

public class CustomInventory {
    
    private final String id;
    private final String title;
    private final int rows;
    private final Map<Integer, InventorySlot> slots;
    private final BiConsumer<Player, InventoryClickEvent> globalClickHandler;
    private final Runnable closeHandler;
    private final Set<UUID> viewers;
    
    public CustomInventory(String id, String title, int rows, Map<Integer, InventorySlot> slots,
                           BiConsumer<Player, InventoryClickEvent> globalClickHandler, Runnable closeHandler) {
        this.id = id;
        this.title = title;
        this.rows = rows;
        this.slots = new HashMap<>(slots);
        this.globalClickHandler = globalClickHandler;
        this.closeHandler = closeHandler;
        this.viewers = new HashSet<>();
    }
    
    public String getId() { return id; }
    public String getTitle() { return title; }
    public int getRows() { return rows; }
    public int getSize() { return rows * 9; }
    
    public void open(Player player) {
        viewers.add(player.getUUID());
        
        List<ItemStack> items = new ArrayList<>(Collections.nCopies(getSize(), null));
        for (Map.Entry<Integer, InventorySlot> entry : slots.entrySet()) {
            if (entry.getKey() < items.size()) {
                items.set(entry.getKey(), entry.getValue().item());
            }
        }
        
        player.sendPacket(new OpenInventoryPacket(id, title, rows, items));
    }
    
    public void close(Player player) {
        viewers.remove(player.getUUID());
        player.sendPacket(new CloseInventoryPacket(id));
        if (closeHandler != null && viewers.isEmpty()) {
            closeHandler.run();
        }
    }
    
    public void handleClick(Player player, int slot, ClickType clickType, boolean shiftClick) {
        InventoryClickEvent event = new InventoryClickEvent(slot, clickType, shiftClick);
        
        if (globalClickHandler != null) {
            globalClickHandler.accept(player, event);
        }
        
        InventorySlot slotData = slots.get(slot);
        if (slotData != null && slotData.clickHandler() != null) {
            slotData.clickHandler().accept(player, event);
        }
    }
    
    public void updateSlot(int slot, ItemStack item) {
        slots.put(slot, new InventorySlot(item, slots.getOrDefault(slot, new InventorySlot(null, null)).clickHandler()));
        
        for (UUID viewerId : viewers) {
        }
    }
    
    public void refresh(Player player) {
        if (viewers.contains(player.getUUID())) {
            close(player);
            open(player);
        }
    }
    
    public Set<UUID> getViewers() {
        return Collections.unmodifiableSet(viewers);
    }
    
    public record OpenInventoryPacket(String id, String title, int rows, List<ItemStack> items) {}
    public record CloseInventoryPacket(String id) {}
    public record UpdateSlotPacket(String inventoryId, int slot, ItemStack item) {}
}
