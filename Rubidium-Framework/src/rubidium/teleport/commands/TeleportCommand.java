package rubidium.teleport.commands;

import rubidium.core.logging.RubidiumLogger;
import rubidium.teleport.Location;
import rubidium.teleport.TeleportResult;
import rubidium.teleport.TeleportService;

import java.util.UUID;
import java.util.function.Consumer;

public class TeleportCommand {
    
    public static final String PERMISSION_SELF = "rubidium.teleport.self";
    public static final String PERMISSION_OTHERS = "rubidium.teleport.others";
    public static final String PERMISSION_COORDINATES = "rubidium.teleport.coordinates";
    public static final String PERMISSION_BYPASS_COOLDOWN = "rubidium.teleport.bypass.cooldown";
    
    private final TeleportService teleportService;
    private final RubidiumLogger logger;
    
    public TeleportCommand(TeleportService teleportService, RubidiumLogger logger) {
        this.teleportService = teleportService;
        this.logger = logger;
    }
    
    public void execute(UUID sender, String[] args, String currentWorld, 
            Consumer<String> messageSender, boolean hasPermission) {
        
        if (args.length == 0) {
            sendUsage(messageSender);
            return;
        }
        
        if (args.length == 1) {
            TeleportResult result = teleportService.teleportToPlayer(sender, args[0], false);
            messageSender.accept(formatResult(result));
            return;
        }
        
        if (args.length == 2) {
            TeleportResult result = teleportService.teleportPlayerToTarget(
                sender, args[0], false);
            messageSender.accept(formatResult(result));
            return;
        }
        
        if (args.length >= 3) {
            try {
                double x = parseCoordinate(args[0]);
                double y = parseCoordinate(args[1]);
                double z = parseCoordinate(args[2]);
                String world = args.length > 3 ? args[3] : currentWorld;
                
                TeleportResult result = teleportService.teleportToCoordinates(
                    sender, world, x, y, z, false);
                messageSender.accept(formatResult(result));
            } catch (NumberFormatException e) {
                messageSender.accept("Invalid coordinates. Use: /tp <x> <y> <z> [world]");
            }
            return;
        }
        
        sendUsage(messageSender);
    }
    
    private double parseCoordinate(String value) throws NumberFormatException {
        if (value.startsWith("~")) {
            return 0;
        }
        return Double.parseDouble(value);
    }
    
    private void sendUsage(Consumer<String> messageSender) {
        messageSender.accept("Usage:");
        messageSender.accept("  /tp <player> - Teleport to a player");
        messageSender.accept("  /tp <player> <target> - Teleport player to target");
        messageSender.accept("  /tp <x> <y> <z> [world] - Teleport to coordinates");
    }
    
    private String formatResult(TeleportResult result) {
        if (result.success()) {
            return "Teleported to " + result.destination().toReadableString();
        }
        return result.message();
    }
}
