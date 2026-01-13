package jurassictale.dino.behavior;

import jurassictale.dino.DinoEntity;
import jurassictale.dino.DinoEntity.DinoState;

public class ScavengerBehavior implements BehaviorController {
    
    @Override
    public void update(BehaviorContext context) {
        DinoEntity entity = context.entity();
        
        switch (entity.getState()) {
            case IDLE -> {
                entity.setCurrentAnimation("idle_look");
                if (Math.random() < 0.02) {
                    entity.setState(DinoState.WANDERING);
                }
            }
            case WANDERING -> {
                entity.setCurrentAnimation("fly");
            }
            case EATING -> {
                entity.setCurrentAnimation("eat");
                if (entity.getStateTime() > 5000) {
                    entity.setState(DinoState.IDLE);
                }
            }
            case ATTACKING -> {
                entity.setCurrentAnimation("dive_attack");
                if (entity.getStateTime() > 1000) {
                    entity.setState(DinoState.WANDERING);
                }
            }
            default -> {}
        }
    }
    
    @Override
    public String getBehaviorName() {
        return "Scavenger";
    }
}
