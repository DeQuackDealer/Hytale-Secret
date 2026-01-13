package jurassictale.ai.steering.behaviors;

import jurassictale.ai.AIContext;
import jurassictale.ai.steering.*;

import java.util.function.Function;

public class ArriveBehavior implements SteeringBehavior {
    
    private final Function<AIContext, Vec2> targetProvider;
    private final float maxSpeed;
    private final float slowingRadius;
    private final float arrivalRadius;
    
    public ArriveBehavior(Function<AIContext, Vec2> targetProvider, float maxSpeed, 
                          float slowingRadius, float arrivalRadius) {
        this.targetProvider = targetProvider;
        this.maxSpeed = maxSpeed;
        this.slowingRadius = slowingRadius;
        this.arrivalRadius = arrivalRadius;
    }
    
    public static ArriveBehavior atTarget(float maxSpeed, float slowingRadius) {
        return new ArriveBehavior(
            ctx -> ctx.getTargetPosition().orElse(Vec2.ZERO),
            maxSpeed, slowingRadius, 0.5f
        );
    }
    
    @Override
    public SteeringOutput calculate(AIContext context) {
        Vec2 target = targetProvider.apply(context);
        if (target.isZero()) return SteeringOutput.ZERO;
        
        Vec2 position = Vec2.of(context.entity().getX(), context.entity().getZ());
        Vec2 toTarget = target.subtract(position);
        double distance = toTarget.length();
        
        if (distance < arrivalRadius) return SteeringOutput.ZERO;
        
        float targetSpeed;
        if (distance < slowingRadius) {
            targetSpeed = maxSpeed * (float)(distance / slowingRadius);
        } else {
            targetSpeed = maxSpeed;
        }
        
        Vec2 desired = toTarget.normalize().multiply(targetSpeed);
        Vec2 velocity = Vec2.of(context.entity().getVelocityX(), context.entity().getVelocityZ());
        
        return SteeringOutput.linear(desired.subtract(velocity));
    }
}
