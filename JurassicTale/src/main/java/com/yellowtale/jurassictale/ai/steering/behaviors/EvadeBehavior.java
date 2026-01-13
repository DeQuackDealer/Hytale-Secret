package com.yellowtale.jurassictale.ai.steering.behaviors;

import com.yellowtale.jurassictale.ai.AIContext;
import com.yellowtale.jurassictale.ai.steering.*;

public class EvadeBehavior implements SteeringBehavior {
    
    private final float maxSpeed;
    private final float predictionTime;
    private final float panicDistance;
    
    public EvadeBehavior(float maxSpeed, float predictionTime, float panicDistance) {
        this.maxSpeed = maxSpeed;
        this.predictionTime = predictionTime;
        this.panicDistance = panicDistance;
    }
    
    @Override
    public SteeringOutput calculate(AIContext context) {
        if (!context.perception().hasThreats()) return SteeringOutput.ZERO;
        
        Vec2 threatPos = context.perception().getNearestThreatPosition().orElse(Vec2.ZERO);
        Vec2 threatVel = context.perception().getNearestThreatVelocity().orElse(Vec2.ZERO);
        
        Vec2 position = Vec2.of(context.entity().getX(), context.entity().getZ());
        double distance = position.distanceTo(threatPos);
        
        if (distance > panicDistance) return SteeringOutput.ZERO;
        
        float lookAhead = (float) Math.min(predictionTime, distance / maxSpeed);
        Vec2 predictedPos = threatPos.add(threatVel.multiply(lookAhead));
        
        Vec2 desired = position.subtract(predictedPos).normalize().multiply(maxSpeed);
        Vec2 velocity = Vec2.of(context.entity().getVelocityX(), context.entity().getVelocityZ());
        
        float urgency = (float)(1.0 - distance / panicDistance);
        return SteeringOutput.linear(desired.subtract(velocity).multiply(urgency));
    }
}
