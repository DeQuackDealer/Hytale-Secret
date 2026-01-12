package com.yellowtale.rubidium.performance;

import com.yellowtale.rubidium.performance.memory.ByteBufferPool;
import com.yellowtale.rubidium.performance.scheduler.TickScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class PerformanceMonitor {
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitor.class);
    
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final MemoryMXBean memoryMXBean;
    private final OperatingSystemMXBean osMXBean;
    private final List<GarbageCollectorMXBean> gcBeans;
    
    private volatile PerformanceSnapshot latestSnapshot;
    private final ConcurrentLinkedDeque<PerformanceSnapshot> history = new ConcurrentLinkedDeque<>();
    private static final int MAX_HISTORY = 300;
    
    private long lastGcTime = 0;
    private long lastGcCount = 0;
    private final AtomicLong tickCount = new AtomicLong();
    private volatile double currentTps = 20.0;
    private long lastTickCheck = System.nanoTime();
    private int ticksThisSecond = 0;
    
    public PerformanceMonitor() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Rubidium-PerformanceMonitor");
            t.setDaemon(true);
            return t;
        });
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.osMXBean = ManagementFactory.getOperatingSystemMXBean();
        this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    }
    
    public void start() {
        if (running.compareAndSet(false, true)) {
            scheduler.scheduleAtFixedRate(this::collectMetrics, 0, 1, TimeUnit.SECONDS);
            logger.info("Performance monitor started (with RPAL integration)");
        }
    }
    
    public void stop() {
        if (running.compareAndSet(true, false)) {
            scheduler.shutdown();
            logger.info("Performance monitor stopped");
        }
    }
    
    public void recordTick() {
        tickCount.incrementAndGet();
        ticksThisSecond++;
        
        long now = System.nanoTime();
        long elapsed = now - lastTickCheck;
        
        if (elapsed >= 1_000_000_000L) {
            currentTps = ticksThisSecond * (1_000_000_000.0 / elapsed);
            ticksThisSecond = 0;
            lastTickCheck = now;
        }
    }
    
    private void collectMetrics() {
        try {
            long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
            long nonHeapUsed = memoryMXBean.getNonHeapMemoryUsage().getUsed();
            double cpuLoad = osMXBean.getSystemLoadAverage();
            
            long gcTime = 0;
            long gcCount = 0;
            for (GarbageCollectorMXBean gc : gcBeans) {
                gcTime += gc.getCollectionTime();
                gcCount += gc.getCollectionCount();
            }
            
            long gcTimeDelta = gcTime - lastGcTime;
            long gcCountDelta = gcCount - lastGcCount;
            lastGcTime = gcTime;
            lastGcCount = gcCount;
            
            double bufferPoolHitRate = 0.0;
            long bufferPoolAllocations = 0;
            try {
                ByteBufferPool.PoolStats bufferStats = ByteBufferPool.getInstance().getStats();
                bufferPoolHitRate = bufferStats.hitRate();
                bufferPoolAllocations = bufferStats.totalAllocations();
            } catch (Exception e) {
            }
            
            double schedulerTps = currentTps;
            long schedulerTick = 0;
            double tickUtilization = 0.0;
            try {
                if (RPAL.getInstance().isInitialized()) {
                    RPAL rpal = RPAL.getInstance();
                    if (rpal.getConfig().enableTickScheduler()) {
                        TickScheduler ts = rpal.getTickScheduler();
                        if (ts != null && ts.isRunning()) {
                            schedulerTps = ts.getCurrentTps();
                            schedulerTick = ts.getCurrentTick();
                            tickUtilization = ts.getStats().tickUtilization();
                        }
                    }
                }
            } catch (Exception e) {
            }
            
            PerformanceSnapshot snapshot = new PerformanceSnapshot(
                System.currentTimeMillis(),
                heapUsed / (1024 * 1024),
                heapMax / (1024 * 1024),
                nonHeapUsed / (1024 * 1024),
                cpuLoad,
                Thread.activeCount(),
                (float) schedulerTps,
                0,
                0,
                gcTime,
                gcCount,
                gcTimeDelta,
                gcCountDelta,
                bufferPoolHitRate,
                bufferPoolAllocations,
                tickCount.get(),
                tickUtilization
            );
            
            latestSnapshot = snapshot;
            history.addLast(snapshot);
            
            while (history.size() > MAX_HISTORY) {
                history.removeFirst();
            }
            
        } catch (Exception e) {
            logger.warn("Failed to collect metrics", e);
        }
    }
    
    public PerformanceSnapshot getLatest() {
        return latestSnapshot;
    }
    
    public PerformanceSnapshot[] getHistory() {
        return history.toArray(new PerformanceSnapshot[0]);
    }
    
    public PerformanceSnapshot[] getRecentHistory(int seconds) {
        long cutoff = System.currentTimeMillis() - (seconds * 1000L);
        return history.stream()
            .filter(s -> s.timestamp() >= cutoff)
            .toArray(PerformanceSnapshot[]::new);
    }
    
    public double getAverageTps(int seconds) {
        PerformanceSnapshot[] recent = getRecentHistory(seconds);
        if (recent.length == 0) return 20.0;
        
        double sum = 0;
        for (PerformanceSnapshot s : recent) {
            sum += s.tps();
        }
        return sum / recent.length;
    }
    
    public boolean isHealthy() {
        PerformanceSnapshot snapshot = latestSnapshot;
        if (snapshot == null) return true;
        
        return snapshot.tps() >= 18.0 && 
               snapshot.memoryUsedMb() < snapshot.memoryMaxMb() * 0.9;
    }
    
    public record PerformanceSnapshot(
        long timestamp,
        long memoryUsedMb,
        long memoryMaxMb,
        long nonHeapUsedMb,
        double cpuLoad,
        int threadCount,
        float tps,
        int playerCount,
        int entityCount,
        long gcTimeMs,
        long gcCount,
        long gcTimeDeltaMs,
        long gcCountDelta,
        double bufferPoolHitRate,
        long bufferPoolAllocations,
        long totalTicks,
        double tickUtilization
    ) {
        public double memoryUtilization() {
            return memoryMaxMb > 0 ? (double) memoryUsedMb / memoryMaxMb : 0;
        }
        
        public boolean isLagging() {
            return tps < 18.0f;
        }
        
        public boolean isMemoryPressureHigh() {
            return memoryUtilization() > 0.85;
        }
    }
}
