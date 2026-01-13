package rubidium.gui;

import rubidium.api.player.Player;
import rubidium.inventory.ItemStack;

import java.util.*;
import java.util.function.Consumer;

public abstract class Menu {
    
    protected final String title;
    protected final int rows;
    protected final Map<Integer, MenuItem> items;
    protected final Set<UUID> viewers;
    
    protected Menu(String title, int rows) {
        this.title = title;
        this.rows = Math.min(Math.max(rows, 1), 6);
        this.items = new HashMap<>();
        this.viewers = new HashSet<>();
    }
    
    public String getTitle() { return title; }
    public int getRows() { return rows; }
    public int getSize() { return rows * 9; }
    
    public void setItem(int slot, MenuItem item) {
        if (slot >= 0 && slot < getSize()) {
            items.put(slot, item);
        }
    }
    
    public void setItem(int row, int col, MenuItem item) {
        setItem(row * 9 + col, item);
    }
    
    public MenuItem getItem(int slot) {
        return items.get(slot);
    }
    
    public void removeItem(int slot) {
        items.remove(slot);
    }
    
    public void fill(MenuItem item) {
        for (int i = 0; i < getSize(); i++) {
            setItem(i, item);
        }
    }
    
    public void fillBorder(MenuItem item) {
        for (int i = 0; i < 9; i++) {
            setItem(0, i, item);
            setItem(rows - 1, i, item);
        }
        for (int i = 1; i < rows - 1; i++) {
            setItem(i, 0, item);
            setItem(i, 8, item);
        }
    }
    
    public void fillRow(int row, MenuItem item) {
        for (int col = 0; col < 9; col++) {
            setItem(row, col, item);
        }
    }
    
    public void fillColumn(int col, MenuItem item) {
        for (int row = 0; row < rows; row++) {
            setItem(row, col, item);
        }
    }
    
    public abstract void open(Player player);
    
    public abstract void close(Player player);
    
    public abstract void update(Player player);
    
    public void handleClick(Player player, int slot, ClickType clickType) {
        MenuItem item = items.get(slot);
        if (item != null && item.getClickHandler() != null) {
            item.getClickHandler().accept(new ClickContext(player, this, slot, clickType));
        }
    }
    
    public void handleClose(Player player) {
        viewers.remove(player.getUUID());
        onClose(player);
    }
    
    protected void onClose(Player player) {}
    
    public Set<UUID> getViewers() {
        return Collections.unmodifiableSet(viewers);
    }
    
    public boolean isViewing(Player player) {
        return viewers.contains(player.getUUID());
    }
    
    public static class MenuItem {
        private final ItemStack itemStack;
        private Consumer<ClickContext> clickHandler;
        
        public MenuItem(ItemStack itemStack) {
            this.itemStack = itemStack;
        }
        
        public MenuItem(ItemStack itemStack, Consumer<ClickContext> clickHandler) {
            this.itemStack = itemStack;
            this.clickHandler = clickHandler;
        }
        
        public ItemStack getItemStack() { return itemStack; }
        public Consumer<ClickContext> getClickHandler() { return clickHandler; }
        
        public MenuItem onClick(Consumer<ClickContext> handler) {
            this.clickHandler = handler;
            return this;
        }
        
        public static MenuItem of(ItemStack item) {
            return new MenuItem(item);
        }
        
        public static MenuItem of(ItemStack item, Consumer<ClickContext> handler) {
            return new MenuItem(item, handler);
        }
    }
    
    public record ClickContext(Player player, Menu menu, int slot, ClickType clickType) {
        public void close() {
            menu.close(player);
        }
        
        public void reopen() {
            menu.open(player);
        }
    }
    
    public enum ClickType {
        LEFT, RIGHT, SHIFT_LEFT, SHIFT_RIGHT, MIDDLE, DROP, CONTROL_DROP, DOUBLE_CLICK
    }
    
    public static abstract class PaginatedMenu extends Menu {
        protected int currentPage = 0;
        protected int itemsPerPage;
        protected List<MenuItem> pageItems;
        protected int previousSlot = -1;
        protected int nextSlot = -1;
        
        protected PaginatedMenu(String title, int rows) {
            super(title, rows);
            this.pageItems = new ArrayList<>();
            this.itemsPerPage = (rows - 1) * 9;
        }
        
        public void setPageItems(List<MenuItem> items) {
            this.pageItems = new ArrayList<>(items);
        }
        
        public int getMaxPages() {
            return (int) Math.ceil((double) pageItems.size() / itemsPerPage);
        }
        
        public int getCurrentPage() { return currentPage; }
        
        public void setCurrentPage(int page) {
            this.currentPage = Math.max(0, Math.min(page, getMaxPages() - 1));
        }
        
        public void nextPage() {
            if (currentPage < getMaxPages() - 1) {
                currentPage++;
            }
        }
        
        public void previousPage() {
            if (currentPage > 0) {
                currentPage--;
            }
        }
        
        public void setPaginationSlots(int previousSlot, int nextSlot) {
            this.previousSlot = previousSlot;
            this.nextSlot = nextSlot;
        }
        
        protected void populatePage() {
            int startIndex = currentPage * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, pageItems.size());
            
            for (int i = 0; i < itemsPerPage; i++) {
                int itemIndex = startIndex + i;
                if (itemIndex < endIndex) {
                    setItem(i, pageItems.get(itemIndex));
                } else {
                    removeItem(i);
                }
            }
        }
    }
}
