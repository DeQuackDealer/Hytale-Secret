package rubidium.display;

import rubidium.api.player.Player;
import rubidium.core.logging.RubidiumLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardManager {
    
    private final RubidiumLogger logger;
    private final Map<String, Scoreboard> scoreboards;
    private final Map<UUID, String> playerActiveBoard;
    private long updateInterval;
    private long lastUpdate;
    
    public ScoreboardManager(RubidiumLogger logger) {
        this.logger = logger;
        this.scoreboards = new ConcurrentHashMap<>();
        this.playerActiveBoard = new ConcurrentHashMap<>();
        this.updateInterval = 250;
        this.lastUpdate = 0;
    }
    
    public Scoreboard createScoreboard(String id, String title) {
        Scoreboard board = new Scoreboard(id, title);
        scoreboards.put(id, board);
        logger.debug("Created scoreboard: " + id);
        return board;
    }
    
    public Optional<Scoreboard> getScoreboard(String id) {
        return Optional.ofNullable(scoreboards.get(id));
    }
    
    public void removeScoreboard(String id) {
        Scoreboard board = scoreboards.remove(id);
        if (board != null) {
            playerActiveBoard.entrySet().removeIf(e -> e.getValue().equals(id));
            logger.debug("Removed scoreboard: " + id);
        }
    }
    
    public void showToPlayer(Player player, String scoreboardId) {
        if (scoreboards.containsKey(scoreboardId)) {
            playerActiveBoard.put(player.getUUID(), scoreboardId);
            sendScoreboardToPlayer(player, scoreboards.get(scoreboardId));
        }
    }
    
    public void hideFromPlayer(Player player) {
        playerActiveBoard.remove(player.getUUID());
        clearPlayerScoreboard(player);
    }
    
    public void tick(Collection<Player> onlinePlayers) {
        long now = System.currentTimeMillis();
        if (now - lastUpdate < updateInterval) return;
        lastUpdate = now;
        
        for (Player player : onlinePlayers) {
            String boardId = playerActiveBoard.get(player.getUUID());
            if (boardId != null) {
                Scoreboard board = scoreboards.get(boardId);
                if (board != null) {
                    sendScoreboardToPlayer(player, board);
                }
            }
        }
        
        scoreboards.values().forEach(Scoreboard::markClean);
    }
    
    private void sendScoreboardToPlayer(Player player, Scoreboard board) {
        List<String> lines = board.renderFor(player);
        player.sendPacket(new ScoreboardPacket(board.getTitle(), lines));
    }
    
    private void clearPlayerScoreboard(Player player) {
        player.sendPacket(new ScoreboardPacket(null, List.of()));
    }
    
    public void setUpdateInterval(long millis) {
        this.updateInterval = Math.max(50, millis);
    }
    
    public record ScoreboardPacket(String title, List<String> lines) {}
}
