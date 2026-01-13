package jurassictale.ai.utility;

import jurassictale.ai.AIContext;

public interface UtilityAction {
    String getName();
    float score(AIContext context);
    void execute(AIContext context);
    default float getBasePriority() { return 1.0f; }
}
