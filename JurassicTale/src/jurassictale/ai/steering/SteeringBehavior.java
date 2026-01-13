package jurassictale.ai.steering;

import jurassictale.ai.AIContext;

public interface SteeringBehavior {
    SteeringOutput calculate(AIContext context);
    default float getWeight() { return 1.0f; }
}
