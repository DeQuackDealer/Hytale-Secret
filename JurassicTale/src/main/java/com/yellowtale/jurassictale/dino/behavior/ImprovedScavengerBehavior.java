package com.yellowtale.jurassictale.dino.behavior;

import com.yellowtale.jurassictale.ai.actions.*;

public class ImprovedScavengerBehavior extends UtilityBehaviorController {
    
    private static final float MAX_SPEED = 10.0f;
    private static final float PANIC_DISTANCE = 20.0f;
    
    @Override
    protected void configureActions() {
        selector.addAction(new IdleAction());
        selector.addAction(new WanderAction(MAX_SPEED * 0.4f));
        selector.addAction(new ScavengeAction(MAX_SPEED * 0.5f));
        selector.addAction(new FleeAction(MAX_SPEED * 1.3f, PANIC_DISTANCE));
    }
    
    @Override
    public String getBehaviorName() {
        return "ImprovedScavenger";
    }
}
