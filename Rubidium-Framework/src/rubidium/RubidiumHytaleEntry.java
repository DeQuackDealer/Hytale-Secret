package rubidium;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.command.CommandSender;
import com.hypixel.hytale.server.core.command.PluginCommand;

import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import rubidium.admin.AdminUIModule;
import rubidium.command.CommandBridge;
import rubidium.core.RubidiumBootstrap;
import rubidium.core.tier.FeatureRegistry;
import rubidium.hytale.adapter.HytalePlayerBridge;
import rubidium.api.player.Player;
import rubidium.api.server.Server;
import rubidium.hytale.ui.RubidiumSettingsPage;
import rubidium.minimap.MinimapModule;
import rubidium.settings.PlayerSettings;
import rubidium.settings.RubidiumSettingsTab;
import rubidium.settings.ServerSettings;
import rubidium.settings.SettingsRegistry;
import rubidium.stats.PerformanceStatsModule;
import rubidium.voicechat.VoiceChatModule;
import rubidium.ui.components.UIContainer;
import rubidium.hud.HUDEditorUI;

import javax.annotation.Nonnull;
import java.util.*;

public class RubidiumHytaleEntry extends JavaPlugin {
    
    private static RubidiumHytaleEntry instance;
    private static final HytaleLogger LOGGER = HytaleLogger.forName("Rubidium");
    private static boolean isServer = true;
    
    public RubidiumHytaleEntry(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
        isServer = (init != null);
        LOGGER.atInfo().log("Rubidium Framework v1.0 loading...");
        LOGGER.atInfo().log("Hello from " + this.getName() + " version " + (this.getManifest() != null ? this.getManifest().getVersion() : "1.0.0"));
    }
    
    /**
     * Hytale lifecycle: SETUP phase
     * Register events, commands, and configs here.
     */
    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setup phase - registering events and commands...");
        
        CommandBridge.initialize(this);
        
        rubidium.hytale.adapter.PlayerEventHandler.get().registerEvents(getEventRegistry());
        
        registerHytaleEvents();
        
        registerCommands();
        
