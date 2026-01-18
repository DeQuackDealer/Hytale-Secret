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
            int mapViewSize = size - 10;
            int mapAreaSize = size - 35;
            
            PlayerSettings settings = SettingsRegistry.get().getPlayerSettings(playerId);
            float zoom = settings.getMinimapZoom();
            boolean rotate = settings.isMinimapRotate();
            
            Player player = rubidium.api.server.Server.getPlayer(playerId).orElse(null);
            double playerX = player != null ? player.getX() : 0;
            double playerY = player != null ? player.getY() : 0;
            double playerZ = player != null ? player.getZ() : 0;
            
            UIContainer mapPanel = new UIContainer("minimap_hud")
                .setPosition(x, y)
                .setSize(size, size + 25)
                .setBackground(0x1E1E23EE);
            
            UIContainer mapFrame = new UIContainer("map_frame")
                .setPosition(5, 5)
                .setSize(mapViewSize, mapViewSize)
                .setBackground(0x14141ABB);
            
            UIContainer mapView = new UIContainer("map_view")
                .setPosition(2, 2)
                .setSize(mapAreaSize, mapAreaSize)
                .setBackground(0x2A4A35);
            
            mapView.addChild(new UIContainer("map_grid_h1")
                .setPosition(0, mapAreaSize / 3)
                .setSize(mapAreaSize, 1)
                .setBackground(0x1A3A2540));
            mapView.addChild(new UIContainer("map_grid_h2")
                .setPosition(0, 2 * mapAreaSize / 3)
                .setSize(mapAreaSize, 1)
                .setBackground(0x1A3A2540));
            mapView.addChild(new UIContainer("map_grid_v1")
                .setPosition(mapAreaSize / 3, 0)
                .setSize(1, mapAreaSize)
                .setBackground(0x1A3A2540));
            mapView.addChild(new UIContainer("map_grid_v2")
                .setPosition(2 * mapAreaSize / 3, 0)
                .setSize(1, mapAreaSize)
                .setBackground(0x1A3A2540));
            
            int centerX = mapAreaSize / 2;
            int centerY = mapAreaSize / 2;
            
            mapView.addChild(new UIText("player_marker")
                .setText("\u25B2")
                .setFontSize(14)
                .setColor(0x4A90D9)
                .setPosition(centerX - 5, centerY - 7));
            
            mapView.addChild(new UIContainer("player_dot")
                .setPosition(centerX - 2, centerY - 2)
                .setSize(4, 4)
                .setBackground(0xFFFFFF));
            
            List<Waypoint> waypoints = getWaypoints(playerId);
            double viewRadius = 100.0 / zoom;
            int wpIndex = 0;
            
            for (Waypoint wp : waypoints) {
                if (!wp.isVisible()) continue;
                
                double relX = wp.getX() - playerX;
                double relZ = wp.getZ() - playerZ;
                double dist = Math.sqrt(relX * relX + relZ * relZ);
                
                double normX = relX / viewRadius;
                double normZ = relZ / viewRadius;
                
                boolean isOnMap = Math.abs(normX) <= 1.0 && Math.abs(normZ) <= 1.0;
                
                if (!isOnMap) {
                    double angle = Math.atan2(normZ, normX);
                    normX = Math.cos(angle) * 0.9;
                    normZ = Math.sin(angle) * 0.9;
                }
                
                int wpScreenX = centerX + (int)(normX * (mapAreaSize / 2 - 10));
                int wpScreenY = centerY + (int)(normZ * (mapAreaSize / 2 - 10));
                
                wpScreenX = Math.max(8, Math.min(mapAreaSize - 12, wpScreenX));
                wpScreenY = Math.max(8, Math.min(mapAreaSize - 12, wpScreenY));
                
                String wpIcon = isOnMap ? "\u25C6" : "\u25C7";
                mapView.addChild(new UIText("wp_icon_" + wpIndex)
                    .setText(wpIcon)
                    .setFontSize(isOnMap ? 10 : 8)
                    .setColor(wp.getColor())
                    .setPosition(wpScreenX - 4, wpScreenY - 5));
                
                if (isOnMap && dist < viewRadius * 0.5) {
                    mapView.addChild(new UIText("wp_name_" + wpIndex)
                        .setText(wp.getName())
                        .setFontSize(8)
                        .setColor(0xD0D0D5)
                        .setPosition(wpScreenX + 6, wpScreenY - 4));
                }
                
                wpIndex++;
            }
            
            mapFrame.addChild(mapView);
            
            int compassSize = 16;
            mapFrame.addChild(new UIText("compass_n")
                .setText("N")
                .setFontSize(10)
                .setColor(0xFF6B6B)
                .setPosition(mapViewSize / 2 - 3, 4));
            mapFrame.addChild(new UIText("compass_s")
                .setText("S")
                .setFontSize(9)
                .setColor(0xA0A0AA)
                .setPosition(mapViewSize / 2 - 3, mapViewSize - 12));
            mapFrame.addChild(new UIText("compass_e")
                .setText("E")
                .setFontSize(9)
                .setColor(0xA0A0AA)
                .setPosition(mapViewSize - 10, mapViewSize / 2 - 4));
            mapFrame.addChild(new UIText("compass_w")
                .setText("W")
                .setFontSize(9)
                .setColor(0xA0A0AA)
                .setPosition(4, mapViewSize / 2 - 4));
            
            mapPanel.addChild(mapFrame);
            
            UIContainer controls = new UIContainer("map_controls")
                .setPosition(5, size - 5)
                .setSize(mapViewSize, 22)
                .setBackground(0x14141ACC);
            
            controls.addChild(new UIButton("zoom_in")
                .setText("+")
                .setSize(20, 18)
                .setPosition(2, 2)
                .setBackground(0x404050)
                .onClick(() -> settings.setMinimapZoom(zoom + 0.25f)));
            
            controls.addChild(new UIButton("zoom_out")
                .setText("-")
                .setSize(20, 18)
                .setPosition(24, 2)
                .setBackground(0x404050)
                .onClick(() -> settings.setMinimapZoom(zoom - 0.25f)));
            
            controls.addChild(new UIText("zoom_level")
                .setText(String.format("%.1fx", zoom))
                .setFontSize(9)
                .setColor(0xA0A0AA)
                .setPosition(48, 6));
            
            String rotateIcon = rotate ? "\u21BB" : "\u2191";
            controls.addChild(new UIButton("rotate_toggle")
                .setText(rotateIcon)
                .setSize(18, 18)
                .setPosition(mapViewSize - 22, 2)
                .setBackground(rotate ? 0x4A90D9 : 0x404050)
                .onClick(() -> settings.setMinimapRotate(!rotate)));
            
            mapPanel.addChild(controls);
            
            String coordsText = String.format("%.0f, %.0f, %.0f", playerX, playerY, playerZ);
            mapPanel.addChild(new UIText("coordinates")
                .setText(coordsText)
                .setFontSize(9)
                .setColor(0x808090)
                .setPosition(8, size + 5));
            
            if (!waypoints.isEmpty()) {
                Waypoint nearest = null;
                double nearestDist = Double.MAX_VALUE;
                for (Waypoint wp : waypoints) {
                    double d = wp.distanceTo(playerX, playerY, playerZ);
                    if (d < nearestDist) {
                        nearestDist = d;
                        nearest = wp;
                    }
                }
                if (nearest != null) {
                    String distText = String.format("%s: %.0fm", nearest.getName(), nearestDist);
                    mapPanel.addChild(new UIText("nearest_wp")
                        .setText(distText)
                        .setFontSize(8)
                        .setColor(nearest.getColor())
                        .setPosition(8, size + 15));
                }
            }
        }
        
        @Override
        public int getDefaultWidth() { return 160; }
        
        @Override
        public int getDefaultHeight() { return 185; }
    }
}
