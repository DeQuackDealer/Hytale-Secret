package rubidium.inventory;

import rubidium.api.player.Player;
import rubidium.core.logging.RubidiumLogger;
import rubidium.inventory.InventoryBuilder.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InventoryManager {
    
    private final RubidiumLogger logger;
    private final Map<UUID, CustomInventory> openInventories;
    private final Map<String, CustomInventory> registeredInventories;
    
    public InventoryManager(RubidiumLogger logger) {
        this.logger = logger;
        this.openInventories = new ConcurrentHashMap<>();
        this.registeredInventories = new ConcurrentHashMap<>();
    }
    
    public void registerInventory(CustomInventory inventory) {
        registeredInventories.put(inventory.getId(), inventory);
    }
    
    public Optional<CustomInventory> getInventory(String id) {
        return Optional.ofNullable(registeredInventories.get(id));
    }
    
    public void openInventory(Player player, String inventoryId) {
        CustomInventory inv = registeredInventories.get(inventoryId);
        if (inv != null) {
            closeCurrentInventory(player);
            openInventories.put(player.getUUID(), inv);
            inv.open(player);
        }
    }
    
    public void openInventory(Player player, CustomInventory inventory) {
        closeCurrentInventory(player);
        openInventories.put(player.getUUID(), inventory);
        inventory.open(player);
    }
    
    public void closeCurrentInventory(Player player) {
        CustomInventory current = openInventories.remove(player.getUUID());
        if (current != null) {
            current.close(player);
        }
    }
    
    public Optional<CustomInventory> getOpenInventory(Player player) {
        return Optional.ofNullable(openInventories.get(player.getUUID()));
    }
    
    public void handleClick(Player player, int slot, ClickType clickType, boolean shiftClick) {
        CustomInventory inv = openInventories.get(player.getUUID());
        if (inv != null) {
            inv.handleClick(player, slot, clickType, shiftClick);
        }
    }
    
    public void onPlayerQuit(Player player) {
        closeCurrentInventory(player);
    }
    
    public PaginatedInventory createPaginated(String id, String title, List<ItemStack> items, int itemsPerPage) {
        return new PaginatedInventory(id, title, items, itemsPerPage);
    }
    
    public static class PaginatedInventory {
        private final String id;
        private final String title;
        private final List<ItemStack> items;
        private final int itemsPerPage;
        private int currentPage;
        
        public PaginatedInventory(String id, String title, List<ItemStack> items, int itemsPerPage) {
            this.id = id;
            this.title = title;
            this.items = new ArrayList<>(items);
            this.itemsPerPage = Math.min(itemsPerPage, 45);
            this.currentPage = 0;
        }
        
        public int getTotalPages() {
            return (int) Math.ceil((double) items.size() / itemsPerPage);
        }
        
        public void nextPage() {
            if (currentPage < getTotalPages() - 1) {
                currentPage++;
            }
        }
        
        public void previousPage() {
            if (currentPage > 0) {
                currentPage--;
            }
        }
        
        public void setPage(int page) {
            this.currentPage = Math.max(0, Math.min(page, getTotalPages() - 1));
        }
        
        public CustomInventory buildForPage(int page) {
            InventoryBuilder builder = InventoryBuilder.create(id + "_page_" + page)
                .title(title + " (Page " + (page + 1) + "/" + getTotalPages() + ")")
                .rows(6);
            
            int startIndex = page * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, items.size());
            
            for (int i = startIndex; i < endIndex; i++) {
                builder.setItem(i - startIndex, items.get(i));
            }
            
            if (page > 0) {
                builder.setItem(5, 0, ItemStack.builder("arrow").displayName("Previous Page").build(), (p, e) -> previousPage());
            }
            if (page < getTotalPages() - 1) {
                builder.setItem(5, 8, ItemStack.builder("arrow").displayName("Next Page").build(), (p, e) -> nextPage());
            }
            
            return builder.build();
        }
        
        public void openFor(Player player) {
            buildForPage(currentPage).open(player);
        }
    }
}
