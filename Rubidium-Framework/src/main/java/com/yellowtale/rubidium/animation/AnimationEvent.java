package com.yellowtale.rubidium.animation;

import java.util.Map;

public record AnimationEvent(
    float time,
    String eventId,
    EventType type,
    Map<String, Object> data
) {
    
    public AnimationEvent(float time, String eventId, EventType type) {
        this(time, eventId, type, Map.of());
    }
    
    public static AnimationEvent sound(float time, String soundId) {
        return new AnimationEvent(time, soundId, EventType.SOUND, Map.of("sound", soundId));
    }
    
    public static AnimationEvent particle(float time, String particleId, String bone) {
        return new AnimationEvent(time, particleId, EventType.PARTICLE, 
            Map.of("particle", particleId, "bone", bone));
    }
    
    public static AnimationEvent callback(float time, String callbackId) {
        return new AnimationEvent(time, callbackId, EventType.CALLBACK);
    }
    
    public static AnimationEvent footstep(float time, String foot) {
        return new AnimationEvent(time, "footstep", EventType.FOOTSTEP, Map.of("foot", foot));
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getData(String key, Class<T> type) {
        Object value = data.get(key);
        return type.isInstance(value) ? (T) value : null;
    }
    
    public enum EventType {
        SOUND,
        PARTICLE,
        CALLBACK,
        FOOTSTEP,
        DAMAGE,
        EFFECT,
        CUSTOM
    }
}
