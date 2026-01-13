package rubidium.replay;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public final class ReplayFramePool {
    
    private final ConcurrentLinkedQueue<ReplayFrame> pool;
    private final int maxPoolSize;
    private final AtomicInteger currentSize;
    private final AtomicInteger acquireCount;
    private final AtomicInteger releaseCount;
    private final AtomicInteger missCount;
    
    public ReplayFramePool(int initialSize, int maxSize) {
        this.pool = new ConcurrentLinkedQueue<>();
        this.maxPoolSize = maxSize;
        this.currentSize = new AtomicInteger(0);
        this.acquireCount = new AtomicInteger(0);
        this.releaseCount = new AtomicInteger(0);
        this.missCount = new AtomicInteger(0);
        
        for (int i = 0; i < initialSize; i++) {
            pool.offer(new ReplayFrame());
            currentSize.incrementAndGet();
        }
    }
    
    public ReplayFrame acquire() {
        acquireCount.incrementAndGet();
        
        ReplayFrame frame = pool.poll();
        if (frame != null) {
            currentSize.decrementAndGet();
            return frame;
        }
        
        missCount.incrementAndGet();
        return new ReplayFrame();
    }
    
    public void release(ReplayFrame frame) {
        if (frame == null) return;
        
        releaseCount.incrementAndGet();
        frame.reset();
        
        if (currentSize.get() < maxPoolSize) {
            pool.offer(frame);
            currentSize.incrementAndGet();
        }
    }
    
    public int getPoolSize() { return currentSize.get(); }
    public int getMaxPoolSize() { return maxPoolSize; }
    public int getAcquireCount() { return acquireCount.get(); }
    public int getReleaseCount() { return releaseCount.get(); }
    public int getMissCount() { return missCount.get(); }
    
    public double getHitRate() {
        int total = acquireCount.get();
        if (total == 0) return 1.0;
        return 1.0 - ((double) missCount.get() / total);
    }
    
    public void clear() {
        pool.clear();
        currentSize.set(0);
    }
    
    public void warmup(int count) {
        int toAdd = Math.min(count, maxPoolSize - currentSize.get());
        for (int i = 0; i < toAdd; i++) {
            pool.offer(new ReplayFrame());
            currentSize.incrementAndGet();
        }
    }
}
