package com.yellowtale.rubidium.api.scheduler;

import com.yellowtale.rubidium.api.RubidiumPlugin;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public interface TaskScheduler {
    
    Task runTask(RubidiumPlugin plugin, Runnable task);
    
    Task runTaskLater(RubidiumPlugin plugin, Runnable task, long delay, TimeUnit unit);
    
    Task runTaskTimer(RubidiumPlugin plugin, Runnable task, long delay, long period, TimeUnit unit);
    
    Task runTaskAsync(RubidiumPlugin plugin, Runnable task);
    
    Task runTaskLaterAsync(RubidiumPlugin plugin, Runnable task, long delay, TimeUnit unit);
    
    Task runTaskTimerAsync(RubidiumPlugin plugin, Runnable task, long delay, long period, TimeUnit unit);
    
    Task runOnMainThread(RubidiumPlugin plugin, Runnable task);
    
    void cancelTask(int taskId);
    
    void cancelTasks(RubidiumPlugin plugin);
    
    boolean isMainThread();
    
    int getCurrentTick();
    
    default Task runNextTick(RubidiumPlugin plugin, Runnable task) {
        return runTaskLater(plugin, task, 1, TimeUnit.MILLISECONDS);
    }
    
    default Task schedule(RubidiumPlugin plugin, Runnable task, ScheduleOptions options) {
        if (options.isAsync()) {
            if (options.getPeriod() > 0) {
                return runTaskTimerAsync(plugin, task, options.getDelay(), options.getPeriod(), options.getUnit());
            } else if (options.getDelay() > 0) {
                return runTaskLaterAsync(plugin, task, options.getDelay(), options.getUnit());
            } else {
                return runTaskAsync(plugin, task);
            }
        } else {
            if (options.getPeriod() > 0) {
                return runTaskTimer(plugin, task, options.getDelay(), options.getPeriod(), options.getUnit());
            } else if (options.getDelay() > 0) {
                return runTaskLater(plugin, task, options.getDelay(), options.getUnit());
            } else {
                return runTask(plugin, task);
            }
        }
    }
    
    interface Task {
        int getTaskId();
        RubidiumPlugin getOwner();
        boolean isCancelled();
        void cancel();
        boolean isSync();
    }
    
    class ScheduleOptions {
        private long delay = 0;
        private long period = -1;
        private TimeUnit unit = TimeUnit.MILLISECONDS;
        private boolean async = false;
        
        public static ScheduleOptions immediate() {
            return new ScheduleOptions();
        }
        
        public static ScheduleOptions delayed(long delay, TimeUnit unit) {
            return new ScheduleOptions().delay(delay).unit(unit);
        }
        
        public static ScheduleOptions repeating(long period, TimeUnit unit) {
            return new ScheduleOptions().period(period).unit(unit);
        }
        
        public ScheduleOptions delay(long delay) {
            this.delay = delay;
            return this;
        }
        
        public ScheduleOptions period(long period) {
            this.period = period;
            return this;
        }
        
        public ScheduleOptions unit(TimeUnit unit) {
            this.unit = unit;
            return this;
        }
        
        public ScheduleOptions async() {
            this.async = true;
            return this;
        }
        
        public long getDelay() { return delay; }
        public long getPeriod() { return period; }
        public TimeUnit getUnit() { return unit; }
        public boolean isAsync() { return async; }
    }
}
