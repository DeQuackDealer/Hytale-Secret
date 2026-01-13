package rubidium.animation;

import rubidium.core.logging.RubidiumLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AnimationManager {
    
    private final RubidiumLogger logger;
    
    private final Map<String, AnimationDefinition> definitions;
    private final Map<UUID, AnimationState> entityAnimations;
    private final Map<String, AnimationBlendTree> blendTrees;
    
    public AnimationManager(RubidiumLogger logger) {
        this.logger = logger;
        this.definitions = new ConcurrentHashMap<>();
        this.entityAnimations = new ConcurrentHashMap<>();
        this.blendTrees = new ConcurrentHashMap<>();
    }
    
    public void registerAnimation(AnimationDefinition animation) {
        definitions.put(animation.getId(), animation);
        logger.debug("Registered animation: {}", animation.getId());
    }
    
    public Optional<AnimationDefinition> getAnimation(String id) {
        return Optional.ofNullable(definitions.get(id));
    }
    
    public void registerBlendTree(AnimationBlendTree tree) {
        blendTrees.put(tree.getId(), tree);
        logger.debug("Registered blend tree: {}", tree.getId());
    }
    
    public AnimationState getOrCreateState(UUID entityId) {
        return entityAnimations.computeIfAbsent(entityId, id -> new AnimationState(id));
    }
    
    public void play(UUID entityId, String animationId) {
        play(entityId, animationId, PlayMode.ONCE, 1.0f);
    }
    
    public void play(UUID entityId, String animationId, PlayMode mode, float speed) {
        AnimationDefinition def = definitions.get(animationId);
        if (def == null) {
            logger.warn("Unknown animation: {}", animationId);
            return;
        }
        
        AnimationState state = getOrCreateState(entityId);
        state.play(def, mode, speed);
    }
    
    public void crossfade(UUID entityId, String animationId, float duration) {
        AnimationDefinition def = definitions.get(animationId);
        if (def == null) return;
        
        AnimationState state = getOrCreateState(entityId);
        state.crossfade(def, duration);
    }
    
    public void stop(UUID entityId) {
        AnimationState state = entityAnimations.get(entityId);
        if (state != null) {
            state.stop();
        }
    }
    
    public void pause(UUID entityId) {
        AnimationState state = entityAnimations.get(entityId);
        if (state != null) {
            state.pause();
        }
    }
    
    public void resume(UUID entityId) {
        AnimationState state = entityAnimations.get(entityId);
        if (state != null) {
            state.resume();
        }
    }
    
    public void update(float deltaTime) {
        for (AnimationState state : entityAnimations.values()) {
            state.update(deltaTime);
        }
    }
    
    public void removeEntity(UUID entityId) {
        entityAnimations.remove(entityId);
    }
    
    public boolean isPlaying(UUID entityId, String animationId) {
        AnimationState state = entityAnimations.get(entityId);
        return state != null && state.isPlaying(animationId);
    }
    
    public float getProgress(UUID entityId) {
        AnimationState state = entityAnimations.get(entityId);
        return state != null ? state.getProgress() : 0;
    }
    
    public void setLayerWeight(UUID entityId, int layer, float weight) {
        AnimationState state = entityAnimations.get(entityId);
        if (state != null) {
            state.setLayerWeight(layer, weight);
        }
    }
    
    public int getActiveAnimationCount() {
        return (int) entityAnimations.values().stream()
            .filter(AnimationState::isActive)
            .count();
    }
    
    public Collection<String> getRegisteredAnimations() {
        return Collections.unmodifiableCollection(definitions.keySet());
    }
    
    public enum PlayMode {
        ONCE,
        LOOP,
        PING_PONG,
        CLAMP
    }
}
