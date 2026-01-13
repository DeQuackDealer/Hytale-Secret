package rubidium.command;

import rubidium.api.player.CommandSender;
import rubidium.api.RubidiumPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class CommandManager {
    
    private static final Logger logger = Logger.getLogger("Rubidium-Commands");
    
    private final Map<String, RegisteredCommand> commands;
    private final Map<String, String> aliasToCommand;
    
    public CommandManager() {
        this.commands = new ConcurrentHashMap<>();
        this.aliasToCommand = new ConcurrentHashMap<>();
    }
    
    public void register(RubidiumPlugin plugin, Command command) {
        String name = command.getName().toLowerCase();
        
        if (commands.containsKey(name)) {
            logger.warning("Command '" + name + "' is already registered, overwriting...");
        }
        
        commands.put(name, new RegisteredCommand(plugin, command));
        
        for (String alias : command.getAliases()) {
            aliasToCommand.put(alias.toLowerCase(), name);
        }
        
        logger.fine("Registered command: " + name);
    }
    
    public void register(RubidiumPlugin plugin, Command... commands) {
        for (Command command : commands) {
            register(plugin, command);
        }
    }
    
    public void unregister(String name) {
        name = name.toLowerCase();
        RegisteredCommand reg = commands.remove(name);
        
        if (reg != null) {
            for (String alias : reg.command().getAliases()) {
                aliasToCommand.remove(alias.toLowerCase());
            }
            logger.fine("Unregistered command: " + name);
        }
    }
    
    public void unregisterAll(RubidiumPlugin plugin) {
        List<String> toRemove = new ArrayList<>();
        
        for (Map.Entry<String, RegisteredCommand> entry : commands.entrySet()) {
            if (entry.getValue().plugin() == plugin) {
                toRemove.add(entry.getKey());
            }
        }
        
        for (String name : toRemove) {
            unregister(name);
        }
    }
    
    public boolean dispatch(CommandSender sender, String commandLine) {
        String[] parts = commandLine.split(" ");
        if (parts.length == 0) return false;
        
        String label = parts[0].toLowerCase();
        if (label.startsWith("/")) {
            label = label.substring(1);
        }
        
        String[] args = parts.length > 1 
            ? Arrays.copyOfRange(parts, 1, parts.length) 
            : new String[0];
        
        return execute(sender, label, args);
    }
    
    public boolean execute(CommandSender sender, String label, String[] args) {
        String commandName = resolveCommand(label);
        RegisteredCommand reg = commands.get(commandName);
        
        if (reg == null) {
            return false;
        }
        
        Command command = reg.command();
        
        if (command.getPermission() != null && !sender.hasPermission(command.getPermission())) {
            sender.sendMessage("You don't have permission to use this command.");
            return true;
        }
        
        try {
            return command.execute(sender, label, args);
        } catch (Exception e) {
            sender.sendMessage("An error occurred while executing the command.");
            logger.severe("Error executing command '" + label + "': " + e.getMessage());
            e.printStackTrace();
            return true;
        }
    }
    
    public List<String> tabComplete(CommandSender sender, String commandLine) {
        String[] parts = commandLine.split(" ", -1);
        if (parts.length == 0) return Collections.emptyList();
        
        String label = parts[0].toLowerCase();
        if (label.startsWith("/")) {
            label = label.substring(1);
        }
        
        if (parts.length == 1) {
            List<String> completions = new ArrayList<>();
            for (Map.Entry<String, RegisteredCommand> entry : commands.entrySet()) {
                if (entry.getKey().startsWith(label)) {
                    Command cmd = entry.getValue().command();
                    if (cmd.getPermission() == null || sender.hasPermission(cmd.getPermission())) {
                        completions.add(entry.getKey());
                    }
                }
            }
            return completions;
        }
        
        String commandName = resolveCommand(label);
        RegisteredCommand reg = commands.get(commandName);
        if (reg == null) return Collections.emptyList();
        
        Command command = reg.command();
        if (command.getPermission() != null && !sender.hasPermission(command.getPermission())) {
            return Collections.emptyList();
        }
        
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);
        return command.tabComplete(sender, args);
    }
    
    private String resolveCommand(String label) {
        label = label.toLowerCase();
        if (commands.containsKey(label)) {
            return label;
        }
        return aliasToCommand.getOrDefault(label, label);
    }
    
    public Optional<Command> getCommand(String name) {
        RegisteredCommand reg = commands.get(name.toLowerCase());
        return reg != null ? Optional.of(reg.command()) : Optional.empty();
    }
    
    public Collection<Command> getAllCommands() {
        List<Command> result = new ArrayList<>();
        for (RegisteredCommand reg : commands.values()) {
            result.add(reg.command());
        }
        return result;
    }
    
    public record RegisteredCommand(RubidiumPlugin plugin, Command command) {}
}
