package jurassictale.dino.behavior;

import jurassictale.dino.DinoEntity;
import jurassictale.dino.DinoEntity.DinoState;

public class AmbushBehavior implements BehaviorController {
    
    @Override
    public void update(BehaviorContext context) {
        DinoEntity entity = context.entity();
        
        switch (entity.getState()) {
            case IDLE -> {
                entity.setCurrentAnimation("crouch_idle");
            }
            case CHASING -> {
                entity.setCurrentAnimation("sprint");
                if (entity.getStateTime() > 5000) {
                    entity.setState(DinoState.IDLE);
                }
            }
            case ATTACKING -> {
                entity.setCurrentAnimation("pounce");
                if (entity.getStateTime() > 800) {
                    entity.setState(DinoState.IDLE);
                }
            }
            default -> {}
        }
    }
    
    @Override
    public String getBehaviorName() {
        return "Ambush";
    }
}
