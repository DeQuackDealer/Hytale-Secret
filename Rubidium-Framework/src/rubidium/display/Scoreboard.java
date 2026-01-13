package rubidium.display;

import rubidium.api.player.Player;

import java.util.*;
import java.util.function.Function;

public class Scoreboard {
    
    private final String id;
    private String title;
    private final List<ScoreboardLine> lines;
    private final Set<UUID> viewers;
    private boolean dirty;
    
    public Scoreboard(String id, String title) {
        this.id = id;
        this.title = title;
        this.lines = new ArrayList<>(15);
        this.viewers = new HashSet<>();
        this.dirty = true;
    }
    
    public String getId() { return id; }
    public String getTitle() { return title; }
    
    public Scoreboard setTitle(String title) {
        this.title = title;
        this.dirty = true;
        return this;
    }
    
    public Scoreboard setLine(int index, String text) {
        ensureCapacity(index);
        lines.set(index, new ScoreboardLine(text, null));
        dirty = true;
        return this;
    }
    
    public Scoreboard setLine(int index, Function<Player, String> dynamicText) {
        ensureCapacity(index);
        lines.set(index, new ScoreboardLine(null, dynamicText));
        dirty = true;
        return this;
    }
    
    public Scoreboard removeLine(int index) {
        if (index < lines.size()) {
            lines.set(index, null);
            dirty = true;
        }
        return this;
    }
    
    public Scoreboard clearLines() {
        lines.clear();
        dirty = true;
        return this;
    }
    
    private void ensureCapacity(int index) {
        while (lines.size() <= index) {
            lines.add(null);
        }
    }
    
    public void addViewer(Player player) {
        viewers.add(player.getUUID());
    }
    
    public void removeViewer(Player player) {
        viewers.remove(player.getUUID());
    }
    
    public Set<UUID> getViewers() {
        return Collections.unmodifiableSet(viewers);
    }
    
    public List<String> renderFor(Player player) {
        List<String> rendered = new ArrayList<>();
        for (ScoreboardLine line : lines) {
            if (line == null) {
                rendered.add("");
            } else if (line.dynamicText() != null) {
                rendered.add(line.dynamicText().apply(player));
            } else {
                rendered.add(line.staticText());
            }
        }
        return rendered;
    }
    
    public boolean isDirty() { return dirty; }
    public void markClean() { dirty = false; }
    
    public record ScoreboardLine(String staticText, Function<Player, String> dynamicText) {}
}
