package rubidium.performance;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

/**
 * Object pool for reducing garbage collection pressure.
 */
public class ObjectPool<T> {
    
    private final Queue<T> pool;
    private final Supplier<T> factory;
    private final int maxSize;
    
    public ObjectPool(Supplier<T> factory, int maxSize) {
        this.pool = new ConcurrentLinkedQueue<>();
        this.factory = factory;
        this.maxSize = maxSize;
    }
    
    public static <T> ObjectPool<T> create(Supplier<T> factory, int maxSize) {
        return new ObjectPool<>(factory, maxSize);
    }
    
    public T acquire() {
        T object = pool.poll();
        return object != null ? object : factory.get();
    }
    
    public void release(T object) {
        if (object != null && pool.size() < maxSize) {
            pool.offer(object);
        }
    }
    
    public int getPoolSize() {
        return pool.size();
    }
    
    public int getMaxSize() {
        return maxSize;
    }
    
    public void clear() {
        pool.clear();
    }
    
    public void preallocate(int count) {
        for (int i = 0; i < count && pool.size() < maxSize; i++) {
            pool.offer(factory.get());
        }
    }
}
