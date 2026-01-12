package com.yellowtale.rubidium.api.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginLogger {
    
    private final Logger logger;
    private final String prefix;
    
    public PluginLogger(String pluginId) {
        this.logger = LoggerFactory.getLogger("Plugin:" + pluginId);
        this.prefix = "[" + pluginId + "] ";
    }
    
    public void info(String message) {
        logger.info(prefix + message);
    }
    
    public void info(String format, Object... args) {
        logger.info(prefix + format, args);
    }
    
    public void warn(String message) {
        logger.warn(prefix + message);
    }
    
    public void warn(String format, Object... args) {
        logger.warn(prefix + format, args);
    }
    
    public void error(String message) {
        logger.error(prefix + message);
    }
    
    public void error(String format, Object... args) {
        logger.error(prefix + format, args);
    }
    
    public void error(String message, Throwable throwable) {
        logger.error(prefix + message, throwable);
    }
    
    public void debug(String message) {
        logger.debug(prefix + message);
    }
    
    public void debug(String format, Object... args) {
        logger.debug(prefix + format, args);
    }
    
    public void trace(String message) {
        logger.trace(prefix + message);
    }
    
    public void trace(String format, Object... args) {
        logger.trace(prefix + format, args);
    }
    
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }
    
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }
}
