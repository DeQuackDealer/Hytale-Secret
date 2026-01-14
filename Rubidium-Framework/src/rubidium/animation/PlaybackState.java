package rubidium.animation;

import java.util.UUID;

/**
 * Playback state for an active animation.
 */
public class PlaybackState {
    
    private final UUID id;
    private final BlockyAnimation animation;
    private final AnimationTarget target;
    private final boolean loop;
    private float speed;
    private float currentTime;
    private long lastUpdateTime;
    private boolean paused;
    
    public PlaybackState(UUID id, BlockyAnimation animation, AnimationTarget target, boolean loop, float speed) {
        this.id = id;
        this.animation = animation;
        this.target = target;
        this.loop = loop;
        this.speed = speed;
        this.currentTime = 0;
        this.lastUpdateTime = System.currentTimeMillis();
        this.paused = false;
    }
    
    public void update(long now) {
        if (paused) return;
        
        float deltaSeconds = (now - lastUpdateTime) / 1000.0f;
        currentTime += deltaSeconds * speed * 20;
        lastUpdateTime = now;
    }
    
    public void restart() {
        currentTime = 0;
        lastUpdateTime = System.currentTimeMillis();
    }
    
    public boolean isComplete() {
        return currentTime >= animation.getDuration();
    }
    
    public UUID getId() { return id; }
    public BlockyAnimation getAnimation() { return animation; }
    public AnimationTarget getTarget() { return target; }
    public boolean isLoop() { return loop; }
    public float getSpeed() { return speed; }
    public void setSpeed(float speed) { this.speed = speed; }
    public float getCurrentTime() { return currentTime; }
    public boolean isPaused() { return paused; }
    public void setPaused(boolean paused) { 
        this.paused = paused;
        if (!paused) this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public float getProgress() {
        return currentTime / animation.getDuration();
    }
}
