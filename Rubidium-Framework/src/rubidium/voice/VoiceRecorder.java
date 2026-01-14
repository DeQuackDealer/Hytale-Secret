package rubidium.voice;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class VoiceRecorder {
    
    private static final Logger logger = Logger.getLogger("Rubidium-VoiceRecorder");
    
    private final Path recordingsDir;
    private final Duration retentionPeriod;
    private final ExecutorService writeExecutor;
    
    private final Map<UUID, RecordingSession> activeSessions = new ConcurrentHashMap<>();
    
    private volatile boolean enabled = true;
    
    public VoiceRecorder(Path recordingsDir, Duration retentionPeriod) {
        this.recordingsDir = recordingsDir;
        this.retentionPeriod = retentionPeriod;
        this.writeExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "VoiceRecorder-Writer");
            t.setDaemon(true);
            return t;
        });
        
        try {
            Files.createDirectories(recordingsDir);
        } catch (IOException e) {
            logger.warning("Failed to create recordings directory: " + e.getMessage());
        }
    }
    
    public void startRecording(UUID playerId, String channelId) {
        if (!enabled) return;
        
        String sessionId = generateSessionId(playerId);
        Path sessionPath = recordingsDir.resolve(sessionId);
        
        try {
            Files.createDirectories(sessionPath);
            RecordingSession session = new RecordingSession(sessionId, playerId, channelId, sessionPath);
            activeSessions.put(playerId, session);
            logger.info("Started voice recording for player " + playerId);
        } catch (IOException e) {
            logger.warning("Failed to start recording: " + e.getMessage());
        }
    }
    
    public void stopRecording(UUID playerId) {
        RecordingSession session = activeSessions.remove(playerId);
        if (session != null) {
            writeExecutor.submit(() -> {
                try {
                    session.complete();
                    logger.info("Stopped voice recording for player " + playerId);
                } catch (Exception e) {
                    logger.warning("Failed to finalize recording: " + e.getMessage());
                }
            });
        }
    }
    
    public void recordAudio(UUID speakerId, UUID listenerId, byte[] audioData, long timestamp) {
        RecordingSession session = activeSessions.get(listenerId);
        if (session == null) {
            session = activeSessions.get(speakerId);
        }
        
        if (session != null) {
            session.addFrame(new AudioFrame(speakerId, audioData, timestamp));
        }
    }
    
    public void cleanOldRecordings() {
        if (!Files.exists(recordingsDir)) return;
        
        Instant cutoff = Instant.now().minus(retentionPeriod);
        
        try (var stream = Files.list(recordingsDir)) {
            stream.filter(Files::isDirectory)
                  .forEach(dir -> {
                      try {
                          Instant modified = Files.getLastModifiedTime(dir).toInstant();
                          if (modified.isBefore(cutoff)) {
                              deleteDirectory(dir);
                              logger.info("Deleted old recording: " + dir.getFileName());
                          }
                      } catch (IOException e) {
                          logger.warning("Failed to check recording age: " + e.getMessage());
                      }
                  });
        } catch (IOException e) {
            logger.warning("Failed to list recordings: " + e.getMessage());
        }
    }
    
    private void deleteDirectory(Path dir) throws IOException {
        if (Files.isDirectory(dir)) {
            try (var entries = Files.list(dir)) {
                for (Path entry : entries.toList()) {
                    deleteDirectory(entry);
                }
            }
        }
        Files.deleteIfExists(dir);
    }
    
    public List<RecordingInfo> getRecordings(UUID playerId) {
        List<RecordingInfo> recordings = new ArrayList<>();
        
        try (var stream = Files.list(recordingsDir)) {
            stream.filter(Files::isDirectory)
                  .filter(dir -> dir.getFileName().toString().contains(playerId.toString().substring(0, 8)))
                  .forEach(dir -> {
                      try {
                          Path metaFile = dir.resolve("meta.json");
                          if (Files.exists(metaFile)) {
                              recordings.add(new RecordingInfo(
                                  dir.getFileName().toString(),
                                  playerId,
                                  Files.getLastModifiedTime(dir).toInstant(),
                                  Files.size(dir.resolve("audio.raw"))
                              ));
                          }
                      } catch (IOException ignored) {}
                  });
        } catch (IOException e) {
            logger.warning("Failed to list recordings: " + e.getMessage());
        }
        
        return recordings;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public void shutdown() {
        for (UUID playerId : activeSessions.keySet()) {
            stopRecording(playerId);
        }
        writeExecutor.shutdown();
        try {
            if (!writeExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                writeExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            writeExecutor.shutdownNow();
        }
    }
    
    private String generateSessionId(UUID playerId) {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .format(LocalDateTime.now());
        return playerId.toString().substring(0, 8) + "_" + timestamp;
    }
    
    public record AudioFrame(UUID speakerId, byte[] data, long timestamp) {}
    
    public record RecordingInfo(String id, UUID playerId, Instant createdAt, long sizeBytes) {}
    
    private static class RecordingSession {
        private final String id;
        private final UUID playerId;
        private final String channelId;
        private final Path sessionPath;
        private final Queue<AudioFrame> frameBuffer = new ConcurrentLinkedQueue<>();
        private final long startTime;
        private volatile boolean active = true;
        
        RecordingSession(String id, UUID playerId, String channelId, Path sessionPath) {
            this.id = id;
            this.playerId = playerId;
            this.channelId = channelId;
            this.sessionPath = sessionPath;
            this.startTime = System.currentTimeMillis();
        }
        
        void addFrame(AudioFrame frame) {
            if (active) {
                frameBuffer.offer(frame);
            }
        }
        
        void complete() throws IOException {
            active = false;
            
            Path audioFile = sessionPath.resolve("audio.raw");
            try (OutputStream out = Files.newOutputStream(audioFile)) {
                AudioFrame frame;
                while ((frame = frameBuffer.poll()) != null) {
                    out.write(frame.data);
                }
            }
            
            Path metaFile = sessionPath.resolve("meta.json");
            String meta = String.format(
                "{\"id\":\"%s\",\"playerId\":\"%s\",\"channelId\":\"%s\",\"startTime\":%d,\"endTime\":%d}",
                id, playerId, channelId, startTime, System.currentTimeMillis()
            );
            Files.writeString(metaFile, meta);
        }
    }
}
