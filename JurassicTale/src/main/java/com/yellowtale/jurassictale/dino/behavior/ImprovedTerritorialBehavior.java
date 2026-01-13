package com.yellowtale.jurassictale.dino.behavior;

import com.yellowtale.jurassictale.ai.actions.*;

public class ImprovedTerritorialBehavior extends UtilityBehaviorController {
    
    private static final float MAX_SPEED = 10.0f;
    private static final float TERRITORY_RADIUS = 40.0f;
    private static final float ATTACK_RANGE = 4.0f;
    private static final long ROAR_COOLDOWN = 10000;
    private static final long ATTACK_COOLDOWN = 2000;
    
    @Override
    protected void configureActions() {
        selector.addAction(new IdleAction());
        selector.addAction(new PatrolAction(MAX_SPEED * 0.5f));
        selector.addAction(new RoarAction(TERRITORY_RADIUS, ROAR_COOLDOWN));
        selector.addAction(new ChaseAction(MAX_SPEED, ATTACK_RANGE));
        selector.addAction(new AttackAction(ATTACK_RANGE, ATTACK_COOLDOWN));
    }
    
    @Override
    public String getBehaviorName() {
        return "ImprovedTerritorial";
    }
}
