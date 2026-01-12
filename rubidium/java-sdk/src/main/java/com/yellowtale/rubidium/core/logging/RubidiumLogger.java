package com.yellowtale.rubidium.core.logging;

/**
 * Logger interface for Rubidium components.
 */
public interface RubidiumLogger {
    
    /**
     * Get the logger name.
     */
    String getName();
    
    /**
     * Check if trace level is enabled.
     */
    boolean isTraceEnabled();
    
    /**
     * Check if debug level is enabled.
     */
    boolean isDebugEnabled();
    
    /**
     * Check if info level is enabled.
     */
    boolean isInfoEnabled();
    
    /**
     * Log a trace message.
     */
    void trace(String message);
    
    /**
     * Log a trace message with arguments.
     */
    void trace(String message, Object... args);
    
    /**
     * Log a debug message.
     */
    void debug(String message);
    
    /**
     * Log a debug message with arguments.
     */
    void debug(String message, Object... args);
    
    /**
     * Log an info message.
     */
    void info(String message);
    
    /**
     * Log an info message with arguments.
     */
    void info(String message, Object... args);
    
    /**
     * Log a warning message.
     */
    void warn(String message);
    
    /**
     * Log a warning message with arguments.
     */
    void warn(String message, Object... args);
    
    /**
     * Log a warning message with throwable.
     */
    void warn(String message, Throwable throwable);
    
    /**
     * Log an error message.
     */
    void error(String message);
    
    /**
     * Log an error message with arguments.
     */
    void error(String message, Object... args);
    
    /**
     * Log an error message with throwable.
     */
    void error(String message, Throwable throwable);
}
