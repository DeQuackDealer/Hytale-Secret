package rubidium.voice;

import rubidium.hytale.api.player.Player;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents an active voice session for a player.
 */
public class VoiceSession {
    
    private final UUID sessionId;
    private final Player player;
    private final Instant connectedAt;
    
    private VoicePlayerSettings settings;
    private final Set<VoiceChannel> joinedChannels = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Float> playerVolumes = new ConcurrentHashMap<>();
    
    private volatile boolean muted = false;
    private volatile boolean deafened = false;
    private volatile boolean speaking = false;
    private volatile VoiceMode voiceMode = VoiceMode.VOICE_ACTIVITY;
    private volatile boolean pushToTalkActive = false;
    
    private volatile double lastX, lastY, lastZ;
    
    public VoiceSession(Player player, VoicePlayerSettings settings) {
        this.sessionId = UUID.randomUUID();
        this.player = player;
        this.settings = settings;
        this.connectedAt = Instant.now();
        
        updatePosition();
    }
    
    public UUID getSessionId() { return sessionId; }
    public Player getPlayer() { return player; }
    public UUID getPlayerId() { return player.getUuid(); }
    public Instant getConnectedAt() { return connectedAt; }
    
    public boolean isMuted() { return muted; }
    public boolean isDeafened() { return deafened; }
    public boolean isSpeaking() { return speaking; }
    
    public void setMuted(boolean muted) {
        this.muted = muted;
        if (muted) this.speaking = false;
    }
    
    public void setDeafened(boolean deafened) {
        this.deafened = deafened;
    }
    
    public void setSpeaking(boolean speaking) {
        if (!muted) {
            this.speaking = speaking;
        }
    }
    
    public VoiceMode getVoiceMode() { return voiceMode; }
    
    public void setVoiceMode(VoiceMode mode) {
        this.voiceMode = mode;
    }
    
    public boolean isPushToTalkActive() { return pushToTalkActive; }
    
    public void setPushToTalkActive(boolean active) {
        this.pushToTalkActive = active;
        if (voiceMode == VoiceMode.PUSH_TO_TALK) {
            setSpeaking(active && !muted);
        }
    }
    
    public void updateSettings(VoicePlayerSettings settings) {
        this.settings = settings;
    }
    
    public VoicePlayerSettings getSettings() {
        return settings;
    }
    
    public void joinChannel(VoiceChannel channel) {
        joinedChannels.add(channel);
        channel.addMember(this);
    }
    
    public void leaveChannel(VoiceChannel channel) {
        joinedChannels.remove(channel);
        channel.removeMember(this);
    }
    
    public void leaveAllChannels() {
        for (VoiceChannel channel : new ArrayList<>(joinedChannels)) {
            leaveChannel(channel);
        }
    }
    
    public Set<VoiceChannel> getJoinedChannels() {
        return Collections.unmodifiableSet(joinedChannels);
    }
    
    public boolean isInChannel(String channelId) {
        return joinedChannels.stream().anyMatch(c -> c.getId().equals(channelId));
    }
    
    public void setVolumeForPlayer(UUID targetId, float volume) {
        playerVolumes.put(targetId, Math.max(0f, Math.min(2f, volume)));
    }
    
    public float getVolumeForPlayer(UUID targetId) {
        return playerVolumes.getOrDefault(targetId, 1.0f);
    }
    
    public void updatePosition() {
        var loc = player.getLocation();
        this.lastX = loc.x();
        this.lastY = loc.y();
        this.lastZ = loc.z();
    }
    
    public double getX() { return lastX; }
    public double getY() { return lastY; }
    public double getZ() { return lastZ; }
    
    public double distanceTo(VoiceSession other) {
        double dx = lastX - other.lastX;
        double dy = lastY - other.lastY;
        double dz = lastZ - other.lastZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    public void disconnect() {
        leaveAllChannels();
        speaking = false;
    }
    
    public enum VoiceMode {
        VOICE_ACTIVITY,
        PUSH_TO_TALK,
        DISABLED
    }
}
