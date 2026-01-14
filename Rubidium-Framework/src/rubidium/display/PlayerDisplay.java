package rubidium.display;

import java.util.*;

/**
 * Per-player display state.
 */
public class PlayerDisplay {
    
    private final UUID playerId;
    private final List<String> scoreboardLines;
    private String scoreboardTitle;
    private BossBarData bossBar;
    
    public PlayerDisplay(UUID playerId) {
        this.playerId = playerId;
        this.scoreboardLines = new ArrayList<>();
    }
    
    public UUID getPlayerId() { return playerId; }
    
    public void setScoreboardTitle(String title) {
        this.scoreboardTitle = title;
    }
    
    public String getScoreboardTitle() { return scoreboardTitle; }
    
    public void setScoreboardLine(int line, String text) {
        while (scoreboardLines.size() <= line) {
            scoreboardLines.add("");
        }
        scoreboardLines.set(line, text);
    }
    
    public void clearScoreboard() {
        scoreboardLines.clear();
        scoreboardTitle = null;
    }
    
    public List<String> getScoreboardLines() {
        return scoreboardLines;
    }
    
    public void setBossBar(String title, float progress, String color) {
        this.bossBar = new BossBarData(title, progress, color);
    }
    
    public void removeBossBar() {
        this.bossBar = null;
    }
    
    public BossBarData getBossBar() { return bossBar; }
    
    public record BossBarData(String title, float progress, String color) {}
}
