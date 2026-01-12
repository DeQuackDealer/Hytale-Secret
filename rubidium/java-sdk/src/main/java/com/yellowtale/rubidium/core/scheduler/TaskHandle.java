package com.yellowtale.rubidium.core.scheduler;

/**
 * Handle for a scheduled task that allows cancellation.
 */
public final class TaskHandle {
    
    private final long taskId;
    private final RubidiumScheduler scheduler;
    
    TaskHandle(long taskId, RubidiumScheduler scheduler) {
        this.taskId = taskId;
        this.scheduler = scheduler;
    }
    
    /**
     * Get the unique task ID.
     */
    public long getTaskId() {
        return taskId;
    }
    
    /**
     * Cancel this task.
     * @return true if the task was cancelled, false if already completed or not found
     */
    public boolean cancel() {
        return scheduler.cancelTask(taskId);
    }
}
