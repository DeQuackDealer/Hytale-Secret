package rubidium.api.ui.overlays;

import rubidium.api.ui.RubidiumOverlayPage;
import rubidium.api.ui.RubidiumUI;
import rubidium.features.minimap.MinimapManager;
import rubidium.features.minimap.Waypoint;
import com.hypixel.hytale.server.api.player.Player;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.util.UUID;

public class MinimapOverlay extends RubidiumOverlayPage<Void> {
    
    private static final String PAGE_ID = "rubidium_minimap";
    private static final String UI_PATH = "Common/UI/Custom/Pages/RubidiumMinimap.ui";
    
    private final MinimapManager minimapManager;
    private float zoomLevel = 1.0f;
    private boolean showWaypoints = true;
    private boolean showPlayers = true;
    
    public MinimapOverlay(MinimapManager minimapManager) {
        this.minimapManager = minimapManager;
    }
    
    @Override
    public String getPageId() {
        return PAGE_ID;
    }
    
    @Override
    public String getUIPath() {
        return UI_PATH;
    }
    
    @Override
    protected void registerEvents(UIEventBuilder events) {
        events.onClick("#ZoomInBtn", this::zoomIn);
        events.onClick("#ZoomOutBtn", this::zoomOut);
        events.onToggle("#ShowWaypointsToggle", this::setShowWaypoints);
        events.onToggle("#ShowPlayersToggle", this::setShowPlayers);
    }
    
    @Override
    protected void buildUI(UICommandBuilder builder) {
        builder.set("#ZoomLevel.text", String.format("%.1fx", zoomLevel));
        builder.set("#ShowWaypointsToggle.checked", showWaypoints);
        builder.set("#ShowPlayersToggle.checked", showPlayers);
        updatePlayerPosition(builder);
    }
    
    public void zoomIn() {
        zoomLevel = Math.min(4.0f, zoomLevel + 0.5f);
        state.set("#ZoomLevel.text", String.format("%.1fx", zoomLevel));
        rebuild();
    }
    
    public void zoomOut() {
        zoomLevel = Math.max(0.5f, zoomLevel - 0.5f);
        state.set("#ZoomLevel.text", String.format("%.1fx", zoomLevel));
        rebuild();
    }
    
    public void setShowWaypoints(boolean show) {
        this.showWaypoints = show;
        state.set("#WaypointContainer.visible", show);
        rebuild();
    }
    
    public void setShowPlayers(boolean show) {
        this.showPlayers = show;
        state.set("#PlayerMarkers.visible", show);
        rebuild();
    }
    
    public void updatePlayerPosition(UICommandBuilder builder) {
        if (player != null) {
            double x = player.getX();
            double z = player.getZ();
            builder.set("#PlayerArrow.x", (float) x);
            builder.set("#PlayerArrow.y", (float) z);
        }
    }
    
    public void addWaypoint(Waypoint waypoint) {
        state.set("#Waypoint_" + waypoint.getId() + ".x", (float) waypoint.getX());
        state.set("#Waypoint_" + waypoint.getId() + ".y", (float) waypoint.getZ());
        state.set("#Waypoint_" + waypoint.getId() + ".visible", true);
        rebuild();
    }
    
    public void removeWaypoint(String waypointId) {
        state.set("#Waypoint_" + waypointId + ".visible", false);
        rebuild();
    }
    
    public float getZoomLevel() {
        return zoomLevel;
    }
    
    public boolean isShowingWaypoints() {
        return showWaypoints;
    }
    
    public boolean isShowingPlayers() {
        return showPlayers;
    }
    
    public static void open(Player player, MinimapManager minimapManager) {
        MinimapOverlay overlay = new MinimapOverlay(minimapManager);
        RubidiumUI.openOverlay(player, overlay);
    }
}
