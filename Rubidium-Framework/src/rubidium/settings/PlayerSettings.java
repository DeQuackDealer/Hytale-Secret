package rubidium.settings;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerSettings {
    
    private final UUID playerId;
    private final Map<String, Object> values = new ConcurrentHashMap<>();
    
    private boolean minimapEnabled = true;
    private boolean statisticsEnabled = false;
    private boolean voiceChatEnabled = true;
    private boolean waypointsEnabled = true;
    
    private float minimapZoom = 1.0f;
    private int minimapSize = 150;
    private boolean minimapRotate = true;
    
    private float voiceChatVolume = 1.0f;
    private String pushToTalkKey = "V";
    private boolean voiceChatPTTMode = true;
    
    private final Map<String, HUDPosition> hudPositions = new ConcurrentHashMap<>();
    
    public PlayerSettings(UUID playerId) {
        this.playerId = playerId;
        initDefaults();
    }
    
    private void initDefaults() {
        hudPositions.put("minimap", new HUDPosition(10, 10, 150, 150, AnchorPoint.TOP_RIGHT));
        hudPositions.put("statistics", new HUDPosition(10, 10, 200, 80, AnchorPoint.TOP_LEFT));
        hudPositions.put("voicechat", new HUDPosition(10, 200, 180, 120, AnchorPoint.TOP_LEFT));
        hudPositions.put("waypoints", new HUDPosition(0, 0, 0, 0, AnchorPoint.CENTER));
    }
    
    public UUID getPlayerId() { return playerId; }
    
    public boolean isMinimapEnabled() { return minimapEnabled; }
    public void setMinimapEnabled(boolean enabled) { this.minimapEnabled = enabled; }
    
    public boolean isStatisticsEnabled() { return statisticsEnabled; }
    public void setStatisticsEnabled(boolean enabled) { this.statisticsEnabled = enabled; }
    
    public boolean isVoiceChatEnabled() { return voiceChatEnabled; }
    public void setVoiceChatEnabled(boolean enabled) { this.voiceChatEnabled = enabled; }
    
    public boolean isWaypointsEnabled() { return waypointsEnabled; }
    public void setWaypointsEnabled(boolean enabled) { this.waypointsEnabled = enabled; }
    
    public float getMinimapZoom() { return minimapZoom; }
    public void setMinimapZoom(float zoom) { this.minimapZoom = Math.max(0.5f, Math.min(3.0f, zoom)); }
    
    public int getMinimapSize() { return minimapSize; }
    public void setMinimapSize(int size) { this.minimapSize = Math.max(100, Math.min(300, size)); }
    
    public boolean isMinimapRotate() { return minimapRotate; }
    public void setMinimapRotate(boolean rotate) { this.minimapRotate = rotate; }
    
    public float getVoiceChatVolume() { return voiceChatVolume; }
    public void setVoiceChatVolume(float volume) { this.voiceChatVolume = Math.max(0f, Math.min(2.0f, volume)); }
    
    public String getPushToTalkKey() { return pushToTalkKey; }
    public void setPushToTalkKey(String key) { this.pushToTalkKey = key; }
    
    public boolean isVoiceChatPTTMode() { return voiceChatPTTMode; }
    public void setVoiceChatPTTMode(boolean ptt) { this.voiceChatPTTMode = ptt; }
    
    public HUDPosition getHUDPosition(String widgetId) {
        return hudPositions.get(widgetId);
    }
    
    public void setHUDPosition(String widgetId, HUDPosition position) {
        hudPositions.put(widgetId, position);
    }
    
    public Map<String, HUDPosition> getAllHUDPositions() {
        return Collections.unmodifiableMap(hudPositions);
    }
    
    public void set(String key, Object value) {
        values.put(key, value);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        Object value = values.get(key);
        if (value == null) return defaultValue;
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }
    
    public void save() {
        System.out.println("[Rubidium] Saving settings for player " + playerId);
    }
    
    public void load() {
        System.out.println("[Rubidium] Loading settings for player " + playerId);
    }
    
    public static class HUDPosition {
        private int x, y, width, height;
        private AnchorPoint anchor;
        private boolean visible = true;
        
        public HUDPosition(int x, int y, int width, int height, AnchorPoint anchor) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.anchor = anchor;
        }
        
        public int getX() { return x; }
        public void setX(int x) { this.x = x; }
        
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
        
        public int getWidth() { return width; }
        public void setWidth(int width) { this.width = width; }
        
        public int getHeight() { return height; }
        public void setHeight(int height) { this.height = height; }
        
        public AnchorPoint getAnchor() { return anchor; }
        public void setAnchor(AnchorPoint anchor) { this.anchor = anchor; }
        
        public boolean isVisible() { return visible; }
        public void setVisible(boolean visible) { this.visible = visible; }
    }
    
    public enum AnchorPoint {
        TOP_LEFT, TOP_CENTER, TOP_RIGHT,
        CENTER_LEFT, CENTER, CENTER_RIGHT,
        BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
    }
}
