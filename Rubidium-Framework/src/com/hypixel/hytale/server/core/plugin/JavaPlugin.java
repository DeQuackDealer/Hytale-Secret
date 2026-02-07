package com.hypixel.hytale.server.core.plugin;

import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.server.core.command.CommandManager;
import com.hypixel.hytale.server.core.command.PluginCommand;
import com.hypixel.hytale.server.core.event.EventRegistry;

import java.nio.file.Path;
import java.util.logging.Logger;

public abstract class JavaPlugin extends PluginBase {
    
    private final JavaPluginInit init;
    private final Logger logger;
    
    protected JavaPlugin(JavaPluginInit init) {
        this.init = init;
        this.logger = Logger.getLogger(getClass().getSimpleName());
    }
    
    protected void preLoad() {
    }
    
    protected void setup() {
    }
    
    protected void start() {
    }
    
    protected void shutdown() {
    }
    
    public String getName() {
        if (init != null) {
            PluginManifest m = init.getManifest();
            if (m != null) return m.getName();
        }
        return getClass().getSimpleName();
    }
    
    public PluginManifest getManifest() {
        return init != null ? init.getManifest() : null;
    }
    
    public Logger getLogger() {
        return logger;
    }
    
    public JavaPluginInit getInit() {
        return init;
    }
    
    public Path getDataDirectory() {
        return init != null ? init.getDataDirectory() : Path.of("plugins", getName());
    }
    
    public Path getFile() {
        return init != null ? init.getFile() : Path.of(getName() + ".jar");
    }
    
    public CommandManager getCommandRegistry() {
        return CommandManager.get();
    }
    
    public EventRegistry getEventRegistry() {
        return EventRegistry.get();
    }
    
    public void registerCommand(PluginCommand command) {
        getCommandRegistry().registerCommand(this, command);
    }
    
    public boolean isEnabled() {
        return true;
    }
}
