package com.yellowtale.jurassictale.ai.actions;

import com.yellowtale.jurassictale.ai.AIContext;
import com.yellowtale.jurassictale.ai.utility.UtilityAction;
import com.yellowtale.jurassictale.dino.DinoEntity.DinoState;

public class RoarAction implements UtilityAction {
    
    private final float territoryRadius;
    private final long roarCooldown;
    
    public RoarAction(float territoryRadius, long roarCooldown) {
        this.territoryRadius = territoryRadius;
        this.roarCooldown = roarCooldown;
    }
    
    @Override
    public String getName() {
        return "Roar";
    }
    
    @Override
    public float score(AIContext context) {
        if (!context.perception().hasThreats()) return 0.0f;
        
        double threatDist = context.getDistanceToNearestThreat();
        if (threatDist > territoryRadius) return 0.0f;
        
        if (context.entity().getLastRoarTime() + roarCooldown > context.currentTime()) {
            return 0.0f;
        }
        
        float base = 0.5f;
        if (threatDist < territoryRadius * 0.5) base += 0.3f;
        
        return base;
    }
    
    @Override
    public void execute(AIContext context) {
        context.entity().setState(DinoState.ROARING);
        context.entity().setCurrentAnimation("roar");
        context.entity().setVelocity(0, 0);
        context.entity().setLastRoarTime(context.currentTime());
    }
}
