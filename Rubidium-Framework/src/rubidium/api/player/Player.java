package rubidium.api.player;

import java.util.UUID;

public interface Player extends CommandSender {
    
    UUID getUUID();
    
    default UUID getUuid() { return getUUID(); }
    
    default UUID getUniqueId() { return getUUID(); }
    
    default UUID getId() { return getUUID(); }
    
    String getName();
    
    String getDisplayName();
    
    void setDisplayName(String displayName);
    
    boolean isOnline();
    
    void kick(String reason);
    
    void teleport(double x, double y, double z);
    
    void teleport(double x, double y, double z, float yaw, float pitch);
    
    Location getLocation();
    
    default double getX() { return getLocation().x(); }
    default double getY() { return getLocation().y(); }
    default double getZ() { return getLocation().z(); }
    default float getYaw() { return getLocation().yaw(); }
    default float getPitch() { return getLocation().pitch(); }
    
    String getWorld();
    
    int getPing();
    
    String getAddress();
    
    long getFirstPlayed();
    
    long getLastPlayed();
    
    boolean hasPlayedBefore();
    
    void setOp(boolean op);
    
    boolean isOp();
    
    void showTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut);
    
    void showActionBar(String message);
    
    void playSound(String sound, float volume, float pitch);
    
    PlayerInventory getInventory();
    
    PlayerData getData();
    
    void sendPacket(Object packet);
    
    default double getHealth() { return 20.0; }
    
    default void setHealth(double health) {}
    
    default double getMaxHealth() { return 20.0; }
    
    default boolean isFlying() { return false; }
    
    default void setFlying(boolean flying) {}
    
    default boolean isGliding() { return false; }
    
    default boolean isSprinting() { return false; }
    
    default boolean isSneaking() { return false; }
    
    default boolean isSwimming() { return false; }
    
    default boolean isOnGround() { return true; }
    
    default double getVelX() { return 0.0; }
    
    default double getVelY() { return 0.0; }
    
    default double getVelZ() { return 0.0; }
    
    default double getArmor() { return 0.0; }
    
    default int getHeldItemSlot() { return 0; }
    
    default int getFoodLevel() { return 20; }
    
    default void setFoodLevel(int level) {}
    
    default GameMode getGameMode() { return GameMode.SURVIVAL; }
    
    default void setGameMode(GameMode mode) {}
    
    default Location getPosition() { return getLocation(); }
    
    void teleport(Location location);
    
    enum GameMode {
        SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR
    }
    
    record Location(double x, double y, double z, float yaw, float pitch) {
        public double distanceTo(Location other) {
            double dx = x - other.x;
            double dy = y - other.y;
            double dz = z - other.z;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
        
        public double distanceSquared(Location other) {
            double dx = x - other.x;
            double dy = y - other.y;
            double dz = z - other.z;
            return dx * dx + dy * dy + dz * dz;
        }
    }
}
