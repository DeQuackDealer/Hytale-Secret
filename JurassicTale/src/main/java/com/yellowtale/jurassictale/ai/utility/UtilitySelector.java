package com.yellowtale.jurassictale.ai.utility;

import com.yellowtale.jurassictale.ai.AIContext;
import java.util.*;

public class UtilitySelector {
    
    private final List<UtilityAction> actions;
    private final float noiseAmount;
    private final Random random;
    
    public UtilitySelector() {
        this(0.05f);
    }
    
    public UtilitySelector(float noiseAmount) {
        this.actions = new ArrayList<>();
        this.noiseAmount = noiseAmount;
        this.random = new Random();
    }
    
    public void addAction(UtilityAction action) {
        actions.add(action);
    }
    
    public Optional<UtilityAction> selectBest(AIContext context) {
        if (actions.isEmpty()) return Optional.empty();
        
        UtilityAction best = null;
        float bestScore = Float.NEGATIVE_INFINITY;
        
        for (UtilityAction action : actions) {
            float score = action.score(context) * action.getBasePriority();
            score += (random.nextFloat() - 0.5f) * noiseAmount;
            
            if (score > bestScore) {
                bestScore = score;
                best = action;
            }
        }
        
        return bestScore > 0 ? Optional.ofNullable(best) : Optional.empty();
    }
    
    public List<ScoredAction> scoreAll(AIContext context) {
        List<ScoredAction> scored = new ArrayList<>();
        for (UtilityAction action : actions) {
            float score = action.score(context) * action.getBasePriority();
            scored.add(new ScoredAction(action, score));
        }
        scored.sort((a, b) -> Float.compare(b.score(), a.score()));
        return scored;
    }
    
    public record ScoredAction(UtilityAction action, float score) {}
}
