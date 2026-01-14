package rubidium.qol.features;

import rubidium.core.logging.RubidiumLogger;
import rubidium.qol.QoLFeature;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;

public class LagDetectionFeature extends QoLFeature {
    
    public record LagConfig(
        double tpsWarningThreshold,
        double tpsCriticalThreshold,
        long memoryWarningPercentage,
        long memoryCriticalPercentage,
        int tickHistorySize,
        boolean alertOnWarning,
        boolean alertOnCritical,
        String warningMessage,
        String criticalMessage
    ) {
        public static LagConfig defaults() {
            return new LagConfig(
                18.0,
                15.0,
                80,
                95,
                100,
                true,
                true,
                "&eTPS has dropped to {tps} - Consider reducing load",
                "&c&lCRITICAL: TPS is at {tps}! Server performance severely degraded"
            );
        }
    }
    
    public enum LagLevel {
        NORMAL,
        WARNING,
        CRITICAL
    }
    
    public record LagStatus(
        double currentTps,
        double avgTps,
        long usedMemoryMb,
        long maxMemoryMb,
        int memoryPercentage,
        LagLevel tpsLevel,
        LagLevel memoryLevel,
        long lastTickDuration
    ) {}
    
    private final Deque<Long> tickTimes = new ConcurrentLinkedDeque<>();
    private final Deque<Double> tpsHistory = new ConcurrentLinkedDeque<>();
    
    private LagConfig config;
    private Consumer<String> alertHandler;
    private long lastTickTime;
    private boolean lastWasWarning = false;
    private boolean lastWasCritical = false;
    
    public LagDetectionFeature(RubidiumLogger logger) {
        super("lag-detection", "Lag Detection", 
            "Monitors server TPS and memory usage with configurable alerts",
            logger);
        this.config = LagConfig.defaults();
        this.lastTickTime = System.nanoTime();
    }
    
    public void setConfig(LagConfig config) {
        this.config = config;
    }
    
    public LagConfig getConfig() {
        return config;
    }
    
    public void setAlertHandler(Consumer<String> handler) {
        this.alertHandler = handler;
    }
    
    @Override
    protected void onEnable() {
        tickTimes.clear();
        tpsHistory.clear();
        lastTickTime = System.nanoTime();
        lastWasWarning = false;
        lastWasCritical = false;
        logger.debug("Lag detection enabled");
    }
    
    @Override
    protected void onDisable() {
        tickTimes.clear();
        tpsHistory.clear();
    }
    
    @Override
    public void tick() {
        if (!enabled) return;
        
        long currentTime = System.nanoTime();
        long tickDuration = currentTime - lastTickTime;
        lastTickTime = currentTime;
        
        tickTimes.addLast(tickDuration);
        while (tickTimes.size() > config.tickHistorySize()) {
            tickTimes.remove(0);
        }
        
        double currentTps = calculateTps();
        tpsHistory.addLast(currentTps);
        while (tpsHistory.size() > config.tickHistorySize()) {
            tpsHistory.remove(0);
        }
        
        checkAndAlert(currentTps);
    }
    
    private double calculateTps() {
        if (tickTimes.isEmpty()) return 20.0;
        
        double avgTickNanos = tickTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(50_000_000.0);
        
        double tps = 1_000_000_000.0 / avgTickNanos;
        return Math.min(tps, 20.0);
    }
    
    private void checkAndAlert(double currentTps) {
        LagLevel level = getTpsLevel(currentTps);
        
        if (level == LagLevel.CRITICAL && !lastWasCritical && config.alertOnCritical()) {
            String message = config.criticalMessage()
                .replace("{tps}", String.format("%.1f", currentTps));
            if (alertHandler != null) {
                alertHandler.accept(message);
            }
            logger.warn("Critical TPS: {}", String.format("%.1f", currentTps));
            lastWasCritical = true;
            lastWasWarning = true;
        } else if (level == LagLevel.WARNING && !lastWasWarning && config.alertOnWarning()) {
            String message = config.warningMessage()
                .replace("{tps}", String.format("%.1f", currentTps));
            if (alertHandler != null) {
                alertHandler.accept(message);
            }
            logger.info("TPS warning: {}", String.format("%.1f", currentTps));
            lastWasWarning = true;
        } else if (level == LagLevel.NORMAL) {
            if (lastWasCritical || lastWasWarning) {
                logger.info("TPS recovered to {}", String.format("%.1f", currentTps));
            }
            lastWasWarning = false;
            lastWasCritical = false;
        }
    }
    
    public double getCurrentTps() {
        return calculateTps();
    }
    
    public double getAverageTps() {
        if (tpsHistory.isEmpty()) return 20.0;
        return tpsHistory.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(20.0);
    }
    
    public double getMinTps() {
        if (tpsHistory.isEmpty()) return 20.0;
        return tpsHistory.stream()
            .mapToDouble(Double::doubleValue)
            .min()
            .orElse(20.0);
    }
    
    public LagLevel getTpsLevel(double tps) {
        if (tps < config.tpsCriticalThreshold()) {
            return LagLevel.CRITICAL;
        } else if (tps < config.tpsWarningThreshold()) {
            return LagLevel.WARNING;
        }
        return LagLevel.NORMAL;
    }
    
    public LagLevel getMemoryLevel() {
        int percentage = getMemoryPercentage();
        if (percentage >= config.memoryCriticalPercentage()) {
            return LagLevel.CRITICAL;
        } else if (percentage >= config.memoryWarningPercentage()) {
            return LagLevel.WARNING;
        }
        return LagLevel.NORMAL;
    }
    
    public long getUsedMemoryMb() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    }
    
    public long getMaxMemoryMb() {
        return Runtime.getRuntime().maxMemory() / (1024 * 1024);
    }
    
    public int getMemoryPercentage() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long max = runtime.maxMemory();
        return (int) ((used * 100) / max);
    }
    
    public LagStatus getStatus() {
        double currentTps = getCurrentTps();
        return new LagStatus(
            currentTps,
            getAverageTps(),
            getUsedMemoryMb(),
            getMaxMemoryMb(),
            getMemoryPercentage(),
            getTpsLevel(currentTps),
            getMemoryLevel(),
            tickTimes.isEmpty() ? 0 : tickTimes.peekLast() / 1_000_000
        );
    }
    
    public String getStatusReport() {
        LagStatus status = getStatus();
        
        String tpsColor = switch (status.tpsLevel()) {
            case NORMAL -> "&a";
            case WARNING -> "&e";
            case CRITICAL -> "&c";
        };
        
        String memColor = switch (status.memoryLevel()) {
            case NORMAL -> "&a";
            case WARNING -> "&e";
            case CRITICAL -> "&c";
        };
        
        return String.format("""
            &7Server Performance:
            &7TPS: %s%.1f &7(avg: %.1f, min: %.1f)
            &7Memory: %s%d%% &7(%dMB / %dMB)
            &7Last tick: %dms""",
            tpsColor, status.currentTps(), status.avgTps(), getMinTps(),
            memColor, status.memoryPercentage(), status.usedMemoryMb(), status.maxMemoryMb(),
            status.lastTickDuration()
        );
    }
    
    public void forceGarbageCollection() {
        long beforeMb = getUsedMemoryMb();
        System.gc();
        long afterMb = getUsedMemoryMb();
        logger.info("GC freed {}MB of memory", beforeMb - afterMb);
    }
}
