package com.yellowtale.rubidium.core.scheduler;

/**
 * Priority levels for scheduled tasks.
 * Higher priority tasks are executed first within a tick.
 */
public enum TaskPriority {
    /**
     * Lowest priority. May be deferred to future ticks under load.
     */
    LOW,
    
    /**
     * Normal priority. Standard task execution.
     */
    NORMAL,
    
    /**
     * High priority. Preferred over normal tasks.
     */
    HIGH,
    
    /**
     * Critical priority. Always executed, never deferred.
     * Use sparingly for essential operations.
     */
    CRITICAL
}
