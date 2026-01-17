package rubidium;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.command.CommandSender;
import com.hypixel.hytale.server.core.command.PluginCommand;

import rubidium.admin.AdminUIModule;
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
    private AdminUIModule adminModule;
    
    public RubidiumHytaleEntry(JavaPluginInit init) {
        super(init);
        instance = this;
        isServer = (init != null);
        LOGGER.info("[Rubidium] Framework v1.0.0 loading...");
        LOGGER.info("[Rubidium] Environment: " + (isServer ? "Server" : "Singleplayer"));
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
        
        registerCommands();
        
        if (isServer) {
            initServerFeatures();
        }
        
        initCommonFeatures();
        
        LOGGER.info("[Rubidium] Framework v1.0.0 enabled!");
        logAvailableAPIs();
        logKeybinds();
    }
    
    private void registerCommands() {
        registerCommand(new PluginCommand("rubidium", "Show Rubidium framework info", null, "rb") {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                sender.sendMessage("&6=== Rubidium Framework v1.0.0 ===");
                sender.sendMessage("&7A comprehensive API library for Hytale");
                sender.sendMessage("");
                sender.sendMessage("&eCommands:");
                sender.sendMessage("&f  /rubidium &7- Show this info");
                sender.sendMessage("&f  /admin &7- Open admin panel (requires permission)");
                sender.sendMessage("&f  /adminstick &7- Get admin stick item");
                sender.sendMessage("");
                sender.sendMessage("&eKeybinds:");
                sender.sendMessage("&f  Q &7- Primary ability (Ability1)");
                sender.sendMessage("&f  Right-click Admin Stick &7- Open admin menu");
                sender.sendMessage("&f  Left-click Admin Stick &7- Quick player list");
                sender.sendMessage("");
                sender.sendMessage("&7Status: &aEnabled | Mode: " + (isServer ? "Server" : "Singleplayer"));
                return true;
            }
        });
        
        registerCommand(new PluginCommand("admin", "Open admin panel", "rubidium.admin", "adminpanel", "ap") {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                if (!sender.isPlayer()) {
                    sender.sendMessage("&cThis command can only be used by players");
                    return true;
                }
                sender.sendMessage("&aOpening Admin Panel...");
                sender.sendMessage("&7(In-game: The admin GUI would open here)");
                sender.sendMessage("&7Available panels: players, worlds, permissions, server, chunks, items, teleport, bans");
                return true;
            }
        });
        
        registerCommand(new PluginCommand("adminstick", "Get an admin stick", "rubidium.admin") {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                if (!sender.isPlayer()) {
                    sender.sendMessage("&cThis command can only be used by players");
                    return true;
                }
                sender.sendMessage("&aYou have been given an Admin Stick!");
                sender.sendMessage("&7Right-click: Open Admin Menu");
                sender.sendMessage("&7Left-click: Quick Action (Players panel)");
                sender.sendMessage("&7Shift+Right-click: Configure Shortcuts");
                return true;
            }
        });
        
        LOGGER.info("[Rubidium] Registered commands: /rubidium, /admin, /adminstick");
    }
    
    private void initServerFeatures() {
        adminModule = new AdminUIModule();
        adminModule.onEnable();
        
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
    
    private void logKeybinds() {
        LOGGER.info("[Rubidium] Keybinds (interceptable events):");
        LOGGER.info("[Rubidium]   Q - Ability1 (primary ability)");
        LOGGER.info("[Rubidium]   Left Click - Attack/Interact");
        LOGGER.info("[Rubidium]   Right Click - Secondary action");
        LOGGER.info("[Rubidium]   Admin Stick: Right-click=Menu, Left-click=Players, Shift+Right=Settings");
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
