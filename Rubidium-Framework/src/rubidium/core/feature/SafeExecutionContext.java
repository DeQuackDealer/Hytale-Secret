package rubidium.core.feature;

import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.Logger;

/**
 * Safe execution wrapper that isolates feature code and handles exceptions gracefully.
 */
public class SafeExecutionContext {
    
    private static final Logger logger = Logger.getLogger("Rubidium-SafeExec");
    
    private final String contextName;
    private final FeatureOrchestrator orchestrator;
    private final ScheduledExecutorService executor;
    
    public SafeExecutionContext(String contextName, FeatureOrchestrator orchestrator) {
        this.contextName = contextName;
        this.orchestrator = orchestrator;
        this.executor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "SafeExec-" + contextName);
            t.setDaemon(true);
            return t;
        });
    }
    
    public <T> T runSafe(String featureId, Supplier<T> action, T defaultValue) {
        FeatureGuard guard = orchestrator.getGuard(featureId);
        if (guard == null) {
            try {
                return action.get();
            } catch (Exception e) {
                logger.warn("[" + contextName + "] Unguarded execution failed for " + featureId + ": " + e.getMessage());
                return defaultValue;
            }
        }
        
        return guard.execute(action, () -> defaultValue);
    }
    
    public void runSafe(String featureId, Runnable action) {
        runSafe(featureId, () -> { action.run(); return null; }, null);
    }
    
    public <T> CompletableFuture<T> runAsync(String featureId, Supplier<T> action, T defaultValue) {
        return CompletableFuture.supplyAsync(
            () -> runSafe(featureId, action, defaultValue),
            executor
        );
    }
    
    public CompletableFuture<Void> runAsync(String featureId, Runnable action) {
        return CompletableFuture.runAsync(
            () -> runSafe(featureId, action),
            executor
        );
    }
    
    public <T> T runWithTimeout(String featureId, Supplier<T> action, T defaultValue, long timeoutMs) {
        try {
            return CompletableFuture.supplyAsync(
                () -> runSafe(featureId, action, defaultValue),
                executor
            ).get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            logger.warn("[" + contextName + "] Timeout executing " + featureId);
            orchestrator.recordTimeout(featureId);
            return defaultValue;
        } catch (Exception e) {
            logger.warn("[" + contextName + "] Error executing " + featureId + ": " + e.getMessage());
            return defaultValue;
        }
    }
    
    public <T, R> Function<T, R> wrapFunction(String featureId, Function<T, R> func, R defaultValue) {
        return input -> runSafe(featureId, () -> func.apply(input), defaultValue);
    }
    
    public <T> Consumer<T> wrapConsumer(String featureId, Consumer<T> consumer) {
        return input -> runSafe(featureId, () -> consumer.accept(input));
    }
    
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
