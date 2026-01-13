package jurassictale.ai.steering;

public record Vec2(double x, double z) {
    
    public static final Vec2 ZERO = new Vec2(0, 0);
    
    public static Vec2 of(double x, double z) {
        return new Vec2(x, z);
    }
    
    public static Vec2 fromAngle(double radians) {
        return new Vec2(Math.cos(radians), Math.sin(radians));
    }
    
    public Vec2 add(Vec2 other) {
        return new Vec2(x + other.x, z + other.z);
    }
    
    public Vec2 subtract(Vec2 other) {
        return new Vec2(x - other.x, z - other.z);
    }
    
    public Vec2 multiply(double scalar) {
        return new Vec2(x * scalar, z * scalar);
    }
    
    public Vec2 divide(double scalar) {
        if (scalar == 0) return ZERO;
        return new Vec2(x / scalar, z / scalar);
    }
    
    public double length() {
        return Math.sqrt(x * x + z * z);
    }
    
    public double lengthSquared() {
        return x * x + z * z;
    }
    
    public Vec2 normalize() {
        double len = length();
        if (len == 0) return ZERO;
        return new Vec2(x / len, z / len);
    }
    
    public Vec2 limit(double maxLength) {
        double lenSq = lengthSquared();
        if (lenSq > maxLength * maxLength) {
            return normalize().multiply(maxLength);
        }
        return this;
    }
    
    public Vec2 truncate(double maxLength) {
        return limit(maxLength);
    }
    
    public double dot(Vec2 other) {
        return x * other.x + z * other.z;
    }
    
    public double distanceTo(Vec2 other) {
        double dx = x - other.x;
        double dz = z - other.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
    
    public double distanceSquaredTo(Vec2 other) {
        double dx = x - other.x;
        double dz = z - other.z;
        return dx * dx + dz * dz;
    }
    
    public double angle() {
        return Math.atan2(z, x);
    }
    
    public Vec2 rotate(double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec2(x * cos - z * sin, x * sin + z * cos);
    }
    
    public Vec2 perpendicular() {
        return new Vec2(-z, x);
    }
    
    public Vec2 negate() {
        return new Vec2(-x, -z);
    }
    
    public boolean isZero() {
        return x == 0 && z == 0;
    }
    
    public Vec2 lerp(Vec2 target, double t) {
        return new Vec2(
            x + (target.x - x) * t,
            z + (target.z - z) * t
        );
    }
}
