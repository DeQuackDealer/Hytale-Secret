package com.yellowtale.rubidium.core.module;

import com.yellowtale.rubidium.core.logging.RubidiumLogger;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Convenient base class for implementing modules.
 * Provides common functionality and state management.
 */
public abstract class AbstractModule implements Module {
    
    private final AtomicReference<ModuleState> state = new AtomicReference<>(ModuleState.DISCOVERED);
    protected ModuleContext context;
    protected RubidiumLogger logger;
    
    @Override
    public final void onLoad(ModuleContext context) {
        state.set(ModuleState.LOADING);
        this.context = context;
        this.logger = context.getLogger();
        
        try {
            doLoad();
            state.set(ModuleState.LOADED);
        } catch (Exception e) {
            state.set(ModuleState.FAILED);
            throw e;
        }
    }
    
    @Override
    public final void onEnable() {
        state.set(ModuleState.ENABLING);
        try {
            doEnable();
            state.set(ModuleState.ENABLED);
        } catch (Exception e) {
            state.set(ModuleState.FAILED);
            throw e;
        }
    }
    
    @Override
    public final void onDisable() {
        state.set(ModuleState.DISABLING);
        try {
            doDisable();
        } finally {
            state.set(ModuleState.DISABLED);
        }
    }
    
    @Override
    public final void onReload() {
        if (supportsReload()) {
            doReload();
        }
    }
    
    @Override
    public ModuleState getState() {
        return state.get();
    }
    
    /**
     * Override to perform module loading.
     */
    protected void doLoad() {
        // Default no-op
    }
    
    /**
     * Override to perform module enabling.
     */
    protected abstract void doEnable();
    
    /**
     * Override to perform module disabling.
     */
    protected abstract void doDisable();
    
    /**
     * Override to handle configuration reloads.
     */
    protected void doReload() {
        // Default no-op
    }
}
