package com.yellowtale.jurassictale.dino.behavior;

import com.yellowtale.jurassictale.ai.actions.*;

public class ImprovedAmbushBehavior extends UtilityBehaviorController {
    
    private static final float MAX_SPEED = 15.0f;
    private static final float AMBUSH_RANGE = 25.0f;
    private static final float STRIKE_RANGE = 8.0f;
    private static final float POUNCE_SPEED = 20.0f;
    
    @Override
    protected void configureActions() {
        selector.addAction(new IdleAction());
        selector.addAction(new WanderAction(MAX_SPEED * 0.3f));
        selector.addAction(new AmbushAction(AMBUSH_RANGE, STRIKE_RANGE));
        selector.addAction(new PounceAction(STRIKE_RANGE, POUNCE_SPEED));
        selector.addAction(new ChaseAction(MAX_SPEED, STRIKE_RANGE * 0.5f));
    }
    
    @Override
    public String getBehaviorName() {
        return "ImprovedAmbush";
    }
}
