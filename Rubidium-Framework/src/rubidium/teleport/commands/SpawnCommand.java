package rubidium.teleport.commands;

import rubidium.teleport.Location;
import rubidium.teleport.TeleportResult;
import rubidium.teleport.TeleportService;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public class SpawnCommand {
    
    public static final String PERMISSION_USE = "rubidium.spawn";
    public static final String PERMISSION_SET = "rubidium.spawn.set";
    
    private final TeleportService teleportService;
    private Function<UUID, Location> playerLocationProvider;
    
    public SpawnCommand(TeleportService teleportService) {
        this.teleportService = teleportService;
    }
    
    public void setPlayerLocationProvider(Function<UUID, Location> provider) {
        this.playerLocationProvider = provider;
    }
    
    public void execute(UUID sender, Consumer<String> messageSender) {
        TeleportResult result = teleportService.teleportToSpawn(sender);
        
        if (result.success()) {
            messageSender.accept("Teleported to spawn.");
        } else {
            messageSender.accept(result.message());
        }
    }
    
    public void setSpawn(UUID sender, Consumer<String> messageSender) {
        if (playerLocationProvider == null) {
            messageSender.accept("Location provider not configured.");
            return;
        }
        
        Location location = playerLocationProvider.apply(sender);
        if (location == null) {
            messageSender.accept("Could not get your location.");
            return;
        }
        
        teleportService.setSpawn(location);
        messageSender.accept("Spawn location set to " + location.toReadableString());
    }
}
