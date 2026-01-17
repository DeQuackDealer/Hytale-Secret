package rubidium;

import rubidium.api.npc.NPCAPI;
import rubidium.api.scheduler.SchedulerAPI;

import java.util.logging.Logger;

/**
 * Rubidium Framework - API Library for Hytale Plugin Development
 * 
 * This is the main plugin class that integrates Rubidium's APIs
 * with the official Hytale server. Rubidium provides:
 * 
 * - PathfindingAPI: Built-in A* pathfinding with async support
 * - NPCAPI: Create and manage NPCs with behaviors
 * - AIBehaviorAPI: Behavior trees and goal selectors
 * - WorldQueryAPI: Spatial queries, raycasting, line-of-sight
 * - SchedulerAPI: Task scheduling, cooldowns, chains
 * - ScoreboardAPI: Dynamic scoreboards and teams
 * - HologramAPI: Floating text and item displays
 * - TeleportAPI: Warps, TPA requests
 * - BossBarAPI: Custom boss bars
 * - InventoryAPI: Custom GUIs with click handlers
 * - ConfigAPI: YAML configuration management
 * - MessageAPI: Colors, placeholders, localization
 * 
 * Usage:
 * 1. Add Rubidium.jar to your server's earlyplugins folder
 * 2. Use Rubidium's APIs in your plugins via static methods
 * 
 * Example:
 * {@code
 * var path = PathfindingAPI.findPath(start, goal, context);
 * var npc = NPCAPI.spawn("mymod:guard", location);
 * SchedulerAPI.runLater(() -> npc.moveTo(target), 20);
 * }
 */
public class RubidiumPlugin {
    
    private static RubidiumPlugin instance;
    private static final Logger LOGGER = Logger.getLogger("Rubidium");
    private boolean enabled = false;
    
    public RubidiumPlugin() {
        instance = this;
        LOGGER.info("Rubidium Framework v1.0.0 loading...");
    }
    
    public void onEnable() {
        enabled = true;
        LOGGER.info("Rubidium Framework enabled!");
        LOGGER.info("Available APIs: Pathfinding, NPC, AI, WorldQuery, Scheduler, Scoreboard, Hologram, Teleport, BossBar, Inventory, Config, Message");
        
        // Register tick listener for NPC/AI updates
        SchedulerAPI.onTick(tick -> {
            // Tick all NPCs
            for (var npc : NPCAPI.all()) {
                npc.tick();
            }
        });
    }
    
    public void onDisable() {
        enabled = false;
        LOGGER.info("Rubidium Framework disabling...");
        
        // Cleanup
        SchedulerAPI.shutdown();
        
        LOGGER.info("Rubidium Framework disabled.");
    }
    
    /**
     * Get the Rubidium plugin instance.
     * @return The singleton instance of RubidiumPlugin
     */
    public static RubidiumPlugin getInstance() {
        return instance;
    }
    
    /**
     * Get the Rubidium version.
     * @return Version string
     */
    public static String getVersion() {
        return "1.0.0";
    }
    
    /**
     * Check if Rubidium is enabled.
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Get the logger for Rubidium.
     * @return Logger instance
     */
    public static Logger getLogger() {
        return LOGGER;
    }
}
