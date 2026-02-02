package rubidium.anticheat;

import rubidium.api.anticheat.AnticheatService;
import rubidium.api.anticheat.CombatSnapshot;
import rubidium.api.anticheat.Finding;
import rubidium.api.anticheat.Finding.FindingLevel;
import rubidium.api.anticheat.Finding.FindingType;
import rubidium.api.anticheat.MovementSnapshot;
import rubidium.api.player.Player;
import rubidium.api.scheduler.SchedulerAPI;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public class AnticheatServiceImpl implements AnticheatService {
    
    private static final Logger LOGGER = Logger.getLogger("RubidiumAnticheat");
    private static AnticheatServiceImpl instance;
    
    private volatile boolean enabled = true;
    private final Map<UUID, PlayerViolationData> playerData = new ConcurrentHashMap<>();
    private final List<Finding> recentFindings = new CopyOnWriteArrayList<>();
    private final AnticheatConfig config;
    
    private static final int MAX_FINDINGS = 1000;
    
    public AnticheatServiceImpl() {
        this.config = new AnticheatConfig();
        instance = this;
        LOGGER.info("[Anticheat] Anticheat service initialized");
    }
    
    public static AnticheatServiceImpl getInstance() {
        if (instance == null) {
            instance = new AnticheatServiceImpl();
        }
        return instance;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        LOGGER.info("[Anticheat] Anticheat " + (enabled ? "enabled" : "disabled"));
    }
    
    @Override
    public void processMovement(Player player, MovementSnapshot snapshot) {
        if (!enabled) return;
        
        UUID playerId = player.getUUID();
        PlayerViolationData data = playerData.computeIfAbsent(playerId, k -> new PlayerViolationData());
        
        if (data.lastMovement != null && !snapshot.isTeleporting()) {
            double dx = snapshot.getX() - data.lastMovement.getX();
            double dy = snapshot.getY() - data.lastMovement.getY();
            double dz = snapshot.getZ() - data.lastMovement.getZ();
            
            long timeDelta = snapshot.getTimestamp() - data.lastMovement.getTimestamp();
            if (timeDelta <= 0) timeDelta = 50;
            
            double horizontalSpeed = Math.sqrt(dx * dx + dz * dz) / (timeDelta / 1000.0);
            double verticalSpeed = Math.abs(dy) / (timeDelta / 1000.0);
            
            if (horizontalSpeed > config.getMaxHorizontalSpeed()) {
                Finding finding = createFinding(
                    playerId,
                    FindingType.SPEED_HACK,
                    FindingLevel.LIKELY,
                    "Excessive horizontal speed: " + String.format("%.2f", horizontalSpeed) + " blocks/s",
                    "speed=" + horizontalSpeed
                );
                addFinding(finding, data);
            }
            
            if (verticalSpeed > config.getMaxVerticalSpeed() && dy > 0 && !snapshot.isOnGround()) {
                if (!snapshot.isGliding() && !data.wasFlying) {
                    Finding finding = createFinding(
                        playerId,
                        FindingType.FLY_HACK,
                        FindingLevel.LIKELY,
                        "Possible fly hack: " + String.format("%.2f", verticalSpeed) + " blocks/s upward",
                        "verticalSpeed=" + verticalSpeed
                    );
                    addFinding(finding, data);
                }
            }
            
            if (!snapshot.isOnGround() && !snapshot.isGliding() && !data.wasFlying) {
                data.airTicks++;
                if (data.airTicks > config.getMaxAirTicks() && !snapshot.isInWater()) {
                    Finding finding = createFinding(
                        playerId,
                        FindingType.FLY_HACK,
                        FindingLevel.SUSPICIOUS,
                        "Excessive air time: " + data.airTicks + " ticks",
                        "airTicks=" + data.airTicks
                    );
                    addFinding(finding, data);
                }
            } else {
                data.airTicks = 0;
            }
            
            if (snapshot.isOnGround() && data.lastMovement.isOnGround()) {
                if (dy > 0.5 && data.lastVerticalVelocity <= 0) {
                    Finding finding = createFinding(
                        playerId,
                        FindingType.INVALID_MOVEMENT,
                        FindingLevel.SUSPICIOUS,
                        "Invalid ground-to-air transition",
                        "dy=" + dy
                    );
                    addFinding(finding, data);
                }
            }
            
            data.lastVerticalVelocity = dy / (timeDelta / 1000.0);
        }
        
        data.lastMovement = snapshot;
        data.wasFlying = player.isFlying();
    }
    
    @Override
    public void processCombat(Player player, CombatSnapshot snapshot) {
        if (!enabled || !snapshot.isAttack()) return;
        
        UUID playerId = player.getUUID();
        PlayerViolationData data = playerData.computeIfAbsent(playerId, k -> new PlayerViolationData());
        
        if (snapshot.getDistanceToTarget().isPresent()) {
            double distance = snapshot.getDistanceToTarget().get();
            if (distance > config.getMaxAttackDistance()) {
                Finding finding = createFinding(
                    playerId,
                    FindingType.REACH,
                    FindingLevel.LIKELY,
                    "Attack distance too far: " + String.format("%.2f", distance) + " blocks",
                    "distance=" + distance
                );
                addFinding(finding, data);
            }
        }
        
        if (snapshot.getAngleToTarget().isPresent()) {
            double angle = snapshot.getAngleToTarget().get();
            if (angle > config.getMaxAttackAngle()) {
                Finding finding = createFinding(
                    playerId,
                    FindingType.KILLAURA,
                    FindingLevel.LIKELY,
                    "Attack angle suspicious: " + String.format("%.1f", angle) + " degrees",
                    "angle=" + angle
                );
                addFinding(finding, data);
            }
        }
        
        data.recentAttacks.add(snapshot.getTimestamp());
        data.recentAttacks.removeIf(t -> System.currentTimeMillis() - t > 1000);
        
        if (data.recentAttacks.size() > config.getMaxCps()) {
            Finding finding = createFinding(
                playerId,
                FindingType.HIGH_CPS,
                FindingLevel.SUSPICIOUS,
                "Excessive CPS: " + data.recentAttacks.size(),
                "cps=" + data.recentAttacks.size()
            );
            addFinding(finding, data);
        }
    }
    
    private Finding createFinding(UUID playerId, FindingType type, FindingLevel level, String description, String data) {
        return new Finding(
            UUID.randomUUID(),
            playerId,
            type,
            level,
            description,
            data,
            Instant.now(),
            SchedulerAPI.getTickCount()
        );
    }
    
    private void addFinding(Finding finding, PlayerViolationData data) {
        recentFindings.add(0, finding);
        data.findings.add(0, finding);
        data.violationCount++;
        
        if (recentFindings.size() > MAX_FINDINGS) {
            recentFindings.remove(recentFindings.size() - 1);
        }
        if (data.findings.size() > 100) {
            data.findings.remove(data.findings.size() - 1);
        }
        
        LOGGER.warning("[Anticheat] " + finding.getType() + " - " + finding.getDescription() + 
                      " (Player: " + finding.getPlayerId() + ")");
    }
    
    @Override
    public List<Finding> getRecentFindings(int count) {
        return recentFindings.subList(0, Math.min(count, recentFindings.size()));
    }
    
    @Override
    public List<Finding> getPlayerFindings(UUID playerId, int count) {
        PlayerViolationData data = playerData.get(playerId);
        if (data == null) return Collections.emptyList();
        return data.findings.subList(0, Math.min(count, data.findings.size()));
    }
    
    @Override
    public int getPlayerViolationCount(UUID playerId) {
        PlayerViolationData data = playerData.get(playerId);
        return data != null ? data.violationCount : 0;
    }
    
    @Override
    public boolean shouldKickPlayer(UUID playerId) {
        PlayerViolationData data = playerData.get(playerId);
        if (data == null) return false;
        return data.violationCount >= config.getViolationsBeforeKick();
    }
    
    @Override
    public void clearPlayerData(UUID playerId) {
        playerData.remove(playerId);
    }
    
    @Override
    public void reloadConfig() {
        config.reload();
        LOGGER.info("[Anticheat] Configuration reloaded");
    }
    
    public AnticheatConfig getConfig() {
        return config;
    }
    
    private static class PlayerViolationData {
        MovementSnapshot lastMovement;
        int violationCount = 0;
        int airTicks = 0;
        double lastVerticalVelocity = 0;
        boolean wasFlying = false;
        final List<Finding> findings = new CopyOnWriteArrayList<>();
        final List<Long> recentAttacks = new CopyOnWriteArrayList<>();
    }
    
    public static class AnticheatConfig {
        private double maxHorizontalSpeed = 10.0;
        private double maxVerticalSpeed = 4.0;
        private double maxAttackDistance = 6.0;
        private double maxAttackAngle = 90.0;
        private int maxCps = 20;
        private int maxAirTicks = 80;
        private int violationsBeforeKick = 50;
        
        public double getMaxHorizontalSpeed() { return maxHorizontalSpeed; }
        public void setMaxHorizontalSpeed(double speed) { this.maxHorizontalSpeed = speed; }
        
        public double getMaxVerticalSpeed() { return maxVerticalSpeed; }
        public void setMaxVerticalSpeed(double speed) { this.maxVerticalSpeed = speed; }
        
        public double getMaxAttackDistance() { return maxAttackDistance; }
        public void setMaxAttackDistance(double distance) { this.maxAttackDistance = distance; }
        
        public double getMaxAttackAngle() { return maxAttackAngle; }
        public void setMaxAttackAngle(double angle) { this.maxAttackAngle = angle; }
        
        public int getMaxCps() { return maxCps; }
        public void setMaxCps(int cps) { this.maxCps = cps; }
        
        public int getMaxAirTicks() { return maxAirTicks; }
        public void setMaxAirTicks(int ticks) { this.maxAirTicks = ticks; }
        
        public int getViolationsBeforeKick() { return violationsBeforeKick; }
        public void setViolationsBeforeKick(int count) { this.violationsBeforeKick = count; }
        
        public void reload() {}
    }
}
