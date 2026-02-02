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

public class RubidiumSettingsPage extends InteractiveCustomUIPage<RubidiumSettingsPage.SettingsData> {
    
    private static final Logger LOGGER = Logger.getLogger("Rubidium-Settings");
    public static final String UI_PATH = "Common/UI/Custom/Pages/RubidiumSettings.ui";
    
    private final UUID playerId;
    
    public RubidiumSettingsPage(PlayerRef playerRef, UUID playerId) {
        super(playerRef, CustomPageLifetime.UNTIL_DISMISSED, SettingsData.CODEC);
        this.playerId = playerId;
        LOGGER.info("[Rubidium] Opening settings page for player: " + playerId);
    }
    
    @Override
    public String getUIPath() {
        return UI_PATH;
    }
    
    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder ui, UIEventBuilder events, Store<EntityStore> store) {
        PlayerSettings settings = SettingsRegistry.get().getPlayerSettings(playerId);
        ServerSettings serverSettings = SettingsRegistry.get().getServerSettings();
        
        ui.set("#MinimapToggle.checked", settings.isMinimapEnabled());
        ui.set("#WaypointsToggle.checked", settings.isWaypointsEnabled());
        ui.set("#StatsToggle.checked", settings.isStatisticsEnabled());
        ui.set("#VoiceChatToggle.checked", settings.isVoiceChatEnabled());
        ui.set("#VolumeSlider.value", (int)(settings.getVoiceChatVolume() * 100));
        ui.set("#ZoomSlider.value", (int)(settings.getMinimapZoom() * 10));
        ui.set("#RotateToggle.checked", settings.isMinimapRotate());
        
        events.on("#MinimapToggle", "change", SettingsData.CODEC);
        events.on("#WaypointsToggle", "change", SettingsData.CODEC);
        events.on("#StatsToggle", "change", SettingsData.CODEC);
        events.on("#VoiceChatToggle", "change", SettingsData.CODEC);
        events.on("#VolumeSlider", "change", SettingsData.CODEC);
        events.on("#ZoomSlider", "change", SettingsData.CODEC);
        events.on("#RotateToggle", "change", SettingsData.CODEC);
        events.on("#SaveButton", "click", SettingsData.CODEC);
        events.on("#ResetButton", "click", SettingsData.CODEC);
        events.on("#BackButton", "click", SettingsData.CODEC);
        
        LOGGER.info("[Rubidium] Settings page bound to UI elements");
    }
    
    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, SettingsData data) {
        if (data == null) return;
        
        PlayerSettings settings = SettingsRegistry.get().getPlayerSettings(playerId);
        
        switch (data.elementId) {
            case "#MinimapToggle" -> settings.setMinimapEnabled(data.boolValue);
            case "#WaypointsToggle" -> settings.setWaypointsEnabled(data.boolValue);
            case "#StatsToggle" -> settings.setStatisticsEnabled(data.boolValue);
            case "#VoiceChatToggle" -> settings.setVoiceChatEnabled(data.boolValue);
            case "#VolumeSlider" -> settings.setVoiceChatVolume(data.floatValue / 100f);
            case "#ZoomSlider" -> settings.setMinimapZoom(data.floatValue / 10f);
            case "#RotateToggle" -> settings.setMinimapRotate(data.boolValue);
            case "#SaveButton" -> {
                settings.save();
                LOGGER.info("[Rubidium] Settings saved for player: " + playerId);
            }
            case "#ResetButton" -> {
                resetDefaults(settings);
                rebuild();
            }
            case "#BackButton" -> dismiss();
        }
    }
    
    private void resetDefaults(PlayerSettings settings) {
        settings.setMinimapEnabled(true);
        settings.setWaypointsEnabled(true);
        settings.setStatisticsEnabled(false);
        settings.setVoiceChatEnabled(true);
        settings.setVoiceChatVolume(1.0f);
        settings.setMinimapZoom(2.0f);
        settings.setMinimapRotate(false);
        settings.save();
        LOGGER.info("[Rubidium] Settings reset to defaults");
    }
    
    public static class SettingsData {
        public static final BuilderCodec<SettingsData> CODEC = BuilderCodec.of(SettingsData.class);
        
        public String elementId = "";
        public String action = "";
        public boolean boolValue = false;
        public float floatValue = 0f;
        public String stringValue = "";
    }
}
