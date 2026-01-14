package rubidium.teleport.commands;

import rubidium.teleport.Location;
import rubidium.teleport.TeleportResult;
import rubidium.teleport.TeleportService;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public class WarpCommand {
    
    public static final String PERMISSION_USE = "rubidium.warp";
    public static final String PERMISSION_SET = "rubidium.warp.set";
    public static final String PERMISSION_DELETE = "rubidium.warp.delete";
    
    private final TeleportService teleportService;
    private Function<UUID, Location> playerLocationProvider;
    
    public WarpCommand(TeleportService teleportService) {
        this.teleportService = teleportService;
    }
    
    public void setPlayerLocationProvider(Function<UUID, Location> provider) {
        this.playerLocationProvider = provider;
    }
    
    public void teleportWarp(UUID sender, String warpName, Consumer<String> messageSender) {
        if (warpName.isEmpty()) {
            listWarps(messageSender);
            return;
        }
        
        TeleportResult result = teleportService.teleportToWarp(sender, warpName);
        
        if (result.success()) {
            messageSender.accept("Teleported to warp '" + warpName + "'");
        } else {
            messageSender.accept(result.message());
        }
    }
    
    public void setWarp(UUID sender, String warpName, Consumer<String> messageSender) {
        if (warpName.isEmpty()) {
            messageSender.accept("Usage: /setwarp <name>");
            return;
        }
        
        if (playerLocationProvider == null) {
            messageSender.accept("Location provider not configured.");
            return;
        }
        
        Location location = playerLocationProvider.apply(sender);
        if (location == null) {
            messageSender.accept("Could not get your location.");
            return;
        }
        
        TeleportResult result = teleportService.setWarp(warpName, location);
        messageSender.accept(result.message());
    }
    
    public void deleteWarp(String warpName, Consumer<String> messageSender) {
        if (warpName.isEmpty()) {
            messageSender.accept("Usage: /delwarp <name>");
            return;
        }
        
        if (teleportService.deleteWarp(warpName)) {
            messageSender.accept("Warp '" + warpName + "' deleted.");
        } else {
            messageSender.accept("Warp '" + warpName + "' not found.");
        }
    }
    
    public void listWarps(Consumer<String> messageSender) {
        Map<String, Location> warps = teleportService.getWarps();
        
        if (warps.isEmpty()) {
            messageSender.accept("No warps available.");
            return;
        }
        
        messageSender.accept("Available warps (" + warps.size() + "):");
        for (var entry : warps.entrySet()) {
            messageSender.accept("  " + entry.getKey() + ": " + entry.getValue().toReadableString());
        }
    }
}
