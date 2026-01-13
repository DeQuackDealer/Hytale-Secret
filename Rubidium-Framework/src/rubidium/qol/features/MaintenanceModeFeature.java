package rubidium.qol.features;

import rubidium.core.logging.RubidiumLogger;
import rubidium.qol.QoLFeature;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class MaintenanceModeFeature extends QoLFeature {
    
    public record MaintenanceConfig(
        String kickMessage,
        String motdLine1,
        String motdLine2,
        Set<String> bypassPermissions,
        boolean autoSchedule,
        String scheduleTime
    ) {
        public static MaintenanceConfig defaults() {
            return new MaintenanceConfig(
                "&c&lServer Maintenance\n\n&7The server is currently under maintenance.\n&7Please try again later.",
                "&c&lMaintenance Mode",
                "&7The server is currently under maintenance",
                Set.of("admin", "maintenance.bypass"),
                false,
                null
            );
        }
    }
    
    private final Set<String> whitelistedPlayers = ConcurrentHashMap.newKeySet();
    private MaintenanceConfig config;
    private boolean maintenanceActive = false;
    private Instant maintenanceStartTime;
    private String maintenanceReason;
    
    private Consumer<String> kickHandler;
    private Consumer<String> broadcastHandler;
    
    public MaintenanceModeFeature(RubidiumLogger logger) {
        super("maintenance-mode", "Maintenance Mode", 
            "Restricts server access during maintenance with bypass permissions",
            logger);
        this.config = MaintenanceConfig.defaults();
    }
    
    public void setConfig(MaintenanceConfig config) {
        this.config = config;
    }
    
    public MaintenanceConfig getConfig() {
        return config;
    }
    
    public void setKickHandler(Consumer<String> handler) {
        this.kickHandler = handler;
    }
    
    public void setBroadcastHandler(Consumer<String> handler) {
        this.broadcastHandler = handler;
    }
    
    @Override
    protected void onEnable() {
        logger.debug("Maintenance mode feature enabled");
    }
    
    @Override
    protected void onDisable() {
        if (maintenanceActive) {
            endMaintenance();
        }
    }
    
    public void startMaintenance(String reason) {
        maintenanceActive = true;
        maintenanceStartTime = Instant.now();
        maintenanceReason = reason;
        
        if (broadcastHandler != null) {
            String message = "&c&lMaintenance mode activated" + 
                (reason != null ? ": &7" + reason : "");
            broadcastHandler.accept(message);
        }
        
        logger.info("Maintenance mode started" + (reason != null ? ": " + reason : ""));
    }
    
    public void endMaintenance() {
        maintenanceActive = false;
        maintenanceStartTime = null;
        maintenanceReason = null;
        
        if (broadcastHandler != null) {
            broadcastHandler.accept("&a&lMaintenance mode ended. Server is now open!");
        }
        
        logger.info("Maintenance mode ended");
    }
    
    public boolean isMaintenanceActive() {
        return enabled && maintenanceActive;
    }
    
    public String getMaintenanceReason() {
        return maintenanceReason;
    }
    
    public Optional<Instant> getMaintenanceStartTime() {
        return Optional.ofNullable(maintenanceStartTime);
    }
    
    public void addWhitelistedPlayer(String playerId) {
        whitelistedPlayers.add(playerId);
        logger.debug("Added {} to maintenance whitelist", playerId);
    }
    
    public void removeWhitelistedPlayer(String playerId) {
        whitelistedPlayers.remove(playerId);
    }
    
    public Set<String> getWhitelistedPlayers() {
        return Collections.unmodifiableSet(whitelistedPlayers);
    }
    
    public record JoinResult(boolean allowed, String denyMessage) {
        public static JoinResult allowed() {
            return new JoinResult(true, null);
        }
        
        public static JoinResult denied(String message) {
            return new JoinResult(false, message);
        }
    }
    
    public JoinResult canPlayerJoin(String playerId, Set<String> permissions) {
        if (!enabled || !maintenanceActive) {
            return JoinResult.allowed();
        }
        
        if (whitelistedPlayers.contains(playerId)) {
            return JoinResult.allowed();
        }
        
        if (permissions != null) {
            for (String bypassPerm : config.bypassPermissions()) {
                if (permissions.contains(bypassPerm)) {
                    return JoinResult.allowed();
                }
            }
        }
        
        String message = config.kickMessage();
        if (maintenanceReason != null) {
            message = message.replace("{reason}", maintenanceReason);
        }
        
        return JoinResult.denied(message);
    }
    
    public void kickNonWhitelisted(Set<String> onlinePlayers, Map<String, Set<String>> playerPermissions) {
        if (!enabled || !maintenanceActive) return;
        
        for (String playerId : onlinePlayers) {
            Set<String> perms = playerPermissions.getOrDefault(playerId, Set.of());
            JoinResult result = canPlayerJoin(playerId, perms);
            
            if (!result.allowed() && kickHandler != null) {
                kickHandler.accept(playerId);
            }
        }
    }
    
    public List<String> getMaintenanceMotd() {
        if (!enabled || !maintenanceActive) {
            return List.of();
        }
        
        String line1 = config.motdLine1();
        String line2 = config.motdLine2();
        
        if (maintenanceReason != null) {
            line2 = line2.replace("{reason}", maintenanceReason);
        }
        
        return List.of(line1, line2);
    }
    
    public String handleCommand(String sender, String[] args) {
        if (args.length == 0) {
            return maintenanceActive ? 
                "Maintenance mode is &aactive&f" + (maintenanceReason != null ? ": " + maintenanceReason : "") :
                "Maintenance mode is &cinactive&f";
        }
        
        String subCommand = args[0].toLowerCase();
        
        return switch (subCommand) {
            case "on", "start" -> {
                String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : null;
                startMaintenance(reason);
                yield "Maintenance mode &aactivated" + (reason != null ? ": " + reason : "");
            }
            case "off", "end" -> {
                endMaintenance();
                yield "Maintenance mode &cdeactivated";
            }
            case "add" -> {
                if (args.length < 2) yield "Usage: /maintenance add <player>";
                addWhitelistedPlayer(args[1]);
                yield "Added " + args[1] + " to maintenance whitelist";
            }
            case "remove" -> {
                if (args.length < 2) yield "Usage: /maintenance remove <player>";
                removeWhitelistedPlayer(args[1]);
                yield "Removed " + args[1] + " from maintenance whitelist";
            }
            case "list" -> {
                if (whitelistedPlayers.isEmpty()) yield "No players in maintenance whitelist";
                yield "Whitelist: " + String.join(", ", whitelistedPlayers);
            }
            default -> """
                Maintenance Commands:
                /maintenance on [reason] - Enable maintenance
                /maintenance off - Disable maintenance
                /maintenance add <player> - Add to whitelist
                /maintenance remove <player> - Remove from whitelist
                /maintenance list - Show whitelisted players""";
        };
    }
}
