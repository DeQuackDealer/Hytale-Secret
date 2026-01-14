package rubidium.core.feature;

import rubidium.hytale.api.player.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Central orchestrator for all Rubidium features.
 * Manages lifecycle, health monitoring, and graceful degradation.
 */
public class FeatureOrchestrator {
    
    private static final Logger logger = Logger.getLogger("Rubidium-Orchestrator");
    
    private static FeatureOrchestrator instance;
    
    private final Map<String, FeatureLifecycle> features = new ConcurrentHashMap<>();
    private final Map<String, FeatureGuard> guards = new ConcurrentHashMap<>();
    private final Map<String, FeatureHealth> healthStatus = new ConcurrentHashMap<>();
    private final Map<String, FeatureState> featureStates = new ConcurrentHashMap<>();
    
    private final ScheduledExecutorService healthChecker;
    private final List<Consumer<FeatureEvent>> eventListeners = new CopyOnWriteArrayList<>();
    private final SafeExecutionContext safeContext;
    
    private volatile boolean running = false;
    
    private FeatureOrchestrator() {
        this.healthChecker = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "Rubidium-HealthChecker");
            t.setDaemon(true);
            return t;
        });
        this.safeContext = new SafeExecutionContext("Orchestrator", this);
    }
    
    public static FeatureOrchestrator getInstance() {
        if (instance == null) {
            instance = new FeatureOrchestrator();
        }
        return instance;
    }
    
    public void registerFeature(FeatureLifecycle feature) {
        String id = feature.getFeatureId();
        features.put(id, feature);
        guards.put(id, new FeatureGuard(id));
        featureStates.put(id, FeatureState.REGISTERED);
        healthStatus.put(id, FeatureHealth.disabled("Not initialized"));
        
        logger.info("Registered feature: " + feature.getFeatureName() + " (" + id + ")");
        fireEvent(new FeatureEvent(id, FeatureEvent.Type.REGISTERED, null));
    }
    
    public void startAll() {
        running = true;
        
        List<FeatureLifecycle> sorted = features.values().stream()
            .sorted(Comparator
                .comparing((FeatureLifecycle f) -> f.getPriority().getOrder())
                .thenComparing(f -> f.getDependencies().length))
            .collect(Collectors.toList());
        
        for (FeatureLifecycle feature : sorted) {
            startFeature(feature.getFeatureId());
        }
        
        healthChecker.scheduleAtFixedRate(
            this::runHealthChecks,
            30, 30, TimeUnit.SECONDS
        );
        
        logger.info("Feature orchestrator started with " + features.size() + " features");
    }
    
    public boolean startFeature(String featureId) {
        FeatureLifecycle feature = features.get(featureId);
        if (feature == null) return false;
        
        if (!checkDependencies(feature)) {
            logger.warn("Cannot start " + featureId + ": missing dependencies");
            featureStates.put(featureId, FeatureState.DEPENDENCY_FAILED);
            return false;
        }
        
        try {
            featureStates.put(featureId, FeatureState.INITIALIZING);
            feature.initialize();
            
            featureStates.put(featureId, FeatureState.STARTING);
            feature.start();
            
            featureStates.put(featureId, FeatureState.RUNNING);
            healthStatus.put(featureId, FeatureHealth.healthy());
            guards.get(featureId).reset();
            
            logger.info("Started feature: " + feature.getFeatureName());
            fireEvent(new FeatureEvent(featureId, FeatureEvent.Type.STARTED, null));
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to start feature " + featureId + ": " + e.getMessage());
            featureStates.put(featureId, FeatureState.FAILED);
            healthStatus.put(featureId, FeatureHealth.unhealthy("Failed to start", e));
            
            if (!feature.isOptional()) {
                throw new RuntimeException("Critical feature failed: " + featureId, e);
            }
            
            fireEvent(new FeatureEvent(featureId, FeatureEvent.Type.FAILED, e.getMessage()));
            return false;
        }
    }
    
    public void stopFeature(String featureId) {
        FeatureLifecycle feature = features.get(featureId);
        if (feature == null) return;
        
        try {
            featureStates.put(featureId, FeatureState.STOPPING);
            feature.stop();
            featureStates.put(featureId, FeatureState.STOPPED);
            healthStatus.put(featureId, FeatureHealth.disabled("Stopped"));
            
            logger.info("Stopped feature: " + feature.getFeatureName());
            fireEvent(new FeatureEvent(featureId, FeatureEvent.Type.STOPPED, null));
            
        } catch (Exception e) {
            logger.warn("Error stopping feature " + featureId + ": " + e.getMessage());
            featureStates.put(featureId, FeatureState.FAILED);
        }
    }
    
    public void disableFeature(String featureId, String reason) {
        stopFeature(featureId);
        featureStates.put(featureId, FeatureState.DISABLED);
        healthStatus.put(featureId, FeatureHealth.disabled(reason));
        guards.get(featureId).forceOpen();
        
        logger.warn("Feature disabled: " + featureId + " - " + reason);
        fireEvent(new FeatureEvent(featureId, FeatureEvent.Type.DISABLED, reason));
    }
    
    public boolean restartFeature(String featureId) {
        stopFeature(featureId);
        return startFeature(featureId);
    }
    
    public void stopAll() {
        running = false;
        healthChecker.shutdown();
        
        List<String> ids = new ArrayList<>(features.keySet());
        Collections.reverse(ids);
        
        for (String id : ids) {
            try {
                FeatureLifecycle feature = features.get(id);
                if (feature != null) {
                    feature.shutdown();
                }
            } catch (Exception e) {
                logger.warn("Error shutting down " + id + ": " + e.getMessage());
            }
        }
        
        safeContext.shutdown();
        logger.info("Feature orchestrator stopped");
    }
    
    private boolean checkDependencies(FeatureLifecycle feature) {
        for (String dep : feature.getDependencies()) {
            FeatureState state = featureStates.get(dep);
            if (state != FeatureState.RUNNING) {
                return false;
            }
        }
        return true;
    }
    
    private void runHealthChecks() {
        for (Map.Entry<String, FeatureLifecycle> entry : features.entrySet()) {
            String id = entry.getKey();
            FeatureLifecycle feature = entry.getValue();
            
            if (featureStates.get(id) != FeatureState.RUNNING) continue;
            
            try {
                FeatureHealth health = feature.healthCheck();
                healthStatus.put(id, health);
                
                if (health.status() == FeatureHealth.Status.UNHEALTHY) {
                    handleUnhealthyFeature(id, health);
                } else if (health.status() == FeatureHealth.Status.DEGRADED) {
                    logger.warn("Feature " + id + " is degraded: " + health.message());
                }
                
            } catch (Exception e) {
                logger.error("Health check failed for " + id + ": " + e.getMessage());
                healthStatus.put(id, FeatureHealth.unhealthy("Health check failed", e));
                handleUnhealthyFeature(id, healthStatus.get(id));
            }
        }
    }
    
    private void handleUnhealthyFeature(String featureId, FeatureHealth health) {
        FeatureLifecycle feature = features.get(featureId);
        if (feature == null) return;
        
        logger.warn("Feature " + featureId + " is unhealthy: " + health.message());
        
        if (feature.isOptional()) {
            FeatureGuard guard = guards.get(featureId);
            if (guard.getFailureCount() >= 10) {
                disableFeature(featureId, "Too many failures: " + health.message());
            } else {
                guard.recordFailure(new RuntimeException(health.message()));
            }
        } else {
            logger.error("Critical feature " + featureId + " is unhealthy!");
            fireEvent(new FeatureEvent(featureId, FeatureEvent.Type.CRITICAL_FAILURE, health.message()));
        }
    }
    
    public void recordTimeout(String featureId) {
        FeatureGuard guard = guards.get(featureId);
        if (guard != null) {
            guard.recordFailure(new TimeoutException("Feature timeout"));
        }
    }
    
    public FeatureGuard getGuard(String featureId) {
        return guards.get(featureId);
    }
    
    public FeatureHealth getHealth(String featureId) {
        return healthStatus.getOrDefault(featureId, 
            FeatureHealth.disabled("Feature not registered"));
    }
    
    public FeatureState getState(String featureId) {
        return featureStates.getOrDefault(featureId, FeatureState.UNKNOWN);
    }
    
    public Map<String, FeatureHealth> getAllHealth() {
        return new HashMap<>(healthStatus);
    }
    
    public boolean isFeatureAvailable(String featureId) {
        FeatureState state = featureStates.get(featureId);
        if (state != FeatureState.RUNNING) return false;
        
        FeatureGuard guard = guards.get(featureId);
        return guard == null || guard.allowRequest();
    }
    
    public SafeExecutionContext getSafeContext() {
        return safeContext;
    }
    
    public void addEventListener(Consumer<FeatureEvent> listener) {
        eventListeners.add(listener);
    }
    
    private void fireEvent(FeatureEvent event) {
        for (Consumer<FeatureEvent> listener : eventListeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                logger.warn("Error in event listener: " + e.getMessage());
            }
        }
    }
    
    public void notifyPlayers(String featureId, String message, 
                               java.util.function.BiConsumer<Player, String> notifier) {
    }
    
    public <T> T executeWithGuard(String featureId, java.util.function.Supplier<T> action, T fallbackValue) {
        if (!isFeatureAvailable(featureId)) {
            logger.fine("Feature " + featureId + " unavailable, using fallback");
            return fallbackValue;
        }
        
        FeatureGuard guard = guards.get(featureId);
        if (guard == null) {
            try {
                return action.get();
            } catch (Exception e) {
                logger.warn("Unguarded execution failed: " + e.getMessage());
                return fallbackValue;
            }
        }
        
        return guard.execute(action, () -> {
            logger.fine("Feature " + featureId + " circuit open, using fallback");
            return fallbackValue;
        });
    }
    
    public void executeWithGuard(String featureId, Runnable action, Runnable fallback) {
        executeWithGuard(featureId, () -> { action.run(); return null; }, null);
        if (!isFeatureAvailable(featureId) && fallback != null) {
            fallback.run();
        }
    }
    
    public <T> java.util.concurrent.CompletableFuture<T> executeAsync(String featureId, 
            java.util.function.Supplier<T> action, T fallbackValue) {
        return safeContext.runAsync(featureId, action, fallbackValue);
    }
    
    public void tryRecoverFeature(String featureId) {
        FeatureGuard guard = guards.get(featureId);
        if (guard == null) return;
        
        if (guard.getState() == FeatureGuard.CircuitState.OPEN) {
            logger.info("Attempting recovery for feature: " + featureId);
            
            FeatureLifecycle feature = features.get(featureId);
            if (feature != null) {
                try {
                    FeatureHealth health = feature.healthCheck();
                    if (health.isOperational()) {
                        guard.reset();
                        featureStates.put(featureId, FeatureState.RUNNING);
                        healthStatus.put(featureId, health);
                        fireEvent(new FeatureEvent(featureId, FeatureEvent.Type.RECOVERED, "Auto-recovered"));
                        logger.info("Feature " + featureId + " recovered");
                    }
                } catch (Exception e) {
                    logger.warn("Recovery attempt failed for " + featureId + ": " + e.getMessage());
                }
            }
        }
    }
    
    public void scheduleRecoveryAttempt(String featureId, long delayMs) {
        healthChecker.schedule(() -> tryRecoverFeature(featureId), delayMs, TimeUnit.MILLISECONDS);
    }
    
    public enum FeatureState {
        UNKNOWN,
        REGISTERED,
        INITIALIZING,
        STARTING,
        RUNNING,
        STOPPING,
        STOPPED,
        DISABLED,
        FAILED,
        DEPENDENCY_FAILED
    }
    
    public record FeatureEvent(String featureId, Type type, String message) {
        public enum Type {
            REGISTERED,
            STARTED,
            STOPPED,
            DISABLED,
            FAILED,
            RECOVERED,
            DEGRADED,
            CRITICAL_FAILURE
        }
    }
}
