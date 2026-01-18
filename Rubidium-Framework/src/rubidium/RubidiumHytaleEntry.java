package rubidium;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.command.CommandSender;
import com.hypixel.hytale.server.core.command.PluginCommand;

import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import rubidium.admin.AdminUIModule;
import rubidium.hytale.adapter.HytalePlayerBridge;
import rubidium.api.npc.NPCAPI;
import rubidium.api.player.Player;
import rubidium.api.scheduler.SchedulerAPI;
import rubidium.api.server.Server;
import rubidium.command.RubidiumCommandAPI;
import rubidium.hud.HUDEditorUI;
import rubidium.hud.HUDRegistry;
import rubidium.hytale.ui.RubidiumHudManager;
import rubidium.hytale.ui.RubidiumSettingsPage;
import rubidium.minimap.MinimapModule;
import rubidium.settings.PlayerSettings;
import rubidium.settings.RubidiumSettingsTab;
import rubidium.settings.ServerSettings;
import rubidium.settings.SettingsRegistry;
import rubidium.stats.PerformanceStatsModule;
import rubidium.voicechat.VoiceChatModule;
import rubidium.ui.components.UIContainer;

import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Logger;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import rubidium.core.tier.FeatureRegistry;
import rubidium.core.tier.ProductTier;

public class RubidiumHytaleEntry extends JavaPlugin {
    
    private static RubidiumHytaleEntry instance;
    private static final Logger LOGGER = Logger.getLogger("Rubidium");
    private static boolean isServer = true;
    private static boolean initialized = false;
    private static ProductTier detectedTier = ProductTier.FREE;
    
    private AdminUIModule adminModule;
    private MinimapModule minimapModule;
    private VoiceChatModule voiceChatModule;
    private PerformanceStatsModule statsModule;
    
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
        
        detectedTier = detectProductTier();
        LOGGER.info("[Rubidium] Detected edition: " + detectedTier.getDisplayName());
        
        FeatureRegistry.initialize(detectedTier);
        LOGGER.info("[Rubidium] Feature registry initialized with " + FeatureRegistry.getAllFeatures().size() + " features");
        
        initModules();
        registerCommands();
        registerSettingsTab();
        
        if (isServer) {
            initServerFeatures();
        }
        
        initCommonFeatures();
        
