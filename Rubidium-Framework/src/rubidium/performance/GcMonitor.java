package rubidium.performance;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class GcMonitor {
    private final MemoryMXBean memoryBean;
    private final List<GarbageCollectorMXBean> gcBeans;
    private final AtomicLong lastGcCount = new AtomicLong(0);
    private final AtomicLong lastGcTime = new AtomicLong(0);
    
    private final double heapThreshold;
    private final long gcTimeThresholdMs;
    
    public GcMonitor() {
        this(0.85, 100);
    }
    
    public GcMonitor(double heapThreshold, long gcTimeThresholdMs) {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        this.heapThreshold = heapThreshold;
        this.gcTimeThresholdMs = gcTimeThresholdMs;
    }
    
    public MemoryStats getMemoryStats() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        return new MemoryStats(
            heapUsage.getUsed(),
            heapUsage.getMax(),
            heapUsage.getCommitted(),
            nonHeapUsage.getUsed()
        );
    }
    
    public GcStats getGcStats() {
        long totalCount = 0;
        long totalTime = 0;
        
        for (GarbageCollectorMXBean gc : gcBeans) {
            long count = gc.getCollectionCount();
            long time = gc.getCollectionTime();
            if (count >= 0) totalCount += count;
            if (time >= 0) totalTime += time;
        }
        
        long deltaCount = totalCount - lastGcCount.getAndSet(totalCount);
        long deltaTime = totalTime - lastGcTime.getAndSet(totalTime);
        
        return new GcStats(totalCount, totalTime, deltaCount, deltaTime);
    }
    
    public boolean isMemoryPressureHigh() {
        MemoryStats stats = getMemoryStats();
        return stats.getHeapUsageRatio() > heapThreshold;
    }
    
    public boolean isGcOverloaded() {
        GcStats stats = getGcStats();
        return stats.deltaTimeMs > gcTimeThresholdMs;
    }
    
    public void suggestGc() {
        if (isMemoryPressureHigh()) {
            System.gc();
        }
    }
    
    public record MemoryStats(long heapUsed, long heapMax, long heapCommitted, long nonHeapUsed) {
        public double getHeapUsageRatio() {
            return heapMax > 0 ? (double) heapUsed / heapMax : 0;
        }
        
        public long getHeapAvailable() {
            return heapMax - heapUsed;
        }
        
        public String formatHeapUsed() {
            return formatBytes(heapUsed);
        }
        
        public String formatHeapMax() {
            return formatBytes(heapMax);
        }
        
        private String formatBytes(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
    
    public record GcStats(long totalCount, long totalTimeMs, long deltaCount, long deltaTimeMs) {}
}
