package com.yellowtale.rubidium.performance;

public record PerformanceMetrics(
    int currentTps,
    int targetTps,
    int viewRadius,
    long heapUsed,
    long heapMax,
    boolean cpuPressure,
    boolean memoryPressure,
    PerformanceManager.ServerState serverState,
    int playerCount,
    long chunkCount
) {
    
    public double getHeapUsagePercent() {
        return heapMax > 0 ? (double) heapUsed / heapMax * 100 : 0;
    }
    
    public double getTpsPercent() {
        return targetTps > 0 ? (double) currentTps / targetTps * 100 : 0;
    }
    
    public boolean isHealthy() {
        return !cpuPressure && !memoryPressure && currentTps >= targetTps - 2;
    }
    
    public String getStatusSummary() {
        if (serverState == PerformanceManager.ServerState.EMPTY) {
            return "IDLE";
        }
        if (cpuPressure && memoryPressure) {
            return "CRITICAL";
        }
        if (cpuPressure) {
            return "CPU_PRESSURE";
        }
        if (memoryPressure) {
            return "MEMORY_PRESSURE";
        }
        return "HEALTHY";
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
