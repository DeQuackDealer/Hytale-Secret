package com.yellowtale.rubidium.core;

import com.yellowtale.rubidium.core.config.ConfigManager;
import com.yellowtale.rubidium.core.lifecycle.LifecycleManager;
import com.yellowtale.rubidium.core.logging.LogManager;
import com.yellowtale.rubidium.core.metrics.MetricsRegistry;
import com.yellowtale.rubidium.core.module.ModuleManager;
import com.yellowtale.rubidium.core.network.NetworkManager;
import com.yellowtale.rubidium.core.performance.PerformanceBudgetManager;
import com.yellowtale.rubidium.core.scheduler.RubidiumScheduler;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Core entry point for the Rubidium server framework.
 * 
 * Rubidium provides a modular, performance-focused infrastructure layer
 * for game servers. It is designed to be runtime-reloadable and safe
 * for use with official modding APIs when they become available.
 * 
 * Thread-safe singleton with lazy initialization.
 */
public final class RubidiumCore {
    
    private static final AtomicReference<RubidiumCore> INSTANCE = new AtomicReference<>();
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
    
    private final Path dataDirectory;
    private final ModuleManager moduleManager;
    private final LifecycleManager lifecycleManager;
    private final ConfigManager configManager;
    private final RubidiumScheduler scheduler;
    private final PerformanceBudgetManager performanceManager;
    private final MetricsRegistry metricsRegistry;
    private final LogManager logManager;
    private final NetworkManager networkManager;
    
    private volatile boolean running = false;
    private volatile long startTime;
    
    private RubidiumCore(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.logManager = new LogManager(dataDirectory.resolve("logs"));
        this.metricsRegistry = new MetricsRegistry();
        this.configManager = new ConfigManager(dataDirectory.resolve("config"));
        this.performanceManager = new PerformanceBudgetManager(metricsRegistry);
        this.scheduler = new RubidiumScheduler(performanceManager, metricsRegistry);
        this.lifecycleManager = new LifecycleManager(logManager);
        this.networkManager = new NetworkManager(metricsRegistry, performanceManager);
        this.moduleManager = new ModuleManager(
            dataDirectory.resolve("modules"),
            lifecycleManager,
            configManager,
            logManager
        );
    }
    
    /**
     * Initialize the Rubidium core with the specified data directory.
     * This method is idempotent - subsequent calls with different paths will be ignored.
     * 
     * @param dataDirectory Root directory for Rubidium data (configs, modules, logs)
     * @return The initialized RubidiumCore instance
     */
    public static RubidiumCore initialize(Path dataDirectory) {
        if (INITIALIZED.compareAndSet(false, true)) {
            RubidiumCore core = new RubidiumCore(dataDirectory);
            INSTANCE.set(core);
            core.logManager.getLogger("Core").info("Rubidium Core initialized at {}", dataDirectory);
        }
        return INSTANCE.get();
    }
    
    /**
     * Get the singleton instance. Throws if not initialized.
     */
    public static RubidiumCore getInstance() {
        RubidiumCore instance = INSTANCE.get();
        if (instance == null) {
            throw new IllegalStateException("RubidiumCore has not been initialized. Call initialize() first.");
        }
        return instance;
    }
    
    /**
     * Check if Rubidium has been initialized.
     */
    public static boolean isInitialized() {
        return INITIALIZED.get();
    }
    
