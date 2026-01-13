package rubidium.display;

import rubidium.api.player.Player;

import java.util.*;
import java.util.function.Function;

public class Hologram {
    
    private final String id;
    private double x, y, z;
    private final List<HologramLine> lines;
    private final Set<UUID> viewers;
    private double viewDistance;
    private boolean visible;
    private long updateInterval;
    private long lastUpdate;
    
    public Hologram(String id, double x, double y, double z) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.lines = new ArrayList<>();
        this.viewers = new HashSet<>();
        this.viewDistance = 48.0;
        this.visible = true;
        this.updateInterval = 1000;
        this.lastUpdate = 0;
    }
    
    public String getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    
    public Hologram setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }
    
    public Hologram addLine(String text) {
        lines.add(new HologramLine(text, null));
        return this;
    }
    
    public Hologram addLine(Function<Player, String> dynamicText) {
        lines.add(new HologramLine(null, dynamicText));
        return this;
    }
    
    public Hologram setLine(int index, String text) {
        if (index >= 0 && index < lines.size()) {
            lines.set(index, new HologramLine(text, null));
        }
        return this;
    }
    
    public Hologram removeLine(int index) {
        if (index >= 0 && index < lines.size()) {
            lines.remove(index);
        }
        return this;
    }
    
    public Hologram clearLines() {
        lines.clear();
        return this;
    }
    
    public int getLineCount() { return lines.size(); }
    
    public Hologram setViewDistance(double distance) {
        this.viewDistance = distance;
        return this;
    }
    
    public double getViewDistance() { return viewDistance; }
    
    public Hologram setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }
    
    public boolean isVisible() { return visible; }
    
    public Hologram setUpdateInterval(long millis) {
        this.updateInterval = millis;
        return this;
    }
    
    public List<String> renderFor(Player player) {
        List<String> rendered = new ArrayList<>();
        for (HologramLine line : lines) {
            if (line.dynamicText() != null) {
                rendered.add(line.dynamicText().apply(player));
            } else {
                rendered.add(line.staticText());
            }
        }
        return rendered;
    }
    
    public boolean shouldUpdate() {
        long now = System.currentTimeMillis();
        if (now - lastUpdate >= updateInterval) {
            lastUpdate = now;
            return true;
        }
        return false;
    }
    
    public boolean isInRange(double px, double py, double pz) {
        double dx = px - x;
        double dy = py - y;
        double dz = pz - z;
        return (dx * dx + dy * dy + dz * dz) <= (viewDistance * viewDistance);
    }
    
    public void addViewer(UUID playerId) { viewers.add(playerId); }
    public void removeViewer(UUID playerId) { viewers.remove(playerId); }
    public Set<UUID> getViewers() { return Collections.unmodifiableSet(viewers); }
    
    public record HologramLine(String staticText, Function<Player, String> dynamicText) {}
}
