package com.yellowtale.jurassictale.ai.actions;

import com.yellowtale.jurassictale.ai.AIContext;
import com.yellowtale.jurassictale.ai.steering.*;
import com.yellowtale.jurassictale.ai.steering.behaviors.WanderBehavior;
import com.yellowtale.jurassictale.ai.utility.UtilityAction;
import com.yellowtale.jurassictale.dino.DinoEntity.DinoState;

public class PatrolAction implements UtilityAction {
    
    private final WanderBehavior patrolBehavior;
    private final float maxSpeed;
    
    public PatrolAction(float maxSpeed) {
        this.maxSpeed = maxSpeed;
        this.patrolBehavior = new WanderBehavior(3.0f, 1.5f, 0.2f, maxSpeed);
    }
    
    @Override
    public String getName() {
        return "Patrol";
    }
    
    @Override
    public float score(AIContext context) {
        if (context.isInDanger()) return 0.0f;
        if (context.hasTarget()) return 0.1f;
        
        float base = 0.5f;
        if (context.entity().getState() == DinoState.IDLE && context.getStateTime() > 5000) {
            base += 0.3f;
        }
        
        return base;
    }
    
    @Override
    public void execute(AIContext context) {
        context.entity().setState(DinoState.WANDERING);
        context.entity().setCurrentAnimation("patrol");
        
        SteeringOutput steering = patrolBehavior.calculate(context);
        Vec2 velocity = steering.linear().limit(maxSpeed * 0.6);
        context.entity().setVelocity(velocity.x(), velocity.z());
    }
}
