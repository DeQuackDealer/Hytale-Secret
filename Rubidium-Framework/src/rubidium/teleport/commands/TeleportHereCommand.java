package rubidium.teleport.commands;

import rubidium.teleport.TeleportResult;
import rubidium.teleport.TeleportService;

import java.util.UUID;
import java.util.function.Consumer;

public class TeleportHereCommand {
    
    public static final String PERMISSION = "rubidium.teleport.here";
    
    private final TeleportService teleportService;
    
    public TeleportHereCommand(TeleportService teleportService) {
        this.teleportService = teleportService;
    }
    
    public void execute(UUID sender, String[] args, Consumer<String> messageSender) {
        if (args.length == 0) {
            messageSender.accept("Usage: /tphere <player>");
            return;
        }
        
        TeleportResult result = teleportService.teleportHere(sender, args[0], false);
        messageSender.accept(result.message());
    }
}
