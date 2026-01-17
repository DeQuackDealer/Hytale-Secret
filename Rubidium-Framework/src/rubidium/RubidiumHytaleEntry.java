package rubidium;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import rubidium.api.npc.NPCAPI;
import rubidium.api.scheduler.SchedulerAPI;

import java.util.logging.Logger;

/**
 * Official Hytale plugin entrypoint for Rubidium Framework.
 * 
 * This class extends the official Hytale JavaPlugin and serves as
 * the main entry point when Rubidium is loaded on a Hytale server
 * or in singleplayer (local server mode).
 * 
 * The manifest.json "main" field points to this class:
 * "main": "rubidium.RubidiumHytaleEntry"
 * 
 * Supports both singleplayer (local server) and multiplayer modes.
 */
public class RubidiumHytaleEntry extends JavaPlugin {
    
    private static RubidiumHytaleEntry instance;
    private static final Logger LOGGER = Logger.getLogger("Rubidium");
    private static boolean isServer = true;
    private static boolean initialized = false;
    
    public RubidiumHytaleEntry(JavaPluginInit init) {
        super(init);
        instance = this;
        detectEnvironment();
        LOGGER.info("[Rubidium] Framework v1.0.0 loading...");
    }
    
    public RubidiumHytaleEntry() {
        super(null);
        instance = this;
        isServer = false;
        LOGGER.info("[Rubidium] Framework v1.0.0 loading (client mode)...");
    }
    
    private void detectEnvironment() {
        try {
            Class.forName("com.hypixel.hytale.server.HytaleServer");
            isServer = true;
        } catch (ClassNotFoundException e) {
            isServer = false;
        }
    }
    
    @Override
    public void onEnable() {
        if (initialized) {
            LOGGER.warning("[Rubidium] Already initialized, skipping...");
            return;
        }
        initialized = true;
        
        LOGGER.info("[Rubidium] Framework initializing...");
        LOGGER.info("[Rubidium] Mode: " + (isServer ? "Server/Singleplayer" : "Client"));
        
        if (isServer) {
            initServerFeatures();
        }
        
        initCommonFeatures();
        
        LOGGER.info("[Rubidium] Framework v1.0.0 enabled!");
        logAvailableAPIs();
    }
    
    private void initServerFeatures() {
        SchedulerAPI.runTimer("rubidium:npc_tick", () -> {
            for (var npc : NPCAPI.all()) {
                npc.tick();
            }
        }, 1, 1);
    }
    
    private void initCommonFeatures() {
    }
    
    private void logAvailableAPIs() {
        LOGGER.info("[Rubidium] Available APIs:");
        LOGGER.info("[Rubidium]   - PathfindingAPI: A* pathfinding with async support");
        LOGGER.info("[Rubidium]   - NPCAPI: NPC creation and behavior management");
        LOGGER.info("[Rubidium]   - AIBehaviorAPI: Behavior trees and goal selectors");
        LOGGER.info("[Rubidium]   - WorldQueryAPI: Raycasting and spatial queries");
        LOGGER.info("[Rubidium]   - SchedulerAPI: Task scheduling and cooldowns");
        LOGGER.info("[Rubidium]   - ScoreboardAPI: Dynamic scoreboards");
        LOGGER.info("[Rubidium]   - HologramAPI: Floating text displays");
        LOGGER.info("[Rubidium]   - TeleportAPI: Warps and TPA");
        LOGGER.info("[Rubidium]   - BossBarAPI: Custom boss bars");
        LOGGER.info("[Rubidium]   - InventoryAPI: Custom GUI menus");
        LOGGER.info("[Rubidium]   - ConfigAPI: YAML configuration");
        LOGGER.info("[Rubidium]   - MessageAPI: Colors and localization");
        LOGGER.info("[Rubidium]   - AdminUI: GUI-based server administration");
    }
    
    @Override
    public void onDisable() {
        LOGGER.info("[Rubidium] Framework disabling...");
        
        if (isServer) {
            SchedulerAPI.shutdown();
        }
        
        initialized = false;
        LOGGER.info("[Rubidium] Framework disabled.");
    }
    
    public static RubidiumHytaleEntry getInstance() {
        return instance;
    }
    
    public static String getVersion() {
        return "1.0.0";
    }
    
    public static boolean isServerMode() {
        return isServer;
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
}
