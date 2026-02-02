package rubidium.api.map;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class MapAPI {
    private static MapAPI instance;
    private static final Logger logger = Logger.getLogger("Rubidium-MapAPI");
    
    private final Map<UUID, PlayerMapData> playerMaps = new ConcurrentHashMap<>();
    private final Map<String, WorldMapData> worldMaps = new ConcurrentHashMap<>();
    private final Map<String, List<Waypoint>> globalWaypoints = new ConcurrentHashMap<>();
    private final MapConfig config;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    public static MapAPI get() {
        if (instance == null) {
            instance = new MapAPI();
        }
        return instance;
    }
    
    private MapAPI() {
        this.config = new MapConfig();
        startAutoSave();
        logger.info("[Rubidium] MapAPI initialized - BetterMap-style features enabled");
    }
    
    public PlayerMapData getPlayerMap(UUID playerId) {
        return playerMaps.computeIfAbsent(playerId, id -> new PlayerMapData(id));
    }
    
    public WorldMapData getWorldMap(String worldName) {
        return worldMaps.computeIfAbsent(worldName, name -> new WorldMapData(name));
    }
    
    public void recordExploration(UUID playerId, String worldName, int chunkX, int chunkZ) {
        PlayerMapData playerMap = getPlayerMap(playerId);
        playerMap.markExplored(worldName, chunkX, chunkZ);
        
        if (config.isSharedExploration()) {
            WorldMapData worldMap = getWorldMap(worldName);
            worldMap.markExplored(chunkX, chunkZ, playerId);
        }
    }
    
    public boolean isExplored(UUID playerId, String worldName, int chunkX, int chunkZ) {
        if (config.isSharedExploration()) {
            return getWorldMap(worldName).isExplored(chunkX, chunkZ);
        }
        return getPlayerMap(playerId).isExplored(worldName, chunkX, chunkZ);
    }
    
    public Waypoint createWaypoint(UUID playerId, String name, String worldName, double x, double y, double z) {
        Waypoint waypoint = new Waypoint(UUID.randomUUID(), playerId, name, worldName, x, y, z);
        getPlayerMap(playerId).addWaypoint(waypoint);
        logger.info("[Rubidium] Created waypoint '" + name + "' for player " + playerId);
        return waypoint;
    }
    
    public Waypoint createGlobalWaypoint(String name, String worldName, double x, double y, double z, int color) {
        Waypoint waypoint = new Waypoint(UUID.randomUUID(), null, name, worldName, x, y, z);
        waypoint.setColor(color);
        waypoint.setGlobal(true);
        globalWaypoints.computeIfAbsent(worldName, k -> new CopyOnWriteArrayList<>()).add(waypoint);
        logger.info("[Rubidium] Created global waypoint '" + name + "'");
        return waypoint;
    }
    
    public List<Waypoint> getPlayerWaypoints(UUID playerId) {
        return getPlayerMap(playerId).getWaypoints();
    }
    
    public List<Waypoint> getGlobalWaypoints(String worldName) {
        return globalWaypoints.getOrDefault(worldName, Collections.emptyList());
    }
    
    public List<Waypoint> getAllVisibleWaypoints(UUID playerId, String worldName) {
        List<Waypoint> visible = new ArrayList<>();
        visible.addAll(getPlayerWaypoints(playerId));
        visible.addAll(getGlobalWaypoints(worldName));
        return visible;
    }
    
    public void deleteWaypoint(UUID playerId, UUID waypointId) {
        getPlayerMap(playerId).removeWaypoint(waypointId);
    }
    
    public void shareWaypoint(UUID fromPlayer, UUID toPlayer, UUID waypointId) {
        PlayerMapData fromMap = getPlayerMap(fromPlayer);
        Waypoint original = fromMap.getWaypoint(waypointId);
        if (original != null) {
            Waypoint shared = original.copy();
            shared.setSharedFrom(fromPlayer);
            getPlayerMap(toPlayer).addWaypoint(shared);
        }
    }
    
    public void setPlayerZoom(UUID playerId, float minZoom, float maxZoom) {
        PlayerMapData playerMap = getPlayerMap(playerId);
        playerMap.setMinZoom(Math.max(config.getMinZoomLimit(), minZoom));
        playerMap.setMaxZoom(Math.min(config.getMaxZoomLimit(), maxZoom));
    }
    
    public List<PlayerRadarEntry> getRadarEntries(UUID playerId, String worldName, double x, double z, double range) {
        if (!config.isRadarEnabled()) {
            return Collections.emptyList();
        }
        
        List<PlayerRadarEntry> entries = new ArrayList<>();
        double effectiveRange = config.getRadarRange() < 0 ? Double.MAX_VALUE : Math.min(range, config.getRadarRange());
        
        for (Map.Entry<UUID, PlayerMapData> entry : playerMaps.entrySet()) {
            if (entry.getKey().equals(playerId)) continue;
            
            PlayerMapData otherPlayer = entry.getValue();
            if (otherPlayer.getCurrentWorld() != null && otherPlayer.getCurrentWorld().equals(worldName)) {
                double dx = otherPlayer.getLastX() - x;
                double dz = otherPlayer.getLastZ() - z;
                double distance = Math.sqrt(dx * dx + dz * dz);
                
                if (distance <= effectiveRange) {
                    double angle = Math.atan2(dz, dx);
                    entries.add(new PlayerRadarEntry(entry.getKey(), distance, angle));
                }
            }
        }
        return entries;
    }
    
    public void updatePlayerPosition(UUID playerId, String worldName, double x, double y, double z, float yaw) {
        PlayerMapData playerMap = getPlayerMap(playerId);
        playerMap.setCurrentWorld(worldName);
        playerMap.setPosition(x, y, z, yaw);
        
        int chunkX = (int) Math.floor(x / 16);
        int chunkZ = (int) Math.floor(z / 16);
        int radius = config.getExplorationRadius();
        
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                recordExploration(playerId, worldName, chunkX + dx, chunkZ + dz);
            }
        }
    }
    
    public LocationOverlay getLocationOverlay(UUID playerId) {
        PlayerMapData playerMap = getPlayerMap(playerId);
        return new LocationOverlay(
            playerMap.getCurrentWorld(),
            playerMap.getLastX(),
            playerMap.getLastY(),
            playerMap.getLastZ(),
            playerMap.getLastYaw()
        );
    }
    
    public MapConfig getConfig() {
        return config;
    }
    
    private void startAutoSave() {
        int interval = config.getAutoSaveInterval();
        scheduler.scheduleAtFixedRate(this::saveAllData, interval, interval, TimeUnit.MINUTES);
    }
    
    public void saveAllData() {
        logger.info("[Rubidium] Auto-saving map data...");
    }
    
    public void shutdown() {
        scheduler.shutdown();
        saveAllData();
    }
    
    public static class PlayerMapData {
        private final UUID playerId;
        private final Map<String, Set<Long>> exploredChunks = new ConcurrentHashMap<>();
        private final List<Waypoint> waypoints = new CopyOnWriteArrayList<>();
        private String currentWorld;
        private double lastX, lastY, lastZ;
        private float lastYaw;
        private float minZoom = 10f;
        private float maxZoom = 256f;
        
        public PlayerMapData(UUID playerId) {
            this.playerId = playerId;
        }
        
        public void markExplored(String world, int chunkX, int chunkZ) {
            exploredChunks.computeIfAbsent(world, k -> ConcurrentHashMap.newKeySet())
                .add(chunkKey(chunkX, chunkZ));
        }
        
        public boolean isExplored(String world, int chunkX, int chunkZ) {
            Set<Long> chunks = exploredChunks.get(world);
            return chunks != null && chunks.contains(chunkKey(chunkX, chunkZ));
        }
        
        public int getExploredChunkCount(String world) {
            Set<Long> chunks = exploredChunks.get(world);
            return chunks != null ? chunks.size() : 0;
        }
        
        private long chunkKey(int x, int z) {
            return ((long) x << 32) | (z & 0xFFFFFFFFL);
        }
        
        public void addWaypoint(Waypoint waypoint) { waypoints.add(waypoint); }
        public void removeWaypoint(UUID waypointId) { waypoints.removeIf(w -> w.getId().equals(waypointId)); }
        public List<Waypoint> getWaypoints() { return new ArrayList<>(waypoints); }
        public Waypoint getWaypoint(UUID id) { return waypoints.stream().filter(w -> w.getId().equals(id)).findFirst().orElse(null); }
        
        public void setPosition(double x, double y, double z, float yaw) {
            this.lastX = x; this.lastY = y; this.lastZ = z; this.lastYaw = yaw;
        }
        
        public void setCurrentWorld(String world) { this.currentWorld = world; }
        public String getCurrentWorld() { return currentWorld; }
        public double getLastX() { return lastX; }
        public double getLastY() { return lastY; }
        public double getLastZ() { return lastZ; }
        public float getLastYaw() { return lastYaw; }
        public void setMinZoom(float zoom) { this.minZoom = zoom; }
        public void setMaxZoom(float zoom) { this.maxZoom = zoom; }
        public float getMinZoom() { return minZoom; }
        public float getMaxZoom() { return maxZoom; }
    }
    
    public static class WorldMapData {
        private final String worldName;
        private final Set<Long> exploredChunks = ConcurrentHashMap.newKeySet();
        private final Map<Long, UUID> explorerMap = new ConcurrentHashMap<>();
        
        public WorldMapData(String worldName) {
            this.worldName = worldName;
        }
        
        public void markExplored(int chunkX, int chunkZ, UUID explorer) {
            long key = chunkKey(chunkX, chunkZ);
            exploredChunks.add(key);
            explorerMap.putIfAbsent(key, explorer);
        }
        
        public boolean isExplored(int chunkX, int chunkZ) {
            return exploredChunks.contains(chunkKey(chunkX, chunkZ));
        }
        
        public UUID getExplorer(int chunkX, int chunkZ) {
            return explorerMap.get(chunkKey(chunkX, chunkZ));
        }
        
        public int getTotalExploredChunks() {
            return exploredChunks.size();
        }
        
        private long chunkKey(int x, int z) {
            return ((long) x << 32) | (z & 0xFFFFFFFFL);
        }
    }
    
    public static class Waypoint {
        private final UUID id;
        private UUID owner;
        private String name;
        private final String worldName;
        private final double x, y, z;
        private int color = 0xFFFFFF;
        private boolean global = false;
        private UUID sharedFrom;
        
        public Waypoint(UUID id, UUID owner, String name, String worldName, double x, double y, double z) {
            this.id = id;
            this.owner = owner;
            this.name = name;
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        public UUID getId() { return id; }
        public UUID getOwner() { return owner; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getWorldName() { return worldName; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public int getColor() { return color; }
        public void setColor(int color) { this.color = color; }
        public boolean isGlobal() { return global; }
        public void setGlobal(boolean global) { this.global = global; }
        public UUID getSharedFrom() { return sharedFrom; }
        public void setSharedFrom(UUID sharedFrom) { this.sharedFrom = sharedFrom; }
        
        public Waypoint copy() {
            Waypoint copy = new Waypoint(UUID.randomUUID(), owner, name, worldName, x, y, z);
            copy.setColor(color);
            return copy;
        }
        
        public double distanceTo(double px, double py, double pz) {
            double dx = x - px;
            double dy = y - py;
            double dz = z - pz;
            return Math.sqrt(dx*dx + dy*dy + dz*dz);
        }
    }
    
    public static class MapConfig {
        private int explorationRadius = 16;
        private int updateRateMs = 500;
        private MapQuality mapQuality = MapQuality.MEDIUM;
        private float minZoomLimit = 10f;
        private float maxZoomLimit = 256f;
        private boolean sharedExploration = false;
        private int maxChunksToLoad = 10000;
        private boolean radarEnabled = true;
        private double radarRange = -1;
        private boolean hidePlayersOnMap = false;
        private boolean hideUnexploredWaypoints = true;
        private boolean allowWaypointTeleport = true;
        private boolean allowMarkerTeleport = true;
        private int autoSaveInterval = 5;
        private final Set<String> allowedWorlds = ConcurrentHashMap.newKeySet();
        
        public MapConfig() {
            allowedWorlds.add("default");
            allowedWorlds.add("world");
        }
        
        public int getExplorationRadius() { return explorationRadius; }
        public void setExplorationRadius(int radius) { this.explorationRadius = radius; }
        public int getUpdateRateMs() { return updateRateMs; }
        public void setUpdateRateMs(int rate) { this.updateRateMs = rate; }
        public MapQuality getMapQuality() { return mapQuality; }
        public void setMapQuality(MapQuality quality) { this.mapQuality = quality; }
        public float getMinZoomLimit() { return minZoomLimit; }
        public float getMaxZoomLimit() { return maxZoomLimit; }
        public boolean isSharedExploration() { return sharedExploration; }
        public void setSharedExploration(boolean shared) { this.sharedExploration = shared; }
        public int getMaxChunksToLoad() { return maxChunksToLoad; }
        public void setMaxChunksToLoad(int max) { this.maxChunksToLoad = max; }
        public boolean isRadarEnabled() { return radarEnabled; }
        public void setRadarEnabled(boolean enabled) { this.radarEnabled = enabled; }
        public double getRadarRange() { return radarRange; }
        public void setRadarRange(double range) { this.radarRange = range; }
        public boolean isHidePlayersOnMap() { return hidePlayersOnMap; }
        public boolean isHideUnexploredWaypoints() { return hideUnexploredWaypoints; }
        public boolean isAllowWaypointTeleport() { return allowWaypointTeleport; }
        public boolean isAllowMarkerTeleport() { return allowMarkerTeleport; }
        public int getAutoSaveInterval() { return autoSaveInterval; }
        public void setAutoSaveInterval(int minutes) { this.autoSaveInterval = minutes; }
        public Set<String> getAllowedWorlds() { return allowedWorlds; }
        public void addAllowedWorld(String world) { allowedWorlds.add(world); }
        public void removeAllowedWorld(String world) { allowedWorlds.remove(world); }
        public boolean isWorldAllowed(String world) { return allowedWorlds.contains(world); }
    }
    
    public enum MapQuality {
        LOW(30000, 8),
        MEDIUM(10000, 16),
        HIGH(3000, 32);
        
        private final int maxChunks;
        private final int resolution;
        
        MapQuality(int maxChunks, int resolution) {
            this.maxChunks = maxChunks;
            this.resolution = resolution;
        }
        
        public int getMaxChunks() { return maxChunks; }
        public int getResolution() { return resolution; }
    }
    
    public static class PlayerRadarEntry {
        private final UUID playerId;
        private final double distance;
        private final double angle;
        
        public PlayerRadarEntry(UUID playerId, double distance, double angle) {
            this.playerId = playerId;
            this.distance = distance;
            this.angle = angle;
        }
        
        public UUID getPlayerId() { return playerId; }
        public double getDistance() { return distance; }
        public double getAngle() { return angle; }
        public double getAngleDegrees() { return Math.toDegrees(angle); }
    }
    
    public static class LocationOverlay {
        private final String world;
        private final double x, y, z;
        private final float yaw;
        
        public LocationOverlay(String world, double x, double y, double z, float yaw) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
        }
        
        public String getWorld() { return world; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public float getYaw() { return yaw; }
        public String getDirection() {
            float normalizedYaw = ((yaw % 360) + 360) % 360;
            if (normalizedYaw >= 315 || normalizedYaw < 45) return "South";
            if (normalizedYaw >= 45 && normalizedYaw < 135) return "West";
            if (normalizedYaw >= 135 && normalizedYaw < 225) return "North";
            return "East";
        }
        public String getFormattedCoords() {
            return String.format("X: %.1f Y: %.1f Z: %.1f", x, y, z);
        }
    }
}
