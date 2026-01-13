package rubidium.tablist;

import rubidium.api.player.Player;

import java.util.*;
import java.util.function.Function;

public class TabList {
    
    private String header;
    private String footer;
    private Function<Player, String> dynamicHeader;
    private Function<Player, String> dynamicFooter;
    private final List<TabListEntry> customEntries;
    private SortMode sortMode;
    private boolean showPing;
    private boolean showGamemode;
    
    public TabList() {
        this.customEntries = new ArrayList<>();
        this.sortMode = SortMode.ALPHABETICAL;
        this.showPing = true;
        this.showGamemode = false;
    }
    
    public TabList setHeader(String header) {
        this.header = header;
        this.dynamicHeader = null;
        return this;
    }
    
    public TabList setHeader(Function<Player, String> dynamicHeader) {
        this.dynamicHeader = dynamicHeader;
        this.header = null;
        return this;
    }
    
    public TabList setFooter(String footer) {
        this.footer = footer;
        this.dynamicFooter = null;
        return this;
    }
    
    public TabList setFooter(Function<Player, String> dynamicFooter) {
        this.dynamicFooter = dynamicFooter;
        this.footer = null;
        return this;
    }
    
    public TabList setSortMode(SortMode mode) {
        this.sortMode = mode;
        return this;
    }
    
    public TabList setShowPing(boolean show) {
        this.showPing = show;
        return this;
    }
    
    public TabList setShowGamemode(boolean show) {
        this.showGamemode = show;
        return this;
    }
    
    public TabList addCustomEntry(TabListEntry entry) {
        customEntries.add(entry);
        return this;
    }
    
    public TabList clearCustomEntries() {
        customEntries.clear();
        return this;
    }
    
    public String renderHeader(Player player) {
        if (dynamicHeader != null) return dynamicHeader.apply(player);
        return header;
    }
    
    public String renderFooter(Player player) {
        if (dynamicFooter != null) return dynamicFooter.apply(player);
        return footer;
    }
    
    public List<TabListEntry> getCustomEntries() {
        return Collections.unmodifiableList(customEntries);
    }
    
    public SortMode getSortMode() { return sortMode; }
    public boolean shouldShowPing() { return showPing; }
    public boolean shouldShowGamemode() { return showGamemode; }
    
    public enum SortMode {
        ALPHABETICAL,
        TEAM,
        CUSTOM,
        NONE
    }
    
    public record TabListEntry(
        String name,
        String displayName,
        int ping,
        String prefix,
        String suffix,
        int sortOrder
    ) {
        public static TabListEntry of(String name) {
            return new TabListEntry(name, name, 0, "", "", 0);
        }
        
        public static TabListEntry of(String name, String displayName) {
            return new TabListEntry(name, displayName, 0, "", "", 0);
        }
        
        public TabListEntry withPrefix(String prefix) {
            return new TabListEntry(name, displayName, ping, prefix, suffix, sortOrder);
        }
        
        public TabListEntry withSuffix(String suffix) {
            return new TabListEntry(name, displayName, ping, prefix, suffix, sortOrder);
        }
        
        public TabListEntry withPing(int ping) {
            return new TabListEntry(name, displayName, ping, prefix, suffix, sortOrder);
        }
        
        public TabListEntry withSortOrder(int order) {
            return new TabListEntry(name, displayName, ping, prefix, suffix, order);
        }
    }
}
