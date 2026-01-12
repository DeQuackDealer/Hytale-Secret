package com.yellowtale.rubidium.core.logging;

import java.time.LocalDateTime;

/**
 * Default implementation of RubidiumLogger.
 */
final class RubidiumLoggerImpl implements RubidiumLogger {
    
    private final String name;
    private final LogManager logManager;
    
    RubidiumLoggerImpl(String name, LogManager logManager) {
        this.name = name;
        this.logManager = logManager;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean isTraceEnabled() {
        return isLevelEnabled(LogLevel.TRACE);
    }
    
    @Override
    public boolean isDebugEnabled() {
        return isLevelEnabled(LogLevel.DEBUG);
    }
    
    @Override
    public boolean isInfoEnabled() {
        return isLevelEnabled(LogLevel.INFO);
    }
    
    private boolean isLevelEnabled(LogLevel level) {
        return logManager.getEffectiveLevel(name).ordinal() <= level.ordinal();
    }
    
    @Override
    public void trace(String message) {
        log(LogLevel.TRACE, message, null);
    }
    
    @Override
    public void trace(String message, Object... args) {
        log(LogLevel.TRACE, format(message, args), null);
    }
    
    @Override
    public void debug(String message) {
        log(LogLevel.DEBUG, message, null);
    }
    
    @Override
    public void debug(String message, Object... args) {
        log(LogLevel.DEBUG, format(message, args), null);
    }
    
    @Override
    public void info(String message) {
        log(LogLevel.INFO, message, null);
    }
    
    @Override
    public void info(String message, Object... args) {
        log(LogLevel.INFO, format(message, args), null);
    }
    
    @Override
    public void warn(String message) {
        log(LogLevel.WARN, message, null);
    }
    
    @Override
    public void warn(String message, Object... args) {
        log(LogLevel.WARN, format(message, args), null);
    }
    
    @Override
    public void warn(String message, Throwable throwable) {
        log(LogLevel.WARN, message, throwable);
    }
    
    @Override
    public void error(String message) {
        log(LogLevel.ERROR, message, null);
    }
    
    @Override
    public void error(String message, Object... args) {
        log(LogLevel.ERROR, format(message, args), null);
    }
    
    @Override
    public void error(String message, Throwable throwable) {
        log(LogLevel.ERROR, message, throwable);
    }
    
    private void log(LogLevel level, String message, Throwable throwable) {
        if (!isLevelEnabled(level)) {
            return;
        }
        
        logManager.log(new LogManager.LogEntry(
            LocalDateTime.now(),
            level,
            name,
            message,
            throwable
        ));
    }
    
    private String format(String message, Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }
        
        StringBuilder result = new StringBuilder();
        int argIndex = 0;
        int i = 0;
        
        while (i < message.length()) {
            if (i < message.length() - 1 && message.charAt(i) == '{' && message.charAt(i + 1) == '}') {
                if (argIndex < args.length) {
                    result.append(args[argIndex++]);
                } else {
                    result.append("{}");
                }
                i += 2;
            } else {
                result.append(message.charAt(i));
                i++;
            }
        }
        
        return result.toString();
    }
}
