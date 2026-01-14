package rubidium.ui;

import java.util.*;
import java.util.function.BiConsumer;

public class ListWidget extends Widget {
    private final List<Object> items = new ArrayList<>();
    private int selectedIndex = -1;
    private boolean multiSelect = false;
    private final Set<Integer> selectedIndices = new HashSet<>();
    private int itemHeight = 24;
    private BiConsumer<Integer, Object> onSelect;
    
    public ListWidget(String id) { super(id); }
    
    @Override public String getType() { return "list"; }
    
    public ListWidget setItems(List<?> items) { this.items.clear(); this.items.addAll(items); setProperty("items", items); return this; }
    public List<Object> getItems() { return Collections.unmodifiableList(items); }
    public ListWidget addItem(Object item) { items.add(item); return this; }
    public ListWidget removeItem(int index) { if (index >= 0 && index < items.size()) items.remove(index); return this; }
    public ListWidget clearItems() { items.clear(); selectedIndex = -1; selectedIndices.clear(); return this; }
    
    public int getSelectedIndex() { return selectedIndex; }
    public ListWidget setSelectedIndex(int index) { this.selectedIndex = index; setProperty("selectedIndex", index); return this; }
    public Object getSelectedItem() { return selectedIndex >= 0 && selectedIndex < items.size() ? items.get(selectedIndex) : null; }
    
    public boolean isMultiSelect() { return multiSelect; }
    public ListWidget setMultiSelect(boolean multi) { this.multiSelect = multi; setProperty("multiSelect", multi); return this; }
    public Set<Integer> getSelectedIndices() { return Collections.unmodifiableSet(selectedIndices); }
    
    public int getItemHeight() { return itemHeight; }
    public ListWidget setItemHeight(int height) { this.itemHeight = height; setProperty("itemHeight", height); return this; }
    
    public ListWidget onSelect(BiConsumer<Integer, Object> handler) { this.onSelect = handler; return this; }
}
