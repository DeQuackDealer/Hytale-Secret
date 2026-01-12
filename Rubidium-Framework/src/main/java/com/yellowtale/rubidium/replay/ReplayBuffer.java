package com.yellowtale.rubidium.replay;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class ReplayBuffer {
    
    private final UUID playerId;
    private final ReplayFrame[] frames;
    private final int capacity;
    
    private final AtomicInteger writeIndex;
    private final AtomicInteger size;
    private final AtomicLong totalFramesWritten;
    
    private final ReplayFramePool framePool;
    
    public ReplayBuffer(UUID playerId, int capacityFrames, ReplayFramePool framePool) {
        this.playerId = playerId;
        this.capacity = capacityFrames;
        this.frames = new ReplayFrame[capacityFrames];
        this.writeIndex = new AtomicInteger(0);
        this.size = new AtomicInteger(0);
        this.totalFramesWritten = new AtomicLong(0);
        this.framePool = framePool;
        
        for (int i = 0; i < capacityFrames; i++) {
            frames[i] = framePool != null ? framePool.acquire() : new ReplayFrame();
        }
    }
    
    public void write(Consumer<ReplayFrame> frameWriter) {
        int index = writeIndex.getAndUpdate(i -> (i + 1) % capacity);
        
        ReplayFrame frame = frames[index];
        frame.reset();
        frame.setPlayerId(playerId);
        frame.setTimestamp(System.currentTimeMillis());
        
        frameWriter.accept(frame);
        
        int currentSize = size.get();
        if (currentSize < capacity) {
            size.incrementAndGet();
        }
        
        totalFramesWritten.incrementAndGet();
    }
    
    public ReplayFrame[] snapshot(int maxFrames) {
        int currentSize = Math.min(size.get(), maxFrames);
        if (currentSize == 0) {
            return new ReplayFrame[0];
        }
        
        ReplayFrame[] result = new ReplayFrame[currentSize];
        int currentWriteIndex = writeIndex.get();
        
        int startIndex = (currentWriteIndex - currentSize + capacity) % capacity;
        
        for (int i = 0; i < currentSize; i++) {
            int bufferIndex = (startIndex + i) % capacity;
            result[i] = frames[bufferIndex].copy();
        }
        
        return result;
    }
    
    public ReplaySegment toSegment(int tickRate, int captureRadius) {
        ReplayFrame[] snapshotFrames = snapshot(capacity);
        if (snapshotFrames.length == 0) {
            return null;
        }
        
        ReplaySegment segment = new ReplaySegment(playerId, tickRate, captureRadius);
        for (ReplayFrame frame : snapshotFrames) {
            segment.addFrame(frame);
        }
        
        return segment;
    }
    
    public ReplaySegment toSegment(int maxFrames, int tickRate, int captureRadius) {
        ReplayFrame[] snapshotFrames = snapshot(maxFrames);
        if (snapshotFrames.length == 0) {
            return null;
        }
        
        ReplaySegment segment = new ReplaySegment(playerId, tickRate, captureRadius);
        for (ReplayFrame frame : snapshotFrames) {
            segment.addFrame(frame);
        }
        
        return segment;
    }
    
    public void clear() {
        size.set(0);
        writeIndex.set(0);
    }
    
    public void release() {
        if (framePool != null) {
            for (ReplayFrame frame : frames) {
                framePool.release(frame);
            }
        }
    }
    
    public UUID getPlayerId() { return playerId; }
    public int getCapacity() { return capacity; }
    public int getSize() { return size.get(); }
    public long getTotalFramesWritten() { return totalFramesWritten.get(); }
    public boolean isFull() { return size.get() >= capacity; }
    public boolean isEmpty() { return size.get() == 0; }
    
    public long getOldestTimestamp() {
        if (isEmpty()) return 0;
        int oldestIndex = (writeIndex.get() - size.get() + capacity) % capacity;
        return frames[oldestIndex].getTimestamp();
    }
    
    public long getNewestTimestamp() {
        if (isEmpty()) return 0;
        int newestIndex = (writeIndex.get() - 1 + capacity) % capacity;
        return frames[newestIndex].getTimestamp();
    }
    
    public long getBufferedDurationMs() {
        return getNewestTimestamp() - getOldestTimestamp();
    }
}
