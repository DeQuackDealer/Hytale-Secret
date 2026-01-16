package rubidium.api.sound;

import rubidium.api.registry.ResourceId;
import java.util.*;

public class SoundDefinition {
    
    private final ResourceId id;
    private final String displayName;
    private final SoundCategory category;
    private final List<String> sounds;
    private final float volume;
    private final float pitch;
    private final float minPitch;
    private final float maxPitch;
    private final boolean stream;
    private final int attenuationDistance;
    
    private SoundDefinition(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.category = builder.category;
        this.sounds = List.copyOf(builder.sounds);
        this.volume = builder.volume;
        this.pitch = builder.pitch;
        this.minPitch = builder.minPitch;
        this.maxPitch = builder.maxPitch;
        this.stream = builder.stream;
        this.attenuationDistance = builder.attenuationDistance;
    }
    
    public ResourceId getId() { return id; }
    public String getDisplayName() { return displayName; }
    public SoundCategory getCategory() { return category; }
    public List<String> getSounds() { return sounds; }
    public float getVolume() { return volume; }
    public float getPitch() { return pitch; }
    public float getMinPitch() { return minPitch; }
    public float getMaxPitch() { return maxPitch; }
    public boolean isStream() { return stream; }
    public int getAttenuationDistance() { return attenuationDistance; }
    
    public static Builder builder(String id) {
        return new Builder(ResourceId.of(id));
    }
    
    public static Builder builder(ResourceId id) {
        return new Builder(id);
    }
    
    public enum SoundCategory {
        MASTER, MUSIC, RECORD, WEATHER, BLOCKS, HOSTILE, NEUTRAL, PLAYERS, AMBIENT, VOICE
    }
    
    public static class Builder {
        private final ResourceId id;
        private String displayName;
        private SoundCategory category = SoundCategory.MASTER;
        private List<String> sounds = new ArrayList<>();
        private float volume = 1.0f;
        private float pitch = 1.0f;
        private float minPitch = 0.8f;
        private float maxPitch = 1.2f;
        private boolean stream = false;
        private int attenuationDistance = 16;
        
        private Builder(ResourceId id) {
            this.id = id;
            this.displayName = id.path();
        }
        
        public Builder displayName(String name) { this.displayName = name; return this; }
        public Builder category(SoundCategory cat) { this.category = cat; return this; }
        public Builder sound(String path) { this.sounds.add(path); return this; }
        public Builder sounds(String... paths) { this.sounds.addAll(Arrays.asList(paths)); return this; }
        public Builder volume(float vol) { this.volume = vol; return this; }
        public Builder pitch(float pitch) { this.pitch = pitch; return this; }
        public Builder pitchRange(float min, float max) { this.minPitch = min; this.maxPitch = max; return this; }
        public Builder stream() { this.stream = true; return this; }
        public Builder attenuationDistance(int dist) { this.attenuationDistance = dist; return this; }
        
        public SoundDefinition build() {
            return new SoundDefinition(this);
        }
    }
}
