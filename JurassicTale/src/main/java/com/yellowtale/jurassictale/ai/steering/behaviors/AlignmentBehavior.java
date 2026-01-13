package com.yellowtale.jurassictale.ai.steering.behaviors;

import com.yellowtale.jurassictale.ai.AIContext;
import com.yellowtale.jurassictale.ai.steering.*;

import java.util.List;

public class AlignmentBehavior implements SteeringBehavior {
    
    private final float maxSpeed;
    
    public AlignmentBehavior(float maxSpeed) {
        this.maxSpeed = maxSpeed;
    }
    
    @Override
    public SteeringOutput calculate(AIContext context) {
        List<Vec2> neighborVelocities = context.perception().getNearbyAllyVelocities();
        if (neighborVelocities.isEmpty()) return SteeringOutput.ZERO;
        
        Vec2 avgVelocity = Vec2.ZERO;
        for (Vec2 vel : neighborVelocities) {
            avgVelocity = avgVelocity.add(vel);
        }
        avgVelocity = avgVelocity.divide(neighborVelocities.size());
        
        if (avgVelocity.isZero()) return SteeringOutput.ZERO;
        
        Vec2 desired = avgVelocity.normalize().multiply(maxSpeed);
        Vec2 velocity = Vec2.of(context.entity().getVelocityX(), context.entity().getVelocityZ());
        
        return SteeringOutput.linear(desired.subtract(velocity));
    }
}
