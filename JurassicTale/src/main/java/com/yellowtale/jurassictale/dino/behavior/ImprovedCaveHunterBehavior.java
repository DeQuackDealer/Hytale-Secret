package com.yellowtale.jurassictale.dino.behavior;

import com.yellowtale.jurassictale.ai.actions.*;

public class ImprovedCaveHunterBehavior extends UtilityBehaviorController {
    
    private static final float MAX_SPEED = 11.0f;
    private static final float ATTACK_RANGE = 2.5f;
    private static final float AMBUSH_RANGE = 15.0f;
    private static final float STRIKE_RANGE = 5.0f;
    private static final long ATTACK_COOLDOWN = 1200;
    private static final float SEPARATION_RADIUS = 2.5f;
    
    @Override
    protected void configureActions() {
        selector.addAction(new IdleAction());
        selector.addAction(new WanderAction(MAX_SPEED * 0.3f));
        selector.addAction(new AmbushAction(AMBUSH_RANGE, STRIKE_RANGE));
        selector.addAction(new ChaseAction(MAX_SPEED, ATTACK_RANGE));
        selector.addAction(new AttackAction(ATTACK_RANGE, ATTACK_COOLDOWN));
        selector.addAction(new FlockAction(MAX_SPEED * 0.5f, SEPARATION_RADIUS));
    }
    
    @Override
    public String getBehaviorName() {
        return "ImprovedCaveHunter";
    }
}
