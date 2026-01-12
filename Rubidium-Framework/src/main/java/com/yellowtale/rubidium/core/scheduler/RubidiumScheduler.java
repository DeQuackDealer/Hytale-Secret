package com.yellowtale.rubidium.core.scheduler;

import com.yellowtale.rubidium.core.metrics.MetricsRegistry;
import com.yellowtale.rubidium.core.performance.PerformanceBudgetManager;

import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tick-based task scheduler with async support.
 * 
 * Features:
 * - Synchronous tick-aligned task execution
 * - Asynchronous task execution with callbacks
 * - Deferred execution queue for non-critical tasks
 * - Priority-based task ordering
 * - Performance budget integration
 */
public final class RubidiumScheduler {
    
    private static final int TICKS_PER_SECOND = 20;
    private static final long TICK_DURATION_MS = 1000 / TICKS_PER_SECOND;
    private static final long TICK_DURATION_NANOS = TICK_DURATION_MS * 1_000_000;
    
    private final PerformanceBudgetManager performanceManager;
    private final MetricsRegistry metricsRegistry;
    
    private final AtomicLong currentTick = new AtomicLong(0);
    private final AtomicLong taskIdCounter = new AtomicLong(0);
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    private final Map<Long, ScheduledTask> tasks = new ConcurrentHashMap<>();
    private final Queue<ScheduledTask> tickQueue = new PriorityBlockingQueue<>();
    private final Queue<DeferredTask> deferredQueue = new ConcurrentLinkedQueue<>();
    
    private ExecutorService asyncExecutor;
    private ScheduledExecutorService tickExecutor;
    
    public RubidiumScheduler(PerformanceBudgetManager performanceManager, MetricsRegistry metricsRegistry) {
        this.performanceManager = performanceManager;
        this.metricsRegistry = metricsRegistry;
    }
    
