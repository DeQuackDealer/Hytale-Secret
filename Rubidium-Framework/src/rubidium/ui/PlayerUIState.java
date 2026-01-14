package rubidium.ui;

import java.util.*;

/**
 * Per-player UI state.
 */
public class PlayerUIState {
    
    private final UUID playerId;
    private final Deque<UIScreen> screenStack = new ArrayDeque<>();
    private final Map<String, Map<String, Object>> widgetData = new HashMap<>();
    
    public PlayerUIState(UUID playerId) {
        this.playerId = playerId;
    }
    
    public UUID getPlayerId() { return playerId; }
    
    public void pushScreen(UIScreen screen) {
        screenStack.push(screen);
    }
    
    public UIScreen popScreen() {
        return screenStack.isEmpty() ? null : screenStack.pop();
    }
    
    public UIScreen getCurrentScreen() {
        return screenStack.peek();
    }
    
    public boolean hasScreens() {
        return !screenStack.isEmpty();
    }
    
    public int getScreenStackSize() {
        return screenStack.size();
    }
    
    public void clearScreens() {
        screenStack.clear();
    }
    
    public void updateWidgetData(String widgetId, Map<String, Object> data) {
        widgetData.put(widgetId, new HashMap<>(data));
    }
    
    public Map<String, Object> getWidgetData(String widgetId) {
        return widgetData.getOrDefault(widgetId, Map.of());
    }
    
    public void clearWidgetData() {
        widgetData.clear();
    }
}
