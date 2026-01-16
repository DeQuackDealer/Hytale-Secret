package rubidium.api.scoreboard;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ScoreboardAPI {
    
    private static final Map<String, Scoreboard> scoreboards = new ConcurrentHashMap<>();
    private static final Map<UUID, String> playerScoreboards = new ConcurrentHashMap<>();
    
    private ScoreboardAPI() {}
    
    public static Scoreboard.Builder create(String id) {
        return new Scoreboard.Builder(id);
    }
    
    public static Scoreboard register(Scoreboard scoreboard) {
        scoreboards.put(scoreboard.getId(), scoreboard);
        return scoreboard;
    }
    
    public static Scoreboard register(Scoreboard.Builder builder) {
        return register(builder.build());
    }
    
    public static Optional<Scoreboard> get(String id) {
        return Optional.ofNullable(scoreboards.get(id));
    }
    
    public static void remove(String id) {
        scoreboards.remove(id);
    }
    
    public static void show(UUID playerId, String scoreboardId) {
        playerScoreboards.put(playerId, scoreboardId);
    }
    
    public static void hide(UUID playerId) {
        playerScoreboards.remove(playerId);
    }
    
    public static Optional<String> getPlayerScoreboard(UUID playerId) {
        return Optional.ofNullable(playerScoreboards.get(playerId));
    }
    
    public static Scoreboard sidebar(String id, String title) {
        return create(id)
            .title(title)
            .type(Scoreboard.DisplayType.SIDEBAR)
            .build();
    }
    
    public static Scoreboard belowName(String id, String title) {
        return create(id)
            .title(title)
            .type(Scoreboard.DisplayType.BELOW_NAME)
            .build();
    }
    
    public static Scoreboard tabList(String id, String title) {
        return create(id)
            .title(title)
            .type(Scoreboard.DisplayType.TAB_LIST)
            .build();
    }
    
    public static class Scoreboard {
        private final String id;
        private String title;
        private final DisplayType type;
        private final List<ScoreEntry> entries = new ArrayList<>();
        private final Map<String, Team> teams = new ConcurrentHashMap<>();
        private boolean visible = true;
        
        private Scoreboard(Builder builder) {
            this.id = builder.id;
            this.title = builder.title;
            this.type = builder.type;
            this.entries.addAll(builder.entries);
        }
        
        public String getId() { return id; }
        public String getTitle() { return title; }
        public DisplayType getType() { return type; }
        public List<ScoreEntry> getEntries() { return Collections.unmodifiableList(entries); }
        public boolean isVisible() { return visible; }
        
        public void setTitle(String title) { this.title = title; }
        public void setVisible(boolean visible) { this.visible = visible; }
        
        public void setLine(int line, String text) {
            while (entries.size() <= line) {
                entries.add(new ScoreEntry("", entries.size()));
            }
            entries.set(line, new ScoreEntry(text, line));
        }
        
        public void setLine(int line, String text, int score) {
            while (entries.size() <= line) {
                entries.add(new ScoreEntry("", entries.size()));
            }
            entries.set(line, new ScoreEntry(text, score));
        }
        
        public void addLine(String text) {
            entries.add(new ScoreEntry(text, entries.size()));
        }
        
        public void removeLine(int line) {
            if (line >= 0 && line < entries.size()) {
                entries.remove(line);
            }
        }
        
        public void clearLines() {
            entries.clear();
        }
        
        public Team createTeam(String name) {
            Team team = new Team(name);
            teams.put(name, team);
            return team;
        }
        
        public Optional<Team> getTeam(String name) {
            return Optional.ofNullable(teams.get(name));
        }
        
        public void removeTeam(String name) {
            teams.remove(name);
        }
        
        public Collection<Team> getTeams() {
            return teams.values();
        }
        
        public enum DisplayType {
            SIDEBAR,
            BELOW_NAME,
            TAB_LIST
        }
        
        public static class Builder {
            private final String id;
            private String title = "";
            private DisplayType type = DisplayType.SIDEBAR;
            private List<ScoreEntry> entries = new ArrayList<>();
            
            public Builder(String id) { this.id = id; }
            
            public Builder title(String title) { this.title = title; return this; }
            public Builder type(DisplayType type) { this.type = type; return this; }
            public Builder line(String text) { this.entries.add(new ScoreEntry(text, entries.size())); return this; }
            public Builder line(String text, int score) { this.entries.add(new ScoreEntry(text, score)); return this; }
            
            public Builder lines(String... lines) {
                for (String line : lines) {
                    this.entries.add(new ScoreEntry(line, entries.size()));
                }
                return this;
            }
            
            public Scoreboard build() {
                return new Scoreboard(this);
            }
        }
    }
    
    public record ScoreEntry(String text, int score) {
        public ScoreEntry withText(String newText) {
            return new ScoreEntry(newText, score);
        }
        
        public ScoreEntry withScore(int newScore) {
            return new ScoreEntry(text, newScore);
        }
    }
    
    public static class Team {
        private final String name;
        private String displayName;
        private String prefix = "";
        private String suffix = "";
        private TeamColor color = TeamColor.WHITE;
        private final Set<UUID> members = ConcurrentHashMap.newKeySet();
        private boolean friendlyFire = true;
        private boolean seeInvisible = false;
        private NameTagVisibility nameTagVisibility = NameTagVisibility.ALWAYS;
        private CollisionRule collisionRule = CollisionRule.ALWAYS;
        
        public Team(String name) {
            this.name = name;
            this.displayName = name;
        }
        
        public String getName() { return name; }
        public String getDisplayName() { return displayName; }
        public String getPrefix() { return prefix; }
        public String getSuffix() { return suffix; }
        public TeamColor getColor() { return color; }
        public Set<UUID> getMembers() { return Collections.unmodifiableSet(members); }
        public boolean isFriendlyFire() { return friendlyFire; }
        public boolean canSeeInvisible() { return seeInvisible; }
        public NameTagVisibility getNameTagVisibility() { return nameTagVisibility; }
        public CollisionRule getCollisionRule() { return collisionRule; }
        
        public Team displayName(String name) { this.displayName = name; return this; }
        public Team prefix(String prefix) { this.prefix = prefix; return this; }
        public Team suffix(String suffix) { this.suffix = suffix; return this; }
        public Team color(TeamColor color) { this.color = color; return this; }
        public Team friendlyFire(boolean allow) { this.friendlyFire = allow; return this; }
        public Team seeInvisible(boolean see) { this.seeInvisible = see; return this; }
        public Team nameTagVisibility(NameTagVisibility vis) { this.nameTagVisibility = vis; return this; }
        public Team collisionRule(CollisionRule rule) { this.collisionRule = rule; return this; }
        
        public void addMember(UUID playerId) { members.add(playerId); }
        public void removeMember(UUID playerId) { members.remove(playerId); }
        public boolean hasMember(UUID playerId) { return members.contains(playerId); }
        public void clearMembers() { members.clear(); }
        
        public String formatName(String playerName) {
            return prefix + playerName + suffix;
        }
    }
    
    public enum TeamColor {
        BLACK, DARK_BLUE, DARK_GREEN, DARK_AQUA, DARK_RED, DARK_PURPLE,
        GOLD, GRAY, DARK_GRAY, BLUE, GREEN, AQUA, RED, LIGHT_PURPLE,
        YELLOW, WHITE
    }
    
    public enum NameTagVisibility {
        ALWAYS, HIDE_FOR_OTHER_TEAMS, HIDE_FOR_OWN_TEAM, NEVER
    }
    
    public enum CollisionRule {
        ALWAYS, PUSH_OTHER_TEAMS, PUSH_OWN_TEAM, NEVER
    }
}
