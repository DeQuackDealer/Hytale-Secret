# Configuration API

## Overview

The Configuration API provides a simple, type-safe interface for loading, saving, and managing YAML configuration files. It supports nested sections, automatic type conversion, default values, and hot reloading.

## Package

```java
import rubidium.api.config.ConfigAPI;
import rubidium.api.config.ConfigAPI.Config;
```

---

## ConfigAPI Static Methods

### Path Configuration

| Method | Returns | Description |
|--------|---------|-------------|
| `setConfigRoot(Path root)` | `void` | Sets the root directory for config files |
| `getConfigRoot()` | `Path` | Returns the current config root (default: `config/`) |

### Loading Configurations

| Method | Returns | Description |
|--------|---------|-------------|
| `load(String name)` | `Config` | Loads an existing config file |
| `loadOrCreate(String name, Map<String, Object> defaults)` | `Config` | Loads or creates config with defaults |
| `get(String name)` | `Optional<Config>` | Gets a previously loaded config |

### Bulk Operations

| Method | Returns | Description |
|--------|---------|-------------|
| `saveAll()` | `void` | Saves all loaded configurations |
| `reloadAll()` | `void` | Reloads all loaded configurations from disk |

### Builder

| Method | Returns | Description |
|--------|---------|-------------|
| `builder()` | `ConfigBuilder` | Creates a builder for constructing default values |

---

## Config Class

### Metadata

| Method | Returns | Description |
|--------|---------|-------------|
| `getName()` | `String` | Returns the config name |
| `getPath()` | `Path` | Returns the file path |

### File Operations

| Method | Returns | Description |
|--------|---------|-------------|
| `load()` | `void` | Loads config from disk |
| `save()` | `void` | Saves config to disk |

### Setting Values

| Method | Returns | Description |
|--------|---------|-------------|
| `set(String key, Object value)` | `void` | Sets a value (supports dot notation) |
| `setDefaults(Map<String, Object> defaults)` | `void` | Sets default values |

### Getting Values

| Method | Returns | Description |
|--------|---------|-------------|
| `get(String key)` | `<T>` | Gets raw value (supports dot notation) |
| `get(String key, T defaultValue)` | `<T>` | Gets value with fallback default |
| `getString(String key)` | `String` | Gets string value |
| `getString(String key, String def)` | `String` | Gets string with default |
| `getInt(String key)` | `int` | Gets integer value |
| `getInt(String key, int def)` | `int` | Gets integer with default |
| `getLong(String key)` | `long` | Gets long value |
| `getLong(String key, long def)` | `long` | Gets long with default |
| `getDouble(String key)` | `double` | Gets double value |
| `getDouble(String key, double def)` | `double` | Gets double with default |
| `getBoolean(String key)` | `boolean` | Gets boolean value |
| `getBoolean(String key, boolean def)` | `boolean` | Gets boolean with default |
| `getStringList(String key)` | `List<String>` | Gets string list |
| `getSection(String key)` | `Map<String, Object>` | Gets a config section as map |

### Inspection

| Method | Returns | Description |
|--------|---------|-------------|
| `contains(String key)` | `boolean` | Checks if key exists |
| `getKeys()` | `Set<String>` | Returns all top-level keys |
| `getAll()` | `Map<String, Object>` | Returns all config data (unmodifiable) |

---

## ConfigBuilder

Fluent builder for constructing default configuration values.

| Method | Returns | Description |
|--------|---------|-------------|
| `set(String key, Object value)` | `ConfigBuilder` | Sets a value |
| `section(String name, Map<String, Object> section)` | `ConfigBuilder` | Adds a section |
| `build()` | `Map<String, Object>` | Builds the defaults map |

---

## Code Examples

### Basic Usage

```java
// Set config directory
ConfigAPI.setConfigRoot(Paths.get("plugins/MyPlugin/config"));

// Load or create with defaults
Config config = ConfigAPI.loadOrCreate("settings", Map.of(
    "enabled", true,
    "max-players", 100,
    "welcome-message", "Welcome to the server!"
));

// Read values
boolean enabled = config.getBoolean("enabled");
int maxPlayers = config.getInt("max-players", 50);
String message = config.getString("welcome-message");

// Update values
config.set("max-players", 150);
config.save();
```

### Using the Builder

```java
Map<String, Object> defaults = ConfigAPI.builder()
    .set("version", 1)
    .set("debug", false)
    .section("database", Map.of(
        "host", "localhost",
        "port", 5432,
        "name", "rubidium"
    ))
    .section("features", Map.of(
        "voice-chat", true,
        "minimap", true,
        "waypoints", false
    ))
    .build();

Config config = ConfigAPI.loadOrCreate("plugin", defaults);
```

