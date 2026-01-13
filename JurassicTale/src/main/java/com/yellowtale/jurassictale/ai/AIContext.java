package com.yellowtale.jurassictale.ai;

import com.yellowtale.jurassictale.ai.perception.PerceptionData;
import com.yellowtale.jurassictale.ai.steering.Vec2;
import com.yellowtale.jurassictale.dino.DinoEntity;

import java.util.Optional;

public record AIContext(
    DinoEntity entity,
    PerceptionData perception,
    float deltaTime,
    long currentTime
) {
    public double getHealth() {
        return entity.getHealth();
    }
    
    public double getMaxHealth() {
        return entity.getDefinition().getHealth();
    }
    
    public double getHealthPercent() {
        return getHealth() / getMaxHealth();
    }
    
    public double getHunger() {
        return entity.getHunger();
    }
    
    public double getTorpor() {
        return entity.getTorpor();
    }
    
    public boolean isInDanger() {
        return perception.hasThreats();
    }
    
    public boolean hasTarget() {
        return perception.hasTarget();
    }
    
    public Optional<Vec2> getTargetPosition() {
        return perception.getTargetPosition();
    }
    
    public double getDistanceToNearestThreat() {
        return perception.getNearestThreatDistance();
    }
    
    public int getNearbyAllyCount() {
        return perception.getAllyCount();
    }
    
    public double getAggressionMultiplier() {
        return entity.getAggressionMultiplier();
    }
    
    public long getStateTime() {
        return entity.getStateTime();
    }
}
