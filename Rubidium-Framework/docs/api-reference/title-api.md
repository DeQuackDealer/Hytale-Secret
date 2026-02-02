# TitleAPI Reference

## Overview

The `TitleAPI` provides methods for displaying titles, subtitles, action bars, and boss bars to players. These UI elements are useful for conveying important information, game events, and status updates.

**Package:** `rubidium.api.title`

## Getting Started

```java
import rubidium.api.title.TitleAPI;
import rubidium.api.title.TitleAPI.BossBarColor;
import rubidium.api.title.TitleAPI.BossBarStyle;

// Get the singleton instance
TitleAPI title = TitleAPI.get();
```

## Public Methods

### Instance Access

#### `get()`
Returns the singleton instance of the TitleAPI.

```java
public static TitleAPI get()
```

**Returns:** `TitleAPI` - The singleton instance

---

### Title Display

#### `sendTitle(UUID playerId, String title, String subtitle, int fadeIn, int stay, int fadeOut)`
Sends a title with full timing control.

```java
public void sendTitle(UUID playerId, String title, String subtitle, int fadeIn, int stay, int fadeOut)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `playerId` | `UUID` | The player's unique identifier |
| `title` | `String` | The main title text |
| `subtitle` | `String` | The subtitle text |
| `fadeIn` | `int` | Fade in duration in ticks |
| `stay` | `int` | Stay duration in ticks |
| `fadeOut` | `int` | Fade out duration in ticks |

**Timing Notes:**
- 20 ticks = 1 second
- Default values: fadeIn=10, stay=70, fadeOut=20

**Example:**
```java
// Show a welcome title for 5 seconds
TitleAPI.get().sendTitle(
    playerId,
    "§6Welcome!",           // Gold colored title
    "§7Enjoy your stay",    // Gray subtitle
    20,                     // 1 second fade in
    100,                    // 5 seconds stay
    20                      // 1 second fade out
);
```

#### `sendTitle(UUID playerId, String title, String subtitle)`
Sends a title with default timing (fadeIn=10, stay=70, fadeOut=20).

```java
public void sendTitle(UUID playerId, String title, String subtitle)
```

**Example:**
```java
TitleAPI.get().sendTitle(playerId, "§aLevel Up!", "§eYou are now level 5");
```

#### `sendTitle(UUID playerId, String title)`
Sends a title-only message with default timing.

```java
public void sendTitle(UUID playerId, String title)
```

**Example:**
```java
TitleAPI.get().sendTitle(playerId, "§c§lGAME OVER");
```

#### `sendSubtitle(UUID playerId, String subtitle)`
Sends a subtitle-only message (empty title) with default timing.

```java
public void sendSubtitle(UUID playerId, String subtitle)
```

**Example:**
```java
TitleAPI.get().sendSubtitle(playerId, "§7Press SPACE to continue");
```

#### `clearTitle(UUID playerId)`
Clears any active title from the player's screen.

```java
public void clearTitle(UUID playerId)
```

**Example:**
```java
TitleAPI.get().clearTitle(playerId);
```

---

### Action Bar

#### `sendActionBar(UUID playerId, String message)`
Displays a message in the action bar (above the hotbar).

```java
public void sendActionBar(UUID playerId, String message)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `playerId` | `UUID` | The player's unique identifier |
| `message` | `String` | The message to display |

**Example:**
```java
TitleAPI.get().sendActionBar(playerId, "§a+50 XP");
```

#### `sendActionBar(UUID playerId, String message, int durationTicks)`
Displays an action bar message that auto-clears after a duration.

