package jurassictale.ai.actions;

import jurassictale.ai.AIContext;
import jurassictale.ai.steering.*;
import jurassictale.ai.steering.behaviors.*;
import jurassictale.ai.utility.UtilityAction;
import jurassictale.dino.DinoEntity.DinoState;

public class FlockAction implements UtilityAction {
    
    private final SteeringPipeline flockPipeline;
    private final float maxSpeed;
    
    public FlockAction(float maxSpeed, float separationRadius) {
        this.maxSpeed = maxSpeed;
        this.flockPipeline = new SteeringPipeline(maxSpeed)
            .addBehavior(new SeparationBehavior(separationRadius, maxSpeed), 1.5f)
            .addBehavior(new CohesionBehavior(maxSpeed), 1.0f)
            .addBehavior(new AlignmentBehavior(maxSpeed), 1.0f)
            .addBehavior(WanderBehavior.gentle(maxSpeed), 0.3f);
    }
    
    @Override
    public String getName() {
        return "Flock";
    }
    
    @Override
    public float score(AIContext context) {
        if (context.isInDanger()) return 0.0f;
        if (!context.perception().hasAllies()) return 0.0f;
        
        int allyCount = context.getNearbyAllyCount();
        if (allyCount < 2) return 0.2f;
        
        float base = 0.5f + Math.min(0.3f, allyCount * 0.05f);
        return base;
    }
    
    @Override
    public void execute(AIContext context) {
        context.entity().setState(DinoState.WANDERING);
        context.entity().setCurrentAnimation("walk");
        
        SteeringOutput steering = flockPipeline.calculate(context);
        Vec2 velocity = steering.linear().limit(maxSpeed * 0.7);
        context.entity().setVelocity(velocity.x(), velocity.z());
    }
}
