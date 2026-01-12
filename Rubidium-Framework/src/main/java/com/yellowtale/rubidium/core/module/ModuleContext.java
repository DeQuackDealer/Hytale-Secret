package com.yellowtale.rubidium.core.module;

import com.yellowtale.rubidium.core.config.ConfigManager;
import com.yellowtale.rubidium.core.logging.RubidiumLogger;
import com.yellowtale.rubidium.core.metrics.MetricsRegistry;
import com.yellowtale.rubidium.core.scheduler.RubidiumScheduler;

import java.nio.file.Path;

/**
 * Context object provided to modules during initialization.
 * Provides access to Rubidium core services scoped to the module.
 */
public final class ModuleContext {
    
    private final String moduleId;
    private final Path dataDirectory;
    private final RubidiumLogger logger;
    private final ConfigManager configManager;
    private final RubidiumScheduler scheduler;
    private final MetricsRegistry metricsRegistry;
    private final ModuleManager moduleManager;
    
    public ModuleContext(
        String moduleId,
        Path dataDirectory,
        RubidiumLogger logger,
        ConfigManager configManager,
        RubidiumScheduler scheduler,
        MetricsRegistry metricsRegistry,
        ModuleManager moduleManager
    ) {
        this.moduleId = moduleId;
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        this.configManager = configManager;
        this.scheduler = scheduler;
        this.metricsRegistry = metricsRegistry;
        this.moduleManager = moduleManager;
    }
    
    /**
     * Get the unique identifier of this module.
     */
    public String getModuleId() {
        return moduleId;
    }
    
    /**
     * Get the data directory for this module's files.
     */
    public Path getDataDirectory() {
        return dataDirectory;
    }
    
    /**
     * Get the logger scoped to this module.
     */
    public RubidiumLogger getLogger() {
        return logger;
    }
    
    /**
     * Get the configuration manager.
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * Get the task scheduler.
     */
    public RubidiumScheduler getScheduler() {
        return scheduler;
    }
    
    /**
     * Get the metrics registry for recording performance data.
     */
    public MetricsRegistry getMetricsRegistry() {
        return metricsRegistry;
    }
    
    /**
     * Get the module manager for inter-module communication.
     */
    public ModuleManager getModuleManager() {
        return moduleManager;
    }
    
    /**
     * Check if another module is loaded and enabled.
     */
    public boolean isModuleEnabled(String moduleId) {
        return moduleManager.isModuleEnabled(moduleId);
    }
    
    /**
     * Get another module by ID if it's enabled.
     */
    public <T extends Module> T getModule(String moduleId, Class<T> type) {
        return moduleManager.getModule(moduleId, type);
    }
}