```java
public void sendActionBar(UUID playerId, String message, int durationTicks)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `playerId` | `UUID` | The player's unique identifier |
| `message` | `String` | The message to display |
| `durationTicks` | `int` | How long to display (in ticks) |

**Example:**
```java
// Show for 3 seconds
TitleAPI.get().sendActionBar(playerId, "§eAbility Ready!", 60);
```

#### `clearActionBar(UUID playerId)`
Clears the action bar for a player.

```java
public void clearActionBar(UUID playerId)
```

---

### Boss Bar

#### `sendBossBar(UUID playerId, String id, String text, float progress, BossBarColor color, BossBarStyle style)`
Creates or updates a boss bar for a player.

```java
public void sendBossBar(UUID playerId, String id, String text, float progress, BossBarColor color, BossBarStyle style)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `playerId` | `UUID` | The player's unique identifier |
| `id` | `String` | Unique identifier for this boss bar |
| `text` | `String` | The text to display |
| `progress` | `float` | Progress value (0.0 to 1.0) |
| `color` | `BossBarColor` | The bar color |
| `style` | `BossBarStyle` | The bar style |

**Example:**
```java
// Create a boss health bar
TitleAPI.get().sendBossBar(
    playerId,
    "dragon_boss",                    // Unique ID
    "§c§lEnder Dragon",               // Display text
    0.75f,                            // 75% health
    BossBarColor.RED,                 // Red color
    BossBarStyle.SEGMENTED_10         // 10 segments
);
```

#### `updateBossBar(UUID playerId, String id, String text, float progress)`
Updates an existing boss bar's text and progress.

```java
public void updateBossBar(UUID playerId, String id, String text, float progress)
```

**Parameters:**
| Name | Type | Description |
|------|------|-------------|
| `playerId` | `UUID` | The player's unique identifier |
| `id` | `String` | The boss bar's unique identifier |
| `text` | `String` | New text to display |
| `progress` | `float` | New progress value (0.0 to 1.0) |

**Example:**
```java
// Update boss health
TitleAPI.get().updateBossBar(playerId, "dragon_boss", "§c§lEnder Dragon", 0.50f);
```

#### `removeBossBar(UUID playerId, String id)`
Removes a boss bar from a player's screen.

```java
public void removeBossBar(UUID playerId, String id)
```

**Example:**
```java
TitleAPI.get().removeBossBar(playerId, "dragon_boss");
```

---

### Broadcasting

#### `broadcastTitle(Collection<UUID> players, String title, String subtitle, int fadeIn, int stay, int fadeOut)`
Sends a title to multiple players.

```java
public void broadcastTitle(Collection<UUID> players, String title, String subtitle, int fadeIn, int stay, int fadeOut)
```

**Example:**
```java
List<UUID> allPlayers = server.getOnlinePlayers();
TitleAPI.get().broadcastTitle(
    allPlayers,
    "§6§lSERVER ANNOUNCEMENT",
    "§eRestarting in 5 minutes",
    10, 100, 20
);
```

#### `broadcastActionBar(Collection<UUID> players, String message)`
Sends an action bar message to multiple players.

```java
public void broadcastActionBar(Collection<UUID> players, String message)
```

**Example:**
```java
TitleAPI.get().broadcastActionBar(allPlayers, "§aNew event starting!");
```

---

### Lifecycle

#### `shutdown()`
Shuts down the internal scheduler. Call when disabling the plugin.

```java
public void shutdown()
```

---

## Enums

### BossBarColor

```java
public enum BossBarColor {
    PINK,
    BLUE,
    RED,
    GREEN,
    YELLOW,
    PURPLE,
    WHITE
}
```

### BossBarStyle

```java
public enum BossBarStyle {
    SOLID,          // No segments
    SEGMENTED_6,    // 6 segments
    SEGMENTED_10,   // 10 segments
    SEGMENTED_12,   // 12 segments
    SEGMENTED_20    // 20 segments
}
```

---

## Inner Classes

### TitlePacket

Data class for title information.

```java
public static class TitlePacket {
    public final String title;
    public final String subtitle;
    public final int fadeIn;
    public final int stay;
    public final int fadeOut;
}
```

### BossBarPacket

Data class for boss bar information.

```java
public static class BossBarPacket {
    public final String id;
    public final String text;
    public final float progress;
    public final BossBarColor color;
    public final BossBarStyle style;
}
```

---

## Color Codes

Rubidium supports standard color codes using `§`:

