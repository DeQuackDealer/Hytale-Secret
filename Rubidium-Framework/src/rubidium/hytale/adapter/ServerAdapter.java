package rubidium.hytale.adapter;

import rubidium.hytale.api.player.Player;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.player.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapts the real HytaleServer to Rubidium's internal APIs.
 * Manages player adapters and provides unified access to server functionality.
 */
public class ServerAdapter {
    
    private static ServerAdapter instance;
    
    private HytaleServer hytaleServer;
    private final Map<UUID, PlayerAdapter> playerAdapters = new ConcurrentHashMap<>();
    
    private ServerAdapter() {}
    
    public static ServerAdapter getInstance() {
        if (instance == null) {
            instance = new ServerAdapter();
        }
        return instance;
    }
    
    public void initialize(HytaleServer server) {
        this.hytaleServer = server;
    }
    
    public HytaleServer getHytaleServer() {
        return hytaleServer;
    }
    
    public Player wrapPlayer(ServerPlayer hytalePlayer) {
        return playerAdapters.computeIfAbsent(
            hytalePlayer.getUuid(),
            uuid -> new PlayerAdapter(hytalePlayer)
        );
    }
    
    public void removePlayer(UUID playerId) {
        playerAdapters.remove(playerId);
    }
    
    public Optional<Player> getPlayer(UUID playerId) {
        return Optional.ofNullable(playerAdapters.get(playerId));
    }
    
    public Collection<Player> getOnlinePlayers() {
        return Collections.unmodifiableCollection(playerAdapters.values());
    }
    
    public int getOnlinePlayerCount() {
        return hytaleServer != null ? hytaleServer.getOnlinePlayerCount() : 0;
    }
    
    public void broadcast(String message) {
        if (hytaleServer != null) {
            hytaleServer.broadcast(message);
        }
    }
    
    public String getServerName() {
        return hytaleServer != null ? hytaleServer.getServerName() : "Unknown";
    }
    
    public String getServerVersion() {
        return hytaleServer != null ? hytaleServer.getVersion() : "Unknown";
    }
    
    public boolean isServerRunning() {
        return hytaleServer != null && hytaleServer.isRunning();
    }
}
