package rubidium.hytale.api;

import rubidium.hytale.api.player.Player;
import rubidium.hytale.api.world.World;

import java.util.*;

/**
 * Main server interface for accessing server functionality.
 */
public class HytaleServer {
    
    private static HytaleServer instance;
    
    private final Map<UUID, Player> onlinePlayers = new HashMap<>();
    private final Map<String, World> worlds = new HashMap<>();
    private double tps = 20.0;
    
    public static HytaleServer getInstance() {
        if (instance == null) {
            instance = new HytaleServer();
        }
        return instance;
    }
    
    public Collection<Player> getOnlinePlayers() {
        return onlinePlayers.values();
    }
    
    public int getOnlinePlayerCount() {
        return onlinePlayers.size();
    }
    
    public Optional<Player> getPlayer(UUID uuid) {
        return Optional.ofNullable(onlinePlayers.get(uuid));
    }
    
    public Optional<Player> getPlayer(String name) {
        return onlinePlayers.values().stream()
            .filter(p -> p.getUsername().equalsIgnoreCase(name))
            .findFirst();
    }
    
    public Collection<World> getWorlds() {
        return worlds.values();
    }
    
    public Optional<World> getWorld(String name) {
        return Optional.ofNullable(worlds.get(name));
    }
    
    public double getTPS() {
        return tps;
    }
    
    public void broadcastMessage(String message) {
        for (Player player : onlinePlayers.values()) {
            player.sendMessage(message);
        }
    }
}
