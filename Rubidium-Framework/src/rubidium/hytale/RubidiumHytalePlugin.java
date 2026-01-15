package rubidium.hytale;

import rubidium.core.RubidiumCore;
import rubidium.core.feature.FeatureOrchestrator;
import rubidium.hytale.adapter.ServerAdapter;
import rubidium.hytale.adapter.EventAdapter;
import rubidium.hytale.api.event.*;
import rubidium.welcome.FirstJoinHandler;
import com.hypixel.hytale.server.core.HytaleServer;

import java.util.logging.Logger;

/**
 * Main entry point for Rubidium Framework on Hytale servers.
 */
public class RubidiumHytalePlugin {
    
    private static final Logger logger = Logger.getLogger("Rubidium");
    
    private static RubidiumHytalePlugin instance;
    
    private RubidiumCore core;
    private FeatureOrchestrator orchestrator;
    
    public RubidiumHytalePlugin() {
        instance = this;
    }
    
    public static RubidiumHytalePlugin getInstance() {
        return instance;
    }
    
    public void onEnable(HytaleServer hytaleServer) {
        logger.info("Rubidium Framework initializing...");
        
        ServerAdapter.getInstance().initialize(hytaleServer);
        
        core = RubidiumCore.initialize(java.nio.file.Paths.get("rubidium"));
        orchestrator = FeatureOrchestrator.getInstance();
        
        registerCoreFeatures();
        registerEventBridges();
        
        FirstJoinHandler.register();
        
        orchestrator.startAll();
        
        logger.info("Rubidium Framework v1.0.0 enabled - Ready for Hytale!");
        logger.info("  - Player API: Connected");
        logger.info("  - Entity API: Connected");
        logger.info("  - Event Bridge: Active");
        logger.info("  - Feature Orchestrator: " + orchestrator.getAllHealth().size() + " features");
    }
    
    private void registerCoreFeatures() {
    }
    
    private void registerEventBridges() {
        EventAdapter eventAdapter = EventAdapter.getInstance();
        
        eventAdapter.registerListener(PlayerJoinEvent.class, event -> {
            logger.fine("Player joined: " + event.getPlayer().getUsername());
        });
        
        eventAdapter.registerListener(PlayerQuitEvent.class, event -> {
            logger.fine("Player quit: " + event.getPlayer().getUsername());
        });
        
        eventAdapter.registerListener(BlockBreakEvent.class, event -> {
            logger.fine("Block broken at " + event.getX() + "," + event.getY() + "," + event.getZ());
        });
    }
    
    public void onDisable() {
        logger.info("Rubidium Framework disabling...");
        
        if (orchestrator != null) {
            orchestrator.stopAll();
        }
        
        logger.info("Rubidium Framework disabled");
    }
    
    public RubidiumCore getCore() {
        return core;
    }
    
    public FeatureOrchestrator getOrchestrator() {
        return orchestrator;
    }
}
