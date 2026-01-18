package rubidium.ui.components;

import java.util.ArrayList;
import java.util.List;

public class UIContainer extends UIComponent {
    
    private String title;
    private int width;
    private int height;
    private int background = 0x2D2D35;
    private boolean centered = false;
    private boolean scrollable = false;
    private int cornerRadius = 0;
    private final List<UIComponent> children = new ArrayList<>();
    
    public UIContainer(String id) {
        super(id);
    }
    
    public UIContainer setTitle(String title) {
        this.title = title;
        return this;
    }
    
    public UIContainer setSize(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }
    
    public UIContainer setBackground(int color) {
        this.background = color;
        return this;
    }
    
    public UIContainer setCentered(boolean centered) {
        this.centered = centered;
        return this;
    }
    
    public UIContainer setScrollable(boolean scrollable) {
        this.scrollable = scrollable;
        return this;
    }
    
    public UIContainer setCornerRadius(int radius) {
        this.cornerRadius = radius;
        return this;
    }
    
    @Override
    public UIContainer setPosition(int x, int y) {
        super.setPosition(x, y);
        return this;
    }
    
    public UIContainer addChild(UIComponent child) {
        children.add(child);
        return this;
    }
    
    public List<UIComponent> getChildren() {
        return children;
    }
    
    public String getTitle() { return title; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getBackground() { return background; }
    public boolean isCentered() { return centered; }
    public boolean isScrollable() { return scrollable; }
    public int getCornerRadius() { return cornerRadius; }
}
