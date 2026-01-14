package rubidium.voice;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Voice moderation service for admin controls and recording.
 */
public class VoiceModerationService {
    
    private static final Logger logger = Logger.getLogger("Rubidium-VoiceModeration");
    
    private final Map<UUID, ModerationRecord> moderationRecords = new ConcurrentHashMap<>();
    private final Map<UUID, VoiceMute> activeMutes = new ConcurrentHashMap<>();
    private final Set<UUID> monitoredPlayers = ConcurrentHashMap.newKeySet();
    private final Map<String, VoiceRecording> activeRecordings = new ConcurrentHashMap<>();
    
    private final ScheduledExecutorService scheduler;
    
    public VoiceModerationService() {
        this.scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "VoiceModeration");
            t.setDaemon(true);
            return t;
        });
    }
    
    public void initialize() {
        scheduler.scheduleAtFixedRate(this::cleanupExpiredMutes, 1, 1, TimeUnit.MINUTES);
        logger.info("Voice moderation service initialized");
    }
    
    public void mutePlayer(UUID playerId, UUID moderatorId, String reason, long durationSeconds) {
        Instant expiry = durationSeconds > 0 
            ? Instant.now().plusSeconds(durationSeconds) 
            : Instant.MAX;
            
        VoiceMute mute = new VoiceMute(playerId, moderatorId, reason, Instant.now(), expiry);
        activeMutes.put(playerId, mute);
        
        addRecord(playerId, new ModerationAction(
            ModerationAction.Type.MUTE,
            moderatorId,
            reason,
            Instant.now(),
            durationSeconds
        ));
        
        logger.info("Voice muted: " + playerId + " by " + moderatorId + " - " + reason);
    }
    
    public void unmutePlayer(UUID playerId, UUID moderatorId) {
        VoiceMute removed = activeMutes.remove(playerId);
        if (removed != null) {
            addRecord(playerId, new ModerationAction(
                ModerationAction.Type.UNMUTE,
                moderatorId,
                "Unmuted",
                Instant.now(),
                0
            ));
            logger.info("Voice unmuted: " + playerId + " by " + moderatorId);
        }
    }
    
    public boolean isPlayerMuted(UUID playerId) {
        VoiceMute mute = activeMutes.get(playerId);
        if (mute == null) return false;
        
        if (mute.expiry().isBefore(Instant.now())) {
            activeMutes.remove(playerId);
            return false;
        }
        
        return true;
    }
    
    public Optional<VoiceMute> getActiveMute(UUID playerId) {
        return Optional.ofNullable(activeMutes.get(playerId));
    }
    
    public void startMonitoring(UUID playerId, UUID moderatorId) {
        monitoredPlayers.add(playerId);
        logger.info("Started monitoring voice for: " + playerId + " by " + moderatorId);
    }
    
    public void stopMonitoring(UUID playerId) {
        monitoredPlayers.remove(playerId);
        logger.info("Stopped monitoring voice for: " + playerId);
    }
    
    public boolean isBeingMonitored(UUID playerId) {
        return monitoredPlayers.contains(playerId);
    }
    
    public String startRecording(String channelId, UUID moderatorId, int maxDurationSeconds) {
        String recordingId = UUID.randomUUID().toString();
        
        VoiceRecording recording = new VoiceRecording(
            recordingId,
            channelId,
            moderatorId,
            Instant.now(),
            maxDurationSeconds
        );
        
        activeRecordings.put(recordingId, recording);
        logger.info("Started voice recording: " + recordingId + " for channel " + channelId);
        
        return recordingId;
    }
    
    public void stopRecording(String recordingId) {
        VoiceRecording recording = activeRecordings.remove(recordingId);
        if (recording != null) {
            recording.finalize();
            logger.info("Stopped voice recording: " + recordingId);
        }
    }
    
    public void reportPlayer(UUID reporterId, UUID reportedId, String reason) {
        addRecord(reportedId, new ModerationAction(
            ModerationAction.Type.REPORT,
            reporterId,
            reason,
            Instant.now(),
            0
        ));
        logger.info("Voice report: " + reportedId + " by " + reporterId + " - " + reason);
    }
    
    public List<ModerationAction> getPlayerHistory(UUID playerId) {
        ModerationRecord record = moderationRecords.get(playerId);
        return record != null ? record.getActions() : List.of();
    }
    
    private void addRecord(UUID playerId, ModerationAction action) {
        moderationRecords.computeIfAbsent(playerId, k -> new ModerationRecord(playerId))
            .addAction(action);
    }
    
    private void cleanupExpiredMutes() {
        Instant now = Instant.now();
        activeMutes.entrySet().removeIf(e -> e.getValue().expiry().isBefore(now));
    }
    
    public record VoiceMute(
        UUID playerId,
        UUID moderatorId,
        String reason,
        Instant issuedAt,
        Instant expiry
    ) {}
    
    public record ModerationAction(
        Type type,
        UUID moderatorId,
        String reason,
        Instant timestamp,
        long durationSeconds
    ) {
        public enum Type {
            MUTE, UNMUTE, WARN, REPORT, BAN, UNBAN
        }
    }
    
    private static class ModerationRecord {
        private final UUID playerId;
        private final List<ModerationAction> actions = new CopyOnWriteArrayList<>();
        
        ModerationRecord(UUID playerId) {
            this.playerId = playerId;
        }
        
        void addAction(ModerationAction action) {
            actions.add(action);
        }
        
        List<ModerationAction> getActions() {
            return Collections.unmodifiableList(actions);
        }
    }
    
    private static class VoiceRecording {
        private final String id;
        private final String channelId;
        private final UUID moderatorId;
        private final Instant startedAt;
        private final int maxDurationSeconds;
        private final List<byte[]> audioFrames = new CopyOnWriteArrayList<>();
        
        VoiceRecording(String id, String channelId, UUID moderatorId, Instant startedAt, int maxDurationSeconds) {
            this.id = id;
            this.channelId = channelId;
            this.moderatorId = moderatorId;
            this.startedAt = startedAt;
            this.maxDurationSeconds = maxDurationSeconds;
        }
        
        void addFrame(byte[] frame) {
            if (audioFrames.size() < maxDurationSeconds * 50) {
                audioFrames.add(frame);
            }
        }
        
        void finalize() {
        }
    }
}
