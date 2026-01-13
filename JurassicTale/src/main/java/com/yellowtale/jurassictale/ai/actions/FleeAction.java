package com.yellowtale.jurassictale.ai.actions;

import com.yellowtale.jurassictale.ai.AIContext;
import com.yellowtale.jurassictale.ai.steering.*;
import com.yellowtale.jurassictale.ai.steering.behaviors.EvadeBehavior;
import com.yellowtale.jurassictale.ai.utility.UtilityAction;
import com.yellowtale.jurassictale.dino.DinoEntity.DinoState;

public class FleeAction implements UtilityAction {
    
    private final EvadeBehavior evadeBehavior;
    private final float panicDistance;
    
    public FleeAction(float maxSpeed, float panicDistance) {
        this.evadeBehavior = new EvadeBehavior(maxSpeed, 1.0f, panicDistance);
        this.panicDistance = panicDistance;
    }
    
    @Override
    public String getName() {
        return "Flee";
    }
    
    @Override
    public float getBasePriority() {
        return 1.5f;
    }
    
    @Override
    public float score(AIContext context) {
        if (!context.isInDanger()) return 0.0f;
        
        double threatDist = context.getDistanceToNearestThreat();
        if (threatDist > panicDistance) return 0.0f;
        
        float urgency = (float) (1.0 - threatDist / panicDistance);
        float base = 0.6f + urgency * 0.4f;
        
        if (context.getHealthPercent() < 0.3) base += 0.2f;
        
        return Math.min(1.0f, base);
    }
    
    @Override
    public void execute(AIContext context) {
        context.entity().setState(DinoState.FLEEING);
        context.entity().setCurrentAnimation("run");
        
        SteeringOutput steering = evadeBehavior.calculate(context);
        float maxSpeed = context.entity().getDefinition().getSpeed();
        Vec2 velocity = steering.linear().limit(maxSpeed * 1.2);
        context.entity().setVelocity(velocity.x(), velocity.z());
    }
}
