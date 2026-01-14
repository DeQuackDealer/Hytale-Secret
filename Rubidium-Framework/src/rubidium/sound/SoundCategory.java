package rubidium.sound;

/**
 * Sound category for volume control grouping.
 */
public class SoundCategory {
    
    private final String id;
    private float volume;
    private boolean muted;
    
    public SoundCategory(String id, float defaultVolume) {
        this.id = id;
        this.volume = defaultVolume;
        this.muted = false;
    }
    
    public String getId() { return id; }
    
    public float getVolume() {
        return muted ? 0 : volume;
    }
    
    public void setVolume(float volume) {
        this.volume = Math.max(0, Math.min(1, volume));
    }
    
    public boolean isMuted() { return muted; }
    public void setMuted(boolean muted) { this.muted = muted; }
}
