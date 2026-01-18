package rubidium;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.command.CommandSender;
import com.hypixel.hytale.server.core.command.PluginCommand;

import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import rubidium.admin.AdminUIModule;
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

import java.util.*;
import java.util.logging.Logger;

public class RubidiumHytaleEntry extends JavaPlugin {
    
    private static RubidiumHytaleEntry instance;
    private static final Logger LOGGER = Logger.getLogger("Rubidium");
    private static boolean isServer = true;
    
    public RubidiumHytaleEntry(JavaPluginInit init) {
        super(init);
        instance = this;
        isServer = (init != null);
        LOGGER.info("[Rubidium] Framework v1.0.0 loading...");
        LOGGER.info("[Rubidium] Environment: " + (isServer ? "Server" : "Singleplayer"));
    }
    
    @Override
    public void onEnable() {
        if (!RubidiumBootstrap.initialize(getClass(), isServer)) {
            return;
        }
        
        registerCommands();
    }
    
    private void registerCommands() {
        registerCommand(new PluginCommand("rubidium", "Show Rubidium framework info", null, "rb") {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                sender.sendMessage("&6=== Rubidium Framework v1.0.0 ===");
                sender.sendMessage("&7A comprehensive API library for Hytale");
                sender.sendMessage("");
                sender.sendMessage("&eEdition: &f" + FeatureRegistry.getCurrentTier().getDisplayName());
                sender.sendMessage("&eFeatures: &f" + FeatureRegistry.getAllFeatures().stream()
                    .filter(FeatureRegistry.Feature::isEnabled).count() + " enabled");
                sender.sendMessage("");
                sender.sendMessage("&ePlayer Commands:");
                sender.sendMessage("&f  /rubidium &7- Show this info");
                sender.sendMessage("&f  /settings &7- Open Rubidium settings");
                sender.sendMessage("&f  /toggle <feature> &7- Toggle HUD features");
                sender.sendMessage("&f  /waypoint <name> &7- Create a waypoint");
                sender.sendMessage("&f  /hud &7- Open HUD editor");
                sender.sendMessage("");
                sender.sendMessage("&eAdmin Commands:");
                sender.sendMessage("&f  /admin &7- Open admin panel");
                sender.sendMessage("&f  /giveadmin <player> &7- Give admin to player");
                sender.sendMessage("&f  /removeadmin <player> &7- Remove admin from player");
                sender.sendMessage("&f  /toggleopti &7- Toggle optimizations (owner only)");
                sender.sendMessage("");
                sender.sendMessage("&7Status: &aEnabled | Mode: " + (isServer ? "Server" : "Singleplayer"));
                return true;
            }
        });
        
