package com.hypixel.hytale.server.core.plugin;

import com.hypixel.hytale.server.core.command.CommandManager;
import com.hypixel.hytale.server.core.command.PluginCommand;
import com.hypixel.hytale.server.core.event.EventRegistry;

import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Development stub for Hytale's JavaPlugin.
 * This stub allows compilation without the actual HytaleServer.jar.
 * At runtime, the real JavaPlugin from HytaleServer.jar will be used.
 * 
 * Hytale Plugin Lifecycle:
 *   NONE → PRELOAD → SETUP → START → ENABLED → SHUTDOWN → DISABLED
 * 
 * Note: Hytale does NOT use Bukkit-style onEnable/onDisable.
 * Use setup(), start(), and shutdown() instead.
 * 
 * DO NOT modify this file - it mirrors the official Hytale API.
 */
public abstract class JavaPlugin {
    
    private final JavaPluginInit init;
    private final Logger logger;
    private EventRegistry eventRegistry;
    private CommandManager commandManager;
    
    protected JavaPlugin(JavaPluginInit init) {
        this.init = init;
        this.logger = Logger.getLogger(getClass().getSimpleName());
        this.eventRegistry = EventRegistry.get();
        this.commandManager = CommandManager.get();
    }
    
    /**
     * Called during PRELOAD phase - before setup.
     * For early initialization before events/commands are registered.
     */
    protected void preLoad() {
    }
    
    /**
     * Called during SETUP phase - register events, commands, configs.
     * Override this to register event handlers and commands.
     */
    protected void setup() {
    }
    
    /**
     * Called during START phase - after setup, before players can join.
     * Override this for initialization that depends on other plugins.
     */
    protected void start() {
    }
    
    /**
     * Called during SHUTDOWN phase - cleanup before disable.
     */
    protected void shutdown() {
    }
    
    
    /**
     * Get the plugin name from manifest.
     */
    public String getName() {
        return init != null && init.getManifest() != null ? 
            init.getManifest().getName() : getClass().getSimpleName();
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
     * Get the plugin's data directory for storing configuration and data files.
     */
    public Path getDataDirectory() {
        return init != null ? init.getDataDirectory() : Path.of("plugins", getName());
    }
    
    /**
     * Get the plugin file path (the JAR file).
     */
    public Path getFile() {
        return init != null ? init.getFile() : Path.of(getName() + ".jar");
    }
    
    /**
     * Get the command registry for registering commands.
     * This is the Hytale way to access command registration.
     */
    public CommandManager getCommandRegistry() {
        return commandManager;
    }
    
    /**
     * Get the event registry for registering event listeners.
     * This returns the Hytale event registry for registering handlers.
     */
    public EventRegistry getEventRegistry() {
        return eventRegistry;
    }
    
    /**
     * Register a command with this plugin.
     */
    public void registerCommand(PluginCommand command) {
        getCommandRegistry().registerCommand(this, command);
    }
    
    /**
     * Check if the plugin is currently enabled.
     */
    public boolean isEnabled() {
        return true;
    }
    
    /**
     * Check if the plugin is currently disabled.
     */
    public boolean isDisabled() {
        return !isEnabled();
    }
    
    /**
     * Internal method called by Hytale to set the real event registry.
     * This allows our stub to work with the real registry at runtime.
     */
    public void setEventRegistry(EventRegistry registry) {
        this.eventRegistry = registry;
    }
    
    /**
     * Internal method called by Hytale to set the real command manager.
     */
    public void setCommandManager(CommandManager manager) {
        this.commandManager = manager;
    }
}
