# ScoreboardAPI Reference

## Overview

The `ScoreboardAPI` provides a comprehensive system for creating and managing scoreboards, score entries, and teams. Scoreboards can be displayed in the sidebar, below player names, or in the tab list.

**Package:** `rubidium.api.scoreboard`

## Getting Started

```java
import rubidium.api.scoreboard.ScoreboardAPI;
import rubidium.api.scoreboard.ScoreboardAPI.Scoreboard;
import rubidium.api.scoreboard.ScoreboardAPI.Team;
import rubidium.api.scoreboard.ScoreboardAPI.ScoreEntry;

// Create a scoreboard
Scoreboard scoreboard = ScoreboardAPI.create("my_scoreboard")
    .title("§6My Server")
    .line("§7Welcome!")
    .build();
```

## Static Methods

### Scoreboard Creation

#### `create(String id)`
Creates a new scoreboard builder.

```java
public static Scoreboard.Builder create(String id)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `id` | `String` | Unique identifier for the scoreboard |

**Returns:** `Scoreboard.Builder` - A builder for configuring the scoreboard

**Example:**
```java
Scoreboard scoreboard = ScoreboardAPI.create("lobby")
    .title("§6§lLOBBY")
    .type(Scoreboard.DisplayType.SIDEBAR)
    .line("§7Players: §a24")
    .line("")
    .line("§7Server: §aUS-East")
    .build();
```

#### `register(Scoreboard scoreboard)`
Registers a scoreboard for use.

```java
public static Scoreboard register(Scoreboard scoreboard)
```

**Returns:** `Scoreboard` - The registered scoreboard

#### `register(Scoreboard.Builder builder)`
Builds and registers a scoreboard in one step.

```java
public static Scoreboard register(Scoreboard.Builder builder)
```

**Example:**
```java
Scoreboard board = ScoreboardAPI.register(
    ScoreboardAPI.create("game")
        .title("§c§lBattle Royale")
        .line("§7Alive: §a50")
);
```

---

### Quick Creation Methods

#### `sidebar(String id, String title)`
Creates a sidebar scoreboard with the given title.

```java
public static Scoreboard sidebar(String id, String title)
```

**Example:**
```java
Scoreboard sidebar = ScoreboardAPI.sidebar("game_stats", "§e§lGame Stats");
```

#### `belowName(String id, String title)`
Creates a below-name scoreboard.

```java
public static Scoreboard belowName(String id, String title)
```

**Example:**
```java
Scoreboard health = ScoreboardAPI.belowName("health_display", "§c❤");
```

#### `tabList(String id, String title)`
Creates a tab list scoreboard.

```java
public static Scoreboard tabList(String id, String title)
```

---

### Scoreboard Retrieval

#### `get(String id)`
Retrieves a registered scoreboard by ID.

```java
public static Optional<Scoreboard> get(String id)
```

**Returns:** `Optional<Scoreboard>` - The scoreboard if found

**Example:**
```java
ScoreboardAPI.get("game").ifPresent(board -> {
    board.setLine(0, "§7Alive: §a" + aliveCount);
});
```

#### `remove(String id)`
Removes a scoreboard from the registry.

```java
public static void remove(String id)
```

---

### Player Management

#### `show(UUID playerId, String scoreboardId)`
Shows a scoreboard to a player.

```java
public static void show(UUID playerId, String scoreboardId)
```

**Example:**
```java
ScoreboardAPI.show(playerId, "lobby");
```

#### `hide(UUID playerId)`
Hides the scoreboard from a player.

```java
public static void hide(UUID playerId)
```

#### `getPlayerScoreboard(UUID playerId)`
Gets the scoreboard currently shown to a player.

```java
public static Optional<String> getPlayerScoreboard(UUID playerId)
```

**Returns:** `Optional<String>` - The scoreboard ID if one is shown

---

## Scoreboard Class

### Properties

```java
public String getId()
public String getTitle()
public DisplayType getType()
public List<ScoreEntry> getEntries()
public boolean isVisible()
```

### Setters

```java
public void setTitle(String title)
public void setVisible(boolean visible)
```

### Line Management

#### `setLine(int line, String text)`
Sets text at a specific line number (score = line number).

```java
public void setLine(int line, String text)
```

**Example:**
```java
scoreboard.setLine(0, "§7Players: §a" + playerCount);
scoreboard.setLine(1, "");  // Empty line for spacing
scoreboard.setLine(2, "§7Time: §e" + timeLeft);
```

#### `setLine(int line, String text, int score)`
Sets text at a specific line with a custom score value.

```java
public void setLine(int line, String text, int score)
```

**Example:**
```java
// Lines are sorted by score (higher = higher on the board)
scoreboard.setLine(0, "§6First Place: Player1", 100);
scoreboard.setLine(1, "§7Second Place: Player2", 75);
scoreboard.setLine(2, "§7Third Place: Player3", 50);
```

#### `addLine(String text)`
Adds a new line at the bottom.

```java
public void addLine(String text)
```

#### `removeLine(int line)`
Removes a line by index.

```java
public void removeLine(int line)
```

#### `clearLines()`
Removes all lines from the scoreboard.

```java
public void clearLines()
```

### Team Management

#### `createTeam(String name)`
Creates a new team on this scoreboard.

```java
public Team createTeam(String name)
```

**Returns:** `Team` - The created team

**Example:**
```java
Team redTeam = scoreboard.createTeam("red")
    .displayName("Red Team")
    .prefix("§c[RED] ")
    .color(TeamColor.RED);
