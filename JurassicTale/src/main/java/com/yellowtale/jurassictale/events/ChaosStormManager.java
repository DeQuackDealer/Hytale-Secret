package com.yellowtale.jurassictale.events;

import com.yellowtale.rubidium.core.logging.RubidiumLogger;
import com.yellowtale.jurassictale.JurassicTaleConfig;
import com.yellowtale.jurassictale.territory.TerritoryManager;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class ChaosStormManager {
    
    private final RubidiumLogger logger;
    private final JurassicTaleConfig config;
    private final TerritoryManager territoryManager;
    
    private final ScheduledExecutorService scheduler;
    private final List<Consumer<ChaosStormEvent>> eventListeners;
    
    private ChaosStorm activeStorm;
    private long lastStormEndTime;
    private ScheduledFuture<?> stormTask;
    
    public ChaosStormManager(RubidiumLogger logger, JurassicTaleConfig config, 
                             TerritoryManager territoryManager) {
        this.logger = logger;
        this.config = config;
        this.territoryManager = territoryManager;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.eventListeners = new ArrayList<>();
        this.lastStormEndTime = 0;
    }
    
    public void startStorm() {
        if (activeStorm != null) {
            logger.warn("Cannot start storm - one is already active");
            return;
        }
        
        long cooldownMs = config.chaosStorm().cooldownMinutes() * 60 * 1000L;
        if (System.currentTimeMillis() - lastStormEndTime < cooldownMs) {
            logger.warn("Cannot start storm - cooldown not elapsed");
            return;
        }
        
        activeStorm = new ChaosStorm(
            UUID.randomUUID(),
            System.currentTimeMillis(),
            config.chaosStorm().stormDurationMinutes()
        );
        
        fireEvent(new ChaosStormEvent(ChaosStormEvent.Type.STARTED, activeStorm));
        logger.info("CHAOS STORM STARTED - Duration: {} minutes", 
            config.chaosStorm().stormDurationMinutes());
        
        long durationMs = config.chaosStorm().stormDurationMinutes() * 60 * 1000L;
        stormTask = scheduler.schedule(this::endStorm, durationMs, TimeUnit.MILLISECONDS);
    }
    
    public void endStorm() {
        if (activeStorm == null) return;
        
        ChaosStorm endedStorm = activeStorm;
        activeStorm = null;
        lastStormEndTime = System.currentTimeMillis();
        
        fireEvent(new ChaosStormEvent(ChaosStormEvent.Type.ENDED, endedStorm));
        logger.info("CHAOS STORM ENDED");
    }
    
    public void scheduleNextStorm(long delayMinutes) {
        scheduler.schedule(this::startStorm, delayMinutes, TimeUnit.MINUTES);
        logger.info("Next chaos storm scheduled in {} minutes", delayMinutes);
    }
    
    public void issueWarning() {
        if (activeStorm != null) return;
        
        fireEvent(new ChaosStormEvent(ChaosStormEvent.Type.WARNING, null));
        logger.info("CHAOS STORM WARNING - Storm approaching in {} minutes", 
            config.chaosStorm().warningMinutes());
    }
    
    public boolean isStormActive() {
        return activeStorm != null;
    }
    
    public Optional<ChaosStorm> getActiveStorm() {
        return Optional.ofNullable(activeStorm);
    }
    
    public long getTimeUntilStormEnd() {
        if (activeStorm == null) return -1;
        
        long endTime = activeStorm.getStartTime() + 
            (activeStorm.getDurationMinutes() * 60 * 1000L);
        return Math.max(0, endTime - System.currentTimeMillis());
    }
    
    public long getCooldownRemaining() {
        long cooldownMs = config.chaosStorm().cooldownMinutes() * 60 * 1000L;
        long elapsed = System.currentTimeMillis() - lastStormEndTime;
        return Math.max(0, cooldownMs - elapsed);
    }
    
    public void onEvent(Consumer<ChaosStormEvent> listener) {
        eventListeners.add(listener);
    }
    
    private void fireEvent(ChaosStormEvent event) {
        for (Consumer<ChaosStormEvent> listener : eventListeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                logger.error("Error in chaos storm event listener: {}", e.getMessage());
            }
        }
    }
    
    public void shutdown() {
        if (stormTask != null) {
            stormTask.cancel(false);
        }
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    public record ChaosStorm(UUID id, long startTime, int durationMinutes) {
        public long getStartTime() { return startTime; }
        public int getDurationMinutes() { return durationMinutes; }
    }
    
    public record ChaosStormEvent(Type type, ChaosStorm storm) {
        public enum Type {
            WARNING,
            STARTED,
            INTENSIFYING,
            ENDED
        }
    }
}