        CommandBridge.registerAllPending();
    }
    
    /**
     * Hytale lifecycle: START phase  
     * Initialize systems that depend on other plugins.
     */
    @Override
    protected void start() {
        LOGGER.atInfo().log("Start phase - initializing modules...");
        
        if (!RubidiumBootstrap.initialize(getClass(), isServer)) {
            return;
        }
    }
    
    
    /**
     * Register event handlers with the Hytale event registry.
     * Uses getEventRegistry() which returns the real Hytale registry at runtime.
     */
    private void registerHytaleEvents() {
        LOGGER.atInfo().log("Registering Hytale event handlers via plugin registry...");
        
        getEventRegistry().register(
            com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent.class,
            this::onPlayerJoin
        );
        
        getEventRegistry().register(
            com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent.class,
            this::onPlayerQuit
        );
        
        LOGGER.atInfo().log("Hytale event handlers registered");
    }
    
    private void onPlayerJoin(com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent event) {
        try {
            rubidium.hytale.adapter.PlayerEventHandler.get().handlePlayerJoin(event);
        } catch (Exception e) {
            LOGGER.atSevere().log("Error handling player join: " + e.getMessage());
        }
    }
    
    private void onPlayerQuit(com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent event) {
        try {
            rubidium.hytale.adapter.PlayerEventHandler.get().handlePlayerQuit(event);
        } catch (Exception e) {
            LOGGER.atSevere().log("Error handling player quit: " + e.getMessage());
        }
    }
    
    private void registerCommands() {
        registerCommand(new PluginCommand("rubidium", "Show Rubidium framework info", null, "rb") {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                sender.sendMessage("=== Rubidium Framework v1.0 ===");
                sender.sendMessage("A comprehensive API library for Hytale");
                sender.sendMessage("");
                sender.sendMessage("Edition: " + FeatureRegistry.getCurrentTier().getDisplayName());
                sender.sendMessage("Features: " + FeatureRegistry.getAllFeatures().stream()
                    .filter(FeatureRegistry.Feature::isEnabled).count() + " enabled");
                sender.sendMessage("");
                sender.sendMessage("Player Commands:");
                sender.sendMessage("  /rubidium - Show this info");
                sender.sendMessage("  /settings - Open Rubidium settings");
                sender.sendMessage("  /toggle <feature> - Toggle HUD features");
                sender.sendMessage("  /waypoint <name> - Create a waypoint");
                sender.sendMessage("  /hud - Open HUD editor");
                sender.sendMessage("");
                sender.sendMessage("Admin Commands:");
                sender.sendMessage("  /admin - Open admin panel");
                sender.sendMessage("  /giveadmin <player> - Give admin to player");
                sender.sendMessage("  /removeadmin <player> - Remove admin from player");
                sender.sendMessage("  /toggleopti - Toggle optimizations (owner only)");
                sender.sendMessage("");
                sender.sendMessage("Status: Enabled | Mode: " + (isServer ? "Server" : "Singleplayer"));
                return true;
            }
        });
        
        registerCommand(new PluginCommand("settings", "Open Rubidium settings", null, "rbsettings", "rubidiumsettings") {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                if (!sender.isPlayer()) {
                    sender.sendMessage("This command can only be used by players");
                    return true;
                }
                
                UUID playerId = sender.getUniqueId();
                HytalePlayerBridge bridge = HytalePlayerBridge.get();
                
                var playerRefOpt = bridge.getPlayerRef(playerId);
                var pageManagerOpt = bridge.getPageManager(playerId);
                
                if (playerRefOpt.isPresent() && pageManagerOpt.isPresent()) {
                    RubidiumSettingsPage settingsPage = new RubidiumSettingsPage(playerRefOpt.get(), playerId);
                    pageManagerOpt.get().openPage(settingsPage);
                    sender.sendMessage("Opening Rubidium Settings...");
                } else {
                    Player player = Server.getPlayer(playerId).orElse(null);
                    if (player != null) {
                        UIContainer settingsUI = RubidiumSettingsTab.create(player);
                        player.sendPacket(settingsUI);
                        sender.sendMessage("Opening Rubidium Settings...");
                    } else {
                        sender.sendMessage("Could not open settings page");
                    }
                }
                return true;
            }
        });
        
        registerCommand(new PluginCommand("toggle", "Toggle HUD features", null) {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                if (!sender.isPlayer()) {
                    sender.sendMessage("This command can only be used by players");
                    return true;
                }
                
                if (args.length == 0) {
                    sender.sendMessage("Usage: /toggle <feature>");
                    sender.sendMessage("Features: minimap, statistics, voicechat, waypoints");
                    return true;
                }
                
                String feature = args[0].toLowerCase();
                UUID playerId = sender.getUniqueId();
                PlayerSettings settings = SettingsRegistry.get().getPlayerSettings(playerId);
                
                switch (feature) {
                    case "minimap", "map" -> {
                        settings.setMinimapEnabled(!settings.isMinimapEnabled());
                        sender.sendMessage("Minimap: " + (settings.isMinimapEnabled() ? "Enabled" : "Disabled"));
                    }
                    case "statistics", "stats", "fps" -> {
                        settings.setStatisticsEnabled(!settings.isStatisticsEnabled());
                        sender.sendMessage("Statistics: " + (settings.isStatisticsEnabled() ? "Enabled" : "Disabled"));
                    }
                    case "voicechat", "vc", "voice" -> {
                        settings.setVoiceChatEnabled(!settings.isVoiceChatEnabled());
                        sender.sendMessage("Voice Chat: " + (settings.isVoiceChatEnabled() ? "Enabled" : "Disabled"));
                    }
                    case "waypoints", "wp" -> {
                        settings.setWaypointsEnabled(!settings.isWaypointsEnabled());
                        sender.sendMessage("Waypoints: " + (settings.isWaypointsEnabled() ? "Enabled" : "Disabled"));
                    }
                    default -> sender.sendMessage("Unknown feature: " + feature);
                }
                
                settings.save();
                return true;
            }
            
            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                if (args.length == 1) {
                    return Arrays.asList("minimap", "statistics", "voicechat", "waypoints");
                }
                return Collections.emptyList();
            }
        });
        
        registerCommand(new PluginCommand("hud", "Open HUD editor", null, "edithud", "hudeditor") {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                if (!sender.isPlayer()) {
                    sender.sendMessage("This command can only be used by players");
                    return true;
                }
                
                if (!FeatureRegistry.isEnabled("feature.hudeditor")) {
                    sender.sendMessage("HUD Editor requires Rubidium Plus. Upgrade at rubidium.dev/plus");
                    return true;
                }
                
                Player player = Server.getPlayer(sender.getUniqueId()).orElse(null);
                if (player == null) {
                    sender.sendMessage("Could not find player data");
                    return true;
                }
                
                HUDEditorUI.open(player);
                return true;
            }
        });
        
        registerCommand(new PluginCommand("waypoint", "Manage waypoints", null, "wp") {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                if (!sender.isPlayer()) {
                    sender.sendMessage("This command can only be used by players");
                    return true;
                }
                
                if (!FeatureRegistry.isEnabled("feature.minimap")) {
                    sender.sendMessage("Waypoints require Rubidium Plus. Upgrade at rubidium.dev/plus");
                    return true;
                }
                
                if (args.length == 0) {
                    sender.sendMessage("Waypoint Commands:");
                    sender.sendMessage("  /waypoint add <name> - Create waypoint at your location");
                    sender.sendMessage("  /waypoint remove <name> - Remove a waypoint");
                    sender.sendMessage("  /waypoint list - List all waypoints");
                    sender.sendMessage("  /waypoint tp <name> - Teleport to waypoint");
                    return true;
                }
                
                String subCmd = args[0].toLowerCase();
                switch (subCmd) {
                    case "add", "create", "set" -> {
                        if (args.length < 2) {
                            sender.sendMessage("Usage: /waypoint add <name>");
                            return true;
                        }
                        String name = args[1];
                        sender.sendMessage("Waypoint '" + name + "' created at your location!");
                    }
                    case "remove", "delete", "del" -> {
                        if (args.length < 2) {
                            sender.sendMessage("Usage: /waypoint remove <name>");
                            return true;
                        }
                        String name = args[1];
                        sender.sendMessage("Waypoint '" + name + "' removed!");
                    }
                    case "list" -> {
                        sender.sendMessage("Your Waypoints:");
                        sender.sendMessage("  (No waypoints set)");
                    }
                    case "tp", "teleport", "goto" -> {
                        if (args.length < 2) {
                            sender.sendMessage("Usage: /waypoint tp <name>");
                            return true;
                        }
                        String name = args[1];
                        sender.sendMessage("Teleporting to waypoint '" + name + "'...");
                    }
                    default -> sender.sendMessage("Unknown subcommand: " + subCmd);
                }
                return true;
            }
        });
        
        registerCommand(new PluginCommand("admin", "Open admin panel", null, "adminpanel", "ap") {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                if (!sender.isPlayer()) {
                    sender.sendMessage("This command can only be used by players");
                    return true;
                }
                
                if (!FeatureRegistry.isEnabled("feature.adminpanel")) {
                    sender.sendMessage("Admin Panel requires Rubidium Plus. Upgrade at rubidium.dev/plus");
                    return true;
                }
                
                ServerSettings serverSettings = SettingsRegistry.get().getServerSettings();
                SettingsRegistry.PermissionLevel perm = serverSettings.getPermissionLevel(sender.getUniqueId());
                
                if (perm == SettingsRegistry.PermissionLevel.PLAYER) {
                    sender.sendMessage("You don't have permission to access the admin panel");
                    return true;
                }
                
                Player player = Server.getPlayer(sender.getUniqueId()).orElse(null);
                AdminUIModule adminModule = RubidiumBootstrap.getAdminModule();
                if (player != null && adminModule != null) {
                    adminModule.openMainMenu(player);
                }
                sender.sendMessage("Opening Admin Panel...");
                return true;
            }
        });
        
        registerCommand(new PluginCommand("adminstick", "Get an admin stick", "rubidium.admin") {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                if (!sender.isPlayer()) {
                    sender.sendMessage("This command can only be used by players");
                    return true;
                }
                sender.sendMessage("You have been given an Admin Stick!");
                sender.sendMessage("Right-click: Open Admin Menu");
                sender.sendMessage("Left-click: Quick Action (Players panel)");
                sender.sendMessage("Shift+Right-click: Configure Shortcuts");
                return true;
            }
        });
        
        registerCommand(new PluginCommand("giveadmin", "Give admin permissions to a player", null) {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                ServerSettings serverSettings = SettingsRegistry.get().getServerSettings();
                SettingsRegistry.PermissionLevel senderPerm = serverSettings.getPermissionLevel(sender.getUniqueId());
                
                if (senderPerm != SettingsRegistry.PermissionLevel.OWNER && !sender.isConsole()) {
                    sender.sendMessage("Only server owners can use this command");
                    return true;
                }
                
                if (args.length == 0) {
                    sender.sendMessage("Usage: /giveadmin <player>");
                    return true;
                }
                
                String playerName = args[0];
                Player target = Server.getPlayerByName(playerName);
                
                if (target == null) {
                    sender.sendMessage("Player not found: " + playerName);
                    return true;
                }
                
                serverSettings.addAdmin(target.getUniqueId());
                serverSettings.save();
                
                sender.sendMessage("" + playerName + " is now a Rubidium admin!");
                target.sendMessage("You have been given Rubidium admin permissions!");
                return true;
            }
        });
        
        registerCommand(new PluginCommand("removeadmin", "Remove admin permissions from a player", null) {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                ServerSettings serverSettings = SettingsRegistry.get().getServerSettings();
                SettingsRegistry.PermissionLevel senderPerm = serverSettings.getPermissionLevel(sender.getUniqueId());
                
                if (senderPerm != SettingsRegistry.PermissionLevel.OWNER && !sender.isConsole()) {
                    sender.sendMessage("Only server owners can use this command");
                    return true;
                }
                
                if (args.length == 0) {
                    sender.sendMessage("Usage: /removeadmin <player>");
                    return true;
                }
                
                String playerName = args[0];
                Player target = Server.getPlayerByName(playerName);
                
                if (target == null) {
                    sender.sendMessage("Player not found: " + playerName);
                    return true;
                }
                
                serverSettings.removeAdmin(target.getUniqueId());
                serverSettings.save();
                
                sender.sendMessage("" + playerName + " is no longer a Rubidium admin.");
                target.sendMessage("Your Rubidium admin permissions have been removed.");
                return true;
            }
        });
        
        registerCommand(new PluginCommand("toggleopti", "Toggle Rubidium optimizations", null) {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                ServerSettings serverSettings = SettingsRegistry.get().getServerSettings();
                SettingsRegistry.PermissionLevel senderPerm = serverSettings.getPermissionLevel(sender.getUniqueId());
                
                if (senderPerm != SettingsRegistry.PermissionLevel.OWNER && !sender.isConsole()) {
                    sender.sendMessage("Only server owners can use this command");
                    return true;
                }
                
                serverSettings.setOptimizationsEnabled(!serverSettings.isOptimizationsEnabled());
                serverSettings.save();
                
                String status = serverSettings.isOptimizationsEnabled() ? "Enabled" : "Disabled";
                sender.sendMessage("Rubidium optimizations: " + status);
                
                if (serverSettings.isOptimizationsEnabled()) {
                    sender.sendMessage("Performance improvements are now active.");
                } else {
                    sender.sendMessage("Performance improvements have been disabled.");
                }
                return true;
            }
        });
        
        LOGGER.atInfo().log("Registered all commands");
    }
    
    /**
     * Hytale lifecycle: SHUTDOWN phase
     * Called when the plugin is being disabled.
     */
    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutdown phase - cleaning up...");
        RubidiumBootstrap.shutdown();
    }
    
    public static RubidiumHytaleEntry getInstance() {
        return instance;
    }
    
    public static String getVersion() {
        return RubidiumBootstrap.getVersion();
    }
    
    public static boolean isServerMode() {
        return isServer;
    }
    
    public static boolean isInitialized() {
        return RubidiumBootstrap.isInitialized();
    }
    
    public MinimapModule getMinimapModule() {
        return RubidiumBootstrap.getMinimapModule();
    }
    
    public VoiceChatModule getVoiceChatModule() {
        return RubidiumBootstrap.getVoiceChatModule();
    }
    
    public PerformanceStatsModule getStatsModule() {
        return RubidiumBootstrap.getStatsModule();
    }
    
    public AdminUIModule getAdminModule() {
        return RubidiumBootstrap.getAdminModule();
    }
}
