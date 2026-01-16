package rubidium.api.ui;

import java.util.*;

public class UIScreen extends UIComponent {
    
    private String title;
    private int backgroundColor = 0xC0101010;
    private boolean pauseGame = false;
    private boolean closeable = true;
    
    public UIScreen(String id) {
        super(id);
        this.title = id;
        this.width = 176;
        this.height = 166;
    }
    
    public String getTitle() { return title; }
    public int getBackgroundColor() { return backgroundColor; }
    public boolean pausesGame() { return pauseGame; }
    public boolean isCloseable() { return closeable; }
    
    public UIScreen title(String title) { this.title = title; return this; }
    public UIScreen background(int color) { this.backgroundColor = color; return this; }
    public UIScreen pauseGame(boolean pause) { this.pauseGame = pause; return this; }
    public UIScreen closeable(boolean close) { this.closeable = close; return this; }
    
    @Override
    public UIScreen setSize(int w, int h) { super.setSize(w, h); return this; }
    
    @Override
    public UIScreen add(UIComponent child) { super.add(child); return this; }
    
    public UIScreen row(UIComponent... components) {
        int xOffset = 8;
        for (UIComponent c : components) {
            c.setPosition(xOffset, c.getY());
            add(c);
            xOffset += c.getWidth() + 4;
        }
        return this;
    }
    
    public UIScreen column(UIComponent... components) {
        int yOffset = 20;
        for (UIComponent c : components) {
            c.setPosition(c.getX(), yOffset);
            add(c);
            yOffset += c.getHeight() + 4;
        }
        return this;
    }
    
    public Optional<UIComponent> findById(String id) {
        if (this.id.equals(id)) return Optional.of(this);
        for (UIComponent child : children) {
            if (child.getId().equals(id)) return Optional.of(child);
        }
        return Optional.empty();
    }
}
