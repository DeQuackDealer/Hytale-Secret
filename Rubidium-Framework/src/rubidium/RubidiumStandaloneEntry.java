package rubidium;

import rubidium.core.RubidiumBootstrap;
import rubidium.core.tier.FeatureRegistry;
import rubidium.hytale.api.JavaPlugin;
import rubidium.api.command.CommandAPI;
import rubidium.settings.PlayerSettings;
import rubidium.settings.SettingsRegistry;

import java.util.UUID;
import java.util.logging.Logger;

public class RubidiumStandaloneEntry extends JavaPlugin {
    
    private static final Logger LOGGER = Logger.getLogger("Rubidium-Standalone");
    private static RubidiumStandaloneEntry instance;
    private static final UUID CONSOLE_UUID = UUID.nameUUIDFromBytes("console".getBytes());
    
    @Override
    public void onLoad() {
        instance = this;
        LOGGER.info("[Rubidium] Standalone mode - preparing to initialize...");
    }
    
    @Override
    public void onEnable() {
        RubidiumBootstrap.initialize(getClass(), true);
        registerCommands();
        LOGGER.info("[Rubidium] Standalone mode enabled!");
    }
    
    private void registerCommands() {
        CommandAPI.register(CommandAPI.create("rubidium")
            .description("Show Rubidium framework info")
            .aliases("rb")
            .executor(ctx -> {
                sendMessage(ctx.sender(), "=== Rubidium Framework v" + RubidiumBootstrap.getVersion() + " ===");
                sendMessage(ctx.sender(), "A comprehensive API library for Hytale");
                sendMessage(ctx.sender(), "");
                sendMessage(ctx.sender(), "Edition: " + FeatureRegistry.getCurrentTier().getDisplayName());
                sendMessage(ctx.sender(), "Features: " + FeatureRegistry.getAllFeatures().stream()
                    .filter(FeatureRegistry.Feature::isEnabled).count() + "/" + 
                    FeatureRegistry.getAllFeatures().size() + " enabled");
                sendMessage(ctx.sender(), "");
                sendMessage(ctx.sender(), "Player Commands:");
                sendMessage(ctx.sender(), "  /rubidium - Show this info");
                sendMessage(ctx.sender(), "  /settings - Open Rubidium settings");
                sendMessage(ctx.sender(), "  /toggle <feature> - Toggle HUD features");
                sendMessage(ctx.sender(), "  /waypoint <action> - Manage waypoints");
                sendMessage(ctx.sender(), "  /hud - Open HUD editor");
                sendMessage(ctx.sender(), "");
                sendMessage(ctx.sender(), "Admin Commands:");
                sendMessage(ctx.sender(), "  /admin - Open admin panel");
                sendMessage(ctx.sender(), "  /toggleopti - Toggle optimizations");
                sendMessage(ctx.sender(), "");
                sendMessage(ctx.sender(), "Mode: Standalone");
                sendMessage(ctx.sender(), "Status: " + (RubidiumBootstrap.isInitialized() ? "Enabled" : "Disabled"));
                return true;
            }));
        
        CommandAPI.register(CommandAPI.create("rbversion")
            .description("Show Rubidium version")
            .executor(ctx -> {
                sendMessage(ctx.sender(), "Rubidium v" + RubidiumBootstrap.getVersion());
                sendMessage(ctx.sender(), "Edition: " + FeatureRegistry.getCurrentTier().getDisplayName());
                return true;
            }));
        
        CommandAPI.register(CommandAPI.create("settings")
            .description("Open Rubidium settings")
            .aliases("rbsettings", "rubidiumsettings")
            .executor(ctx -> {
                sendMessage(ctx.sender(), "=== Rubidium Settings ===");
                PlayerSettings settings = SettingsRegistry.get().getPlayerSettings(CONSOLE_UUID);
                sendMessage(ctx.sender(), "Minimap: " + (settings.isMinimapEnabled() ? "Enabled" : "Disabled"));
                sendMessage(ctx.sender(), "Statistics: " + (settings.isStatisticsEnabled() ? "Enabled" : "Disabled"));
                sendMessage(ctx.sender(), "Voice Chat: " + (settings.isVoiceChatEnabled() ? "Enabled" : "Disabled"));
                sendMessage(ctx.sender(), "Waypoints: " + (settings.isWaypointsEnabled() ? "Enabled" : "Disabled"));
                sendMessage(ctx.sender(), "");
                sendMessage(ctx.sender(), "Use /toggle <feature> to change settings");
                return true;
            }));
        
        CommandAPI.register(CommandAPI.create("toggle")
            .description("Toggle HUD features")
            .executor(ctx -> {
                if (!ctx.hasArg(0)) {
                    sendMessage(ctx.sender(), "Usage: /toggle <feature>");
                    sendMessage(ctx.sender(), "Features: minimap, statistics, voicechat, waypoints");
                    return true;
                }
                
                String feature = ctx.arg(0).toLowerCase();
                PlayerSettings settings = SettingsRegistry.get().getPlayerSettings(CONSOLE_UUID);
                
                switch (feature) {
                    case "minimap", "map" -> {
                        settings.setMinimapEnabled(!settings.isMinimapEnabled());
                        sendMessage(ctx.sender(), "Minimap: " + (settings.isMinimapEnabled() ? "Enabled" : "Disabled"));
                    }
                    case "statistics", "stats", "fps" -> {
                        settings.setStatisticsEnabled(!settings.isStatisticsEnabled());
                        sendMessage(ctx.sender(), "Statistics: " + (settings.isStatisticsEnabled() ? "Enabled" : "Disabled"));
                    }
                    case "voicechat", "vc", "voice" -> {
                        settings.setVoiceChatEnabled(!settings.isVoiceChatEnabled());
                        sendMessage(ctx.sender(), "Voice Chat: " + (settings.isVoiceChatEnabled() ? "Enabled" : "Disabled"));
                    }
                    case "waypoints", "wp" -> {
                        settings.setWaypointsEnabled(!settings.isWaypointsEnabled());
                        sendMessage(ctx.sender(), "Waypoints: " + (settings.isWaypointsEnabled() ? "Enabled" : "Disabled"));
                    }
                    default -> sendMessage(ctx.sender(), "Unknown feature: " + feature);
                }
                return true;
            }));
        
        CommandAPI.register(CommandAPI.create("waypoint")
            .description("Manage waypoints")
            .aliases("wp")
            .executor(ctx -> {
                if (!FeatureRegistry.isEnabled("feature.minimap")) {
                    sendMessage(ctx.sender(), "Waypoints require Rubidium Plus. Upgrade at rubidium.dev/plus");
                    return true;
                }
                
                if (!ctx.hasArg(0)) {
                    sendMessage(ctx.sender(), "Waypoint Commands:");
                    sendMessage(ctx.sender(), "  /waypoint add <name> - Create waypoint");
                    sendMessage(ctx.sender(), "  /waypoint remove <name> - Remove waypoint");
                    sendMessage(ctx.sender(), "  /waypoint list - List all waypoints");
                    return true;
                }
                
                String action = ctx.arg(0).toLowerCase();
                switch (action) {
                    case "add", "create" -> {
                        String name = ctx.hasArg(1) ? ctx.arg(1) : "Unnamed";
                        sendMessage(ctx.sender(), "Waypoint '" + name + "' created!");
                    }
                    case "remove", "delete" -> {
                        if (!ctx.hasArg(1)) {
                            sendMessage(ctx.sender(), "Usage: /waypoint remove <name>");
                            return true;
                        }
                        sendMessage(ctx.sender(), "Waypoint '" + ctx.arg(1) + "' removed!");
                    }
                    case "list" -> sendMessage(ctx.sender(), "No waypoints set.");
                    default -> sendMessage(ctx.sender(), "Unknown action: " + action);
                }
                return true;
            }));
        
        CommandAPI.register(CommandAPI.create("hud")
            .description("Open HUD editor")
            .aliases("edithud", "hudeditor")
            .executor(ctx -> {
                if (!FeatureRegistry.isEnabled("feature.hudeditor")) {
                    sendMessage(ctx.sender(), "HUD Editor requires Rubidium Plus. Upgrade at rubidium.dev/plus");
                    return true;
                }
                sendMessage(ctx.sender(), "HUD Editor opened (simulated in standalone mode)");
                return true;
            }));
        
        CommandAPI.register(CommandAPI.create("admin")
            .description("Open admin panel")
            .aliases("adminpanel", "ap")
            .executor(ctx -> {
                if (!FeatureRegistry.isEnabled("feature.adminpanel")) {
                    sendMessage(ctx.sender(), "Admin Panel requires Rubidium Plus. Upgrade at rubidium.dev/plus");
                    return true;
                }
                sendMessage(ctx.sender(), "Admin Panel opened (simulated in standalone mode)");
                return true;
            }));
        
        CommandAPI.register(CommandAPI.create("toggleopti")
            .description("Toggle Rubidium optimizations")
            .executor(ctx -> {
                var serverSettings = SettingsRegistry.get().getServerSettings();
                serverSettings.setOptimizationsEnabled(!serverSettings.isOptimizationsEnabled());
                String status = serverSettings.isOptimizationsEnabled() ? "Enabled" : "Disabled";
                sendMessage(ctx.sender(), "Rubidium optimizations: " + status);
                return true;
            }));
        
        LOGGER.info("[Rubidium] Standalone commands registered (" + CommandAPI.all().size() + " commands)");
    }
    
