package com.yellowtale.rubidium.core.logging;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Structured logging system with per-module log levels.
 * 
 * Features:
 * - Hierarchical loggers
 * - Per-module log levels
 * - Structured log entries
 * - Async file writing
 * - Log rotation
 */
public final class LogManager {
    
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    private final Path logDirectory;
    private final Map<String, RubidiumLogger> loggers = new ConcurrentHashMap<>();
    private final Map<String, LogLevel> logLevels = new ConcurrentHashMap<>();
    private final BlockingQueue<LogEntry> logQueue = new LinkedBlockingQueue<>(10000);
    private final AtomicBoolean running = new AtomicBoolean(true);
    
    private volatile LogLevel defaultLevel = LogLevel.INFO;
    private PrintWriter currentWriter;
    private LocalDate currentDate;
    private Thread writerThread;
    
    public LogManager(Path logDirectory) {
        this.logDirectory = logDirectory;
        try {
            Files.createDirectories(logDirectory);
        } catch (IOException e) {
            System.err.println("Failed to create log directory: " + e.getMessage());
        }
        startWriterThread();
    }
    
    /**
     * Get or create a logger for a component.
     */
    public RubidiumLogger getLogger(String name) {
        return loggers.computeIfAbsent(name, n -> new RubidiumLoggerImpl(n, this));
    }
    
    /**
     * Set the log level for a specific logger.
     */
    public void setLogLevel(String name, LogLevel level) {
        logLevels.put(name, level);
    }
    
    /**
     * Set the default log level.
     */
    public void setDefaultLevel(LogLevel level) {
        this.defaultLevel = level;
    }
    
    /**
     * Get the effective log level for a logger.
     */
    LogLevel getEffectiveLevel(String name) {
        LogLevel level = logLevels.get(name);
        if (level != null) return level;
        
        int dot = name.lastIndexOf(':');
        if (dot > 0) {
            return getEffectiveLevel(name.substring(0, dot));
        }
        
        return defaultLevel;
    }
    
    /**
     * Queue a log entry for writing.
     */
    void log(LogEntry entry) {
        if (!logQueue.offer(entry)) {
            System.err.println("Log queue full, dropping: " + entry.message());
        }
    }
    
    /**
     * Shutdown the log manager.
     * Drains remaining log entries before closing.
     */
    public void shutdown() {
        running.set(false);
        
        while (!logQueue.isEmpty()) {
            try {
                LogEntry entry = logQueue.poll(10, TimeUnit.MILLISECONDS);
                if (entry != null) {
                    writeEntry(entry);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        if (writerThread != null) {
            writerThread.interrupt();
            try {
                writerThread.join(2000);
            } catch (InterruptedException ignored) {}
        }
        closeWriter();
    }
    
    private void startWriterThread() {
        writerThread = new Thread(this::writerLoop, "Rubidium-LogWriter");
        writerThread.setDaemon(true);
        writerThread.start();
    }
    
    private void writerLoop() {
        while (running.get() || !logQueue.isEmpty()) {
            try {
                LogEntry entry = logQueue.poll(100, TimeUnit.MILLISECONDS);
                if (entry != null) {
                    writeEntry(entry);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private void writeEntry(LogEntry entry) {
        ensureWriter(entry.timestamp().toLocalDate());
        
        String formatted = formatEntry(entry);
        
        PrintStream console = entry.level().ordinal() >= LogLevel.WARN.ordinal() 
            ? System.err : System.out;
        console.println(formatted);
        
        if (currentWriter != null) {
            currentWriter.println(formatted);
            currentWriter.flush();
        }
    }
    
    private String formatEntry(LogEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append(entry.timestamp().format(TIMESTAMP_FORMAT));
        sb.append(" [").append(entry.level().name()).append("] ");
        sb.append("[").append(entry.loggerName()).append("] ");
        sb.append(entry.message());
        
        if (entry.throwable() != null) {
            sb.append("\n");
            StringWriter sw = new StringWriter();
            entry.throwable().printStackTrace(new PrintWriter(sw));
            sb.append(sw);
        }
        
        return sb.toString();
    }
    
    private void ensureWriter(LocalDate date) {
        if (currentWriter != null && date.equals(currentDate)) {
            return;
        }
        
        closeWriter();
        currentDate = date;
        
        try {
            Path logFile = logDirectory.resolve("rubidium-" + date.format(FILE_DATE_FORMAT) + ".log");
            currentWriter = new PrintWriter(new BufferedWriter(
                new FileWriter(logFile.toFile(), true)
            ));
        } catch (IOException e) {
            System.err.println("Failed to open log file: " + e.getMessage());
        }
    }
    
    private void closeWriter() {
        if (currentWriter != null) {
            currentWriter.close();
            currentWriter = null;
        }
    }
    
    record LogEntry(
        LocalDateTime timestamp,
        LogLevel level,
        String loggerName,
        String message,
        Throwable throwable
    ) {}
}
