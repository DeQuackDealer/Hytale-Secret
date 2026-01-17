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
 * the main entry point when Rubidium is loaded on a Hytale server.
 * 
 * The manifest.json Main field should point to this class:
 * "Main": "rubidium.RubidiumHytaleEntry"
 */
public class RubidiumHytaleEntry extends JavaPlugin {
    
    private static RubidiumHytaleEntry instance;
    private static final Logger LOGGER = Logger.getLogger("Rubidium");
    
    public RubidiumHytaleEntry(JavaPluginInit init) {
        super(init);
        instance = this;
        LOGGER.info("Rubidium Framework v1.0.0 loading...");
    }
    
    @Override
    public void onEnable() {
        LOGGER.info("Rubidium Framework initializing...");
        
        // Start NPC tick loop integrated with server
        SchedulerAPI.runTimer("rubidium:npc_tick", () -> {
            for (var npc : NPCAPI.all()) {
                npc.tick();
            }
        }, 1, 1);
        
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
    
    @Override
    public void onDisable() {
        LOGGER.info("Rubidium Framework disabling...");
        
        // Cleanup scheduled tasks
        SchedulerAPI.shutdown();
        
        LOGGER.info("Rubidium Framework disabled.");
    }
    
    /**
     * Get the Rubidium plugin instance.
     * @return The singleton instance of RubidiumHytaleEntry
     */
    public static RubidiumHytaleEntry getInstance() {
        return instance;
    }
    
    /**
     * Get the Rubidium version.
     * @return Version string
     */
    public static String getVersion() {
        return "1.0.0";
    }
}
