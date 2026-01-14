package rubidium.qol;

import rubidium.core.logging.RubidiumLogger;
import rubidium.core.config.ConfigManager;
import rubidium.core.scheduler.Scheduler;
import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QoLManager {
    
    private final RubidiumLogger logger;
    private final ConfigManager configManager;
    private final Map<String, QoLFeature> features = new ConcurrentHashMap<>();
    private final Path configPath;
    private Properties featureStates;
    private long tickTaskId = -1;
    private Scheduler scheduler;
    
    public QoLManager(RubidiumLogger logger, ConfigManager configManager, Path dataDir) {
        this.logger = logger;
        this.configManager = configManager;
        this.configPath = dataDir.resolve("qol-features.properties");
        this.featureStates = new Properties();
        loadFeatureStates();
    }
    
    public void startTickLoop(Scheduler scheduler) {
        this.scheduler = scheduler;
        this.tickTaskId = scheduler.scheduleRepeating(
            "qol-tick",
            this::tick,
            Duration.ofMillis(50),
            Duration.ofMillis(50),
            Scheduler.Priority.NORMAL
        );
        logger.info("QoL tick loop started");
    }
    
    public void stopTickLoop() {
        if (tickTaskId >= 0 && scheduler != null) {
            scheduler.cancel(tickTaskId);
            tickTaskId = -1;
            logger.info("QoL tick loop stopped");
        }
    }
    
    public void registerFeature(QoLFeature feature) {
        features.put(feature.getId(), feature);
        
        String savedState = featureStates.getProperty(feature.getId());
        if ("true".equals(savedState)) {
            feature.enable();
        }
        
        logger.debug("Registered QoL feature: {} ({})", feature.getName(), feature.getId());
    }
    
    public void unregisterFeature(String featureId) {
        QoLFeature feature = features.remove(featureId);
        if (feature != null && feature.isEnabled()) {
            feature.disable();
        }
    }
    
    public Optional<QoLFeature> getFeature(String featureId) {
        return Optional.ofNullable(features.get(featureId));
    }
    
    public Collection<QoLFeature> getAllFeatures() {
        return Collections.unmodifiableCollection(features.values());
    }
    
    public List<QoLFeature> getEnabledFeatures() {
        return features.values().stream()
            .filter(QoLFeature::isEnabled)
            .toList();
    }
    
    public void enableFeature(String featureId) {
        QoLFeature feature = features.get(featureId);
        if (feature != null) {
            feature.enable();
            featureStates.setProperty(featureId, "true");
            saveFeatureStates();
        }
    }
    
    public void disableFeature(String featureId) {
        QoLFeature feature = features.get(featureId);
        if (feature != null) {
            feature.disable();
            featureStates.setProperty(featureId, "false");
            saveFeatureStates();
        }
    }
    
    public boolean toggleFeature(String featureId) {
        QoLFeature feature = features.get(featureId);
        if (feature != null) {
            boolean newState = feature.toggle();
            featureStates.setProperty(featureId, String.valueOf(newState));
            saveFeatureStates();
            return newState;
        }
        return false;
    }
    
    public void enableAll() {
        features.values().forEach(QoLFeature::enable);
        features.keySet().forEach(id -> featureStates.setProperty(id, "true"));
        saveFeatureStates();
    }
    
    public void disableAll() {
        features.values().forEach(QoLFeature::disable);
        features.keySet().forEach(id -> featureStates.setProperty(id, "false"));
        saveFeatureStates();
    }
    
    public void tick() {
        for (QoLFeature feature : features.values()) {
            if (feature.isEnabled()) {
                try {
                    feature.tick();
                } catch (Exception e) {
                    logger.error("Error ticking QoL feature '{}': {}", feature.getName(), e.getMessage());
                }
            }
        }
    }
    
    public void reloadAll() {
        features.values().forEach(QoLFeature::reload);
        logger.info("Reloaded all QoL features");
    }
    
    public void shutdown() {
        stopTickLoop();
        features.values().stream()
            .filter(QoLFeature::isEnabled)
            .forEach(QoLFeature::disable);
        saveFeatureStates();
    }
    
    private void loadFeatureStates() {
        if (Files.exists(configPath)) {
            try (InputStream is = Files.newInputStream(configPath)) {
                featureStates.load(is);
                logger.debug("Loaded QoL feature states from {}", configPath);
            } catch (IOException e) {
                logger.warn("Failed to load QoL feature states: {}", e.getMessage());
            }
        }
    }
    
    private void saveFeatureStates() {
        try {
            Files.createDirectories(configPath.getParent());
            try (OutputStream os = Files.newOutputStream(configPath)) {
                featureStates.store(os, "Rubidium QoL Feature States");
            }
        } catch (IOException e) {
            logger.error("Failed to save QoL feature states: {}", e.getMessage());
        }
    }
}
