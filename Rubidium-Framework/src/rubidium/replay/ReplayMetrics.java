package rubidium.replay;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public final class ReplayMetrics {
    
    private final LongAdder framesCaptured = new LongAdder();
    private final LongAdder framesDropped = new LongAdder();
    private final LongAdder bytesWritten = new LongAdder();
    private final LongAdder bytesCompressed = new LongAdder();
    private final LongAdder sessionsStarted = new LongAdder();
    private final LongAdder sessionsCompleted = new LongAdder();
    private final LongAdder sessionsPruned = new LongAdder();
    
    private final AtomicLong captureLatencyNs = new AtomicLong();
    private final AtomicLong compressionLatencyNs = new AtomicLong();
    private final AtomicLong writeLatencyNs = new AtomicLong();
    
    private final LongAdder captureTimeSamples = new LongAdder();
    private final LongAdder captureTimeTotal = new LongAdder();
    
    private final AtomicLong lastResetTime = new AtomicLong(System.currentTimeMillis());
    
    public void recordFrameCapture() {
        framesCaptured.increment();
    }
    
    public void recordFrameDropped() {
        framesDropped.increment();
    }
    
    public void recordBytesWritten(long bytes) {
        bytesWritten.add(bytes);
    }
    
    public void recordBytesCompressed(long uncompressed, long compressed) {
        bytesWritten.add(uncompressed);
        bytesCompressed.add(compressed);
    }
    
    public void recordSessionStarted() {
        sessionsStarted.increment();
    }
    
    public void recordSessionCompleted() {
        sessionsCompleted.increment();
    }
    
    public void recordSessionPruned() {
        sessionsPruned.increment();
    }
    
    public void recordCaptureTime(long nanoseconds) {
        captureTimeTotal.add(nanoseconds);
        captureTimeSamples.increment();
        captureLatencyNs.set(nanoseconds);
    }
    
    public void recordCompressionLatency(long nanoseconds) {
        compressionLatencyNs.set(nanoseconds);
    }
    
    public void recordWriteLatency(long nanoseconds) {
        writeLatencyNs.set(nanoseconds);
    }
    
    public long getFramesCaptured() {
        return framesCaptured.sum();
    }
    
    public long getFramesDropped() {
        return framesDropped.sum();
    }
    
    public long getBytesWritten() {
        return bytesWritten.sum();
    }
    
    public long getBytesCompressed() {
        return bytesCompressed.sum();
    }
    
    public double getCompressionRatio() {
        long written = bytesWritten.sum();
        long compressed = bytesCompressed.sum();
        if (compressed == 0) return 1.0;
        return (double) written / compressed;
    }
    
    public long getSessionsStarted() {
        return sessionsStarted.sum();
    }
    
    public long getSessionsCompleted() {
        return sessionsCompleted.sum();
    }
    
    public long getSessionsPruned() {
        return sessionsPruned.sum();
    }
    
    public long getActiveSessions() {
        return sessionsStarted.sum() - sessionsCompleted.sum();
    }
    
    public double getAverageCaptureTimeMs() {
        long samples = captureTimeSamples.sum();
        if (samples == 0) return 0;
        return (captureTimeTotal.sum() / samples) / 1_000_000.0;
    }
    
    public double getLastCaptureLatencyMs() {
        return captureLatencyNs.get() / 1_000_000.0;
    }
    
    public double getLastCompressionLatencyMs() {
        return compressionLatencyNs.get() / 1_000_000.0;
    }
    
    public double getLastWriteLatencyMs() {
        return writeLatencyNs.get() / 1_000_000.0;
    }
    
    public double getFramesPerSecond() {
        long elapsed = System.currentTimeMillis() - lastResetTime.get();
        if (elapsed < 1000) return 0;
        return (framesCaptured.sum() * 1000.0) / elapsed;
    }
    
    public double getDropRate() {
        long captured = framesCaptured.sum();
        long dropped = framesDropped.sum();
        long total = captured + dropped;
        if (total == 0) return 0;
        return (double) dropped / total;
    }
    
    public void reset() {
        framesCaptured.reset();
        framesDropped.reset();
        bytesWritten.reset();
        bytesCompressed.reset();
        sessionsStarted.reset();
        sessionsCompleted.reset();
        sessionsPruned.reset();
        captureTimeSamples.reset();
        captureTimeTotal.reset();
        captureLatencyNs.set(0);
        compressionLatencyNs.set(0);
        writeLatencyNs.set(0);
        lastResetTime.set(System.currentTimeMillis());
    }
    
    public String toSummary() {
        return String.format("""
            Replay Metrics Summary:
              Frames: %d captured, %d dropped (%.2f%% drop rate)
              Sessions: %d started, %d completed, %d pruned
              Data: %.2f MB written, %.2f MB compressed (%.1fx ratio)
              Performance: %.2f fps, %.3fms avg capture time
              Latency: capture=%.3fms, compress=%.3fms, write=%.3fms
            """,
            getFramesCaptured(),
            getFramesDropped(),
            getDropRate() * 100,
            getSessionsStarted(),
            getSessionsCompleted(),
            getSessionsPruned(),
            getBytesWritten() / (1024.0 * 1024),
            getBytesCompressed() / (1024.0 * 1024),
            getCompressionRatio(),
            getFramesPerSecond(),
            getAverageCaptureTimeMs(),
            getLastCaptureLatencyMs(),
            getLastCompressionLatencyMs(),
            getLastWriteLatencyMs()
        );
    }
}
