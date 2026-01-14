package rubidium.teleport.commands;

import rubidium.teleport.Location;
import rubidium.teleport.TeleportResult;
import rubidium.teleport.TeleportService;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HomeCommand {
    
    public static final String PERMISSION_USE = "rubidium.home";
    public static final String PERMISSION_SET = "rubidium.home.set";
    public static final String PERMISSION_DELETE = "rubidium.home.delete";
    public static final String PERMISSION_MULTIPLE = "rubidium.home.multiple";
    
    private final TeleportService teleportService;
    private Function<UUID, Location> playerLocationProvider;
    private int maxHomes = 3;
    
    public HomeCommand(TeleportService teleportService) {
        this.teleportService = teleportService;
    }
    
    public void setPlayerLocationProvider(Function<UUID, Location> provider) {
        this.playerLocationProvider = provider;
    }
    
    public void setMaxHomes(int max) {
        this.maxHomes = max;
    }
    
    public void teleportHome(UUID sender, String homeName, Consumer<String> messageSender) {
        String name = homeName.isEmpty() ? "home" : homeName;
        TeleportResult result = teleportService.teleportToHome(sender, name);
        
        if (result.success()) {
            messageSender.accept("Teleported to home '" + name + "'");
        } else {
            messageSender.accept(result.message());
        }
    }
    
    public void setHome(UUID sender, String homeName, boolean hasMultiplePermission,
            Consumer<String> messageSender) {
        
        if (playerLocationProvider == null) {
            messageSender.accept("Location provider not configured.");
            return;
        }
        
        String name = homeName.isEmpty() ? "home" : homeName;
        Map<String, Location> homes = teleportService.getHomes(sender);
        
        if (!hasMultiplePermission && !homes.isEmpty() && !homes.containsKey(name.toLowerCase())) {
            messageSender.accept("You can only have one home. Delete your existing home first.");
            return;
        }
        
        if (homes.size() >= maxHomes && !homes.containsKey(name.toLowerCase())) {
            messageSender.accept("You have reached the maximum number of homes (" + maxHomes + ").");
            return;
        }
        
        Location location = playerLocationProvider.apply(sender);
        if (location == null) {
            messageSender.accept("Could not get your location.");
            return;
        }
        
        TeleportResult result = teleportService.setHome(sender, name, location);
        messageSender.accept(result.message());
    }
    
    public void deleteHome(UUID sender, String homeName, Consumer<String> messageSender) {
        String name = homeName.isEmpty() ? "home" : homeName;
        
        if (teleportService.deleteHome(sender, name)) {
            messageSender.accept("Home '" + name + "' deleted.");
        } else {
            messageSender.accept("Home '" + name + "' not found.");
        }
    }
    
    public void listHomes(UUID sender, Consumer<String> messageSender) {
        Map<String, Location> homes = teleportService.getHomes(sender);
        
        if (homes.isEmpty()) {
            messageSender.accept("You have no homes set. Use /sethome to create one.");
            return;
        }
        
        messageSender.accept("Your homes (" + homes.size() + "/" + maxHomes + "):");
        for (var entry : homes.entrySet()) {
            messageSender.accept("  " + entry.getKey() + ": " + entry.getValue().toReadableString());
        }
    }
}
