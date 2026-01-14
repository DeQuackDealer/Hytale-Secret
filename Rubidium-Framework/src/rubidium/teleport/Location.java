package rubidium.teleport;

import java.util.Objects;

public record Location(
    String world,
    double x,
    double y,
    double z,
    float yaw,
    float pitch
) {
    public Location(String world, double x, double y, double z) {
        this(world, x, y, z, 0.0f, 0.0f);
    }
    
    public Location withYawPitch(float yaw, float pitch) {
        return new Location(world, x, y, z, yaw, pitch);
    }
    
    public Location add(double dx, double dy, double dz) {
        return new Location(world, x + dx, y + dy, z + dz, yaw, pitch);
    }
    
    public double distanceTo(Location other) {
        if (!world.equals(other.world)) {
            return Double.MAX_VALUE;
        }
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    public int blockX() { return (int) Math.floor(x); }
    public int blockY() { return (int) Math.floor(y); }
    public int blockZ() { return (int) Math.floor(z); }
    
    public String toReadableString() {
        return String.format("%s: %.1f, %.1f, %.1f", world, x, y, z);
    }
    
    public String serialize() {
        return String.format("%s;%.3f;%.3f;%.3f;%.3f;%.3f", world, x, y, z, yaw, pitch);
    }
    
    public static Location deserialize(String data) {
        var parts = data.split(";");
        if (parts.length < 4) return null;
        try {
            String world = parts[0];
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0f;
            float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0f;
            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location loc)) return false;
        return Double.compare(x, loc.x) == 0 &&
               Double.compare(y, loc.y) == 0 &&
               Double.compare(z, loc.z) == 0 &&
               world.equals(loc.world);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(world, x, y, z);
    }
}
