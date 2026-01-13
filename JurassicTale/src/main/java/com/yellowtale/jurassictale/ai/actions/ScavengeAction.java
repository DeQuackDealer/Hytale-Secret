package com.yellowtale.jurassictale.ai.actions;

import com.yellowtale.jurassictale.ai.AIContext;
import com.yellowtale.jurassictale.ai.steering.*;
import com.yellowtale.jurassictale.ai.steering.behaviors.SeekBehavior;
import com.yellowtale.jurassictale.ai.utility.UtilityAction;
import com.yellowtale.jurassictale.dino.DinoEntity.DinoState;

public class ScavengeAction implements UtilityAction {
    
    private final float maxSpeed;
    
    public ScavengeAction(float maxSpeed) {
        this.maxSpeed = maxSpeed;
    }
    
    @Override
    public String getName() {
        return "Scavenge";
    }
    
    @Override
    public float score(AIContext context) {
        float hunger = (float) context.getHunger();
        if (hunger < 30) return 0.0f;
        
        float base = 0.3f + hunger / 200.0f;
        if (context.isInDanger()) base *= 0.3f;
        
        return Math.min(0.8f, base);
    }
    
    @Override
    public void execute(AIContext context) {
        context.entity().setState(DinoState.WANDERING);
        context.entity().setCurrentAnimation("walk_sniff");
        
        SteeringBehavior seek = SeekBehavior.toTarget(maxSpeed);
        SteeringOutput steering = seek.calculate(context);
        Vec2 velocity = steering.linear().limit(maxSpeed * 0.5);
        context.entity().setVelocity(velocity.x(), velocity.z());
    }
}
