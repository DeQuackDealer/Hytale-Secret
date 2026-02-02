# Plugin Loader API

## Overview

The Plugin Loader system discovers, loads, and manages plugins from JAR files. It supports multiple descriptor formats (Hytale-native, Rubidium, and legacy Bukkit-style), environment detection, and both Rubidium and Hytale-native plugin types.

## Package

```java
import rubidium.hytale.api.PluginLoader;
import rubidium.hytale.api.JavaPlugin;
import rubidium.hytale.api.PluginMetadata;
```

---

## Environment Detection

The loader automatically detects the runtime environment:

| Environment | Description | Detection |
|-------------|-------------|-----------|
| `HYTALE_SERVER` | Running on Hytale dedicated server | Presence of `com.hypixel.hytale.server.HytaleServer` |
| `STANDALONE` | Running in standalone/development mode | `-Drubidium.mode=standalone` or default |
| `SINGLEPLAYER` | Running in singleplayer context | `-Drubidium.mode=singleplayer` |

```java
Environment env = PluginLoader.Environment.detect();
```

---

## PluginLoader

### Constructors

| Constructor | Description |
|-------------|-------------|
| `PluginLoader(Path pluginsDirectory)` | Creates loader with auto-detected environment |
| `PluginLoader(Path pluginsDirectory, Environment environment)` | Creates loader with specified environment |

### Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `loadPlugins()` | `void` | Discovers and loads all JAR plugins in directory |
| `enablePlugins()` | `void` | Enables all loaded plugins (calls lifecycle methods) |
| `disablePlugins()` | `void` | Disables all plugins in reverse order |
| `getPlugin(String name)` | `Optional<Object>` | Gets a plugin by name |
| `getLoadedPlugins()` | `List<JavaPlugin>` | Returns all loaded Rubidium plugins |
| `getLoadedHytalePlugins()` | `List<JavaPlugin>` | Returns all loaded Hytale-native plugins |

### Code Example

```java
// Create and initialize the plugin loader
Path pluginsDir = Paths.get("plugins");
PluginLoader loader = new PluginLoader(pluginsDir);

// Load all plugins from the directory
loader.loadPlugins();

// Enable plugins (calls onLoad, then onEnable)
loader.enablePlugins();

// Get a specific plugin
loader.getPlugin("MyPlugin").ifPresent(plugin -> {
    ((MyPlugin) plugin).doSomething();
});

// On shutdown
loader.disablePlugins();
```

---

## Plugin Descriptor Formats

The loader supports multiple plugin descriptor formats, checked in this order:

### 1. Hytale-Native (manifests.json)

Array format with PascalCase fields:

```json
[
    {
        "Group": "com.example",
        "Name": "MyPlugin",
        "Version": "1.0.0",
        "Description": "An example plugin",
        "Main": "com.example.myplugin.Main"
    }
]
```

### 2. Rubidium Legacy (manifest.json)

Single object with environment-specific entry points:

```json
{
    "Name": "MyPlugin",
    "Version": "1.0.0",
    "Description": "An example plugin",
    "Main": "com.example.myplugin.ServerMain",
    "StandaloneMain": "com.example.myplugin.StandaloneMain",
    "EntryPoints": {
        "hytale-server": "com.example.myplugin.HytaleMain",
        "rubidium-standalone": "com.example.myplugin.StandaloneMain",
        "singleplayer": "com.example.myplugin.ClientMain"
    }
}
```

### 3. Rubidium Alternative (plugin.json)

Simple format with lowercase keys:

```json
{
    "name": "MyPlugin",
    "version": "1.0.0",
    "main": "com.example.myplugin.Main",
    "description": "An example plugin"
}
```

### 4. Legacy Bukkit-style (plugin.yml)

YAML format for compatibility:

```yaml
name: MyPlugin
version: 1.0.0
main: com.example.myplugin.Main
description: An example plugin
```

---

## PluginMetadata

Metadata record for plugin information.

```java
public record PluginMetadata(
    String name,
    String version,
    String mainClass,
    String description
) {}
```

---

## JavaPlugin Base Class

Rubidium plugins extend the `JavaPlugin` class.

### Lifecycle Methods

| Method | When Called | Description |
|--------|-------------|-------------|
| `onLoad()` | After class instantiation | Early initialization |
| `onEnable()` | After all plugins loaded | Main initialization |
| `onDisable()` | On server shutdown | Cleanup and save |

### Utility Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `getName()` | `String` | Plugin name from metadata |
| `getVersion()` | `String` | Plugin version |
| `getDescription()` | `String` | Plugin description |
| `isEnabled()` | `boolean` | Whether plugin is enabled |
| `getLogger()` | `Logger` | Plugin-specific logger |
| `getDataFolder()` | `Path` | Plugin data directory |

