package jurassictale.dino.behavior;

import jurassictale.dino.DinoEntity;
import jurassictale.dino.DinoEntity.DinoState;

public class TerritorialBehavior implements BehaviorController {
    
    @Override
    public void update(BehaviorContext context) {
        DinoEntity entity = context.entity();
        
        switch (entity.getState()) {
            case IDLE -> {
                if (Math.random() < 0.01) {
                    entity.setState(DinoState.WANDERING);
                    entity.setCurrentAnimation("patrol");
                }
            }
            case WANDERING -> {
                entity.setCurrentAnimation("patrol");
                if (entity.getStateTime() > 8000 && Math.random() < 0.03) {
                    entity.setState(DinoState.IDLE);
                }
            }
            case ROARING -> {
                entity.setCurrentAnimation("roar");
                if (entity.getStateTime() > 3000) {
                    entity.setState(DinoState.CHASING);
                }
            }
            case CHASING -> {
                entity.setCurrentAnimation("run");
            }
            case ATTACKING -> {
                entity.setCurrentAnimation("bite");
                if (entity.getStateTime() > 1500) {
                    entity.setState(DinoState.CHASING);
                }
            }
            default -> {}
        }
    }
    
    @Override
    public String getBehaviorName() {
        return "Territorial";
    }
}
