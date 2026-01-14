package rubidium.performance;

import java.util.*;
import java.util.concurrent.*;

/**
 * Async chunk loading system.
 */
public class ChunkLoader {
    
    private static ChunkLoader instance;
    
    private final ExecutorService loadExecutor;
    private final Map<ChunkPosition, CompletableFuture<ChunkData>> pendingLoads;
    private final Map<ChunkPosition, ChunkData> chunkCache;
    private final int cacheSize;
    
    private ChunkLoader() {
        this.loadExecutor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "Rubidium-ChunkLoader");
            t.setDaemon(true);
            return t;
        });
        this.pendingLoads = new ConcurrentHashMap<>();
        this.chunkCache = new ConcurrentHashMap<>();
        this.cacheSize = 256;
    }
    
    public static ChunkLoader getInstance() {
        if (instance == null) {
            instance = new ChunkLoader();
        }
        return instance;
    }
    
    public CompletableFuture<ChunkData> loadChunkAsync(String world, int x, int z) {
        ChunkPosition pos = new ChunkPosition(world, x, z);
        
        ChunkData cached = chunkCache.get(pos);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        return pendingLoads.computeIfAbsent(pos, p -> 
            CompletableFuture.supplyAsync(() -> loadChunk(p), loadExecutor)
                .whenComplete((data, error) -> {
                    pendingLoads.remove(p);
                    if (data != null) {
                        cacheChunk(p, data);
                    }
                })
        );
    }
    
    private ChunkData loadChunk(ChunkPosition pos) {
        return new ChunkData(pos.world(), pos.x(), pos.z());
    }
    
    private void cacheChunk(ChunkPosition pos, ChunkData data) {
        if (chunkCache.size() >= cacheSize) {
            Iterator<ChunkPosition> iter = chunkCache.keySet().iterator();
            if (iter.hasNext()) {
                iter.next();
                iter.remove();
            }
        }
        chunkCache.put(pos, data);
    }
    
    public void unloadChunk(String world, int x, int z) {
        chunkCache.remove(new ChunkPosition(world, x, z));
    }
    
    public boolean isChunkLoaded(String world, int x, int z) {
        return chunkCache.containsKey(new ChunkPosition(world, x, z));
    }
    
    public void clearCache() {
        chunkCache.clear();
    }
    
    public int getCacheSize() {
        return chunkCache.size();
    }
    
    public void shutdown() {
        loadExecutor.shutdown();
    }
    
    public record ChunkPosition(String world, int x, int z) {}
    
    public static class ChunkData {
        private final String world;
        private final int x;
        private final int z;
        private final long loadTime;
        
        public ChunkData(String world, int x, int z) {
            this.world = world;
            this.x = x;
            this.z = z;
            this.loadTime = System.currentTimeMillis();
        }
        
        public String getWorld() { return world; }
        public int getX() { return x; }
        public int getZ() { return z; }
        public long getLoadTime() { return loadTime; }
    }
}