### Nested Values (Dot Notation)

```java
// Set nested values
config.set("database.host", "192.168.1.100");
config.set("database.credentials.username", "admin");
config.set("database.credentials.password", "secret");

// Get nested values
String host = config.getString("database.host");
int port = config.getInt("database.port", 3306);

// Get entire section
Map<String, Object> dbConfig = config.getSection("database");
```

### Multiple Configurations

```java
// Load multiple configs
Config mainConfig = ConfigAPI.loadOrCreate("main", mainDefaults);
Config messagesConfig = ConfigAPI.loadOrCreate("messages", messageDefaults);
Config spawnsConfig = ConfigAPI.loadOrCreate("spawns", spawnDefaults);

// Access by name later
ConfigAPI.get("messages").ifPresent(config -> {
    String joinMsg = config.getString("join-message");
});

// Save all at once
ConfigAPI.saveAll();

// Reload all (e.g., on /reload command)
ConfigAPI.reloadAll();
```

### Type Conversions

The config API automatically handles type conversions:

```yaml
# config.yml
count: 42
ratio: 3.14
enabled: true
name: "Steve"
```

```java
// All these work correctly
int count = config.getInt("count");        // 42
long countLong = config.getLong("count");  // 42L
double ratio = config.getDouble("ratio");  // 3.14
boolean enabled = config.getBoolean("enabled"); // true
String name = config.getString("name");    // "Steve"

// With defaults for missing values
int missing = config.getInt("missing", -1);  // -1
```

---

## YAML File Format

Configurations are stored in YAML format:

```yaml
# settings.yml
version: 1
debug: false

database:
  host: "localhost"
  port: 5432
  name: "rubidium"

features:
  voice-chat: true
  minimap: true
  waypoints: false

messages:
  welcome: "Welcome, {player}!"
  goodbye: "See you later!"
```

---

## Hot Reloading

```java
// Reload a specific config
Config config = ConfigAPI.get("settings").orElseThrow();
config.load();  // Reloads from disk

// Reload all configs
ConfigAPI.reloadAll();

// Example reload command
public void onReloadCommand(Player sender) {
    try {
        ConfigAPI.reloadAll();
        sender.sendMessage("Configurations reloaded!");
    } catch (IOException e) {
        sender.sendMessage("Error reloading configs: " + e.getMessage());
    }
}
```

---

## Plugin Configuration Pattern

```java
public class MyPlugin extends JavaPlugin {
    
    private Config config;
    
    @Override
    public void onEnable() {
        // Set config directory to plugin folder
        ConfigAPI.setConfigRoot(getDataFolder().toPath());
        
        // Load configuration with defaults
        try {
            config = ConfigAPI.loadOrCreate("config", getDefaults());
        } catch (IOException e) {
            getLogger().severe("Failed to load config: " + e.getMessage());
            return;
        }
        
        // Use configuration
        if (config.getBoolean("features.voice-chat")) {
            enableVoiceChat();
        }
    }
    
    @Override
    public void onDisable() {
        // Save configuration
        try {
            ConfigAPI.saveAll();
        } catch (IOException e) {
            getLogger().warning("Failed to save config: " + e.getMessage());
        }
    }
    
    private Map<String, Object> getDefaults() {
        return ConfigAPI.builder()
            .set("config-version", 1)
            .section("features", Map.of(
                "voice-chat", true,
                "minimap", true
            ))
            .section("settings", Map.of(
                "max-range", 64,
                "update-interval", 20
            ))
            .build();
    }
    
    public Config getPluginConfig() {
        return config;
    }
}
```

---

## Performance Considerations

- **Parsing**: Config files are parsed once on load; values are cached in memory
- **Save Operations**: Save operations write to disk; avoid calling frequently
- **Thread Safety**: Config objects use ConcurrentHashMap internally
- **File Size**: Keep configs under 1MB for optimal load times
- **Hot Reload**: Reloading clears and re-parses; minimal overhead for typical configs

### Best Practices

1. **Load once at startup** - Avoid repeated load calls
2. **Cache frequently accessed values** - Store in local variables for hot paths
3. **Batch saves** - Use `saveAll()` rather than individual saves
4. **Use defaults** - Always provide defaults for optional values
5. **Version configs** - Include a version key for migration support

```java
// Good: Cache frequently accessed values
private int maxRange;
private boolean voiceChatEnabled;

public void loadConfig() {
    maxRange = config.getInt("settings.max-range", 64);
    voiceChatEnabled = config.getBoolean("features.voice-chat", false);
}

// Use cached values in hot path
public void onPlayerMove(Player player) {
    if (voiceChatEnabled && isInRange(player, maxRange)) {
        updateVoiceChat(player);
    }
}
```
