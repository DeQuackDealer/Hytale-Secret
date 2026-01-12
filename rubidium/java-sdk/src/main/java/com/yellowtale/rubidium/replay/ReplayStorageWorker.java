package com.yellowtale.rubidium.replay;

import com.yellowtale.rubidium.core.logging.RubidiumLogger;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public final class ReplayStorageWorker {
    
    public record StorageConfig(
        Path baseDir,
        long maxStorageBytes,
        long maxStoragePerPlayerBytes,
        Duration retentionPeriod,
        int compressionWorkers,
        int maxQueueSize
    ) {
        public StorageConfig {
            if (compressionWorkers < 1) compressionWorkers = 1;
            if (maxQueueSize < 1) maxQueueSize = 100;
        }
        
        public static StorageConfig defaults(Path baseDir) {
            return new StorageConfig(
                baseDir,
                10L * 1024 * 1024 * 1024,
                512L * 1024 * 1024,
                Duration.ofDays(7),
                2,
                100
            );
        }
    }
    
    public record WriteTask(ReplaySession session, boolean finalize) {}
    public record PruneTask(UUID playerId) {}
    
    private final RubidiumLogger logger;
    private final StorageConfig config;
    private final ExecutorService executor;
    private final BlockingQueue<WriteTask> writeQueue;
    private final BlockingQueue<PruneTask> pruneQueue;
    
    private final AtomicLong totalBytesWritten;
    private final AtomicLong totalSessionsWritten;
    private final AtomicLong totalSessionsPruned;
    private final Map<UUID, Long> playerStorageUsage;
    
    private volatile boolean running;
    
    public ReplayStorageWorker(RubidiumLogger logger, StorageConfig config) {
        this.logger = logger;
        this.config = config;
        this.executor = Executors.newFixedThreadPool(config.compressionWorkers(), r -> {
            Thread t = new Thread(r, "ReplayStorage-Worker");
            t.setDaemon(true);
            return t;
        });
        this.writeQueue = new LinkedBlockingQueue<>(config.maxQueueSize());
        this.pruneQueue = new LinkedBlockingQueue<>();
        this.totalBytesWritten = new AtomicLong(0);
        this.totalSessionsWritten = new AtomicLong(0);
        this.totalSessionsPruned = new AtomicLong(0);
        this.playerStorageUsage = new ConcurrentHashMap<>();
        this.running = false;
    }
    
    public void start() {
        if (running) return;
        running = true;
        
        try {
            Files.createDirectories(config.baseDir());
        } catch (IOException e) {
            logger.error("Failed to create replay storage directory", e);
        }
        
        executor.submit(this::writeWorker);
        executor.submit(this::pruneWorker);
        
        calculateStorageUsage();
        
        logger.info("Replay storage worker started with {} workers", config.compressionWorkers());
    }
    
    public void stop() {
        if (!running) return;
        running = false;
        
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("Replay storage worker stopped");
    }
    
    public boolean submitWrite(ReplaySession session, boolean finalize) {
        if (!running) return false;
        return writeQueue.offer(new WriteTask(session, finalize));
    }
    
    public void requestPrune(UUID playerId) {
        pruneQueue.offer(new PruneTask(playerId));
    }
    
    private void writeWorker() {
        while (running) {
            try {
                WriteTask task = writeQueue.poll(100, TimeUnit.MILLISECONDS);
                if (task == null) continue;
                
                processWriteTask(task);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error in write worker", e);
            }
        }
    }
    
    private void pruneWorker() {
        while (running) {
            try {
                PruneTask task = pruneQueue.poll(1, TimeUnit.SECONDS);
                if (task != null) {
                    prunePlayer(task.playerId());
                }
                
                if (pruneQueue.isEmpty()) {
                    pruneExpiredSessions();
                    enforceStorageLimits();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error in prune worker", e);
            }
        }
    }
    
    private void processWriteTask(WriteTask task) {
        try {
            if (task.finalize()) {
                task.session().finalize();
            }
            
            long bytes = task.session().getTotalBytes();
            totalBytesWritten.addAndGet(bytes);
            totalSessionsWritten.incrementAndGet();
            
            playerStorageUsage.merge(task.session().getPlayerId(), bytes, Long::sum);
            
            logger.debug("Wrote replay session {} ({} bytes, {} frames)",
                task.session().getSessionId(),
                bytes,
                task.session().getTotalFrames());
                
        } catch (Exception e) {
            logger.error("Failed to write replay session", e);
        }
    }
    
    private void prunePlayer(UUID playerId) {
        Path playerDir = config.baseDir().resolve("players").resolve(playerId.toString());
        if (!Files.exists(playerDir)) return;
        
        try (Stream<Path> sessions = Files.list(playerDir)) {
            sessions.forEach(sessionDir -> {
                try {
                    deleteSession(sessionDir);
                    totalSessionsPruned.incrementAndGet();
                } catch (IOException e) {
                    logger.error("Failed to delete session: {}", sessionDir, e);
                }
            });
            
            Files.deleteIfExists(playerDir);
            playerStorageUsage.remove(playerId);
            
            logger.info("Pruned all replay data for player {}", playerId);
        } catch (IOException e) {
            logger.error("Failed to prune player data: {}", playerId, e);
        }
    }
    
    private void pruneExpiredSessions() {
        Instant cutoff = Instant.now().minus(config.retentionPeriod());
        
        try (Stream<Path> players = Files.list(config.baseDir())) {
            players.filter(Files::isDirectory).forEach(playerDir -> {
                try (Stream<Path> sessions = Files.list(playerDir)) {
                    sessions.filter(Files::isDirectory).forEach(sessionDir -> {
                        try {
                            Path metadata = sessionDir.resolve("session.properties");
                            if (Files.exists(metadata)) {
                                Instant created = Files.getLastModifiedTime(metadata).toInstant();
                                if (created.isBefore(cutoff)) {
                                    deleteSession(sessionDir);
                                    totalSessionsPruned.incrementAndGet();
                                }
                            }
                        } catch (IOException e) {
                            logger.error("Failed to check session age", e);
                        }
                    });
                } catch (IOException e) {
                    logger.error("Failed to list sessions", e);
                }
            });
        } catch (IOException e) {
            logger.error("Failed to prune expired sessions", e);
        }
    }
    
    private void enforceStorageLimits() {
        long totalUsage = playerStorageUsage.values().stream().mapToLong(Long::longValue).sum();
        
        if (totalUsage > config.maxStorageBytes()) {
            logger.warn("Storage limit exceeded: {} / {} bytes", totalUsage, config.maxStorageBytes());
            
            playerStorageUsage.entrySet().stream()
                .filter(e -> e.getValue() > config.maxStoragePerPlayerBytes())
                .forEach(e -> {
                    pruneOldestSessions(e.getKey(), 
                        e.getValue() - config.maxStoragePerPlayerBytes());
                });
        }
    }
    
    private void pruneOldestSessions(UUID playerId, long bytesToFree) {
        Path playerDir = config.baseDir().resolve("players").resolve(playerId.toString());
        if (!Files.exists(playerDir)) return;
        
        try (Stream<Path> sessions = Files.list(playerDir)) {
            List<Path> sortedSessions = sessions
                .filter(Files::isDirectory)
                .sorted((a, b) -> {
                    try {
                        return Files.getLastModifiedTime(a).compareTo(Files.getLastModifiedTime(b));
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .toList();
            
            long freed = 0;
            for (Path session : sortedSessions) {
                if (freed >= bytesToFree) break;
                try {
                    freed += getDirectorySize(session);
                    deleteSession(session);
                    totalSessionsPruned.incrementAndGet();
                } catch (IOException e) {
                    logger.error("Failed to delete session", e);
                }
            }
            
            logger.info("Pruned {} bytes from player {}", freed, playerId);
        } catch (IOException e) {
            logger.error("Failed to prune oldest sessions", e);
        }
    }
    
    private void deleteSession(Path sessionDir) throws IOException {
        try (Stream<Path> files = Files.walk(sessionDir)) {
            files.sorted(Comparator.reverseOrder())
                 .forEach(path -> {
                     try {
                         Files.delete(path);
                     } catch (IOException e) {
                         logger.error("Failed to delete: {}", path, e);
                     }
                 });
        }
    }
    
    private long getDirectorySize(Path dir) throws IOException {
        try (Stream<Path> files = Files.walk(dir)) {
            return files.filter(Files::isRegularFile)
                        .mapToLong(p -> {
                            try {
                                return Files.size(p);
                            } catch (IOException e) {
                                return 0;
                            }
                        })
                        .sum();
        }
    }
    
    private void calculateStorageUsage() {
        try (Stream<Path> players = Files.list(config.baseDir())) {
            players.filter(Files::isDirectory).forEach(playerDir -> {
                try {
                    UUID playerId = UUID.fromString(playerDir.getFileName().toString());
                    long usage = getDirectorySize(playerDir);
                    playerStorageUsage.put(playerId, usage);
                } catch (Exception e) {
                    // Not a player directory
                }
            });
        } catch (IOException e) {
            logger.error("Failed to calculate storage usage", e);
        }
    }
    
    public long getTotalBytesWritten() { return totalBytesWritten.get(); }
    public long getTotalSessionsWritten() { return totalSessionsWritten.get(); }
    public long getTotalSessionsPruned() { return totalSessionsPruned.get(); }
    public int getQueueSize() { return writeQueue.size(); }
    public Map<UUID, Long> getPlayerStorageUsage() { return Collections.unmodifiableMap(playerStorageUsage); }
}
