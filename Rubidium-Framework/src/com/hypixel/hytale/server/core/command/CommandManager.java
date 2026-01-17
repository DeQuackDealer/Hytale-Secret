package com.hypixel.hytale.server.core.command;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stub for Hytale's CommandManager.
 * At runtime, the real CommandManager from HytaleServer.jar will be used.
 */
public class CommandManager {
    
    private static final CommandManager INSTANCE = new CommandManager();
    private final Map<String, PluginCommand> commands = new ConcurrentHashMap<>();
    
    public static CommandManager get() {
        return INSTANCE;
    }
    
    public void registerCommand(JavaPlugin plugin, PluginCommand command) {
        commands.put(command.getName().toLowerCase(), command);
        for (String alias : command.getAliases()) {
            commands.put(alias.toLowerCase(), command);
        }
        System.out.println("[Hytale] Registered command: /" + command.getName());
    }
    
    public void unregisterCommand(String name) {
        commands.remove(name.toLowerCase());
    }
    
    public boolean handleCommand(CommandSender sender, String commandLine) {
        String[] parts = commandLine.split(" ");
        if (parts.length == 0) return false;
        
        String label = parts[0].toLowerCase();
        if (label.startsWith("/")) {
            label = label.substring(1);
        }
        
        PluginCommand command = commands.get(label);
        if (command == null) {
            return false;
        }
        
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);
        
        try {
            return command.execute(sender, label, args);
        } catch (Exception e) {
            sender.sendMessage("Error executing command: " + e.getMessage());
            return true;
        }
    }
}