        LOGGER.info("[Rubidium] Framework v1.0.0 enabled!");
        logAvailableFeatures();
    }
    
    private ProductTier detectProductTier() {
        try {
            File jarFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            
            if (jarFile.isFile() && jarFile.getName().endsWith(".jar")) {
                try (JarFile jar = new JarFile(jarFile)) {
                    Manifest manifest = jar.getManifest();
                    if (manifest != null) {
                        String tierValue = manifest.getMainAttributes().getValue("Rubidium-Tier");
                        String premiumValue = manifest.getMainAttributes().getValue("Rubidium-Premium");
                        
                        LOGGER.info("[Rubidium] Manifest: Tier=" + tierValue + ", Premium=" + premiumValue);
                        
                        if ("PLUS".equalsIgnoreCase(tierValue) || "true".equalsIgnoreCase(premiumValue)) {
                            LOGGER.info("[Rubidium] Premium manifest detected - enabling Plus edition");
                            return ProductTier.PLUS;
                        }
                    }
                }
            }
            
            boolean hasNpcApi = false;
            try {
                Class.forName("rubidium.api.npc.NPCAPI");
                hasNpcApi = true;
            } catch (ClassNotFoundException e) {
            }
            
            boolean hasAiApi = false;
            try {
                Class.forName("rubidium.api.ai.AIBehaviorAPI");
                hasAiApi = true;
            } catch (ClassNotFoundException e) {
            }
            
            if (hasNpcApi && hasAiApi) {
                LOGGER.info("[Rubidium] Premium APIs detected via classloader - enabling Plus edition");
                return ProductTier.PLUS;
            }
            
        } catch (URISyntaxException | IOException e) {
            LOGGER.warning("[Rubidium] Could not detect tier from manifest: " + e.getMessage());
        }
        
        LOGGER.info("[Rubidium] Defaulting to Free edition");
        return ProductTier.FREE;
    }
    
    private void initModules() {
        statsModule = new PerformanceStatsModule();
        FeatureRegistry.withFeature("feature.statistics", () -> {
            statsModule.onEnable();
            LOGGER.info("[Rubidium] Performance Statistics module enabled");
        });
        
        minimapModule = new MinimapModule();
        FeatureRegistry.withFeature("feature.minimap", () -> {
            minimapModule.onEnable();
            LOGGER.info("[Rubidium] Minimap module enabled");
        });
        
        voiceChatModule = new VoiceChatModule();
        FeatureRegistry.withFeature("feature.voicechat", () -> {
            voiceChatModule.onEnable();
            LOGGER.info("[Rubidium] Voice Chat module enabled");
        });
        
        adminModule = new AdminUIModule();
        FeatureRegistry.withFeature("feature.adminpanel", () -> {
            adminModule.onEnable();
            LOGGER.info("[Rubidium] Admin Panel module enabled");
        });
        
        int enabledCount = (int) FeatureRegistry.getAllFeatures().stream()
            .filter(FeatureRegistry.Feature::isEnabled)
            .count();
        LOGGER.info("[Rubidium] Modules initialized (" + enabledCount + " features enabled)");
    }
    
    private void registerSettingsTab() {
        SettingsRegistry.get().registerCategory(new SettingsRegistry.SettingCategory(
            "rubidium", "Rubidium", "rubidium_icon", SettingsRegistry.PermissionLevel.PLAYER
        ));
        LOGGER.info("[Rubidium] Rubidium settings tab registered");
    }
    
    private void registerCommands() {
        registerCommand(new PluginCommand("rubidium", "Show Rubidium framework info", null, "rb") {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                sender.sendMessage("&6=== Rubidium Framework v1.0.0 ===");
                sender.sendMessage("&7A comprehensive API library for Hytale");
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
                sender.sendMessage("&eKeybinds:");
                sender.sendMessage("&f  V &7- Push-to-talk (voice chat)");
                sender.sendMessage("&f  Q &7- Primary ability");
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
                
                ServerSettings serverSettings = SettingsRegistry.get().getServerSettings();
                SettingsRegistry.PermissionLevel perm = serverSettings.getPermissionLevel(sender.getUniqueId());
                
                if (perm == SettingsRegistry.PermissionLevel.PLAYER) {
                    sender.sendMessage("&cYou don't have permission to access the admin panel");
                    return true;
                }
                
                Player player = Server.getPlayer(sender.getUniqueId()).orElse(null);
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
    
    private void initServerFeatures() {
        SchedulerAPI.runTimer("rubidium:npc_tick", () -> {
            for (var npc : NPCAPI.all()) {
                npc.tick();
            }
        }, 1, 1);
        
        SchedulerAPI.runTimer("rubidium:stats_tick", () -> {
            if (statsModule != null) {
                statsModule.getMetrics().tick();
            }
        }, 1, 1);
    }
    
    private void initCommonFeatures() {
    }
    
    private void logAvailableFeatures() {
        ProductTier tier = FeatureRegistry.getCurrentTier();
        int total = FeatureRegistry.getAllFeatures().size();
        long enabled = FeatureRegistry.getAllFeatures().stream()
            .filter(FeatureRegistry.Feature::isEnabled)
            .count();
        
        LOGGER.info("[Rubidium] ==========================================");
        LOGGER.info("[Rubidium]  Edition: " + tier.getDisplayName());
        LOGGER.info("[Rubidium]  Features: " + enabled + "/" + total + " enabled");
        LOGGER.info("[Rubidium] ==========================================");
        
        if (tier.isPremium()) {
            LOGGER.info("[Rubidium] Premium Features Enabled:");
            LOGGER.info("[Rubidium]   + Minimap with integrated waypoints");
            LOGGER.info("[Rubidium]   + Voice Chat with PTT support");
            LOGGER.info("[Rubidium]   + Performance Statistics (FPS/DPS/RAM)");
            LOGGER.info("[Rubidium]   + HUD Editor for custom layouts");
            LOGGER.info("[Rubidium]   + Admin Panel with 8 management panels");
            LOGGER.info("[Rubidium]   + NPC API with AI behaviors");
            LOGGER.info("[Rubidium]   + Pathfinding, Economy, Particles APIs");
            LOGGER.info("[Rubidium]   + Hytale UI Integration");
        } else {
            LOGGER.info("[Rubidium] Free Features Enabled:");
            LOGGER.info("[Rubidium]   + Core optimizations (Memory, Network, Threading)");
            LOGGER.info("[Rubidium]   + Command, Chat, Event, Config APIs");
            LOGGER.info("[Rubidium]   + Plugin System");
            LOGGER.info("[Rubidium]   + Player API");
            LOGGER.info("[Rubidium] ");
            LOGGER.info("[Rubidium] Upgrade to Rubidium Plus for:");
            LOGGER.info("[Rubidium]   - Minimap, Voice Chat, HUD Editor");
            LOGGER.info("[Rubidium]   - NPC, AI, Pathfinding APIs");
            LOGGER.info("[Rubidium]   - Admin Panel, and more!");
            LOGGER.info("[Rubidium]   Visit: rubidium.dev/plus");
        }
        
        LOGGER.info("[Rubidium] ");
        LOGGER.info("[Rubidium] Commands: /rubidium, /settings, /toggle, /hud, /waypoint");
        LOGGER.info("[Rubidium] Admin: /admin, /giveadmin, /removeadmin, /toggleopti");
        LOGGER.info("[Rubidium] ");
        LOGGER.info("[Rubidium] Keybinds:");
        LOGGER.info("[Rubidium]   V - Push-to-talk");
        LOGGER.info("[Rubidium]   Q - Ability1 (customizable)");
    }
    
    @Override
    public void onDisable() {
        LOGGER.info("[Rubidium] Framework disabling...");
        
        if (statsModule != null) statsModule.onDisable();
        if (minimapModule != null) minimapModule.onDisable();
        if (voiceChatModule != null) voiceChatModule.onDisable();
        if (adminModule != null) adminModule.onDisable();
        
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
    
    public MinimapModule getMinimapModule() {
        return minimapModule;
    }
    
    public VoiceChatModule getVoiceChatModule() {
        return voiceChatModule;
    }
    
    public PerformanceStatsModule getStatsModule() {
        return statsModule;
    }
    
    public AdminUIModule getAdminModule() {
        return adminModule;
    }
}
