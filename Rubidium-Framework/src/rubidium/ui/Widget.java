package rubidium.ui;

import java.util.*;
import java.util.function.Consumer;

/**
 * Base class for all UI widgets.
 */
public abstract class Widget {
    
    protected String id;
    protected int x, y, width, height;
    protected boolean visible = true;
    protected boolean enabled = true;
    protected String tooltip;
    protected final Map<String, Object> properties = new HashMap<>();
    protected final List<Widget> children = new ArrayList<>();
    protected Widget parent;
    
    protected final Map<String, List<Consumer<RubidiumUI.UIInputEvent>>> eventHandlers = new HashMap<>();
    
    public Widget(String id) {
        this.id = id;
    }
    
    public abstract String getType();
    
    public String getId() { return id; }
    
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    
    public Widget setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }
    
    public Widget setSize(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }
    
    public Widget setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        return this;
    }
    
    public boolean isVisible() { return visible; }
    public Widget setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }
    
    public boolean isEnabled() { return enabled; }
    public Widget setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }
    
    public String getTooltip() { return tooltip; }
    public Widget setTooltip(String tooltip) {
        this.tooltip = tooltip;
        return this;
    }
    
    public Widget setProperty(String key, Object value) {
        properties.put(key, value);
        return this;
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, T defaultValue) {
        Object value = properties.get(key);
        return value != null ? (T) value : defaultValue;
    }
    
    public Widget addChild(Widget child) {
        children.add(child);
        child.parent = this;
        return this;
    }
    
    public Widget removeChild(Widget child) {
        children.remove(child);
        child.parent = null;
        return this;
    }
    
    public List<Widget> getChildren() {
        return Collections.unmodifiableList(children);
    }
    
    public Widget getParent() { return parent; }
    
    public Widget on(String event, Consumer<RubidiumUI.UIInputEvent> handler) {
        eventHandlers.computeIfAbsent(event, k -> new ArrayList<>()).add(handler);
        return this;
    }
    
    public void handleEvent(String event, RubidiumUI.UIInputEvent e) {
        List<Consumer<RubidiumUI.UIInputEvent>> handlers = eventHandlers.get(event);
        if (handlers != null) {
            for (Consumer<RubidiumUI.UIInputEvent> handler : handlers) {
                handler.accept(e);
            }
        }
    }
    
    public RubidiumUI.WidgetData toData() {
        List<RubidiumUI.WidgetData> childData = children.stream()
            .map(Widget::toData)
            .toList();
            
        return new RubidiumUI.WidgetData(
            id, getType(), x, y, width, height,
            new HashMap<>(properties), childData
        );
    }
}
