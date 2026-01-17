package com.hypixel.hytale.server.core.plugin;

import com.hypixel.hytale.server.core.command.CommandManager;
import com.hypixel.hytale.server.core.command.PluginCommand;
import com.hypixel.hytale.server.core.event.EventRegistry;

import java.util.logging.Logger;

/**
 * Development stub for Hytale's JavaPlugin.
 * This stub allows compilation without the actual HytaleServer.jar.
 * At runtime, the real JavaPlugin from HytaleServer.jar will be used.
 * 
 * DO NOT modify this file - it mirrors the official Hytale API.
 */
public abstract class JavaPlugin {
    
    private final JavaPluginInit init;
    private final Logger logger;
    
    protected JavaPlugin(JavaPluginInit init) {
        this.init = init;
        this.logger = Logger.getLogger(getClass().getSimpleName());
    }
    
    /**
     * Called when the plugin is enabled.
     */
    public void onEnable() {
    }
    
    /**
     * Called when the plugin is disabled.
     */
    public void onDisable() {
    }
    
    /**
     * Get the plugin logger.
     */
    public Logger getLogger() {
        return logger;
    }
    
    /**
     * Get the plugin initialization data.
     */
    public JavaPluginInit getInit() {
        return init;
    }
    
    /**
     * Get the command manager for registering commands.
     */
    public CommandManager getCommandManager() {
        return CommandManager.get();
    }
    
    /**
     * Get the event registry for registering event listeners.
     */
    public EventRegistry getEventRegistry() {
        return EventRegistry.get();
    }
    
    /**
     * Register a command with this plugin.
     */
    public void registerCommand(PluginCommand command) {
        getCommandManager().registerCommand(this, command);
    }
}
