package rubidium.scheduler;

import rubidium.api.RubidiumPlugin;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class Scheduler {
    
    private static final Logger logger = Logger.getLogger("Rubidium-Scheduler");
    
    private final ScheduledExecutorService asyncExecutor;
    private final ExecutorService syncExecutor;
    private final Map<Long, ScheduledTask> tasks;
    private final AtomicLong taskIdCounter;
    
    private volatile boolean running = true;
    
    public Scheduler() {
        this.asyncExecutor = Executors.newScheduledThreadPool(4, r -> {
            Thread t = new Thread(r, "Rubidium-Async-Scheduler");
            t.setDaemon(true);
            return t;
        });
        this.syncExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Rubidium-Sync-Scheduler");
            t.setDaemon(true);
            return t;
        });
        this.tasks = new ConcurrentHashMap<>();
        this.taskIdCounter = new AtomicLong(0);
    }
    
    public ScheduledTask runTask(RubidiumPlugin plugin, Runnable task) {
        return runTaskLater(plugin, task, 0);
    }
    
    public ScheduledTask runTaskAsync(RubidiumPlugin plugin, Runnable task) {
        return runTaskLaterAsync(plugin, task, 0);
    }
    
    public ScheduledTask runTaskLater(RubidiumPlugin plugin, Runnable task, long delayTicks) {
        long delayMs = ticksToMillis(delayTicks);
        return scheduleTask(plugin, task, delayMs, -1, false);
    }
    
    public ScheduledTask runTaskLaterAsync(RubidiumPlugin plugin, Runnable task, long delayTicks) {
        long delayMs = ticksToMillis(delayTicks);
        return scheduleTask(plugin, task, delayMs, -1, true);
    }
    
    public ScheduledTask runTaskTimer(RubidiumPlugin plugin, Runnable task, long delayTicks, long periodTicks) {
        long delayMs = ticksToMillis(delayTicks);
        long periodMs = ticksToMillis(periodTicks);
        return scheduleTask(plugin, task, delayMs, periodMs, false);
    }
    
    public ScheduledTask runTaskTimerAsync(RubidiumPlugin plugin, Runnable task, long delayTicks, long periodTicks) {
        long delayMs = ticksToMillis(delayTicks);
        long periodMs = ticksToMillis(periodTicks);
        return scheduleTask(plugin, task, delayMs, periodMs, true);
    }
    
    public <T> CompletableFuture<T> supplyAsync(RubidiumPlugin plugin, Callable<T> callable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        
        runTaskAsync(plugin, () -> {
            try {
                future.complete(callable.call());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }
    
    public CompletableFuture<Void> runAsync(RubidiumPlugin plugin, Runnable runnable) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        runTaskAsync(plugin, () -> {
            try {
                runnable.run();
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }
    
    private ScheduledTask scheduleTask(RubidiumPlugin plugin, Runnable task, long delayMs, long periodMs, boolean async) {
        long taskId = taskIdCounter.incrementAndGet();
        
        ScheduledTask scheduledTask = new ScheduledTask(taskId, plugin, task, periodMs > 0);
        tasks.put(taskId, scheduledTask);
        
        Runnable wrappedTask = () -> {
            if (!running || scheduledTask.isCancelled()) {
                return;
            }
            
            try {
                if (async) {
                    task.run();
                } else {
                    syncExecutor.submit(task);
                }
            } catch (Exception e) {
                logger.error("Error in scheduled task " + taskId + ": " + e.getMessage());
                e.printStackTrace();
            }
        };
        
        Future<?> future;
        if (periodMs > 0) {
            future = asyncExecutor.scheduleAtFixedRate(wrappedTask, delayMs, periodMs, TimeUnit.MILLISECONDS);
        } else {
            future = asyncExecutor.schedule(wrappedTask, delayMs, TimeUnit.MILLISECONDS);
        }
        
        scheduledTask.setFuture(future);
        return scheduledTask;
    }
    
    public void cancelTask(long taskId) {
        ScheduledTask task = tasks.remove(taskId);
        if (task != null) {
            task.cancel();
        }
    }
    
    public void cancelTasks(RubidiumPlugin plugin) {
        List<Long> toCancel = new ArrayList<>();
        
        for (Map.Entry<Long, ScheduledTask> entry : tasks.entrySet()) {
            if (entry.getValue().getPlugin() == plugin) {
                toCancel.add(entry.getKey());
            }
        }
        
        for (Long taskId : toCancel) {
            cancelTask(taskId);
        }
    }
    
    public void shutdown() {
        running = false;
        
        for (ScheduledTask task : tasks.values()) {
            task.cancel();
        }
        tasks.clear();
        
        asyncExecutor.shutdown();
        syncExecutor.shutdown();
        
        try {
            if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
            if (!syncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                syncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            syncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("Scheduler shut down");
    }
    
    private long ticksToMillis(long ticks) {
        return ticks * 50;
    }
    
    public static class ScheduledTask {
        private final long taskId;
        private final RubidiumPlugin plugin;
        private final Runnable task;
        private final boolean repeating;
        private volatile boolean cancelled = false;
        private Future<?> future;
        
        ScheduledTask(long taskId, RubidiumPlugin plugin, Runnable task, boolean repeating) {
            this.taskId = taskId;
            this.plugin = plugin;
            this.task = task;
            this.repeating = repeating;
        }
        
        public long getTaskId() { return taskId; }
        public RubidiumPlugin getPlugin() { return plugin; }
        public boolean isRepeating() { return repeating; }
        public boolean isCancelled() { return cancelled; }
        
        void setFuture(Future<?> future) {
            this.future = future;
        }
        
        public void cancel() {
            cancelled = true;
            if (future != null) {
                future.cancel(false);
            }
        }
    }
}
