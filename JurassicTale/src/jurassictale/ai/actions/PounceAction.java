package jurassictale.ai.actions;

import jurassictale.ai.AIContext;
import jurassictale.ai.steering.Vec2;
import jurassictale.ai.utility.UtilityAction;
import jurassictale.dino.DinoEntity.DinoState;

public class PounceAction implements UtilityAction {
    
    private final float strikeRange;
    private final float pounceSpeed;
    
    public PounceAction(float strikeRange, float pounceSpeed) {
        this.strikeRange = strikeRange;
        this.pounceSpeed = pounceSpeed;
    }
    
    @Override
    public String getName() {
        return "Pounce";
    }
    
    @Override
    public float getBasePriority() {
        return 1.3f;
    }
    
    @Override
    public float score(AIContext context) {
        if (!context.hasTarget()) return 0.0f;
        
        Vec2 targetPos = context.getTargetPosition().orElse(Vec2.ZERO);
        Vec2 pos = Vec2.of(context.entity().getX(), context.entity().getZ());
        double distance = pos.distanceTo(targetPos);
        
        if (distance > strikeRange) return 0.0f;
        
        float base = 0.95f;
        base *= context.getAggressionMultiplier();
        
        return base;
    }
    
    @Override
    public void execute(AIContext context) {
        context.entity().setState(DinoState.ATTACKING);
        context.entity().setCurrentAnimation("pounce");
        
        Vec2 targetPos = context.getTargetPosition().orElse(Vec2.ZERO);
        Vec2 pos = Vec2.of(context.entity().getX(), context.entity().getZ());
        Vec2 direction = targetPos.subtract(pos).normalize();
        
        context.entity().setVelocity(direction.x() * pounceSpeed, direction.z() * pounceSpeed);
    }
}
