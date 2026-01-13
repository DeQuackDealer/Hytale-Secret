package jurassictale.ai.pool;

import java.util.*;
import java.util.function.*;

public class ObjectPool<T> {
    
    private final Deque<T> available;
    private final Supplier<T> factory;
    private final Consumer<T> resetter;
    private final int maxSize;
    private int created;
    
    public ObjectPool(Supplier<T> factory, Consumer<T> resetter, int initialSize, int maxSize) {
        this.available = new ArrayDeque<>(initialSize);
        this.factory = factory;
        this.resetter = resetter;
        this.maxSize = maxSize;
        this.created = 0;
        
        for (int i = 0; i < initialSize; i++) {
            available.push(factory.get());
            created++;
        }
    }
    
    public static <T> ObjectPool<T> create(Supplier<T> factory, Consumer<T> resetter, int size) {
        return new ObjectPool<>(factory, resetter, size, size * 2);
    }
    
    public T acquire() {
        if (!available.isEmpty()) {
            return available.pop();
        }
        if (created < maxSize) {
            created++;
            return factory.get();
        }
        return factory.get();
    }
    
    public void release(T object) {
        if (available.size() < maxSize) {
            resetter.accept(object);
            available.push(object);
        }
    }
    
    public int getAvailable() {
        return available.size();
    }
    
    public int getCreated() {
        return created;
    }
}
