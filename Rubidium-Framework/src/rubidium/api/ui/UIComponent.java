package rubidium.api.ui;

import java.util.*;
import java.util.function.Consumer;

public abstract class UIComponent {
    
    protected String id;
    protected int x, y;
    protected int width, height;
    protected boolean visible = true;
    protected boolean enabled = true;
    protected UIComponent parent;
    protected List<UIComponent> children = new ArrayList<>();
    protected Map<String, Consumer<UIEvent>> eventHandlers = new HashMap<>();
    
    public UIComponent(String id) {
        this.id = id;
    }
    
    public String getId() { return id; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean isVisible() { return visible; }
    public boolean isEnabled() { return enabled; }
    
    public UIComponent setPosition(int x, int y) { this.x = x; this.y = y; return this; }
    public UIComponent setSize(int w, int h) { this.width = w; this.height = h; return this; }
    public UIComponent setVisible(boolean v) { this.visible = v; return this; }
    public UIComponent setEnabled(boolean e) { this.enabled = e; return this; }
    
    public UIComponent add(UIComponent child) {
        child.parent = this;
        children.add(child);
        return this;
    }
    
    public UIComponent on(String event, Consumer<UIEvent> handler) {
        eventHandlers.put(event, handler);
        return this;
    }
    
    public void fireEvent(String event, UIEvent e) {
        Consumer<UIEvent> handler = eventHandlers.get(event);
        if (handler != null) handler.accept(e);
    }
    
    public record UIEvent(String type, Object source, Object data) {}
}
