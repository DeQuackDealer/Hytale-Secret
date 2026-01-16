package rubidium.api.ui.components;

import rubidium.api.ui.UIComponent;
import java.util.*;

public class UIList extends UIComponent {
    
    private List<String> items = new ArrayList<>();
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private int itemHeight = 20;
    private int selectedColor = 0xFF4040FF;
    private int hoverColor = 0x40FFFFFF;
    
    public UIList(String id) {
        super(id);
        this.width = 150;
        this.height = 100;
    }
    
    public List<String> getItems() { return items; }
    public int getSelectedIndex() { return selectedIndex; }
    public int getScrollOffset() { return scrollOffset; }
    public int getItemHeight() { return itemHeight; }
    public int getSelectedColor() { return selectedColor; }
    public int getHoverColor() { return hoverColor; }
    
    public String getSelectedItem() {
        return selectedIndex >= 0 && selectedIndex < items.size() ? items.get(selectedIndex) : null;
    }
    
    public UIList items(String... items) { this.items = new ArrayList<>(Arrays.asList(items)); return this; }
    public UIList items(List<String> items) { this.items = new ArrayList<>(items); return this; }
    public UIList addItem(String item) { this.items.add(item); return this; }
    public UIList select(int index) { this.selectedIndex = index; return this; }
    public UIList scroll(int offset) { this.scrollOffset = offset; return this; }
    public UIList itemHeight(int h) { this.itemHeight = h; return this; }
    public UIList selectedColor(int color) { this.selectedColor = color; return this; }
    public UIList hoverColor(int color) { this.hoverColor = color; return this; }
    
    @Override
    public UIList setPosition(int x, int y) { super.setPosition(x, y); return this; }
    
    @Override
    public UIList setSize(int w, int h) { super.setSize(w, h); return this; }
    
    @Override
    public UIList on(String event, java.util.function.Consumer<UIEvent> handler) {
        super.on(event, handler);
        return this;
    }
}
