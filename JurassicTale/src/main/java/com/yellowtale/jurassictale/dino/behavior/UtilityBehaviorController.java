package com.yellowtale.jurassictale.dino.behavior;

import com.yellowtale.jurassictale.ai.AIContext;
import com.yellowtale.jurassictale.ai.perception.PerceptionData;
import com.yellowtale.jurassictale.ai.utility.UtilityAction;
import com.yellowtale.jurassictale.ai.utility.UtilitySelector;
import com.yellowtale.jurassictale.dino.DinoEntity;

import java.util.Optional;

public abstract class UtilityBehaviorController implements BehaviorController {
    
    protected final UtilitySelector selector;
    private UtilityAction currentAction;
    private long lastUpdateTime;
    
    protected UtilityBehaviorController() {
        this.selector = new UtilitySelector(0.05f);
        this.lastUpdateTime = System.currentTimeMillis();
        configureActions();
    }
    
    protected abstract void configureActions();
    
    @Override
    public void update(BehaviorContext context) {
        DinoEntity entity = context.entity();
        long now = System.currentTimeMillis();
        float deltaTime = (now - lastUpdateTime) / 1000.0f;
        lastUpdateTime = now;
        
        PerceptionData perception = context.perception() != null 
            ? context.perception() 
            : new PerceptionData();
        
        AIContext aiContext = new AIContext(entity, perception, deltaTime, now);
        
        Optional<UtilityAction> bestAction = selector.selectBest(aiContext);
        
        if (bestAction.isPresent()) {
            UtilityAction action = bestAction.get();
            if (currentAction == null || !currentAction.getName().equals(action.getName())) {
                currentAction = action;
            }
            currentAction.execute(aiContext);
        }
        
        entity.applyVelocity(deltaTime);
        entity.addHunger(deltaTime * 0.1);
    }
}
