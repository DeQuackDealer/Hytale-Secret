package rubidium.hytale.api.logging;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Plugin logger with console and file output.
 */
public class PluginLogger {
    
    private final String name;
    private final Path logFile;
    private LogLevel level = LogLevel.INFO;
    
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    
    public enum LogLevel {
        DEBUG(0, "DEBUG"),
        INFO(1, "INFO"),
        WARN(2, "WARN"),
        ERROR(3, "ERROR");
        
        private final int priority;
        private final String label;
        
        LogLevel(int priority, String label) {
            this.priority = priority;
            this.label = label;
        }
        
        public int getPriority() {
            return priority;
        }
        
        public String getLabel() {
            return label;
        }
    }
    
    public PluginLogger(String name) {
        this.name = name;
        
        Path logsDir = Paths.get("logs", name);
        try {
            Files.createDirectories(logsDir);
        } catch (IOException ignored) {}
        
        String date = DATE_FORMAT.format(new Date());
        this.logFile = logsDir.resolve(date + ".log");
    }
    
    public void setLevel(LogLevel level) {
        this.level = level;
    }
    
    public void debug(String message, Object... args) {
        log(LogLevel.DEBUG, message, args);
    }
    
    public void info(String message, Object... args) {
        log(LogLevel.INFO, message, args);
    }
    
    public void warn(String message, Object... args) {
        log(LogLevel.WARN, message, args);
    }
    
    public void error(String message, Object... args) {
        log(LogLevel.ERROR, message, args);
    }
    
    public void error(String message, Throwable throwable) {
        log(LogLevel.ERROR, message + ": " + throwable.getMessage());
        throwable.printStackTrace();
    }
    
    private void log(LogLevel level, String message, Object... args) {
        if (level.getPriority() < this.level.getPriority()) {
            return;
        }
        
        String formatted = formatMessage(message, args);
        String timestamp = TIME_FORMAT.format(new Date());
        String logLine = String.format("[%s] [%s/%s]: %s", 
            timestamp, name, level.getLabel(), formatted);
        
        System.out.println(logLine);
        
        try (FileWriter writer = new FileWriter(logFile.toFile(), true)) {
            writer.write(logLine + "\n");
        } catch (IOException ignored) {}
    }
    
    private String formatMessage(String message, Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }
        
        String result = message;
        for (Object arg : args) {
            int index = result.indexOf("{}");
            if (index >= 0) {
                result = result.substring(0, index) + arg + result.substring(index + 2);
            }
        }
        return result;
    }
}
