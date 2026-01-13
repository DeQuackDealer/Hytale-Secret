package jurassictale.dino.behavior;

import jurassictale.dino.DinoEntity;
import jurassictale.dino.DinoEntity.DinoState;

public class PackBehavior implements BehaviorController {
    
    @Override
    public void update(BehaviorContext context) {
        DinoEntity entity = context.entity();
        double aggressionMultiplier = entity.getAggressionMultiplier();
        
        switch (entity.getState()) {
            case IDLE -> {
                if (Math.random() < 0.03 * aggressionMultiplier) {
                    entity.setState(DinoState.WANDERING);
                    entity.setCurrentAnimation("walk");
                }
            }
            case WANDERING -> {
                if (entity.getStateTime() > 3000 && Math.random() < 0.05) {
                    entity.setState(DinoState.IDLE);
                    entity.setCurrentAnimation("idle");
                }
            }
            case CHASING -> {
                entity.setCurrentAnimation("run");
            }
            case ATTACKING -> {
                entity.setCurrentAnimation("attack");
                if (entity.getStateTime() > 1000) {
                    entity.setState(DinoState.CHASING);
                }
            }
            default -> {}
        }
    }
    
    @Override
    public String getBehaviorName() {
        return "Pack";
    }
}
