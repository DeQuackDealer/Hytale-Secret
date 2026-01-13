package jurassictale.ai.actions;

import jurassictale.ai.AIContext;
import jurassictale.ai.steering.Vec2;
import jurassictale.ai.utility.UtilityAction;
import jurassictale.dino.DinoEntity.DinoState;

public class DefendAction implements UtilityAction {
    
    private final float defendRange;
    
    public DefendAction(float defendRange) {
        this.defendRange = defendRange;
    }
    
    @Override
    public String getName() {
        return "Defend";
    }
    
    @Override
    public float score(AIContext context) {
        if (!context.isInDanger()) return 0.0f;
        
        double threatDist = context.getDistanceToNearestThreat();
        if (threatDist > defendRange) return 0.0f;
        
        int allyCount = context.getNearbyAllyCount();
        float base = 0.4f + allyCount * 0.1f;
        
        if (context.getHealthPercent() > 0.7) base += 0.2f;
        
        return Math.min(1.0f, base);
    }
    
    @Override
    public void execute(AIContext context) {
        context.entity().setState(DinoState.DEFENDING);
        context.entity().setCurrentAnimation("defend");
        
        Vec2 threatPos = context.perception().getNearestThreatPosition().orElse(Vec2.ZERO);
        Vec2 pos = Vec2.of(context.entity().getX(), context.entity().getZ());
        Vec2 facing = threatPos.subtract(pos).normalize();
        
        if (!facing.isZero()) {
            context.entity().setYaw((float) facing.angle());
        }
        context.entity().setVelocity(0, 0);
    }
}
