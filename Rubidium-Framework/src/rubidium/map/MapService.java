package rubidium.map;

import rubidium.core.feature.*;
import rubidium.hytale.api.player.Player;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Map service providing minimap, world map, waypoints, and zone visualization.
 */
public class MapService implements FeatureLifecycle {
    
    private static final Logger logger = Logger.getLogger("Rubidium-Map");
    
    private static MapService instance;
    
    private final MapDataProvider dataProvider;
    private final WaypointManager waypointManager;
    private final ZoneManager zoneManager;
    private final FogOfWarManager fogOfWarManager;
    private final MapRenderer renderer;
    
    private final Map<UUID, PlayerMapState> playerStates = new ConcurrentHashMap<>();
    private final ScheduledExecutorService updateScheduler;
    
    private volatile boolean running = false;
    
    public MapService() {
        this.dataProvider = new MapDataProvider();
        this.waypointManager = new WaypointManager();
        this.zoneManager = new ZoneManager();
        this.fogOfWarManager = new FogOfWarManager();
        this.renderer = new MapRenderer();
        this.updateScheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "MapService-Updater");
            t.setDaemon(true);
            return t;
        });
    }
    
    public static MapService getInstance() {
        if (instance == null) {
            instance = new MapService();
        }
        return instance;
    }
    
    @Override
    public String getFeatureId() { return "map"; }
    
    @Override
    public String getFeatureName() { return "Map System"; }
    
    @Override
    public void initialize() throws FeatureInitException {
        logger.info("Initializing Map Service...");
        
        try {
            dataProvider.initialize();
            waypointManager.loadWaypoints();
            zoneManager.loadZones();
            
        } catch (Exception e) {
            throw new FeatureInitException("Failed to initialize map service", e);
        }
    }
    
    @Override
    public void start() {
        running = true;
        
        updateScheduler.scheduleAtFixedRate(
            this::updatePlayerPositions,
            0, 100, TimeUnit.MILLISECONDS
        );
        
        updateScheduler.scheduleAtFixedRate(
            this::broadcastMapUpdates,
            0, 500, TimeUnit.MILLISECONDS
        );
        
        logger.info("Map Service started");
    }
    
    @Override
    public void stop() {
        running = false;
        updateScheduler.shutdown();
        logger.info("Map Service stopped");
    }
    
    @Override
    public void shutdown() {
        stop();
        waypointManager.saveWaypoints();
        zoneManager.saveZones();
    }
    
    @Override
    public FeatureHealth healthCheck() {
        if (!running) {
            return FeatureHealth.disabled("Map service not running");
        }
        
        return FeatureHealth.healthy()
            .withMetric("activePlayers", playerStates.size())
            .withMetric("waypoints", waypointManager.getTotalWaypointCount())
            .withMetric("zones", zoneManager.getZoneCount());
    }
    
    public void onPlayerJoin(Player player) {
        PlayerMapState state = new PlayerMapState(player.getUuid());
        playerStates.put(player.getUuid(), state);
        
        fogOfWarManager.initializePlayer(player.getUuid());
        
        sendInitialMapData(player);
    }
    
    public void onPlayerQuit(UUID playerId) {
        playerStates.remove(playerId);
    }
    
    private void sendInitialMapData(Player player) {
        UUID playerId = player.getUuid();
        
        List<Waypoint> playerWaypoints = waypointManager.getPlayerWaypoints(playerId);
        List<Waypoint> publicWaypoints = waypointManager.getPublicWaypoints();
        
        MapInitPacket packet = new MapInitPacket(
            playerWaypoints,
            publicWaypoints,
            zoneManager.getVisibleZones(player),
            fogOfWarManager.getRevealedChunks(playerId)
        );
        
        player.sendPacket(packet);
    }
    
    private void updatePlayerPositions() {
        if (!running) return;
        
        for (PlayerMapState state : playerStates.values()) {
            state.updateFromLocation();
        }
    }
    
    private void broadcastMapUpdates() {
        if (!running) return;
        
        List<PlayerPositionUpdate> updates = new ArrayList<>();
        
        for (PlayerMapState state : playerStates.values()) {
            if (state.isVisible()) {
                updates.add(new PlayerPositionUpdate(
                    state.getPlayerId(),
                    state.getX(),
                    state.getY(),
                    state.getZ(),
                    state.getYaw()
                ));
            }
        }
        
        MapUpdatePacket packet = new MapUpdatePacket(updates);
        
        for (PlayerMapState state : playerStates.values()) {
        }
    }
    
    public Waypoint createWaypoint(UUID ownerId, String name, double x, double y, double z, String world) {
        return waypointManager.createWaypoint(ownerId, name, x, y, z, world);
    }
    
    public void deleteWaypoint(UUID ownerId, String waypointId) {
        waypointManager.deleteWaypoint(ownerId, waypointId);
    }
    
    public List<Waypoint> getPlayerWaypoints(UUID playerId) {
        return waypointManager.getPlayerWaypoints(playerId);
    }
    
    public void shareWaypoint(String waypointId, UUID targetPlayerId) {
        waypointManager.shareWaypoint(waypointId, targetPlayerId);
    }
    
    public Zone createZone(String id, String name, Zone.ZoneType type) {
        return zoneManager.createZone(id, name, type);
    }
    
    public Optional<Zone> getZone(String id) {
        return zoneManager.getZone(id);
    }
    
    public List<Zone> getZonesAt(double x, double y, double z, String world) {
        return zoneManager.getZonesAt(x, y, z, world);
    }
    
    public void revealArea(UUID playerId, double x, double z, int radius) {
        fogOfWarManager.revealArea(playerId, x, z, radius);
    }
    
    public boolean isRevealed(UUID playerId, double x, double z) {
        return fogOfWarManager.isRevealed(playerId, x, z);
    }
    
    public void setPlayerMapVisibility(UUID playerId, boolean visible) {
        PlayerMapState state = playerStates.get(playerId);
        if (state != null) {
            state.setVisible(visible);
        }
    }
    
    public WaypointManager getWaypointManager() { return waypointManager; }
    public ZoneManager getZoneManager() { return zoneManager; }
    public FogOfWarManager getFogOfWarManager() { return fogOfWarManager; }
    
    public record MapInitPacket(
        List<Waypoint> playerWaypoints,
        List<Waypoint> publicWaypoints,
        List<Zone> zones,
        Set<ChunkCoord> revealedChunks
    ) {}
    
    public record MapUpdatePacket(List<PlayerPositionUpdate> positions) {}
    
    public record PlayerPositionUpdate(UUID playerId, double x, double y, double z, float yaw) {}
    
    public record ChunkCoord(int x, int z) {}
}
