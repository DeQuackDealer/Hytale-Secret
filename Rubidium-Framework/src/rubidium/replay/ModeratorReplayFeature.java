package rubidium.replay;

import rubidium.api.player.Player;
import rubidium.core.logging.RubidiumLogger;
import rubidium.core.scheduler.Scheduler;
import rubidium.qol.QoLFeature;

import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ModeratorReplayFeature extends QoLFeature {
    
    public record ReplayConfig(
        int targetFps,
        int captureRadius,
        Duration bufferDuration,
        Duration segmentDuration,
        boolean continuousMode,
        boolean recordOnSuspicion,
        boolean recordOnCombat,
        boolean recordOnReport,
        Path storageDir,
        long maxStorageBytes,
        long maxStoragePerPlayerBytes,
        Duration retentionPeriod,
        int compressionWorkers,
        double minTpsThreshold
    ) {
        public static ReplayConfig defaults(Path dataDir) {
            return new ReplayConfig(
                20,
                64,
                Duration.ofMinutes(5),
                Duration.ofSeconds(30),
                false,
                true,
                true,
                true,
                dataDir.resolve("replays"),
                10L * 1024 * 1024 * 1024,
                512L * 1024 * 1024,
                Duration.ofDays(7),
                2,
                15.0
            );
        }
    }
    
    private ReplayConfig config;
    private Scheduler scheduler;
    private long captureTaskId = -1;
    
    private ReplayFramePool framePool;
    private ReplayStorageWorker storageWorker;
    
    private final Map<UUID, ReplayBuffer> playerBuffers;
    private final Map<UUID, ReplaySession> activeSessions;
    private final Map<UUID, Long> lastCaptureTime;
    
    private Supplier<Collection<? extends Player>> playerProvider;
    private Supplier<Double> tpsProvider;
    
    private long tickCounter;
    private int tickInterval;
    
    private long totalFramesCaptured;
    private long totalSessionsCreated;
    private long droppedFramesDueToTps;
    
    public ModeratorReplayFeature(RubidiumLogger logger, Path dataDir) {
        super("moderator-replay", "Moderator Replay", logger);
        this.config = ReplayConfig.defaults(dataDir);
        this.playerBuffers = new ConcurrentHashMap<>();
        this.activeSessions = new ConcurrentHashMap<>();
        this.lastCaptureTime = new ConcurrentHashMap<>();
        this.tickCounter = 0;
        this.tickInterval = calculateTickInterval(config.targetFps());
    }
    
    public void setConfig(ReplayConfig config) {
        this.config = config;
        this.tickInterval = calculateTickInterval(config.targetFps());
        
        if (storageWorker != null) {
            storageWorker.stop();
            storageWorker = new ReplayStorageWorker(logger, 
                new ReplayStorageWorker.StorageConfig(
                    config.storageDir(),
                    config.maxStorageBytes(),
                    config.maxStoragePerPlayerBytes(),
                    config.retentionPeriod(),
                    config.compressionWorkers(),
                    100
                ));
            storageWorker.start();
        }
    }
    
    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }
    
    public void setPlayerProvider(Supplier<Collection<? extends Player>> provider) {
        this.playerProvider = provider;
    }
    
    public void setTpsProvider(Supplier<Double> provider) {
        this.tpsProvider = provider;
    }
    
    private int calculateTickInterval(int targetFps) {
        return Math.max(1, 20 / targetFps);
    }
    
    @Override
    protected void onEnable() {
        int bufferFrames = (int) (config.bufferDuration().toMillis() / (1000.0 / config.targetFps()));
        int poolSize = bufferFrames * 10;
        
        framePool = new ReplayFramePool(poolSize, poolSize * 2);
        
        storageWorker = new ReplayStorageWorker(logger,
            new ReplayStorageWorker.StorageConfig(
                config.storageDir(),
                config.maxStorageBytes(),
                config.maxStoragePerPlayerBytes(),
                config.retentionPeriod(),
                config.compressionWorkers(),
                100
            ));
        storageWorker.start();
        
        if (scheduler != null) {
            captureTaskId = scheduler.scheduleRepeating(
                "replay-capture",
                this::captureFrame,
                Duration.ofMillis(50),
                Duration.ofMillis(50),
                Scheduler.Priority.LOW
            );
        }
        
        logger.info("Moderator replay enabled: {}fps, {}m radius, {}min buffer",
            config.targetFps(), config.captureRadius(), config.bufferDuration().toMinutes());
    }
    
    @Override
    protected void onDisable() {
        if (captureTaskId >= 0 && scheduler != null) {
            scheduler.cancel(captureTaskId);
            captureTaskId = -1;
        }
        
        activeSessions.values().forEach(session -> {
            session.finalize();
            storageWorker.submitWrite(session, false);
        });
        activeSessions.clear();
        
        playerBuffers.values().forEach(ReplayBuffer::release);
        playerBuffers.clear();
        
        if (storageWorker != null) {
            storageWorker.stop();
            storageWorker = null;
        }
        
        if (framePool != null) {
            framePool.clear();
            framePool = null;
        }
        
        logger.info("Moderator replay disabled");
    }
    
    @Override
    public void tick() {
        tickCounter++;
    }
    
    private void captureFrame() {
        if (!isEnabled() || playerProvider == null) return;
        
        if (tickCounter % tickInterval != 0) return;
        
        if (tpsProvider != null && tpsProvider.get() < config.minTpsThreshold()) {
            droppedFramesDueToTps++;
            return;
        }
        
        long now = System.currentTimeMillis();
        
        for (Player player : playerProvider.get()) {
            UUID playerId = player.getUuid();
            
            ReplayBuffer buffer = playerBuffers.computeIfAbsent(playerId, id -> {
                int bufferFrames = (int) (config.bufferDuration().toMillis() / (1000.0 / config.targetFps()));
                return new ReplayBuffer(id, bufferFrames, framePool);
            });
            
            buffer.write(frame -> {
                frame.setTimestamp(now);
                frame.setPosition(player.getX(), player.getY(), player.getZ());
                frame.setRotation(player.getYaw(), player.getPitch());
                frame.setVelocity(player.getVelX(), player.getVelY(), player.getVelZ());
                
                frame.setHealth((float) player.getHealth());
                frame.setArmor((float) player.getArmor());
                
                frame.setOnGround(player.isOnGround());
                frame.setSprinting(player.isSprinting());
                frame.setSneaking(player.isSneaking());
                frame.setSwimming(player.isSwimming());
                frame.setFlying(player.isFlying());
                frame.setGliding(player.isGliding());
                
                frame.setHeldItemSlot((byte) player.getHeldItemSlot());
            });
            
            lastCaptureTime.put(playerId, now);
            totalFramesCaptured++;
            
            if (config.continuousMode()) {
                ReplaySession session = activeSessions.get(playerId);
                if (session != null) {
                    long sessionDuration = now - session.getStartTime().toEpochMilli();
                    if (sessionDuration >= config.segmentDuration().toMillis()) {
                        flushActiveSession(playerId);
                    }
                }
            }
        }
    }
    
    public void startRecording(UUID playerId, String playerName, ReplaySession.TriggerType trigger, String reason) {
        if (!isEnabled()) return;
        
        if (activeSessions.containsKey(playerId)) {
            logger.debug("Already recording player: {}", playerName);
            return;
        }
        
        ReplayBuffer buffer = playerBuffers.get(playerId);
        ReplaySession session = new ReplaySession(playerId, playerName, trigger, reason, config.storageDir());
        
        if (buffer != null && !buffer.isEmpty()) {
            ReplaySegment prerollSegment = buffer.toSegment(config.targetFps(), config.captureRadius());
            if (prerollSegment != null) {
                session.addSegment(prerollSegment);
            }
        }
        
        activeSessions.put(playerId, session);
        totalSessionsCreated++;
        
        logger.info("Started recording {} (trigger: {}, reason: {})", playerName, trigger, reason);
    }
    
    public void stopRecording(UUID playerId) {
        ReplaySession session = activeSessions.remove(playerId);
        if (session == null) return;
        
        ReplayBuffer buffer = playerBuffers.get(playerId);
        if (buffer != null && !buffer.isEmpty()) {
            ReplaySegment finalSegment = buffer.toSegment(config.targetFps(), config.captureRadius());
            if (finalSegment != null) {
                session.addSegment(finalSegment);
            }
        }
        
        storageWorker.submitWrite(session, true);
        
        logger.info("Stopped recording {} (session: {})", 
            session.getPlayerName(), session.getSessionId());
    }
    
    private void flushActiveSession(UUID playerId) {
        ReplaySession session = activeSessions.get(playerId);
        if (session == null) return;
        
        ReplayBuffer buffer = playerBuffers.get(playerId);
        if (buffer != null && !buffer.isEmpty()) {
            ReplaySegment segment = buffer.toSegment(config.targetFps(), config.captureRadius());
            if (segment != null) {
                session.addSegment(segment);
            }
        }
    }
    
    public void onPlayerJoin(Player player) {
        int bufferFrames = (int) (config.bufferDuration().toMillis() / (1000.0 / config.targetFps()));
        playerBuffers.put(player.getUuid(), new ReplayBuffer(player.getUuid(), bufferFrames, framePool));
        
        if (config.continuousMode()) {
            startRecording(player.getUuid(), player.getName(), 
                ReplaySession.TriggerType.CONTINUOUS, "Continuous recording");
        }
    }
    
    public void onPlayerLeave(Player player) {
        UUID playerId = player.getUuid();
        
        stopRecording(playerId);
        
        ReplayBuffer buffer = playerBuffers.remove(playerId);
        if (buffer != null) {
            buffer.release();
        }
        
        lastCaptureTime.remove(playerId);
    }
    
    public void onSuspiciousActivity(Player player, String reason) {
        if (!config.recordOnSuspicion()) return;
        startRecording(player.getUuid(), player.getName(), 
            ReplaySession.TriggerType.ANTICHEAT, reason);
    }
    
    public void onCombatStart(Player player, Player opponent) {
        if (!config.recordOnCombat()) return;
        startRecording(player.getUuid(), player.getName(),
            ReplaySession.TriggerType.COMBAT, "Combat with " + opponent.getName());
    }
    
    public void onPlayerReport(Player reported, Player reporter, String reason) {
        if (!config.recordOnReport()) return;
        startRecording(reported.getUuid(), reported.getName(),
            ReplaySession.TriggerType.REPORT, "Reported by " + reporter.getName() + ": " + reason);
    }
    
    public ReplayBuffer getBuffer(UUID playerId) {
        return playerBuffers.get(playerId);
    }
    
    public ReplaySession getActiveSession(UUID playerId) {
        return activeSessions.get(playerId);
    }
    
    public Set<UUID> getRecordingPlayers() {
        return Collections.unmodifiableSet(activeSessions.keySet());
    }
    
    public void purgePlayerData(UUID playerId) {
        stopRecording(playerId);
        ReplayBuffer buffer = playerBuffers.remove(playerId);
        if (buffer != null) {
            buffer.release();
        }
        storageWorker.requestPrune(playerId);
    }
    
    public ReplayConfig getConfig() { return config; }
    public long getTotalFramesCaptured() { return totalFramesCaptured; }
    public long getTotalSessionsCreated() { return totalSessionsCreated; }
    public long getDroppedFramesDueToTps() { return droppedFramesDueToTps; }
    public int getActiveBufferCount() { return playerBuffers.size(); }
    public int getActiveSessionCount() { return activeSessions.size(); }
    
    public double getFramePoolHitRate() {
        return framePool != null ? framePool.getHitRate() : 0;
    }
    
    @Override
    public String getDescription() {
        return String.format(
            "Records player actions at %dfps for moderator review. " +
            "Buffer: %dm, Radius: %d blocks, Storage: %s",
            config.targetFps(),
            config.bufferDuration().toMinutes(),
            config.captureRadius(),
            config.storageDir()
        );
    }
}
