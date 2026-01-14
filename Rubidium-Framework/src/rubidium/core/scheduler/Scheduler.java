package rubidium.core.scheduler;

import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Task scheduler for delayed and repeating tasks.
 */
public class Scheduler {
    
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
}
