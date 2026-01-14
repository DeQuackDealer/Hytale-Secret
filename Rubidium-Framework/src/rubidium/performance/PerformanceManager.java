package rubidium.performance;

import java.util.*;
import java.util.concurrent.*;

/**
 * Performance optimization system with tick budgeting, entity culling, and caching.
 */
public class PerformanceManager {
    
    private static PerformanceManager instance;
    
    private final Map<String, PerformanceMetric> metrics;
    private final Map<String, ObjectPool<?>> objectPools;
    private final ScheduledExecutorService scheduler;
    
    private long tickBudgetNanos = 50_000_000;
    private long lastTickTime = 0;
    private double currentTps = 20.0;
    private final long[] tickHistory = new long[100];
    private int tickHistoryIndex = 0;
    
    private boolean memoryOptimizationEnabled = true;
    private boolean entityCullingEnabled = true;
    private boolean asyncChunkLoadingEnabled = true;
    
    private PerformanceManager() {
        this.metrics = new ConcurrentHashMap<>();
        this.objectPools = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "Rubidium-Perf");
            t.setDaemon(true);
            return t;
        });
        
        scheduler.scheduleAtFixedRate(this::updateTps, 0, 50, TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(this::collectGarbageIfNeeded, 30, 30, TimeUnit.SECONDS);
    }
    
    public static PerformanceManager getInstance() {
        if (instance == null) {
            instance = new PerformanceManager();
        }
        return instance;
    }
    
    public void recordTickStart() {
        lastTickTime = System.nanoTime();
    }
    
    public void recordTickEnd() {
        long duration = System.nanoTime() - lastTickTime;
        tickHistory[tickHistoryIndex] = duration;
        tickHistoryIndex = (tickHistoryIndex + 1) % tickHistory.length;
    }
    
    private void updateTps() {
        long totalNanos = 0;
        int validTicks = 0;
        
        for (long tickNanos : tickHistory) {
            if (tickNanos > 0) {
                totalNanos += tickNanos;
                validTicks++;
            }
        }
        
        if (validTicks > 0) {
            double avgTickMs = (totalNanos / validTicks) / 1_000_000.0;
            currentTps = Math.min(20.0, 1000.0 / avgTickMs);
        }
    }
    
    public double getTps() {
        return currentTps;
    }
    
    public double getAverageTickMs() {
        long total = 0;
        int count = 0;
        
        for (long tickNanos : tickHistory) {
            if (tickNanos > 0) {
                total += tickNanos;
                count++;
            }
        }
        
        return count > 0 ? (total / count) / 1_000_000.0 : 0;
    }
    
    public boolean isTickBudgetExceeded() {
        return System.nanoTime() - lastTickTime > tickBudgetNanos;
    }
    
    public long getRemainingTickBudget() {
        long elapsed = System.nanoTime() - lastTickTime;
        return Math.max(0, tickBudgetNanos - elapsed);
    }
    
    public void setTickBudgetMs(long ms) {
        this.tickBudgetNanos = ms * 1_000_000;
    }
    
    public void startTiming(String name) {
        metrics.computeIfAbsent(name, k -> new PerformanceMetric(name)).startTiming();
    }
    
    public void stopTiming(String name) {
        PerformanceMetric metric = metrics.get(name);
        if (metric != null) {
            metric.stopTiming();
        }
    }
    
    public Optional<PerformanceMetric> getMetric(String name) {
        return Optional.ofNullable(metrics.get(name));
    }
    
    public Collection<PerformanceMetric> getAllMetrics() {
        return metrics.values();
    }
    
    public <T> void registerObjectPool(String name, ObjectPool<T> pool) {
        objectPools.put(name, pool);
    }
    
    @SuppressWarnings("unchecked")
    public <T> Optional<ObjectPool<T>> getObjectPool(String name) {
        return Optional.ofNullable((ObjectPool<T>) objectPools.get(name));
    }
    
    private void collectGarbageIfNeeded() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        double usagePercent = (double) usedMemory / maxMemory;
        
        if (memoryOptimizationEnabled && usagePercent > 0.85) {
            System.gc();
        }
    }
    
    public MemoryInfo getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        long usedMemory = totalMemory - freeMemory;
        
        return new MemoryInfo(usedMemory, freeMemory, totalMemory, maxMemory);
    }
    
    public boolean isMemoryOptimizationEnabled() { return memoryOptimizationEnabled; }
    public void setMemoryOptimizationEnabled(boolean enabled) { this.memoryOptimizationEnabled = enabled; }
    
    public boolean isEntityCullingEnabled() { return entityCullingEnabled; }
    public void setEntityCullingEnabled(boolean enabled) { this.entityCullingEnabled = enabled; }
    
    public boolean isAsyncChunkLoadingEnabled() { return asyncChunkLoadingEnabled; }
    public void setAsyncChunkLoadingEnabled(boolean enabled) { this.asyncChunkLoadingEnabled = enabled; }
    
    public void shutdown() {
        scheduler.shutdown();
    }
    
    public record MemoryInfo(long used, long free, long total, long max) {
        public double getUsagePercent() {
            return (double) used / max;
        }
        
        public String getFormattedUsed() {
            return formatBytes(used);
        }
        
        public String getFormattedMax() {
            return formatBytes(max);
        }
        
        private static String formatBytes(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}
