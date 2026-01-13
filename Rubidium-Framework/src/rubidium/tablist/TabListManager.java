package rubidium.tablist;

import rubidium.api.player.Player;
import rubidium.core.logging.RubidiumLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TabListManager {
    
    private final RubidiumLogger logger;
    private TabList globalTabList;
    private final Map<UUID, TabList> playerTabLists;
    private final Map<UUID, String> playerPrefixes;
    private final Map<UUID, String> playerSuffixes;
    private long updateInterval;
    private long lastUpdate;
    
    public TabListManager(RubidiumLogger logger) {
        this.logger = logger;
        this.globalTabList = new TabList();
        this.playerTabLists = new ConcurrentHashMap<>();
        this.playerPrefixes = new ConcurrentHashMap<>();
        this.playerSuffixes = new ConcurrentHashMap<>();
        this.updateInterval = 1000;
        this.lastUpdate = 0;
    }
    
    public TabList getGlobalTabList() {
        return globalTabList;
    }
    
    public void setGlobalTabList(TabList tabList) {
        this.globalTabList = tabList;
    }
    
    public void setPlayerTabList(Player player, TabList tabList) {
        playerTabLists.put(player.getUUID(), tabList);
    }
    
    public void clearPlayerTabList(Player player) {
        playerTabLists.remove(player.getUUID());
    }
    
    public TabList getTabListFor(Player player) {
        return playerTabLists.getOrDefault(player.getUUID(), globalTabList);
    }
    
    public void setPlayerPrefix(Player player, String prefix) {
        playerPrefixes.put(player.getUUID(), prefix);
    }
    
    public void setPlayerSuffix(Player player, String suffix) {
        playerSuffixes.put(player.getUUID(), suffix);
    }
    
    public String getPlayerPrefix(Player player) {
        return playerPrefixes.getOrDefault(player.getUUID(), "");
    }
    
    public String getPlayerSuffix(Player player) {
        return playerSuffixes.getOrDefault(player.getUUID(), "");
    }
    
    public void clearPlayerPrefix(Player player) {
        playerPrefixes.remove(player.getUUID());
    }
    
    public void clearPlayerSuffix(Player player) {
        playerSuffixes.remove(player.getUUID());
    }
    
    public void tick(Collection<Player> onlinePlayers) {
        long now = System.currentTimeMillis();
        if (now - lastUpdate < updateInterval) return;
        lastUpdate = now;
        
        for (Player player : onlinePlayers) {
            TabList tabList = getTabListFor(player);
            sendTabListUpdate(player, tabList, onlinePlayers);
        }
    }
    
    private void sendTabListUpdate(Player player, TabList tabList, Collection<Player> onlinePlayers) {
        String header = tabList.renderHeader(player);
        String footer = tabList.renderFooter(player);
        
        List<TabList.TabListEntry> entries = new ArrayList<>();
        
        for (Player online : onlinePlayers) {
            String prefix = playerPrefixes.getOrDefault(online.getUUID(), "");
            String suffix = playerSuffixes.getOrDefault(online.getUUID(), "");
            String displayName = prefix + online.getName() + suffix;
            
            entries.add(new TabList.TabListEntry(
                online.getName(),
                displayName,
                online.getPing(),
                prefix,
                suffix,
                0
            ));
        }
        
        entries.addAll(tabList.getCustomEntries());
        
        switch (tabList.getSortMode()) {
            case ALPHABETICAL -> entries.sort(Comparator.comparing(TabList.TabListEntry::name));
            case CUSTOM -> entries.sort(Comparator.comparingInt(TabList.TabListEntry::sortOrder));
            default -> {}
        }
        
        player.sendPacket(new TabListPacket(header, footer, entries));
    }
    
    public void setUpdateInterval(long millis) {
        this.updateInterval = Math.max(100, millis);
    }
    
    public void onPlayerQuit(Player player) {
        playerTabLists.remove(player.getUUID());
        playerPrefixes.remove(player.getUUID());
        playerSuffixes.remove(player.getUUID());
    }
    
    public record TabListPacket(String header, String footer, List<TabList.TabListEntry> entries) {}
}
