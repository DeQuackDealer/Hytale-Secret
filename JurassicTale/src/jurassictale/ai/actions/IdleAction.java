package jurassictale.ai.actions;

import jurassictale.ai.AIContext;
import jurassictale.ai.utility.UtilityAction;
import jurassictale.dino.DinoEntity.DinoState;

public class IdleAction implements UtilityAction {
    
    @Override
    public String getName() {
        return "Idle";
    }
    
    @Override
    public float score(AIContext context) {
        if (context.isInDanger()) return 0.0f;
        if (context.hasTarget()) return 0.1f;
        
        float base = 0.3f;
        if (context.getHealthPercent() < 0.5) base += 0.2f;
        if (context.getStateTime() < 2000) base += 0.1f;
        
        return base;
    }
    
    @Override
    public void execute(AIContext context) {
        context.entity().setState(DinoState.IDLE);
        context.entity().setCurrentAnimation("idle");
        context.entity().setVelocity(0, 0);
    }
}
