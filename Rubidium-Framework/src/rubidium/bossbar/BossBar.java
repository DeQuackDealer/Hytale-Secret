package rubidium.bossbar;

import rubidium.api.player.Player;

import java.util.*;

public class BossBar {
    
    private final String id;
    private String title;
    private float progress;
    private Color color;
    private Style style;
    private final Set<UUID> viewers;
    private boolean visible;
    
    public BossBar(String id, String title) {
        this(id, title, 1.0f, Color.PURPLE, Style.SOLID);
    }
    
    public BossBar(String id, String title, float progress, Color color, Style style) {
        this.id = id;
        this.title = title;
        this.progress = Math.max(0, Math.min(1, progress));
        this.color = color;
        this.style = style;
        this.viewers = new HashSet<>();
        this.visible = true;
    }
    
    public String getId() { return id; }
    public String getTitle() { return title; }
    public float getProgress() { return progress; }
    public Color getColor() { return color; }
    public Style getStyle() { return style; }
    public boolean isVisible() { return visible; }
    
    public BossBar setTitle(String title) {
        this.title = title;
        broadcastUpdate(UpdateType.TITLE);
        return this;
    }
    
    public BossBar setProgress(float progress) {
        this.progress = Math.max(0, Math.min(1, progress));
        broadcastUpdate(UpdateType.PROGRESS);
        return this;
    }
    
    public BossBar setColor(Color color) {
        this.color = color;
        broadcastUpdate(UpdateType.COLOR);
        return this;
    }
    
    public BossBar setStyle(Style style) {
        this.style = style;
        broadcastUpdate(UpdateType.STYLE);
        return this;
    }
    
    public BossBar setVisible(boolean visible) {
        this.visible = visible;
        broadcastUpdate(visible ? UpdateType.SHOW : UpdateType.HIDE);
        return this;
    }
    
    public void addViewer(Player player) {
        if (viewers.add(player.getUUID())) {
            player.sendPacket(new BossBarPacket(id, Action.ADD, title, progress, color, style));
        }
    }
    
    public void removeViewer(Player player) {
        if (viewers.remove(player.getUUID())) {
            player.sendPacket(new BossBarPacket(id, Action.REMOVE, null, 0, null, null));
        }
    }
    
    public Set<UUID> getViewers() {
        return Collections.unmodifiableSet(viewers);
    }
    
    private void broadcastUpdate(UpdateType type) {
    }
    
    public enum Color { PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE }
    public enum Style { SOLID, SEGMENTED_6, SEGMENTED_10, SEGMENTED_12, SEGMENTED_20 }
    public enum Action { ADD, REMOVE, UPDATE_PROGRESS, UPDATE_TITLE, UPDATE_STYLE, UPDATE_COLOR }
    private enum UpdateType { TITLE, PROGRESS, COLOR, STYLE, SHOW, HIDE }
    
    public record BossBarPacket(String id, Action action, String title, float progress, Color color, Style style) {}
}
