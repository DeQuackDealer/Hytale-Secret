package rubidium.voice;

/**
 * Player-specific voice settings.
 */
public class VoicePlayerSettings {
    
    private float inputVolume;
    private float outputVolume;
    private float voiceActivationThreshold;
    private VoiceSession.VoiceMode voiceMode;
    private boolean noiseSuppressionEnabled;
    private boolean echoCanncellationEnabled;
    private boolean spatialAudioEnabled;
    private int inputDeviceId;
    private int outputDeviceId;
    
    public VoicePlayerSettings() {
        this.inputVolume = 1.0f;
        this.outputVolume = 1.0f;
        this.voiceActivationThreshold = 0.02f;
        this.voiceMode = VoiceSession.VoiceMode.VOICE_ACTIVITY;
        this.noiseSuppressionEnabled = true;
        this.echoCanncellationEnabled = true;
        this.spatialAudioEnabled = true;
        this.inputDeviceId = -1;
        this.outputDeviceId = -1;
    }
    
    public static VoicePlayerSettings defaults() {
        return new VoicePlayerSettings();
    }
    
    public float getInputVolume() { return inputVolume; }
    public void setInputVolume(float inputVolume) {
        this.inputVolume = Math.max(0f, Math.min(2f, inputVolume));
    }
    
    public float getOutputVolume() { return outputVolume; }
    public void setOutputVolume(float outputVolume) {
        this.outputVolume = Math.max(0f, Math.min(2f, outputVolume));
    }
    
    public float getVoiceActivationThreshold() { return voiceActivationThreshold; }
    public void setVoiceActivationThreshold(float threshold) {
        this.voiceActivationThreshold = Math.max(0f, Math.min(1f, threshold));
    }
    
    public VoiceSession.VoiceMode getVoiceMode() { return voiceMode; }
    public void setVoiceMode(VoiceSession.VoiceMode voiceMode) {
        this.voiceMode = voiceMode;
    }
    
    public boolean isNoiseSuppressionEnabled() { return noiseSuppressionEnabled; }
    public void setNoiseSuppressionEnabled(boolean enabled) {
        this.noiseSuppressionEnabled = enabled;
    }
    
    public boolean isEchoCancellationEnabled() { return echoCanncellationEnabled; }
    public void setEchoCancellationEnabled(boolean enabled) {
        this.echoCanncellationEnabled = enabled;
    }
    
    public boolean isSpatialAudioEnabled() { return spatialAudioEnabled; }
    public void setSpatialAudioEnabled(boolean enabled) {
        this.spatialAudioEnabled = enabled;
    }
    
    public int getInputDeviceId() { return inputDeviceId; }
    public void setInputDeviceId(int id) { this.inputDeviceId = id; }
    
    public int getOutputDeviceId() { return outputDeviceId; }
    public void setOutputDeviceId(int id) { this.outputDeviceId = id; }
    
    public VoicePlayerSettings copy() {
        VoicePlayerSettings copy = new VoicePlayerSettings();
        copy.inputVolume = this.inputVolume;
        copy.outputVolume = this.outputVolume;
        copy.voiceActivationThreshold = this.voiceActivationThreshold;
        copy.voiceMode = this.voiceMode;
        copy.noiseSuppressionEnabled = this.noiseSuppressionEnabled;
        copy.echoCanncellationEnabled = this.echoCanncellationEnabled;
        copy.spatialAudioEnabled = this.spatialAudioEnabled;
        copy.inputDeviceId = this.inputDeviceId;
        copy.outputDeviceId = this.outputDeviceId;
        return copy;
    }
}
