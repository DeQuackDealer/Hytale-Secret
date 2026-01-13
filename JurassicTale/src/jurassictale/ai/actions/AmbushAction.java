package jurassictale.ai.actions;

import jurassictale.ai.AIContext;
import jurassictale.ai.steering.Vec2;
import jurassictale.ai.utility.UtilityAction;
import jurassictale.dino.DinoEntity.DinoState;

public class AmbushAction implements UtilityAction {
    
    private final float ambushRange;
    private final float strikeRange;
    
    public AmbushAction(float ambushRange, float strikeRange) {
        this.ambushRange = ambushRange;
        this.strikeRange = strikeRange;
    }
    
    @Override
    public String getName() {
        return "Ambush";
    }
    
    @Override
    public float score(AIContext context) {
        if (!context.perception().hasPrey()) return 0.0f;
        
        var nearestPrey = context.perception().getNearestPrey();
        if (nearestPrey.isEmpty()) return 0.0f;
        
        double distance = nearestPrey.get().distance();
        
        if (distance > ambushRange || distance < strikeRange) return 0.0f;
        
        float base = 0.6f;
        if (context.entity().getState() == DinoState.IDLE) base += 0.2f;
        
        return base;
    }
    
    @Override
    public void execute(AIContext context) {
        context.entity().setState(DinoState.IDLE);
        context.entity().setCurrentAnimation("crouch_idle");
        context.entity().setVelocity(0, 0);
    }
}
