package com.yellowtale.rubidium.api.anticheat;

public class MovementSnapshot {
    private final double x, y, z;
    private final float yaw, pitch;
    private final boolean onGround;
    private final boolean inWater;
    private final boolean isGliding;
    private final boolean isTeleporting;
    private final boolean tookFallDamage;
    private final long timestamp;
    private final String world;
    
    private MovementSnapshot(Builder builder) {
        this.x = builder.x;
        this.y = builder.y;
        this.z = builder.z;
        this.yaw = builder.yaw;
        this.pitch = builder.pitch;
        this.onGround = builder.onGround;
        this.inWater = builder.inWater;
        this.isGliding = builder.isGliding;
        this.isTeleporting = builder.isTeleporting;
        this.tookFallDamage = builder.tookFallDamage;
        this.timestamp = builder.timestamp;
        this.world = builder.world;
    }
    
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public boolean isOnGround() { return onGround; }
    public boolean isInWater() { return inWater; }
    public boolean isGliding() { return isGliding; }
    public boolean isTeleporting() { return isTeleporting; }
    public boolean tookFallDamage() { return tookFallDamage; }
    public long getTimestamp() { return timestamp; }
    public String getWorld() { return world; }
    
    public double distanceTo(MovementSnapshot other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    public static Builder builder(double x, double y, double z) {
        return new Builder(x, y, z);
    }
    
    public static class Builder {
        private double x, y, z;
        private float yaw, pitch;
        private boolean onGround = true;
        private boolean inWater = false;
        private boolean isGliding = false;
        private boolean isTeleporting = false;
        private boolean tookFallDamage = false;
        private long timestamp = System.currentTimeMillis();
        private String world = "world";
        
        public Builder(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        public Builder rotation(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
            return this;
        }
        
        public Builder onGround(boolean onGround) {
            this.onGround = onGround;
            return this;
        }
        
        public Builder inWater(boolean inWater) {
            this.inWater = inWater;
            return this;
        }
        
        public Builder gliding(boolean isGliding) {
            this.isGliding = isGliding;
            return this;
        }
        
        public Builder teleporting(boolean isTeleporting) {
            this.isTeleporting = isTeleporting;
            return this;
        }
        
        public Builder fallDamage(boolean tookFallDamage) {
            this.tookFallDamage = tookFallDamage;
            return this;
        }
        
        public Builder world(String world) {
            this.world = world;
            return this;
        }
        
        public MovementSnapshot build() {
            return new MovementSnapshot(this);
        }
    }
}
