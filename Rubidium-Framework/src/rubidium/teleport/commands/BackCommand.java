package rubidium.teleport.commands;

import rubidium.teleport.TeleportResult;
import rubidium.teleport.TeleportService;

import java.util.UUID;
import java.util.function.Consumer;

public class BackCommand {
    
    public static final String PERMISSION = "rubidium.teleport.back";
    
    private final TeleportService teleportService;
    
    public BackCommand(TeleportService teleportService) {
        this.teleportService = teleportService;
    }
    
    public void execute(UUID sender, Consumer<String> messageSender) {
        TeleportResult result = teleportService.back(sender);
        
        if (result.success()) {
            messageSender.accept("Teleported to previous location: " + 
                result.destination().toReadableString());
        } else {
            messageSender.accept(result.message());
        }
    }
}
