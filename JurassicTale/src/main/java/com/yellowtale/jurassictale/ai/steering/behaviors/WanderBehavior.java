package com.yellowtale.jurassictale.ai.steering.behaviors;

import com.yellowtale.jurassictale.ai.AIContext;
import com.yellowtale.jurassictale.ai.steering.*;

import java.util.Random;

public class WanderBehavior implements SteeringBehavior {
    
    private final float circleDistance;
    private final float circleRadius;
    private final float angleChange;
    private final float maxSpeed;
    private final Random random;
    
    private double wanderAngle;
    
    public WanderBehavior(float circleDistance, float circleRadius, float angleChange, float maxSpeed) {
        this.circleDistance = circleDistance;
        this.circleRadius = circleRadius;
        this.angleChange = angleChange;
        this.maxSpeed = maxSpeed;
        this.random = new Random();
        this.wanderAngle = random.nextDouble() * Math.PI * 2;
    }
    
    public static WanderBehavior gentle(float maxSpeed) {
        return new WanderBehavior(2.0f, 1.0f, 0.3f, maxSpeed);
    }
    
    public static WanderBehavior erratic(float maxSpeed) {
        return new WanderBehavior(1.5f, 2.0f, 0.8f, maxSpeed);
    }
    
    @Override
    public SteeringOutput calculate(AIContext context) {
        Vec2 velocity = Vec2.of(context.entity().getVelocityX(), context.entity().getVelocityZ());
        Vec2 heading = velocity.isZero() ? Vec2.fromAngle(context.entity().getYaw()) : velocity.normalize();
        
        Vec2 circleCenter = heading.multiply(circleDistance);
        
        wanderAngle += (random.nextDouble() - 0.5) * angleChange * 2;
        Vec2 displacement = Vec2.fromAngle(wanderAngle).multiply(circleRadius);
        
        Vec2 wanderForce = circleCenter.add(displacement).normalize().multiply(maxSpeed);
        return SteeringOutput.linear(wanderForce.subtract(velocity));
    }
}
