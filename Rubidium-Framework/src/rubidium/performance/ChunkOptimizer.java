package rubidium.performance;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ChunkOptimizer {
    private final Map<ChunkKey, ChunkData> loadedChunks = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanupTime = new AtomicLong(System.currentTimeMillis());
    
    private final long unloadDelayMs;
    private final int maxLoadedChunks;
    private final long cleanupIntervalMs;
    
    public ChunkOptimizer() {
        this(30000, 1000, 5000);
    }
    
    public ChunkOptimizer(long unloadDelayMs, int maxLoadedChunks, long cleanupIntervalMs) {
        this.unloadDelayMs = unloadDelayMs;
        this.maxLoadedChunks = maxLoadedChunks;
        this.cleanupIntervalMs = cleanupIntervalMs;
    }
    
    public void markChunkAccessed(int worldId, int x, int z) {
        ChunkKey key = new ChunkKey(worldId, x, z);
        loadedChunks.computeIfAbsent(key, k -> new ChunkData()).markAccessed();
    }
    
    public List<ChunkKey> getChunksToUnload() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime.get() < cleanupIntervalMs) {
            return Collections.emptyList();
        }
        lastCleanupTime.set(now);
        
        List<ChunkKey> toUnload = new ArrayList<>();
        long threshold = now - unloadDelayMs;
        
        for (Map.Entry<ChunkKey, ChunkData> entry : loadedChunks.entrySet()) {
            if (entry.getValue().lastAccessTime < threshold) {
                toUnload.add(entry.getKey());
            }
        }
        
        if (loadedChunks.size() > maxLoadedChunks) {
            List<Map.Entry<ChunkKey, ChunkData>> sorted = new ArrayList<>(loadedChunks.entrySet());
            sorted.sort(Comparator.comparingLong(e -> e.getValue().lastAccessTime));
            
            int toRemove = loadedChunks.size() - maxLoadedChunks;
            for (int i = 0; i < toRemove && i < sorted.size(); i++) {
                ChunkKey key = sorted.get(i).getKey();
                if (!toUnload.contains(key)) {
                    toUnload.add(key);
                }
            }
        }
        
        toUnload.forEach(loadedChunks::remove);
        return toUnload;
    }
    
    public int getLoadedChunkCount() {
        return loadedChunks.size();
    }
    
    public void clear() {
        loadedChunks.clear();
    }
    
    public record ChunkKey(int worldId, int x, int z) {}
    
    private static class ChunkData {
        volatile long lastAccessTime;
        volatile int accessCount;
        
        void markAccessed() {
            lastAccessTime = System.currentTimeMillis();
            accessCount++;
        }
    }
}
