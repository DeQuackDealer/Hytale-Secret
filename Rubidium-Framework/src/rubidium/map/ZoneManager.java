package rubidium.map;

import rubidium.hytale.api.player.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages map zones.
 */
public class ZoneManager {
    
    private static final Logger logger = Logger.getLogger("Rubidium-Zones");
    
    private final Map<String, Zone> zones = new ConcurrentHashMap<>();
    
    public void loadZones() {
        logger.info("Loading zones...");
    }
    
    public void saveZones() {
        logger.info("Saving zones...");
    }
    
    public Zone createZone(String id, String name, Zone.ZoneType type) {
        Zone zone = new Zone(id, name, type);
        zones.put(id, zone);
        logger.info("Created zone: " + name + " (" + type + ")");
        return zone;
    }
    
    public void deleteZone(String id) {
        Zone removed = zones.remove(id);
        if (removed != null) {
            logger.info("Deleted zone: " + removed.getName());
        }
    }
    
    public Optional<Zone> getZone(String id) {
        return Optional.ofNullable(zones.get(id));
    }
    
    public Collection<Zone> getAllZones() {
        return Collections.unmodifiableCollection(zones.values());
    }
    
    public List<Zone> getZonesByType(Zone.ZoneType type) {
        return zones.values().stream()
            .filter(z -> z.getType() == type)
            .toList();
    }
    
    public List<Zone> getZonesAt(double x, double y, double z, String world) {
        final double px = x, py = y, pz = z;
        return zones.values().stream()
            .filter(zone -> zone.getWorld() == null || zone.getWorld().equals(world))
            .filter(zone -> zone.contains(px, py, pz))
            .toList();
    }
    
    public Optional<Zone> getPrimaryZoneAt(double x, double y, double z, String world) {
        return getZonesAt(x, y, z, world).stream()
            .min(Comparator.comparingDouble(Zone::getArea));
    }
    
    public List<Zone> getVisibleZones(Player player) {
        return zones.values().stream()
            .filter(Zone::isVisible)
            .toList();
    }
    
    public boolean isInZoneType(double x, double y, double z, String world, Zone.ZoneType type) {
        return getZonesAt(x, y, z, world).stream()
            .anyMatch(zone -> zone.getType() == type);
    }
    
    public boolean isSafeZone(double x, double y, double z, String world) {
        return isInZoneType(x, y, z, world, Zone.ZoneType.SAFE) ||
               isInZoneType(x, y, z, world, Zone.ZoneType.TOWN);
    }
    
    public boolean isPvPZone(double x, double y, double z, String world) {
        return isInZoneType(x, y, z, world, Zone.ZoneType.PVP);
    }
    
    public int getZoneCount() {
        return zones.size();
    }
}
