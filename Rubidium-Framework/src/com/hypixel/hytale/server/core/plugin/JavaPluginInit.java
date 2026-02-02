package com.hypixel.hytale.server.core.plugin;

import com.hypixel.hytale.common.plugin.PluginManifest;
import java.nio.file.Path;

/**
 * Development stub for Hytale's JavaPluginInit.
 * This class is passed to a JavaPlugin's constructor during plugin loading.
 * At runtime, the real JavaPluginInit from HytaleServer.jar will be used.
 * 
 * DO NOT modify this file - it mirrors the official Hytale API.
 */
public class JavaPluginInit {
    
    private final PluginManifest manifest;
    private final Path dataDirectory;
    private final Path file;
    
    /**
     * Default constructor for anonymous class usage in tests.
     */
    public JavaPluginInit() {
        this.manifest = null;
        this.dataDirectory = Path.of("plugins", "Rubidium");
        this.file = Path.of("rubidium.jar");
    }
    
    /**
     * Full constructor with all parameters.
     */
    public JavaPluginInit(PluginManifest manifest, Path dataDirectory, Path file) {
        this.manifest = manifest;
        this.dataDirectory = dataDirectory;
        this.file = file;
    }
    
    /**
     * Get the plugin's manifest containing metadata.
     */
    public PluginManifest getManifest() {
        return manifest;
    }
    
    /**
     * Alias for getManifest() - some versions use this.
     */
    public PluginManifest getPluginManifest() {
        return manifest;
    }
    
    /**
     * Get the plugin's data directory for storing files.
     */
    public Path getDataDirectory() {
        return dataDirectory;
    }
    
    /**
     * Get the path to the plugin JAR file.
     */
    public Path getFile() {
        return file;
    }
    
    /**
     * Check if the plugin is in the server's classpath.
     */
    public boolean isInServerClassPath() {
        return false;
    }
}
