package rubidium.voice;

import rubidium.api.player.Player;
import rubidium.core.logging.RubidiumLogger;
import rubidium.core.scheduler.RubidiumScheduler;

import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class VoiceChatManager {
    
    private final RubidiumLogger logger;
    private final RubidiumScheduler scheduler;
    private VoiceConfig config;
    
    private final Map<UUID, VoiceState> playerStates;
    private final Map<String, VoiceChannel> channels;
    private final Map<UUID, String> playerChannels;
    private final Map<String, VoiceGroup> groups;
    private final Map<UUID, Set<String>> playerGroups;
    
    private final ProximityManager proximityManager;
    private final VoiceMetrics metrics;
    private final AudioMixer audioMixer;
    private VoiceRecorder recorder;
    
    private final Map<UUID, VoiceActivityDetector> vadProcessors;
    private final Map<UUID, RNNoiseProcessor> noiseProcessors;
    private final Map<UUID, AutomaticGainControl> agcProcessors;
    
    private final List<BiConsumer<UUID, VoiceState>> stateListeners;
    private final List<BiConsumer<UUID, VoiceChannel>> channelJoinListeners;
    private final List<BiConsumer<UUID, VoiceChannel>> channelLeaveListeners;
    private final List<BiConsumer<UUID, VoiceGroup>> groupJoinListeners;
    private final List<BiConsumer<UUID, VoiceGroup>> groupLeaveListeners;
    
    private volatile boolean enabled;
    
    public VoiceChatManager(RubidiumLogger logger, RubidiumScheduler scheduler) {
        this.logger = logger;
        this.scheduler = scheduler;
        this.config = VoiceConfig.defaults();
        this.playerStates = new ConcurrentHashMap<>();
        this.channels = new ConcurrentHashMap<>();
        this.playerChannels = new ConcurrentHashMap<>();
        this.groups = new ConcurrentHashMap<>();
        this.playerGroups = new ConcurrentHashMap<>();
        this.proximityManager = new ProximityManager(config);
        this.metrics = new VoiceMetrics();
        this.audioMixer = new AudioMixer();
        this.vadProcessors = new ConcurrentHashMap<>();
        this.noiseProcessors = new ConcurrentHashMap<>();
        this.agcProcessors = new ConcurrentHashMap<>();
        this.stateListeners = new ArrayList<>();
        this.channelJoinListeners = new ArrayList<>();
        this.channelLeaveListeners = new ArrayList<>();
        this.groupJoinListeners = new ArrayList<>();
        this.groupLeaveListeners = new ArrayList<>();
        this.enabled = false;
    }
    
    public void enable() {
        if (enabled) return;
        enabled = true;
        
        VoiceChannel global = createChannel("global", VoiceChannel.ChannelType.GLOBAL);
        global.getSettings().setPersistent(true);
        
        VoiceChannel proximity = createChannel("proximity", VoiceChannel.ChannelType.PROXIMITY);
        proximity.getSettings().setPersistent(true);
        
        audioMixer.initialize();
        
        if (config.enableVoiceRecording()) {
            recorder = new VoiceRecorder(
                Path.of("voice-recordings"),
                config.recordingRetention()
            );
        }
        
        scheduler.runTaskTimer("voice-chat", this::tick, 0, 1);
        scheduler.runTaskTimer("voice-cleanup", this::cleanupInactiveGroups, 0, 20 * 60);
        
        logger.info("Voice chat system enabled (Opus codec, {} Hz, {} kbps)",
            config.sampleRate(), config.bitrate() / 1000);
    }
    
    public void disable() {
        if (!enabled) return;
        enabled = false;
        
        playerStates.clear();
        channels.clear();
        playerChannels.clear();
        groups.clear();
        playerGroups.clear();
        vadProcessors.clear();
        noiseProcessors.clear();
        agcProcessors.clear();
        
        audioMixer.shutdown();
        if (recorder != null) {
            recorder.shutdown();
        }
        
        scheduler.cancelAllTasks("voice-chat");
        scheduler.cancelAllTasks("voice-cleanup");
        
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
    
    private void cleanupInactiveGroups() {
        long now = System.currentTimeMillis();
        long inactiveThreshold = 30 * 60 * 1000;
        
        groups.entrySet().removeIf(entry -> {
            VoiceGroup group = entry.getValue();
            if (!group.isPersistent() && 
                (group.isEmpty() || now - group.getLastActivity() > inactiveThreshold)) {
                logger.debug("Removed inactive voice group: {}", group.getName());
                return true;
            }
            return false;
        });
    }
    
    public void onPlayerJoin(Player player) {
        VoiceState state = new VoiceState(player.getId());
        var loc = player.getPosition();
        state.setPosition(new Vector3d(loc.x(), loc.y(), loc.z()));
        state.setActivationMode(config.defaultActivationMode());
        playerStates.put(player.getId(), state);
        playerGroups.put(player.getId(), ConcurrentHashMap.newKeySet());
        
        if (config.enableVAD()) {
            vadProcessors.put(player.getId(), new VoiceActivityDetector(config));
        }
        if (config.noiseSuppression() != VoiceConfig.NoiseSuppressionLevel.OFF) {
            noiseProcessors.put(player.getId(), new RNNoiseProcessor(config));
        }
        if (config.enableAGC()) {
            agcProcessors.put(player.getId(), new AutomaticGainControl(config));
        }
        
        audioMixer.createStream(player.getId());
        
        if (config.autoJoinGlobal()) {
            VoiceChannel global = channels.get("global");
            if (global != null) {
                joinChannel(player.getId(), global);
            }
        }
    }
    
    public void onPlayerQuit(Player player) {
        UUID playerId = player.getId();
        
        String channelId = playerChannels.remove(playerId);
        if (channelId != null) {
            VoiceChannel channel = channels.get(channelId);
            if (channel != null) {
                channel.removeMember(playerId);
                notifyChannelLeave(playerId, channel);
            }
        }
        
        Set<String> groupIds = playerGroups.remove(playerId);
        if (groupIds != null) {
            for (String groupId : groupIds) {
                VoiceGroup group = groups.get(groupId);
                if (group != null) {
                    group.removeMember(playerId);
                    notifyGroupLeave(playerId, group);
                }
            }
        }
        
        playerStates.remove(playerId);
        vadProcessors.remove(playerId);
        noiseProcessors.remove(playerId);
        agcProcessors.remove(playerId);
        audioMixer.destroyStream(playerId);
        
        if (recorder != null) {
            recorder.stopRecording(playerId);
        }
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
    
    public void setWhisperMode(UUID playerId, boolean whisper) {
        VoiceState state = playerStates.get(playerId);
        if (state != null && config.enableWhisperMode()) {
            state.setWhispering(whisper);
            notifyStateChange(playerId, state);
        }
    }
    
    public void setActivationMode(UUID playerId, VoiceConfig.ActivationMode mode) {
        VoiceState state = playerStates.get(playerId);
        if (state != null) {
            state.setActivationMode(mode);
            notifyStateChange(playerId, state);
        }
    }
    
    public void setPushToTalkActive(UUID playerId, boolean active) {
        VoiceState state = playerStates.get(playerId);
        if (state != null) {
            state.setPttActive(active);
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
    
    public void setPlayerVolume(UUID playerId, UUID targetId, float volume) {
        VoiceState state = playerStates.get(playerId);
        if (state != null) {
            state.setPlayerVolume(targetId, Math.max(0, Math.min(2.0f, volume)));
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
    
    public VoiceGroup createGroup(UUID ownerId, String name) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        VoiceGroup group = new VoiceGroup(id, name, ownerId);
        groups.put(id, group);
        
        Set<String> ownerGroups = playerGroups.get(ownerId);
        if (ownerGroups != null) {
            ownerGroups.add(id);
        }
        
        logger.debug("Created voice group: {} by {}", name, ownerId);
        return group;
    }
    
    public boolean joinGroup(UUID playerId, String groupId) {
        return joinGroup(playerId, groupId, null);
    }
    
    public boolean joinGroup(UUID playerId, String groupId, String password) {
        VoiceGroup group = groups.get(groupId);
        if (group == null) return false;
        
        Set<String> currentGroups = playerGroups.get(playerId);
        if (currentGroups == null) return false;
        
        if (currentGroups.size() >= config.maxGroupsPerPlayer()) {
            return false;
        }
        
        boolean joined = password != null ? 
            group.addMember(playerId, password) : 
            group.addMember(playerId);
        
        if (joined) {
            currentGroups.add(groupId);
            notifyGroupJoin(playerId, group);
        }
        
        return joined;
    }
    
    public void leaveGroup(UUID playerId, String groupId) {
        VoiceGroup group = groups.get(groupId);
        if (group == null) return;
        
        group.removeMember(playerId);
        
        Set<String> currentGroups = playerGroups.get(playerId);
        if (currentGroups != null) {
            currentGroups.remove(groupId);
        }
        
        notifyGroupLeave(playerId, group);
        
        if (group.isEmpty() && !group.isPersistent()) {
            groups.remove(groupId);
        }
    }
    
    public void routeAudio(UUID speaker, byte[] audioData) {
        VoiceState speakerState = playerStates.get(speaker);
        if (speakerState == null || speakerState.isMuted() || speakerState.isSelfMuted()) {
            return;
        }
        
        if (speakerState.getActivationMode() == VoiceConfig.ActivationMode.PUSH_TO_TALK && 
            !speakerState.isPttActive()) {
            return;
        }
        
        byte[] processedAudio = audioData;
        
        RNNoiseProcessor noiseProcessor = noiseProcessors.get(speaker);
        if (noiseProcessor != null) {
            processedAudio = noiseProcessor.process(processedAudio);
        }
        
        AutomaticGainControl agc = agcProcessors.get(speaker);
        if (agc != null) {
            processedAudio = agc.process(processedAudio, config.sampleRate());
        }
        
        VoiceActivityDetector vad = vadProcessors.get(speaker);
        if (vad != null && config.enableVAD()) {
            boolean hasVoice = vad.process(processedAudio);
            if (!hasVoice && speakerState.getActivationMode() == VoiceConfig.ActivationMode.VOICE_ACTIVATION) {
                return;
            }
        }
        
        speakerState.setSpeaking(true);
        speakerState.setLastVoiceActivity(System.currentTimeMillis());
        
        if (recorder != null && config.enableVoiceRecording()) {
            recorder.recordAudio(speaker, speaker, processedAudio, System.currentTimeMillis());
        }
        
        if (speakerState.isWhispering()) {
            routeWhisperAudio(speaker, speakerState, processedAudio);
        } else {
            String channelId = playerChannels.get(speaker);
            VoiceChannel channel = channelId != null ? channels.get(channelId) : null;
            
            if (channel != null && channel.getType() == VoiceChannel.ChannelType.PROXIMITY) {
                routeProximityAudio(speaker, speakerState, processedAudio);
            } else if (channel != null) {
                routeChannelAudio(speaker, channel, processedAudio);
            }
            
            routeGroupAudio(speaker, processedAudio);
        }
        
        metrics.recordPacketSent();
    }
    
    private void routeWhisperAudio(UUID speaker, VoiceState speakerState, byte[] audioData) {
        double whisperRange = config.whisperRange();
        
        for (var entry : playerStates.entrySet()) {
            UUID listenerId = entry.getKey();
            if (listenerId.equals(speaker)) continue;
            
            VoiceState listenerState = entry.getValue();
            if (listenerState.isDeafened() || listenerState.isSelfDeafened()) continue;
            
            double distance = speakerState.getPosition().distance(listenerState.getPosition());
            
            if (distance <= whisperRange) {
                double volume = 1.0 - (distance / whisperRange) * 0.5;
                sendAudioToPlayer(listenerId, speaker, audioData, (float) volume, true);
            }
        }
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
                float playerVolumeModifier = listenerState.getPlayerVolume(speaker);
                sendAudioToPlayer(listenerId, speaker, audioData, (float) volume * playerVolumeModifier, false);
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
            
            float playerVolumeModifier = listenerState.getPlayerVolume(speaker);
            sendAudioToPlayer(member, speaker, audioData, playerVolumeModifier, false);
        }
    }
    
    private void routeGroupAudio(UUID speaker, byte[] audioData) {
        Set<String> speakerGroups = playerGroups.get(speaker);
        if (speakerGroups == null || speakerGroups.isEmpty()) return;
        
        Set<UUID> sentTo = new HashSet<>();
        sentTo.add(speaker);
        
        for (String groupId : speakerGroups) {
            VoiceGroup group = groups.get(groupId);
            if (group == null) continue;
            
            VoiceGroup.MemberSettings speakerSettings = group.getMemberSettings(speaker);
            if (speakerSettings != null && speakerSettings.isMuted()) continue;
            
            for (UUID member : group.getMembers()) {
                if (sentTo.contains(member)) continue;
                sentTo.add(member);
                
                VoiceState listenerState = playerStates.get(member);
                if (listenerState == null || listenerState.isDeafened() || listenerState.isSelfDeafened()) {
                    continue;
                }
                
                VoiceGroup.MemberSettings memberSettings = group.getMemberSettings(member);
                float groupVolume = memberSettings != null ? memberSettings.getVolume() : 1.0f;
                float playerVolume = listenerState.getPlayerVolume(speaker);
                
                sendAudioToPlayer(member, speaker, audioData, groupVolume * playerVolume, false);
            }
        }
    }
    
    private void sendAudioToPlayer(UUID listener, UUID speaker, byte[] audioData, float volume, boolean whisper) {
        VoiceState listenerState = playerStates.get(listener);
        VoiceState speakerState = playerStates.get(speaker);
        
        if (listenerState == null || speakerState == null) return;
        
        byte[] processedAudio = audioData;
        
        if (config.enable3DAudio() && !whisper) {
            var spatial = proximityManager.calculateSpatialAudio(
                speakerState.getPosition(),
                listenerState.getPosition(),
                listenerState.getLookDirection()
            );
            
            processedAudio = audioMixer.applySpatialAudio(audioData, new AudioMixer.SpatialParams(
                speakerState.getPosition().x() - listenerState.getPosition().x(),
                speakerState.getPosition().y() - listenerState.getPosition().y(),
                speakerState.getPosition().z() - listenerState.getPosition().z(),
                spatial.volume(),
                volume
            ));
        }
        
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
    
    public Optional<VoiceGroup> getGroup(String groupId) {
        return Optional.ofNullable(groups.get(groupId));
    }
    
    public List<VoiceGroup> getPlayerGroups(UUID playerId) {
        Set<String> groupIds = playerGroups.get(playerId);
        if (groupIds == null) return Collections.emptyList();
        
        List<VoiceGroup> result = new ArrayList<>();
        for (String id : groupIds) {
            VoiceGroup group = groups.get(id);
            if (group != null) {
                result.add(group);
            }
        }
        return result;
    }
    
    public List<VoiceGroup> getPublicGroups() {
        return groups.values().stream()
            .filter(VoiceGroup::isPublic)
            .toList();
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
    
    public void onGroupJoin(BiConsumer<UUID, VoiceGroup> listener) {
        groupJoinListeners.add(listener);
    }
    
    public void onGroupLeave(BiConsumer<UUID, VoiceGroup> listener) {
        groupLeaveListeners.add(listener);
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
    
    private void notifyGroupJoin(UUID playerId, VoiceGroup group) {
        for (var listener : groupJoinListeners) {
            listener.accept(playerId, group);
        }
    }
    
    private void notifyGroupLeave(UUID playerId, VoiceGroup group) {
        for (var listener : groupLeaveListeners) {
            listener.accept(playerId, group);
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
    
    public AudioMixer getAudioMixer() {
        return audioMixer;
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
        
        public Vector3d subtract(Vector3d other) {
            return new Vector3d(x - other.x, y - other.y, z - other.z);
        }
        
        public Vector3d normalize() {
            double len = Math.sqrt(x * x + y * y + z * z);
            if (len < 0.0001) return new Vector3d(0, 0, 1);
            return new Vector3d(x / len, y / len, z / len);
        }
    }
}
