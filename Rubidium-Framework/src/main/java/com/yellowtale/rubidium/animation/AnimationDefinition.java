package com.yellowtale.rubidium.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class AnimationDefinition {
    
    private final String id;
    private final String name;
    private final float duration;
    private final boolean looping;
    
    private final List<AnimationTrack> tracks;
    private final List<AnimationEvent> events;
    private final Map<String, Float> blendShapes;
    
    private final int priority;
    private final float blendInTime;
    private final float blendOutTime;
    
    private AnimationDefinition(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.duration = builder.duration;
        this.looping = builder.looping;
        this.tracks = List.copyOf(builder.tracks);
        this.events = List.copyOf(builder.events);
        this.blendShapes = Map.copyOf(builder.blendShapes);
        this.priority = builder.priority;
        this.blendInTime = builder.blendInTime;
        this.blendOutTime = builder.blendOutTime;
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public float getDuration() { return duration; }
    public boolean isLooping() { return looping; }
    public List<AnimationTrack> getTracks() { return tracks; }
    public List<AnimationEvent> getEvents() { return events; }
    public Map<String, Float> getBlendShapes() { return blendShapes; }
    public int getPriority() { return priority; }
    public float getBlendInTime() { return blendInTime; }
    public float getBlendOutTime() { return blendOutTime; }
    
    public float getFrameRate() {
        if (tracks.isEmpty()) return 30;
        return tracks.get(0).getKeyframes().size() / duration;
    }
    
    public static Builder builder(String id) {
        return new Builder(id);
    }
    
    public static class Builder {
        private final String id;
        private String name;
        private float duration = 1.0f;
        private boolean looping = false;
        private List<AnimationTrack> tracks = new ArrayList<>();
        private List<AnimationEvent> events = new ArrayList<>();
        private Map<String, Float> blendShapes = new HashMap<>();
        private int priority = 0;
        private float blendInTime = 0.1f;
        private float blendOutTime = 0.1f;
        
        private Builder(String id) {
            this.id = id;
            this.name = id;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder duration(float duration) {
            this.duration = duration;
            return this;
        }
        
        public Builder looping(boolean looping) {
            this.looping = looping;
            return this;
        }
        
        public Builder addTrack(AnimationTrack track) {
            this.tracks.add(track);
            return this;
        }
        
        public Builder addEvent(AnimationEvent event) {
            this.events.add(event);
            return this;
        }
        
        public Builder addBlendShape(String name, float weight) {
            this.blendShapes.put(name, weight);
            return this;
        }
        
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }
        
        public Builder blendTimes(float blendIn, float blendOut) {
            this.blendInTime = blendIn;
            this.blendOutTime = blendOut;
            return this;
        }
        
        public AnimationDefinition build() {
            return new AnimationDefinition(this);
        }
    }
}
