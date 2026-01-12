package com.yellowtale.rubidium.replay;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public final class ReplayBuffer {
    
    private final UUID playerId;
    private final AtomicReferenceArray<ReplayFrame> frames;
    private final int capacity;
    
    private final AtomicInteger writeIndex;
    private final AtomicInteger size;
    private final AtomicLong totalFramesWritten;
    
    private final ReplayFramePool framePool;
    private final ReadWriteLock lock;
    
    public ReplayBuffer(UUID playerId, int capacityFrames, ReplayFramePool framePool) {
        this.playerId = playerId;
        this.capacity = capacityFrames;
        this.frames = new AtomicReferenceArray<>(capacityFrames);
        this.writeIndex = new AtomicInteger(0);
        this.size = new AtomicInteger(0);
        this.totalFramesWritten = new AtomicLong(0);
        this.framePool = framePool;
        this.lock = new ReentrantReadWriteLock();
        
        for (int i = 0; i < capacityFrames; i++) {
            frames.set(i, framePool != null ? framePool.acquire() : new ReplayFrame());
        }
    }
    
    public void write(Consumer<ReplayFrame> frameWriter) {
        ReplayFrame newFrame = framePool != null ? framePool.acquire() : new ReplayFrame();
        newFrame.reset();
        newFrame.setPlayerId(playerId);
        newFrame.setTimestamp(System.currentTimeMillis());
        
        frameWriter.accept(newFrame);
        
        lock.writeLock().lock();
        try {
            int index = writeIndex.getAndUpdate(i -> (i + 1) % capacity);
            
            ReplayFrame oldFrame = frames.getAndSet(index, newFrame);
            
            if (framePool != null && oldFrame != null) {
                framePool.release(oldFrame);
            }
            
            int currentSize = size.get();
            if (currentSize < capacity) {
                size.incrementAndGet();
            }
            
            totalFramesWritten.incrementAndGet();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public ReplayFrame[] snapshot(int maxFrames) {
        lock.readLock().lock();
        try {
            int currentSize = Math.min(size.get(), maxFrames);
            if (currentSize == 0) {
                return new ReplayFrame[0];
            }
            
            ReplayFrame[] result = new ReplayFrame[currentSize];
            int currentWriteIndex = writeIndex.get();
            
            int startIndex = (currentWriteIndex - currentSize + capacity) % capacity;
            
            for (int i = 0; i < currentSize; i++) {
                int bufferIndex = (startIndex + i) % capacity;
                ReplayFrame frame = frames.get(bufferIndex);
                result[i] = frame != null ? frame.copy() : new ReplayFrame();
            }
            
            return result;
        } finally {
            lock.readLock().unlock();
        }
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
        lock.writeLock().lock();
        try {
            if (framePool != null) {
                for (int i = 0; i < capacity; i++) {
                    ReplayFrame frame = frames.get(i);
                    if (frame != null) {
                        framePool.release(frame);
                        frames.set(i, null);
                    }
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public UUID getPlayerId() { return playerId; }
    public int getCapacity() { return capacity; }
    public int getSize() { return size.get(); }
    public long getTotalFramesWritten() { return totalFramesWritten.get(); }
    public boolean isFull() { return size.get() >= capacity; }
    public boolean isEmpty() { return size.get() == 0; }
    
    public long getOldestTimestamp() {
        lock.readLock().lock();
        try {
            if (isEmpty()) return 0;
            int oldestIndex = (writeIndex.get() - size.get() + capacity) % capacity;
            ReplayFrame frame = frames.get(oldestIndex);
            return frame != null ? frame.getTimestamp() : 0;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public long getNewestTimestamp() {
        lock.readLock().lock();
        try {
            if (isEmpty()) return 0;
            int newestIndex = (writeIndex.get() - 1 + capacity) % capacity;
            ReplayFrame frame = frames.get(newestIndex);
            return frame != null ? frame.getTimestamp() : 0;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public long getBufferedDurationMs() {
        return getNewestTimestamp() - getOldestTimestamp();
    }
}
