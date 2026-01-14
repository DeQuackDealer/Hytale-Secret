package rubidium.performance;

/**
 * Performance metric tracking.
 */
public class PerformanceMetric {
    
    private final String name;
    private long startTime;
    private long totalTime;
    private long count;
    private long minTime = Long.MAX_VALUE;
    private long maxTime = 0;
    
    public PerformanceMetric(String name) {
        this.name = name;
    }
    
    public void startTiming() {
        startTime = System.nanoTime();
    }
    
    public void stopTiming() {
        long duration = System.nanoTime() - startTime;
        totalTime += duration;
        count++;
        minTime = Math.min(minTime, duration);
        maxTime = Math.max(maxTime, duration);
    }
    
    public String getName() { return name; }
    
    public long getTotalTimeNanos() { return totalTime; }
    public double getTotalTimeMs() { return totalTime / 1_000_000.0; }
    
    public long getCount() { return count; }
    
    public double getAverageTimeNanos() {
        return count > 0 ? (double) totalTime / count : 0;
    }
    
    public double getAverageTimeMs() {
        return getAverageTimeNanos() / 1_000_000.0;
    }
    
    public long getMinTimeNanos() { return minTime == Long.MAX_VALUE ? 0 : minTime; }
    public double getMinTimeMs() { return getMinTimeNanos() / 1_000_000.0; }
    
    public long getMaxTimeNanos() { return maxTime; }
    public double getMaxTimeMs() { return maxTime / 1_000_000.0; }
    
    public void reset() {
        totalTime = 0;
        count = 0;
        minTime = Long.MAX_VALUE;
        maxTime = 0;
    }
}
