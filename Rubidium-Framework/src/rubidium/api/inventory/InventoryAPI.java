package rubidium.api.inventory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class InventoryAPI {
    
    private static final Map<String, InventoryTemplate> templates = new ConcurrentHashMap<>();
    private static final Map<UUID, CustomInventory> openInventories = new ConcurrentHashMap<>();
    
    private InventoryAPI() {}
    
    public static InventoryTemplate.Builder createTemplate(String id) {
        return new InventoryTemplate.Builder(id);
    }
    
    public static InventoryTemplate registerTemplate(InventoryTemplate template) {
        templates.put(template.getId(), template);
        return template;
    }
    
    public static InventoryTemplate registerTemplate(InventoryTemplate.Builder builder) {
        return registerTemplate(builder.build());
    }
    
    public static Optional<InventoryTemplate> getTemplate(String id) {
        return Optional.ofNullable(templates.get(id));
    }
    
    public static CustomInventory create(String title, int rows) {
        return new CustomInventory(title, rows);
    }
    
    public static CustomInventory fromTemplate(String templateId) {
        InventoryTemplate template = templates.get(templateId);
        if (template == null) {
            throw new IllegalArgumentException("Unknown template: " + templateId);
        }
        return template.createInstance();
    }
    
    public static void open(UUID playerId, CustomInventory inventory) {
        openInventories.put(playerId, inventory);
    }
    
    public static void close(UUID playerId) {
        openInventories.remove(playerId);
    }
    
    public static Optional<CustomInventory> getOpen(UUID playerId) {
        return Optional.ofNullable(openInventories.get(playerId));
    }
    
    public static InventoryTemplate chest(String id, String title) {
        return createTemplate(id).title(title).rows(3).build();
    }
    
    public static InventoryTemplate doubleChest(String id, String title) {
        return createTemplate(id).title(title).rows(6).build();
    }
    
    public static InventoryTemplate hopper(String id, String title) {
        return createTemplate(id).title(title).rows(1).size(5).build();
    }
    
    public record InventoryTemplate(
        String id,
        String title,
        int rows,
        int size,
        Map<Integer, ItemSlot> slots,
        boolean allowPlayerInventory
    ) {
        public CustomInventory createInstance() {
            CustomInventory inv = new CustomInventory(title, rows);
            for (var entry : slots.entrySet()) {
                inv.setSlot(entry.getKey(), entry.getValue());
            }
            return inv;
        }
        
        public String getId() { return id; }
        
        public static class Builder {
            private final String id;
            private String title = "";
            private int rows = 3;
            private int size = -1;
            private Map<Integer, ItemSlot> slots = new HashMap<>();
            private boolean allowPlayerInventory = true;
            
            public Builder(String id) { this.id = id; }
            
            public Builder title(String title) { this.title = title; return this; }
            public Builder rows(int rows) { this.rows = Math.min(6, Math.max(1, rows)); return this; }
            public Builder size(int size) { this.size = size; return this; }
            public Builder allowPlayerInventory(boolean allow) { this.allowPlayerInventory = allow; return this; }
            
            public Builder slot(int index, ItemSlot slot) {
                slots.put(index, slot);
                return this;
            }
            
            public Builder slot(int index, String itemId) {
                slots.put(index, new ItemSlot(itemId, 1, null, null));
                return this;
            }
            
            public Builder slot(int index, String itemId, Consumer<ClickContext> onClick) {
                slots.put(index, new ItemSlot(itemId, 1, onClick, null));
                return this;
            }
            
            public Builder fill(String itemId) {
                int actualSize = size > 0 ? size : rows * 9;
                for (int i = 0; i < actualSize; i++) {
                    if (!slots.containsKey(i)) {
                        slots.put(i, new ItemSlot(itemId, 1, null, null));
                    }
                }
                return this;
            }
            
            public Builder border(String itemId) {
                int actualSize = size > 0 ? size : rows * 9;
                for (int i = 0; i < 9; i++) {
                    slots.putIfAbsent(i, new ItemSlot(itemId, 1, null, null));
                    slots.putIfAbsent(actualSize - 9 + i, new ItemSlot(itemId, 1, null, null));
                }
                for (int i = 1; i < rows - 1; i++) {
                    slots.putIfAbsent(i * 9, new ItemSlot(itemId, 1, null, null));
                    slots.putIfAbsent(i * 9 + 8, new ItemSlot(itemId, 1, null, null));
                }
                return this;
            }
            
            public InventoryTemplate build() {
                int actualSize = size > 0 ? size : rows * 9;
                return new InventoryTemplate(id, title, rows, actualSize, Map.copyOf(slots), allowPlayerInventory);
            }
        }
    }
    
    public static class CustomInventory {
        private final String title;
        private final int rows;
        private final int size;
        private final ItemSlot[] slots;
        private Consumer<UUID> onOpen;
        private Consumer<UUID> onClose;
        private BiConsumer<UUID, ClickContext> onClick;
        
        public CustomInventory(String title, int rows) {
            this.title = title;
            this.rows = rows;
            this.size = rows * 9;
            this.slots = new ItemSlot[size];
        }
        
        public String getTitle() { return title; }
        public int getRows() { return rows; }
        public int getSize() { return size; }
        
        public void setSlot(int index, ItemSlot slot) {
            if (index >= 0 && index < size) {
                slots[index] = slot;
            }
        }
        
        public void setSlot(int index, String itemId) {
            setSlot(index, new ItemSlot(itemId, 1, null, null));
        }
        
        public void setSlot(int index, String itemId, int amount) {
            setSlot(index, new ItemSlot(itemId, amount, null, null));
        }
        
        public void setSlot(int index, String itemId, Consumer<ClickContext> onClick) {
            setSlot(index, new ItemSlot(itemId, 1, onClick, null));
        }
        
        public Optional<ItemSlot> getSlot(int index) {
            return index >= 0 && index < size ? Optional.ofNullable(slots[index]) : Optional.empty();
        }
        
        public void clearSlot(int index) {
            if (index >= 0 && index < size) {
                slots[index] = null;
            }
        }
        
        public void clear() {
            Arrays.fill(slots, null);
        }
        
        public void fill(String itemId) {
            for (int i = 0; i < size; i++) {
                if (slots[i] == null) {
                    slots[i] = new ItemSlot(itemId, 1, null, null);
                }
            }
        }
        
        public void onOpen(Consumer<UUID> handler) { this.onOpen = handler; }
        public void onClose(Consumer<UUID> handler) { this.onClose = handler; }
        public void onClick(BiConsumer<UUID, ClickContext> handler) { this.onClick = handler; }
        
        public void handleOpen(UUID playerId) {
            if (onOpen != null) onOpen.accept(playerId);
        }
        
        public void handleClose(UUID playerId) {
            if (onClose != null) onClose.accept(playerId);
        }
        
        public void handleClick(UUID playerId, int slot, ClickType clickType) {
            ClickContext ctx = new ClickContext(playerId, slot, clickType, slots[slot]);
            
            if (onClick != null) {
                onClick.accept(playerId, ctx);
            }
            
            if (slot >= 0 && slot < size && slots[slot] != null && slots[slot].onClick() != null) {
                slots[slot].onClick().accept(ctx);
            }
        }
        
        public int firstEmpty() {
            for (int i = 0; i < size; i++) {
                if (slots[i] == null) return i;
            }
            return -1;
        }
        
        public boolean addItem(String itemId, int amount) {
            int empty = firstEmpty();
            if (empty == -1) return false;
            setSlot(empty, itemId, amount);
            return true;
        }
    }
    
    public record ItemSlot(
        String itemId,
        int amount,
        Consumer<ClickContext> onClick,
        Map<String, Object> metadata
    ) {
        public ItemSlot withAmount(int newAmount) {
            return new ItemSlot(itemId, newAmount, onClick, metadata);
        }
        
        public ItemSlot withClick(Consumer<ClickContext> handler) {
            return new ItemSlot(itemId, amount, handler, metadata);
        }
    }
    
    public record ClickContext(
        UUID playerId,
        int slot,
        ClickType clickType,
        ItemSlot clickedItem
    ) {}
    
    public enum ClickType {
        LEFT,
        RIGHT,
        SHIFT_LEFT,
        SHIFT_RIGHT,
        MIDDLE,
        NUMBER_KEY,
        DROP,
        CTRL_DROP,
        DOUBLE_CLICK
    }
    
    public static class PagedInventory {
        private final String title;
        private final int itemsPerPage;
        private final List<ItemSlot> items = new ArrayList<>();
        private int currentPage = 0;
        private int prevButtonSlot = 45;
        private int nextButtonSlot = 53;
        private String prevButtonItem = "arrow";
        private String nextButtonItem = "arrow";
        
        public PagedInventory(String title, int itemsPerPage) {
            this.title = title;
            this.itemsPerPage = itemsPerPage;
        }
        
        public void addItem(ItemSlot item) { items.add(item); }
        public void addItem(String itemId) { items.add(new ItemSlot(itemId, 1, null, null)); }
        public void addItem(String itemId, Consumer<ClickContext> onClick) { items.add(new ItemSlot(itemId, 1, onClick, null)); }
        public void clearItems() { items.clear(); currentPage = 0; }
        
        public void setPrevButton(int slot, String itemId) { this.prevButtonSlot = slot; this.prevButtonItem = itemId; }
        public void setNextButton(int slot, String itemId) { this.nextButtonSlot = slot; this.nextButtonItem = itemId; }
        
        public int getPageCount() {
            return (int) Math.ceil((double) items.size() / itemsPerPage);
        }
        
        public int getCurrentPage() { return currentPage; }
        
        public void setPage(int page) {
            this.currentPage = Math.max(0, Math.min(getPageCount() - 1, page));
        }
        
        public void nextPage() { setPage(currentPage + 1); }
        public void prevPage() { setPage(currentPage - 1); }
        
        public CustomInventory render() {
            String pageTitle = title + " (" + (currentPage + 1) + "/" + Math.max(1, getPageCount()) + ")";
            CustomInventory inv = new CustomInventory(pageTitle, 6);
            
            int start = currentPage * itemsPerPage;
            int end = Math.min(start + itemsPerPage, items.size());
            
            for (int i = start; i < end; i++) {
                inv.setSlot(i - start, items.get(i));
            }
            
            if (currentPage > 0) {
                inv.setSlot(prevButtonSlot, prevButtonItem, ctx -> {
                    prevPage();
                });
            }
            
            if (currentPage < getPageCount() - 1) {
                inv.setSlot(nextButtonSlot, nextButtonItem, ctx -> {
                    nextPage();
                });
            }
            
            return inv;
        }
    }
}
