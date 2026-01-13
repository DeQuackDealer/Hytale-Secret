package com.yellowtale.jurassictale.ai.steering.behaviors;

import com.yellowtale.jurassictale.ai.AIContext;
import com.yellowtale.jurassictale.ai.steering.*;

import java.util.function.Function;

public class SeekBehavior implements SteeringBehavior {
    
    private final Function<AIContext, Vec2> targetProvider;
    private final float maxSpeed;
    
    public SeekBehavior(Function<AIContext, Vec2> targetProvider, float maxSpeed) {
        this.targetProvider = targetProvider;
        this.maxSpeed = maxSpeed;
    }
    
    public static SeekBehavior toPosition(Vec2 target, float maxSpeed) {
        return new SeekBehavior(ctx -> target, maxSpeed);
    }
    
    public static SeekBehavior toTarget(float maxSpeed) {
        return new SeekBehavior(ctx -> ctx.getTargetPosition().orElse(Vec2.ZERO), maxSpeed);
    }
    
    @Override
    public SteeringOutput calculate(AIContext context) {
        Vec2 target = targetProvider.apply(context);
        if (target.isZero()) return SteeringOutput.ZERO;
        
        Vec2 position = Vec2.of(context.entity().getX(), context.entity().getZ());
        Vec2 desired = target.subtract(position).normalize().multiply(maxSpeed);
        Vec2 velocity = Vec2.of(context.entity().getVelocityX(), context.entity().getVelocityZ());
        
        return SteeringOutput.linear(desired.subtract(velocity));
    }
}
