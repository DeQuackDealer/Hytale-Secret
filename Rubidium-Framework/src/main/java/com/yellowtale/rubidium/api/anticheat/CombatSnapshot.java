package com.yellowtale.rubidium.api.anticheat;

import java.util.Optional;
import java.util.UUID;

public class CombatSnapshot {
    private final boolean isAttack;
    private final UUID targetId;
    private final Double distanceToTarget;
    private final Double angleToTarget;
    private final double damageDealt;
    private final String weaponType;
    private final boolean wasCritical;
    private final long timestamp;
    
    private CombatSnapshot(Builder builder) {
        this.isAttack = builder.isAttack;
        this.targetId = builder.targetId;
        this.distanceToTarget = builder.distanceToTarget;
        this.angleToTarget = builder.angleToTarget;
        this.damageDealt = builder.damageDealt;
        this.weaponType = builder.weaponType;
        this.wasCritical = builder.wasCritical;
        this.timestamp = builder.timestamp;
    }
    
    public boolean isAttack() { return isAttack; }
    public Optional<UUID> getTargetId() { return Optional.ofNullable(targetId); }
    public Optional<Double> getDistanceToTarget() { return Optional.ofNullable(distanceToTarget); }
    public Optional<Double> getAngleToTarget() { return Optional.ofNullable(angleToTarget); }
    public double getDamageDealt() { return damageDealt; }
    public Optional<String> getWeaponType() { return Optional.ofNullable(weaponType); }
    public boolean wasCritical() { return wasCritical; }
    public long getTimestamp() { return timestamp; }
    
    public static Builder attack(UUID targetId, double distance, double angle) {
        return new Builder(true).target(targetId, distance, angle);
    }
    
    public static Builder miss() {
        return new Builder(true);
    }
    
    public static class Builder {
        private boolean isAttack;
        private UUID targetId;
        private Double distanceToTarget;
        private Double angleToTarget;
        private double damageDealt = 0.0;
        private String weaponType;
        private boolean wasCritical = false;
        private long timestamp = System.currentTimeMillis();
        
        public Builder(boolean isAttack) {
            this.isAttack = isAttack;
        }
        
        public Builder target(UUID targetId, double distance, double angle) {
            this.targetId = targetId;
            this.distanceToTarget = distance;
            this.angleToTarget = angle;
            return this;
        }
        
        public Builder damage(double damage) {
            this.damageDealt = damage;
            return this;
        }
        
        public Builder weapon(String weaponType) {
            this.weaponType = weaponType;
            return this;
        }
        
        public Builder critical(boolean wasCritical) {
            this.wasCritical = wasCritical;
            return this;
        }
        
        public CombatSnapshot build() {
            return new CombatSnapshot(this);
        }
    }
}
