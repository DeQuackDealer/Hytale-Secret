package com.yellowtale.jurassictale.ai.utility;

import com.yellowtale.jurassictale.ai.AIContext;

@FunctionalInterface
public interface UtilityScorer {
    float score(AIContext context);
    
    static UtilityScorer constant(float value) {
        return ctx -> value;
    }
    
    static UtilityScorer linear(float min, float max, java.util.function.ToDoubleFunction<AIContext> valueGetter) {
        return ctx -> {
            double value = valueGetter.applyAsDouble(ctx);
            return (float) Math.max(0, Math.min(1, (value - min) / (max - min)));
        };
    }
    
    static UtilityScorer inverse(float min, float max, java.util.function.ToDoubleFunction<AIContext> valueGetter) {
        return ctx -> {
            double value = valueGetter.applyAsDouble(ctx);
            return 1.0f - (float) Math.max(0, Math.min(1, (value - min) / (max - min)));
        };
    }
    
    static UtilityScorer exponential(float base, java.util.function.ToDoubleFunction<AIContext> valueGetter) {
        return ctx -> (float) Math.pow(base, valueGetter.applyAsDouble(ctx));
    }
    
    static UtilityScorer threshold(float threshold, java.util.function.ToDoubleFunction<AIContext> valueGetter) {
        return ctx -> valueGetter.applyAsDouble(ctx) >= threshold ? 1.0f : 0.0f;
    }
    
    default UtilityScorer multiply(UtilityScorer other) {
        return ctx -> this.score(ctx) * other.score(ctx);
    }
    
    default UtilityScorer add(UtilityScorer other) {
        return ctx -> Math.min(1.0f, this.score(ctx) + other.score(ctx));
    }
    
    default UtilityScorer clamp() {
        return ctx -> Math.max(0, Math.min(1, this.score(ctx)));
    }
}
