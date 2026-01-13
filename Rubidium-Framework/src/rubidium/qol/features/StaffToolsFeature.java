package rubidium.qol.features;

import rubidium.core.logging.RubidiumLogger;
import rubidium.qol.QoLFeature;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class StaffToolsFeature extends QoLFeature {
    
    public record StaffConfig(
        String vanishPermission,
        String godModePermission,
        String teleportPermission,
        String freezePermission,
        String spectatePermission,
        boolean silentJoin,
        boolean logStaffActions
    ) {
        public static StaffConfig defaults() {
            return new StaffConfig(
                "staff.vanish",
                "staff.godmode",
                "staff.teleport",
                "staff.freeze",
                "staff.spectate",
                true,
                true
            );
        }
    }
    
    public enum StaffMode {
        VANISH,
        GOD_MODE,
        FREEZE,
        SPECTATE
    }
    
    private final Map<String, Set<StaffMode>> playerModes = new ConcurrentHashMap<>();
    private final Map<String, String> spectateTargets = new ConcurrentHashMap<>();
    
    private StaffConfig config;
    private BiConsumer<String, String> teleportHandler;
    private Consumer<String> vanishHandler;
    private Consumer<String> unvanishHandler;
    
    public StaffToolsFeature(RubidiumLogger logger) {
        super("staff-tools", "Staff Tools", 
            "Provides vanish, god mode, freeze, teleport, and spectate for staff members",
            logger);
        this.config = StaffConfig.defaults();
    }
    
    public void setConfig(StaffConfig config) {
        this.config = config;
    }
    
    public StaffConfig getConfig() {
        return config;
    }
    
    public void setTeleportHandler(BiConsumer<String, String> handler) {
        this.teleportHandler = handler;
    }
    
    public void setVanishHandler(Consumer<String> handler) {
        this.vanishHandler = handler;
    }
    
    public void setUnvanishHandler(Consumer<String> handler) {
        this.unvanishHandler = handler;
    }
    
    @Override
    protected void onEnable() {
        logger.debug("Staff tools enabled");
    }
    
    @Override
    protected void onDisable() {
        playerModes.clear();
        spectateTargets.clear();
    }
    
    private Set<StaffMode> getPlayerModes(String playerId) {
        return playerModes.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet());
    }
    
    public boolean hasMode(String playerId, StaffMode mode) {
        Set<StaffMode> modes = playerModes.get(playerId);
        return modes != null && modes.contains(mode);
    }
    
    public void enableMode(String playerId, StaffMode mode) {
        getPlayerModes(playerId).add(mode);
        
        if (mode == StaffMode.VANISH && vanishHandler != null) {
            vanishHandler.accept(playerId);
        }
        
        if (config.logStaffActions()) {
            logger.info("Staff {} enabled {}", playerId, mode);
        }
    }
    
    public void disableMode(String playerId, StaffMode mode) {
        Set<StaffMode> modes = playerModes.get(playerId);
        if (modes != null) {
            modes.remove(mode);
            
            if (mode == StaffMode.VANISH && unvanishHandler != null) {
                unvanishHandler.accept(playerId);
            }
            
            if (mode == StaffMode.SPECTATE) {
                spectateTargets.remove(playerId);
            }
            
            if (config.logStaffActions()) {
                logger.info("Staff {} disabled {}", playerId, mode);
            }
        }
    }
    
    public void toggleMode(String playerId, StaffMode mode) {
        if (hasMode(playerId, mode)) {
            disableMode(playerId, mode);
        } else {
            enableMode(playerId, mode);
        }
    }
    
    public Set<StaffMode> getActiveModes(String playerId) {
        Set<StaffMode> modes = playerModes.get(playerId);
        return modes != null ? Collections.unmodifiableSet(modes) : Set.of();
    }
    
    public boolean isVanished(String playerId) {
        return hasMode(playerId, StaffMode.VANISH);
    }
    
    public boolean hasGodMode(String playerId) {
        return hasMode(playerId, StaffMode.GOD_MODE);
    }
    
    public boolean isFrozen(String playerId) {
        return hasMode(playerId, StaffMode.FREEZE);
    }
    
    public boolean isSpectating(String playerId) {
        return hasMode(playerId, StaffMode.SPECTATE);
    }
    
    public Set<String> getVanishedPlayers() {
        Set<String> vanished = new HashSet<>();
        for (Map.Entry<String, Set<StaffMode>> entry : playerModes.entrySet()) {
            if (entry.getValue().contains(StaffMode.VANISH)) {
                vanished.add(entry.getKey());
            }
        }
        return vanished;
    }
    
    public Set<String> getFrozenPlayers() {
        Set<String> frozen = new HashSet<>();
        for (Map.Entry<String, Set<StaffMode>> entry : playerModes.entrySet()) {
            if (entry.getValue().contains(StaffMode.FREEZE)) {
                frozen.add(entry.getKey());
            }
        }
        return frozen;
    }
    
    public void teleportTo(String staffId, String targetId) {
        if (!enabled) return;
        
        if (teleportHandler != null) {
            teleportHandler.accept(staffId, targetId);
        }
        
        if (config.logStaffActions()) {
            logger.info("Staff {} teleported to {}", staffId, targetId);
        }
    }
    
    public void startSpectating(String staffId, String targetId) {
        if (!enabled) return;
        
        enableMode(staffId, StaffMode.SPECTATE);
        spectateTargets.put(staffId, targetId);
        
        if (config.logStaffActions()) {
            logger.info("Staff {} started spectating {}", staffId, targetId);
        }
    }
    
    public void stopSpectating(String staffId) {
        disableMode(staffId, StaffMode.SPECTATE);
    }
    
    public Optional<String> getSpectateTarget(String staffId) {
        return Optional.ofNullable(spectateTargets.get(staffId));
    }
    
    public void onPlayerLeave(String playerId) {
        playerModes.remove(playerId);
        spectateTargets.remove(playerId);
        
        spectateTargets.entrySet().removeIf(entry -> entry.getValue().equals(playerId));
    }
    
    public String handleCommand(String sender, String command, String[] args, Set<String> permissions) {
        if (!enabled) {
            return "&cStaff tools are disabled.";
        }
        
        return switch (command.toLowerCase()) {
            case "vanish", "v" -> {
                if (!permissions.contains(config.vanishPermission())) {
                    yield "&cYou don't have permission to use vanish.";
                }
                toggleMode(sender, StaffMode.VANISH);
                yield isVanished(sender) ? "&aYou are now vanished." : "&aYou are now visible.";
            }
            case "god", "godmode" -> {
                if (!permissions.contains(config.godModePermission())) {
                    yield "&cYou don't have permission to use god mode.";
                }
                toggleMode(sender, StaffMode.GOD_MODE);
                yield hasGodMode(sender) ? "&aGod mode enabled." : "&cGod mode disabled.";
            }
            case "freeze" -> {
                if (!permissions.contains(config.freezePermission())) {
                    yield "&cYou don't have permission to freeze players.";
                }
                if (args.length < 1) yield "&cUsage: /freeze <player>";
                String target = args[0];
                toggleMode(target, StaffMode.FREEZE);
                yield isFrozen(target) ? "&a" + target + " has been frozen." : "&a" + target + " has been unfrozen.";
            }
            case "tp", "teleport" -> {
                if (!permissions.contains(config.teleportPermission())) {
                    yield "&cYou don't have permission to teleport.";
                }
                if (args.length < 1) yield "&cUsage: /tp <player>";
                teleportTo(sender, args[0]);
                yield "&aTeleported to " + args[0];
            }
            case "spectate", "spec" -> {
                if (!permissions.contains(config.spectatePermission())) {
                    yield "&cYou don't have permission to spectate.";
                }
                if (args.length < 1) {
                    if (isSpectating(sender)) {
                        stopSpectating(sender);
                        yield "&aStopped spectating.";
                    }
                    yield "&cUsage: /spectate <player> or /spectate to stop";
                }
                startSpectating(sender, args[0]);
                yield "&aNow spectating " + args[0];
            }
            case "staffmode", "sm" -> {
                Set<StaffMode> modes = getActiveModes(sender);
                if (modes.isEmpty()) {
                    yield "&7No staff modes active.";
                }
                yield "&7Active modes: " + modes.stream().map(Enum::name).reduce((a, b) -> a + ", " + b).orElse("");
            }
            default -> "&cUnknown staff command.";
        };
    }
}
