package com.yellowtale.rubidium.performance;

import com.yellowtale.rubidium.performance.jit.JitOptimizer;
import com.yellowtale.rubidium.performance.memory.ArenaAllocator;
import com.yellowtale.rubidium.performance.memory.ByteBufferPool;
import com.yellowtale.rubidium.performance.memory.ObjectPool;
import com.yellowtale.rubidium.performance.scheduler.TickScheduler;
import com.yellowtale.rubidium.performance.simd.VectorMath;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public final class RPAL {
    private static final Logger LOGGER = Logger.getLogger(RPAL.class.getName());
    private static final RPAL INSTANCE = new RPAL();
    
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final ConcurrentHashMap<String, ArenaAllocator> arenas = new ConcurrentHashMap<>();
    
    private volatile TickScheduler tickScheduler;
    private volatile RPALConfig config = new RPALConfig();
    
    private long lastGcTime = 0;
    private long lastGcCount = 0;
    
    private RPAL() {}
    
    public static RPAL getInstance() {
        return INSTANCE;
    }
    
    public void initialize() {
        initialize(new RPALConfig());
    }
    
    public void initialize(RPALConfig config) {
        if (initialized.compareAndSet(false, true)) {
            this.config = config;
            
            LOGGER.info("[RPAL] Rubidium Performance Acceleration Layer v1.0.0");
            LOGGER.info("[RPAL] Lithium-style optimizations for Hytale servers");
            LOGGER.info("[RPAL] Initializing with config: " + config);
            
            if (config.enableSimd) {
                VectorMath.initialize();
                LOGGER.info("[RPAL] SIMD math: " + (VectorMath.isSimdAvailable() ? "Vector API enabled" : "Scalar fallback"));
            }
            
            if (config.enableTickScheduler) {
                tickScheduler = new TickScheduler(config.asyncThreads);
            }
            
            if (config.preallocateBuffers) {
                ByteBufferPool.getInstance();
                LOGGER.info("[RPAL] ByteBuffer pool pre-initialized (zero-allocation networking)");
            }
            
            JitOptimizer.getInstance();
            
            LOGGER.info("[RPAL] Initialization complete");
            logCapabilities();
        }
    }
    
    public void shutdown() {
        if (initialized.compareAndSet(true, false)) {
            LOGGER.info("[RPAL] Shutting down...");
            
            if (tickScheduler != null) {
                tickScheduler.stop();
            }
            
            arenas.values().forEach(ArenaAllocator::close);
            arenas.clear();
            
            ByteBufferPool.getInstance().clear();
            ObjectPool.clearAllGlobalPools();
            
            LOGGER.info("[RPAL] Shutdown complete");
        }
    }
    
    private void logCapabilities() {
        Capabilities caps = getCapabilities();
        
        LOGGER.info("[RPAL] Capabilities (Lithium-style):");
        LOGGER.info("[RPAL]   SIMD Math: " + (caps.simdAvailable ? "Vector API" : "Scalar"));
        LOGGER.info("[RPAL]   JIT Tiered: " + caps.jitTieredCompilation);
        LOGGER.info("[RPAL]   Direct Buffers: " + caps.directBuffersEnabled);
        LOGGER.info("[RPAL]   Object Pools: " + caps.objectPoolingEnabled);
        LOGGER.info("[RPAL]   Tick Scheduler: " + caps.tickSchedulerEnabled);
        LOGGER.info("[RPAL]   Heap Memory: " + formatBytes(caps.maxHeapMemory));
        LOGGER.info("[RPAL]   CPU Cores: " + caps.availableCpuCores);
        LOGGER.info("[RPAL]   GC: " + caps.gcType);
    }
    
    public Capabilities getCapabilities() {
        Runtime runtime = Runtime.getRuntime();
        
        return new Capabilities(
            VectorMath.isSimdAvailable(),
            JitOptimizer.getInstance().getStats().tieredCompilation(),
            config.enableDirectBuffers,
            config.enableObjectPooling,
            tickScheduler != null,
            runtime.maxMemory(),
            runtime.availableProcessors(),
            getGcType()
        );
    }
    
    private String getGcType() {
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gc : gcBeans) {
            String name = gc.getName().toLowerCase();
            if (name.contains("zgc")) return "ZGC";
            if (name.contains("g1")) return "G1GC";
            if (name.contains("parallel")) return "ParallelGC";
            if (name.contains("shenandoah")) return "Shenandoah";
        }
        return "Unknown";
    }
    
    public ByteBufferPool getBufferPool() {
        return ByteBufferPool.getInstance();
    }
    
    public JitOptimizer getJitOptimizer() {
        return JitOptimizer.getInstance();
    }
    
    public TickScheduler getTickScheduler() {
        if (tickScheduler == null) {
            throw new IllegalStateException("Tick scheduler not enabled in config");
        }
        return tickScheduler;
    }
    
    public ArenaAllocator createArena(String name) {
        return arenas.computeIfAbsent(name, ArenaAllocator::new);
    }
    
    public ArenaAllocator createArena(String name, int blockSize, boolean direct) {
        return arenas.computeIfAbsent(name, n -> new ArenaAllocator(n, blockSize, direct));
    }
    
    public ArenaAllocator getArena(String name) {
        return arenas.get(name);
    }
    
    public void destroyArena(String name) {
        ArenaAllocator arena = arenas.remove(name);
        if (arena != null) {
            arena.close();
        }
    }
    
    public PerformanceSnapshot getPerformanceSnapshot() {
        Runtime runtime = Runtime.getRuntime();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        
        long gcTime = 0;
        long gcCount = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcTime += gc.getCollectionTime();
            gcCount += gc.getCollectionCount();
        }
        
        long gcTimeDelta = gcTime - lastGcTime;
        long gcCountDelta = gcCount - lastGcCount;
        lastGcTime = gcTime;
        lastGcCount = gcCount;
        
        Map<String, ArenaAllocator.ArenaStats> arenaStats = new HashMap<>();
        for (Map.Entry<String, ArenaAllocator> entry : arenas.entrySet()) {
            arenaStats.put(entry.getKey(), entry.getValue().getStats());
        }
        
        return new PerformanceSnapshot(
            runtime.totalMemory() - runtime.freeMemory(),
            runtime.totalMemory(),
            runtime.maxMemory(),
            memoryBean.getHeapMemoryUsage().getUsed(),
            memoryBean.getNonHeapMemoryUsage().getUsed(),
            gcTime,
            gcCount,
            gcTimeDelta,
            gcCountDelta,
            ByteBufferPool.getInstance().getStats(),
            JitOptimizer.getInstance().getStats(),
            tickScheduler != null ? tickScheduler.getStats() : null,
            arenaStats,
            Thread.activeCount(),
            System.currentTimeMillis()
        );
    }
    
    public boolean isInitialized() {
        return initialized.get();
    }
    
    public RPALConfig getConfig() {
        return config;
    }
    
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    public record RPALConfig(
        boolean enableSimd,
        boolean enableDirectBuffers,
        boolean enableObjectPooling,
        boolean enableTickScheduler,
        boolean preallocateBuffers,
        int asyncThreads
    ) {
        public RPALConfig() {
            this(true, true, true, true, true, Runtime.getRuntime().availableProcessors());
        }
    }
    
    public record Capabilities(
        boolean simdAvailable,
        boolean jitTieredCompilation,
        boolean directBuffersEnabled,
        boolean objectPoolingEnabled,
        boolean tickSchedulerEnabled,
        long maxHeapMemory,
        int availableCpuCores,
        String gcType
    ) {}
    
    public record PerformanceSnapshot(
        long usedMemory,
        long totalMemory,
        long maxMemory,
        long heapUsed,
        long nonHeapUsed,
        long totalGcTimeMs,
        long totalGcCount,
        long gcTimeDeltaMs,
        long gcCountDelta,
        ByteBufferPool.PoolStats bufferPoolStats,
        JitOptimizer.JitStats jitStats,
        TickScheduler.SchedulerStats schedulerStats,
        Map<String, ArenaAllocator.ArenaStats> arenaStats,
        int activeThreads,
        long timestamp
    ) {
        public double memoryUtilization() {
            return maxMemory > 0 ? (double) usedMemory / maxMemory : 0.0;
        }
        
        public double bufferPoolHitRate() {
            return bufferPoolStats != null ? bufferPoolStats.hitRate() : 0.0;
        }
        
        public boolean isMemoryPressureHigh() {
            return memoryUtilization() > 0.85;
        }
    }
}
