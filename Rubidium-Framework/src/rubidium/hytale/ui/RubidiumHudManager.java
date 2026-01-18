package rubidium.hytale.ui;

import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.protocol.packets.interface_.HudComponent;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

import rubidium.settings.PlayerSettings;
import rubidium.settings.SettingsRegistry;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class RubidiumHudManager {
    
    private static final Logger LOGGER = Logger.getLogger("Rubidium-HUD");
    private static RubidiumHudManager instance;
    
    private final Map<UUID, HudManager> playerHuds = new ConcurrentHashMap<>();
    private final Map<UUID, RubidiumCustomHud> customHuds = new ConcurrentHashMap<>();
    
    public static RubidiumHudManager get() {
        if (instance == null) {
            instance = new RubidiumHudManager();
        }
        return instance;
    }
    
    public void initializePlayer(PlayerRef playerRef, UUID playerId, HudManager hudManager) {
        LOGGER.info("[Rubidium] Initializing HUD for player: " + playerId);
        
        playerHuds.put(playerId, hudManager);
        
        PlayerSettings settings = SettingsRegistry.get().getPlayerSettings(playerId);
        
        RubidiumCustomHud customHud = new RubidiumCustomHud(playerRef, playerId);
        customHuds.put(playerId, customHud);
        
        hudManager.setCustomHud(playerRef, customHud);
        
        updatePlayerHud(playerRef, playerId);
        
        LOGGER.info("[Rubidium] HUD initialized for: " + playerId);
    }
    
    public void updatePlayerHud(PlayerRef playerRef, UUID playerId) {
        HudManager hudManager = playerHuds.get(playerId);
        if (hudManager == null) {
            LOGGER.warning("[Rubidium] No HUD manager for player: " + playerId);
            return;
        }
        
        PlayerSettings settings = SettingsRegistry.get().getPlayerSettings(playerId);
        RubidiumCustomHud customHud = customHuds.get(playerId);
        
        if (customHud != null) {
            customHud.setMinimapEnabled(settings.isMinimapEnabled());
            customHud.setStatsEnabled(settings.isStatisticsEnabled());
            customHud.setVoiceChatEnabled(settings.isVoiceChatEnabled());
            customHud.setWaypointsEnabled(settings.isWaypointsEnabled());
            customHud.refresh(playerRef);
        }
        
        if (settings.isMinimapEnabled()) {
            hudManager.showHudComponents(playerRef, HudComponent.MINIMAP);
        } else {
            hudManager.hideHudComponents(playerRef, HudComponent.MINIMAP);
        }
    }
    
    public void cleanupPlayer(UUID playerId) {
        LOGGER.info("[Rubidium] Cleaning up HUD for player: " + playerId);
        playerHuds.remove(playerId);
        customHuds.remove(playerId);
    }
    
    public RubidiumCustomHud getCustomHud(UUID playerId) {
        return customHuds.get(playerId);
    }
    
    public static class RubidiumCustomHud extends CustomUIHud {
        
        private static final Logger LOGGER = Logger.getLogger("Rubidium-CustomHud");
        
        private final UUID playerId;
        private boolean minimapEnabled = true;
        private boolean statsEnabled = true;
        private boolean voiceChatEnabled = true;
        private boolean waypointsEnabled = true;
        
        public RubidiumCustomHud(PlayerRef playerRef, UUID playerId) {
            this.playerId = playerId;
        }
        
        public void setMinimapEnabled(boolean enabled) {
            this.minimapEnabled = enabled;
        }
        
        public void setStatsEnabled(boolean enabled) {
            this.statsEnabled = enabled;
        }
        
        public void setVoiceChatEnabled(boolean enabled) {
            this.voiceChatEnabled = enabled;
        }
        
        public void setWaypointsEnabled(boolean enabled) {
            this.waypointsEnabled = enabled;
        }
        
        public void refresh(PlayerRef playerRef) {
            LOGGER.fine("[Rubidium] Refreshing custom HUD for: " + playerId);
        }
        
        public void buildMinimap(UICommandBuilder ui) {
            if (!minimapEnabled) return;
            
            ui.append("rubidium_hud", "minimap")
                .set("rubidium_hud.minimap.visible", true)
                .set("rubidium_hud.minimap.size", 150)
                .set("rubidium_hud.minimap.position_x", 10)
                .set("rubidium_hud.minimap.position_y", 10);
            
            if (waypointsEnabled) {
                ui.append("rubidium_hud.minimap", "waypoints")
                    .set("rubidium_hud.minimap.waypoints.visible", true);
            }
        }
        
        public void buildStats(UICommandBuilder ui) {
            if (!statsEnabled) return;
            
            ui.append("rubidium_hud", "stats")
                .set("rubidium_hud.stats.visible", true)
                .set("rubidium_hud.stats.show_fps", true)
                .set("rubidium_hud.stats.show_ram", true)
                .set("rubidium_hud.stats.show_ping", true);
        }
        
        public void buildVoiceChat(UICommandBuilder ui) {
            if (!voiceChatEnabled) return;
            
            ui.append("rubidium_hud", "voicechat")
                .set("rubidium_hud.voicechat.visible", true)
                .set("rubidium_hud.voicechat.ptt_indicator", true);
        }
    }
}
