package rubidium.minimap;

import rubidium.api.RubidiumModule;
import rubidium.api.player.Player;
import rubidium.api.world.Location;
import rubidium.hud.HUDRegistry;
import rubidium.settings.PlayerSettings;
import rubidium.settings.SettingsRegistry;
import rubidium.ui.components.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MinimapModule implements RubidiumModule {
    
    private static MinimapModule instance;
    
    private final Map<UUID, List<Waypoint>> playerWaypoints = new ConcurrentHashMap<>();
    private final List<Waypoint> serverWaypoints = new ArrayList<>();
    private boolean enabled = true;
    
    @Override
    public String getId() { return "rubidium-minimap"; }
    
    @Override
    public String getName() { return "Minimap & Waypoints"; }
    
    @Override
    public String getVersion() { return "1.0.0"; }
    
    @Override
    public void onEnable() {
        instance = this;
        
        HUDRegistry.get().registerWidget(new MinimapHUDWidget());
        
        log("Minimap module enabled");
    }
    
    @Override
    public void onDisable() {
        playerWaypoints.clear();
        serverWaypoints.clear();
        instance = null;
    }
    
    public static MinimapModule getInstance() {
        return instance;
    }
    
    public void addWaypoint(UUID playerId, Waypoint waypoint) {
        if (playerId == null) {
            serverWaypoints.add(waypoint);
        } else {
            playerWaypoints.computeIfAbsent(playerId, k -> new ArrayList<>()).add(waypoint);
        }
    }
    
    public void removeWaypoint(UUID playerId, String waypointId) {
        if (playerId == null) {
            serverWaypoints.removeIf(w -> w.getId().equals(waypointId));
        } else {
            List<Waypoint> waypoints = playerWaypoints.get(playerId);
            if (waypoints != null) {
                waypoints.removeIf(w -> w.getId().equals(waypointId));
            }
        }
    }
    
    public List<Waypoint> getWaypoints(UUID playerId) {
        List<Waypoint> result = new ArrayList<>(serverWaypoints);
        List<Waypoint> playerList = playerWaypoints.get(playerId);
        if (playerList != null) {
            result.addAll(playerList);
        }
        return result;
    }
    
    public Optional<Waypoint> getWaypoint(UUID playerId, String waypointId) {
        return getWaypoints(playerId).stream()
            .filter(w -> w.getId().equals(waypointId))
            .findFirst();
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public static class Waypoint {
        private final String id;
        private String name;
        private double x, y, z;
        private String worldId;
        private int color;
        private String icon;
        private boolean visible;
        private final UUID ownerId;
        private final long createdAt;
        
        public Waypoint(String id, String name, double x, double y, double z, String worldId, int color, UUID ownerId) {
            this.id = id;
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.worldId = worldId;
            this.color = color;
            this.icon = "waypoint";
            this.visible = true;
            this.ownerId = ownerId;
            this.createdAt = System.currentTimeMillis();
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public void setPosition(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
        
        public String getWorldId() { return worldId; }
        public void setWorldId(String worldId) { this.worldId = worldId; }
        
        public int getColor() { return color; }
        public void setColor(int color) { this.color = color; }
        
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        
        public boolean isVisible() { return visible; }
        public void setVisible(boolean visible) { this.visible = visible; }
        
        public UUID getOwnerId() { return ownerId; }
        public long getCreatedAt() { return createdAt; }
        
        public double distanceTo(double px, double py, double pz) {
            double dx = x - px;
            double dy = y - py;
            double dz = z - pz;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }
    
    private class MinimapHUDWidget extends HUDRegistry.HUDWidget {
        
        public MinimapHUDWidget() {
            super("minimap", "Minimap", "Shows a map of your surroundings with waypoints", true, true, true);
        }
        
        @Override
        public boolean isVisible(UUID playerId) {
            if (!enabled) return false;
            rubidium.settings.ServerSettings serverSettings = SettingsRegistry.get().getServerSettings();
            if (!serverSettings.isMinimapAllowed()) return false;
            
            PlayerSettings settings = SettingsRegistry.get().getPlayerSettings(playerId);
            return settings.isMinimapEnabled();
        }
        
        @Override
        public void render(UUID playerId, HUDRegistry.RenderContext ctx, PlayerSettings.HUDPosition position) {
            int x = ctx.resolveX(position);
            int y = ctx.resolveY(position);
            int size = position.getWidth();
            
            PlayerSettings settings = SettingsRegistry.get().getPlayerSettings(playerId);
            
            UIContainer mapPanel = new UIContainer("minimap_hud")
                .setPosition(x, y)
                .setSize(size, size)
                .setBackground(0x1E1E23DD);
            
            UIContainer mapView = new UIContainer("map_view")
                .setPosition(5, 5)
                .setSize(size - 10, size - 30)
                .setBackground(0x2D4F2D);
            
            mapView.addChild(new UIText("map_center")
                .setText("+")
                .setFontSize(16)
                .setColor(0xFFFFFF)
                .setPosition((size - 10) / 2 - 5, (size - 30) / 2 - 8));
            
            List<Waypoint> waypoints = getWaypoints(playerId);
            int wpY = 10;
            for (Waypoint wp : waypoints) {
                if (!wp.isVisible()) continue;
                
                int wpX = (int) ((wp.getX() % 100) + 50);
                int wpZ = (int) ((wp.getZ() % 100) + 50);
                
                wpX = Math.max(5, Math.min(size - 25, wpX));
                wpZ = Math.max(5, Math.min(size - 45, wpZ));
                
                mapView.addChild(new UIText("wp_" + wp.getId())
                    .setText("*")
                    .setFontSize(14)
                    .setColor(wp.getColor())
                    .setPosition(wpX, wpZ));
            }
            
            mapPanel.addChild(mapView);
            
            UIContainer controls = new UIContainer("map_controls")
                .setPosition(5, size - 25)
                .setSize(size - 10, 20)
                .setBackground(0x14141A);
            
            controls.addChild(new UIButton("zoom_in")
                .setText("+")
                .setSize(20, 18)
                .setPosition(2, 1)
                .setBackground(0x505060)
                .onClick(() -> {
                    settings.setMinimapZoom(settings.getMinimapZoom() + 0.25f);
                }));
            
            controls.addChild(new UIButton("zoom_out")
                .setText("-")
                .setSize(20, 18)
                .setPosition(24, 1)
                .setBackground(0x505060)
                .onClick(() -> {
                    settings.setMinimapZoom(settings.getMinimapZoom() - 0.25f);
                }));
            
            controls.addChild(new UIText("zoom_level")
                .setText(String.format("%.1fx", settings.getMinimapZoom()))
                .setFontSize(10)
                .setColor(0xA0A0AA)
                .setPosition(50, 4));
            
            controls.addChild(new UIText("coords")
                .setText("0, 0, 0")
                .setFontSize(9)
                .setColor(0x808090)
                .setPosition(85, 4));
            
            mapPanel.addChild(controls);
        }
        
        @Override
        public int getDefaultWidth() { return 150; }
        
        @Override
        public int getDefaultHeight() { return 150; }
    }
}
