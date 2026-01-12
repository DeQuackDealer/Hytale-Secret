package com.yellowtale.rubidium.core.lifecycle;

/**
 * Listener for lifecycle phase transitions.
 * Implement only the methods you need; all have default no-op implementations.
 */
public interface LifecycleListener {
    
    /**
     * Called on any phase change.
     */
    default void onPhaseChange(LifecyclePhase oldPhase, LifecyclePhase newPhase) {}
    
    /**
     * Called when entering STARTING phase.
     */
    default void onStarting() {}
    
    /**
     * Called when entering RUNNING phase.
     */
    default void onRunning() {}
    
    /**
     * Called when entering RELOADING phase.
     */
    default void onReloading() {}
    
    /**
     * Called when entering STOPPING phase.
     */
    default void onStopping() {}
    
    /**
     * Called when entering STOPPED phase.
     */
    default void onStopped() {}
}
