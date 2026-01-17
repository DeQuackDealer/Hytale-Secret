package com.hypixel.hytale.server.core.plugin;

import java.nio.file.Path;

/**
 * Development stub for Hytale's JavaPluginInit.
 * This stub allows compilation without the actual HytaleServer.jar.
 * At runtime, the real JavaPluginInit from HytaleServer.jar will be used.
 * 
 * DO NOT modify this file - it mirrors the official Hytale API.
 */
public interface JavaPluginInit {
    
    /**
     * Get the plugin's data folder.
     */
    Path getDataFolder();
    
    /**
     * Get the plugin's name.
     */
    String getName();
    
    /**
     * Get the plugin's version.
     */
    String getVersion();
}
