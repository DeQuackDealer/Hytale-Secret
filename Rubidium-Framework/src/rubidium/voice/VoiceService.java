package rubidium.voice;

import rubidium.core.feature.*;
import rubidium.hytale.api.player.Player;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Main voice chat service with 3D spatial audio, channels, and moderation.
 */
public class VoiceService implements FeatureLifecycle {
    
    private static final Logger logger = Logger.getLogger("Rubidium-Voice");
    
    private static VoiceService instance;
    
    private final VoiceSessionManager sessionManager;
    private final AudioMixer audioMixer;
    private final VoiceChannelManager channelManager;
    private final VoiceModerationService moderationService;
    private final VoiceSettings defaultSettings;
    
    private final Map<UUID, VoiceSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, VoicePlayerSettings> playerSettings = new ConcurrentHashMap<>();
    
    private volatile boolean running = false;
    
    public VoiceService() {
        this.sessionManager = new VoiceSessionManager(this);
        this.audioMixer = new AudioMixer();
        this.channelManager = new VoiceChannelManager();
        this.moderationService = new VoiceModerationService();
        this.defaultSettings = VoiceSettings.defaults();
    }
    
    public static VoiceService getInstance() {
        if (instance == null) {
            instance = new VoiceService();
        }
        return instance;
    }
    
    @Override
    public String getFeatureId() { return "voice"; }
    
    @Override
    public String getFeatureName() { return "Voice Chat"; }
    
    @Override
    public void initialize() throws FeatureInitException {
        logger.info("Initializing Voice Service...");
        
        try {
            channelManager.createChannel("global", VoiceChannel.Type.GLOBAL);
            channelManager.createChannel("proximity", VoiceChannel.Type.PROXIMITY);
            
            audioMixer.initialize();
            moderationService.initialize();
            
        } catch (Exception e) {
            throw new FeatureInitException("Failed to initialize voice service", e);
        }
    }
    
    @Override
    public void start() {
        running = true;
        sessionManager.start();
        logger.info("Voice Service started");
    }
    
    @Override
    public void stop() {
        running = false;
        sessionManager.stop();
        activeSessions.values().forEach(VoiceSession::disconnect);
        activeSessions.clear();
        logger.info("Voice Service stopped");
    }
    
    @Override
    public void shutdown() {
        stop();
        audioMixer.shutdown();
    }
    
    @Override
    public FeatureHealth healthCheck() {
        if (!running) {
            return FeatureHealth.disabled("Voice service not running");
        }
        
        int activeSessCount = activeSessions.size();
        int channelCount = channelManager.getChannelCount();
        
        if (audioMixer.getProcessingLoad() > 0.9) {
            return FeatureHealth.degraded("High audio processing load", List.of(
                new FeatureHealth.HealthIssue(
                    FeatureHealth.HealthIssue.Severity.WARNING,
                    "HIGH_LOAD",
                    "Audio processing at " + (int)(audioMixer.getProcessingLoad() * 100) + "%",
                    java.time.Instant.now()
                )
            ));
        }
        
        return FeatureHealth.healthy()
            .withMetric("activeSessions", activeSessCount)
            .withMetric("channels", channelCount);
    }
    
    public VoiceSession connectPlayer(Player player) {
        if (!running) return null;
        
        UUID playerId = player.getUuid();
        
        if (activeSessions.containsKey(playerId)) {
            return activeSessions.get(playerId);
        }
        
        VoicePlayerSettings settings = playerSettings.computeIfAbsent(
            playerId, k -> VoicePlayerSettings.defaults()
        );
        
        VoiceSession session = sessionManager.createSession(player, settings);
        activeSessions.put(playerId, session);
        
        channelManager.getChannel("proximity").ifPresent(session::joinChannel);
        
        logger.fine("Player " + player.getUsername() + " connected to voice");
        return session;
    }
    
    public void disconnectPlayer(UUID playerId) {
        VoiceSession session = activeSessions.remove(playerId);
        if (session != null) {
            session.disconnect();
            logger.fine("Player disconnected from voice: " + playerId);
        }
    }
    
    public Optional<VoiceSession> getSession(UUID playerId) {
        return Optional.ofNullable(activeSessions.get(playerId));
    }
    
    public void setPlayerSettings(UUID playerId, VoicePlayerSettings settings) {
        playerSettings.put(playerId, settings);
        VoiceSession session = activeSessions.get(playerId);
        if (session != null) {
            session.updateSettings(settings);
        }
    }
    
    public VoicePlayerSettings getPlayerSettings(UUID playerId) {
        return playerSettings.getOrDefault(playerId, VoicePlayerSettings.defaults());
    }
    
    public void mutePlayer(UUID playerId, boolean muted) {
        VoiceSession session = activeSessions.get(playerId);
        if (session != null) {
            session.setMuted(muted);
        }
    }
    
    public void deafenPlayer(UUID playerId, boolean deafened) {
        VoiceSession session = activeSessions.get(playerId);
        if (session != null) {
            session.setDeafened(deafened);
        }
    }
    
    public void setPlayerVolume(UUID targetId, UUID forPlayerId, float volume) {
        VoiceSession session = activeSessions.get(forPlayerId);
        if (session != null) {
            session.setVolumeForPlayer(targetId, volume);
        }
    }
    
    public VoiceChannelManager getChannelManager() {
        return channelManager;
    }
    
    public AudioMixer getAudioMixer() {
        return audioMixer;
    }
    
    public VoiceModerationService getModerationService() {
        return moderationService;
    }
    
    public int getActiveSessionCount() {
        return activeSessions.size();
    }
}
