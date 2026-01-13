package com.yellowtale.rubidium.animation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AnimationState {
    
    private final UUID entityId;
    
    private AnimationDefinition currentAnimation;
    private AnimationDefinition previousAnimation;
    
    private float currentTime;
    private float speed;
    private boolean playing;
    private boolean paused;
    
    private AnimationManager.PlayMode playMode;
    private boolean reversed;
    
    private float crossfadeTime;
    private float crossfadeDuration;
    private boolean crossfading;
    
    private final Map<Integer, Float> layerWeights;
    
    public AnimationState(UUID entityId) {
        this.entityId = entityId;
        this.currentTime = 0;
        this.speed = 1.0f;
        this.playing = false;
        this.paused = false;
        this.playMode = AnimationManager.PlayMode.ONCE;
        this.reversed = false;
        this.crossfading = false;
        this.layerWeights = new HashMap<>();
        this.layerWeights.put(0, 1.0f);
    }
    
    public void play(AnimationDefinition animation, AnimationManager.PlayMode mode, float speed) {
        this.currentAnimation = animation;
        this.playMode = mode;
        this.speed = speed;
        this.currentTime = 0;
        this.playing = true;
        this.paused = false;
        this.reversed = false;
        this.crossfading = false;
    }
    
    public void crossfade(AnimationDefinition animation, float duration) {
        if (currentAnimation == null) {
            play(animation, AnimationManager.PlayMode.LOOP, 1.0f);
            return;
        }
        
        this.previousAnimation = this.currentAnimation;
        this.currentAnimation = animation;
        this.crossfadeTime = 0;
        this.crossfadeDuration = duration;
        this.crossfading = true;
        this.playing = true;
    }
    
    public void stop() {
        this.playing = false;
        this.currentTime = 0;
        this.crossfading = false;
    }
    
    public void pause() {
        this.paused = true;
    }
    
    public void resume() {
        this.paused = false;
    }
    
    public void update(float deltaTime) {
        if (!playing || paused) return;
        
        float dt = deltaTime * speed * (reversed ? -1 : 1);
        currentTime += dt;
        
        if (crossfading) {
            crossfadeTime += deltaTime;
            if (crossfadeTime >= crossfadeDuration) {
                crossfading = false;
                previousAnimation = null;
            }
        }
        
        if (currentAnimation != null) {
            float duration = currentAnimation.getDuration();
            
            switch (playMode) {
                case ONCE -> {
                    if (currentTime >= duration) {
                        currentTime = duration;
                        playing = false;
                    }
                }
                case LOOP -> {
                    while (currentTime >= duration) {
                        currentTime -= duration;
                    }
                    while (currentTime < 0) {
                        currentTime += duration;
                    }
                }
                case PING_PONG -> {
                    if (currentTime >= duration) {
                        currentTime = duration;
                        reversed = true;
                    } else if (currentTime <= 0) {
                        currentTime = 0;
                        reversed = false;
                    }
                }
                case CLAMP -> {
                    currentTime = Math.max(0, Math.min(currentTime, duration));
                }
            }
        }
    }
    
    public boolean isPlaying(String animationId) {
        return playing && currentAnimation != null && 
               currentAnimation.getId().equals(animationId);
    }
    
    public boolean isActive() {
        return playing && !paused;
    }
    
    public float getProgress() {
        if (currentAnimation == null || currentAnimation.getDuration() == 0) {
            return 0;
        }
        return currentTime / currentAnimation.getDuration();
    }
    
    public float getCrossfadeBlend() {
        if (!crossfading || crossfadeDuration == 0) return 1.0f;
        return Math.min(1.0f, crossfadeTime / crossfadeDuration);
    }
    
    public void setLayerWeight(int layer, float weight) {
        layerWeights.put(layer, Math.max(0, Math.min(1, weight)));
    }
    
    public float getLayerWeight(int layer) {
        return layerWeights.getOrDefault(layer, 0f);
    }
    
    public UUID getEntityId() { return entityId; }
    public AnimationDefinition getCurrentAnimation() { return currentAnimation; }
    public AnimationDefinition getPreviousAnimation() { return previousAnimation; }
    public float getCurrentTime() { return currentTime; }
    public float getSpeed() { return speed; }
    public void setSpeed(float speed) { this.speed = speed; }
    public boolean isPlaying() { return playing; }
    public boolean isPaused() { return paused; }
    public boolean isCrossfading() { return crossfading; }
}
