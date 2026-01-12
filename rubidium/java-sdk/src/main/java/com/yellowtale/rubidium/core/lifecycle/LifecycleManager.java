package com.yellowtale.rubidium.core.lifecycle;

import com.yellowtale.rubidium.core.logging.LogManager;
import com.yellowtale.rubidium.core.logging.RubidiumLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages lifecycle events and transitions for the Rubidium runtime.
 * 
 * Provides hooks for components to react to lifecycle changes:
 * - STARTING: Runtime is initializing
 * - RUNNING: Runtime is fully operational
 * - RELOADING: Runtime is reloading configurations
 * - STOPPING: Runtime is shutting down
 * - STOPPED: Runtime has fully stopped
 */
public final class LifecycleManager {
    
    private final RubidiumLogger logger;
    private final AtomicReference<LifecyclePhase> currentPhase = new AtomicReference<>(LifecyclePhase.STOPPED);
    private final List<LifecycleListener> listeners = new CopyOnWriteArrayList<>();
    private final List<ShutdownHook> shutdownHooks = new CopyOnWriteArrayList<>();
    
    public LifecycleManager(LogManager logManager) {
        this.logger = logManager.getLogger("Lifecycle");
    }
    
    /**
     * Register a lifecycle listener.
     */
    public void addListener(LifecycleListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove a lifecycle listener.
     */
    public void removeListener(LifecycleListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Register a shutdown hook to be called during STOPPING phase.
     * Hooks are called in reverse registration order.
     */
    public void addShutdownHook(String name, Runnable action) {
        shutdownHooks.add(new ShutdownHook(name, action));
    }
    
    /**
     * Get the current lifecycle phase.
     */
    public LifecyclePhase getCurrentPhase() {
        return currentPhase.get();
    }
    
    /**
     * Check if the runtime is in a running state.
     */
    public boolean isRunning() {
        return currentPhase.get() == LifecyclePhase.RUNNING;
    }
    
    /**
     * Transition to the STARTING phase.
     */
    public void transitionToStarting() {
        transition(LifecyclePhase.STARTING);
    }
    
    /**
     * Transition to the RUNNING phase.
     */
    public void transitionToRunning() {
        transition(LifecyclePhase.RUNNING);
    }
    
    /**
     * Transition to the RELOADING phase.
     */
    public void transitionToReloading() {
        transition(LifecyclePhase.RELOADING);
    }
    
    /**
     * Transition to the STOPPING phase and execute shutdown hooks.
     */
    public void transitionToStopping() {
        transition(LifecyclePhase.STOPPING);
        executeShutdownHooks();
    }
    
    /**
     * Transition to the STOPPED phase.
     */
    public void transitionToStopped() {
        transition(LifecyclePhase.STOPPED);
    }
    
    private void transition(LifecyclePhase newPhase) {
        LifecyclePhase oldPhase = currentPhase.getAndSet(newPhase);
        if (oldPhase != newPhase) {
            logger.info("Lifecycle transition: {} -> {}", oldPhase, newPhase);
            notifyListeners(oldPhase, newPhase);
        }
    }
    
    private void notifyListeners(LifecyclePhase oldPhase, LifecyclePhase newPhase) {
        for (LifecycleListener listener : listeners) {
            try {
                listener.onPhaseChange(oldPhase, newPhase);
                
                switch (newPhase) {
                    case STARTING -> listener.onStarting();
                    case RUNNING -> listener.onRunning();
                    case RELOADING -> listener.onReloading();
                    case STOPPING -> listener.onStopping();
                    case STOPPED -> listener.onStopped();
                }
            } catch (Exception e) {
                logger.error("Error notifying lifecycle listener", e);
            }
        }
    }
    
    private void executeShutdownHooks() {
        logger.info("Executing {} shutdown hooks", shutdownHooks.size());
        
        List<ShutdownHook> reversed = new ArrayList<>(shutdownHooks);
        java.util.Collections.reverse(reversed);
        
        for (ShutdownHook hook : reversed) {
            try {
                logger.debug("Running shutdown hook: {}", hook.name());
                hook.action().run();
            } catch (Exception e) {
                logger.error("Error in shutdown hook '{}': {}", hook.name(), e.getMessage());
            }
        }
    }
    
    private record ShutdownHook(String name, Runnable action) {}
}
