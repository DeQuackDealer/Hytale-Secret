package com.yellowtale.jurassictale.dino.behavior;

import com.yellowtale.jurassictale.ai.actions.*;

public class ImprovedHerdBehavior extends UtilityBehaviorController {
    
    private static final float MAX_SPEED = 8.0f;
    private static final float PANIC_DISTANCE = 30.0f;
    private static final float SEPARATION_RADIUS = 4.0f;
    private static final float DEFEND_RANGE = 15.0f;
    
    @Override
    protected void configureActions() {
        selector.addAction(new IdleAction());
        selector.addAction(new WanderAction(MAX_SPEED * 0.5f));
        selector.addAction(new FleeAction(MAX_SPEED * 1.2f, PANIC_DISTANCE));
        selector.addAction(new FlockAction(MAX_SPEED * 0.7f, SEPARATION_RADIUS));
        selector.addAction(new DefendAction(DEFEND_RANGE));
    }
    
    @Override
    public String getBehaviorName() {
        return "ImprovedHerd";
    }
}
