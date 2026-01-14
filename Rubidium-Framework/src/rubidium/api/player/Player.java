package rubidium.api.player;

import java.util.UUID;

public interface Player extends CommandSender {
    
    UUID getUUID();
    
    default UUID getUuid() { return getUUID(); }
    
    default UUID getUniqueId() { return getUUID(); }
    
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
