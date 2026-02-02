package rubidium.performance;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

public class TpsMonitor {
    private static final int SAMPLE_SIZE = 100;
    private final long[] tickTimes = new long[SAMPLE_SIZE];
    private final AtomicInteger tickIndex = new AtomicInteger(0);
    private final AtomicLong lastTickTime = new AtomicLong(System.nanoTime());
    private volatile double currentTps = 20.0;
    private volatile double averageTickTime = 50.0;
    
    private final double targetTps;
    private final double criticalTps;
    
    public TpsMonitor() {
        this(20.0, 15.0);
    }
    
    public TpsMonitor(double targetTps, double criticalTps) {
        this.targetTps = targetTps;
        this.criticalTps = criticalTps;
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            tickTimes[i] = 50_000_000L;
        }
    }
    
    public void recordTick() {
        long now = System.nanoTime();
        long elapsed = now - lastTickTime.getAndSet(now);
        int index = tickIndex.getAndUpdate(i -> (i + 1) % SAMPLE_SIZE);
        tickTimes[index] = elapsed;
        updateMetrics();
    }
    
    private void updateMetrics() {
        long totalTime = 0;
        for (long time : tickTimes) {
            totalTime += time;
        }
        averageTickTime = totalTime / (double) SAMPLE_SIZE / 1_000_000.0;
        currentTps = Math.min(targetTps, 1000.0 / averageTickTime);
    }
    
    public double getCurrentTps() {
        return currentTps;
    }
    
    public double getAverageTickTime() {
        return averageTickTime;
    }
    
    public boolean isCritical() {
        return currentTps < criticalTps;
    }
    
    public boolean isHealthy() {
        return currentTps >= targetTps * 0.95;
    }
    
    public PerformanceLevel getPerformanceLevel() {
        if (currentTps >= targetTps * 0.95) return PerformanceLevel.EXCELLENT;
        if (currentTps >= targetTps * 0.8) return PerformanceLevel.GOOD;
        if (currentTps >= criticalTps) return PerformanceLevel.DEGRADED;
        return PerformanceLevel.CRITICAL;
    }
    
    public enum PerformanceLevel {
        EXCELLENT, GOOD, DEGRADED, CRITICAL
    }
}
