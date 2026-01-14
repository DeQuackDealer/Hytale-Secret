package rubidium.chat;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player chat state.
 */
public class PlayerChatState {
    
    private final UUID playerId;
    private String currentChannel = "global";
    private final Set<String> mutedChannels;
    private final Map<UUID, Long> lastMessages;
    private long lastMessageTime;
    private boolean globalMuted;
    
    public PlayerChatState(UUID playerId) {
        this.playerId = playerId;
        this.mutedChannels = ConcurrentHashMap.newKeySet();
        this.lastMessages = new ConcurrentHashMap<>();
    }
    
    public UUID getPlayerId() { return playerId; }
    
    public String getCurrentChannel() { return currentChannel; }
    public void setCurrentChannel(String channel) { this.currentChannel = channel; }
    
    public void muteChannel(String channelId) { mutedChannels.add(channelId); }
    public void unmuteChannel(String channelId) { mutedChannels.remove(channelId); }
    public boolean isChannelMuted(String channelId) { return mutedChannels.contains(channelId); }
    
    public long getLastMessageTime() { return lastMessageTime; }
    public void updateLastMessageTime() { this.lastMessageTime = System.currentTimeMillis(); }
    
    public boolean isGlobalMuted() { return globalMuted; }
    public void setGlobalMuted(boolean muted) { this.globalMuted = muted; }
}
