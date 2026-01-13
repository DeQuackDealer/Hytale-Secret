package com.yellowtale.rubidium.voice;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class VoiceMetrics {
    
    private final LongAdder packetsSent;
    private final LongAdder packetsReceived;
    private final LongAdder packetsDropped;
    private final LongAdder bytesTransferred;
    private final LongAdder activeSpeakers;
    private final LongAdder tickCount;
    
    private final AtomicLong peakActiveSpeakers;
    private final AtomicLong lastResetTime;
    
    public VoiceMetrics() {
        this.packetsSent = new LongAdder();
        this.packetsReceived = new LongAdder();
        this.packetsDropped = new LongAdder();
        this.bytesTransferred = new LongAdder();
        this.activeSpeakers = new LongAdder();
        this.tickCount = new LongAdder();
        this.peakActiveSpeakers = new AtomicLong(0);
        this.lastResetTime = new AtomicLong(System.currentTimeMillis());
    }
    
    public void recordPacketSent() {
        packetsSent.increment();
    }
    
    public void recordPacketReceived() {
        packetsReceived.increment();
    }
    
    public void recordPacketDropped() {
        packetsDropped.increment();
    }
    
    public void recordBytesTransferred(long bytes) {
        bytesTransferred.add(bytes);
    }
    
    public void recordActiveSpeakers(int count) {
        activeSpeakers.reset();
        activeSpeakers.add(count);
        peakActiveSpeakers.updateAndGet(current -> Math.max(current, count));
    }
    
    public void recordTick() {
        tickCount.increment();
    }
    
    public long getPacketsSent() {
        return packetsSent.sum();
    }
    
    public long getPacketsReceived() {
        return packetsReceived.sum();
    }
    
    public long getPacketsDropped() {
        return packetsDropped.sum();
    }
    
    public long getBytesTransferred() {
        return bytesTransferred.sum();
    }
    
    public long getActiveSpeakers() {
        return activeSpeakers.sum();
    }
    
    public long getPeakActiveSpeakers() {
        return peakActiveSpeakers.get();
    }
    
    public double getPacketsPerSecond() {
        long elapsed = System.currentTimeMillis() - lastResetTime.get();
        if (elapsed < 1000) return 0;
        return (packetsSent.sum() * 1000.0) / elapsed;
    }
    
    public double getDropRate() {
        long total = packetsSent.sum() + packetsDropped.sum();
        if (total == 0) return 0;
        return (double) packetsDropped.sum() / total;
    }
    
    public void reset() {
        packetsSent.reset();
        packetsReceived.reset();
        packetsDropped.reset();
        bytesTransferred.reset();
        activeSpeakers.reset();
        tickCount.reset();
        peakActiveSpeakers.set(0);
        lastResetTime.set(System.currentTimeMillis());
    }
    
    public String toSummary() {
        return String.format("""
            Voice Chat Metrics:
              Packets: %d sent, %d received, %d dropped (%.2f%% drop rate)
              Data: %.2f MB transferred
              Active speakers: %d (peak: %d)
              Rate: %.1f packets/second
            """,
            getPacketsSent(),
            getPacketsReceived(),
            getPacketsDropped(),
            getDropRate() * 100,
            getBytesTransferred() / (1024.0 * 1024.0),
            getActiveSpeakers(),
            getPeakActiveSpeakers(),
            getPacketsPerSecond()
        );
    }
}
