package rubidium.map;

import rubidium.hytale.api.player.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class HytaleMapIntegration {
    
    private static final Logger logger = Logger.getLogger("Rubidium-HytaleMap");
    
    private final WaypointManager waypointManager;
    private final ZoneManager zoneManager;
    
    private final Map<UUID, MapOverlayState> playerOverlays = new ConcurrentHashMap<>();
    
    public HytaleMapIntegration(WaypointManager waypointManager, ZoneManager zoneManager) {
        this.waypointManager = waypointManager;
        this.zoneManager = zoneManager;
    }
    
    public void onPlayerJoin(Player player) {
        MapOverlayState state = new MapOverlayState(player.getUuid());
        playerOverlays.put(player.getUuid(), state);
        
        syncWaypointsToHytaleMap(player);
        syncZonesToHytaleMap(player);
    }
    
    public void onPlayerQuit(UUID playerId) {
        playerOverlays.remove(playerId);
    }
    
    public void syncWaypointsToHytaleMap(Player player) {
        List<Waypoint> waypoints = waypointManager.getPlayerWaypoints(player.getUuid());
        List<Waypoint> publicWaypoints = waypointManager.getPublicWaypoints();
        
        List<MapMarker> markers = new ArrayList<>();
        
        for (Waypoint wp : waypoints) {
            markers.add(new MapMarker(
                wp.getId(),
                MapMarker.MarkerType.WAYPOINT,
                wp.getName(),
                wp.getX(), wp.getY(), wp.getZ(),
                parseColor(wp.getColor()),
                wp.getIcon().name().toLowerCase(),
                true
            ));
        }
        
        for (Waypoint wp : publicWaypoints) {
            markers.add(new MapMarker(
                "public-" + wp.getId(),
                MapMarker.MarkerType.PUBLIC_WAYPOINT,
                wp.getName(),
                wp.getX(), wp.getY(), wp.getZ(),
                parseColor(wp.getColor()),
                wp.getIcon().name().toLowerCase(),
                true
            ));
        }
        
        player.sendPacket(new SyncMapMarkersPacket(markers));
    }
    
    public void syncZonesToHytaleMap(Player player) {
        List<Zone> zones = zoneManager.getVisibleZones(player);
        
        List<MapZoneOverlay> overlays = new ArrayList<>();
        
        for (Zone zone : zones) {
            overlays.add(new MapZoneOverlay(
                zone.getId(),
                zone.getName(),
                zone.getType().name(),
                zone.getVertices(),
                parseColor(zone.getFillColor()),
                parseColor(zone.getBorderColor()),
                zone.getOpacity()
            ));
        }
        
        player.sendPacket(new SyncMapZonesPacket(overlays));
    }
    
    public void addCustomMarker(Player player, String id, String name, double x, double y, double z, String icon) {
        MapOverlayState state = playerOverlays.get(player.getUuid());
        if (state == null) return;
        
        MapMarker marker = new MapMarker(
            id,
            MapMarker.MarkerType.CUSTOM,
            name,
            x, y, z,
            0xFFFFFF,
            icon,
            true
        );
        
        state.addMarker(marker);
        player.sendPacket(new AddMapMarkerPacket(marker));
    }
    
    public void removeCustomMarker(Player player, String markerId) {
        MapOverlayState state = playerOverlays.get(player.getUuid());
        if (state == null) return;
        
        state.removeMarker(markerId);
        player.sendPacket(new RemoveMapMarkerPacket(markerId));
    }
    
    public void highlightLocation(Player player, double x, double y, double z, int durationMs) {
        player.sendPacket(new HighlightLocationPacket(x, y, z, durationMs));
    }
    
    public void setPlayerMapIcon(Player player, UUID targetPlayer, String icon) {
        player.sendPacket(new SetPlayerMapIconPacket(targetPlayer, icon));
    }
    
    public void showQuestObjective(Player player, String questId, String name, double x, double y, double z) {
        MapMarker marker = new MapMarker(
            "quest-" + questId,
            MapMarker.MarkerType.QUEST_OBJECTIVE,
            name,
            x, y, z,
            0xFFD700,
            "quest_marker",
            true
        );
        
        MapOverlayState state = playerOverlays.get(player.getUuid());
        if (state != null) {
            state.addMarker(marker);
        }
        
        player.sendPacket(new AddMapMarkerPacket(marker));
    }
    
    public void hideQuestObjective(Player player, String questId) {
        String markerId = "quest-" + questId;
        
        MapOverlayState state = playerOverlays.get(player.getUuid());
        if (state != null) {
            state.removeMarker(markerId);
        }
        
        player.sendPacket(new RemoveMapMarkerPacket(markerId));
    }
    
    public record MapMarker(
        String id,
        MarkerType type,
        String name,
        double x, double y, double z,
        int color,
        String icon,
        boolean visible
    ) {
        public enum MarkerType {
            WAYPOINT,
            PUBLIC_WAYPOINT,
            QUEST_OBJECTIVE,
            BOSS,
            POINT_OF_INTEREST,
            PLAYER,
            CUSTOM
        }
    }
    
    public record MapZoneOverlay(
        String id,
        String name,
        String type,
        List<Zone.ZoneVertex> vertices,
        int fillColor,
        int borderColor,
        float opacity
    ) {}
    
    private static int parseColor(String color) {
        if (color == null || color.isEmpty()) return 0xFFFFFF;
        try {
            String hex = color.startsWith("#") ? color.substring(1) : color;
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }
    
    public record SyncMapMarkersPacket(List<MapMarker> markers) {}
    public record SyncMapZonesPacket(List<MapZoneOverlay> zones) {}
    public record AddMapMarkerPacket(MapMarker marker) {}
    public record RemoveMapMarkerPacket(String markerId) {}
    public record HighlightLocationPacket(double x, double y, double z, int durationMs) {}
    public record SetPlayerMapIconPacket(UUID playerId, String icon) {}
    
    private static class MapOverlayState {
        private final UUID playerId;
        private final Map<String, MapMarker> markers = new ConcurrentHashMap<>();
        
        MapOverlayState(UUID playerId) {
            this.playerId = playerId;
        }
        
        void addMarker(MapMarker marker) {
            markers.put(marker.id(), marker);
        }
        
        void removeMarker(String id) {
            markers.remove(id);
        }
        
        Collection<MapMarker> getMarkers() {
            return markers.values();
        }
    }
}
