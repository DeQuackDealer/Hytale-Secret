package com.yellowtale.rubidium.performance.memory;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public final class ByteBufferPool {
    private static final Logger LOGGER = Logger.getLogger(ByteBufferPool.class.getName());
    
    private static final int[] BUCKET_SIZES = {64, 256, 1024, 4096, 16384, 65536, 262144};
    private static final int MAX_POOL_SIZE_PER_BUCKET = 64;
    
    @SuppressWarnings("unchecked")
    private final ConcurrentLinkedQueue<ByteBuffer>[] directPools = new ConcurrentLinkedQueue[BUCKET_SIZES.length];
    @SuppressWarnings("unchecked")
    private final ConcurrentLinkedQueue<ByteBuffer>[] heapPools = new ConcurrentLinkedQueue[BUCKET_SIZES.length];
    private final AtomicInteger[] directPoolSizes = new AtomicInteger[BUCKET_SIZES.length];
    private final AtomicInteger[] heapPoolSizes = new AtomicInteger[BUCKET_SIZES.length];
    
    private final AtomicLong allocations = new AtomicLong();
    private final AtomicLong directAllocations = new AtomicLong();
    private final AtomicLong poolHits = new AtomicLong();
    private final AtomicLong poolMisses = new AtomicLong();
    private final AtomicLong totalBytesAllocated = new AtomicLong();
    private final AtomicLong currentBytesInUse = new AtomicLong();
    
    private static final ByteBufferPool INSTANCE = new ByteBufferPool();
    
    private ByteBufferPool() {
        for (int i = 0; i < BUCKET_SIZES.length; i++) {
            directPools[i] = new ConcurrentLinkedQueue<>();
            heapPools[i] = new ConcurrentLinkedQueue<>();
            directPoolSizes[i] = new AtomicInteger();
            heapPoolSizes[i] = new AtomicInteger();
        }
    }
    
    public static ByteBufferPool getInstance() {
        return INSTANCE;
    }
    
    private int findBucket(int size) {
        for (int i = 0; i < BUCKET_SIZES.length; i++) {
            if (size <= BUCKET_SIZES[i]) return i;
        }
        return -1;
    }
    
    public PooledByteBuffer acquire(int minCapacity, boolean direct) {
        allocations.incrementAndGet();
        int bucket = findBucket(minCapacity);
        
        if (bucket >= 0) {
            ConcurrentLinkedQueue<ByteBuffer> pool = direct ? directPools[bucket] : heapPools[bucket];
            AtomicInteger poolSize = direct ? directPoolSizes[bucket] : heapPoolSizes[bucket];
            
            ByteBuffer buffer = pool.poll();
            if (buffer != null) {
                poolSize.decrementAndGet();
                poolHits.incrementAndGet();
                buffer.clear();
                currentBytesInUse.addAndGet(buffer.capacity());
                return new PooledByteBuffer(buffer, this, bucket, direct);
            }
        }
        
        poolMisses.incrementAndGet();
        int allocSize = bucket >= 0 ? BUCKET_SIZES[bucket] : minCapacity;
        
        ByteBuffer buffer;
        if (direct) {
            buffer = ByteBuffer.allocateDirect(allocSize);
            directAllocations.incrementAndGet();
        } else {
            buffer = ByteBuffer.allocate(allocSize);
        }
        
        totalBytesAllocated.addAndGet(allocSize);
        currentBytesInUse.addAndGet(allocSize);
        
        return new PooledByteBuffer(buffer, this, bucket, direct);
    }
    
    public PooledByteBuffer acquireDirect(int minCapacity) {
        return acquire(minCapacity, true);
    }
    
    public PooledByteBuffer acquireHeap(int minCapacity) {
        return acquire(minCapacity, false);
    }
    
    void release(ByteBuffer buffer, int bucket, boolean direct) {
        currentBytesInUse.addAndGet(-buffer.capacity());
        
        if (bucket < 0) {
            return;
        }
        
        AtomicInteger poolSize = direct ? directPoolSizes[bucket] : heapPoolSizes[bucket];
        if (poolSize.get() >= MAX_POOL_SIZE_PER_BUCKET) {
            return;
        }
        
        buffer.clear();
        ConcurrentLinkedQueue<ByteBuffer> pool = direct ? directPools[bucket] : heapPools[bucket];
        pool.offer(buffer);
        poolSize.incrementAndGet();
    }
    
    public void clear() {
        for (int i = 0; i < BUCKET_SIZES.length; i++) {
            directPools[i].clear();
            heapPools[i].clear();
            directPoolSizes[i].set(0);
            heapPoolSizes[i].set(0);
        }
        LOGGER.info("[RPAL] ByteBuffer pools cleared");
    }
    
    public PoolStats getStats() {
        int totalPooled = 0;
        long pooledBytes = 0;
        
        for (int i = 0; i < BUCKET_SIZES.length; i++) {
            int directCount = directPoolSizes[i].get();
            int heapCount = heapPoolSizes[i].get();
            totalPooled += directCount + heapCount;
            pooledBytes += (long) (directCount + heapCount) * BUCKET_SIZES[i];
        }
        
        return new PoolStats(
            allocations.get(),
            directAllocations.get(),
            poolHits.get(),
            poolMisses.get(),
            totalBytesAllocated.get(),
            currentBytesInUse.get(),
            totalPooled,
            pooledBytes
        );
    }
    
    public record PoolStats(
        long totalAllocations,
        long directAllocations,
        long poolHits,
        long poolMisses,
        long totalBytesAllocated,
        long currentBytesInUse,
        int buffersInPool,
        long pooledBytes
    ) {
        public double hitRate() {
            long total = poolHits + poolMisses;
            return total > 0 ? (double) poolHits / total : 0.0;
        }
    }
    
    public static final class PooledByteBuffer implements AutoCloseable {
        private final ByteBuffer buffer;
        private final ByteBufferPool pool;
        private final int bucket;
        private final boolean direct;
        private volatile boolean released = false;
        
        PooledByteBuffer(ByteBuffer buffer, ByteBufferPool pool, int bucket, boolean direct) {
            this.buffer = buffer;
            this.pool = pool;
            this.bucket = bucket;
            this.direct = direct;
        }
        
        public ByteBuffer buffer() {
            if (released) throw new IllegalStateException("Buffer already released");
            return buffer;
        }
        
        public boolean isDirect() {
            return direct;
        }
        
        public int capacity() {
            return buffer.capacity();
        }
        
        public void release() {
            if (!released) {
                released = true;
                pool.release(buffer, bucket, direct);
            }
        }
        
        @Override
        public void close() {
            release();
        }
    }
}
