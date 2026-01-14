package rubidium.optimization;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class ChunkLoadOptimizer {
    
    private static final Logger logger = Logger.getLogger("Rubidium-ChunkOptimizer");
    
    private final Map<UUID, PlayerChunkState> playerStates = new ConcurrentHashMap<>();
    private final Set<ChunkPos> priorityChunks = ConcurrentHashMap.newKeySet();
    
    private int viewDistance = 10;
    private int loadRadius = 3;
    private int unloadDelay = 200;
    private boolean spiralLoading = true;
    private int maxChunksPerTick = 4;
    
    public ChunkLoadOptimizer() {}
    
    public void setViewDistance(int distance) {
        this.viewDistance = distance;
    }
    
    public void setLoadRadius(int radius) {
        this.loadRadius = radius;
    }
    
    public void setUnloadDelay(int ticks) {
        this.unloadDelay = ticks;
    }
    
    public void setMaxChunksPerTick(int max) {
        this.maxChunksPerTick = max;
    }
    
    public void onPlayerJoin(UUID playerId, int chunkX, int chunkZ) {
        PlayerChunkState state = new PlayerChunkState(playerId, chunkX, chunkZ);
        playerStates.put(playerId, state);
    }
    
    public void onPlayerQuit(UUID playerId) {
        playerStates.remove(playerId);
    }
    
    public void onPlayerMove(UUID playerId, int newChunkX, int newChunkZ) {
        PlayerChunkState state = playerStates.get(playerId);
        if (state != null && (state.chunkX != newChunkX || state.chunkZ != newChunkZ)) {
            state.previousChunkX = state.chunkX;
            state.previousChunkZ = state.chunkZ;
            state.chunkX = newChunkX;
            state.chunkZ = newChunkZ;
            state.lastMoveTime = System.currentTimeMillis();
        }
    }
    
    public List<ChunkPos> getChunksToLoad(UUID playerId) {
        PlayerChunkState state = playerStates.get(playerId);
        if (state == null) return Collections.emptyList();
        
        List<ChunkPos> chunks = new ArrayList<>();
        
        if (spiralLoading) {
            chunks = generateSpiralOrder(state.chunkX, state.chunkZ, loadRadius);
        } else {
            for (int dx = -loadRadius; dx <= loadRadius; dx++) {
                for (int dz = -loadRadius; dz <= loadRadius; dz++) {
                    chunks.add(new ChunkPos(state.chunkX + dx, state.chunkZ + dz));
                }
            }
        }
        
        chunks.removeAll(state.loadedChunks);
        
        if (chunks.size() > maxChunksPerTick) {
            chunks = chunks.subList(0, maxChunksPerTick);
        }
        
        return chunks;
    }
    
    public List<ChunkPos> getChunksToUnload(UUID playerId) {
        PlayerChunkState state = playerStates.get(playerId);
        if (state == null) return Collections.emptyList();
        
        List<ChunkPos> toUnload = new ArrayList<>();
        
        for (ChunkPos chunk : state.loadedChunks) {
            int dx = Math.abs(chunk.x() - state.chunkX);
            int dz = Math.abs(chunk.z() - state.chunkZ);
            
            if (dx > viewDistance || dz > viewDistance) {
                Long unloadTime = state.unloadScheduled.get(chunk);
                if (unloadTime == null) {
                    state.unloadScheduled.put(chunk, System.currentTimeMillis());
                } else if (System.currentTimeMillis() - unloadTime > unloadDelay * 50L) {
                    toUnload.add(chunk);
                }
            } else {
                state.unloadScheduled.remove(chunk);
            }
        }
        
        return toUnload;
    }
    
    public void markChunkLoaded(UUID playerId, ChunkPos chunk) {
        PlayerChunkState state = playerStates.get(playerId);
        if (state != null) {
            state.loadedChunks.add(chunk);
            state.unloadScheduled.remove(chunk);
        }
    }
    
    public void markChunkUnloaded(UUID playerId, ChunkPos chunk) {
        PlayerChunkState state = playerStates.get(playerId);
        if (state != null) {
            state.loadedChunks.remove(chunk);
            state.unloadScheduled.remove(chunk);
        }
    }
    
    public void addPriorityChunk(ChunkPos chunk) {
        priorityChunks.add(chunk);
    }
    
    public void removePriorityChunk(ChunkPos chunk) {
        priorityChunks.remove(chunk);
    }
    
    public boolean isPriorityChunk(ChunkPos chunk) {
        return priorityChunks.contains(chunk);
    }
    
    private List<ChunkPos> generateSpiralOrder(int centerX, int centerZ, int radius) {
        List<ChunkPos> result = new ArrayList<>();
        result.add(new ChunkPos(centerX, centerZ));
        
        for (int layer = 1; layer <= radius; layer++) {
            int x = centerX - layer;
            int z = centerZ - layer;
            
            for (int i = 0; i < layer * 2; i++) {
                result.add(new ChunkPos(x + i, z));
            }
            x = centerX + layer;
            for (int i = 0; i < layer * 2; i++) {
                result.add(new ChunkPos(x, z + i));
            }
            z = centerZ + layer;
            for (int i = 0; i < layer * 2; i++) {
                result.add(new ChunkPos(x - i, z));
            }
            x = centerX - layer;
            for (int i = 0; i < layer * 2; i++) {
                result.add(new ChunkPos(x, z - i));
            }
        }
        
        return result;
    }
    
    public record ChunkPos(int x, int z) {}
    
    private static class PlayerChunkState {
        final UUID playerId;
        int chunkX;
        int chunkZ;
        int previousChunkX;
        int previousChunkZ;
        long lastMoveTime;
        final Set<ChunkPos> loadedChunks = ConcurrentHashMap.newKeySet();
        final Map<ChunkPos, Long> unloadScheduled = new ConcurrentHashMap<>();
        
        PlayerChunkState(UUID playerId, int chunkX, int chunkZ) {
            this.playerId = playerId;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.previousChunkX = chunkX;
            this.previousChunkZ = chunkZ;
            this.lastMoveTime = System.currentTimeMillis();
        }
    }
}