    /**
     * Start the Rubidium runtime.
     * Initializes all subsystems and begins tick processing.
     */
    public void start() {
        if (running) {
            logManager.getLogger("Core").warn("Rubidium is already running");
            return;
        }
        
        startTime = System.currentTimeMillis();
        running = true;
        
        logManager.getLogger("Core").info("Starting Rubidium Core...");
        lifecycleManager.transitionToStarting();
        
        boolean configStarted = false;
        boolean metricsStarted = false;
        boolean performanceStarted = false;
        boolean schedulerStarted = false;
        boolean networkStarted = false;
        
        try {
            configManager.initialize();
            configStarted = true;
            logManager.getLogger("Core").debug("ConfigManager initialized");
            
            metricsRegistry.initialize();
            metricsStarted = true;
            logManager.getLogger("Core").debug("MetricsRegistry initialized");
            
            performanceManager.initialize();
            performanceStarted = true;
            logManager.getLogger("Core").debug("PerformanceBudgetManager initialized");
            
            scheduler.start();
            schedulerStarted = true;
            logManager.getLogger("Core").debug("Scheduler started");
            
            networkManager.start();
            networkStarted = true;
            logManager.getLogger("Core").debug("NetworkManager started");
            
            moduleManager.discoverAndLoadModules();
            logManager.getLogger("Core").debug("Modules loaded");
            
            lifecycleManager.transitionToRunning();
            
            logManager.getLogger("Core").info("Rubidium Core started successfully in {}ms", 
                System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            logManager.getLogger("Core").error("Failed to start Rubidium Core, rolling back...", e);
            
            try {
                moduleManager.unloadAllModules();
            } catch (Exception ex) {
                logManager.getLogger("Core").error("Error unloading modules during rollback", ex);
            }
            
            if (networkStarted) {
                try { networkManager.stop(); } 
                catch (Exception ex) { logManager.getLogger("Core").error("Error stopping network during rollback", ex); }
            }
            if (schedulerStarted) {
                try { scheduler.stop(); } 
                catch (Exception ex) { logManager.getLogger("Core").error("Error stopping scheduler during rollback", ex); }
            }
            if (performanceStarted) {
                try { performanceManager.shutdown(); } 
                catch (Exception ex) { logManager.getLogger("Core").error("Error stopping performance manager during rollback", ex); }
            }
            if (metricsStarted) {
                try { metricsRegistry.shutdown(); } 
                catch (Exception ex) { logManager.getLogger("Core").error("Error stopping metrics during rollback", ex); }
            }
            if (configStarted) {
                try { configManager.shutdown(); } 
                catch (Exception ex) { logManager.getLogger("Core").error("Error stopping config during rollback", ex); }
            }
            
            running = false;
            lifecycleManager.transitionToStopped();
            throw new RuntimeException("Rubidium startup failed", e);
        }
    }
    
    /**
     * Stop the Rubidium runtime gracefully.
     * Unloads all modules and stops all subsystems.
     */
    public void stop() {
        if (!running) {
            return;
        }
        
        logManager.getLogger("Core").info("Stopping Rubidium Core...");
        lifecycleManager.transitionToStopping();
        
        try {
            moduleManager.unloadAllModules();
            logManager.getLogger("Core").debug("Modules unloaded");
            
            networkManager.stop();
            logManager.getLogger("Core").debug("NetworkManager stopped");
            
            scheduler.stop();
            logManager.getLogger("Core").debug("Scheduler stopped");
            
            performanceManager.shutdown();
            logManager.getLogger("Core").debug("PerformanceBudgetManager shutdown");
            
            metricsRegistry.shutdown();
            logManager.getLogger("Core").debug("MetricsRegistry shutdown");
            
            configManager.shutdown();
            logManager.getLogger("Core").debug("ConfigManager shutdown");
        } finally {
            running = false;
            lifecycleManager.transitionToStopped();
            logManager.getLogger("Core").info("Rubidium Core stopped");
        }
    }
    
    /**
     * Reload the runtime configuration and all reloadable modules.
     */
    public void reload() {
        logManager.getLogger("Core").info("Reloading Rubidium Core...");
        configManager.reloadAll();
        moduleManager.reloadAllModules();
        logManager.getLogger("Core").info("Rubidium Core reloaded");
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public long getUptime() {
        return running ? System.currentTimeMillis() - startTime : 0;
    }
    
    public Path getDataDirectory() {
        return dataDirectory;
    }
    
    public ModuleManager getModuleManager() {
        return moduleManager;
    }
    
    public LifecycleManager getLifecycleManager() {
        return lifecycleManager;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public RubidiumScheduler getScheduler() {
        return scheduler;
    }
    
    public PerformanceBudgetManager getPerformanceManager() {
        return performanceManager;
    }
    
    public MetricsRegistry getMetricsRegistry() {
        return metricsRegistry;
    }
    
    public LogManager getLogManager() {
        return logManager;
    }
    
    public NetworkManager getNetworkManager() {
        return networkManager;
    }
}
