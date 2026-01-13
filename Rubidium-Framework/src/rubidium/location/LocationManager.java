package rubidium.location;

import rubidium.api.player.Player;
import rubidium.core.logging.RubidiumLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LocationManager {
    
    private final RubidiumLogger logger;
    private final Map<String, SavedLocation> savedLocations;
    private final Map<UUID, Map<String, SavedLocation>> playerLocations;
    private final Map<UUID, LocationHistory> playerHistory;
    private int maxHistorySize;
    
    public LocationManager(RubidiumLogger logger) {
        this.logger = logger;
        this.savedLocations = new ConcurrentHashMap<>();
        this.playerLocations = new ConcurrentHashMap<>();
        this.playerHistory = new ConcurrentHashMap<>();
        this.maxHistorySize = 10;
    }
    
    public void saveGlobalLocation(String name, SavedLocation location) {
        savedLocations.put(name.toLowerCase(), location);
        logger.info("Saved global location: " + name);
    }
    
    public Optional<SavedLocation> getGlobalLocation(String name) {
        return Optional.ofNullable(savedLocations.get(name.toLowerCase()));
    }
    
    public void deleteGlobalLocation(String name) {
        savedLocations.remove(name.toLowerCase());
    }
    
    public Collection<String> getGlobalLocationNames() {
        return Collections.unmodifiableSet(savedLocations.keySet());
    }
    
    public void savePlayerLocation(Player player, String name, SavedLocation location) {
        playerLocations
            .computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>())
            .put(name.toLowerCase(), location);
    }
    
    public Optional<SavedLocation> getPlayerLocation(Player player, String name) {
        Map<String, SavedLocation> locs = playerLocations.get(player.getUUID());
        if (locs == null) return Optional.empty();
        return Optional.ofNullable(locs.get(name.toLowerCase()));
    }
    
    public void deletePlayerLocation(Player player, String name) {
        Map<String, SavedLocation> locs = playerLocations.get(player.getUUID());
        if (locs != null) {
            locs.remove(name.toLowerCase());
        }
    }
    
    public Collection<String> getPlayerLocationNames(Player player) {
        Map<String, SavedLocation> locs = playerLocations.get(player.getUUID());
        if (locs == null) return List.of();
        return Collections.unmodifiableSet(locs.keySet());
    }
    
    public void recordPosition(Player player) {
        LocationHistory history = playerHistory.computeIfAbsent(
            player.getUUID(), k -> new LocationHistory(maxHistorySize)
        );
        history.record(SavedLocation.fromPlayer(player));
    }
    
    public Optional<SavedLocation> getLastPosition(Player player) {
        LocationHistory history = playerHistory.get(player.getUUID());
        if (history == null) return Optional.empty();
        return history.getLast();
    }
    
    public List<SavedLocation> getPositionHistory(Player player) {
        LocationHistory history = playerHistory.get(player.getUUID());
        if (history == null) return List.of();
        return history.getAll();
    }
    
    public void clearHistory(Player player) {
        playerHistory.remove(player.getUUID());
    }
    
    public void setMaxHistorySize(int size) {
        this.maxHistorySize = Math.max(1, size);
    }
    
    public double calculateDistance(SavedLocation a, SavedLocation b) {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        double dz = a.z() - b.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    public Optional<String> findNearestGlobalLocation(Player player, double maxDistance) {
        double px = player.getLocation().x();
        double py = player.getLocation().y();
        double pz = player.getLocation().z();
        SavedLocation playerLoc = new SavedLocation(px, py, pz, 0, 0, player.getWorld());
        
        String nearest = null;
        double nearestDist = maxDistance;
        
        for (Map.Entry<String, SavedLocation> entry : savedLocations.entrySet()) {
            double dist = calculateDistance(playerLoc, entry.getValue());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = entry.getKey();
            }
        }
        
        return Optional.ofNullable(nearest);
    }
    
    public record SavedLocation(double x, double y, double z, float yaw, float pitch, String world) {
        public static SavedLocation fromPlayer(Player player) {
            return new SavedLocation(
                player.getLocation().x(),
                player.getLocation().y(),
                player.getLocation().z(),
                player.getLocation().yaw(),
                player.getLocation().pitch(),
                player.getWorld()
            );
        }
    }
    
    public static class LocationHistory {
        private final int maxSize;
        private final Deque<SavedLocation> history;
        
        public LocationHistory(int maxSize) {
            this.maxSize = maxSize;
            this.history = new ArrayDeque<>(maxSize);
        }
        
        public void record(SavedLocation location) {
            if (history.size() >= maxSize) {
                history.pollFirst();
            }
            history.addLast(location);
        }
        
        public Optional<SavedLocation> getLast() {
            return Optional.ofNullable(history.peekLast());
        }
        
        public List<SavedLocation> getAll() {
            return new ArrayList<>(history);
        }
        
        public void clear() {
            history.clear();
        }
    }
}
