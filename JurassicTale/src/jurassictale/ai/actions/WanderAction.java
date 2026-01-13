package jurassictale.ai.actions;

import jurassictale.ai.AIContext;
import jurassictale.ai.steering.*;
import jurassictale.ai.steering.behaviors.WanderBehavior;
import jurassictale.ai.utility.UtilityAction;
import jurassictale.dino.DinoEntity.DinoState;

public class WanderAction implements UtilityAction {
    
    private final WanderBehavior wanderBehavior;
    
    public WanderAction(float maxSpeed) {
        this.wanderBehavior = WanderBehavior.gentle(maxSpeed);
    }
    
    @Override
    public String getName() {
        return "Wander";
    }
    
    @Override
    public float score(AIContext context) {
        if (context.isInDanger()) return 0.0f;
        if (context.hasTarget()) return 0.1f;
        
        float base = 0.4f;
        if (context.getStateTime() > 3000 && context.entity().getState() == DinoState.IDLE) {
            base += 0.3f;
        }
        
        return base;
    }
    
    @Override
    public void execute(AIContext context) {
        context.entity().setState(DinoState.WANDERING);
        context.entity().setCurrentAnimation("walk");
        
        SteeringOutput steering = wanderBehavior.calculate(context);
        Vec2 velocity = steering.linear().limit(context.entity().getDefinition().getSpeed() * 0.5);
        context.entity().setVelocity(velocity.x(), velocity.z());
    }
}
