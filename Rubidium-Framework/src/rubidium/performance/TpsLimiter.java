package rubidium.performance;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TpsLimiter {
    
    private final AtomicInteger targetTps;
    private final AtomicLong lastTickTime;
    private final AtomicLong tickCount;
    
    private volatile long tickIntervalNanos;
    private volatile boolean enabled;
    
    public TpsLimiter() {
        this(20);
    }
    
    public TpsLimiter(int targetTps) {
        this.targetTps = new AtomicInteger(targetTps);
        this.lastTickTime = new AtomicLong(System.nanoTime());
        this.tickCount = new AtomicLong(0);
        this.enabled = true;
        updateTickInterval();
    }
    
    public void setTargetTps(int tps) {
        if (tps < 1 || tps > 100) {
            throw new IllegalArgumentException("TPS must be between 1 and 100");
        }
        targetTps.set(tps);
        updateTickInterval();
    }
    
    public int getTargetTps() {
        return targetTps.get();
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void preTickWait() throws InterruptedException {
        if (!enabled) return;
        
        long now = System.nanoTime();
        long elapsed = now - lastTickTime.get();
        long sleepNanos = tickIntervalNanos - elapsed;
        
        if (sleepNanos > 0) {
            long sleepMillis = sleepNanos / 1_000_000;
            int sleepNanosRemainder = (int) (sleepNanos % 1_000_000);
            
            if (sleepMillis > 0 || sleepNanosRemainder > 0) {
                Thread.sleep(sleepMillis, sleepNanosRemainder);
            }
        }
    }
    
    public void postTick() {
        lastTickTime.set(System.nanoTime());
        tickCount.incrementAndGet();
    }
    
    public long getTickCount() {
        return tickCount.get();
    }
    
    public double getMeasuredTps() {
        return 1_000_000_000.0 / tickIntervalNanos;
    }
    
    private void updateTickInterval() {
        tickIntervalNanos = 1_000_000_000L / targetTps.get();
    }
    
    public static long calculateSleepTime(int targetTps, long tickStartNanos) {
        long targetInterval = 1_000_000_000L / targetTps;
        long elapsed = System.nanoTime() - tickStartNanos;
        return Math.max(0, targetInterval - elapsed);
    }
}
