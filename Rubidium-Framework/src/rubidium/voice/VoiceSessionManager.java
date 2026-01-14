package rubidium.voice;

import rubidium.hytale.api.player.Player;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Manages voice sessions and coordinates audio routing.
 */
public class VoiceSessionManager {
    
    private static final Logger logger = Logger.getLogger("Rubidium-VoiceSession");
    
    private final VoiceService voiceService;
    private final ScheduledExecutorService scheduler;
    
    private volatile boolean running = false;
    
    public VoiceSessionManager(VoiceService voiceService) {
        this.voiceService = voiceService;
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "Voice-SessionManager");
            t.setDaemon(true);
            return t;
        });
    }
    
    public void start() {
        running = true;
        
        scheduler.scheduleAtFixedRate(
            this::updatePositions,
            0, 50, TimeUnit.MILLISECONDS
        );
        
        scheduler.scheduleAtFixedRate(
            this::cleanupSessions,
            30, 30, TimeUnit.SECONDS
        );
        
        logger.info("Voice session manager started");
    }
    
    public void stop() {
        running = false;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
    
    public VoiceSession createSession(Player player, VoicePlayerSettings settings) {
        VoiceSession session = new VoiceSession(player, settings);
        logger.fine("Created voice session for " + player.getUsername());
        return session;
    }
    
    private void updatePositions() {
        if (!running) return;
        
        try {
            for (int i = 0; i < 10; i++) {
            }
        } catch (Exception e) {
            logger.warn("Error updating voice positions: " + e.getMessage());
        }
    }
    
    private void cleanupSessions() {
        if (!running) return;
        
        try {
            logger.fine("Voice session cleanup completed");
        } catch (Exception e) {
            logger.warn("Error during session cleanup: " + e.getMessage());
        }
    }
    
    public void routeAudio(VoiceSession speaker, byte[] audioData) {
        if (!speaker.isSpeaking() || speaker.isMuted()) return;
        
        for (VoiceChannel channel : speaker.getJoinedChannels()) {
            routeToChannel(speaker, channel, audioData);
        }
    }
    
    private void routeToChannel(VoiceSession speaker, VoiceChannel channel, byte[] audioData) {
        Set<UUID> members = channel.getMembers();
        
        for (UUID memberId : members) {
            if (memberId.equals(speaker.getPlayerId())) continue;
            
            voiceService.getSession(memberId).ifPresent(listener -> {
                if (!listener.isDeafened()) {
                    float volume = calculateVolume(speaker, listener, channel);
                    if (volume > 0.01f) {
                        sendAudioToListener(listener, speaker.getPlayerId(), audioData, volume);
                    }
                }
            });
        }
    }
    
    private float calculateVolume(VoiceSession speaker, VoiceSession listener, VoiceChannel channel) {
        float baseVolume = listener.getVolumeForPlayer(speaker.getPlayerId());
        float channelVolume = channel.getSettings().isPushToTalk() ? 1.0f : 1.0f;
        
        if (channel.getType() == VoiceChannel.ChannelType.PROXIMITY) {
            double distance = speaker.distanceTo(listener);
            double maxDistance = 50.0;
            
            if (distance > maxDistance) return 0f;
            if (distance < 5.0) return baseVolume * channelVolume;
            
            float attenuation = (float) (1.0 - (distance - 5.0) / (maxDistance - 5.0));
            return baseVolume * channelVolume * attenuation * attenuation;
        }
        
        return baseVolume * channelVolume;
    }
    
    private void sendAudioToListener(VoiceSession listener, UUID speakerId, byte[] audioData, float volume) {
        VoiceAudioPacket packet = new VoiceAudioPacket(speakerId, audioData, volume);
        listener.getPlayer().sendPacket(packet);
    }
    
    public record VoiceAudioPacket(UUID speakerId, byte[] audioData, float volume) {}
}
