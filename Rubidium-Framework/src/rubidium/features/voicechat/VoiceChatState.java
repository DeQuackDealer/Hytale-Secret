package rubidium.features.voicechat;

public class VoiceChatState {
    
    private boolean speaking = false;
    private boolean muted = false;
    private boolean deafened = false;
    private float volume = 1.0f;
    
    public boolean isSpeaking() {
        return speaking;
    }
    
    public void setSpeaking(boolean speaking) {
        this.speaking = speaking;
    }
    
    public boolean isMuted() {
        return muted;
    }
    
    public void setMuted(boolean muted) {
        this.muted = muted;
    }
    
    public boolean isDeafened() {
        return deafened;
    }
    
    public void setDeafened(boolean deafened) {
        this.deafened = deafened;
    }
    
    public float getVolume() {
        return volume;
    }
    
    public void setVolume(float volume) {
        this.volume = Math.max(0f, Math.min(1f, volume));
    }
}
