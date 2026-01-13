package jurassictale.dino.behavior;

import jurassictale.dino.DinoEntity;
import jurassictale.dino.DinoEntity.DinoState;

public class HerdBehavior implements BehaviorController {
    
    @Override
    public void update(BehaviorContext context) {
        DinoEntity entity = context.entity();
        
        switch (entity.getState()) {
            case IDLE -> {
                if (Math.random() < 0.02) {
                    entity.setState(DinoState.WANDERING);
                    entity.setCurrentAnimation("walk");
                }
            }
            case WANDERING -> {
                if (entity.getStateTime() > 5000 && Math.random() < 0.05) {
                    entity.setState(DinoState.IDLE);
                    entity.setCurrentAnimation("idle");
                }
            }
            case FLEEING -> {
                entity.setCurrentAnimation("run");
                if (entity.getStateTime() > 10000) {
                    entity.setState(DinoState.IDLE);
                }
            }
            case DEFENDING -> {
                entity.setCurrentAnimation("defend");
            }
            default -> {}
        }
    }
    
    @Override
    public String getBehaviorName() {
        return "Herd";
    }
}
