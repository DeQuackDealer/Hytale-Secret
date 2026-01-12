package com.yellowtale.rubidium.core.module;

/**
 * Represents the current lifecycle state of a module.
 */
public enum ModuleState {
    /**
     * Module has been discovered but not yet loaded.
     */
    DISCOVERED,
    
    /**
     * Module is currently being loaded.
     */
    LOADING,
    
    /**
     * Module has been loaded but is not yet enabled.
     */
    LOADED,
    
    /**
     * Module is currently being enabled.
     */
    ENABLING,
    
    /**
     * Module is fully enabled and active.
     */
    ENABLED,
    
    /**
     * Module is currently being disabled.
     */
    DISABLING,
    
    /**
     * Module has been disabled but remains in memory.
     */
    DISABLED,
    
    /**
     * Module is currently being unloaded.
     */
    UNLOADING,
    
    /**
     * Module has been completely unloaded.
     */
    UNLOADED,
    
    /**
     * Module failed to load or enable.
     */
    FAILED;
    
    /**
     * Check if the module is in an active, usable state.
     */
    public boolean isActive() {
        return this == ENABLED;
    }
    
    /**
     * Check if the module is in a transitional state.
     */
    public boolean isTransitioning() {
        return this == LOADING || this == ENABLING || 
               this == DISABLING || this == UNLOADING;
    }
    
    /**
     * Check if the module can be enabled from this state.
     */
    public boolean canEnable() {
        return this == LOADED || this == DISABLED;
    }
    
    /**
     * Check if the module can be disabled from this state.
     */
    public boolean canDisable() {
        return this == ENABLED;
    }
}
