package rubidium.animation;

import java.util.*;
import java.util.concurrent.*;

/**
 * Animation playback controller for .blockyanim files.
 * Supports keyframes, transitions, and easing.
 */
public class AnimationPlayer {
    
    private static AnimationPlayer instance;
    
    private final Map<String, BlockyAnimation> animations;
    private final Map<UUID, PlaybackState> playbackStates;
    private final ScheduledExecutorService ticker;
    
    private AnimationPlayer() {
        this.animations = new ConcurrentHashMap<>();
        this.playbackStates = new ConcurrentHashMap<>();
        this.ticker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Rubidium-Animation");
            t.setDaemon(true);
            return t;
        });
        
        ticker.scheduleAtFixedRate(this::tick, 0, 50, TimeUnit.MILLISECONDS);
    }
    
    public static AnimationPlayer getInstance() {
        if (instance == null) {
            instance = new AnimationPlayer();
        }
        return instance;
    }
    
    public void registerAnimation(BlockyAnimation animation) {
        animations.put(animation.getId(), animation);
    }
    
    public UUID play(String animationId, AnimationTarget target) {
        return play(animationId, target, false, 1.0f);
    }
    
    public UUID play(String animationId, AnimationTarget target, boolean loop, float speed) {
        BlockyAnimation animation = animations.get(animationId);
        if (animation == null) {
            throw new IllegalArgumentException("Animation not found: " + animationId);
        }
        
        UUID playbackId = UUID.randomUUID();
        PlaybackState state = new PlaybackState(playbackId, animation, target, loop, speed);
        playbackStates.put(playbackId, state);
        
        return playbackId;
    }
    
    public void stop(UUID playbackId) {
        playbackStates.remove(playbackId);
    }
    
    public void stopAll(AnimationTarget target) {
        playbackStates.entrySet().removeIf(e -> e.getValue().getTarget().equals(target));
    }
    
    public void pause(UUID playbackId) {
        PlaybackState state = playbackStates.get(playbackId);
        if (state != null) {
            state.setPaused(true);
        }
    }
    
    public void resume(UUID playbackId) {
        PlaybackState state = playbackStates.get(playbackId);
        if (state != null) {
            state.setPaused(false);
        }
    }
    
    public void setSpeed(UUID playbackId, float speed) {
        PlaybackState state = playbackStates.get(playbackId);
        if (state != null) {
            state.setSpeed(speed);
        }
    }
    
    private void tick() {
        long now = System.currentTimeMillis();
        List<UUID> completed = new ArrayList<>();
        
        for (PlaybackState state : playbackStates.values()) {
            if (state.isPaused()) continue;
            
            state.update(now);
            
            if (state.isComplete()) {
                if (state.isLoop()) {
                    state.restart();
                } else {
                    completed.add(state.getId());
                }
            } else {
                applyAnimation(state);
            }
        }
        
        completed.forEach(playbackStates::remove);
    }
    
    private void applyAnimation(PlaybackState state) {
        BlockyAnimation animation = state.getAnimation();
        AnimationTarget target = state.getTarget();
        float time = state.getCurrentTime();
        
        for (NodeAnimation nodeAnim : animation.getNodeAnimations().values()) {
            if (!nodeAnim.getPosition().isEmpty()) {
                float[] pos = interpolateKeyframes(nodeAnim.getPosition(), time);
                target.setPosition(nodeAnim.getNodeName(), pos);
            }
            
            if (!nodeAnim.getOrientation().isEmpty()) {
                float[] orient = interpolateKeyframes(nodeAnim.getOrientation(), time);
                target.setOrientation(nodeAnim.getNodeName(), orient);
            }
            
            if (!nodeAnim.getShapeStretch().isEmpty()) {
                float[] stretch = interpolateKeyframes(nodeAnim.getShapeStretch(), time);
                target.setScale(nodeAnim.getNodeName(), stretch);
            }
            
            if (!nodeAnim.getShapeVisible().isEmpty()) {
                boolean visible = getVisibilityAtTime(nodeAnim.getShapeVisible(), time);
                target.setVisible(nodeAnim.getNodeName(), visible);
            }
        }
    }
    
    private float[] interpolateKeyframes(List<Keyframe> keyframes, float time) {
        if (keyframes.isEmpty()) return new float[]{0, 0, 0};
        if (keyframes.size() == 1) return keyframes.get(0).getDelta();
        
        Keyframe before = keyframes.get(0);
        Keyframe after = keyframes.get(keyframes.size() - 1);
        
        for (int i = 0; i < keyframes.size() - 1; i++) {
            if (keyframes.get(i).getTime() <= time && keyframes.get(i + 1).getTime() >= time) {
                before = keyframes.get(i);
                after = keyframes.get(i + 1);
                break;
            }
        }
        
        float t = (time - before.getTime()) / (after.getTime() - before.getTime());
        t = applyEasing(t, after.getInterpolationType());
        
        float[] beforeDelta = before.getDelta();
        float[] afterDelta = after.getDelta();
        
        float[] result = new float[beforeDelta.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = beforeDelta[i] + (afterDelta[i] - beforeDelta[i]) * t;
        }
        
        return result;
    }
    
    private boolean getVisibilityAtTime(List<Keyframe> keyframes, float time) {
        for (int i = keyframes.size() - 1; i >= 0; i--) {
            if (keyframes.get(i).getTime() <= time) {
                return keyframes.get(i).getDelta()[0] > 0.5f;
            }
        }
        return true;
    }
    
    private float applyEasing(float t, InterpolationType type) {
        return switch (type) {
            case LINEAR -> t;
            case SMOOTH -> t * t * (3 - 2 * t);
            case EASE_IN -> t * t;
            case EASE_OUT -> 1 - (1 - t) * (1 - t);
            case EASE_IN_OUT -> t < 0.5f ? 2 * t * t : 1 - (float)Math.pow(-2 * t + 2, 2) / 2;
            case STEP -> t < 1 ? 0 : 1;
        };
    }
    
    public Optional<BlockyAnimation> getAnimation(String id) {
        return Optional.ofNullable(animations.get(id));
    }
    
    public boolean isPlaying(UUID playbackId) {
        return playbackStates.containsKey(playbackId);
    }
    
    public void shutdown() {
        ticker.shutdown();
    }
}
