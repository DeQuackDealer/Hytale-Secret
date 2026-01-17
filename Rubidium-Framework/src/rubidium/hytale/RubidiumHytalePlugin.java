package rubidium.hytale;

import rubidium.RubidiumPlugin;
import rubidium.api.npc.NPCAPI;
import rubidium.api.scheduler.SchedulerAPI;

import java.util.logging.Logger;

/**
 * Hytale-specific plugin wrapper for Rubidium Framework.
 * 
 * This class serves as the entry point when Rubidium is loaded
 * as a Hytale plugin. It initializes all Rubidium APIs and
 * sets up integration with the Hytale server.
 * 
 * When running on a real Hytale server, this should extend
 * com.hypixel.hytale.server.core.plugin.JavaPlugin. For development
 * without the Hytale JAR, it works as a standalone wrapper.
 */
public class RubidiumHytalePlugin {
    
    private static final Logger LOGGER = Logger.getLogger("Rubidium");
    private static RubidiumHytalePlugin instance;
    
    private RubidiumPlugin rubidium;
    private boolean enabled = false;
    
    public RubidiumHytalePlugin() {
        instance = this;
        LOGGER.info("Rubidium Framework v1.0.0 loading...");
    }
    
    public static RubidiumHytalePlugin getInstance() {
        return instance;
    }
    
    /**
     * Called when the plugin is enabled by Hytale.
     */
    public void onEnable() {
        LOGGER.info("Rubidium Framework initializing...");
        
        rubidium = new RubidiumPlugin();
        rubidium.onEnable();
        
        // Start NPC tick loop
        SchedulerAPI.runTimer("rubidium:npc_tick", () -> {
            for (var npc : NPCAPI.all()) {
                npc.tick();
            }
        }, 1, 1);
        
        enabled = true;
        
        LOGGER.info("Rubidium Framework v1.0.0 enabled!");
        LOGGER.info("Available APIs:");
        LOGGER.info("  - PathfindingAPI: A* pathfinding with async support");
        LOGGER.info("  - NPCAPI: NPC creation and behavior management");
        LOGGER.info("  - AIBehaviorAPI: Behavior trees and goal selectors");
        LOGGER.info("  - WorldQueryAPI: Raycasting and spatial queries");
        LOGGER.info("  - SchedulerAPI: Task scheduling and cooldowns");
        LOGGER.info("  - ScoreboardAPI: Dynamic scoreboards");
        LOGGER.info("  - HologramAPI: Floating text displays");
        LOGGER.info("  - TeleportAPI: Warps and TPA");
        LOGGER.info("  - BossBarAPI: Custom boss bars");
        LOGGER.info("  - InventoryAPI: Custom GUI menus");
        LOGGER.info("  - ConfigAPI: YAML configuration");
        LOGGER.info("  - MessageAPI: Colors and localization");
    }
    
    /**
     * Called when the plugin is disabled by Hytale.
     */
    public void onDisable() {
        LOGGER.info("Rubidium Framework disabling...");
        
        if (rubidium != null) {
            rubidium.onDisable();
        }
        
        SchedulerAPI.shutdown();
        enabled = false;
        
        LOGGER.info("Rubidium Framework disabled.");
    }
    
    /**
     * Check if Rubidium is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Get the core Rubidium instance.
     */
    public RubidiumPlugin getRubidium() {
        return rubidium;
    }
}
