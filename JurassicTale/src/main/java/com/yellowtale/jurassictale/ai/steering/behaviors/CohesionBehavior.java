package com.yellowtale.jurassictale.ai.steering.behaviors;

import com.yellowtale.jurassictale.ai.AIContext;
import com.yellowtale.jurassictale.ai.steering.*;

import java.util.List;

public class CohesionBehavior implements SteeringBehavior {
    
    private final float maxSpeed;
    
    public CohesionBehavior(float maxSpeed) {
        this.maxSpeed = maxSpeed;
    }
    
    @Override
    public SteeringOutput calculate(AIContext context) {
        List<Vec2> neighbors = context.perception().getNearbyAllyPositions();
        if (neighbors.isEmpty()) return SteeringOutput.ZERO;
        
        Vec2 centerOfMass = Vec2.ZERO;
        for (Vec2 neighbor : neighbors) {
            centerOfMass = centerOfMass.add(neighbor);
        }
        centerOfMass = centerOfMass.divide(neighbors.size());
        
        Vec2 position = Vec2.of(context.entity().getX(), context.entity().getZ());
        Vec2 desired = centerOfMass.subtract(position).normalize().multiply(maxSpeed);
        Vec2 velocity = Vec2.of(context.entity().getVelocityX(), context.entity().getVelocityZ());
        
        return SteeringOutput.linear(desired.subtract(velocity));
    }
}
