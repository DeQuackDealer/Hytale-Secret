package rubidium.api.server;

import rubidium.api.player.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class Server {
    
    private static final Map<UUID, Player> onlinePlayers = new ConcurrentHashMap<>();
    private static final Set<UUID> bannedPlayers = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, BanEntry> banEntries = new ConcurrentHashMap<>();
    
    private Server() {}
    
    public static Collection<Player> getOnlinePlayers() {
        return Collections.unmodifiableCollection(onlinePlayers.values());
    }
    
    public static Optional<Player> getPlayer(UUID uuid) {
        return Optional.ofNullable(onlinePlayers.get(uuid));
    }
    
    public static Optional<Player> getPlayer(String name) {
        return onlinePlayers.values().stream()
            .filter(p -> p.getName().equalsIgnoreCase(name))
            .findFirst();
    }
    
    public static Player getPlayerByName(String name) {
        return getPlayer(name).orElse(null);
    }
    
    public static int getOnlineCount() {
        return onlinePlayers.size();
    }
    
    public static void banPlayer(UUID uuid, String reason, String duration) {
        bannedPlayers.add(uuid);
        banEntries.put(uuid, new BanEntry(uuid, reason, duration, System.currentTimeMillis()));
    }
    
    public static void unbanPlayer(UUID uuid) {
        bannedPlayers.remove(uuid);
        banEntries.remove(uuid);
    }
    
    public static boolean isBanned(UUID uuid) {
        return bannedPlayers.contains(uuid);
    }
    
    public static Optional<BanEntry> getBanEntry(UUID uuid) {
        return Optional.ofNullable(banEntries.get(uuid));
    }
    
    public static Collection<BanEntry> getAllBans() {
        return Collections.unmodifiableCollection(banEntries.values());
    }
    
    public static void broadcast(String message) {
        for (Player player : onlinePlayers.values()) {
            player.sendMessage(message);
        }
    }
    
    public static void registerPlayer(Player player) {
        onlinePlayers.put(player.getUniqueId(), player);
    }
    
    public static void unregisterPlayer(UUID uuid) {
        onlinePlayers.remove(uuid);
    }
    
    public record BanEntry(UUID uuid, String reason, String duration, long timestamp) {}
}
