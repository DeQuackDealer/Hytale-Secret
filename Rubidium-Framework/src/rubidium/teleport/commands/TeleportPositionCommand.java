package rubidium.teleport.commands;

import rubidium.teleport.TeleportResult;
import rubidium.teleport.TeleportService;

import java.util.UUID;
import java.util.function.Consumer;

public class TeleportPositionCommand {
    
    public static final String PERMISSION = "rubidium.teleport.position";
    
    private final TeleportService teleportService;
    
    public TeleportPositionCommand(TeleportService teleportService) {
        this.teleportService = teleportService;
    }
    
    public void execute(UUID sender, String[] args, String currentWorld, 
            Consumer<String> messageSender) {
        
        if (args.length < 3) {
            messageSender.accept("Usage: /tppos <x> <y> <z> [world]");
            return;
        }
        
        try {
            double x = Double.parseDouble(args[0]);
            double y = Double.parseDouble(args[1]);
            double z = Double.parseDouble(args[2]);
            String world = args.length > 3 ? args[3] : currentWorld;
            
            TeleportResult result = teleportService.teleportToCoordinates(
                sender, world, x, y, z, false);
            
            if (result.success()) {
                messageSender.accept("Teleported to " + 
                    String.format("%.1f, %.1f, %.1f in %s", x, y, z, world));
            } else {
                messageSender.accept(result.message());
            }
        } catch (NumberFormatException e) {
            messageSender.accept("Invalid coordinates. Please enter valid numbers.");
        }
    }
}
