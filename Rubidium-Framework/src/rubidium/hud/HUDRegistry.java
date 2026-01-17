package rubidium.hud;

import rubidium.settings.PlayerSettings;
import rubidium.settings.SettingsRegistry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HUDRegistry {
    
    private static final HUDRegistry INSTANCE = new HUDRegistry();
    
    private final Map<String, HUDWidget> widgets = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> enabledWidgets = new ConcurrentHashMap<>();
    
    public static HUDRegistry get() {
        return INSTANCE;
    }
    
    public void registerWidget(HUDWidget widget) {
        widgets.put(widget.getId(), widget);
        System.out.println("[Rubidium-HUD] Registered widget: " + widget.getName());
    }
    
    public void unregisterWidget(String id) {
        widgets.remove(id);
    }
    
    public Optional<HUDWidget> getWidget(String id) {
        return Optional.ofNullable(widgets.get(id));
    }
    
    public Collection<HUDWidget> getAllWidgets() {
        return Collections.unmodifiableCollection(widgets.values());
    }
    
    public void enableWidget(UUID playerId, String widgetId) {
        enabledWidgets.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet()).add(widgetId);
    }
    
    public void disableWidget(UUID playerId, String widgetId) {
        Set<String> playerWidgets = enabledWidgets.get(playerId);
        if (playerWidgets != null) {
            playerWidgets.remove(widgetId);
        }
    }
    
    public boolean isWidgetEnabled(UUID playerId, String widgetId) {
        Set<String> playerWidgets = enabledWidgets.get(playerId);
        if (playerWidgets == null) {
            HUDWidget widget = widgets.get(widgetId);
            return widget != null && widget.isEnabledByDefault();
        }
        return playerWidgets.contains(widgetId);
    }
    
    public Set<String> getEnabledWidgets(UUID playerId) {
        Set<String> playerWidgets = enabledWidgets.get(playerId);
        if (playerWidgets == null) {
            Set<String> defaults = new HashSet<>();
            for (HUDWidget widget : widgets.values()) {
                if (widget.isEnabledByDefault()) {
                    defaults.add(widget.getId());
                }
            }
            return defaults;
        }
        return Collections.unmodifiableSet(playerWidgets);
    }
    
    public void renderAll(UUID playerId, RenderContext ctx) {
        PlayerSettings settings = SettingsRegistry.get().getPlayerSettings(playerId);
        
        for (String widgetId : getEnabledWidgets(playerId)) {
            HUDWidget widget = widgets.get(widgetId);
            if (widget != null && widget.isVisible(playerId)) {
                PlayerSettings.HUDPosition pos = settings.getHUDPosition(widgetId);
                if (pos != null && pos.isVisible()) {
                    widget.render(playerId, ctx, pos);
                }
            }
        }
    }
    
    public static abstract class HUDWidget {
        protected final String id;
        protected final String name;
        protected final String description;
        protected final boolean enabledByDefault;
        protected final boolean resizable;
        protected final boolean draggable;
        
        public HUDWidget(String id, String name, String description, boolean enabledByDefault, boolean resizable, boolean draggable) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.enabledByDefault = enabledByDefault;
            this.resizable = resizable;
            this.draggable = draggable;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public boolean isEnabledByDefault() { return enabledByDefault; }
        public boolean isResizable() { return resizable; }
        public boolean isDraggable() { return draggable; }
        
        public abstract boolean isVisible(UUID playerId);
        public abstract void render(UUID playerId, RenderContext ctx, PlayerSettings.HUDPosition position);
        public abstract int getDefaultWidth();
        public abstract int getDefaultHeight();
    }
    
    public static class RenderContext {
        private final int screenWidth;
        private final int screenHeight;
        private final float deltaTime;
        
        public RenderContext(int screenWidth, int screenHeight, float deltaTime) {
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
            this.deltaTime = deltaTime;
        }
        
        public int getScreenWidth() { return screenWidth; }
        public int getScreenHeight() { return screenHeight; }
        public float getDeltaTime() { return deltaTime; }
        
        public int resolveX(PlayerSettings.HUDPosition pos) {
            return switch (pos.getAnchor()) {
                case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> pos.getX();
                case TOP_CENTER, CENTER, BOTTOM_CENTER -> (screenWidth / 2) - (pos.getWidth() / 2) + pos.getX();
                case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> screenWidth - pos.getWidth() - pos.getX();
            };
        }
        
        public int resolveY(PlayerSettings.HUDPosition pos) {
            return switch (pos.getAnchor()) {
                case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> pos.getY();
                case CENTER_LEFT, CENTER, CENTER_RIGHT -> (screenHeight / 2) - (pos.getHeight() / 2) + pos.getY();
                case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> screenHeight - pos.getHeight() - pos.getY();
            };
        }
    }
}
