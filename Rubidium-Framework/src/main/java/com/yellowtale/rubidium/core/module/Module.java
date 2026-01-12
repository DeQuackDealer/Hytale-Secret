package com.yellowtale.rubidium.core.module;

import com.yellowtale.rubidium.core.config.ConfigManager;
import com.yellowtale.rubidium.core.logging.RubidiumLogger;

import java.util.Set;

/**
 * Base interface for all Rubidium modules.
 * 
 * Modules are the fundamental building blocks of Rubidium functionality.
 * They can be loaded, unloaded, and reloaded at runtime without requiring
 * a server restart.
 * 
 * Implementation notes:
 * - Modules must be thread-safe
 * - State should be cleanly releasable on disable
 * - Dependencies should be declared via getHardDependencies/getSoftDependencies
 */
public interface Module {
    
    /**
     * Get the unique identifier for this module.
     * Must be lowercase, alphanumeric with underscores only.
     */
    String getId();
    
    /**
     * Get the human-readable display name.
     */
    String getDisplayName();
    
    /**
     * Get the semantic version string (e.g., "1.0.0").
     */
    String getVersion();
    
    /**
     * Get a brief description of this module's functionality.
     */
    String getDescription();
    
    /**
     * Get modules that must be loaded before this module.
     * If any hard dependency fails to load, this module will not load.
     */
    default Set<String> getHardDependencies() {
        return Set.of();
    }
    
    /**
     * Get modules that should be loaded before this module if available.
     * Soft dependencies are optional - missing ones won't prevent loading.
     */
    default Set<String> getSoftDependencies() {
        return Set.of();
    }
    
    /**
     * Called when the module is first loaded into memory.
     * Use for heavy initialization that should only happen once.
     * 
     * @param context The module context providing access to Rubidium services
     */
    void onLoad(ModuleContext context);
    
    /**
     * Called when the module is enabled and should become active.
     * Register event listeners, start tasks, etc.
     */
    void onEnable();
    
    /**
     * Called when the module is disabled.
     * Clean up resources, unregister listeners, stop tasks.
     */
    void onDisable();
    
    /**
     * Called when the module's configuration is reloaded.
     * Re-read config values and adjust behavior accordingly.
     */
    default void onReload() {
        // Default no-op for modules that don't need reload handling
    }
    
    /**
     * Check if this module supports runtime reloading.
     */
    default boolean supportsReload() {
        return true;
    }
    
    /**
     * Get the current state of this module.
     */
    ModuleState getState();
}
