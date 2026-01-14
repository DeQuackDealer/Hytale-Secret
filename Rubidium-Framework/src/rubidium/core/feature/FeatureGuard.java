package rubidium.core.feature;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Circuit breaker pattern for feature protection.
 * Prevents cascading failures by isolating problematic features.
 */
public class FeatureGuard {
    
    private static final Logger logger = Logger.getLogger("Rubidium-FeatureGuard");
    
    private final String featureId;
    private final int failureThreshold;
    private final Duration resetTimeout;
    private final Duration halfOpenTimeout;
    
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicReference<CircuitState> state = new AtomicReference<>(CircuitState.CLOSED);
    private final AtomicReference<Instant> lastFailure = new AtomicReference<>(Instant.MIN);
    private final AtomicReference<Instant> stateChangedAt = new AtomicReference<>(Instant.now());
    
    public FeatureGuard(String featureId) {
        this(featureId, 5, Duration.ofMinutes(1), Duration.ofSeconds(30));
    }
    
    public FeatureGuard(String featureId, int failureThreshold, Duration resetTimeout, Duration halfOpenTimeout) {
        this.featureId = featureId;
        this.failureThreshold = failureThreshold;
        this.resetTimeout = resetTimeout;
        this.halfOpenTimeout = halfOpenTimeout;
    }
    
    public <T> T execute(Supplier<T> action, Supplier<T> fallback) {
        if (!allowRequest()) {
            logger.fine("Circuit OPEN for " + featureId + ", using fallback");
            return fallback.get();
        }
        
        CircuitState currentState = state.get();
        
        if (currentState == CircuitState.HALF_OPEN) {
            synchronized (this) {
                if (state.get() == CircuitState.HALF_OPEN) {
                    try {
                        T result = executeWithTimeout(action);
                        recordSuccess();
                        return result;
                    } catch (Exception e) {
                        recordFailure(e);
                        logger.warning("Half-open trial failed for " + featureId + ": " + e.getMessage());
                        return fallback.get();
                    }
                }
            }
        }
        
        try {
            T result = action.get();
            recordSuccess();
            return result;
        } catch (Exception e) {
            recordFailure(e);
            logger.warning("Feature " + featureId + " failed: " + e.getMessage());
            return fallback.get();
        }
    }
    
    private <T> T executeWithTimeout(Supplier<T> action) throws Exception {
        java.util.concurrent.FutureTask<T> task = new java.util.concurrent.FutureTask<>(action::get);
        Thread t = new Thread(task, "CircuitBreaker-Trial-" + featureId);
        t.setDaemon(true);
        t.start();
        
        try {
            return task.get(halfOpenTimeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            task.cancel(true);
            throw new RuntimeException("Operation timed out during half-open trial");
        } catch (java.util.concurrent.ExecutionException e) {
            throw (Exception) e.getCause();
        }
    }
    
    public void execute(Runnable action, Runnable fallback) {
        execute(() -> { action.run(); return null; }, () -> { fallback.run(); return null; });
    }
    
    public boolean allowRequest() {
        CircuitState currentState = state.get();
        
        switch (currentState) {
            case CLOSED -> {
                return true;
            }
            case OPEN -> {
                if (Duration.between(stateChangedAt.get(), Instant.now()).compareTo(resetTimeout) > 0) {
                    if (state.compareAndSet(CircuitState.OPEN, CircuitState.HALF_OPEN)) {
                        stateChangedAt.set(Instant.now());
                        logger.info("Circuit for " + featureId + " entering HALF_OPEN state");
                    }
                    return true;
                }
                return false;
            }
            case HALF_OPEN -> {
                return true;
            }
            default -> {
                return false;
            }
        }
    }
    
    public void recordSuccess() {
        failureCount.set(0);
        successCount.incrementAndGet();
        
        if (state.get() == CircuitState.HALF_OPEN) {
            if (successCount.get() >= 3) {
                state.set(CircuitState.CLOSED);
                stateChangedAt.set(Instant.now());
                successCount.set(0);
                logger.info("Circuit for " + featureId + " CLOSED (recovered)");
            }
        }
    }
    
    public void recordFailure(Exception e) {
        lastFailure.set(Instant.now());
        int failures = failureCount.incrementAndGet();
        successCount.set(0);
        
        if (state.get() == CircuitState.HALF_OPEN) {
            state.set(CircuitState.OPEN);
            stateChangedAt.set(Instant.now());
            logger.warning("Circuit for " + featureId + " re-OPENED after HALF_OPEN failure");
        } else if (failures >= failureThreshold && state.get() == CircuitState.CLOSED) {
            state.set(CircuitState.OPEN);
            stateChangedAt.set(Instant.now());
            logger.warning("Circuit for " + featureId + " OPENED after " + failures + " failures");
        }
    }
    
    public void reset() {
        failureCount.set(0);
        successCount.set(0);
        state.set(CircuitState.CLOSED);
        stateChangedAt.set(Instant.now());
    }
    
    public void forceOpen() {
        state.set(CircuitState.OPEN);
        stateChangedAt.set(Instant.now());
    }
    
    public CircuitState getState() {
        return state.get();
    }
    
    public int getFailureCount() {
        return failureCount.get();
    }
    
    public Instant getLastFailure() {
        return lastFailure.get();
    }
    
    public enum CircuitState {
        CLOSED,
        OPEN,
        HALF_OPEN
    }
}
