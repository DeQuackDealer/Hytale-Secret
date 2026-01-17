package com.hypixel.hytale.server.core.command;

import java.util.Collections;
import java.util.List;

/**
 * Stub for Hytale's PluginCommand.
 * At runtime, the real PluginCommand from HytaleServer.jar will be used.
 */
public abstract class PluginCommand {
    
    private final String name;
    private final String description;
    private final String permission;
    private final String[] aliases;
    
    public PluginCommand(String name) {
        this(name, "", null);
    }
    
    public PluginCommand(String name, String description, String permission, String... aliases) {
        this.name = name;
        this.description = description;
        this.permission = permission;
        this.aliases = aliases != null ? aliases : new String[0];
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getPermission() {
        return permission;
    }
    
    public String[] getAliases() {
        return aliases;
    }
    
    public abstract boolean execute(CommandSender sender, String label, String[] args);
    
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
