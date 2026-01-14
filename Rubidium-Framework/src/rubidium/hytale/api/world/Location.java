package rubidium.hytale.api.world;

/**
 * Represents a location in the world.
 */
public class Location {
    
    private World world;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    
    public Location(World world, double x, double y, double z) {
        this(world, x, y, z, 0, 0);
    }
    
    public Location(World world, double x, double y, double z, float yaw, float pitch) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }
    
    public World getWorld() { return world; }
    public void setWorld(World world) { this.world = world; }
    
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    
    public double getZ() { return z; }
    public void setZ(double z) { this.z = z; }
    
    public float getYaw() { return yaw; }
    public void setYaw(float yaw) { this.yaw = yaw; }
    
    public float getPitch() { return pitch; }
    public void setPitch(float pitch) { this.pitch = pitch; }
    
    public int getBlockX() { return (int) Math.floor(x); }
    public int getBlockY() { return (int) Math.floor(y); }
    public int getBlockZ() { return (int) Math.floor(z); }
    
    public Location add(double x, double y, double z) {
        return new Location(world, this.x + x, this.y + y, this.z + z, yaw, pitch);
    }
    
    public Location subtract(double x, double y, double z) {
        return new Location(world, this.x - x, this.y - y, this.z - z, yaw, pitch);
    }
    
    public double distance(Location other) {
        return Math.sqrt(distanceSquared(other));
    }
    
    public double distanceSquared(Location other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }
    
    public Location clone() {
        return new Location(world, x, y, z, yaw, pitch);
    }
    
    @Override
    public String toString() {
        return String.format("Location{world=%s, x=%.2f, y=%.2f, z=%.2f}", 
            world != null ? world.getName() : "null", x, y, z);
    }
}
