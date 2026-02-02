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

import rubidium.admin.AdminUIModule;

import java.util.UUID;
import java.util.logging.Logger;

public class RubidiumAdminPage extends InteractiveCustomUIPage<RubidiumAdminPage.AdminData> {
    
    private static final Logger LOGGER = Logger.getLogger("Rubidium-Admin");
    public static final String UI_PATH = "Common/UI/Custom/Pages/RubidiumAdmin.ui";
    
    private final UUID playerId;
    private final PageManager pageManager;
    private String currentTab = "Players";
    
    public RubidiumAdminPage(PlayerRef playerRef, UUID playerId, PageManager pageManager) {
        super(playerRef, CustomPageLifetime.UNTIL_DISMISSED, AdminData.CODEC);
        this.playerId = playerId;
        this.pageManager = pageManager;
    }
    
    @Override
    public String getUIPath() {
        return UI_PATH;
    }
    
    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder ui, UIEventBuilder events, Store<EntityStore> store) {
        events.on("#PlayersTab", "click", AdminData.CODEC);
        events.on("#WorldTab", "click", AdminData.CODEC);
        events.on("#PermsTab", "click", AdminData.CODEC);
        events.on("#ServerTab", "click", AdminData.CODEC);
        events.on("#BansTab", "click", AdminData.CODEC);
        events.on("#TeleportTab", "click", AdminData.CODEC);
        events.on("#ActionButton1", "click", AdminData.CODEC);
        events.on("#ActionButton2", "click", AdminData.CODEC);
        events.on("#CloseButton", "click", AdminData.CODEC);
        
        updateTabContent(ui);
        
        LOGGER.info("[Rubidium] Admin panel opened for player: " + playerId);
    }
    
    private void updateTabContent(UICommandBuilder ui) {
        ui.clear("#ContentList");
        
        switch (currentTab) {
            case "Players" -> {
                ui.append("#ContentList", "item", "View online players");
                ui.append("#ContentList", "item", "Kick player");
                ui.append("#ContentList", "item", "Manage permissions");
            }
            case "World" -> {
                ui.append("#ContentList", "item", "World settings");
                ui.append("#ContentList", "item", "Time of day");
                ui.append("#ContentList", "item", "Weather control");
            }
            case "Perms" -> {
                ui.append("#ContentList", "item", "Permission groups");
                ui.append("#ContentList", "item", "Add admin");
                ui.append("#ContentList", "item", "Remove admin");
            }
            case "Server" -> {
                ui.append("#ContentList", "item", "Server status");
                ui.append("#ContentList", "item", "Performance metrics");
                ui.append("#ContentList", "item", "Restart server");
            }
            case "Bans" -> {
                ui.append("#ContentList", "item", "View banned players");
                ui.append("#ContentList", "item", "Ban player");
                ui.append("#ContentList", "item", "Unban player");
            }
            case "Teleport" -> {
                ui.append("#ContentList", "item", "Teleport to player");
                ui.append("#ContentList", "item", "Summon player");
                ui.append("#ContentList", "item", "Set spawn point");
            }
        }
    }
    
    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, AdminData data) {
        if (data == null) return;
        
        switch (data.elementId) {
            case "#PlayersTab" -> { currentTab = "Players"; rebuild(); }
            case "#WorldTab" -> { currentTab = "World"; rebuild(); }
            case "#PermsTab" -> { currentTab = "Perms"; rebuild(); }
            case "#ServerTab" -> { currentTab = "Server"; rebuild(); }
            case "#BansTab" -> { currentTab = "Bans"; rebuild(); }
            case "#TeleportTab" -> { currentTab = "Teleport"; rebuild(); }
            case "#ActionButton1" -> executeAction();
            case "#ActionButton2" -> refresh();
            case "#CloseButton" -> dismiss();
        }
    }
    
    private void executeAction() {
        LOGGER.info("[Rubidium] Admin action executed in tab: " + currentTab);
    }
    
    private void refresh() {
        rebuild();
        LOGGER.info("[Rubidium] Admin panel refreshed");
    }
    
    public static class AdminData {
        public static final BuilderCodec<AdminData> CODEC = BuilderCodec.of(AdminData.class);
        
        public String elementId = "";
        public String action = "";
        public String targetPlayer = "";
    }
}
