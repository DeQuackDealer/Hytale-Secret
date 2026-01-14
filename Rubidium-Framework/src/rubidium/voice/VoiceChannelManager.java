package rubidium.voice;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages voice channels.
 */
public class VoiceChannelManager {
    
    private static final Logger logger = Logger.getLogger("Rubidium-VoiceChannels");
    
    private final Map<String, VoiceChannel> channels = new ConcurrentHashMap<>();
    
    public VoiceChannel createChannel(String id, VoiceChannel.ChannelType type) {
        return createChannel(id, id, type);
    }
    
    public VoiceChannel createChannel(String id, String name, VoiceChannel.ChannelType type) {
        VoiceChannel channel = new VoiceChannel(id, name, type);
        channels.put(id, channel);
        logger.info("Created voice channel: " + name + " (" + type + ")");
        return channel;
    }
    
    public void deleteChannel(String id) {
        VoiceChannel channel = channels.remove(id);
        if (channel != null) {
            logger.info("Deleted voice channel: " + channel.getName());
        }
    }
    
    public Optional<VoiceChannel> getChannel(String id) {
        return Optional.ofNullable(channels.get(id));
    }
    
    public Collection<VoiceChannel> getAllChannels() {
        return Collections.unmodifiableCollection(channels.values());
    }
    
    public List<VoiceChannel> getChannelsByType(VoiceChannel.ChannelType type) {
        return channels.values().stream()
            .filter(c -> c.getType() == type)
            .toList();
    }
    
    public int getChannelCount() {
        return channels.size();
    }
    
    public VoiceChannel getOrCreatePartyChannel(UUID partyId) {
        String channelId = "party_" + partyId.toString();
        return channels.computeIfAbsent(channelId, id -> {
            VoiceChannel channel = new VoiceChannel(id, "Party", VoiceChannel.ChannelType.PARTY);
            logger.fine("Created party voice channel: " + id);
            return channel;
        });
    }
    
    public VoiceChannel createPrivateChannel(UUID ownerId, String name) {
        String channelId = "private_" + ownerId + "_" + System.currentTimeMillis();
        VoiceChannel channel = new VoiceChannel(channelId, name, VoiceChannel.ChannelType.PRIVATE);
        channel.setOwner(ownerId);
        channels.put(channelId, channel);
        logger.fine("Created private voice channel: " + name);
        return channel;
    }
}
