package rubidium.ui;

import rubidium.hytale.api.player.Player;

import java.util.*;

/**
 * Represents a UI screen containing widgets.
 */
public class UIScreen {
    
    private final String id;
    private String title;
    private final List<Widget> widgets = new ArrayList<>();
    private final Map<String, Widget> widgetById = new HashMap<>();
    private ScreenLayer layer = ScreenLayer.MENU;
    private boolean pausesGame = false;
    private boolean capturesMouse = true;
    
    public UIScreen(String id) {
        this.id = id;
    }
    
    public String getId() { return id; }
    
    public String getTitle() { return title; }
    public UIScreen setTitle(String title) {
        this.title = title;
        return this;
    }
    
    public ScreenLayer getLayer() { return layer; }
    public UIScreen setLayer(ScreenLayer layer) {
        this.layer = layer;
        return this;
    }
    
    public boolean pausesGame() { return pausesGame; }
    public UIScreen setPausesGame(boolean pauses) {
        this.pausesGame = pauses;
        return this;
    }
    
    public boolean capturesMouse() { return capturesMouse; }
    public UIScreen setCapturesMouse(boolean captures) {
        this.capturesMouse = captures;
        return this;
    }
    
    public UIScreen addWidget(Widget widget) {
        widgets.add(widget);
        widgetById.put(widget.getId(), widget);
        registerChildren(widget);
        return this;
    }
    
    private void registerChildren(Widget widget) {
        for (Widget child : widget.getChildren()) {
            widgetById.put(child.getId(), child);
            registerChildren(child);
        }
    }
    
    public UIScreen removeWidget(String widgetId) {
        Widget widget = widgetById.remove(widgetId);
        if (widget != null) {
            widgets.remove(widget);
        }
        return this;
    }
    
    public Optional<Widget> getWidget(String id) {
        return Optional.ofNullable(widgetById.get(id));
    }
    
    public List<Widget> getWidgets() {
        return Collections.unmodifiableList(widgets);
    }
    
    public void handleInput(Player player, String widgetId, RubidiumUI.UIInputEvent event) {
        Widget widget = widgetById.get(widgetId);
        if (widget != null && widget.isEnabled()) {
            widget.handleEvent(event.type().name().toLowerCase(), event);
        }
    }
    
    public void onOpen(Player player) {}
    public void onClose(Player player) {}
    public void onUpdate(Player player) {}
    
    public List<RubidiumUI.WidgetData> toWidgetData() {
        return widgets.stream()
            .filter(Widget::isVisible)
            .map(Widget::toData)
            .toList();
    }
    
    public enum ScreenLayer {
        HUD,
        OVERLAY,
        MENU,
        MODAL,
        DEBUG
    }
}
