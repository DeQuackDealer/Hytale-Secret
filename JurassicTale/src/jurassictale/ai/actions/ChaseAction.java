package jurassictale.ai.actions;

import jurassictale.ai.AIContext;
import jurassictale.ai.steering.*;
import jurassictale.ai.steering.behaviors.PursuitBehavior;
import jurassictale.ai.utility.UtilityAction;
import jurassictale.dino.DinoEntity.DinoState;

public class ChaseAction implements UtilityAction {
    
    private final PursuitBehavior pursuitBehavior;
    private final float attackRange;
    
    public ChaseAction(float maxSpeed, float attackRange) {
        this.pursuitBehavior = new PursuitBehavior(maxSpeed, 2.0f);
        this.attackRange = attackRange;
    }
    
    @Override
    public String getName() {
        return "Chase";
    }
    
    @Override
    public float score(AIContext context) {
        if (!context.hasTarget()) return 0.0f;
        
        Vec2 targetPos = context.getTargetPosition().orElse(Vec2.ZERO);
        Vec2 pos = Vec2.of(context.entity().getX(), context.entity().getZ());
        double distance = pos.distanceTo(targetPos);
        
        if (distance <= attackRange) return 0.0f;
        
        float base = 0.7f;
        base *= context.getAggressionMultiplier();
        
        if (context.getHunger() > 50) base += 0.2f;
        
        return Math.min(1.0f, base);
    }
    
    @Override
    public void execute(AIContext context) {
        context.entity().setState(DinoState.CHASING);
        context.entity().setCurrentAnimation("run");
        
        SteeringOutput steering = pursuitBehavior.calculate(context);
        float maxSpeed = context.entity().getDefinition().getSpeed();
        Vec2 velocity = steering.linear().limit(maxSpeed);
        context.entity().setVelocity(velocity.x(), velocity.z());
    }
}