| Code | Color | Code | Style |
|------|-------|------|-------|
| `§0` | Black | `§l` | Bold |
| `§1` | Dark Blue | `§m` | Strikethrough |
| `§2` | Dark Green | `§n` | Underline |
| `§3` | Dark Aqua | `§o` | Italic |
| `§4` | Dark Red | `§r` | Reset |
| `§5` | Dark Purple | | |
| `§6` | Gold | | |
| `§7` | Gray | | |
| `§8` | Dark Gray | | |
| `§9` | Blue | | |
| `§a` | Green | | |
| `§b` | Aqua | | |
| `§c` | Red | | |
| `§d` | Light Purple | | |
| `§e` | Yellow | | |
| `§f` | White | | |

---

## Best Practices

1. **Use unique boss bar IDs** - Each boss bar needs a unique ID for proper updating/removal.

2. **Clear titles when done** - If you send a persistent title, clear it when appropriate.

3. **Don't spam action bars** - Action bar messages overwrite each other; use sparingly.

4. **Consider timing** - 20 ticks = 1 second. Adjust fadeIn/stay/fadeOut for readability.

5. **Use color codes** - Colored text is more visible and helps categorize information.

6. **Shutdown gracefully** - Call `shutdown()` when your plugin disables to clean up schedulers.

---

## Complete Example

```java
import rubidium.api.title.TitleAPI;
import rubidium.api.title.TitleAPI.BossBarColor;
import rubidium.api.title.TitleAPI.BossBarStyle;

public class GameUI {
    private final TitleAPI title = TitleAPI.get();
    
    public void onGameStart(UUID playerId) {
        // Show game starting countdown
        title.sendTitle(playerId, "§e3", "", 5, 15, 5);
        
        // Schedule countdown (pseudo-code)
        scheduleTask(() -> title.sendTitle(playerId, "§e2", "", 5, 15, 5), 20);
        scheduleTask(() -> title.sendTitle(playerId, "§e1", "", 5, 15, 5), 40);
        scheduleTask(() -> title.sendTitle(playerId, "§a§lGO!", "", 10, 20, 10), 60);
    }
    
    public void showBossHealth(UUID playerId, String bossName, double healthPercent) {
        title.sendBossBar(
            playerId,
            "current_boss",
            "§c" + bossName,
            (float) healthPercent,
            BossBarColor.RED,
            BossBarStyle.SEGMENTED_10
        );
    }
    
    public void updateBossHealth(UUID playerId, String bossName, double healthPercent) {
        title.updateBossBar(playerId, "current_boss", "§c" + bossName, (float) healthPercent);
        
        if (healthPercent <= 0) {
            title.removeBossBar(playerId, "current_boss");
            title.sendTitle(playerId, "§6§lVICTORY!", "§eBoss defeated!");
        }
    }
    
    public void showCooldownStatus(UUID playerId, String abilityName, int secondsLeft) {
        if (secondsLeft > 0) {
            title.sendActionBar(playerId, "§c" + abilityName + " on cooldown: " + secondsLeft + "s");
        } else {
            title.sendActionBar(playerId, "§a" + abilityName + " ready!", 40);
        }
    }
    
    public void showQuestProgress(UUID playerId, String questName, int current, int max) {
        float progress = (float) current / max;
        title.sendBossBar(
            playerId,
            "quest_progress",
            "§e" + questName + " §7(" + current + "/" + max + ")",
            progress,
            BossBarColor.YELLOW,
            BossBarStyle.SOLID
        );
    }
    
    public void onPlayerDeath(UUID playerId) {
        title.clearTitle(playerId);
        title.removeBossBar(playerId, "current_boss");
        title.sendTitle(
            playerId,
            "§c§lYOU DIED",
            "§7Respawning in 5 seconds...",
            20, 60, 20
        );
    }
    
    public void onServerShutdown(List<UUID> allPlayers) {
        title.broadcastTitle(
            allPlayers,
            "§c§lSERVER CLOSING",
            "§7Thank you for playing!",
            10, 100, 20
        );
        
        // Clean shutdown
        title.shutdown();
    }
}
```
