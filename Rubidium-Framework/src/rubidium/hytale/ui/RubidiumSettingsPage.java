package rubidium.hytale.ui;

import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import rubidium.settings.PlayerSettings;
import rubidium.settings.ServerSettings;
import rubidium.settings.SettingsRegistry;

import java.util.UUID;
import java.util.logging.Logger;

public class RubidiumSettingsPage extends InteractiveCustomUIPage<RubidiumSettingsPage.SettingsEventData> {
    
    private static final Logger LOGGER = Logger.getLogger("Rubidium-Settings");
    private static final String ROOT = "rubidium_settings";
    private final UUID playerId;
    
    public RubidiumSettingsPage(PlayerRef playerRef, UUID playerId) {
        super(playerRef, CustomPageLifetime.UNTIL_DISMISSED, SettingsEventData.CODEC);
        this.playerId = playerId;
        LOGGER.info("[Rubidium] Opening settings page for player: " + playerId);
    }
    
    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder ui, UIEventBuilder events, Store<EntityStore> store) {
        PlayerSettings settings = SettingsRegistry.get().getPlayerSettings(playerId);
        ServerSettings serverSettings = SettingsRegistry.get().getServerSettings();
        SettingsRegistry.PermissionLevel permLevel = serverSettings.getPermissionLevel(playerId);
        
        ui.clear(ROOT);
        
        ui.append(ROOT, "header")
            .set(ROOT + ".header.text", "Rubidium Settings")
            .set(ROOT + ".header.fontSize", 24)
            .set(ROOT + ".header.color", "#8A2BE2");
        
        ui.append(ROOT, "subtitle")
            .set(ROOT + ".subtitle.text", "Configure your Rubidium experience")
            .set(ROOT + ".subtitle.fontSize", 12)
            .set(ROOT + ".subtitle.color", "#808090");
        
        ui.append(ROOT, "version")
            .set(ROOT + ".version.text", "v1.0.0")
            .set(ROOT + ".version.fontSize", 10);
        
        ui.append(ROOT, "status_indicator")
            .set(ROOT + ".status_indicator.color", "#32CD32")
            .set(ROOT + ".status_indicator.text", "Online");
        
        buildDisplaySection(ui, settings, serverSettings);
        buildVoiceChatSection(ui, settings, serverSettings);
        buildMinimapSection(ui, settings, serverSettings);
        
        if (permLevel == SettingsRegistry.PermissionLevel.OWNER || 
            permLevel == SettingsRegistry.PermissionLevel.ADMIN) {
            buildAdminSection(ui, serverSettings, permLevel);
        }
        
        ui.append(ROOT, "edit_hud_btn")
            .set(ROOT + ".edit_hud_btn.text", "Edit HUD Layout")
            .set(ROOT + ".edit_hud_btn.action", "open_hud_editor");
        
        ui.append(ROOT, "reset_btn")
            .set(ROOT + ".reset_btn.text", "Reset to Defaults")
            .set(ROOT + ".reset_btn.action", "reset_defaults");
        
        events.on("toggle_setting", SettingsEventData.CODEC);
        events.on("open_hud_editor", SettingsEventData.CODEC);
        events.on("reset_defaults", SettingsEventData.CODEC);
        
        LOGGER.info("[Rubidium] Settings page built successfully");
    }
    
    private void buildDisplaySection(UICommandBuilder ui, PlayerSettings settings, ServerSettings serverSettings) {
        ui.append(ROOT, "display_section")
            .set(ROOT + ".display_section.title", "Display Features");
        
        addToggle(ui, "minimap", "Minimap", "Show the minimap overlay",
                  settings.isMinimapEnabled(), serverSettings.isMinimapAllowed());
        addToggle(ui, "waypoints", "Waypoints", "Show waypoint markers",
                  settings.isWaypointsEnabled(), serverSettings.isWaypointsAllowed());
        addToggle(ui, "statistics", "Performance Stats", "Show FPS, DPS, RAM usage",
                  settings.isStatisticsEnabled(), serverSettings.isStatisticsAllowed());
    }
    
    private void buildVoiceChatSection(UICommandBuilder ui, PlayerSettings settings, ServerSettings serverSettings) {
        ui.append(ROOT, "voicechat_section")
            .set(ROOT + ".voicechat_section.title", "Voice Chat");
        
        addToggle(ui, "voicechat", "Voice Chat", "Enable voice communication",
                  settings.isVoiceChatEnabled(), serverSettings.isVoiceChatAllowed());
        
        ui.append(ROOT, "voicechat_volume")
            .set(ROOT + ".voicechat_volume.label", "Volume")
            .set(ROOT + ".voicechat_volume.value", settings.getVoiceChatVolume())
            .set(ROOT + ".voicechat_volume.min", 0)
            .set(ROOT + ".voicechat_volume.max", 100);
        
        ui.append(ROOT, "ptt_key")
            .set(ROOT + ".ptt_key.label", "Push-to-Talk Key")
            .set(ROOT + ".ptt_key.value", settings.getPushToTalkKey());
    }
    
    private void buildMinimapSection(UICommandBuilder ui, PlayerSettings settings, ServerSettings serverSettings) {
        ui.append(ROOT, "minimap_section")
            .set(ROOT + ".minimap_section.title", "Minimap Settings");
        
        ui.append(ROOT, "minimap_zoom")
            .set(ROOT + ".minimap_zoom.label", "Zoom Level")
            .set(ROOT + ".minimap_zoom.value", settings.getMinimapZoom())
            .set(ROOT + ".minimap_zoom.min", 1)
            .set(ROOT + ".minimap_zoom.max", 5);
        
        addToggle(ui, "minimap_rotate", "Rotate with Player", "Minimap rotates based on player direction",
                  settings.isMinimapRotateEnabled(), true);
        addToggle(ui, "minimap_compass", "Show Compass", "Display compass directions",
                  settings.isMinimapCompassEnabled(), true);
    }
    
    private void buildAdminSection(UICommandBuilder ui, ServerSettings serverSettings, SettingsRegistry.PermissionLevel permLevel) {
        ui.append(ROOT, "admin_section")
            .set(ROOT + ".admin_section.title", "Server Administration");
        
        ui.append(ROOT, "admin_role")
            .set(ROOT + ".admin_role.text", "Your Role: " + permLevel.name());
        
        if (permLevel == SettingsRegistry.PermissionLevel.OWNER) {
            addToggle(ui, "allow_minimap", "Allow Minimap", "Players can use minimap",
                      serverSettings.isMinimapAllowed(), true);
            addToggle(ui, "allow_voicechat", "Allow Voice Chat", "Players can use voice chat",
                      serverSettings.isVoiceChatAllowed(), true);
            addToggle(ui, "optimizations", "Rubidium Optimizations", "Enable performance improvements",
                      serverSettings.isOptimizationsEnabled(), true);
        }
        
        ui.append(ROOT, "open_admin_btn")
            .set(ROOT + ".open_admin_btn.text", "Open Admin Panel")
            .set(ROOT + ".open_admin_btn.action", "open_admin_panel");
    }
    
    private void addToggle(UICommandBuilder ui, String id, String name, String description,
                           boolean enabled, boolean serverAllowed) {
        String path = ROOT + ".toggle_" + id;
        ui.append(ROOT, "toggle_" + id)
            .set(path + ".name", name)
            .set(path + ".description", description)
            .set(path + ".enabled", enabled)
            .set(path + ".serverAllowed", serverAllowed)
            .set(path + ".action", "toggle_setting")
            .set(path + ".settingId", id);
    }
    
    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, SettingsEventData data) {
        if (data == null) return;
        
        LOGGER.info("[Rubidium] Settings event: " + data.action + " - " + data.settingId);
        
        PlayerSettings settings = SettingsRegistry.get().getPlayerSettings(playerId);
        
        switch (data.action) {
            case "toggle_setting" -> {
                toggleSetting(settings, data.settingId);
                settings.save();
                rebuild();
            }
            case "reset_defaults" -> {
                settings.resetToDefaults();
                settings.save();
                rebuild();
            }
            case "open_hud_editor" -> {
                LOGGER.info("[Rubidium] Opening HUD editor for: " + playerId);
            }
            case "open_admin_panel" -> {
                LOGGER.info("[Rubidium] Opening admin panel for: " + playerId);
            }
        }
    }
    
    private void toggleSetting(PlayerSettings settings, String settingId) {
        if (settingId == null) return;
        
        switch (settingId) {
            case "minimap" -> settings.setMinimapEnabled(!settings.isMinimapEnabled());
            case "waypoints" -> settings.setWaypointsEnabled(!settings.isWaypointsEnabled());
            case "statistics" -> settings.setStatisticsEnabled(!settings.isStatisticsEnabled());
            case "voicechat" -> settings.setVoiceChatEnabled(!settings.isVoiceChatEnabled());
            case "minimap_rotate" -> settings.setMinimapRotateEnabled(!settings.isMinimapRotateEnabled());
            case "minimap_compass" -> settings.setMinimapCompassEnabled(!settings.isMinimapCompassEnabled());
        }
    }
    
    public static class SettingsEventData {
        public static final BuilderCodec<SettingsEventData> CODEC = new BuilderCodec<>() {
            @Override
            public SettingsEventData decode(String data) {
                SettingsEventData result = new SettingsEventData();
                if (data != null && data.contains(":")) {
                    String[] parts = data.split(":", 2);
                    result.action = parts[0];
                    result.settingId = parts.length > 1 ? parts[1] : null;
                }
                return result;
            }
            
            @Override
            public String encode(SettingsEventData value) {
                return value.action + ":" + (value.settingId != null ? value.settingId : "");
            }
        };
        
        public String action;
        public String settingId;
        public String value;
    }
}
