package com.yellowtale.rubidium.models;

public record BoundingBox(
    double minX, double minY, double minZ,
    double maxX, double maxY, double maxZ
) {
    
    public double getWidth() { return maxX - minX; }
    public double getHeight() { return maxY - minY; }
    public double getDepth() { return maxZ - minZ; }
    
    public double getCenterX() { return (minX + maxX) / 2; }
    public double getCenterY() { return (minY + maxY) / 2; }
    public double getCenterZ() { return (minZ + maxZ) / 2; }
    
    public double getVolume() {
        return getWidth() * getHeight() * getDepth();
    }
    
    public BoundingBox scale(float factor) {
        double cx = getCenterX();
        double cy = getCenterY();
        double cz = getCenterZ();
        
        double hw = getWidth() / 2 * factor;
        double hh = getHeight() / 2 * factor;
        double hd = getDepth() / 2 * factor;
        
        return new BoundingBox(
            cx - hw, cy - hh, cz - hd,
            cx + hw, cy + hh, cz + hd
        );
    }
    
    public BoundingBox expand(double amount) {
        return new BoundingBox(
            minX - amount, minY - amount, minZ - amount,
            maxX + amount, maxY + amount, maxZ + amount
        );
    }
    
    public BoundingBox offset(double x, double y, double z) {
        return new BoundingBox(
            minX + x, minY + y, minZ + z,
            maxX + x, maxY + y, maxZ + z
        );
    }
    
    public boolean contains(double x, double y, double z) {
        return x >= minX && x <= maxX &&
               y >= minY && y <= maxY &&
               z >= minZ && z <= maxZ;
    }
    
    public boolean intersects(BoundingBox other) {
        return this.minX <= other.maxX && this.maxX >= other.minX &&
               this.minY <= other.maxY && this.maxY >= other.minY &&
               this.minZ <= other.maxZ && this.maxZ >= other.minZ;
    }
    
    public BoundingBox combine(BoundingBox other) {
        return new BoundingBox(
            Math.min(this.minX, other.minX),
            Math.min(this.minY, other.minY),
            Math.min(this.minZ, other.minZ),
            Math.max(this.maxX, other.maxX),
            Math.max(this.maxY, other.maxY),
            Math.max(this.maxZ, other.maxZ)
        );
    }
}
