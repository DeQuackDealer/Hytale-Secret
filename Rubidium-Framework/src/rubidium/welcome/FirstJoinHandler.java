package rubidium.welcome;

import rubidium.hytale.api.player.Player;
import rubidium.hytale.api.event.PlayerJoinEvent;
import rubidium.hytale.adapter.EventAdapter;

import java.io.*;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

public class FirstJoinHandler {
    
    private static final Logger logger = Logger.getLogger("Rubidium");
    private static final String WEBSITE_URL = "dequackdealer.replit.app";
    private static final String DISCORD_CONTACT = "dequackdea1er";
    
    private static FirstJoinHandler instance;
    private final Set<UUID> seenPlayers = new HashSet<>();
    private final Path dataFile;
    
    private FirstJoinHandler() {
        this.dataFile = Paths.get("rubidium", "data", "seen_players.dat");
        loadSeenPlayers();
    }
    
    public static FirstJoinHandler getInstance() {
        if (instance == null) {
            instance = new FirstJoinHandler();
        }
        return instance;
    }
    
    public static void register() {
        FirstJoinHandler handler = getInstance();
        
        EventAdapter.getInstance().registerListener(PlayerJoinEvent.class, event -> {
            handler.onPlayerJoin(event);
        });
        
        logger.info("First-join welcome message handler registered");
    }
    
    private void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUuid();
        
        if (!seenPlayers.contains(playerId)) {
            sendWelcomeMessage(player);
            
            seenPlayers.add(playerId);
            saveSeenPlayers();
        }
    }
    
    private void sendWelcomeMessage(Player player) {
        player.sendMessage("");
        player.sendMessage("\u00A76\u00A7l\u2605 \u00A7eWelcome to a Rubidium-powered server! \u00A76\u00A7l\u2605");
        player.sendMessage("");
        player.sendMessage("\u00A77If you want to install \u00A7bRubidium \u00A77for your server, visit:");
        player.sendMessage("\u00A7a\u27A4 \u00A7f" + WEBSITE_URL);
        player.sendMessage("");
        player.sendMessage("\u00A77For inquiries or hiring, contact via Discord:");
        player.sendMessage("\u00A79\u27A4 \u00A7f" + DISCORD_CONTACT);
        player.sendMessage("");
    }
    
    private void loadSeenPlayers() {
        try {
            if (Files.exists(dataFile)) {
                try (BufferedReader reader = Files.newBufferedReader(dataFile)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty()) {
                            try {
                                seenPlayers.add(UUID.fromString(line));
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                    }
                }
                logger.fine("Loaded " + seenPlayers.size() + " seen players");
            }
        } catch (IOException e) {
            logger.warning("Failed to load seen players: " + e.getMessage());
        }
    }
    
    private void saveSeenPlayers() {
        try {
            Files.createDirectories(dataFile.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(dataFile)) {
                for (UUID uuid : seenPlayers) {
                    writer.write(uuid.toString());
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            logger.warning("Failed to save seen players: " + e.getMessage());
        }
    }
}
