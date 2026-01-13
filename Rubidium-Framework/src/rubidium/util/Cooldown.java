package rubidium.util;

import java.util.*;
import java.util.concurrent.*;

public class Cooldown<K> {
    
    private final Map<K, Long> cooldowns;
    private final long duration;
    private final TimeUnit unit;
    
    public Cooldown(long duration, TimeUnit unit) {
        this.cooldowns = new ConcurrentHashMap<>();
        this.duration = duration;
        this.unit = unit;
    }
    
    public static <K> Cooldown<K> ofSeconds(long seconds) {
        return new Cooldown<>(seconds, TimeUnit.SECONDS);
    }
    
    public static <K> Cooldown<K> ofMillis(long millis) {
        return new Cooldown<>(millis, TimeUnit.MILLISECONDS);
    }
    
    public static <K> Cooldown<K> ofMinutes(long minutes) {
        return new Cooldown<>(minutes, TimeUnit.MINUTES);
    }
    
    public static <K> Cooldown<K> ofTicks(long ticks) {
        return new Cooldown<>(ticks * 50, TimeUnit.MILLISECONDS);
    }
    
    public void set(K key) {
        cooldowns.put(key, System.currentTimeMillis());
    }
    
    public void set(K key, long customDuration, TimeUnit customUnit) {
        long expiresAt = System.currentTimeMillis() + customUnit.toMillis(customDuration);
        cooldowns.put(key, expiresAt - getDurationMillis());
    }
    
    public void remove(K key) {
        cooldowns.remove(key);
    }
    
    public boolean isOnCooldown(K key) {
        Long startTime = cooldowns.get(key);
        if (startTime == null) return false;
        
        if (System.currentTimeMillis() - startTime >= getDurationMillis()) {
            cooldowns.remove(key);
            return false;
        }
        
        return true;
    }
    
    public boolean test(K key) {
        if (isOnCooldown(key)) {
            return false;
        }
        set(key);
        return true;
    }
    
    public long getRemainingMillis(K key) {
        Long startTime = cooldowns.get(key);
        if (startTime == null) return 0;
        
        long remaining = getDurationMillis() - (System.currentTimeMillis() - startTime);
        return Math.max(0, remaining);
    }
    
    public long getRemainingSeconds(K key) {
        return TimeUnit.MILLISECONDS.toSeconds(getRemainingMillis(key));
    }
    
    public double getProgress(K key) {
        Long startTime = cooldowns.get(key);
        if (startTime == null) return 1.0;
        
        long elapsed = System.currentTimeMillis() - startTime;
        return Math.min(1.0, (double) elapsed / getDurationMillis());
    }
    
    public String getRemainingFormatted(K key) {
        long remaining = getRemainingMillis(key);
        return TimeUtil.formatDuration(remaining, true);
    }
    
    private long getDurationMillis() {
        return unit.toMillis(duration);
    }
    
    public void clear() {
        cooldowns.clear();
    }
    
    public void cleanup() {
        long now = System.currentTimeMillis();
        long durationMs = getDurationMillis();
        cooldowns.entrySet().removeIf(entry -> now - entry.getValue() >= durationMs);
    }
    
    public int size() {
        return cooldowns.size();
    }
}
