package rubidium.scoreboard;

import rubidium.api.player.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ScoreboardManager {
    
    private final Map<UUID, Scoreboard> playerScoreboards;
    private Function<Player, List<String>> dynamicProvider;
    
    public ScoreboardManager() {
        this.playerScoreboards = new ConcurrentHashMap<>();
    }
    
    public void setScoreboard(Player player, Scoreboard scoreboard) {
        playerScoreboards.put(player.getUUID(), scoreboard);
        scoreboard.show(player);
    }
    
    public void removeScoreboard(Player player) {
        Scoreboard scoreboard = playerScoreboards.remove(player.getUUID());
        if (scoreboard != null) {
            scoreboard.hide(player);
        }
    }
    
    public Optional<Scoreboard> getScoreboard(Player player) {
        return Optional.ofNullable(playerScoreboards.get(player.getUUID()));
    }
    
    public void updateScoreboard(Player player) {
        Scoreboard scoreboard = playerScoreboards.get(player.getUUID());
        if (scoreboard != null) {
            scoreboard.update(player);
        }
    }
    
    public void setDynamicProvider(Function<Player, List<String>> provider) {
        this.dynamicProvider = provider;
    }
    
    public void updateDynamic(Player player) {
        if (dynamicProvider != null) {
            List<String> lines = dynamicProvider.apply(player);
            Scoreboard scoreboard = playerScoreboards.computeIfAbsent(
                player.getUUID(),
                k -> new Scoreboard("Scoreboard")
            );
            scoreboard.setLines(lines);
            scoreboard.update(player);
        }
    }
    
    public void clear() {
        playerScoreboards.clear();
    }
    
    public static class Scoreboard {
        private String title;
        private final List<String> lines;
        
        public Scoreboard(String title) {
            this.title = title;
            this.lines = new ArrayList<>();
        }
        
        public Scoreboard(String title, List<String> lines) {
            this.title = title;
            this.lines = new ArrayList<>(lines);
        }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public List<String> getLines() { return new ArrayList<>(lines); }
        
        public void setLines(List<String> lines) {
            this.lines.clear();
            this.lines.addAll(lines);
        }
        
        public void setLine(int index, String line) {
            while (lines.size() <= index) {
                lines.add("");
            }
            lines.set(index, line);
        }
        
        public void addLine(String line) {
            lines.add(line);
        }
        
        public void removeLine(int index) {
            if (index >= 0 && index < lines.size()) {
                lines.remove(index);
            }
        }
        
        public void clearLines() {
            lines.clear();
        }
        
        public void show(Player player) {
        }
        
        public void hide(Player player) {
        }
        
        public void update(Player player) {
        }
        
        public static Builder builder(String title) {
            return new Builder(title);
        }
        
        public static class Builder {
            private final Scoreboard scoreboard;
            
            public Builder(String title) {
                this.scoreboard = new Scoreboard(title);
            }
            
            public Builder line(String line) {
                scoreboard.addLine(line);
                return this;
            }
            
            public Builder lines(String... lines) {
                for (String line : lines) {
                    scoreboard.addLine(line);
                }
                return this;
            }
            
            public Builder lines(List<String> lines) {
                for (String line : lines) {
                    scoreboard.addLine(line);
                }
                return this;
            }
            
            public Builder blank() {
                scoreboard.addLine("");
                return this;
            }
            
            public Scoreboard build() {
                return scoreboard;
            }
        }
    }
}
