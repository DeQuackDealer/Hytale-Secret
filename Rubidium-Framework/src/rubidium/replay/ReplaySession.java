package rubidium.replay;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ReplaySession {
    
    public enum TriggerType {
        MANUAL,
        ANTICHEAT,
        COMBAT,
        MOVEMENT,
        CONTINUOUS,
        REPORT
    }
    
    public record SessionMetadata(
        UUID sessionId,
        UUID playerId,
        String playerName,
        TriggerType triggerType,
        String triggerReason,
        Instant startTime,
        Instant endTime,
        int totalFrames,
        int segmentCount,
        long fileSizeBytes
    ) {}
    
    private final UUID sessionId;
    private final UUID playerId;
    private final String playerName;
    private final TriggerType triggerType;
    private final String triggerReason;
    private final Instant startTime;
    
    private final List<ReplaySegment> segments;
    private final Path storageDir;
    
    private Instant endTime;
    private boolean finalized;
    private int totalFrames;
    private long totalBytes;
    
    public ReplaySession(UUID playerId, String playerName, TriggerType triggerType, 
                          String triggerReason, Path storageDir) {
        this.sessionId = UUID.randomUUID();
        this.playerId = playerId;
        this.playerName = playerName;
        this.triggerType = triggerType;
        this.triggerReason = triggerReason;
        this.startTime = Instant.now();
        this.storageDir = storageDir;
        this.segments = new CopyOnWriteArrayList<>();
        this.finalized = false;
    }
    
    public void addSegment(ReplaySegment segment) {
        if (finalized) {
            throw new IllegalStateException("Session is finalized");
        }
        segments.add(segment);
        totalFrames += segment.getFrameCount();
    }
    
    public void finalize() {
        if (finalized) return;
        
        this.endTime = Instant.now();
        this.finalized = true;
        
        try {
            saveToDisk();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save replay session", e);
        }
    }
    
    private void saveToDisk() throws IOException {
        Path sessionDir = storageDir.resolve(sessionId.toString());
        Files.createDirectories(sessionDir);
        
        for (int i = 0; i < segments.size(); i++) {
            byte[] segmentData = segments.get(i).serialize();
            Path segmentFile = sessionDir.resolve(String.format("segment_%04d.rbx", i));
            Files.write(segmentFile, segmentData);
            totalBytes += segmentData.length;
        }
        
        saveMetadata(sessionDir);
    }
    
    private void saveMetadata(Path sessionDir) throws IOException {
        Properties props = new Properties();
        props.setProperty("sessionId", sessionId.toString());
        props.setProperty("playerId", playerId.toString());
        props.setProperty("playerName", playerName);
        props.setProperty("triggerType", triggerType.name());
        props.setProperty("triggerReason", triggerReason != null ? triggerReason : "");
        props.setProperty("startTime", startTime.toString());
        props.setProperty("endTime", endTime != null ? endTime.toString() : "");
        props.setProperty("totalFrames", String.valueOf(totalFrames));
        props.setProperty("segmentCount", String.valueOf(segments.size()));
        props.setProperty("totalBytes", String.valueOf(totalBytes));
        
        Path metadataFile = sessionDir.resolve("session.properties");
        try (OutputStream os = Files.newOutputStream(metadataFile)) {
            props.store(os, "Rubidium Replay Session");
        }
    }
    
    public static ReplaySession load(Path sessionDir) throws IOException {
        Path metadataFile = sessionDir.resolve("session.properties");
        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(metadataFile)) {
            props.load(is);
        }
        
        UUID sessionId = UUID.fromString(props.getProperty("sessionId"));
        UUID playerId = UUID.fromString(props.getProperty("playerId"));
        String playerName = props.getProperty("playerName");
        TriggerType triggerType = TriggerType.valueOf(props.getProperty("triggerType"));
        String triggerReason = props.getProperty("triggerReason");
        
        ReplaySession session = new ReplaySession(playerId, playerName, triggerType, 
            triggerReason, sessionDir.getParent());
        
        try (var stream = Files.list(sessionDir)) {
            stream.filter(p -> p.toString().endsWith(".rbx"))
                  .sorted()
                  .forEach(segmentPath -> {
                      try {
                          byte[] data = Files.readAllBytes(segmentPath);
                          ReplaySegment segment = ReplaySegment.deserialize(data);
                          session.segments.add(segment);
                          session.totalFrames += segment.getFrameCount();
                          session.totalBytes += data.length;
                      } catch (IOException e) {
                          throw new UncheckedIOException(e);
                      }
                  });
        }
        
        session.endTime = Instant.parse(props.getProperty("endTime"));
        session.finalized = true;
        
        return session;
    }
    
    public SessionMetadata getMetadata() {
        return new SessionMetadata(
            sessionId, playerId, playerName, triggerType, triggerReason,
            startTime, endTime, totalFrames, segments.size(), totalBytes
        );
    }
    
    public ReplayFrame getFrame(int globalIndex) {
        int remaining = globalIndex;
        for (ReplaySegment segment : segments) {
            if (remaining < segment.getFrameCount()) {
                return segment.reconstructFrame(remaining);
            }
            remaining -= segment.getFrameCount();
        }
        throw new IndexOutOfBoundsException("Frame index: " + globalIndex);
    }
    
    public ReplayFrame getFrameAtTime(long timestamp) {
        int frameIndex = 0;
        for (ReplaySegment segment : segments) {
            if (timestamp >= segment.getStartTimestamp() && 
                timestamp <= segment.getEndTimestamp()) {
                
                long segmentDuration = segment.getEndTimestamp() - segment.getStartTimestamp();
                if (segmentDuration == 0) {
                    return segment.reconstructFrame(0);
                }
                
                double progress = (double)(timestamp - segment.getStartTimestamp()) / segmentDuration;
                int localIndex = (int)(progress * (segment.getFrameCount() - 1));
                return segment.reconstructFrame(localIndex);
            }
            frameIndex += segment.getFrameCount();
        }
        
        if (!segments.isEmpty()) {
            ReplaySegment lastSegment = segments.get(segments.size() - 1);
            return lastSegment.reconstructFrame(lastSegment.getFrameCount() - 1);
        }
        
        return null;
    }
    
    public UUID getSessionId() { return sessionId; }
    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public TriggerType getTriggerType() { return triggerType; }
    public String getTriggerReason() { return triggerReason; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public int getTotalFrames() { return totalFrames; }
    public int getSegmentCount() { return segments.size(); }
    public long getTotalBytes() { return totalBytes; }
    public boolean isFinalized() { return finalized; }
    public List<ReplaySegment> getSegments() { return Collections.unmodifiableList(segments); }
}
