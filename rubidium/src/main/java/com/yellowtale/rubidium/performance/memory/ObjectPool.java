package com.yellowtale.rubidium.performance.memory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

public final class ObjectPool<T> {
    private static final Logger LOGGER = Logger.getLogger(ObjectPool.class.getName());
    
    private static final ConcurrentHashMap<Class<?>, ObjectPool<?>> GLOBAL_POOLS = new ConcurrentHashMap<>();
    
    private final String name;
    private final Supplier<T> factory;
    private final Consumer<T> resetter;
    private final int maxPoolSize;
    private final ConcurrentLinkedQueue<T> pool = new ConcurrentLinkedQueue<>();
    private final AtomicInteger poolSize = new AtomicInteger();
    
    private final AtomicLong acquisitions = new AtomicLong();
    private final AtomicLong creations = new AtomicLong();
    private final AtomicLong releases = new AtomicLong();
    private final AtomicLong poolHits = new AtomicLong();
    
    private ObjectPool(String name, Supplier<T> factory, Consumer<T> resetter, int maxPoolSize) {
        this.name = name;
        this.factory = factory;
        this.resetter = resetter;
        this.maxPoolSize = maxPoolSize;
    }
    
    public static <T> ObjectPool<T> create(String name, Supplier<T> factory) {
        return new ObjectPool<>(name, factory, obj -> {}, 256);
    }
    
    public static <T> ObjectPool<T> create(String name, Supplier<T> factory, Consumer<T> resetter) {
        return new ObjectPool<>(name, factory, resetter, 256);
    }
    
    public static <T> ObjectPool<T> create(String name, Supplier<T> factory, Consumer<T> resetter, int maxPoolSize) {
        return new ObjectPool<>(name, factory, resetter, maxPoolSize);
    }
    
    @SuppressWarnings("unchecked")
    public static <T> ObjectPool<T> global(Class<T> clazz, Supplier<T> factory) {
        return (ObjectPool<T>) GLOBAL_POOLS.computeIfAbsent(clazz, 
            k -> new ObjectPool<>(clazz.getSimpleName(), factory, obj -> {}, 256));
    }
    
    @SuppressWarnings("unchecked")
    public static <T> ObjectPool<T> global(Class<T> clazz, Supplier<T> factory, Consumer<T> resetter) {
        return (ObjectPool<T>) GLOBAL_POOLS.computeIfAbsent(clazz, 
            k -> new ObjectPool<>(clazz.getSimpleName(), factory, resetter, 256));
    }
    
    public PooledObject<T> acquire() {
        acquisitions.incrementAndGet();
        
        T obj = pool.poll();
        if (obj != null) {
            poolSize.decrementAndGet();
            poolHits.incrementAndGet();
            return new PooledObject<>(obj, this);
        }
        
        creations.incrementAndGet();
        return new PooledObject<>(factory.get(), this);
    }
    
    void release(T obj) {
        releases.incrementAndGet();
        
        if (poolSize.get() >= maxPoolSize) {
            return;
        }
        
        try {
            resetter.accept(obj);
        } catch (Exception e) {
            LOGGER.warning("[RPAL] Failed to reset pooled object: " + e.getMessage());
            return;
        }
        
        pool.offer(obj);
        poolSize.incrementAndGet();
    }
    
    public void preAllocate(int count) {
        int toAllocate = Math.min(count, maxPoolSize - poolSize.get());
        for (int i = 0; i < toAllocate; i++) {
            pool.offer(factory.get());
            poolSize.incrementAndGet();
            creations.incrementAndGet();
        }
        LOGGER.fine("[RPAL] Pre-allocated " + toAllocate + " objects for pool: " + name);
    }
    
    public void clear() {
        pool.clear();
        poolSize.set(0);
    }
    
    public PoolStats getStats() {
        return new PoolStats(
            name,
            acquisitions.get(),
            creations.get(),
            releases.get(),
            poolHits.get(),
            poolSize.get(),
            maxPoolSize
        );
    }
    
    public String getName() {
        return name;
    }
    
    public int getCurrentSize() {
        return poolSize.get();
    }
    
    public record PoolStats(
        String name,
        long acquisitions,
        long creations,
        long releases,
        long poolHits,
        int currentPoolSize,
        int maxPoolSize
    ) {
        public double hitRate() {
            return acquisitions > 0 ? (double) poolHits / acquisitions : 0.0;
        }
        
        public double reuseRatio() {
            return creations > 0 ? (double) poolHits / creations : 0.0;
        }
    }
    
    public static final class PooledObject<T> implements AutoCloseable {
        private final T object;
        private final ObjectPool<T> pool;
        private volatile boolean released = false;
        
        PooledObject(T object, ObjectPool<T> pool) {
            this.object = object;
            this.pool = pool;
        }
        
        public T get() {
            if (released) throw new IllegalStateException("Object already released");
            return object;
        }
        
        public void release() {
            if (!released) {
                released = true;
                pool.release(object);
            }
        }
        
        @Override
        public void close() {
            release();
        }
    }
    
    public static void clearAllGlobalPools() {
        GLOBAL_POOLS.values().forEach(ObjectPool::clear);
        LOGGER.info("[RPAL] All global object pools cleared");
    }
}