    /**
     * Start the scheduler.
     */
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        
        asyncExecutor = Executors.newWorkStealingPool();
        tickExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Rubidium-Scheduler");
            t.setPriority(Thread.MAX_PRIORITY);
            return t;
        });
        
        tickExecutor.scheduleAtFixedRate(
            this::tick,
            0,
            TICK_DURATION_MS,
            TimeUnit.MILLISECONDS
        );
    }
    
    /**
     * Stop the scheduler.
     */
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        
        tickExecutor.shutdownNow();
        asyncExecutor.shutdownNow();
        
        tasks.clear();
        tickQueue.clear();
        deferredQueue.clear();
    }
    
    /**
     * Get the current tick number.
     */
    public long getCurrentTick() {
        return currentTick.get();
    }
    
    /**
     * Schedule a task to run on the next tick.
     */
    public TaskHandle runTask(String owner, Runnable task) {
        return runTaskLater(owner, task, 0);
    }
    
    /**
     * Schedule a task to run after a delay.
     * 
     * @param owner Module or component owning this task
     * @param task The task to run
     * @param delayTicks Delay in ticks before execution
     */
    public TaskHandle runTaskLater(String owner, Runnable task, long delayTicks) {
        long id = taskIdCounter.incrementAndGet();
        long executeTick = currentTick.get() + Math.max(0, delayTicks);
        
        ScheduledTask scheduled = new ScheduledTask(
            id,
            owner,
            task,
            executeTick,
            0,
            TaskPriority.NORMAL,
            false
        );
        
        tasks.put(id, scheduled);
        tickQueue.offer(scheduled);
        
        return new TaskHandle(id, this);
    }
    
    /**
     * Schedule a repeating task.
     * 
     * @param owner Module or component owning this task
     * @param task The task to run
     * @param delayTicks Initial delay in ticks
     * @param periodTicks Period between executions in ticks
     */
    public TaskHandle runTaskTimer(String owner, Runnable task, long delayTicks, long periodTicks) {
        long id = taskIdCounter.incrementAndGet();
        long executeTick = currentTick.get() + Math.max(0, delayTicks);
        
        ScheduledTask scheduled = new ScheduledTask(
            id,
            owner,
            task,
            executeTick,
            Math.max(1, periodTicks),
            TaskPriority.NORMAL,
            false
        );
        
        tasks.put(id, scheduled);
        tickQueue.offer(scheduled);
        
        return new TaskHandle(id, this);
    }
    
    /**
     * Schedule a task to run asynchronously.
     */
    public TaskHandle runTaskAsync(String owner, Runnable task) {
        long id = taskIdCounter.incrementAndGet();
        
        ScheduledTask scheduled = new ScheduledTask(
            id,
            owner,
            task,
            currentTick.get(),
            0,
            TaskPriority.NORMAL,
            true
        );
        
        tasks.put(id, scheduled);
        
        asyncExecutor.submit(() -> {
            try {
                long startNanos = System.nanoTime();
                task.run();
                long durationNanos = System.nanoTime() - startNanos;
                metricsRegistry.recordTaskExecution(owner, durationNanos);
            } finally {
                tasks.remove(id);
            }
        });
        
        return new TaskHandle(id, this);
    }
    
    /**
     * Schedule a task with a callback for the result.
     */
    public <T> CompletableFuture<T> runTaskAsync(String owner, Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        
        asyncExecutor.submit(() -> {
            try {
                long startNanos = System.nanoTime();
                T result = task.call();
                long durationNanos = System.nanoTime() - startNanos;
                metricsRegistry.recordTaskExecution(owner, durationNanos);
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }
    
    /**
     * Schedule a deferred task that runs when budget allows.
     * Deferred tasks are executed after all regular tick tasks.
     */
    public void defer(String owner, Runnable task, TaskPriority priority) {
        deferredQueue.offer(new DeferredTask(owner, task, priority));
    }
    
    /**
     * Cancel a scheduled task.
     */
    public boolean cancelTask(long taskId) {
        return tasks.remove(taskId) != null;
    }
    
    /**
     * Cancel all tasks owned by a module.
     */
    public int cancelTasks(String owner) {
        int cancelled = 0;
        var iterator = tasks.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().owner().equals(owner)) {
                iterator.remove();
                cancelled++;
            }
        }
        return cancelled;
    }
    
    private void tick() {
        long tickNumber = currentTick.incrementAndGet();
        long tickStartNanos = System.nanoTime();
        long budgetNanos = performanceManager.getTickBudgetNanos();
        
        performanceManager.resetTickBudgets();
        
        int tasksProcessed = 0;
        int tasksDeferred = 0;
        
        while (!tickQueue.isEmpty()) {
            ScheduledTask task = tickQueue.peek();
            if (task == null || task.executeTick() > tickNumber) {
                break;
            }
            
            tickQueue.poll();
            
            if (!tasks.containsKey(task.id())) {
                continue;
            }
            
            long elapsed = System.nanoTime() - tickStartNanos;
            if (elapsed > budgetNanos && task.priority() != TaskPriority.CRITICAL) {
                tickQueue.offer(task.withExecuteTick(tickNumber + 1));
                tasksDeferred++;
                continue;
            }
            
            try {
                long startNanos = System.nanoTime();
                task.task().run();
                long durationNanos = System.nanoTime() - startNanos;
                metricsRegistry.recordTaskExecution(task.owner(), durationNanos);
                performanceManager.recordExecutionTime(task.owner(), durationNanos);
                tasksProcessed++;
            } catch (Exception e) {
                metricsRegistry.counter("rubidium.scheduler.task.errors").increment();
            }
            
            if (task.periodTicks() > 0 && tasks.containsKey(task.id())) {
                ScheduledTask next = task.withExecuteTick(tickNumber + task.periodTicks());
                tickQueue.offer(next);
            } else {
                tasks.remove(task.id());
            }
        }
        
        long remainingNanos = budgetNanos - (System.nanoTime() - tickStartNanos);
        if (remainingNanos > 0) {
            processDeferred(Math.min(remainingNanos, budgetNanos / 4));
        }
        
        long tickDurationNanos = System.nanoTime() - tickStartNanos;
        metricsRegistry.recordTickDuration(tickDurationNanos);
        metricsRegistry.counter("rubidium.scheduler.tasks.processed").add(tasksProcessed);
        metricsRegistry.counter("rubidium.scheduler.tasks.deferred").add(tasksDeferred);
        
        if (tickDurationNanos > TICK_DURATION_NANOS) {
            performanceManager.reportTickOverrun(tickNumber, tickDurationNanos);
        }
    }
    
    private void processDeferred(long budgetNanos) {
        long startNanos = System.nanoTime();
        
        while (!deferredQueue.isEmpty()) {
            if (System.nanoTime() - startNanos > budgetNanos) {
                break;
            }
            
            DeferredTask task = deferredQueue.poll();
            if (task == null) break;
            
            try {
                task.task().run();
            } catch (Exception e) {
                // TODO: Log deferred task exception
            }
        }
    }
    
    private record ScheduledTask(
        long id,
        String owner,
        Runnable task,
        long executeTick,
        long periodTicks,
        TaskPriority priority,
        boolean async
    ) implements Comparable<ScheduledTask> {
        
        ScheduledTask withExecuteTick(long newTick) {
            return new ScheduledTask(id, owner, task, newTick, periodTicks, priority, async);
        }
        
        @Override
        public int compareTo(ScheduledTask other) {
            int tickCompare = Long.compare(this.executeTick, other.executeTick);
            if (tickCompare != 0) return tickCompare;
            return Integer.compare(other.priority.ordinal(), this.priority.ordinal());
        }
    }
    
    private record DeferredTask(String owner, Runnable task, TaskPriority priority) 
        implements Comparable<DeferredTask> {
        
        @Override
        public int compareTo(DeferredTask other) {
            return Integer.compare(other.priority.ordinal(), this.priority.ordinal());
        }
    }
}
