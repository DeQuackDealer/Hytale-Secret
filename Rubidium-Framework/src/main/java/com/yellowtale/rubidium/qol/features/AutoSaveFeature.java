package com.yellowtale.rubidium.qol.features;

import com.yellowtale.rubidium.core.logging.RubidiumLogger;
import com.yellowtale.rubidium.qol.QoLFeature;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class AutoSaveFeature extends QoLFeature {
    
    public record AutoSaveConfig(
        Duration saveInterval,
        boolean announceBeforeSave,
        Duration announceDelay,
        String announceMessage,
        String saveCompleteMessage,
        boolean disableDuringSave,
        int maxConcurrentSaves
    ) {
        public static AutoSaveConfig defaults() {
            return new AutoSaveConfig(
                Duration.ofMinutes(5),
                true,
                Duration.ofSeconds(10),
                "&6Server is saving in {seconds} seconds...",
                "&aWorld saved successfully!",
                false,
                1
            );
        }
    }
    
    public interface SaveHandler {
        void performSave();
    }
    
    private AutoSaveConfig config;
    private Instant lastSave;
    private Instant nextSave;
    private Instant announceTime;
    private final AtomicBoolean saving = new AtomicBoolean(false);
    private boolean announced = false;
    
    private SaveHandler saveHandler;
    private Consumer<String> broadcastHandler;
    
    public AutoSaveFeature(RubidiumLogger logger) {
        super("auto-save", "Auto Save", 
            "Automatically saves the world at configurable intervals with announcements",
            logger);
        this.config = AutoSaveConfig.defaults();
    }
    
    public void setConfig(AutoSaveConfig config) {
        this.config = config;
        scheduleNextSave();
    }
    
    public AutoSaveConfig getConfig() {
        return config;
    }
    
    public void setSaveHandler(SaveHandler handler) {
        this.saveHandler = handler;
    }
    
    public void setBroadcastHandler(Consumer<String> handler) {
        this.broadcastHandler = handler;
    }
    
    @Override
    protected void onEnable() {
        lastSave = Instant.now();
        scheduleNextSave();
        logger.debug("Auto-save enabled with {}s interval", config.saveInterval().toSeconds());
    }
    
    @Override
    protected void onDisable() {
        nextSave = null;
        announceTime = null;
    }
    
    private void scheduleNextSave() {
        Instant now = Instant.now();
        nextSave = now.plus(config.saveInterval());
        
        if (config.announceBeforeSave()) {
            announceTime = nextSave.minus(config.announceDelay());
        }
        announced = false;
    }
    
    @Override
    public void tick() {
        if (!enabled || nextSave == null) return;
        
        Instant now = Instant.now();
        
        if (config.announceBeforeSave() && !announced && announceTime != null && now.isAfter(announceTime)) {
            long seconds = Duration.between(now, nextSave).toSeconds();
            String message = config.announceMessage()
                .replace("{seconds}", String.valueOf(Math.max(1, seconds)));
            
            if (broadcastHandler != null) {
                broadcastHandler.accept(message);
            }
            announced = true;
        }
        
        if (now.isAfter(nextSave)) {
            performSave();
        }
    }
    
    public void performSave() {
        if (saving.compareAndSet(false, true)) {
            try {
                logger.info("Starting auto-save...");
                Instant saveStart = Instant.now();
                
                if (saveHandler != null) {
                    saveHandler.performSave();
                }
                
                lastSave = Instant.now();
                long durationMs = Duration.between(saveStart, lastSave).toMillis();
                logger.info("Auto-save completed in {}ms", durationMs);
                
                if (broadcastHandler != null && config.saveCompleteMessage() != null) {
                    broadcastHandler.accept(config.saveCompleteMessage());
                }
                
                scheduleNextSave();
            } catch (Exception e) {
                logger.error("Auto-save failed: {}", e.getMessage());
            } finally {
                saving.set(false);
            }
        } else {
            logger.warn("Save already in progress, skipping");
        }
    }
    
    public void forceSave() {
        if (!enabled) return;
        performSave();
    }
    
    public boolean isSaving() {
        return saving.get();
    }
    
    public Optional<Instant> getLastSaveTime() {
        return Optional.ofNullable(lastSave);
    }
    
    public Optional<Instant> getNextSaveTime() {
        return Optional.ofNullable(nextSave);
    }
    
    public Optional<Duration> getTimeUntilNextSave() {
        if (nextSave == null) return Optional.empty();
        Duration until = Duration.between(Instant.now(), nextSave);
        return until.isNegative() ? Optional.of(Duration.ZERO) : Optional.of(until);
    }
    
    public Optional<Duration> getTimeSinceLastSave() {
        if (lastSave == null) return Optional.empty();
        return Optional.of(Duration.between(lastSave, Instant.now()));
    }
    
    public String getStatus() {
        StringBuilder sb = new StringBuilder("&7Auto-Save Status:\n");
        
        sb.append("  &7Enabled: ").append(enabled ? "&aYes" : "&cNo").append("\n");
        sb.append("  &7Saving: ").append(saving.get() ? "&eYes" : "&aNo").append("\n");
        
        getTimeSinceLastSave().ifPresent(d -> 
            sb.append("  &7Last save: &f").append(formatDuration(d)).append(" ago\n"));
        
        getTimeUntilNextSave().ifPresent(d -> 
            sb.append("  &7Next save: &f").append(formatDuration(d)).append("\n"));
        
        return sb.toString();
    }
    
    private String formatDuration(Duration d) {
        long minutes = d.toMinutes();
        long seconds = d.toSecondsPart();
        
        if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        }
        return String.format("%ds", seconds);
    }
}
