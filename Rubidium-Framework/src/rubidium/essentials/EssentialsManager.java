package rubidium.essentials;

import rubidium.hytale.api.player.Player;
import rubidium.hytale.api.world.Location;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Essentials-style utilities manager.
 * Handles homes, warps, spawn, kits, nicknames, vanish, god mode, etc.
 */
public class EssentialsManager {
    
    private static EssentialsManager instance;
    
    private final Map<UUID, PlayerEssentialsData> playerData;
    private final Map<String, Warp> warps;
    private final Map<String, Kit> kits;
    private final Map<String, Jail> jails;
    private Location spawnLocation;
    
    private EssentialsManager() {
        this.playerData = new ConcurrentHashMap<>();
        this.warps = new ConcurrentHashMap<>();
        this.kits = new ConcurrentHashMap<>();
        this.jails = new ConcurrentHashMap<>();
    }
    
    public static EssentialsManager getInstance() {
        if (instance == null) {
            instance = new EssentialsManager();
        }
        return instance;
    }
    
    public PlayerEssentialsData getPlayerData(Player player) {
        return playerData.computeIfAbsent(player.getUuid(), k -> new PlayerEssentialsData(player.getUuid()));
    }
    
    public void setHome(Player player, String name, Location location) {
        getPlayerData(player).setHome(name, location);
    }
    
    public Optional<Location> getHome(Player player, String name) {
        return Optional.ofNullable(getPlayerData(player).getHome(name));
    }
    
    public void deleteHome(Player player, String name) {
        getPlayerData(player).deleteHome(name);
    }
    
    public Map<String, Location> getHomes(Player player) {
        return getPlayerData(player).getHomes();
    }
    
    public void setWarp(String name, Location location) {
        setWarp(name, location, null);
    }
    
    public void setWarp(String name, Location location, String permission) {
        warps.put(name.toLowerCase(), new Warp(name, location, permission));
    }
    
    public Optional<Warp> getWarp(String name) {
        return Optional.ofNullable(warps.get(name.toLowerCase()));
    }
    
    public void deleteWarp(String name) {
        warps.remove(name.toLowerCase());
    }
    
    public Collection<Warp> getWarps() {
        return warps.values();
    }
    
    public void setSpawn(Location location) {
        this.spawnLocation = location;
    }
    
    public Optional<Location> getSpawn() {
        return Optional.ofNullable(spawnLocation);
    }
    
    public void registerKit(Kit kit) {
        kits.put(kit.getName().toLowerCase(), kit);
    }
    
    public Optional<Kit> getKit(String name) {
        return Optional.ofNullable(kits.get(name.toLowerCase()));
    }
    
    public Collection<Kit> getKits() {
        return kits.values();
    }
    
    public boolean canUseKit(Player player, String kitName) {
        Kit kit = kits.get(kitName.toLowerCase());
        if (kit == null) return false;
        
        PlayerEssentialsData data = getPlayerData(player);
        long lastUsed = data.getKitLastUsed(kitName);
        long cooldown = kit.getCooldownMs();
        
        return System.currentTimeMillis() - lastUsed >= cooldown;
    }
    
    public void useKit(Player player, String kitName) {
        Kit kit = kits.get(kitName.toLowerCase());
        if (kit != null) {
            getPlayerData(player).setKitLastUsed(kitName, System.currentTimeMillis());
        }
    }
    
    public void setNickname(Player player, String nickname) {
        getPlayerData(player).setNickname(nickname);
        player.setDisplayName(nickname);
    }
    
    public void clearNickname(Player player) {
        getPlayerData(player).setNickname(null);
        player.setDisplayName(player.getUsername());
    }
    
    public Optional<String> getNickname(Player player) {
        return Optional.ofNullable(getPlayerData(player).getNickname());
    }
    
    public void setVanished(Player player, boolean vanished) {
        getPlayerData(player).setVanished(vanished);
    }
    
    public boolean isVanished(Player player) {
        return getPlayerData(player).isVanished();
    }
    
    public void setGodMode(Player player, boolean godMode) {
        getPlayerData(player).setGodMode(godMode);
    }
    
    public boolean isGodMode(Player player) {
        return getPlayerData(player).isGodMode();
    }
    
    public void heal(Player player) {
        player.setHealth(player.getMaxHealth());
    }
    
    public void feed(Player player) {
        player.setFoodLevel(20);
    }
    
    public void setFlying(Player player, boolean flying) {
        player.setAllowFlight(flying);
        player.setFlying(flying);
    }
    
    public void setAfk(Player player, boolean afk) {
        getPlayerData(player).setAfk(afk);
    }
    
    public boolean isAfk(Player player) {
        return getPlayerData(player).isAfk();
    }
    
    public void registerJail(String name, Location location) {
        jails.put(name.toLowerCase(), new Jail(name, location));
    }
    
    public Optional<Jail> getJail(String name) {
        return Optional.ofNullable(jails.get(name.toLowerCase()));
    }
    
    public Collection<Jail> getJails() {
        return jails.values();
    }
    
    public void jailPlayer(Player player, String jailName, long durationMs, String reason) {
        Jail jail = jails.get(jailName.toLowerCase());
        if (jail != null) {
            PlayerEssentialsData data = getPlayerData(player);
            data.setJailed(true);
            data.setJailName(jailName);
            data.setJailExpiry(durationMs > 0 ? System.currentTimeMillis() + durationMs : -1);
            data.setJailReason(reason);
            player.teleport(jail.location());
        }
    }
    
    public void unjailPlayer(Player player) {
        PlayerEssentialsData data = getPlayerData(player);
        data.setJailed(false);
        data.setJailName(null);
        data.setJailExpiry(0);
        data.setJailReason(null);
        
        if (spawnLocation != null) {
            player.teleport(spawnLocation);
        }
    }
    
    public boolean isJailed(Player player) {
        PlayerEssentialsData data = getPlayerData(player);
        if (!data.isJailed()) return false;
        
        long expiry = data.getJailExpiry();
        if (expiry > 0 && System.currentTimeMillis() > expiry) {
            unjailPlayer(player);
            return false;
        }
        
        return true;
    }
    
    public void mutePlayer(Player player, long durationMs, String reason) {
        PlayerEssentialsData data = getPlayerData(player);
        data.setMuted(true);
        data.setMuteExpiry(durationMs > 0 ? System.currentTimeMillis() + durationMs : -1);
        data.setMuteReason(reason);
    }
    
    public void unmutePlayer(Player player) {
        PlayerEssentialsData data = getPlayerData(player);
        data.setMuted(false);
        data.setMuteExpiry(0);
        data.setMuteReason(null);
    }
    
    public boolean isMuted(Player player) {
        PlayerEssentialsData data = getPlayerData(player);
        if (!data.isMuted()) return false;
        
        long expiry = data.getMuteExpiry();
        if (expiry > 0 && System.currentTimeMillis() > expiry) {
            unmutePlayer(player);
            return false;
        }
        
        return true;
    }
    
    public void setBackLocation(Player player, Location location) {
        getPlayerData(player).setLastLocation(location);
    }
    
    public Optional<Location> getBackLocation(Player player) {
        return Optional.ofNullable(getPlayerData(player).getLastLocation());
    }
}
