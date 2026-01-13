package com.yellowtale.rubidium.voice;

import java.util.UUID;

public class VoiceState {
    private final UUID playerId;
    private VoiceChatManager.Vector3d position;
    private VoiceChatManager.Vector3d lookDirection;
    
    private boolean speaking;
    private boolean muted;
    private boolean selfMuted;
    private boolean deafened;
    private boolean selfDeafened;
    private boolean prioritySpeaker;
    
    private float inputVolume;
    private float outputVolume;
    private VoiceConfig.NoiseSuppressionLevel noiseSuppression;
    
    private long lastVoiceActivity;
    private long totalSpeakTimeMs;
    private int packetsReceived;
    private int packetsSent;
    private int packetsDropped;
    
    public VoiceState(UUID playerId) {
        this.playerId = playerId;
        this.position = new VoiceChatManager.Vector3d(0, 0, 0);
        this.lookDirection = new VoiceChatManager.Vector3d(0, 0, 1);
        this.speaking = false;
        this.muted = false;
        this.selfMuted = false;
        this.deafened = false;
        this.selfDeafened = false;
        this.prioritySpeaker = false;
        this.inputVolume = 1.0f;
        this.outputVolume = 1.0f;
        this.noiseSuppression = VoiceConfig.NoiseSuppressionLevel.MODERATE;
        this.lastVoiceActivity = 0;
        this.totalSpeakTimeMs = 0;
        this.packetsReceived = 0;
        this.packetsSent = 0;
        this.packetsDropped = 0;
    }
    
    public UUID getPlayerId() { return playerId; }
    public VoiceChatManager.Vector3d getPosition() { return position; }
    public void setPosition(VoiceChatManager.Vector3d position) { this.position = position; }
    public VoiceChatManager.Vector3d getLookDirection() { return lookDirection; }
    public void setLookDirection(VoiceChatManager.Vector3d lookDirection) { this.lookDirection = lookDirection; }
    
    public boolean isSpeaking() { return speaking; }
    public void setSpeaking(boolean speaking) { this.speaking = speaking; }
    public boolean isMuted() { return muted; }
    public void setMuted(boolean muted) { this.muted = muted; }
    public boolean isSelfMuted() { return selfMuted; }
    public void setSelfMuted(boolean selfMuted) { this.selfMuted = selfMuted; }
    public boolean isDeafened() { return deafened; }
    public void setDeafened(boolean deafened) { this.deafened = deafened; }
    public boolean isSelfDeafened() { return selfDeafened; }
    public void setSelfDeafened(boolean selfDeafened) { this.selfDeafened = selfDeafened; }
    public boolean isPrioritySpeaker() { return prioritySpeaker; }
    public void setPrioritySpeaker(boolean prioritySpeaker) { this.prioritySpeaker = prioritySpeaker; }
    
    public float getInputVolume() { return inputVolume; }
    public void setInputVolume(float inputVolume) { this.inputVolume = inputVolume; }
    public float getOutputVolume() { return outputVolume; }
    public void setOutputVolume(float outputVolume) { this.outputVolume = outputVolume; }
    public VoiceConfig.NoiseSuppressionLevel getNoiseSuppression() { return noiseSuppression; }
    public void setNoiseSuppression(VoiceConfig.NoiseSuppressionLevel noiseSuppression) { this.noiseSuppression = noiseSuppression; }
    
    public long getLastVoiceActivity() { return lastVoiceActivity; }
    public void setLastVoiceActivity(long lastVoiceActivity) { this.lastVoiceActivity = lastVoiceActivity; }
    public long getTotalSpeakTimeMs() { return totalSpeakTimeMs; }
    public void addSpeakTime(long ms) { this.totalSpeakTimeMs += ms; }
    public int getPacketsReceived() { return packetsReceived; }
    public void incrementPacketsReceived() { this.packetsReceived++; }
    public int getPacketsSent() { return packetsSent; }
    public void incrementPacketsSent() { this.packetsSent++; }
    public int getPacketsDropped() { return packetsDropped; }
    public void incrementPacketsDropped() { this.packetsDropped++; }
    
    public boolean canSpeak() {
        return !muted && !selfMuted;
    }
    
    public boolean canHear() {
        return !deafened && !selfDeafened;
    }
}
