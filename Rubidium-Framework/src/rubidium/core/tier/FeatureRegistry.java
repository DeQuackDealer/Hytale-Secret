package rubidium.core.tier;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class FeatureRegistry {
    
    private static ProductTier currentTier = ProductTier.FREE;
    private static final Map<String, Feature> features = new ConcurrentHashMap<>();
    private static final List<String> enabledFeatures = new ArrayList<>();
    
    private FeatureRegistry() {}
    
    public static void initialize(ProductTier tier) {
        currentTier = tier;
        registerBuiltinFeatures();
        System.out.println("[Rubidium] Initialized " + tier.getDisplayName() + " edition");
    }
    
    public static ProductTier getCurrentTier() {
        return currentTier;
    }
    
    public static boolean isPremium() {
        return currentTier.isPremium();
    }
    
    public static void register(Feature feature) {
        features.put(feature.id(), feature);
        if (feature.isEnabled()) {
            enabledFeatures.add(feature.id());
        }
    }
    
    public static boolean isEnabled(String featureId) {
        Feature feature = features.get(featureId);
        if (feature == null) return false;
        return feature.isEnabled();
    }
    
    public static boolean requiresPremium(String featureId) {
        Feature feature = features.get(featureId);
        if (feature == null) return false;
        return feature.tier() == ProductTier.PLUS;
    }
    
    public static <T> T withFeature(String featureId, Supplier<T> action, T fallback) {
        if (isEnabled(featureId)) {
            return action.get();
        }
        return fallback;
    }
    
    public static void withFeature(String featureId, Runnable action) {
        if (isEnabled(featureId)) {
            action.run();
        }
    }
    
    public static void requireFeature(String featureId) throws FeatureNotAvailableException {
        if (!isEnabled(featureId)) {
            Feature feature = features.get(featureId);
            if (feature != null && feature.tier() == ProductTier.PLUS) {
                throw new FeatureNotAvailableException(
                    "Feature '" + featureId + "' requires Rubidium Plus. Upgrade at rubidium.dev/plus");
            }
            throw new FeatureNotAvailableException("Feature '" + featureId + "' is not available");
        }
    }
    
    public static List<Feature> getAllFeatures() {
        return List.copyOf(features.values());
    }
    
    public static List<Feature> getFreeFeatures() {
        return features.values().stream()
            .filter(f -> f.tier() == ProductTier.FREE)
            .toList();
    }
    
    public static List<Feature> getPlusFeatures() {
        return features.values().stream()
            .filter(f -> f.tier() == ProductTier.PLUS)
            .toList();
    }
    
    private static void registerBuiltinFeatures() {
        register(new Feature("optimization.memory", "Memory Optimization", ProductTier.FREE, 
            "Automatic memory management and garbage collection tuning"));
        register(new Feature("optimization.network", "Network Optimization", ProductTier.FREE,
            "Packet compression and connection pooling"));
        register(new Feature("optimization.threading", "Thread Pool Management", ProductTier.FREE,
            "Efficient async task scheduling"));
        
        register(new Feature("api.command", "Command API", ProductTier.FREE,
            "Register and manage server commands"));
        register(new Feature("api.chat", "Chat API", ProductTier.FREE,
            "Chat messaging and formatting"));
        register(new Feature("api.event", "Event API", ProductTier.FREE,
            "Event handling and listeners"));
        register(new Feature("api.config", "Config API", ProductTier.FREE,
            "Configuration file management"));
        register(new Feature("api.plugin", "Plugin System", ProductTier.FREE,
            "Plugin loading and lifecycle management"));
        register(new Feature("api.player", "Player API", ProductTier.FREE,
            "Basic player management"));
        
        register(new Feature("api.npc", "NPC API", ProductTier.PLUS,
            "Create and manage NPCs with AI behaviors"));
        register(new Feature("api.ai", "AI Behavior API", ProductTier.PLUS,
            "Advanced AI behavior trees and state machines"));
        register(new Feature("api.pathfinding", "Pathfinding API", ProductTier.PLUS,
            "A* pathfinding and navigation meshes"));
        register(new Feature("api.worldgen", "World Generation API", ProductTier.PLUS,
            "Custom terrain and structure generation"));
        register(new Feature("api.inventory", "Inventory API", ProductTier.PLUS,
            "Custom inventory UIs and item management"));
        register(new Feature("api.economy", "Economy API", ProductTier.PLUS,
            "Virtual currency and transactions"));
        register(new Feature("api.particles", "Particles API", ProductTier.PLUS,
            "Custom particle effects"));
        register(new Feature("api.bossbar", "Bossbar API", ProductTier.PLUS,
            "Boss health bars and progress displays"));
        register(new Feature("api.scoreboard", "Scoreboard API", ProductTier.PLUS,
            "Custom scoreboards and objectives"));
        
        register(new Feature("feature.voicechat", "Voice Chat", ProductTier.PLUS,
            "Proximity voice chat with push-to-talk"));
        register(new Feature("feature.minimap", "Minimap", ProductTier.PLUS,
            "In-game minimap with waypoints"));
        register(new Feature("feature.statistics", "Performance Statistics", ProductTier.PLUS,
            "Real-time performance overlay"));
        register(new Feature("feature.hudeditor", "HUD Editor", ProductTier.PLUS,
            "Drag-and-drop HUD customization"));
        register(new Feature("feature.adminpanel", "Admin Panel", ProductTier.PLUS,
            "Server administration interface"));
        register(new Feature("feature.replay", "Replay System", ProductTier.PLUS,
            "Record and playback sessions"));
        
        register(new Feature("hytale.ui", "Hytale UI Integration", ProductTier.PLUS,
            "Native Hytale CustomUIPage integration"));
        register(new Feature("hytale.hud", "Hytale HUD Integration", ProductTier.PLUS,
            "Native Hytale HudManager integration"));
    }
    
    public record Feature(
        String id,
        String name,
        ProductTier tier,
        String description
    ) {
        public boolean isEnabled() {
            return tier == ProductTier.FREE || FeatureRegistry.currentTier.isPremium();
        }
        
        public boolean isPremiumOnly() {
            return tier == ProductTier.PLUS;
        }
    }
    
    public static class FeatureNotAvailableException extends RuntimeException {
        public FeatureNotAvailableException(String message) {
            super(message);
        }
    }
}