    private void sendMessage(Object sender, String message) {
        try {
            java.lang.reflect.Method sendMethod = sender.getClass().getMethod("sendMessage", String.class);
            sendMethod.invoke(sender, message);
        } catch (Exception e) {
            LOGGER.info("[MSG] " + message);
        }
    }
    
    @Override
    public void onDisable() {
        RubidiumBootstrap.shutdown();
    }
    
    public static RubidiumStandaloneEntry getInstance() {
        return instance;
    }
    
    public static String getRubidiumVersion() {
        return RubidiumBootstrap.getVersion();
    }
    
    public static boolean isRubidiumInitialized() {
        return RubidiumBootstrap.isInitialized();
    }
    
    public static void main(String[] args) {
        LOGGER.info("========================================");
        LOGGER.info("  Rubidium Framework - Standalone Mode");
        LOGGER.info("========================================");
        
        RubidiumStandaloneEntry entry = new RubidiumStandaloneEntry();
        entry.onLoad();
        entry.onEnable();
        
        LOGGER.info("");
        LOGGER.info("Framework initialized successfully!");
        LOGGER.info("Edition: " + FeatureRegistry.getCurrentTier().getDisplayName());
        LOGGER.info("Features: " + FeatureRegistry.getAllFeatures().stream()
            .filter(FeatureRegistry.Feature::isEnabled).count() + " enabled");
        LOGGER.info("Commands: " + CommandAPI.all().size() + " registered");
        LOGGER.info("");
        LOGGER.info("Available commands:");
        for (var cmd : CommandAPI.all()) {
            LOGGER.info("  /" + cmd.getName() + " - " + cmd.getDescription());
        }
        LOGGER.info("");
        LOGGER.info("Press Ctrl+C to exit");
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down Rubidium...");
            entry.onDisable();
        }));
        
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            entry.onDisable();
        }
    }
}
