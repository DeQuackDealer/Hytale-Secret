package rubidium.core.scheduler;

import rubidium.api.RubidiumPlugin;
import rubidium.api.scheduler.TaskScheduler;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class TaskSchedulerImpl implements TaskScheduler {
    
    private static final Logger LOGGER = Logger.getLogger("RubidiumScheduler");
    private static TaskSchedulerImpl instance;
    
    private final ScheduledExecutorService asyncExecutor;
    private final ScheduledExecutorService syncExecutor;
    private final Map<Integer, ScheduledTaskImpl> tasks = new ConcurrentHashMap<>();
    private final Map<RubidiumPlugin, CopyOnWriteArrayList<Integer>> pluginTasks = new ConcurrentHashMap<>();
    private final AtomicInteger taskIdCounter = new AtomicInteger(1);
    private final AtomicInteger tickCounter = new AtomicInteger(0);
    private final Thread mainThread;
    private final BlockingQueue<Runnable> mainThreadQueue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;
    
    public TaskSchedulerImpl() {
        this.mainThread = Thread.currentThread();
        this.asyncExecutor = Executors.newScheduledThreadPool(4, r -> {
            Thread t = new Thread(r, "Rubidium-Async-" + taskIdCounter.get());
            t.setDaemon(true);
            return t;
        });
        this.syncExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Rubidium-Sync");
            t.setDaemon(true);
            return t;
        });
        
        syncExecutor.scheduleAtFixedRate(() -> {
            tickCounter.incrementAndGet();
            processMainThreadQueue();
        }, 0, 50, TimeUnit.MILLISECONDS);
        
        instance = this;
        LOGGER.info("[Scheduler] TaskScheduler initialized with 4 async threads");
    }
    
    public static TaskSchedulerImpl getInstance() {
        if (instance == null) {
            instance = new TaskSchedulerImpl();
        }
        return instance;
    }
    
    private void processMainThreadQueue() {
        Runnable task;
        while ((task = mainThreadQueue.poll()) != null) {
            try {
                task.run();
            } catch (Exception e) {
                LOGGER.severe("[Scheduler] Error in main thread task: " + e.getMessage());
            }
        }
    }
    
    @Override
    public Task runTask(RubidiumPlugin plugin, Runnable task) {
        return scheduleTask(plugin, task, 0, -1, TimeUnit.MILLISECONDS, true);
    }
    
    @Override
    public Task runTaskLater(RubidiumPlugin plugin, Runnable task, long delay, TimeUnit unit) {
        return scheduleTask(plugin, task, delay, -1, unit, true);
    }
    
    @Override
    public Task runTaskTimer(RubidiumPlugin plugin, Runnable task, long delay, long period, TimeUnit unit) {
        return scheduleTask(plugin, task, delay, period, unit, true);
    }
    
    @Override
    public Task runTaskAsync(RubidiumPlugin plugin, Runnable task) {
        return scheduleTask(plugin, task, 0, -1, TimeUnit.MILLISECONDS, false);
    }
    
    @Override
    public Task runTaskLaterAsync(RubidiumPlugin plugin, Runnable task, long delay, TimeUnit unit) {
        return scheduleTask(plugin, task, delay, -1, unit, false);
    }
    
    @Override
    public Task runTaskTimerAsync(RubidiumPlugin plugin, Runnable task, long delay, long period, TimeUnit unit) {
        return scheduleTask(plugin, task, delay, period, unit, false);
    }
    
    @Override
    public Task runOnMainThread(RubidiumPlugin plugin, Runnable task) {
        int taskId = taskIdCounter.getAndIncrement();
        ScheduledTaskImpl scheduledTask = new ScheduledTaskImpl(taskId, plugin, true);
        tasks.put(taskId, scheduledTask);
        registerPluginTask(plugin, taskId);
        
        mainThreadQueue.offer(() -> {
            if (!scheduledTask.isCancelled()) {
                try {
                    task.run();
                } catch (Exception e) {
                    LOGGER.severe("[Scheduler] Error in main thread task: " + e.getMessage());
                } finally {
                    scheduledTask.markCompleted();
                    tasks.remove(taskId);
                }
            }
        });
        
        return scheduledTask;
    }
    
    private Task scheduleTask(RubidiumPlugin plugin, Runnable task, long delay, long period, TimeUnit unit, boolean sync) {
        int taskId = taskIdCounter.getAndIncrement();
        ScheduledTaskImpl scheduledTask = new ScheduledTaskImpl(taskId, plugin, sync);
        tasks.put(taskId, scheduledTask);
        registerPluginTask(plugin, taskId);
        
        ScheduledExecutorService executor = sync ? syncExecutor : asyncExecutor;
        
        Runnable wrappedTask = () -> {
            if (scheduledTask.isCancelled()) return;
            try {
                task.run();
            } catch (Exception e) {
                LOGGER.severe("[Scheduler] Error in task " + taskId + ": " + e.getMessage());
            }
        };
        
        ScheduledFuture<?> future;
        if (period > 0) {
            future = executor.scheduleAtFixedRate(wrappedTask, delay, period, unit);
        } else if (delay > 0) {
            future = executor.schedule(wrappedTask, delay, unit);
        } else {
            future = executor.schedule(wrappedTask, 0, TimeUnit.MILLISECONDS);
        }
        
        scheduledTask.setFuture(future);
        return scheduledTask;
    }
    
    private void registerPluginTask(RubidiumPlugin plugin, int taskId) {
        if (plugin != null) {
            pluginTasks.computeIfAbsent(plugin, k -> new CopyOnWriteArrayList<>()).add(taskId);
        }
    }
    
    @Override
    public void cancelTask(int taskId) {
        ScheduledTaskImpl task = tasks.remove(taskId);
        if (task != null) {
            task.cancel();
        }
    }
    
    @Override
    public void cancelTasks(RubidiumPlugin plugin) {
        CopyOnWriteArrayList<Integer> taskIds = pluginTasks.remove(plugin);
        if (taskIds != null) {
            for (int taskId : taskIds) {
                cancelTask(taskId);
            }
        }
    }
    
    @Override
    public boolean isMainThread() {
        return Thread.currentThread() == mainThread;
    }
    
    @Override
    public int getCurrentTick() {
        return tickCounter.get();
    }
    
    public void shutdown() {
        running = false;
        asyncExecutor.shutdown();
        syncExecutor.shutdown();
        try {
            asyncExecutor.awaitTermination(5, TimeUnit.SECONDS);
            syncExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        LOGGER.info("[Scheduler] TaskScheduler shutdown complete");
    }
    
    public int getActiveTaskCount() {
        return tasks.size();
    }
    
    private static class ScheduledTaskImpl implements Task {
        private final int taskId;
        private final RubidiumPlugin owner;
        private final boolean sync;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private volatile ScheduledFuture<?> future;
        
        public ScheduledTaskImpl(int taskId, RubidiumPlugin owner, boolean sync) {
            this.taskId = taskId;
            this.owner = owner;
            this.sync = sync;
        }
        
        public void setFuture(ScheduledFuture<?> future) {
            this.future = future;
        }
        
        public void markCompleted() {
            cancelled.set(true);
        }
        
        @Override
        public int getTaskId() {
            return taskId;
        }
        
        @Override
        public RubidiumPlugin getOwner() {
            return owner;
        }
        
        @Override
        public boolean isCancelled() {
            return cancelled.get();
        }
        
        @Override
        public void cancel() {
            if (cancelled.compareAndSet(false, true)) {
                if (future != null) {
                    future.cancel(false);
                }
            }
        }
        
        @Override
        public boolean isSync() {
            return sync;
        }
    }
}
