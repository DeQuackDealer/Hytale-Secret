package com.yellowtale.rubidium.performance;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class ChunkLoadMonitor {
    
    private final AtomicLong loadedChunks;
    private final AtomicLong totalLoads;
    private final AtomicLong totalUnloads;
    private final AtomicLong peakChunks;
    
    private final int unloadThreshold;
    private Consumer<Long> gcSuggestionListener;
    
    private volatile long lastUnloadBatch;
    private volatile long lastGcSuggestion;
    
    public ChunkLoadMonitor(int unloadThreshold) {
        this.loadedChunks = new AtomicLong(0);
        this.totalLoads = new AtomicLong(0);
        this.totalUnloads = new AtomicLong(0);
        this.peakChunks = new AtomicLong(0);
        this.unloadThreshold = unloadThreshold;
        this.lastUnloadBatch = 0;
        this.lastGcSuggestion = 0;
    }
    
    public void onChunkLoad() {
        long current = loadedChunks.incrementAndGet();
        totalLoads.incrementAndGet();
        
        long peak = peakChunks.get();
        while (current > peak) {
            if (peakChunks.compareAndSet(peak, current)) {
                break;
            }
            peak = peakChunks.get();
        }
    }
    
    public void onChunkUnload() {
        loadedChunks.decrementAndGet();
        long unloads = totalUnloads.incrementAndGet();
        
        long batchStart = lastUnloadBatch;
        long unloadsSinceBatch = unloads - batchStart;
        
        if (unloadsSinceBatch >= unloadThreshold) {
            lastUnloadBatch = unloads;
            suggestGc();
        }
    }
    
    public void onBulkUnload(int count) {
        loadedChunks.addAndGet(-count);
        totalUnloads.addAndGet(count);
        
        if (count >= unloadThreshold) {
            suggestGc();
        }
    }
    
    public long getLoadedChunks() {
        return loadedChunks.get();
    }
    
    public long getTotalLoads() {
        return totalLoads.get();
    }
    
    public long getTotalUnloads() {
        return totalUnloads.get();
    }
    
    public long getPeakChunks() {
        return peakChunks.get();
    }
    
    public void resetPeak() {
        peakChunks.set(loadedChunks.get());
    }
    
    public void setCurrentChunks(long count) {
        long previous = loadedChunks.getAndSet(count);
        
        if (previous > count && (previous - count) >= unloadThreshold) {
            suggestGc();
        }
    }
    
    public void onGcSuggestion(Consumer<Long> listener) {
        this.gcSuggestionListener = listener;
    }
    
    private void suggestGc() {
        long now = System.currentTimeMillis();
        if (now - lastGcSuggestion < 30000) {
            return;
        }
        
        lastGcSuggestion = now;
        
        if (gcSuggestionListener != null) {
            gcSuggestionListener.accept(loadedChunks.get());
        }
    }
    
    public ChunkLoadStats getStats() {
        return new ChunkLoadStats(
            loadedChunks.get(),
            totalLoads.get(),
            totalUnloads.get(),
            peakChunks.get()
        );
    }
    
    public record ChunkLoadStats(
        long current,
        long totalLoads,
        long totalUnloads,
        long peak
    ) {
        public double getChurnRate() {
            long total = totalLoads + totalUnloads;
            return total > 0 ? (double) totalUnloads / total : 0;
        }
    }
}
