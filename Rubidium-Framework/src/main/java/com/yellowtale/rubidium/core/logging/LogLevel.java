package com.yellowtale.rubidium.core.logging;

/**
 * Log levels for the Rubidium logging system.
 */
public enum LogLevel {
    /**
     * Finest level of detail, typically only for development.
     */
    TRACE,
    
    /**
     * Detailed information for debugging.
     */
    DEBUG,
    
    /**
     * General informational messages.
     */
    INFO,
    
    /**
     * Warning messages for potentially problematic situations.
     */
    WARN,
    
    /**
     * Error messages for failures.
     */
    ERROR,
    
    /**
     * Logging is disabled.
     */
    OFF
}
