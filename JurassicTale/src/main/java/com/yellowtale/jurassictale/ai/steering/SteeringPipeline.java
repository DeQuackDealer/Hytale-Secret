package com.yellowtale.jurassictale.ai.steering;

import com.yellowtale.jurassictale.ai.AIContext;

import java.util.*;

public class SteeringPipeline {
    
    private final List<WeightedBehavior> behaviors;
    private final float maxForce;
    
    public SteeringPipeline(float maxForce) {
        this.behaviors = new ArrayList<>();
        this.maxForce = maxForce;
    }
    
    public SteeringPipeline addBehavior(SteeringBehavior behavior, float weight) {
        behaviors.add(new WeightedBehavior(behavior, weight));
        return this;
    }
    
    public SteeringOutput calculate(AIContext context) {
        Vec2 totalForce = Vec2.ZERO;
        double totalAngular = 0;
        
        for (WeightedBehavior wb : behaviors) {
            SteeringOutput output = wb.behavior.calculate(context);
            totalForce = totalForce.add(output.linear().multiply(wb.weight));
            totalAngular += output.angular() * wb.weight;
        }
        
        totalForce = totalForce.truncate(maxForce);
        
        return new SteeringOutput(totalForce, totalAngular);
    }
    
    public SteeringOutput calculatePrioritized(AIContext context) {
        Vec2 totalForce = Vec2.ZERO;
        double totalAngular = 0;
        double remainingBudget = maxForce;
        
        for (WeightedBehavior wb : behaviors) {
            if (remainingBudget <= 0) break;
            
            SteeringOutput output = wb.behavior.calculate(context);
            Vec2 force = output.linear().multiply(wb.weight);
            double forceMag = force.length();
            
            if (forceMag > remainingBudget) {
                force = force.normalize().multiply(remainingBudget);
                forceMag = remainingBudget;
            }
            
            totalForce = totalForce.add(force);
            totalAngular += output.angular() * wb.weight;
            remainingBudget -= forceMag;
        }
        
        return new SteeringOutput(totalForce, totalAngular);
    }
    
    public void clear() {
        behaviors.clear();
    }
    
    private record WeightedBehavior(SteeringBehavior behavior, float weight) {}
}
