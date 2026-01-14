package rubidium.sound;

/**
 * Sound definition with properties for playback.
 */
public class Sound {
    
    private final String id;
    private final String resourcePath;
    private final String category;
    private final float baseVolume;
    private final boolean looping;
    private float pitchMin = 0.8f;
    private float pitchMax = 1.2f;
    private int cooldownMs = 0;
    
    public Sound(String id, String resourcePath, String category, float baseVolume, boolean looping) {
        this.id = id;
        this.resourcePath = resourcePath;
        this.category = category;
        this.baseVolume = baseVolume;
        this.looping = looping;
    }
    
    public static Builder builder(String id, String resourcePath) {
        return new Builder(id, resourcePath);
    }
    
    public String getId() { return id; }
    public String getResourcePath() { return resourcePath; }
    public String getCategory() { return category; }
    public float getBaseVolume() { return baseVolume; }
    public boolean isLooping() { return looping; }
    public float getPitchMin() { return pitchMin; }
    public float getPitchMax() { return pitchMax; }
    public int getCooldownMs() { return cooldownMs; }
    
    public float getRandomPitch() {
        return pitchMin + (float)(Math.random() * (pitchMax - pitchMin));
    }
    
    public static class Builder {
        private final String id;
        private final String resourcePath;
        private String category = "effects";
        private float baseVolume = 1.0f;
        private boolean looping = false;
        private float pitchMin = 0.8f;
        private float pitchMax = 1.2f;
        private int cooldownMs = 0;
        
        public Builder(String id, String resourcePath) {
            this.id = id;
            this.resourcePath = resourcePath;
        }
        
        public Builder category(String category) { this.category = category; return this; }
        public Builder volume(float volume) { this.baseVolume = volume; return this; }
        public Builder looping(boolean looping) { this.looping = looping; return this; }
        public Builder pitchRange(float min, float max) { this.pitchMin = min; this.pitchMax = max; return this; }
        public Builder cooldown(int ms) { this.cooldownMs = ms; return this; }
        
        public Sound build() {
            Sound sound = new Sound(id, resourcePath, category, baseVolume, looping);
            sound.pitchMin = pitchMin;
            sound.pitchMax = pitchMax;
            sound.cooldownMs = cooldownMs;
            return sound;
        }
    }
}
