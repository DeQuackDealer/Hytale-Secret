package com.yellowtale.jurassictale.ai.steering.behaviors;

import com.yellowtale.jurassictale.ai.AIContext;
import com.yellowtale.jurassictale.ai.steering.*;

import java.util.function.Function;

public class FleeBehavior implements SteeringBehavior {
    
    private final Function<AIContext, Vec2> threatProvider;
    private final float maxSpeed;
    private final float panicDistance;
    
    public FleeBehavior(Function<AIContext, Vec2> threatProvider, float maxSpeed, float panicDistance) {
        this.threatProvider = threatProvider;
        this.maxSpeed = maxSpeed;
        this.panicDistance = panicDistance;
    }
    
    public static FleeBehavior fromThreat(float maxSpeed, float panicDistance) {
        return new FleeBehavior(
            ctx -> ctx.perception().getNearestThreatPosition().orElse(Vec2.ZERO),
            maxSpeed,
            panicDistance
        );
    }
    
    @Override
    public SteeringOutput calculate(AIContext context) {
        Vec2 threat = threatProvider.apply(context);
        if (threat.isZero()) return SteeringOutput.ZERO;
        
        Vec2 position = Vec2.of(context.entity().getX(), context.entity().getZ());
        double distance = position.distanceTo(threat);
        
        if (distance > panicDistance) return SteeringOutput.ZERO;
        
        Vec2 desired = position.subtract(threat).normalize().multiply(maxSpeed);
        Vec2 velocity = Vec2.of(context.entity().getVelocityX(), context.entity().getVelocityZ());
        
        float urgency = (float) (1.0 - distance / panicDistance);
        return SteeringOutput.linear(desired.subtract(velocity).multiply(urgency));
    }
}
