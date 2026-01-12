package com.yellowtale.rubidium.core.config;

/**
 * Listener for configuration reload events.
 */
@FunctionalInterface
public interface ConfigReloadListener<T extends Config> {
    
    /**
     * Called when a configuration is reloaded.
     * 
     * @param oldConfig The previous configuration
     * @param newConfig The newly loaded configuration
     */
    void onReload(T oldConfig, T newConfig);
}