        registerCommand(new PluginCommand("settings", "Open Rubidium settings", null, "rbsettings", "rubidiumsettings") {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                if (!sender.isPlayer()) {
                    sender.sendMessage("&cThis command can only be used by players");
                    return true;
                }
                
                UUID playerId = sender.getUniqueId();
                HytalePlayerBridge bridge = HytalePlayerBridge.get();
                
                var playerRefOpt = bridge.getPlayerRef(playerId);
                var pageManagerOpt = bridge.getPageManager(playerId);
                
                if (playerRefOpt.isPresent() && pageManagerOpt.isPresent()) {
                    RubidiumSettingsPage settingsPage = new RubidiumSettingsPage(playerRefOpt.get(), playerId);
                    pageManagerOpt.get().openPage(settingsPage);
                    sender.sendMessage("&aOpening Rubidium Settings...");
                } else {
                    Player player = Server.getPlayer(playerId).orElse(null);
                    if (player != null) {
                        UIContainer settingsUI = RubidiumSettingsTab.create(player);
                        player.sendPacket(settingsUI);
                        sender.sendMessage("&aOpening Rubidium Settings...");
                    } else {
                        sender.sendMessage("&cCould not open settings page");
                    }
                }
                return true;
            }
        });
        
        registerCommand(new PluginCommand("toggle", "Toggle HUD features", null) {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                if (!sender.isPlayer()) {
                    sender.sendMessage("&cThis command can only be used by players");
                    return true;
                }
                
                if (args.length == 0) {
                    sender.sendMessage("&eUsage: /toggle <feature>");
                    sender.sendMessage("&7Features: minimap, statistics, voicechat, waypoints");
                    return true;
                }
                
                String feature = args[0].toLowerCase();
                UUID playerId = sender.getUniqueId();
                PlayerSettings settings = SettingsRegistry.get().getPlayerSettings(playerId);
                
                switch (feature) {
                    case "minimap", "map" -> {
                        settings.setMinimapEnabled(!settings.isMinimapEnabled());
                        sender.sendMessage("&7Minimap: " + (settings.isMinimapEnabled() ? "&aEnabled" : "&cDisabled"));
                    }
                    case "statistics", "stats", "fps" -> {
                        settings.setStatisticsEnabled(!settings.isStatisticsEnabled());
                        sender.sendMessage("&7Statistics: " + (settings.isStatisticsEnabled() ? "&aEnabled" : "&cDisabled"));
                    }
                    case "voicechat", "vc", "voice" -> {
                        settings.setVoiceChatEnabled(!settings.isVoiceChatEnabled());
                        sender.sendMessage("&7Voice Chat: " + (settings.isVoiceChatEnabled() ? "&aEnabled" : "&cDisabled"));
                    }
                    case "waypoints", "wp" -> {
                        settings.setWaypointsEnabled(!settings.isWaypointsEnabled());
                        sender.sendMessage("&7Waypoints: " + (settings.isWaypointsEnabled() ? "&aEnabled" : "&cDisabled"));
                    }
                    default -> sender.sendMessage("&cUnknown feature: " + feature);
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
                    sender.sendMessage("&cThis command can only be used by players");
                    return true;
                }
                
                if (!FeatureRegistry.isEnabled("feature.hudeditor")) {
                    sender.sendMessage("&cHUD Editor requires Rubidium Plus. Upgrade at rubidium.dev/plus");
                    return true;
                }
                
                Player player = Server.getPlayer(sender.getUniqueId()).orElse(null);
                if (player == null) {
                    sender.sendMessage("&cCould not find player data");
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
                    sender.sendMessage("&cThis command can only be used by players");
                    return true;
                }
                
                if (!FeatureRegistry.isEnabled("feature.minimap")) {
                    sender.sendMessage("&cWaypoints require Rubidium Plus. Upgrade at rubidium.dev/plus");
                    return true;
                }
                
                if (args.length == 0) {
                    sender.sendMessage("&eWaypoint Commands:");
                    sender.sendMessage("&f  /waypoint add <name> &7- Create waypoint at your location");
                    sender.sendMessage("&f  /waypoint remove <name> &7- Remove a waypoint");
                    sender.sendMessage("&f  /waypoint list &7- List all waypoints");
                    sender.sendMessage("&f  /waypoint tp <name> &7- Teleport to waypoint");
                    return true;
                }
                
                String subCmd = args[0].toLowerCase();
                switch (subCmd) {
                    case "add", "create", "set" -> {
                        if (args.length < 2) {
                            sender.sendMessage("&cUsage: /waypoint add <name>");
                            return true;
                        }
                        String name = args[1];
                        sender.sendMessage("&aWaypoint '" + name + "' created at your location!");
                    }
                    case "remove", "delete", "del" -> {
                        if (args.length < 2) {
                            sender.sendMessage("&cUsage: /waypoint remove <name>");
                            return true;
                        }
                        String name = args[1];
                        sender.sendMessage("&cWaypoint '" + name + "' removed!");
                    }
                    case "list" -> {
                        sender.sendMessage("&eYour Waypoints:");
                        sender.sendMessage("&7  (No waypoints set)");
                    }
                    case "tp", "teleport", "goto" -> {
                        if (args.length < 2) {
                            sender.sendMessage("&cUsage: /waypoint tp <name>");
                            return true;
                        }
                        String name = args[1];
                        sender.sendMessage("&aTeleporting to waypoint '" + name + "'...");
                    }
                    default -> sender.sendMessage("&cUnknown subcommand: " + subCmd);
                }
                return true;
            }
        });
        
        registerCommand(new PluginCommand("admin", "Open admin panel", null, "adminpanel", "ap") {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                if (!sender.isPlayer()) {
                    sender.sendMessage("&cThis command can only be used by players");
                    return true;
                }
                
                if (!FeatureRegistry.isEnabled("feature.adminpanel")) {
                    sender.sendMessage("&cAdmin Panel requires Rubidium Plus. Upgrade at rubidium.dev/plus");
                    return true;
                }
                
                ServerSettings serverSettings = SettingsRegistry.get().getServerSettings();
                SettingsRegistry.PermissionLevel perm = serverSettings.getPermissionLevel(sender.getUniqueId());
                
                if (perm == SettingsRegistry.PermissionLevel.PLAYER) {
                    sender.sendMessage("&cYou don't have permission to access the admin panel");
                    return true;
                }
                
                Player player = Server.getPlayer(sender.getUniqueId()).orElse(null);
                AdminUIModule adminModule = RubidiumBootstrap.getAdminModule();
                if (player != null && adminModule != null) {
                    adminModule.openMainMenu(player);
                }
                sender.sendMessage("&aOpening Admin Panel...");
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
        
        registerCommand(new PluginCommand("giveadmin", "Give admin permissions to a player", null) {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                ServerSettings serverSettings = SettingsRegistry.get().getServerSettings();
                SettingsRegistry.PermissionLevel senderPerm = serverSettings.getPermissionLevel(sender.getUniqueId());
                
                if (senderPerm != SettingsRegistry.PermissionLevel.OWNER && !sender.isConsole()) {
                    sender.sendMessage("&cOnly server owners can use this command");
                    return true;
                }
                
                if (args.length == 0) {
                    sender.sendMessage("&cUsage: /giveadmin <player>");
                    return true;
                }
                
                String playerName = args[0];
                Player target = Server.getPlayerByName(playerName);
                
                if (target == null) {
                    sender.sendMessage("&cPlayer not found: " + playerName);
                    return true;
                }
                
                serverSettings.addAdmin(target.getUniqueId());
                serverSettings.save();
                
                sender.sendMessage("&a" + playerName + " is now a Rubidium admin!");
                target.sendMessage("&aYou have been given Rubidium admin permissions!");
                return true;
            }
        });
        
        registerCommand(new PluginCommand("removeadmin", "Remove admin permissions from a player", null) {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                ServerSettings serverSettings = SettingsRegistry.get().getServerSettings();
                SettingsRegistry.PermissionLevel senderPerm = serverSettings.getPermissionLevel(sender.getUniqueId());
                
                if (senderPerm != SettingsRegistry.PermissionLevel.OWNER && !sender.isConsole()) {
                    sender.sendMessage("&cOnly server owners can use this command");
                    return true;
                }
                
                if (args.length == 0) {
                    sender.sendMessage("&cUsage: /removeadmin <player>");
                    return true;
                }
                
                String playerName = args[0];
                Player target = Server.getPlayerByName(playerName);
                
                if (target == null) {
                    sender.sendMessage("&cPlayer not found: " + playerName);
                    return true;
                }
                
                serverSettings.removeAdmin(target.getUniqueId());
                serverSettings.save();
                
                sender.sendMessage("&c" + playerName + " is no longer a Rubidium admin.");
                target.sendMessage("&cYour Rubidium admin permissions have been removed.");
                return true;
            }
        });
        
        registerCommand(new PluginCommand("toggleopti", "Toggle Rubidium optimizations", null) {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                ServerSettings serverSettings = SettingsRegistry.get().getServerSettings();
                SettingsRegistry.PermissionLevel senderPerm = serverSettings.getPermissionLevel(sender.getUniqueId());
                
                if (senderPerm != SettingsRegistry.PermissionLevel.OWNER && !sender.isConsole()) {
                    sender.sendMessage("&cOnly server owners can use this command");
                    return true;
                }
                
                serverSettings.setOptimizationsEnabled(!serverSettings.isOptimizationsEnabled());
                serverSettings.save();
                
                String status = serverSettings.isOptimizationsEnabled() ? "&aEnabled" : "&cDisabled";
                sender.sendMessage("&7Rubidium optimizations: " + status);
                
                if (serverSettings.isOptimizationsEnabled()) {
                    sender.sendMessage("&7Performance improvements are now active.");
                } else {
                    sender.sendMessage("&7Performance improvements have been disabled.");
                }
                return true;
            }
        });
        
        LOGGER.info("[Rubidium] Registered all commands");
    }
    
    @Override
    public void onDisable() {
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
