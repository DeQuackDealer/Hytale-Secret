package com.yellowtale.jurassictale.dino;

import com.yellowtale.jurassictale.dino.behavior.BehaviorController;
import com.yellowtale.jurassictale.territory.TerritoryType;

import java.util.UUID;

public class DinoEntity {
    
    private final UUID id;
    private final DinoDefinition definition;
    
    private double x, y, z;
    private float yaw, pitch;
    
    private int health;
    private int maxHealth;
    private double aggressionMultiplier;
    
    private UUID packId;
    private TerritoryType currentTerritory;
    private BehaviorController behaviorController;
    
    private DinoState state;
    private UUID targetEntityId;
    private long stateStartTime;
    
    private String currentAnimation;
    private float animationTime;
    
    private int torpor;
    private boolean tranquilized;
    private boolean captured;
    
    public DinoEntity(UUID id, DinoDefinition definition, double x, double y, double z) {
        this.id = id;
        this.definition = definition;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = 0;
        this.pitch = 0;
        this.maxHealth = definition.getBaseHealth();
        this.health = maxHealth;
        this.aggressionMultiplier = 1.0;
        this.state = DinoState.IDLE;
        this.stateStartTime = System.currentTimeMillis();
        this.currentAnimation = "idle";
        this.animationTime = 0;
        this.torpor = 0;
        this.tranquilized = false;
        this.captured = false;
    }
    
    public UUID getId() { return id; }
    public DinoDefinition getDefinition() { return definition; }
    
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public void setRotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }
    
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public void setHealth(int health) { this.health = Math.max(0, Math.min(maxHealth, health)); }
    public void damage(int amount) { setHealth(health - amount); }
    public void heal(int amount) { setHealth(health + amount); }
    public boolean isDead() { return health <= 0; }
    
    public double getAggressionMultiplier() { return aggressionMultiplier; }
    public void setAggressionMultiplier(double multiplier) { this.aggressionMultiplier = multiplier; }
    
    public UUID getPackId() { return packId; }
    public void setPackId(UUID packId) { this.packId = packId; }
    public boolean isInPack() { return packId != null; }
    
    public TerritoryType getCurrentTerritory() { return currentTerritory; }
    public void setCurrentTerritory(TerritoryType territory) { this.currentTerritory = territory; }
    
    public BehaviorController getBehaviorController() { return behaviorController; }
    public void setBehaviorController(BehaviorController controller) { this.behaviorController = controller; }
    
    public DinoState getState() { return state; }
    public void setState(DinoState state) {
        this.state = state;
        this.stateStartTime = System.currentTimeMillis();
    }
    public long getStateTime() { return System.currentTimeMillis() - stateStartTime; }
    
    public UUID getTargetEntityId() { return targetEntityId; }
    public void setTargetEntityId(UUID targetId) { this.targetEntityId = targetId; }
    
    public String getCurrentAnimation() { return currentAnimation; }
    public void setCurrentAnimation(String animation) { 
        if (!animation.equals(this.currentAnimation)) {
            this.currentAnimation = animation;
            this.animationTime = 0;
        }
    }
    
    public void updateAnimation() {
        animationTime += 0.05f;
    }
    
    public int getTorpor() { return torpor; }
    public void addTorpor(int amount) {
        this.torpor += amount;
        if (torpor >= maxHealth) {
            tranquilized = true;
            setState(DinoState.UNCONSCIOUS);
        }
    }
    public void reduceTorpor(int amount) {
        this.torpor = Math.max(0, torpor - amount);
        if (torpor == 0 && tranquilized) {
            tranquilized = false;
            setState(DinoState.IDLE);
        }
    }
    
    public boolean isTransquilized() { return tranquilized; }
    public boolean isCaptured() { return captured; }
    public void setCaptured(boolean captured) { this.captured = captured; }
    
    public double distanceSquared(double x, double y, double z) {
        double dx = this.x - x;
        double dy = this.y - y;
        double dz = this.z - z;
        return dx * dx + dy * dy + dz * dz;
    }
    
    public double distance(double x, double y, double z) {
        return Math.sqrt(distanceSquared(x, y, z));
    }
    
    public void lookAt(double x, double y, double z) {
        double dx = x - this.x;
        double dy = y - this.y;
        double dz = z - this.z;
        
        double dist = Math.sqrt(dx * dx + dz * dz);
        this.yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        this.pitch = (float) Math.toDegrees(Math.atan2(dy, dist));
    }
    
    public void moveTowards(double targetX, double targetY, double targetZ, double speed) {
        double dx = targetX - x;
        double dy = targetY - y;
        double dz = targetZ - z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        
        if (dist > 0.1) {
            double factor = speed * definition.getBaseSpeed() / dist;
            x += dx * factor;
            y += dy * factor;
            z += dz * factor;
            lookAt(targetX, targetY, targetZ);
        }
    }
    
    public enum DinoState {
        IDLE,
        WANDERING,
        FLEEING,
        CHASING,
        ATTACKING,
        EATING,
        DRINKING,
        SLEEPING,
        DEFENDING,
        ROARING,
        UNCONSCIOUS,
        CAPTURED
    }
}
