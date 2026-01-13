package com.yellowtale.rubidium.animation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AnimationTrack {
    
    private final String targetBone;
    private final TrackType type;
    private final List<Keyframe> keyframes;
    private final InterpolationType interpolation;
    
    public AnimationTrack(String targetBone, TrackType type, InterpolationType interpolation) {
        this.targetBone = targetBone;
        this.type = type;
        this.keyframes = new ArrayList<>();
        this.interpolation = interpolation;
    }
    
    public String getTargetBone() { return targetBone; }
    public TrackType getType() { return type; }
    public List<Keyframe> getKeyframes() { return Collections.unmodifiableList(keyframes); }
    public InterpolationType getInterpolation() { return interpolation; }
    
    public void addKeyframe(Keyframe keyframe) {
        keyframes.add(keyframe);
        keyframes.sort((a, b) -> Float.compare(a.time(), b.time()));
    }
    
    public float[] evaluate(float time) {
        if (keyframes.isEmpty()) {
            return new float[]{0, 0, 0};
        }
        
        if (keyframes.size() == 1) {
            return keyframes.get(0).values().clone();
        }
        
        Keyframe before = null;
        Keyframe after = null;
        
        for (int i = 0; i < keyframes.size(); i++) {
            if (keyframes.get(i).time() <= time) {
                before = keyframes.get(i);
            }
            if (keyframes.get(i).time() >= time && after == null) {
                after = keyframes.get(i);
            }
        }
        
        if (before == null) return keyframes.get(0).values().clone();
        if (after == null) return keyframes.get(keyframes.size() - 1).values().clone();
        if (before == after) return before.values().clone();
        
        float t = (time - before.time()) / (after.time() - before.time());
        t = applyInterpolation(t);
        
        return interpolate(before.values(), after.values(), t);
    }
    
    private float applyInterpolation(float t) {
        return switch (interpolation) {
            case LINEAR -> t;
            case STEP -> t < 1 ? 0 : 1;
            case EASE_IN -> t * t;
            case EASE_OUT -> 1 - (1 - t) * (1 - t);
            case EASE_IN_OUT -> t < 0.5f 
                ? 2 * t * t 
                : 1 - (float) Math.pow(-2 * t + 2, 2) / 2;
            case CUBIC -> t * t * (3 - 2 * t);
        };
    }
    
    private float[] interpolate(float[] a, float[] b, float t) {
        float[] result = new float[Math.max(a.length, b.length)];
        for (int i = 0; i < result.length; i++) {
            float va = i < a.length ? a[i] : 0;
            float vb = i < b.length ? b[i] : 0;
            result[i] = va + (vb - va) * t;
        }
        return result;
    }
    
    public record Keyframe(float time, float[] values) {}
    
    public enum TrackType {
        POSITION,
        ROTATION,
        SCALE,
        BLEND_SHAPE
    }
    
    public enum InterpolationType {
        LINEAR,
        STEP,
        EASE_IN,
        EASE_OUT,
        EASE_IN_OUT,
        CUBIC
    }
}
