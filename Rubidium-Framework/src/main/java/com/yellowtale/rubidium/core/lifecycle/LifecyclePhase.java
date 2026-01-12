package com.yellowtale.rubidium.core.lifecycle;

/**
 * Represents the lifecycle phases of the Rubidium runtime.
 */
public enum LifecyclePhase {
    /**
     * Runtime has been created but not yet started.
     */
    STOPPED,
    
    /**
     * Runtime is initializing subsystems.
     */
    STARTING,
    
    /**
     * Runtime is fully operational and processing.
     */
    RUNNING,
    
    /**
     * Runtime is reloading configurations and modules.
     */
    RELOADING,
    
    /**
     * Runtime is shutting down subsystems.
     */
    STOPPING;
    
    /**
     * Check if this phase allows module operations.
     */
    public boolean allowsModuleOperations() {
        return this == RUNNING || this == RELOADING;
    }
    
    /**
     * Check if this phase allows task scheduling.
     */
    public boolean allowsTaskScheduling() {
        return this == RUNNING;
    }
}
