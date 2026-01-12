package com.yellowtale.rubidium.performance.scheduler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class TickScheduler {
    private static final Logger LOGGER = Logger.getLogger(TickScheduler.class.getName());
    
    private static final long TARGET_TPS = 20;
    private static final long TICK_TIME_NS = 1_000_000_000L / TARGET_TPS;
    private static final long MAX_TICK_TIME_NS = TICK_TIME_NS * 2;
    
    private final ExecutorService asyncExecutor;
    private final ScheduledExecutorService scheduler;
    private final PriorityQueue<ScheduledTask> taskQueue;
    private final ConcurrentHashMap<String, TaskBudget> taskBudgets;
    private final List<Consumer<TickContext>> tickHandlers;
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong currentTick = new AtomicLong(0);
    private final AtomicLong tickOverruns = new AtomicLong(0);
    private final AtomicLong totalTickTime = new AtomicLong(0);
    private final AtomicInteger skippedTicks = new AtomicInteger(0);
    
    private volatile double currentTps = 20.0;
    private volatile long lastTickStart = 0;
    private volatile long lastTickDuration = 0;
    
    private long[] tickTimes = new long[100];
    private int tickTimeIndex = 0;
    
    public TickScheduler() {
        this(Runtime.getRuntime().availableProcessors());
    }
    
    public TickScheduler(int asyncThreads) {
        this.asyncExecutor = Executors.newFixedThreadPool(asyncThreads, r -> {
            Thread t = new Thread(r, "Rubidium-Async-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        });
        
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Rubidium-Tick-Scheduler");
            t.setPriority(Thread.MAX_PRIORITY);
            return t;
        });
        
        this.taskQueue = new PriorityQueue<>(Comparator.comparingLong(t -> t.nextRun));
        this.taskBudgets = new ConcurrentHashMap<>();
        this.tickHandlers = new CopyOnWriteArrayList<>();
    }
    
    public void start() {
        if (running.compareAndSet(false, true)) {
            LOGGER.info("[RPAL] Tick scheduler starting (target TPS: " + TARGET_TPS + ")");
            lastTickStart = System.nanoTime();
            scheduler.scheduleAtFixedRate(this::runTick, 0, TICK_TIME_NS, TimeUnit.NANOSECONDS);
        }
    }
    
    public void stop() {
        if (running.compareAndSet(true, false)) {
            LOGGER.info("[RPAL] Tick scheduler stopping");
            scheduler.shutdown();
            asyncExecutor.shutdown();
            
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
                if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    asyncExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    private void runTick() {
        if (!running.get()) return;
        
        long tickStart = System.nanoTime();
        long tick = currentTick.incrementAndGet();
        long timeSinceLastTick = tickStart - lastTickStart;
        lastTickStart = tickStart;
        
        updateTps(timeSinceLastTick);
        
        TickContext context = new TickContext(tick, tickStart, currentTps, lastTickDuration);
        
        for (Consumer<TickContext> handler : tickHandlers) {
            try {
                handler.accept(context);
            } catch (Exception e) {
                LOGGER.warning("[RPAL] Tick handler error: " + e.getMessage());
            }
        }
        
        List<ScheduledTask> tasksToRun = new ArrayList<>();
        synchronized (taskQueue) {
            while (!taskQueue.isEmpty() && taskQueue.peek().nextRun <= tick) {
                tasksToRun.add(taskQueue.poll());
            }
        }
        
        long budgetRemaining = TICK_TIME_NS - (System.nanoTime() - tickStart);
        
        for (ScheduledTask task : tasksToRun) {
            if (budgetRemaining <= 0) {
                synchronized (taskQueue) {
                    task.nextRun = tick + 1;
                    taskQueue.offer(task);
                }
                continue;
            }
            
            TaskBudget budget = taskBudgets.get(task.group);
            if (budget != null && budget.isExhausted()) {
                synchronized (taskQueue) {
                    task.nextRun = tick + 1;
                    taskQueue.offer(task);
                }
                continue;
            }
            
            long taskStart = System.nanoTime();
            try {
                task.runnable.run();
            } catch (Exception e) {
                LOGGER.warning("[RPAL] Scheduled task '" + task.id + "' failed: " + e.getMessage());
            }
            long taskDuration = System.nanoTime() - taskStart;
            
            if (budget != null) {
                budget.consume(taskDuration);
            }
            
            if (task.period > 0) {
                synchronized (taskQueue) {
                    task.nextRun = tick + task.period;
                    taskQueue.offer(task);
                }
            }
            
            budgetRemaining -= taskDuration;
        }
        
        long tickDuration = System.nanoTime() - tickStart;
        lastTickDuration = tickDuration;
        totalTickTime.addAndGet(tickDuration);
        
        tickTimes[tickTimeIndex] = tickDuration;
        tickTimeIndex = (tickTimeIndex + 1) % tickTimes.length;
        
        if (tickDuration > MAX_TICK_TIME_NS) {
            tickOverruns.incrementAndGet();
        }
    }
    
    private void updateTps(long timeSinceLastTick) {
        if (timeSinceLastTick > 0) {
            double instantTps = 1_000_000_000.0 / timeSinceLastTick;
            currentTps = currentTps * 0.9 + instantTps * 0.1;
        }
    }
    
    public void registerTickHandler(Consumer<TickContext> handler) {
        tickHandlers.add(handler);
    }
    
    public void unregisterTickHandler(Consumer<TickContext> handler) {
        tickHandlers.remove(handler);
    }
    
    public TaskHandle schedule(String id, Runnable task, long delayTicks) {
        return schedule(id, null, task, delayTicks, 0);
    }
    
    public TaskHandle scheduleRepeating(String id, Runnable task, long delayTicks, long periodTicks) {
        return schedule(id, null, task, delayTicks, periodTicks);
    }
    
    public TaskHandle schedule(String id, String group, Runnable task, long delayTicks, long periodTicks) {
        ScheduledTask scheduled = new ScheduledTask(
            id,
            group,
            task,
            currentTick.get() + delayTicks,
            periodTicks
        );
        
        synchronized (taskQueue) {
            taskQueue.offer(scheduled);
        }
        
        return new TaskHandle(scheduled, this);
    }
    
    public void setTaskBudget(String group, long budgetNanosPerTick) {
        taskBudgets.put(group, new TaskBudget(group, budgetNanosPerTick));
    }
    
    public void removeTaskBudget(String group) {
        taskBudgets.remove(group);
    }
    
    public CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(task, asyncExecutor);
    }
    
    public <T> CompletableFuture<T> supplyAsync(Callable<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, asyncExecutor);
    }
    
    void cancelTask(ScheduledTask task) {
        synchronized (taskQueue) {
            taskQueue.remove(task);
        }
    }
    
    public SchedulerStats getStats() {
        long avgTickTime = 0;
        int validTicks = 0;
        for (long t : tickTimes) {
            if (t > 0) {
                avgTickTime += t;
                validTicks++;
            }
        }
        if (validTicks > 0) {
            avgTickTime /= validTicks;
        }
        
        return new SchedulerStats(
            currentTick.get(),
            currentTps,
            lastTickDuration,
            avgTickTime,
            tickOverruns.get(),
            skippedTicks.get(),
            taskQueue.size(),
            taskBudgets.size()
        );
    }
    
    public long getCurrentTick() {
        return currentTick.get();
    }
    
    public double getCurrentTps() {
        return currentTps;
    }
    
    public boolean isRunning() {
        return running.get();
    }
    
    private static final class ScheduledTask {
        final String id;
        final String group;
        final Runnable runnable;
        long nextRun;
        final long period;
        
        ScheduledTask(String id, String group, Runnable runnable, long nextRun, long period) {
            this.id = id;
            this.group = group;
            this.runnable = runnable;
            this.nextRun = nextRun;
            this.period = period;
        }
    }
    
    private static final class TaskBudget {
        final String group;
        final long budgetPerTick;
        final AtomicLong consumed = new AtomicLong();
        volatile long lastResetTick = 0;
        
        TaskBudget(String group, long budgetPerTick) {
            this.group = group;
            this.budgetPerTick = budgetPerTick;
        }
        
        void consume(long nanos) {
            consumed.addAndGet(nanos);
        }
        
        boolean isExhausted() {
            return consumed.get() >= budgetPerTick;
        }
        
        void reset(long tick) {
            if (tick > lastResetTick) {
                consumed.set(0);
                lastResetTick = tick;
            }
        }
    }
    
    public static final class TaskHandle {
        private final ScheduledTask task;
        private final TickScheduler scheduler;
        private volatile boolean cancelled = false;
        
        TaskHandle(ScheduledTask task, TickScheduler scheduler) {
            this.task = task;
            this.scheduler = scheduler;
        }
        
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
                scheduler.cancelTask(task);
            }
        }
        
        public boolean isCancelled() {
            return cancelled;
        }
        
        public String getId() {
            return task.id;
        }
    }
    
    public record TickContext(
        long tick,
        long startTimeNanos,
        double currentTps,
        long lastTickDurationNanos
    ) {
        public double tickProgress() {
            long elapsed = System.nanoTime() - startTimeNanos;
            return (double) elapsed / TICK_TIME_NS;
        }
        
        public boolean isLagging() {
            return currentTps < TARGET_TPS * 0.9;
        }
    }
    
    public record SchedulerStats(
        long currentTick,
        double currentTps,
        long lastTickNanos,
        long avgTickNanos,
        long tickOverruns,
        int skippedTicks,
        int pendingTasks,
        int taskBudgetGroups
    ) {
        public double tickUtilization() {
            return (double) avgTickNanos / TICK_TIME_NS;
        }
        
        public boolean isHealthy() {
            return currentTps >= TARGET_TPS * 0.95 && tickUtilization() < 0.9;
        }
    }
}
