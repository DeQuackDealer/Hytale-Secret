package jurassictale.ai.actions;

import jurassictale.ai.AIContext;
import jurassictale.ai.steering.Vec2;
import jurassictale.ai.utility.UtilityAction;
import jurassictale.dino.DinoEntity.DinoState;

public class AttackAction implements UtilityAction {
    
    private final float attackRange;
    private final long attackCooldown;
    
    public AttackAction(float attackRange, long attackCooldown) {
        this.attackRange = attackRange;
        this.attackCooldown = attackCooldown;
    }
    
    @Override
    public String getName() {
        return "Attack";
    }
    
    @Override
    public float getBasePriority() {
        return 1.2f;
    }
    
    @Override
    public float score(AIContext context) {
        if (!context.hasTarget()) return 0.0f;
        
        Vec2 targetPos = context.getTargetPosition().orElse(Vec2.ZERO);
        Vec2 pos = Vec2.of(context.entity().getX(), context.entity().getZ());
        double distance = pos.distanceTo(targetPos);
        
        if (distance > attackRange) return 0.0f;
        
        if (context.entity().getLastAttackTime() + attackCooldown > context.currentTime()) {
            return 0.0f;
        }
        
        float base = 0.9f;
        base *= context.getAggressionMultiplier();
        
        return Math.min(1.0f, base);
    }
    
    @Override
    public void execute(AIContext context) {
        context.entity().setState(DinoState.ATTACKING);
        context.entity().setCurrentAnimation("attack");
        context.entity().setVelocity(0, 0);
        context.entity().setLastAttackTime(context.currentTime());
    }
}
