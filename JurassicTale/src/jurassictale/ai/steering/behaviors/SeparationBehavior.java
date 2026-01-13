package jurassictale.ai.steering.behaviors;

import jurassictale.ai.AIContext;
import jurassictale.ai.steering.*;

import java.util.List;

public class SeparationBehavior implements SteeringBehavior {
    
    private final float separationRadius;
    private final float maxForce;
    
    public SeparationBehavior(float separationRadius, float maxForce) {
        this.separationRadius = separationRadius;
        this.maxForce = maxForce;
    }
    
    @Override
    public SteeringOutput calculate(AIContext context) {
        List<Vec2> neighbors = context.perception().getNearbyAllyPositions();
        if (neighbors.isEmpty()) return SteeringOutput.ZERO;
        
        Vec2 position = Vec2.of(context.entity().getX(), context.entity().getZ());
        Vec2 steering = Vec2.ZERO;
        int count = 0;
        
        for (Vec2 neighbor : neighbors) {
            double distance = position.distanceTo(neighbor);
            if (distance > 0 && distance < separationRadius) {
                Vec2 diff = position.subtract(neighbor).normalize();
                diff = diff.divide(distance);
                steering = steering.add(diff);
                count++;
            }
        }
        
        if (count > 0) {
            steering = steering.divide(count);
            if (!steering.isZero()) {
                steering = steering.normalize().multiply(maxForce);
            }
        }
        
        return SteeringOutput.linear(steering);
    }
}
