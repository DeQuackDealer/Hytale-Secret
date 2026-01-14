package rubidium.hytale.api.player;

import rubidium.hytale.api.world.Location;
import rubidium.hytale.api.world.World;

import java.util.UUID;

/**
 * Represents a player in the game.
 */
public interface Player {
    
    UUID getUuid();
    
    default UUID getUUID() { return getUuid(); }
    
    default UUID getUniqueId() { return getUuid(); }
    
    String getUsername();
    
    default String getName() { return getUsername(); }
    
    String getDisplayName();
    
    void setDisplayName(String displayName);
    
    boolean isOnline();
    
    boolean isOp();
    
    boolean hasPermission(String permission);
    
    void sendMessage(String message);
    
    void sendActionBar(String message);
    
    void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut);
    
    Location getLocation();
    
    default double getX() { return getLocation().getX(); }
    default double getY() { return getLocation().getY(); }
    default double getZ() { return getLocation().getZ(); }
    default float getYaw() { return getLocation().getYaw(); }
    default float getPitch() { return getLocation().getPitch(); }
    
    void teleport(Location location);
    
    default void teleport(double x, double y, double z) {
        teleport(new Location(getWorld(), x, y, z, getYaw(), getPitch()));
    }
    
    default void teleport(double x, double y, double z, float yaw, float pitch) {
        teleport(new Location(getWorld(), x, y, z, yaw, pitch));
    }
    
    default void sendPacket(Object packet) {
    }
    
    World getWorld();
    
    double getHealth();
    
    void setHealth(double health);
    
    double getMaxHealth();
    
    void setMaxHealth(double maxHealth);
    
    int getFoodLevel();
    
    void setFoodLevel(int level);
    
    int getPing();
    
    void kick(String reason);
    
    void playSound(String sound, float volume, float pitch);
    
    void playSound(Location location, String sound, float volume, float pitch);
    
    boolean isFlying();
    
    void setFlying(boolean flying);
    
    void setAllowFlight(boolean allow);
    
    boolean getAllowFlight();
    
    void setGameMode(String gameMode);
    
    String getGameMode();
}
