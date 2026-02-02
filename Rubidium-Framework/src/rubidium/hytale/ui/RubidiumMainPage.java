package rubidium.hytale.ui;

import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import rubidium.core.tier.FeatureRegistry;
import rubidium.core.RubidiumBootstrap;

import java.util.UUID;
import java.util.logging.Logger;

public class RubidiumMainPage extends InteractiveCustomUIPage<RubidiumMainPage.MenuData> {
    
    private static final Logger LOGGER = Logger.getLogger("Rubidium-Main");
    public static final String UI_PATH = "Common/UI/Custom/Pages/RubidiumMain.ui";
    
    private final UUID playerId;
    private final PageManager pageManager;
    
    public RubidiumMainPage(PlayerRef playerRef, UUID playerId, PageManager pageManager) {
        super(playerRef, CustomPageLifetime.UNTIL_DISMISSED, MenuData.CODEC);
        this.playerId = playerId;
        this.pageManager = pageManager;
    }
    
    @Override
    public String getUIPath() {
        return UI_PATH;
    }
    
    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder ui, UIEventBuilder events, Store<EntityStore> store) {
        String edition = FeatureRegistry.getCurrentTier().getDisplayName();
        String version = RubidiumBootstrap.getVersion();
        
        ui.set("#VersionLabel.text", edition + " v" + version);
        
        events.on("#SettingsButton", "click", MenuData.CODEC);
        events.on("#MinimapButton", "click", MenuData.CODEC);
        events.on("#VoiceChatButton", "click", MenuData.CODEC);
        events.on("#HUDButton", "click", MenuData.CODEC);
        events.on("#AdminButton", "click", MenuData.CODEC);
        events.on("#StatsButton", "click", MenuData.CODEC);
        events.on("#WaypointsButton", "click", MenuData.CODEC);
        events.on("#ProfilesButton", "click", MenuData.CODEC);
        events.on("#CloseButton", "click", MenuData.CODEC);
        
        LOGGER.info("[Rubidium] Main menu opened for player: " + playerId);
    }
    
    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, MenuData data) {
        if (data == null) return;
        
        switch (data.elementId) {
            case "#SettingsButton" -> {
                dismiss();
                pageManager.openPage(new RubidiumSettingsPage(getPlayerRef(), playerId));
            }
            case "#MinimapButton" -> {
                LOGGER.info("[Rubidium] Minimap settings opened");
            }
            case "#VoiceChatButton" -> {
                LOGGER.info("[Rubidium] Voice chat settings opened");
            }
            case "#HUDButton" -> {
                dismiss();
                pageManager.openPage(new RubidiumHUDEditorPage(getPlayerRef(), playerId));
            }
            case "#AdminButton" -> {
                dismiss();
                pageManager.openPage(new RubidiumAdminPage(getPlayerRef(), playerId, pageManager));
            }
            case "#StatsButton" -> {
                LOGGER.info("[Rubidium] Performance stats toggled");
            }
            case "#WaypointsButton" -> {
                LOGGER.info("[Rubidium] Waypoints manager opened");
            }
            case "#ProfilesButton" -> {
                LOGGER.info("[Rubidium] Profiles manager opened");
            }
            case "#CloseButton" -> dismiss();
        }
    }
    
    public static class MenuData {
        public static final BuilderCodec<MenuData> CODEC = BuilderCodec.of(MenuData.class);
        
        public String elementId = "";
        public String action = "";
    }
}
