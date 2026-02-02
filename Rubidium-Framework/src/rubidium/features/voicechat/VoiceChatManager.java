package rubidium.features.voicechat;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class VoiceChatManager {
    
    private static final Map<UUID, VoiceChatManager> instances = new ConcurrentHashMap<>();
    
    private final Map<UUID, VoiceChatState> playerStates = new ConcurrentHashMap<>();
    private final Set<UUID> speakingPlayers = ConcurrentHashMap.newKeySet();
    private BiConsumer<UUID, Boolean> speakingListener;
    
    public static VoiceChatManager getOrCreate(UUID playerId) {
        return instances.computeIfAbsent(playerId, k -> new VoiceChatManager());
    }
    
    public static void remove(UUID playerId) {
        VoiceChatManager manager = instances.remove(playerId);
        if (manager != null) {
            manager.cleanupPlayer(playerId);
        }
    }
    
    public void setSpeaking(UUID playerId, boolean speaking) {
        VoiceChatState state = getState(playerId);
        boolean wasSpaking = state.isSpeaking();
        state.setSpeaking(speaking);
        
        if (speaking) {
            speakingPlayers.add(playerId);
        } else {
            speakingPlayers.remove(playerId);
        }
        
        if (wasSpaking != speaking && speakingListener != null) {
            speakingListener.accept(playerId, speaking);
        }
    }
    
    public boolean isSpeaking(UUID playerId) {
        return getState(playerId).isSpeaking();
    }
    
    public void setMuted(UUID playerId, boolean muted) {
        getState(playerId).setMuted(muted);
    }
    
    public boolean isMuted(UUID playerId) {
        return getState(playerId).isMuted();
    }
    
    public void setDeafened(UUID playerId, boolean deafened) {
        getState(playerId).setDeafened(deafened);
    }
    
    public boolean isDeafened(UUID playerId) {
        return getState(playerId).isDeafened();
    }
    
    public void setVolume(UUID playerId, float volume) {
        getState(playerId).setVolume(volume);
    }
    
    public float getVolume(UUID playerId) {
        return getState(playerId).getVolume();
    }
    
    public Set<UUID> getSpeakingPlayers() {
        return Set.copyOf(speakingPlayers);
    }
    
    public void onSpeakingChanged(BiConsumer<UUID, Boolean> listener) {
        this.speakingListener = listener;
    }
    
    private VoiceChatState getState(UUID playerId) {
        return playerStates.computeIfAbsent(playerId, k -> new VoiceChatState());
    }
    
    public void cleanupPlayer(UUID playerId) {
        playerStates.remove(playerId);
        speakingPlayers.remove(playerId);
    }
}
