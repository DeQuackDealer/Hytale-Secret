package com.yellowtale.rubidium.core.config;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages typed configuration files with validation and hot-reload support.
 * 
 * Features:
 * - Type-safe configuration loading
 * - Automatic validation
 * - Hot-reload with file watching
 * - Default value generation
 * - Schema migration support
 */
public final class ConfigManager {
    
    private final Path configDirectory;
    private final Map<String, ConfigHolder<?>> configs = new ConcurrentHashMap<>();
    private final Map<String, List<ConfigReloadListener<?>>> reloadListeners = new ConcurrentHashMap<>();
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    
    private WatchService watchService;
    private Thread watchThread;
    
    public ConfigManager(Path configDirectory) {
        this.configDirectory = configDirectory;
    }
    
    /**
     * Initialize the config manager and start file watching.
     */
    public void initialize() {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }
        
        try {
            Files.createDirectories(configDirectory);
            startFileWatcher();
        } catch (IOException e) {
            throw new ConfigException("Failed to initialize config manager", e);
        }
    }
    
    /**
     * Shutdown the config manager.
     */
    public void shutdown() {
        if (!initialized.compareAndSet(true, false)) {
            return;
        }
        
        stopFileWatcher();
        configs.clear();
        reloadListeners.clear();
    }
    
    /**
     * Register and load a configuration.
     * 
     * @param id Unique identifier for this config
     * @param type The configuration class type
     * @param defaultValue Default configuration if file doesn't exist
     * @return The loaded configuration
     */
    public <T extends Config> T register(String id, Class<T> type, T defaultValue) {
        Path configPath = configDirectory.resolve(id + ".properties");
        
        T config;
        if (Files.exists(configPath)) {
            config = load(configPath, type);
        } else {
            config = defaultValue;
            save(configPath, config);
        }
        
        List<String> errors = config.validate();
        if (!errors.isEmpty()) {
            throw new ConfigValidationException(id, errors);
        }
        
        configs.put(id, new ConfigHolder<>(configPath, type, config));
        return config;
    }
    
    /**
     * Get a registered configuration.
     */
    @SuppressWarnings("unchecked")
    public <T extends Config> T get(String id, Class<T> type) {
        ConfigHolder<?> holder = configs.get(id);
        if (holder == null) {
            throw new ConfigException("Config not registered: " + id);
        }
        if (!type.isInstance(holder.config)) {
            throw new ConfigException("Config type mismatch for " + id);
        }
        return (T) holder.config;
    }
    
    /**
     * Check if a configuration is registered.
     */
    public boolean isRegistered(String id) {
        return configs.containsKey(id);
    }
    
    /**
     * Reload a specific configuration.
     */
    @SuppressWarnings("unchecked")
    public <T extends Config> T reload(String id) {
        ConfigHolder<?> holder = configs.get(id);
        if (holder == null) {
            throw new ConfigException("Config not registered: " + id);
        }
        
        T newConfig = (T) load(holder.path, holder.type);
        List<String> errors = newConfig.validate();
        if (!errors.isEmpty()) {
            throw new ConfigValidationException(id, errors);
        }
        
        T oldConfig = (T) holder.config;
        holder.config = newConfig;
        
        List<ConfigReloadListener<?>> listeners = reloadListeners.get(id);
        if (listeners != null) {
            for (ConfigReloadListener<?> listener : listeners) {
                ((ConfigReloadListener<T>) listener).onReload(oldConfig, newConfig);
            }
        }
        
        return newConfig;
    }
    
    /**
     * Reload all configurations.
     */
    public void reloadAll() {
        for (String id : configs.keySet()) {
            try {
                reload(id);
            } catch (Exception e) {
                // Log error but continue with other configs
            }
        }
    }
    
    /**
     * Save a configuration to disk.
     */
    public <T extends Config> void save(String id, T config) {
        ConfigHolder<?> holder = configs.get(id);
        if (holder != null) {
            save(holder.path, config);
            holder.config = config;
        }
    }
    
    /**
     * Add a reload listener for a configuration.
     */
    public <T extends Config> void addReloadListener(String id, ConfigReloadListener<T> listener) {
        reloadListeners.computeIfAbsent(id, k -> new ArrayList<>()).add(listener);
    }
    
    private <T extends Config> T load(Path path, Class<T> type) {
        try (InputStream in = Files.newInputStream(path)) {
            Properties props = new Properties();
            props.load(in);
            
            T config = type.getDeclaredConstructor().newInstance();
            config.load(props);
            return config;
            
        } catch (Exception e) {
            throw new ConfigException("Failed to load config: " + path, e);
        }
    }
    
    private void save(Path path, Config config) {
        try {
            Properties props = new Properties();
            config.save(props);
            
            try (OutputStream out = Files.newOutputStream(path)) {
                props.store(out, "Rubidium Configuration - " + path.getFileName());
            }
        } catch (IOException e) {
            throw new ConfigException("Failed to save config: " + path, e);
        }
    }
    
    private void startFileWatcher() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            configDirectory.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            
            watchThread = new Thread(this::watchLoop, "Rubidium-ConfigWatcher");
            watchThread.setDaemon(true);
            watchThread.start();
        } catch (IOException e) {
            // File watching not available, hot reload disabled
        }
    }
    
    private void stopFileWatcher() {
        if (watchThread != null) {
            watchThread.interrupt();
        }
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException ignored) {}
        }
    }
    
    private void watchLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                WatchKey key = watchService.take();
                
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                    Path changed = pathEvent.context();
                    
                    String filename = changed.toString();
                    if (filename.endsWith(".properties")) {
                        String configId = filename.substring(0, filename.length() - 11);
                        if (configs.containsKey(configId)) {
                            try {
                                Thread.sleep(100);
                                reload(configId);
                            } catch (Exception ignored) {}
                        }
                    }
                }
                
                key.reset();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private static class ConfigHolder<T extends Config> {
        final Path path;
        final Class<T> type;
        volatile Config config;
        
        ConfigHolder(Path path, Class<T> type, Config config) {
            this.path = path;
            this.type = type;
            this.config = config;
        }
    }
}
