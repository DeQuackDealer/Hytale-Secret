package rubidium.display;

import rubidium.hytale.api.player.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Display management for scoreboard, bossbar, and action bar.
 */
public class DisplayManager {
    
    private static DisplayManager instance;
    
    private final Map<UUID, PlayerDisplay> playerDisplays;
    
    private DisplayManager() {
        this.playerDisplays = new ConcurrentHashMap<>();
    }
    
    public static DisplayManager getInstance() {
        if (instance == null) {
            instance = new DisplayManager();
        }
        return instance;
    }
    
    public PlayerDisplay getPlayerDisplay(Player player) {
        return playerDisplays.computeIfAbsent(player.getUuid(), 
            k -> new PlayerDisplay(player.getUuid()));
    }
    
    public void sendActionBar(Player player, String message) {
        player.sendActionBar(message);
    }
    
    public void sendTitle(Player player, String title, String subtitle) {
        player.sendTitle(title, subtitle, 10, 70, 20);
    }
    
    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }
    
    public void removePlayer(UUID playerId) {
        playerDisplays.remove(playerId);
    }
}
