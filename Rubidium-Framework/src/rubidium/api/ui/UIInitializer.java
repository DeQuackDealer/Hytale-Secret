package rubidium.api.ui;

import com.hypixel.hytale.server.api.player.Player;
import rubidium.api.ui.overlays.MinimapOverlay;
import rubidium.api.ui.overlays.StatsOverlay;
import rubidium.api.ui.overlays.VoiceChatOverlay;
import rubidium.core.tier.FeatureRegistry;
import rubidium.features.minimap.MinimapManager;
import rubidium.features.voicechat.VoiceChatManager;
import rubidium.settings.PlayerSettings;
import rubidium.settings.SettingsRegistry;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class UIInitializer {
    
    private static final Logger LOGGER = Logger.getLogger("Rubidium-UI");
    private static UIInitializer instance;
    
    private final Map<UUID, MinimapOverlay> minimapOverlays = new ConcurrentHashMap<>();
    private final Map<UUID, VoiceChatOverlay> voiceChatOverlays = new ConcurrentHashMap<>();
    private final Map<UUID, StatsOverlay> statsOverlays = new ConcurrentHashMap<>();
    
    public static UIInitializer get() {
        if (instance == null) {
            instance = new UIInitializer();
        }
        return instance;
    }
    
    private UIInitializer() {}
    
    public void onPlayerJoin(Player player) {
        if (player == null) return;
        
        UUID playerId = player.getUUID();
        LOGGER.info("[Rubidium-UI] Initializing UI for player: " + player.getName());
        
        PlayerSettings settings = SettingsRegistry.get().getPlayerSettings(playerId);
        
        if (FeatureRegistry.isEnabled("feature.minimap") && settings.isMinimapEnabled()) {
            try {
                MinimapManager minimapManager = MinimapManager.getOrCreate(playerId);
                MinimapOverlay minimap = new MinimapOverlay(minimapManager);
                minimapOverlays.put(playerId, minimap);
                RubidiumUI.openOverlay(player, minimap);
                LOGGER.fine("[Rubidium-UI] Minimap overlay opened for " + player.getName());
            } catch (Exception e) {
                LOGGER.warning("[Rubidium-UI] Failed to open minimap: " + e.getMessage());
            }
        }
        
        if (FeatureRegistry.isEnabled("feature.voicechat") && settings.isVoiceChatEnabled()) {
            try {
                VoiceChatManager voiceChatManager = VoiceChatManager.getOrCreate(playerId);
                VoiceChatOverlay voiceChat = new VoiceChatOverlay(voiceChatManager);
                voiceChatOverlays.put(playerId, voiceChat);
                RubidiumUI.openOverlay(player, voiceChat);
                LOGGER.fine("[Rubidium-UI] VoiceChat overlay opened for " + player.getName());
            } catch (Exception e) {
                LOGGER.warning("[Rubidium-UI] Failed to open voice chat: " + e.getMessage());
            }
        }
        
        if (FeatureRegistry.isEnabled("feature.statistics") && settings.isStatisticsEnabled()) {
            try {
                StatsOverlay stats = new StatsOverlay();
                statsOverlays.put(playerId, stats);
                RubidiumUI.openOverlay(player, stats);
                LOGGER.fine("[Rubidium-UI] Stats overlay opened for " + player.getName());
            } catch (Exception e) {
                LOGGER.warning("[Rubidium-UI] Failed to open stats: " + e.getMessage());
            }
        }
        
        LOGGER.info("[Rubidium-UI] UI initialization complete for " + player.getName());
    }
    
    public void onPlayerQuit(Player player, UUID playerId) {
        LOGGER.info("[Rubidium-UI] Cleaning up UI for player: " + playerId);
        
        MinimapOverlay minimap = minimapOverlays.remove(playerId);
        if (minimap != null && player != null) {
            RubidiumUI.closeOverlay(player, minimap);
        }
        
        VoiceChatOverlay voiceChat = voiceChatOverlays.remove(playerId);
        if (voiceChat != null && player != null) {
            RubidiumUI.closeOverlay(player, voiceChat);
        }
        
        StatsOverlay stats = statsOverlays.remove(playerId);
        if (stats != null && player != null) {
            RubidiumUI.closeOverlay(player, stats);
        }
        
        RubidiumUI.cleanupPlayer(playerId);
    }
    
    public MinimapOverlay getMinimapOverlay(UUID playerId) {
        return minimapOverlays.get(playerId);
    }
    
    public VoiceChatOverlay getVoiceChatOverlay(UUID playerId) {
        return voiceChatOverlays.get(playerId);
    }
    
    public StatsOverlay getStatsOverlay(UUID playerId) {
        return statsOverlays.get(playerId);
    }
    
    public void toggleMinimap(Player player, boolean enabled) {
        UUID playerId = player.getUUID();
        if (enabled && !minimapOverlays.containsKey(playerId)) {
            MinimapManager minimapManager = MinimapManager.getOrCreate(playerId);
            MinimapOverlay minimap = new MinimapOverlay(minimapManager);
            minimapOverlays.put(playerId, minimap);
            RubidiumUI.openOverlay(player, minimap);
        } else if (!enabled && minimapOverlays.containsKey(playerId)) {
            MinimapOverlay minimap = minimapOverlays.remove(playerId);
            if (minimap != null) {
                RubidiumUI.closeOverlay(player, minimap);
            }
        }
    }
    
    public void toggleVoiceChat(Player player, boolean enabled) {
        UUID playerId = player.getUUID();
        if (enabled && !voiceChatOverlays.containsKey(playerId)) {
            VoiceChatManager voiceChatManager = VoiceChatManager.getOrCreate(playerId);
            VoiceChatOverlay voiceChat = new VoiceChatOverlay(voiceChatManager);
            voiceChatOverlays.put(playerId, voiceChat);
            RubidiumUI.openOverlay(player, voiceChat);
        } else if (!enabled && voiceChatOverlays.containsKey(playerId)) {
            VoiceChatOverlay voiceChat = voiceChatOverlays.remove(playerId);
            if (voiceChat != null) {
                RubidiumUI.closeOverlay(player, voiceChat);
            }
        }
    }
    
    public void toggleStats(Player player, boolean enabled) {
        UUID playerId = player.getUUID();
        if (enabled && !statsOverlays.containsKey(playerId)) {
            StatsOverlay stats = new StatsOverlay();
            statsOverlays.put(playerId, stats);
            RubidiumUI.openOverlay(player, stats);
        } else if (!enabled && statsOverlays.containsKey(playerId)) {
            StatsOverlay stats = statsOverlays.remove(playerId);
            if (stats != null) {
                RubidiumUI.closeOverlay(player, stats);
            }
        }
    }
    
    public void shutdown() {
        LOGGER.info("[Rubidium-UI] Shutting down UI system");
        minimapOverlays.clear();
        voiceChatOverlays.clear();
        statsOverlays.clear();
    }
}
