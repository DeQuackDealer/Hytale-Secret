package rubidium.teleport.commands;

import rubidium.teleport.TeleportResult;
import rubidium.teleport.TeleportService;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TeleportAllCommand {
    
    public static final String PERMISSION = "rubidium.teleport.all";
    
    private final TeleportService teleportService;
    private Supplier<Collection<UUID>> onlinePlayersProvider;
    
    public TeleportAllCommand(TeleportService teleportService) {
        this.teleportService = teleportService;
    }
    
    public void setOnlinePlayersProvider(Supplier<Collection<UUID>> provider) {
        this.onlinePlayersProvider = provider;
    }
    
    public void execute(UUID sender, String[] args, Consumer<String> messageSender) {
        if (onlinePlayersProvider == null) {
            messageSender.accept("Teleport all is not configured.");
            return;
        }
        
        Collection<UUID> players = onlinePlayersProvider.get();
        if (players.isEmpty()) {
            messageSender.accept("No players to teleport.");
            return;
        }
        
        List<TeleportResult> results = teleportService.teleportAll(sender, players, true);
        
        int success = (int) results.stream().filter(TeleportResult::success).count();
        messageSender.accept("Teleported " + success + " player(s) to your location.");
    }
}
