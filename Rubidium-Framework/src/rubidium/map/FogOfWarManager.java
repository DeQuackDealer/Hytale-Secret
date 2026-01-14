package rubidium.map;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages fog of war for player map exploration.
 */
public class FogOfWarManager {
    
    private static final int CHUNK_SIZE = 16;
    
    private final Map<UUID, Set<MapService.ChunkCoord>> playerRevealedChunks = new ConcurrentHashMap<>();
    private final Set<MapService.ChunkCoord> globallyRevealed = ConcurrentHashMap.newKeySet();
    
    private boolean fogOfWarEnabled = true;
    private int defaultRevealRadius = 5;
    
    public void initializePlayer(UUID playerId) {
        playerRevealedChunks.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet());
    }
    
    public void revealArea(UUID playerId, double x, double z, int chunkRadius) {
        if (!fogOfWarEnabled) return;
        
        Set<MapService.ChunkCoord> revealed = playerRevealedChunks.get(playerId);
        if (revealed == null) return;
        
        int centerChunkX = (int) Math.floor(x / CHUNK_SIZE);
        int centerChunkZ = (int) Math.floor(z / CHUNK_SIZE);
        
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                if (dx * dx + dz * dz <= chunkRadius * chunkRadius) {
                    revealed.add(new MapService.ChunkCoord(centerChunkX + dx, centerChunkZ + dz));
                }
            }
        }
    }
    
    public void revealChunk(UUID playerId, int chunkX, int chunkZ) {
        Set<MapService.ChunkCoord> revealed = playerRevealedChunks.get(playerId);
        if (revealed != null) {
            revealed.add(new MapService.ChunkCoord(chunkX, chunkZ));
        }
    }
    
    public void revealAll(UUID playerId) {
        Set<MapService.ChunkCoord> revealed = playerRevealedChunks.get(playerId);
        if (revealed != null) {
            for (int x = -1000; x <= 1000; x++) {
                for (int z = -1000; z <= 1000; z++) {
                    revealed.add(new MapService.ChunkCoord(x, z));
                }
            }
        }
    }
    
    public void revealGlobally(int chunkX, int chunkZ) {
        globallyRevealed.add(new MapService.ChunkCoord(chunkX, chunkZ));
    }
    
    public void revealGlobalArea(double x, double z, int chunkRadius) {
        int centerChunkX = (int) Math.floor(x / CHUNK_SIZE);
        int centerChunkZ = (int) Math.floor(z / CHUNK_SIZE);
        
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                if (dx * dx + dz * dz <= chunkRadius * chunkRadius) {
                    globallyRevealed.add(new MapService.ChunkCoord(centerChunkX + dx, centerChunkZ + dz));
                }
            }
        }
    }
    
    public boolean isRevealed(UUID playerId, double x, double z) {
        if (!fogOfWarEnabled) return true;
        
        int chunkX = (int) Math.floor(x / CHUNK_SIZE);
        int chunkZ = (int) Math.floor(z / CHUNK_SIZE);
        
        MapService.ChunkCoord coord = new MapService.ChunkCoord(chunkX, chunkZ);
        
        if (globallyRevealed.contains(coord)) return true;
        
        Set<MapService.ChunkCoord> revealed = playerRevealedChunks.get(playerId);
        return revealed != null && revealed.contains(coord);
    }
    
    public Set<MapService.ChunkCoord> getRevealedChunks(UUID playerId) {
        Set<MapService.ChunkCoord> playerChunks = playerRevealedChunks.getOrDefault(playerId, Set.of());
        Set<MapService.ChunkCoord> combined = new HashSet<>(globallyRevealed);
        combined.addAll(playerChunks);
        return combined;
    }
    
    public void resetPlayer(UUID playerId) {
        Set<MapService.ChunkCoord> revealed = playerRevealedChunks.get(playerId);
        if (revealed != null) {
            revealed.clear();
        }
    }
    
    public void removePlayer(UUID playerId) {
        playerRevealedChunks.remove(playerId);
    }
    
    public boolean isFogOfWarEnabled() { return fogOfWarEnabled; }
    public void setFogOfWarEnabled(boolean enabled) { this.fogOfWarEnabled = enabled; }
    
    public int getDefaultRevealRadius() { return defaultRevealRadius; }
    public void setDefaultRevealRadius(int radius) { this.defaultRevealRadius = radius; }
    
    public int getRevealedChunkCount(UUID playerId) {
        Set<MapService.ChunkCoord> revealed = playerRevealedChunks.get(playerId);
        return revealed != null ? revealed.size() : 0;
    }
}
