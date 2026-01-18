package rubidium.core;

import rubidium.admin.AdminUIModule;
import rubidium.api.npc.NPCAPI;
import rubidium.api.scheduler.SchedulerAPI;
import rubidium.core.tier.FeatureRegistry;
import rubidium.core.tier.ProductTier;
import rubidium.hud.HUDRegistry;
import rubidium.minimap.MinimapModule;
import rubidium.settings.SettingsRegistry;
import rubidium.stats.PerformanceStatsModule;
import rubidium.voicechat.VoiceChatModule;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Logger;

public final class RubidiumBootstrap {
    
    private static final Logger LOGGER = Logger.getLogger("Rubidium");
    private static boolean initialized = false;
    private static ProductTier currentTier = ProductTier.FREE;
    
    private static AdminUIModule adminModule;
    private static MinimapModule minimapModule;
    private static VoiceChatModule voiceChatModule;
    private static PerformanceStatsModule statsModule;
    
    private RubidiumBootstrap() {}
    
    public static synchronized boolean initialize(Class<?> entryClass, boolean isServer) {
        if (initialized) {
            LOGGER.warning("[Rubidium] Already initialized, skipping...");
            return false;
        }
        
        LOGGER.info("[Rubidium] ==========================================");
        LOGGER.info("[Rubidium]  Rubidium Framework v1.0.0");
        LOGGER.info("[Rubidium] ==========================================");
        LOGGER.info("[Rubidium] Mode: " + (isServer ? "Server/Singleplayer" : "Client"));
        
        currentTier = detectProductTier(entryClass);
        LOGGER.info("[Rubidium] Detected edition: " + currentTier.getDisplayName());
        
        FeatureRegistry.initialize(currentTier);
        LOGGER.info("[Rubidium] Feature registry initialized with " + FeatureRegistry.getAllFeatures().size() + " features");
        
        initModules();
        registerSettingsCategory();
        
        if (isServer) {
            initServerFeatures();
        }
        
        initialized = true;
        logAvailableFeatures();
        
        LOGGER.info("[Rubidium] Framework v1.0.0 enabled!");
        return true;
    }
    
    public static synchronized void shutdown() {
        if (!initialized) return;
        
        LOGGER.info("[Rubidium] Framework disabling...");
        
        if (statsModule != null) statsModule.onDisable();
        if (minimapModule != null) minimapModule.onDisable();
        if (voiceChatModule != null) voiceChatModule.onDisable();
        if (adminModule != null) adminModule.onDisable();
        
        SchedulerAPI.shutdown();
        
        initialized = false;
        LOGGER.info("[Rubidium] Framework disabled.");
    }
    
    private static ProductTier detectProductTier(Class<?> entryClass) {
        try {
            File jarFile = new File(entryClass.getProtectionDomain().getCodeSource().getLocation().toURI());
            
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
    
    private static void initModules() {
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
    
    private static void registerSettingsCategory() {
        SettingsRegistry.get().registerCategory(new SettingsRegistry.SettingCategory(
            "rubidium", "Rubidium", "rubidium_icon", SettingsRegistry.PermissionLevel.PLAYER
        ));
        LOGGER.info("[Rubidium] Rubidium settings tab registered");
    }
    
    private static void initServerFeatures() {
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
    
    private static void logAvailableFeatures() {
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
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
    
    public static ProductTier getCurrentTier() {
        return currentTier;
    }
    
    public static String getVersion() {
        return "1.0.0";
    }
    
    public static MinimapModule getMinimapModule() {
        return minimapModule;
    }
    
    public static VoiceChatModule getVoiceChatModule() {
        return voiceChatModule;
    }
    
    public static PerformanceStatsModule getStatsModule() {
        return statsModule;
    }
    
    public static AdminUIModule getAdminModule() {
        return adminModule;
    }
}
