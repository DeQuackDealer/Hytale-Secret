package rubidium.hytale.adapter;

import rubidium.hytale.api.player.Player;
import rubidium.hytale.api.world.Location;
import rubidium.hytale.api.world.World;

import java.util.UUID;

/**
 * Adapts the real Hytale ServerPlayer to Rubidium's Player interface.
 * This is a placeholder implementation until the official Hytale SDK is available.
 */
public class PlayerAdapter implements Player {
    
    private final UUID uuid;
    private String username;
    private String displayName;
    private Location location;
    private World world;
    private double health = 20.0;
    private double maxHealth = 20.0;
    private int foodLevel = 20;
    private int ping = 0;
    private boolean online = true;
    private boolean op = false;
    private boolean flying = false;
    private boolean allowFlight = false;
    private String gameMode = "survival";
    
    public PlayerAdapter(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
        this.displayName = username;
        this.world = null;
        this.location = new Location(null, 0, 64, 0, 0, 0);
    }
    
    @Override
    public UUID getUuid() {
        return uuid;
    }
    
    @Override
    public String getUsername() {
        return username;
    }
    
    @Override
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    @Override
    public boolean isOnline() {
        return online;
    }
    
    public void setOnline(boolean online) {
        this.online = online;
    }
    
    @Override
    public boolean isOp() {
        return op;
    }
    
    public void setOp(boolean op) {
        this.op = op;
    }
    
    @Override
    public boolean hasPermission(String permission) {
        return op;
    }
    
    @Override
    public void sendMessage(String message) {
    }
    
    @Override
    public void sendActionBar(String message) {
    }
    
    @Override
    public void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
    }
    
    @Override
    public Location getLocation() {
        return location;
    }
    
    @Override
    public void teleport(Location location) {
        this.location = location;
        if (location.getWorld() != null) {
            this.world = location.getWorld();
        }
    }
    
    @Override
    public World getWorld() {
        return world;
    }
    
    public void setWorld(World world) {
        this.world = world;
    }
    
    @Override
    public double getHealth() {
        return health;
    }
    
    @Override
    public void setHealth(double health) {
        this.health = Math.max(0, Math.min(health, maxHealth));
    }
    
    @Override
    public double getMaxHealth() {
        return maxHealth;
    }
    
    @Override
    public void setMaxHealth(double maxHealth) {
        this.maxHealth = maxHealth;
    }
    
    @Override
    public int getFoodLevel() {
        return foodLevel;
    }
    
    @Override
    public void setFoodLevel(int level) {
        this.foodLevel = Math.max(0, Math.min(level, 20));
    }
    
    @Override
    public int getPing() {
        return ping;
    }
    
    public void setPing(int ping) {
        this.ping = ping;
    }
    
    @Override
    public void kick(String reason) {
        online = false;
    }
    
    @Override
    public void playSound(String sound, float volume, float pitch) {
    }
    
    @Override
    public void playSound(Location location, String sound, float volume, float pitch) {
    }
    
    @Override
    public boolean isFlying() {
        return flying;
    }
    
    @Override
    public void setFlying(boolean flying) {
        this.flying = flying;
    }
    
    @Override
    public void setAllowFlight(boolean allow) {
        this.allowFlight = allow;
    }
    
    @Override
    public boolean getAllowFlight() {
        return allowFlight;
    }
    
    @Override
    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
    }
    
    @Override
    public String getGameMode() {
        return gameMode;
    }
}
