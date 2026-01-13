package jurassictale.territory;

import rubidium.core.logging.RubidiumLogger;
import jurassictale.JurassicTaleConfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TerritoryManager {
    
    private final RubidiumLogger logger;
    private final JurassicTaleConfig config;
    
    private final Map<Long, Territory> territories;
    private final Map<Long, TerritoryType> chunkTypes;
    
    public TerritoryManager(RubidiumLogger logger, JurassicTaleConfig config) {
        this.logger = logger;
        this.config = config;
        this.territories = new ConcurrentHashMap<>();
        this.chunkTypes = new ConcurrentHashMap<>();
    }
    
    public Territory getTerritory(int chunkX, int chunkZ) {
        long key = chunkKey(chunkX, chunkZ);
        return territories.computeIfAbsent(key, k -> createTerritory(chunkX, chunkZ));
    }
    
    public TerritoryType getTerritoryType(double x, double z) {
        int chunkX = (int) Math.floor(x / 16);
        int chunkZ = (int) Math.floor(z / 16);
        return getTerritory(chunkX, chunkZ).getType();
    }
    
    public TerritoryType getTerritoryTypeForChunk(int chunkX, int chunkZ) {
        long key = chunkKey(chunkX, chunkZ);
        return chunkTypes.computeIfAbsent(key, k -> generateTerritoryType(chunkX, chunkZ));
    }
    
    private Territory createTerritory(int chunkX, int chunkZ) {
        TerritoryType type = generateTerritoryType(chunkX, chunkZ);
        return new Territory(chunkX, chunkZ, type);
    }
    
    private TerritoryType generateTerritoryType(int chunkX, int chunkZ) {
        double dist = Math.sqrt(chunkX * chunkX + chunkZ * chunkZ);
        
        if (dist < 10) {
            return TerritoryType.GRASSLANDS;
        } else if (dist < 25) {
            double angle = Math.atan2(chunkZ, chunkX);
            if (angle > 0 && angle < Math.PI / 2) {
                return TerritoryType.JUNGLE;
            } else if (angle > Math.PI / 2) {
                return TerritoryType.SWAMP;
            } else if (angle < -Math.PI / 2) {
                return TerritoryType.HIGHLANDS;
            } else {
                return TerritoryType.GRASSLANDS;
            }
        } else if (dist < 40) {
            int hash = (chunkX * 31 + chunkZ) % 4;
            return switch (hash) {
                case 0 -> TerritoryType.JUNGLE;
                case 1 -> TerritoryType.SWAMP;
                case 2 -> TerritoryType.HIGHLANDS;
                default -> TerritoryType.CAVES;
            };
        } else {
            return TerritoryType.STORM_ZONE;
        }
    }
    
    public List<Territory> getTerritoriesInRadius(int centerX, int centerZ, int radiusChunks) {
        List<Territory> result = new ArrayList<>();
        for (int x = centerX - radiusChunks; x <= centerX + radiusChunks; x++) {
            for (int z = centerZ - radiusChunks; z <= centerZ + radiusChunks; z++) {
                result.add(getTerritory(x, z));
            }
        }
        return result;
    }
    
    public boolean isInDangerZone(double x, double z) {
        TerritoryType type = getTerritoryType(x, z);
        return type.getDangerLevel() >= 3;
    }
    
    public boolean isInStormZone(double x, double z) {
        return getTerritoryType(x, z) == TerritoryType.STORM_ZONE;
    }
    
    private long chunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
    
    public record Territory(int chunkX, int chunkZ, TerritoryType type) {
        public long getKey() {
            return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
        }
    }
}
