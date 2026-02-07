package com.hypixel.hytale.logger;

import java.util.logging.Logger;

public class HytaleLogger {
    
    private final Logger logger;
    
    private HytaleLogger(String name) {
        this.logger = Logger.getLogger(name);
    }
    
    public static HytaleLogger forEnclosingClass() {
        String callerClass = Thread.currentThread().getStackTrace()[2].getClassName();
        return new HytaleLogger(callerClass);
    }
    
    public static HytaleLogger forName(String name) {
        return new HytaleLogger(name);
    }
    
    public LogEntry atInfo() {
        return new LogEntry(logger, java.util.logging.Level.INFO);
    }
    
    public LogEntry atWarning() {
        return new LogEntry(logger, java.util.logging.Level.WARNING);
    }
    
    public LogEntry atSevere() {
        return new LogEntry(logger, java.util.logging.Level.SEVERE);
    }
    
    public LogEntry atFine() {
        return new LogEntry(logger, java.util.logging.Level.FINE);
    }
    
    public static class LogEntry {
        private final Logger logger;
        private final java.util.logging.Level level;
        
        LogEntry(Logger logger, java.util.logging.Level level) {
            this.logger = logger;
            this.level = level;
        }
        
        public void log(String message) {
            logger.log(level, message);
        }
        
        public void log(String message, Object... args) {
            logger.log(level, String.format(message, args));
        }
    }
}
