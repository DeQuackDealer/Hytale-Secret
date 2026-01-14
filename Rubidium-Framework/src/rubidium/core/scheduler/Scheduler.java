package rubidium.core.scheduler;

import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Task scheduler for delayed and repeating tasks.
 */
public class Scheduler {
    
    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        CRITICAL
    }
    
    private static Scheduler instance;
    
    private final ScheduledExecutorService executor;
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> tasks;
    private long taskIdCounter = 0;
    
    private Scheduler() {
        this.executor = Executors.newScheduledThreadPool(4, r -> {
            Thread t = new Thread(r, "Rubidium-Scheduler");
            t.setDaemon(true);
            return t;
        });
        this.tasks = new ConcurrentHashMap<>();
    }
    
    public static Scheduler getInstance() {
        if (instance == null) {
            instance = new Scheduler();
        }
        return instance;
    }
    
    public long runLater(Runnable task, long delayTicks) {
        long id = taskIdCounter++;
        ScheduledFuture<?> future = executor.schedule(task, delayTicks * 50, TimeUnit.MILLISECONDS);
        tasks.put(id, future);
        return id;
    }
    
    public long runRepeating(Runnable task, long delayTicks, long periodTicks) {
        long id = taskIdCounter++;
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(
            task, delayTicks * 50, periodTicks * 50, TimeUnit.MILLISECONDS);
        tasks.put(id, future);
        return id;
    }
    
    public long runAsync(Runnable task) {
        long id = taskIdCounter++;
        ScheduledFuture<?> future = executor.schedule(task, 0, TimeUnit.MILLISECONDS);
        tasks.put(id, future);
        return id;
    }
    
    public void cancel(long taskId) {
        ScheduledFuture<?> future = tasks.remove(taskId);
        if (future != null) {
            future.cancel(false);
        }
    }
    
    public void cancelAll() {
        tasks.values().forEach(f -> f.cancel(false));
        tasks.clear();
    }
    
    public boolean isRunning(long taskId) {
        ScheduledFuture<?> future = tasks.get(taskId);
        return future != null && !future.isDone();
    }
    
    public void shutdown() {
        cancelAll();
        executor.shutdown();
    }
    
    public long scheduleRepeating(Runnable task, long delayTicks, long periodTicks, Priority priority) {
        return runRepeating(task, delayTicks, periodTicks);
    }
    
    public long scheduleRepeating(Runnable task, long delayTicks, long periodTicks) {
        return runRepeating(task, delayTicks, periodTicks);
    }
    
    public long scheduleRepeating(String owner, Runnable task, java.time.Duration delay, java.time.Duration period, Priority priority) {
        long delayTicks = delay.toMillis() / 50;
        long periodTicks = period.toMillis() / 50;
        return runRepeating(task, delayTicks, periodTicks);
    }
    
    public static final class SimpleTaskHandle {
        private final long taskId;
        private final Scheduler scheduler;
        
        SimpleTaskHandle(long taskId, Scheduler scheduler) {
            this.taskId = taskId;
            this.scheduler = scheduler;
        }
        
        public long getTaskId() { return taskId; }
        
        public boolean cancel() {
            scheduler.cancel(taskId);
            return true;
        }
    }
    
    public long scheduleRepeatingAsync(Runnable task, long delayTicks, long periodTicks, Priority priority) {
        return runRepeating(task, delayTicks, periodTicks);
    }
}