```

#### `getTeam(String name)`
Gets a team by name.

```java
public Optional<Team> getTeam(String name)
```

#### `removeTeam(String name)`
Removes a team.

```java
public void removeTeam(String name)
```

#### `getTeams()`
Gets all teams on this scoreboard.

```java
public Collection<Team> getTeams()
```

---

## Scoreboard.Builder Class

Builder for creating scoreboards with fluent API.

```java
public Builder title(String title)
public Builder type(DisplayType type)
public Builder line(String text)
public Builder line(String text, int score)
public Builder lines(String... lines)
public Scoreboard build()
```

**Example:**
```java
Scoreboard board = ScoreboardAPI.create("pvp_stats")
    .title("§c§lPVP Arena")
    .type(Scoreboard.DisplayType.SIDEBAR)
    .lines(
        "§7Kills: §a0",
        "§7Deaths: §c0",
        "",
        "§7K/D: §e0.00",
        "",
        "§7Server: §bpvp-1"
    )
    .build();
```

---

## Team Class

Represents a scoreboard team with display properties and member management.

### Properties

```java
public String getName()
public String getDisplayName()
public String getPrefix()
public String getSuffix()
public TeamColor getColor()
public Set<UUID> getMembers()
public boolean isFriendlyFire()
public boolean canSeeInvisible()
public NameTagVisibility getNameTagVisibility()
public CollisionRule getCollisionRule()
```

### Fluent Setters

```java
public Team displayName(String name)
public Team prefix(String prefix)
public Team suffix(String suffix)
public Team color(TeamColor color)
public Team friendlyFire(boolean allow)
public Team seeInvisible(boolean see)
public Team nameTagVisibility(NameTagVisibility vis)
public Team collisionRule(CollisionRule rule)
```

### Member Management

```java
public void addMember(UUID playerId)
public void removeMember(UUID playerId)
public boolean hasMember(UUID playerId)
public void clearMembers()
```

### Utilities

```java
public String formatName(String playerName)
```

**Example:**
```java
Team blueTeam = scoreboard.createTeam("blue")
    .displayName("Blue Team")
    .prefix("§9[BLUE] ")
    .suffix(" §7⚔")
    .color(TeamColor.BLUE)
    .friendlyFire(false)
    .seeInvisible(true)
    .nameTagVisibility(NameTagVisibility.HIDE_FOR_OTHER_TEAMS);

// Add players to team
blueTeam.addMember(player1Id);
blueTeam.addMember(player2Id);

