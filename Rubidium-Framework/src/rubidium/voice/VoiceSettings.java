package rubidium.voice;

/**
 * Global voice system settings.
 */
public class VoiceSettings {
    
    private int maxChannels;
    private int maxMembersPerChannel;
    private double defaultProximityRadius;
    private boolean spatialAudioEnabled;
    private int sampleRate;
    private int bitRate;
    private int frameSize;
    private boolean opusEnabled;
    private boolean recordingEnabled;
    private int maxRecordingDuration;
    
    public VoiceSettings() {
        this.maxChannels = 100;
        this.maxMembersPerChannel = 64;
        this.defaultProximityRadius = 50.0;
        this.spatialAudioEnabled = true;
        this.sampleRate = 48000;
        this.bitRate = 64000;
        this.frameSize = 960;
        this.opusEnabled = true;
        this.recordingEnabled = false;
        this.maxRecordingDuration = 300;
    }
    
    public static VoiceSettings defaults() {
        return new VoiceSettings();
    }
    
    public int getMaxChannels() { return maxChannels; }
    public void setMaxChannels(int max) { this.maxChannels = max; }
    
    public int getMaxMembersPerChannel() { return maxMembersPerChannel; }
    public void setMaxMembersPerChannel(int max) { this.maxMembersPerChannel = max; }
    
    public double getDefaultProximityRadius() { return defaultProximityRadius; }
    public void setDefaultProximityRadius(double radius) { this.defaultProximityRadius = radius; }
    
    public boolean isSpatialAudioEnabled() { return spatialAudioEnabled; }
    public void setSpatialAudioEnabled(boolean enabled) { this.spatialAudioEnabled = enabled; }
    
    public int getSampleRate() { return sampleRate; }
    public void setSampleRate(int rate) { this.sampleRate = rate; }
    
    public int getBitRate() { return bitRate; }
    public void setBitRate(int rate) { this.bitRate = rate; }
    
    public int getFrameSize() { return frameSize; }
    public void setFrameSize(int size) { this.frameSize = size; }
    
    public boolean isOpusEnabled() { return opusEnabled; }
    public void setOpusEnabled(boolean enabled) { this.opusEnabled = enabled; }
    
    public boolean isRecordingEnabled() { return recordingEnabled; }
    public void setRecordingEnabled(boolean enabled) { this.recordingEnabled = enabled; }
    
    public int getMaxRecordingDuration() { return maxRecordingDuration; }
    public void setMaxRecordingDuration(int duration) { this.maxRecordingDuration = duration; }
}
