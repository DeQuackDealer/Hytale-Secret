package com.yellowtale.jurassictale.ai.utility;

import com.yellowtale.jurassictale.ai.AIContext;

public interface UtilityAction {
    String getName();
    float score(AIContext context);
    void execute(AIContext context);
    default float getBasePriority() { return 1.0f; }
}
