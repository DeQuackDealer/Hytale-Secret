package com.yellowtale.rubidium;

import com.yellowtale.rubidium.api.RubidiumAPI;
import com.yellowtale.rubidium.api.Plugin;
import com.yellowtale.rubidium.api.PluginManager;
import com.yellowtale.rubidium.event.EventBus;
import com.yellowtale.rubidium.lifecycle.ServerLifecycle;
import com.yellowtale.rubidium.performance.PerformanceMonitor;
import com.yellowtale.rubidium.performance.RPAL;
import com.yellowtale.rubidium.ipc.IpcServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class RubidiumPlugin implements Plugin {
    private static final Logger logger = LoggerFactory.getLogger(RubidiumPlugin.class);
    private static RubidiumPlugin instance;
    
    private final EventBus eventBus;
    private final PluginManager pluginManager;
    private final PerformanceMonitor performanceMonitor;
    private final ServerLifecycle lifecycle;
    private final RubidiumConfig config;
    private IpcServer ipcServer;
    
    private boolean enabled = false;
    
    public RubidiumPlugin() {
        instance = this;
        this.eventBus = new EventBus();
        this.pluginManager = new PluginManager(this);
        this.performanceMonitor = new PerformanceMonitor();
        this.lifecycle = new ServerLifecycle(eventBus);
        this.config = RubidiumConfig.load();
    }
    
    public static RubidiumPlugin getInstance() {
        return instance;
    }
    
    @Override
    public void onLoad() {
        logger.info("Rubidium v{} loading...", getVersion());
        logger.info("  ____       _     _     _ _                 ");
        logger.info(" |  _ \\ _   _| |__ (_) __| (_)_   _ _ __ ___  ");
        logger.info(" | |_) | | | | '_ \\| |/ _` | | | | | '_ ` _ \\ ");
        logger.info(" |  _ <| |_| | |_) | | (_| | | |_| | | | | | |");
        logger.info(" |_| \\_\\\\__,_|_.__/|_|\\__,_|_|\\__,_|_| |_| |_|");
        logger.info("");
        logger.info("Like Lithium & Sodium, but for Hytale servers");
        logger.info("Rubidium Performance Acceleration Layer (RPAL) enabled");
        
        RPAL.RPALConfig rpalConfig = new RPAL.RPALConfig(
            config.isRpalSimdEnabled(),
            config.isRpalDirectBuffersEnabled(),
            config.isRpalObjectPoolingEnabled(),
            config.isRpalTickSchedulerEnabled(),
            config.isRpalPreallocateBuffers(),
            config.getRpalAsyncThreads()
        );
        RPAL.getInstance().initialize(rpalConfig);
        
        File pluginsDir = new File(getDataFolder(), "plugins");
        if (!pluginsDir.exists()) {
            pluginsDir.mkdirs();
        }
        
        pluginManager.discoverPlugins(pluginsDir.toPath());
        pluginManager.loadAll();
        
        logger.info("Rubidium loaded {} plugins", pluginManager.getLoadedPlugins().size());
    }
    
    @Override
    public void onEnable() {
        logger.info("Rubidium enabling...");
        
        if (config.isIpcEnabled()) {
            ipcServer = new IpcServer(config.getIpcPort(), this);
            ipcServer.start();
            logger.info("IPC server started on port {} (Yellow Tale compatible)", config.getIpcPort());
        }
        
        performanceMonitor.start();
        
        if (config.isRpalTickSchedulerEnabled()) {
            try {
                RPAL.getInstance().getTickScheduler().start();
                logger.info("RPAL tick scheduler started");
            } catch (Exception e) {
                logger.warn("Failed to start tick scheduler: {}", e.getMessage());
            }
        }
        
        pluginManager.enableAll();
        
        lifecycle.onServerStart();
        
        enabled = true;
        
        logStartupSummary();
    }
    
    private void logStartupSummary() {
        logger.info("=============================================");
        logger.info("Rubidium v{} enabled successfully!", getVersion());
        logger.info("Like Lithium & Sodium, optimized for Hytale");
        logger.info("=============================================");
        
        if (RPAL.getInstance().isInitialized()) {
            RPAL.Capabilities caps = RPAL.getInstance().getCapabilities();
            logger.info("RPAL Capabilities (Lithium-style optimizations):");
            logger.info("  SIMD acceleration: {}", caps.simdAvailable() ? "YES (Vector API)" : "NO (scalar fallback)");
            logger.info("  JIT tiered compilation: {}", caps.jitTieredCompilation() ? "YES" : "NO");
            logger.info("  Zero-allocation buffers: {}", caps.directBuffersEnabled() ? "YES" : "NO");
            logger.info("  Object pooling: {}", caps.objectPoolingEnabled() ? "YES" : "NO");
            logger.info("  Tick scheduler: {}", caps.tickSchedulerEnabled() ? "YES" : "NO");
            logger.info("  GC type: {}", caps.gcType());
            logger.info("  CPU cores: {}", caps.availableCpuCores());
            logger.info("  Max heap: {} MB", caps.maxHeapMemory() / (1024 * 1024));
        }
        
        logger.info("IPC: {}", config.isIpcEnabled() ? "port " + config.getIpcPort() : "disabled");
        logger.info("Plugins loaded: {}", pluginManager.getLoadedPlugins().size());
        logger.info("=============================================");
    }
    
    @Override
    public void onDisable() {
        logger.info("Rubidium disabling...");
        
        lifecycle.onServerStop();
        
        pluginManager.disableAll();
        
        performanceMonitor.stop();
        
        if (ipcServer != null) {
            ipcServer.stop();
        }
        
        RPAL.getInstance().shutdown();
        
        enabled = false;
        logger.info("Rubidium disabled.");
    }
    
    @Override
    public String getName() {
        return "Rubidium";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0-RPAL";
    }
    
    @Override
    public File getDataFolder() {
        return new File("rubidium");
    }
    
    public EventBus getEventBus() {
        return eventBus;
    }
    
    public PluginManager getPluginManager() {
        return pluginManager;
    }
    
    public PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }
    
    public ServerLifecycle getLifecycle() {
        return lifecycle;
    }
    
    public RubidiumConfig getConfig() {
        return config;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public RubidiumAPI getAPI() {
        return new RubidiumAPI(this);
    }
    
    public RPAL getRPAL() {
        return RPAL.getInstance();
    }
}
