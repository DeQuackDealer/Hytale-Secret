package rubidium.voice;

import rubidium.api.player.Player;
import rubidium.core.logging.RubidiumLogger;
import rubidium.core.scheduler.RubidiumScheduler;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class VoiceChatManager {
    
    private final RubidiumLogger logger;
    private final RubidiumScheduler scheduler;
    private VoiceConfig config;
    
    private final Map<UUID, VoiceState> playerStates;
    private final Map<String, VoiceChannel> channels;
    private final Map<UUID, String> playerChannels;
    private final ProximityManager proximityManager;
    private final VoiceMetrics metrics;
    
    private final List<BiConsumer<UUID, VoiceState>> stateListeners;
    private final List<BiConsumer<UUID, VoiceChannel>> channelJoinListeners;
    private final List<BiConsumer<UUID, VoiceChannel>> channelLeaveListeners;
    
    private volatile boolean enabled;
    
    public VoiceChatManager(RubidiumLogger logger, RubidiumScheduler scheduler) {
        this.logger = logger;
        this.scheduler = scheduler;
        this.config = VoiceConfig.defaults();
        this.playerStates = new ConcurrentHashMap<>();
        this.channels = new ConcurrentHashMap<>();
        this.playerChannels = new ConcurrentHashMap<>();
        this.proximityManager = new ProximityManager(config);
        this.metrics = new VoiceMetrics();
        this.stateListeners = new ArrayList<>();
        this.channelJoinListeners = new ArrayList<>();
        this.channelLeaveListeners = new ArrayList<>();
        this.enabled = false;
    }
    
    public void enable() {
        if (enabled) return;
        enabled = true;
        
        VoiceChannel global = createChannel("global", VoiceChannel.ChannelType.GLOBAL);
        global.getSettings().setPersistent(true);
        
        scheduler.runTaskTimer("voice-chat", this::tick, 0, 1);
        
        logger.info("Voice chat system enabled");
    }
    
    public void disable() {
        if (!enabled) return;
        enabled = false;
        
        playerStates.clear();
        channels.clear();
        playerChannels.clear();
        
        scheduler.cancelAllTasks("voice-chat");
        
        logger.info("Voice chat system disabled");
    }
    
    public void tick() {
        if (!enabled) return;
        
        long now = System.currentTimeMillis();
        
        for (var entry : playerStates.entrySet()) {
            VoiceState state = entry.getValue();
            
            if (state.isSpeaking() && now - state.getLastVoiceActivity() > 500) {
                state.setSpeaking(false);
                notifyStateChange(entry.getKey(), state);
            }
        }
        
        metrics.recordTick();
    }
    
    public void onPlayerJoin(Player player) {
        VoiceState state = new VoiceState(player.getId());
        var loc = player.getPosition();
        state.setPosition(new Vector3d(loc.x(), loc.y(), loc.z()));
        playerStates.put(player.getId(), state);
        
        if (config.autoJoinGlobal()) {
            VoiceChannel global = channels.get("global");
            if (global != null) {
                joinChannel(player.getId(), global);
            }
        }
    }
    
    public void onPlayerQuit(Player player) {
        String channelId = playerChannels.remove(player.getId());
        if (channelId != null) {
            VoiceChannel channel = channels.get(channelId);
            if (channel != null) {
                channel.removeMember(player.getId());
                notifyChannelLeave(player.getId(), channel);
            }
        }
        playerStates.remove(player.getId());
    }
    
    public void onPlayerMove(Player player, Vector3d newPosition) {
        VoiceState state = playerStates.get(player.getId());
        if (state != null) {
            state.setPosition(newPosition);
        }
    }
    
    public void setMuted(UUID playerId, boolean muted) {
        VoiceState state = playerStates.get(playerId);
        if (state != null) {
            state.setSelfMuted(muted);
            notifyStateChange(playerId, state);
        }
    }
    
    public void setDeafened(UUID playerId, boolean deafened) {
        VoiceState state = playerStates.get(playerId);
        if (state != null) {
            state.setSelfDeafened(deafened);
            notifyStateChange(playerId, state);
        }
    }
    
    public void setServerMuted(UUID playerId, boolean muted) {
        VoiceState state = playerStates.get(playerId);
        if (state != null) {
            state.setMuted(muted);
            notifyStateChange(playerId, state);
        }
    }
    
    public void setPrioritySpeaker(UUID playerId, boolean priority) {
        VoiceState state = playerStates.get(playerId);
        if (state != null) {
            state.setPrioritySpeaker(priority);
            notifyStateChange(playerId, state);
        }
    }
    
    public void setInputVolume(UUID playerId, float volume) {
        VoiceState state = playerStates.get(playerId);
        if (state != null) {
            state.setInputVolume(Math.max(0, Math.min(2.0f, volume)));
        }
    }
    
    public void setOutputVolume(UUID playerId, float volume) {
        VoiceState state = playerStates.get(playerId);
        if (state != null) {
            state.setOutputVolume(Math.max(0, Math.min(2.0f, volume)));
        }
    }
    
    public VoiceChannel createChannel(String name, VoiceChannel.ChannelType type) {
        String id = name.toLowerCase().replace(" ", "-");
        VoiceChannel channel = new VoiceChannel(id, name, type);
        channels.put(id, channel);
        logger.debug("Created voice channel: {}", name);
        return channel;
    }
    
    public void deleteChannel(String channelId) {
        VoiceChannel channel = channels.remove(channelId);
        if (channel != null) {
            for (UUID member : channel.getMembers()) {
                playerChannels.remove(member);
                notifyChannelLeave(member, channel);
            }
            logger.debug("Deleted voice channel: {}", channel.getName());
        }
    }
    
    public void joinChannel(UUID playerId, VoiceChannel channel) {
        String currentChannelId = playerChannels.get(playerId);
        if (currentChannelId != null) {
            leaveChannel(playerId, channels.get(currentChannelId));
        }
        
        channel.addMember(playerId);
        playerChannels.put(playerId, channel.getId());
        notifyChannelJoin(playerId, channel);
    }
    
    public void leaveChannel(UUID playerId, VoiceChannel channel) {
        if (channel == null) return;
        
        channel.removeMember(playerId);
        playerChannels.remove(playerId);
        notifyChannelLeave(playerId, channel);
    }
    
    public void routeAudio(UUID speaker, byte[] audioData) {
        VoiceState speakerState = playerStates.get(speaker);
        if (speakerState == null || speakerState.isMuted() || speakerState.isSelfMuted()) {
            return;
        }
        
        speakerState.setSpeaking(true);
        speakerState.setLastVoiceActivity(System.currentTimeMillis());
        
        String channelId = playerChannels.get(speaker);
        VoiceChannel channel = channelId != null ? channels.get(channelId) : null;
        
        if (channel != null && channel.getType() == VoiceChannel.ChannelType.PROXIMITY) {
            routeProximityAudio(speaker, speakerState, audioData);
        } else if (channel != null) {
            routeChannelAudio(speaker, channel, audioData);
        }
        
        metrics.recordPacketSent();
    }
    
    private void routeProximityAudio(UUID speaker, VoiceState speakerState, byte[] audioData) {
        for (var entry : playerStates.entrySet()) {
            UUID listenerId = entry.getKey();
            if (listenerId.equals(speaker)) continue;
            
            VoiceState listenerState = entry.getValue();
            if (listenerState.isDeafened() || listenerState.isSelfDeafened()) continue;
            
            double volume = proximityManager.calculateVolume(
                speakerState.getPosition(),
                listenerState.getPosition()
            );
            
            if (volume > 0) {
                sendAudioToPlayer(listenerId, speaker, audioData, (float) volume);
            }
        }
    }
    
    private void routeChannelAudio(UUID speaker, VoiceChannel channel, byte[] audioData) {
        for (UUID member : channel.getMembers()) {
            if (member.equals(speaker)) continue;
            
            VoiceState listenerState = playerStates.get(member);
            if (listenerState == null || listenerState.isDeafened() || listenerState.isSelfDeafened()) {
                continue;
            }
            
            sendAudioToPlayer(member, speaker, audioData, 1.0f);
        }
    }
    
    private void sendAudioToPlayer(UUID listener, UUID speaker, byte[] audioData, float volume) {
        metrics.recordPacketReceived();
    }
    
    public Optional<VoiceState> getVoiceState(UUID playerId) {
        return Optional.ofNullable(playerStates.get(playerId));
    }
    
    public Optional<VoiceChannel> getCurrentChannel(UUID playerId) {
        String channelId = playerChannels.get(playerId);
        return channelId != null ? Optional.ofNullable(channels.get(channelId)) : Optional.empty();
    }
    
    public List<VoiceChannel> getChannels() {
        return new ArrayList<>(channels.values());
    }
    
    public void onStateChange(BiConsumer<UUID, VoiceState> listener) {
        stateListeners.add(listener);
    }
    
    public void onChannelJoin(BiConsumer<UUID, VoiceChannel> listener) {
        channelJoinListeners.add(listener);
    }
    
    public void onChannelLeave(BiConsumer<UUID, VoiceChannel> listener) {
        channelLeaveListeners.add(listener);
    }
    
    private void notifyStateChange(UUID playerId, VoiceState state) {
        for (var listener : stateListeners) {
            listener.accept(playerId, state);
        }
    }
    
    private void notifyChannelJoin(UUID playerId, VoiceChannel channel) {
        for (var listener : channelJoinListeners) {
            listener.accept(playerId, channel);
        }
    }
    
    private void notifyChannelLeave(UUID playerId, VoiceChannel channel) {
        for (var listener : channelLeaveListeners) {
            listener.accept(playerId, channel);
        }
    }
    
    public void setConfig(VoiceConfig config) {
        this.config = config;
        this.proximityManager.setConfig(config);
    }
    
    public VoiceConfig getConfig() {
        return config;
    }
    
    public VoiceMetrics getMetrics() {
        return metrics;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public record Vector3d(double x, double y, double z) {
        public double distance(Vector3d other) {
            double dx = x - other.x;
            double dy = y - other.y;
            double dz = z - other.z;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }
}