### Code Example

```java
package com.example.myplugin;

import rubidium.hytale.api.JavaPlugin;

public class MyPlugin extends JavaPlugin {
    
    @Override
    public void onLoad() {
        // Early initialization
        getLogger().info("Loading " + getName() + " v" + getVersion());
    }
    
    @Override
    public void onEnable() {
        // Main initialization
        loadConfig();
        registerCommands();
        registerListeners();
        getLogger().info(getName() + " enabled!");
    }
    
    @Override
    public void onDisable() {
        // Cleanup
        saveData();
        getLogger().info(getName() + " disabled!");
    }
}
```

---

## Hytale-Native Plugin Support

The loader also supports Hytale-native plugins that extend `com.hypixel.hytale.server.core.plugin.JavaPlugin`.

### Hytale Lifecycle

| Rubidium | Hytale Equivalent |
|----------|-------------------|
| `onLoad()` | `preLoad()` |
| `onEnable()` | `setup()` → `start()` |
| `onDisable()` | `shutdown()` |

```java
// Hytale-native plugin (detected automatically)
package com.example.hytaleplugin;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

public class MyHytalePlugin extends JavaPlugin {
    
    public MyHytalePlugin(JavaPluginInit init) {
        super(init);
    }
    
    @Override
    protected void setup() {
        // Called during server setup
    }
    
    @Override
    protected void start() {
        // Called when server starts
    }
    
    @Override
    protected void shutdown() {
        // Called on server shutdown
    }
}
```

---

## PluginClassLoader

Internal class that handles class loading from JAR files.

- Loads classes from plugin JAR files
- Uses parent-first delegation model
- Isolates plugins from each other

---

## Complete Server Setup Example

```java
public class ServerBootstrap {
    
    private PluginLoader pluginLoader;
    
    public void start() {
        // Initialize plugin loader
        Path pluginsDir = Paths.get("plugins");
        pluginLoader = new PluginLoader(pluginsDir);
        
        try {
            // Discover and load plugins
            pluginLoader.loadPlugins();
            
            // Log loaded plugins
            for (JavaPlugin plugin : pluginLoader.getLoadedPlugins()) {
                System.out.println("Loaded: " + plugin.getName() + " v" + plugin.getVersion());
            }
            
            // Enable all plugins
            pluginLoader.enablePlugins();
            
        } catch (IOException e) {
            System.err.println("Failed to load plugins: " + e.getMessage());
        }
    }
    
    public void stop() {
        // Disable all plugins in reverse order
        pluginLoader.disablePlugins();
    }
    
    public <T> Optional<T> getPlugin(String name, Class<T> type) {
        return pluginLoader.getPlugin(name)
            .filter(type::isInstance)
            .map(type::cast);
    }
}
```

---

## Plugin Directory Structure

```
plugins/
├── MyPlugin-1.0.0.jar
│   ├── plugin.json
│   └── com/
│       └── example/
│           └── myplugin/
│               └── Main.class
├── AnotherPlugin-2.1.0.jar
│   ├── manifests.json
│   └── ...
└── HytaleNativePlugin-1.0.0.jar
    ├── manifests.json
    └── ...
```

---

## Error Handling

The loader handles errors gracefully:

- **Missing descriptor**: Logs warning, skips plugin
- **Invalid main class**: Logs error with details, skips plugin
- **Load exception**: Logs severe error, continues with other plugins
- **Enable exception**: Logs error, marks plugin as disabled

```
[INFO] PluginLoader initialized in HYTALE_SERVER mode
[INFO] Loaded plugin: MyPlugin v1.0.0
[WARNING] No plugin descriptor found in: BrokenPlugin.jar
[SEVERE] Failed to load plugin: InvalidPlugin.jar - ClassNotFoundException
[INFO] Enabled: MyPlugin
```

---

## Performance Considerations

- **Lazy Loading**: Plugins are only loaded when `loadPlugins()` is called
- **Class Loading**: Uses parent-first delegation to share common classes
- **Memory**: Each plugin gets its own ClassLoader instance
- **Startup Time**: JAR scanning is O(n) where n = number of JAR files

### Best Practices

1. **Minimize onLoad()** - Keep early init lightweight
2. **Defer expensive operations** - Use lazy initialization
3. **Clean up properly** - Release resources in onDisable()
4. **Handle exceptions** - Don't crash the server on errors
5. **Use environment detection** - Adapt behavior to runtime context

```java
@Override
public void onEnable() {
    // Check environment for feature availability
    if (PluginLoader.Environment.detect() == Environment.HYTALE_SERVER) {
        enableServerOnlyFeatures();
    }
}
```
