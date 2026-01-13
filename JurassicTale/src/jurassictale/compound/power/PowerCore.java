package jurassictale.compound.power;

import java.util.UUID;

public class PowerCore {
    
    private final UUID id;
    private final PowerCoreTier tier;
    
    private final double x, y, z;
    private final int radiusBlocks;
    
    private int health;
    private int maxHealth;
    private boolean active;
    
    private long fuelRemaining;
    
    public PowerCore(UUID id, PowerCoreTier tier, double x, double y, double z) {
        this.id = id;
        this.tier = tier;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radiusBlocks = tier.getRadiusBlocks();
        this.maxHealth = tier.getMaxHealth();
        this.health = maxHealth;
        this.active = true;
        this.fuelRemaining = tier.getFuelCapacity();
    }
    
    public UUID getId() { return id; }
    public PowerCoreTier getTier() { return tier; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public int getRadiusBlocks() { return radiusBlocks; }
    
    public long getPowerOutput() {
        return active ? tier.getPowerOutput() : 0;
    }
    
    public long getPowerStorage() {
        return tier.getPowerStorage();
    }
    
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public void setHealth(int health) { this.health = Math.max(0, Math.min(maxHealth, health)); }
    public void damage(int amount) { setHealth(health - amount); }
    public boolean isDestroyed() { return health <= 0; }
    
    public boolean isActive() { return active && health > 0; }
    public void setActive(boolean active) { this.active = active; }
    
    public long getFuelRemaining() { return fuelRemaining; }
    public void consumeFuel(long amount) { fuelRemaining = Math.max(0, fuelRemaining - amount); }
    public void refuel(long amount) { fuelRemaining = Math.min(tier.getFuelCapacity(), fuelRemaining + amount); }
    
    public boolean isInRange(double px, double py, double pz) {
        double dx = px - x;
        double dy = py - y;
        double dz = pz - z;
        double distSq = dx * dx + dy * dy + dz * dz;
        return distSq <= radiusBlocks * radiusBlocks;
    }
    
    public enum PowerCoreTier {
        FIELD_GENERATOR("Field Generator", 1, 100, 500, 32, 1000, 10000),
        INDUSTRIAL("Industrial Generator", 2, 500, 2000, 48, 5000, 50000),
        NUCLEAR("Nuclear Reactor", 3, 2000, 10000, 64, 10000, 500000),
        FUSION("Fusion Reactor", 4, 10000, 50000, 96, 50000, Long.MAX_VALUE);
        
        private final String displayName;
        private final int tier;
        private final long powerOutput;
        private final long powerStorage;
        private final int radiusBlocks;
        private final int maxHealth;
        private final long fuelCapacity;
        
        PowerCoreTier(String displayName, int tier, long powerOutput, long powerStorage, 
                      int radiusBlocks, int maxHealth, long fuelCapacity) {
            this.displayName = displayName;
            this.tier = tier;
            this.powerOutput = powerOutput;
            this.powerStorage = powerStorage;
            this.radiusBlocks = radiusBlocks;
            this.maxHealth = maxHealth;
            this.fuelCapacity = fuelCapacity;
        }
        
        public String getDisplayName() { return displayName; }
        public int getTier() { return tier; }
        public long getPowerOutput() { return powerOutput; }
        public long getPowerStorage() { return powerStorage; }
        public int getRadiusBlocks() { return radiusBlocks; }
        public int getMaxHealth() { return maxHealth; }
        public long getFuelCapacity() { return fuelCapacity; }
    }
}
