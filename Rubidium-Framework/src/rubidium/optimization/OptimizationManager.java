package rubidium.optimization;

import rubidium.core.feature.*;
import rubidium.core.scheduler.RubidiumScheduler;

import java.util.logging.Logger;

public class OptimizationManager implements FeatureLifecycle {
    
    private static final Logger logger = Logger.getLogger("Rubidium-Optimization");
    
    private static OptimizationManager instance;
    
    private final PerformanceOptimizer performanceOptimizer;
    private final EntityCulling entityCulling;
    private final ChunkLoadOptimizer chunkOptimizer;
    
    private RubidiumScheduler scheduler;
    private volatile boolean running = false;
    
    public OptimizationManager() {
        this.performanceOptimizer = PerformanceOptimizer.getInstance();
        this.entityCulling = new EntityCulling();
        this.chunkOptimizer = new ChunkLoadOptimizer();
    }
    
    public static OptimizationManager getInstance() {
        if (instance == null) {
            instance = new OptimizationManager();
        }
        return instance;
    }
    
    public void setScheduler(RubidiumScheduler scheduler) {
        this.scheduler = scheduler;
    }
    
    @Override
    public String getFeatureId() { return "optimization"; }
    
    @Override
    public String getFeatureName() { return "Performance Optimization"; }
    
    @Override
    public void initialize() throws FeatureInitException {
        logger.info("Initializing optimization manager...");
    }
    
    @Override
    public void start() {
        running = true;
        
        performanceOptimizer.start();
        
        if (scheduler != null) {
            scheduler.runTaskTimer("optimization-tick", this::tick, 0, 1);
        }
        
        logger.info("Optimization manager started - performance monitoring active");
    }
    
    @Override
    public void stop() {
        running = false;
        performanceOptimizer.stop();
        
        if (scheduler != null) {
            scheduler.cancelAllTasks("optimization-tick");
        }
        
        logger.info("Optimization manager stopped");
    }
    
    @Override
    public void shutdown() {
        stop();
    }
    
    @Override
    public FeatureHealth healthCheck() {
        if (!running) {
            return FeatureHealth.disabled("Optimization not running");
        }
        
        var summary = performanceOptimizer.getSummary();
        return FeatureHealth.healthy()
            .withMetric("heapUsage", summary.heapUsagePercent())
            .withMetric("avgTickMs", summary.avgTickMs())
            .withMetric("optimizationLevel", summary.level().name());
    }
    
    private void tick() {
        if (!running) return;
    }
    
    public void recordTickTime(long nanos) {
        performanceOptimizer.recordTickTime(nanos);
    }
    
    public boolean shouldRenderEntity(java.util.UUID entityId, 
                                      double ex, double ey, double ez,
                                      double px, double py, double pz,
                                      double lookX, double lookY, double lookZ) {
        return entityCulling.shouldRenderEntity(entityId, ex, ey, ez, px, py, pz, lookX, lookY, lookZ);
    }
    
    public boolean shouldUpdateEntity(java.util.UUID entityId,
                                      double ex, double ey, double ez,
                                      double px, double py, double pz) {
        return entityCulling.shouldUpdateEntity(entityId, ex, ey, ez, px, py, pz);
    }
    
    public void onPlayerJoinChunk(java.util.UUID playerId, int chunkX, int chunkZ) {
        chunkOptimizer.onPlayerJoin(playerId, chunkX, chunkZ);
    }
    
    public void onPlayerMoveChunk(java.util.UUID playerId, int newChunkX, int newChunkZ) {
        chunkOptimizer.onPlayerMove(playerId, newChunkX, newChunkZ);
    }
    
    public void onPlayerQuit(java.util.UUID playerId) {
        chunkOptimizer.onPlayerQuit(playerId);
        entityCulling.clearEntity(playerId);
    }
    
    public java.util.List<ChunkLoadOptimizer.ChunkPos> getChunksToLoad(java.util.UUID playerId) {
        return chunkOptimizer.getChunksToLoad(playerId);
    }
    
    public java.util.List<ChunkLoadOptimizer.ChunkPos> getChunksToUnload(java.util.UUID playerId) {
        return chunkOptimizer.getChunksToUnload(playerId);
    }
    
    public PerformanceOptimizer.PerformanceSummary getPerformanceSummary() {
        return performanceOptimizer.getSummary();
    }
    
    public PerformanceOptimizer getPerformanceOptimizer() { return performanceOptimizer; }
    public EntityCulling getEntityCulling() { return entityCulling; }
    public ChunkLoadOptimizer getChunkOptimizer() { return chunkOptimizer; }
}
