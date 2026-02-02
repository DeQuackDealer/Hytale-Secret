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

import rubidium.hud.HUDRegistry;

import java.util.UUID;
import java.util.logging.Logger;

public class RubidiumHUDEditorPage extends InteractiveCustomUIPage<RubidiumHUDEditorPage.HUDData> {
    
    private static final Logger LOGGER = Logger.getLogger("Rubidium-HUD");
    public static final String UI_PATH = "Common/UI/Custom/Pages/RubidiumHUD.ui";
    
    private final UUID playerId;
    
    public RubidiumHUDEditorPage(PlayerRef playerRef, UUID playerId) {
        super(playerRef, CustomPageLifetime.UNTIL_DISMISSED, HUDData.CODEC);
        this.playerId = playerId;
    }
    
    @Override
    public String getUIPath() {
        return UI_PATH;
    }
    
    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder ui, UIEventBuilder events, Store<EntityStore> store) {
        LOGGER.info("[Rubidium] HUD Editor opened for player: " + playerId);
    }
    
    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, HUDData data) {
        if (data == null) return;
        LOGGER.info("[Rubidium] HUD action: " + data.action);
    }
    
    public static class HUDData {
        public static final BuilderCodec<HUDData> CODEC = BuilderCodec.of(HUDData.class);
        
        public String action = "";
        public String widgetId = "";
        public int x = 0;
        public int y = 0;
    }
}
