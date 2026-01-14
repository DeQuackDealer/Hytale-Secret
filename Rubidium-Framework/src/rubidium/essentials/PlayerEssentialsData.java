package rubidium.essentials;

import rubidium.hytale.api.world.Location;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player essentials data.
 */
public class PlayerEssentialsData {
    
    private final UUID playerId;
    private final Map<String, Location> homes;
    private final Map<String, Long> kitCooldowns;
    
    private String nickname;
    private boolean vanished;
    private boolean godMode;
    private boolean afk;
    private Location lastLocation;
    
    private boolean jailed;
    private String jailName;
    private long jailExpiry;
    private String jailReason;
    
    private boolean muted;
    private long muteExpiry;
    private String muteReason;
    
    public PlayerEssentialsData(UUID playerId) {
        this.playerId = playerId;
        this.homes = new ConcurrentHashMap<>();
        this.kitCooldowns = new ConcurrentHashMap<>();
    }
    
    public UUID getPlayerId() { return playerId; }
    
    public void setHome(String name, Location location) {
        homes.put(name.toLowerCase(), location);
    }
    
    public Location getHome(String name) {
        return homes.get(name.toLowerCase());
    }
    
    public void deleteHome(String name) {
        homes.remove(name.toLowerCase());
    }
    
    public Map<String, Location> getHomes() {
        return Collections.unmodifiableMap(homes);
    }
    
    public long getKitLastUsed(String kitName) {
        return kitCooldowns.getOrDefault(kitName.toLowerCase(), 0L);
    }
    
    public void setKitLastUsed(String kitName, long time) {
        kitCooldowns.put(kitName.toLowerCase(), time);
    }
    
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    
    public boolean isVanished() { return vanished; }
    public void setVanished(boolean vanished) { this.vanished = vanished; }
    
    public boolean isGodMode() { return godMode; }
    public void setGodMode(boolean godMode) { this.godMode = godMode; }
    
    public boolean isAfk() { return afk; }
    public void setAfk(boolean afk) { this.afk = afk; }
    
    public Location getLastLocation() { return lastLocation; }
    public void setLastLocation(Location location) { this.lastLocation = location; }
    
    public boolean isJailed() { return jailed; }
    public void setJailed(boolean jailed) { this.jailed = jailed; }
    
    public String getJailName() { return jailName; }
    public void setJailName(String jailName) { this.jailName = jailName; }
    
    public long getJailExpiry() { return jailExpiry; }
    public void setJailExpiry(long jailExpiry) { this.jailExpiry = jailExpiry; }
    
    public String getJailReason() { return jailReason; }
    public void setJailReason(String jailReason) { this.jailReason = jailReason; }
    
    public boolean isMuted() { return muted; }
    public void setMuted(boolean muted) { this.muted = muted; }
    
    public long getMuteExpiry() { return muteExpiry; }
    public void setMuteExpiry(long muteExpiry) { this.muteExpiry = muteExpiry; }
    
    public String getMuteReason() { return muteReason; }
    public void setMuteReason(String muteReason) { this.muteReason = muteReason; }
}