// Format a player name
String formatted = blueTeam.formatName("Steve"); // "§9[BLUE] Steve §7⚔"
```

---

## Records

### ScoreEntry

```java
public record ScoreEntry(String text, int score) {
    public ScoreEntry withText(String newText)
    public ScoreEntry withScore(int newScore)
}
```

---

## Enums

### DisplayType

```java
public enum DisplayType {
    SIDEBAR,      // Shows on the right side of the screen
    BELOW_NAME,   // Shows below player names
    TAB_LIST      // Shows in the player list
}
```

### TeamColor

```java
public enum TeamColor {
    BLACK, DARK_BLUE, DARK_GREEN, DARK_AQUA, DARK_RED, DARK_PURPLE,
    GOLD, GRAY, DARK_GRAY, BLUE, GREEN, AQUA, RED, LIGHT_PURPLE,
    YELLOW, WHITE
}
```

### NameTagVisibility

```java
public enum NameTagVisibility {
    ALWAYS,                 // Always visible
    HIDE_FOR_OTHER_TEAMS,   // Hidden from other teams
    HIDE_FOR_OWN_TEAM,      // Hidden from own team
    NEVER                   // Never visible
}
```

### CollisionRule

```java
public enum CollisionRule {
    ALWAYS,           // Always push
    PUSH_OTHER_TEAMS, // Only push other teams
    PUSH_OWN_TEAM,    // Only push own team
    NEVER             // Never push
}
```

---

## Best Practices

1. **Use unique IDs** - Each scoreboard needs a unique ID for proper management.

2. **Register before showing** - Always register a scoreboard before showing it to players.

3. **Update efficiently** - Use `setLine()` for individual updates rather than recreating the entire scoreboard.

4. **Use empty lines for spacing** - Add `""` lines to create visual separation.

5. **Color code consistently** - Use color codes to make information scannable.

6. **Clean up on disconnect** - Hide scoreboards when players leave.

---

## Complete Example

```java
import rubidium.api.scoreboard.ScoreboardAPI;
import rubidium.api.scoreboard.ScoreboardAPI.*;

public class GameScoreboard {
    private static final String GAME_BOARD_ID = "battle_royale";
    
    public void initialize() {
        // Create the main game scoreboard
        Scoreboard board = ScoreboardAPI.create(GAME_BOARD_ID)
            .title("§c§lBATTLE ROYALE")
            .lines(
                "§7§m                    ",
                "",
                "§7Alive: §a50",
                "§7Kills: §e0",
                "",
                "§7Zone: §bSafe",
                "§7Time: §f10:00",
                "",
                "§7§m                    ",
                "§6play.myserver.com"
            )
            .build();
        
        // Create teams
        Team redTeam = board.createTeam("red")
            .displayName("Red Team")
            .prefix("§c")
            .color(TeamColor.RED)
            .friendlyFire(false);
            
        Team blueTeam = board.createTeam("blue")
            .displayName("Blue Team")
            .prefix("§9")
            .color(TeamColor.BLUE)
            .friendlyFire(false);
        
        ScoreboardAPI.register(board);
    }
    
    public void showToPlayer(UUID playerId) {
        ScoreboardAPI.show(playerId, GAME_BOARD_ID);
    }
    
    public void updateStats(int alive, int kills, String zone, String time) {
        ScoreboardAPI.get(GAME_BOARD_ID).ifPresent(board -> {
            board.setLine(2, "§7Alive: §a" + alive);
            board.setLine(3, "§7Kills: §e" + kills);
            board.setLine(5, "§7Zone: §b" + zone);
            board.setLine(6, "§7Time: §f" + time);
        });
    }
    
    public void addPlayerToTeam(UUID playerId, String teamName) {
        ScoreboardAPI.get(GAME_BOARD_ID).ifPresent(board -> {
            board.getTeam(teamName).ifPresent(team -> {
                team.addMember(playerId);
            });
        });
    }
    
    public void onPlayerDeath(UUID playerId) {
        ScoreboardAPI.hide(playerId);
    }
    
    public void cleanup() {
        ScoreboardAPI.remove(GAME_BOARD_ID);
    }
}
```

### Leaderboard Example

```java
public class Leaderboard {
    
    public void createKillLeaderboard(Map<String, Integer> playerKills) {
        Scoreboard.Builder builder = ScoreboardAPI.create("kill_leaders")
            .title("§6§lTop Killers")
            .type(Scoreboard.DisplayType.SIDEBAR);
        
        // Sort players by kills
        List<Map.Entry<String, Integer>> sorted = playerKills.entrySet()
            .stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10)
            .toList();
        
        int position = 1;
        for (Map.Entry<String, Integer> entry : sorted) {
            String color = position <= 3 ? "§e" : "§7";
            builder.line(color + position + ". " + entry.getKey(), entry.getValue());
            position++;
        }
        
        ScoreboardAPI.register(builder);
    }
}
```
