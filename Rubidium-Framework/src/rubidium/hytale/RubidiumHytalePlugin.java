package rubidium.hytale;

import rubidium.core.RubidiumCore;
import rubidium.hytale.adapter.HytaleAdapter;
import rubidium.hytale.event.HytaleEventBridge;

import java.util.logging.Logger;

public class RubidiumHytalePlugin {
    
    private static final Logger logger = Logger.getLogger("Rubidium");
    
    private static RubidiumHytalePlugin instance;
    
    private final Object hytalePlugin;
    private final HytaleAdapter adapter;
    private RubidiumCore core;
    
    public RubidiumHytalePlugin(Object hytalePlugin) {
        this.hytalePlugin = hytalePlugin;
        this.adapter = HytaleAdapter.getInstance();
        instance = this;
    }
    
    public static RubidiumHytalePlugin getInstance() {
        return instance;
    }
    
    public void onEnable(Object hytaleServer) {
        logger.info("Rubidium Framework initializing...");
        
        adapter.initialize(hytaleServer);
        
        core = new RubidiumCore();
        
        registerEventBridges();
        
        logger.info("Rubidium Framework v1.0.0 enabled - Ready for Hytale!");
        logger.info("  - Player API: Connected");
        logger.info("  - Entity API: Connected");
        logger.info("  - Event Bridge: Active");
        logger.info("  - Packet Adapter: Active");
    }
    
    private void registerEventBridges() {
        HytaleEventBridge eventBridge = adapter.getEventBridge();
        
        eventBridge.registerListener(HytaleEventBridge.RubidiumPlayerJoinEvent.class, event -> {
            if (event.player() != null) {
                logger.fine("Player joined: " + event.player().getName());
            }
        });
        
        eventBridge.registerListener(HytaleEventBridge.RubidiumPlayerQuitEvent.class, event -> {
            if (event.player() != null) {
                adapter.unwrapPlayer(event.player().getUUID());
                logger.fine("Player quit: " + event.player().getName());
            }
        });
    }
    
    public void onDisable() {
        logger.info("Rubidium Framework disabling...");
        
        adapter.shutdown();
        
        logger.info("Rubidium Framework disabled");
    }
    
    public HytaleAdapter getAdapter() {
        return adapter;
    }
    
    public RubidiumCore getCore() {
        return core;
    }
    
    public Object getHytalePlugin() {
        return hytalePlugin;
    }
}
