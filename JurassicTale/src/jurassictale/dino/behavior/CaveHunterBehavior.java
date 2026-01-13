package jurassictale.dino.behavior;

import jurassictale.dino.DinoEntity;
import jurassictale.dino.DinoEntity.DinoState;

public class CaveHunterBehavior implements BehaviorController {
    
    @Override
    public void update(BehaviorContext context) {
        DinoEntity entity = context.entity();
        
        switch (entity.getState()) {
            case IDLE -> {
                entity.setCurrentAnimation("crouch");
                if (Math.random() < 0.01) {
                    entity.setState(DinoState.WANDERING);
                }
            }
            case WANDERING -> {
                entity.setCurrentAnimation("stalk");
            }
            case CHASING -> {
                entity.setCurrentAnimation("sprint");
            }
            case ATTACKING -> {
                entity.setCurrentAnimation("slash");
                if (entity.getStateTime() > 600) {
                    entity.setState(DinoState.CHASING);
                }
            }
            default -> {}
        }
    }
    
    @Override
    public String getBehaviorName() {
        return "Cave Hunter";
    }
}
