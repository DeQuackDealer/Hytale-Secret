package com.yellowtale.rubidium.performance;

import com.yellowtale.rubidium.core.logging.RubidiumLogger;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class PerformanceManager {
    
    private final RubidiumLogger logger;
    private PerformanceConfig config;
    
    private final ScheduledExecutorService scheduler;
    private final MemoryMXBean memoryBean;
    
    private final AtomicInteger currentTps;
    private final AtomicInteger currentViewRadius;
    private final AtomicLong gcCumulativeMillis;
    private final AtomicLong lastGcSuggestionEpoch;
    private final AtomicLong lastChunkCount;
    private final AtomicInteger playerCount;
    
    private volatile boolean cpuPressure;
    private volatile boolean memoryPressure;
    private volatile ServerState serverState;
    
    private Consumer<Integer> viewRadiusChangeListener;
    private Consumer<Integer> tpsChangeListener;
    private Consumer<PerformanceMetrics> metricsListener;
    
    private long[] tickTimes;
    private int tickIndex;
    
    public PerformanceManager(RubidiumLogger logger) {
        this.logger = logger;
        this.config = PerformanceConfig.defaults();
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        
        this.currentTps = new AtomicInteger(20);
        this.currentViewRadius = new AtomicInteger(config.defaultViewRadius());
        this.gcCumulativeMillis = new AtomicLong(0);
        this.lastGcSuggestionEpoch = new AtomicLong(0);
        this.lastChunkCount = new AtomicLong(0);
        this.playerCount = new AtomicInteger(0);
        
        this.cpuPressure = false;
        this.memoryPressure = false;
        this.serverState = ServerState.NORMAL;
        
        this.tickTimes = new long[100];
        this.tickIndex = 0;
    }
    
    public void start() {
        scheduler.scheduleAtFixedRate(this::monitorPerformance, 1, 1, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::checkMemoryPressure, 5, 5, TimeUnit.SECONDS);
        logger.info("Performance manager started");
    }
    
    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("Performance manager stopped");
    }
    
    public void recordTick(long tickTimeNanos) {
        if (!config.enableTpsLimiting()) {
            return;
        }
        
        tickTimes[tickIndex] = tickTimeNanos;
        tickIndex = (tickIndex + 1) % tickTimes.length;
        
        double avgTickTime = calculateAverageTickTime();
        int measuredTps = (int) Math.min(20, 1_000_000_000.0 / avgTickTime);
        
        int targetTps = calculateTargetTps();
        if (measuredTps < targetTps - 2) {
            cpuPressure = true;
            onCpuPressure();
        } else if (measuredTps >= targetTps) {
            cpuPressure = false;
            onCpuRecovery();
        }
        
        currentTps.set(measuredTps);
    }
    
    public void updatePlayerCount(int count) {
        playerCount.set(count);
        
        if (count == 0 && serverState != ServerState.EMPTY) {
            serverState = ServerState.EMPTY;
            applyEmptyServerOptimizations();
        } else if (count > 0 && serverState == ServerState.EMPTY) {
            serverState = ServerState.NORMAL;
            restoreNormalOperations();
        }
    }
    
    public void updateChunkCount(long count) {
        long previousCount = lastChunkCount.getAndSet(count);
        
        if (previousCount > count && (previousCount - count) > config.chunkUnloadThreshold()) {
            suggestGarbageCollection("Significant chunk unload detected");
        }
    }
    
    public int getTargetTps() {
        return calculateTargetTps();
    }
    
    public int getCurrentTps() {
        return currentTps.get();
    }
    
    public int getCurrentViewRadius() {
        return currentViewRadius.get();
    }
    
    public boolean isCpuPressure() {
        return cpuPressure;
    }
    
    public boolean isMemoryPressure() {
        return memoryPressure;
    }
    
    public ServerState getServerState() {
        return serverState;
    }
    
    public PerformanceMetrics getMetrics() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        
        return new PerformanceMetrics(
            currentTps.get(),
            calculateTargetTps(),
            currentViewRadius.get(),
            heapUsage.getUsed(),
            heapUsage.getMax(),
            cpuPressure,
            memoryPressure,
            serverState,
            playerCount.get(),
            lastChunkCount.get()
        );
    }
    
    public void onViewRadiusChange(Consumer<Integer> listener) {
        this.viewRadiusChangeListener = listener;
    }
    
    public void onTpsChange(Consumer<Integer> listener) {
        this.tpsChangeListener = listener;
    }
    
    public void onMetricsUpdate(Consumer<PerformanceMetrics> listener) {
        this.metricsListener = listener;
    }
    
    private void monitorPerformance() {
        try {
            PerformanceMetrics metrics = getMetrics();
            
            if (metricsListener != null) {
                metricsListener.accept(metrics);
            }
            
            adjustViewRadius();
            
        } catch (Exception e) {
            logger.error("Error monitoring performance: {}", e.getMessage());
        }
    }
    
    private void checkMemoryPressure() {
        if (!config.enableSmartGc() && !config.enableDynamicViewRadius()) {
            return;
        }
        
        try {
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            double usageRatio = (double) heapUsage.getUsed() / heapUsage.getMax();
            
            long totalGcTime = getTotalGcTime();
            long gcTimeDelta = totalGcTime - gcCumulativeMillis.getAndSet(totalGcTime);
            
            boolean highMemoryUsage = usageRatio > config.memoryPressureThreshold();
            boolean excessiveGc = gcTimeDelta > config.gcTimeThreshold();
            
            if (highMemoryUsage || excessiveGc) {
                if (!memoryPressure) {
                    memoryPressure = true;
                    onMemoryPressure();
                }
            } else if (usageRatio < config.memoryRecoveryThreshold()) {
                if (memoryPressure) {
                    memoryPressure = false;
                    onMemoryRecovery();
                }
            }
            
        } catch (Exception e) {
            logger.error("Error checking memory pressure: {}", e.getMessage());
        }
    }
    
    private void onCpuPressure() {
        logger.warn("CPU pressure detected - current TPS: {}", currentTps.get());
        
        if (serverState != ServerState.DEGRADED) {
            serverState = ServerState.DEGRADED;
        }
        
        if (config.enableDynamicViewRadius()) {
            reduceViewRadius();
        }
    }
    
    private void onCpuRecovery() {
        if (serverState == ServerState.DEGRADED && !memoryPressure) {
            serverState = ServerState.NORMAL;
        }
    }
    
    private void onMemoryPressure() {
        logger.warn("Memory pressure detected");
        
        if (serverState != ServerState.DEGRADED) {
            serverState = ServerState.DEGRADED;
        }
        
        if (config.enableDynamicViewRadius()) {
            reduceViewRadius();
        }
        if (config.enableSmartGc()) {
            suggestGarbageCollection("Memory pressure");
        }
    }
    
    private void onMemoryRecovery() {
        if (serverState == ServerState.DEGRADED && !cpuPressure) {
            serverState = ServerState.NORMAL;
        }
    }
    
    private void adjustViewRadius() {
        if (!config.enableDynamicViewRadius()) {
            return;
        }
        
        int current = currentViewRadius.get();
        
        if (cpuPressure || memoryPressure) {
            return;
        }
        
        if (serverState == ServerState.NORMAL && current < config.defaultViewRadius()) {
            int newRadius = Math.min(current + 1, config.defaultViewRadius());
            if (currentViewRadius.compareAndSet(current, newRadius)) {
                logger.info("View radius increased to {}", newRadius);
                notifyViewRadiusChange(newRadius);
            }
        }
    }
    
    private void reduceViewRadius() {
        int current = currentViewRadius.get();
        int minimum = config.minimumViewRadius();
        
        if (current > minimum) {
            int newRadius = Math.max(current - config.viewRadiusStep(), minimum);
            if (currentViewRadius.compareAndSet(current, newRadius)) {
                logger.info("View radius reduced to {} due to resource pressure", newRadius);
                notifyViewRadiusChange(newRadius);
            }
        }
    }
    
    private void applyEmptyServerOptimizations() {
        logger.info("Server empty - applying idle optimizations");
        
        if (config.enableTpsLimiting()) {
            int emptyTps = config.emptyServerTps();
            if (tpsChangeListener != null) {
                tpsChangeListener.accept(emptyTps);
            }
        }
        
        if (config.enableSmartGc()) {
            suggestGarbageCollection("Server empty");
        }
    }
    
    private void restoreNormalOperations() {
        logger.info("Players joined - restoring normal operations");
        
        if (tpsChangeListener != null) {
            tpsChangeListener.accept(config.targetTps());
        }
    }
    
    private void suggestGarbageCollection(String reason) {
        if (!config.enableSmartGc()) {
            return;
        }
        
        long now = System.currentTimeMillis();
        long lastSuggestion = lastGcSuggestionEpoch.get();
        
        if (now - lastSuggestion < config.minGcInterval()) {
            return;
        }
        
        if (!lastGcSuggestionEpoch.compareAndSet(lastSuggestion, now)) {
            return;
        }
        
        logger.debug("Suggesting garbage collection: {}", reason);
        System.gc();
    }
    
    private int calculateTargetTps() {
        if (serverState == ServerState.EMPTY) {
            return config.emptyServerTps();
        }
        return config.targetTps();
    }
    
    private double calculateAverageTickTime() {
        long sum = 0;
        int count = 0;
        for (long time : tickTimes) {
            if (time > 0) {
                sum += time;
                count++;
            }
        }
        return count > 0 ? (double) sum / count : 50_000_000.0;
    }
    
    private long getTotalGcTime() {
        long total = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            long time = gc.getCollectionTime();
            if (time > 0) {
                total += time;
            }
        }
        return total;
    }
    
    private void notifyViewRadiusChange(int newRadius) {
        if (viewRadiusChangeListener != null) {
            viewRadiusChangeListener.accept(newRadius);
        }
    }
    
    public void setConfig(PerformanceConfig config) {
        this.config = config;
    }
    
    public PerformanceConfig getConfig() {
        return config;
    }
    
    public enum ServerState {
        NORMAL,
        DEGRADED,
        EMPTY
    }
}
