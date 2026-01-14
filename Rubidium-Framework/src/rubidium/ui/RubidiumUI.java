package rubidium.ui;

import rubidium.core.feature.*;
import rubidium.hytale.api.player.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Main UI framework for Rubidium features.
 * Provides declarative UI building, theming, and state management.
 */
public class RubidiumUI implements FeatureLifecycle {
    
    private static final Logger logger = Logger.getLogger("Rubidium-UI");
    
    private static RubidiumUI instance;
    
    private final WidgetRegistry widgetRegistry;
    private final ThemeManager themeManager;
    private final StateStore stateStore;
    private final LayoutEngine layoutEngine;
    
    private final Map<UUID, PlayerUIState> playerStates = new ConcurrentHashMap<>();
    private final Map<String, UIScreen> registeredScreens = new ConcurrentHashMap<>();
    
    private volatile boolean running = false;
    
    public RubidiumUI() {
        this.widgetRegistry = new WidgetRegistry();
        this.themeManager = new ThemeManager();
        this.stateStore = new StateStore();
        this.layoutEngine = new LayoutEngine();
    }
    
    public static RubidiumUI getInstance() {
        if (instance == null) {
            instance = new RubidiumUI();
        }
        return instance;
    }
    
    @Override
    public String getFeatureId() { return "ui"; }
    
    @Override
    public String getFeatureName() { return "UI Framework"; }
    
    @Override
    public FeaturePriority getPriority() { return FeaturePriority.HIGH; }
    
    @Override
    public void initialize() throws FeatureInitException {
        logger.info("Initializing UI Framework...");
        
        registerBuiltinWidgets();
        themeManager.loadThemes();
    }
    
    @Override
    public void start() {
        running = true;
        logger.info("UI Framework started");
    }
    
    @Override
    public void stop() {
        running = false;
        playerStates.clear();
        logger.info("UI Framework stopped");
    }
    
    @Override
    public void shutdown() {
        stop();
    }
    
    @Override
    public FeatureHealth healthCheck() {
        if (!running) {
            return FeatureHealth.disabled("UI framework not running");
        }
        return FeatureHealth.healthy()
            .withMetric("activeScreens", playerStates.size())
            .withMetric("registeredWidgets", widgetRegistry.getWidgetCount());
    }
    
    private void registerBuiltinWidgets() {
        widgetRegistry.register("text", TextWidget.class);
        widgetRegistry.register("button", ButtonWidget.class);
        widgetRegistry.register("panel", PanelWidget.class);
        widgetRegistry.register("image", ImageWidget.class);
        widgetRegistry.register("progress", ProgressWidget.class);
        widgetRegistry.register("list", ListWidget.class);
        widgetRegistry.register("slider", SliderWidget.class);
        widgetRegistry.register("toggle", ToggleWidget.class);
        widgetRegistry.register("input", InputWidget.class);
    }
    
    public void registerScreen(String id, UIScreen screen) {
        registeredScreens.put(id, screen);
    }
    
    public void showScreen(Player player, String screenId) {
        UIScreen screen = registeredScreens.get(screenId);
        if (screen == null) {
            logger.warning("Screen not found: " + screenId);
            return;
        }
        
        PlayerUIState state = getOrCreateState(player.getUuid());
        state.pushScreen(screen);
        
        UIRenderPacket packet = layoutEngine.render(screen, state, themeManager.getCurrentTheme());
        player.sendPacket(packet);
    }
    
    public void closeScreen(Player player) {
        PlayerUIState state = playerStates.get(player.getUuid());
        if (state != null) {
            state.popScreen();
            
            UIScreen current = state.getCurrentScreen();
            if (current != null) {
                UIRenderPacket packet = layoutEngine.render(current, state, themeManager.getCurrentTheme());
                player.sendPacket(packet);
            } else {
                player.sendPacket(new UIClosePacket());
            }
        }
    }
    
    public void updateWidget(Player player, String widgetId, Map<String, Object> data) {
        PlayerUIState state = playerStates.get(player.getUuid());
        if (state != null) {
            state.updateWidgetData(widgetId, data);
            player.sendPacket(new UIUpdatePacket(widgetId, data));
        }
    }
    
    public void handleInput(Player player, String widgetId, UIInputEvent event) {
        PlayerUIState state = playerStates.get(player.getUuid());
        if (state == null) return;
        
        UIScreen screen = state.getCurrentScreen();
        if (screen == null) return;
        
        screen.handleInput(player, widgetId, event);
    }
    
    private PlayerUIState getOrCreateState(UUID playerId) {
        return playerStates.computeIfAbsent(playerId, PlayerUIState::new);
    }
    
    public void removePlayerState(UUID playerId) {
        playerStates.remove(playerId);
    }
    
    public UIScreenBuilder createScreen(String id) {
        return new UIScreenBuilder(id);
    }
    
    public WidgetRegistry getWidgetRegistry() { return widgetRegistry; }
    public ThemeManager getThemeManager() { return themeManager; }
    public StateStore getStateStore() { return stateStore; }
    
    public record UIRenderPacket(String screenId, List<WidgetData> widgets, Theme theme) {}
    public record UIUpdatePacket(String widgetId, Map<String, Object> data) {}
    public record UIClosePacket() {}
    
    public record UIInputEvent(Type type, String value, Map<String, Object> extra) {
        public enum Type {
            CLICK, HOVER, SCROLL, KEY_PRESS, VALUE_CHANGE, SUBMIT
        }
    }
    
    public record WidgetData(
        String id,
        String type,
        int x, int y, int width, int height,
        Map<String, Object> properties,
        List<WidgetData> children
    ) {}
}
