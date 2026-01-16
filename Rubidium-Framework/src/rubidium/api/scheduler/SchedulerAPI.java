package rubidium.api.scheduler;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public final class SchedulerAPI {
    
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private static final Map<UUID, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
    private static final Map<String, UUID> namedTasks = new ConcurrentHashMap<>();
    private static long tickCount = 0;
    private static final List<Consumer<Long>> tickListeners = new CopyOnWriteArrayList<>();
    
    private SchedulerAPI() {}
    
    public static UUID runLater(Runnable task, long delayTicks) {
        UUID id = UUID.randomUUID();
        ScheduledFuture<?> future = scheduler.schedule(task, delayTicks * 50, TimeUnit.MILLISECONDS);
        tasks.put(id, future);
        return id;
    }
    
    public static UUID runLater(String name, Runnable task, long delayTicks) {
        cancel(name);
        UUID id = runLater(task, delayTicks);
        namedTasks.put(name, id);
        return id;
    }
    
    public static UUID runTimer(Runnable task, long delayTicks, long periodTicks) {
        UUID id = UUID.randomUUID();
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
            task, 
            delayTicks * 50, 
            periodTicks * 50, 
            TimeUnit.MILLISECONDS
        );
        tasks.put(id, future);
        return id;
    }
    
    public static UUID runTimer(String name, Runnable task, long delayTicks, long periodTicks) {
        cancel(name);
        UUID id = runTimer(task, delayTicks, periodTicks);
        namedTasks.put(name, id);
        return id;
    }
    
    public static UUID runTimerWithDelay(Runnable task, long initialDelay, long periodTicks) {
        return runTimer(task, initialDelay, periodTicks);
    }
    
    public static UUID runAsync(Runnable task) {
        UUID id = UUID.randomUUID();
        CompletableFuture.runAsync(task).whenComplete((v, e) -> tasks.remove(id));
        return id;
    }
    
    public static <T> CompletableFuture<T> supplyAsync(Callable<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }
    
    public static void cancel(UUID taskId) {
        ScheduledFuture<?> future = tasks.remove(taskId);
        if (future != null) {
            future.cancel(false);
        }
    }
    
    public static void cancel(String name) {
        UUID id = namedTasks.remove(name);
        if (id != null) {
            cancel(id);
        }
    }
    
    public static void cancelAll() {
        for (ScheduledFuture<?> future : tasks.values()) {
            future.cancel(false);
        }
        tasks.clear();
        namedTasks.clear();
    }
    
    public static boolean isRunning(UUID taskId) {
        ScheduledFuture<?> future = tasks.get(taskId);
        return future != null && !future.isDone() && !future.isCancelled();
    }
    
    public static boolean isRunning(String name) {
        UUID id = namedTasks.get(name);
        return id != null && isRunning(id);
    }
    
    public static int getActiveTaskCount() {
        return (int) tasks.values().stream()
            .filter(f -> !f.isDone() && !f.isCancelled())
            .count();
    }
    
    public static void tick() {
        tickCount++;
        for (Consumer<Long> listener : tickListeners) {
            try {
                listener.accept(tickCount);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void onTick(Consumer<Long> listener) {
        tickListeners.add(listener);
    }
    
    public static void removeTickListener(Consumer<Long> listener) {
        tickListeners.remove(listener);
    }
    
    public static long getTickCount() {
        return tickCount;
    }
    
    public static void shutdown() {
        cancelAll();
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public static TaskChain chain() {
        return new TaskChain();
    }
    
    public static class TaskChain {
        private final List<ChainedTask> tasks = new ArrayList<>();
        
        public TaskChain delay(long ticks) {
            tasks.add(new ChainedTask(ChainedTask.Type.DELAY, null, ticks));
            return this;
        }
        
        public TaskChain sync(Runnable task) {
            tasks.add(new ChainedTask(ChainedTask.Type.SYNC, task, 0));
            return this;
        }
        
        public TaskChain async(Runnable task) {
            tasks.add(new ChainedTask(ChainedTask.Type.ASYNC, task, 0));
            return this;
        }
        
        public TaskChain repeat(Runnable task, int times, long intervalTicks) {
            for (int i = 0; i < times; i++) {
                sync(task);
                if (i < times - 1) delay(intervalTicks);
            }
            return this;
        }
        
        public void execute() {
            executeNext(0);
        }
        
        private void executeNext(int index) {
            if (index >= tasks.size()) return;
            
            ChainedTask task = tasks.get(index);
            
            switch (task.type()) {
                case DELAY -> runLater(() -> executeNext(index + 1), task.ticks());
                case SYNC -> {
                    if (task.task() != null) task.task().run();
                    executeNext(index + 1);
                }
                case ASYNC -> runAsync(() -> {
                    if (task.task() != null) task.task().run();
                    executeNext(index + 1);
                });
            }
        }
        
        private record ChainedTask(Type type, Runnable task, long ticks) {
            enum Type { DELAY, SYNC, ASYNC }
        }
    }
    
    public static Cooldown createCooldown(long durationTicks) {
        return new Cooldown(durationTicks);
    }
    
    public static class Cooldown {
        private final long durationMs;
        private final Map<Object, Long> cooldowns = new ConcurrentHashMap<>();
        
        public Cooldown(long durationTicks) {
            this.durationMs = durationTicks * 50;
        }
        
        public boolean isOnCooldown(Object key) {
            Long expiry = cooldowns.get(key);
            if (expiry == null) return false;
            if (System.currentTimeMillis() >= expiry) {
                cooldowns.remove(key);
                return false;
            }
            return true;
        }
        
        public void setCooldown(Object key) {
            cooldowns.put(key, System.currentTimeMillis() + durationMs);
        }
        
        public boolean tryUse(Object key) {
            if (isOnCooldown(key)) return false;
            setCooldown(key);
            return true;
        }
        
        public long getRemainingTicks(Object key) {
            Long expiry = cooldowns.get(key);
            if (expiry == null) return 0;
            long remaining = expiry - System.currentTimeMillis();
            return remaining > 0 ? remaining / 50 : 0;
        }
        
        public void clear(Object key) {
            cooldowns.remove(key);
        }
        
        public void clearAll() {
            cooldowns.clear();
        }
    }
}
