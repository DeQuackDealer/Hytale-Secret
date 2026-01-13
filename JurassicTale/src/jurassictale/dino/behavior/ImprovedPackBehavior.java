package jurassictale.dino.behavior;

import jurassictale.ai.actions.*;

public class ImprovedPackBehavior extends UtilityBehaviorController {
    
    private static final float MAX_SPEED = 12.0f;
    private static final float ATTACK_RANGE = 3.0f;
    private static final float ATTACK_COOLDOWN = 1500;
    private static final float SEPARATION_RADIUS = 3.0f;
    
    @Override
    protected void configureActions() {
        selector.addAction(new IdleAction());
        selector.addAction(new WanderAction(MAX_SPEED * 0.4f));
        selector.addAction(new ChaseAction(MAX_SPEED, ATTACK_RANGE));
        selector.addAction(new AttackAction(ATTACK_RANGE, (long) ATTACK_COOLDOWN));
        selector.addAction(new FlockAction(MAX_SPEED * 0.6f, SEPARATION_RADIUS));
    }
    
    @Override
    public String getBehaviorName() {
        return "ImprovedPack";
    }
}
