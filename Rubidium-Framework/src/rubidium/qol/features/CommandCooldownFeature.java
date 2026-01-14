package rubidium.qol.features;

import rubidium.core.logging.RubidiumLogger;
import rubidium.qol.QoLFeature;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CommandCooldownFeature extends QoLFeature {
    
    public record CooldownConfig(
        Duration defaultCooldown,
        Map<String, Duration> commandCooldowns,
        Set<String> exemptPermissions,
        String cooldownMessage
    ) {
        public static CooldownConfig defaults() {
            return new CooldownConfig(
                Duration.ofSeconds(1),
                new HashMap<>(Map.of(
                    "spawn", Duration.ofSeconds(30),
                    "home", Duration.ofSeconds(15),
                    "tp", Duration.ofSeconds(10),
                    "heal", Duration.ofMinutes(5)
                )),
                Set.of("admin", "moderator", "bypass.cooldown"),
                "&cPlease wait {remaining} before using this command again."
            );
        }
    }
    
    private record CooldownKey(String playerId, String command) {}
    
    private final Map<CooldownKey, Instant> cooldowns = new ConcurrentHashMap<>();
    private CooldownConfig config;
    
    public CommandCooldownFeature(RubidiumLogger logger) {
        super("command-cooldown", "Command Cooldowns", 
            "Rate limits commands to prevent spam and abuse",
            logger);
        this.config = CooldownConfig.defaults();
    }
    
    public void setConfig(CooldownConfig config) {
        this.config = config;
    }
    
    public CooldownConfig getConfig() {
        return config;
    }
    
    public void setCommandCooldown(String command, Duration duration) {
        Map<String, Duration> newCooldowns = new HashMap<>(config.commandCooldowns());
        newCooldowns.put(command.toLowerCase(), duration);
        this.config = new CooldownConfig(
            config.defaultCooldown(),
            newCooldowns,
            config.exemptPermissions(),
            config.cooldownMessage()
        );
    }
    
    public void removeCommandCooldown(String command) {
        Map<String, Duration> newCooldowns = new HashMap<>(config.commandCooldowns());
        newCooldowns.remove(command.toLowerCase());
        this.config = new CooldownConfig(
            config.defaultCooldown(),
            newCooldowns,
            config.exemptPermissions(),
            config.cooldownMessage()
        );
    }
    
    @Override
    protected void onEnable() {
        logger.debug("Command cooldowns enabled");
    }
    
    @Override
    protected void onDisable() {
        cooldowns.clear();
    }
    
    public record CooldownResult(boolean permitted, Duration remaining, String message) {
        public boolean allowed() {
            return permitted;
        }
        
        public static CooldownResult allow() {
            return new CooldownResult(true, Duration.ZERO, null);
        }
        
        public static CooldownResult denied(Duration remaining, String message) {
            return new CooldownResult(false, remaining, message);
        }
    }
    
    public CooldownResult checkCooldown(String playerId, String command, Set<String> playerPermissions) {
        if (!enabled) {
            return CooldownResult.allow();
        }
        
        if (playerPermissions != null) {
            for (String exemptPerm : config.exemptPermissions()) {
                if (playerPermissions.contains(exemptPerm)) {
                    return CooldownResult.allow();
                }
            }
        }
        
        String cmdLower = command.toLowerCase();
        Duration cooldownDuration = config.commandCooldowns()
            .getOrDefault(cmdLower, config.defaultCooldown());
        
        if (cooldownDuration.isZero() || cooldownDuration.isNegative()) {
            return CooldownResult.allow();
        }
        
        CooldownKey key = new CooldownKey(playerId, cmdLower);
        Instant lastUse = cooldowns.get(key);
        
        if (lastUse != null) {
            Duration elapsed = Duration.between(lastUse, Instant.now());
            if (elapsed.compareTo(cooldownDuration) < 0) {
                Duration remaining = cooldownDuration.minus(elapsed);
                String message = config.cooldownMessage()
                    .replace("{remaining}", formatDuration(remaining))
                    .replace("{command}", command);
                return CooldownResult.denied(remaining, message);
            }
        }
        
        cooldowns.put(key, Instant.now());
        return CooldownResult.allow();
    }
    
    public void resetCooldown(String playerId, String command) {
        cooldowns.remove(new CooldownKey(playerId, command.toLowerCase()));
    }
    
    public void resetAllCooldowns(String playerId) {
        cooldowns.keySet().removeIf(key -> key.playerId().equals(playerId));
    }
    
    public Optional<Duration> getRemainingCooldown(String playerId, String command) {
        CooldownKey key = new CooldownKey(playerId, command.toLowerCase());
        Instant lastUse = cooldowns.get(key);
        
        if (lastUse == null) {
            return Optional.empty();
        }
        
        Duration cooldownDuration = config.commandCooldowns()
            .getOrDefault(command.toLowerCase(), config.defaultCooldown());
        Duration elapsed = Duration.between(lastUse, Instant.now());
        
        if (elapsed.compareTo(cooldownDuration) >= 0) {
            return Optional.empty();
        }
        
        return Optional.of(cooldownDuration.minus(elapsed));
    }
    
    @Override
    public void tick() {
        if (!enabled) return;
        
        Instant now = Instant.now();
        Duration maxCooldown = config.commandCooldowns().values().stream()
            .max(Duration::compareTo)
            .orElse(config.defaultCooldown());
        
        cooldowns.entrySet().removeIf(entry -> 
            Duration.between(entry.getValue(), now).compareTo(maxCooldown) > 0
        );
    }
    
    private String formatDuration(Duration duration) {
        long seconds = duration.toSeconds();
        if (seconds < 60) {
            return seconds + "s";
        }
        long minutes = duration.toMinutes();
        long remainingSeconds = seconds % 60;
        if (remainingSeconds > 0) {
            return minutes + "m " + remainingSeconds + "s";
        }
        return minutes + "m";
    }
}
