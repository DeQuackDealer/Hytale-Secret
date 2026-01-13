package jurassictale.ai.steering.behaviors;

import jurassictale.ai.AIContext;
import jurassictale.ai.steering.*;

public class PursuitBehavior implements SteeringBehavior {
    
    private final float maxSpeed;
    private final float predictionTime;
    
    public PursuitBehavior(float maxSpeed, float predictionTime) {
        this.maxSpeed = maxSpeed;
        this.predictionTime = predictionTime;
    }
    
    @Override
    public SteeringOutput calculate(AIContext context) {
        if (!context.perception().hasTarget()) return SteeringOutput.ZERO;
        
        Vec2 targetPos = context.perception().getTargetPosition().orElse(Vec2.ZERO);
        Vec2 targetVel = context.perception().getTargetVelocity().orElse(Vec2.ZERO);
        
        Vec2 position = Vec2.of(context.entity().getX(), context.entity().getZ());
        double distance = position.distanceTo(targetPos);
        
        float lookAhead = (float) Math.min(predictionTime, distance / maxSpeed);
        Vec2 predictedPos = targetPos.add(targetVel.multiply(lookAhead));
        
        Vec2 desired = predictedPos.subtract(position).normalize().multiply(maxSpeed);
        Vec2 velocity = Vec2.of(context.entity().getVelocityX(), context.entity().getVelocityZ());
        
        return SteeringOutput.linear(desired.subtract(velocity));
    }
}
