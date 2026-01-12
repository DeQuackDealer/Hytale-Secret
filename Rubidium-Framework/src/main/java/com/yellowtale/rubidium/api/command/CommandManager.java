package com.yellowtale.rubidium.api.command;

import com.yellowtale.rubidium.annotations.Command;
import com.yellowtale.rubidium.api.RubidiumPlugin;
import com.yellowtale.rubidium.api.player.CommandSender;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CommandManager {
    
    private final Map<String, RegisteredCommand> commands;
    private final Map<String, String> aliases;
    
    public CommandManager() {
        this.commands = new ConcurrentHashMap<>();
        this.aliases = new ConcurrentHashMap<>();
    }
    
    public void registerCommand(RubidiumPlugin plugin, String name, Object handler) {
        for (Method method : handler.getClass().getDeclaredMethods()) {
            Command cmd = method.getAnnotation(Command.class);
            if (cmd == null) continue;
            
            method.setAccessible(true);
            
            RegisteredCommand registered = new RegisteredCommand(
                plugin,
                cmd.name(),
                cmd.aliases(),
                cmd.description(),
                cmd.usage(),
                cmd.permission(),
                cmd.permissionMessage(),
                cmd.playerOnly(),
                cmd.minArgs(),
                cmd.maxArgs(),
                handler,
                method
            );
            
            commands.put(cmd.name().toLowerCase(), registered);
            
            for (String alias : cmd.aliases()) {
                aliases.put(alias.toLowerCase(), cmd.name().toLowerCase());
            }
        }
    }
    
    public void unregisterCommands(RubidiumPlugin plugin) {
        commands.entrySet().removeIf(e -> e.getValue().getPlugin().equals(plugin));
        aliases.entrySet().removeIf(e -> {
            RegisteredCommand cmd = commands.get(e.getValue());
            return cmd != null && cmd.getPlugin().equals(plugin);
        });
    }
    
    public boolean executeCommand(CommandSender sender, String commandLine) {
        String[] parts = commandLine.split("\\s+");
        if (parts.length == 0) return false;
        
        String name = parts[0].toLowerCase();
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);
        
        String resolvedName = aliases.getOrDefault(name, name);
        RegisteredCommand command = commands.get(resolvedName);
        
        if (command == null) {
            return false;
        }
        
        if (command.isPlayerOnly() && !sender.isPlayer()) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        
        if (!command.getPermission().isEmpty() && !sender.hasPermission(command.getPermission())) {
            sender.sendMessage(command.getPermissionMessage());
            return true;
        }
        
        if (args.length < command.getMinArgs()) {
            sender.sendMessage("Usage: " + command.getUsage());
            return true;
        }
        
        if (command.getMaxArgs() >= 0 && args.length > command.getMaxArgs()) {
            sender.sendMessage("Too many arguments. Usage: " + command.getUsage());
            return true;
        }
        
        try {
            command.execute(sender, args);
        } catch (Exception e) {
            sender.sendMessage("An error occurred while executing the command.");
            command.getPlugin().getLogger().error("Error executing command {}", name, e);
        }
        
        return true;
    }
    
    public Collection<RegisteredCommand> getCommands() {
        return Collections.unmodifiableCollection(commands.values());
    }
    
    public Optional<RegisteredCommand> getCommand(String name) {
        String resolvedName = aliases.getOrDefault(name.toLowerCase(), name.toLowerCase());
        return Optional.ofNullable(commands.get(resolvedName));
    }
    
    public List<String> tabComplete(CommandSender sender, String commandLine) {
        return Collections.emptyList();
    }
    
    public static class RegisteredCommand {
        private final RubidiumPlugin plugin;
        private final String name;
        private final String[] aliases;
        private final String description;
        private final String usage;
        private final String permission;
        private final String permissionMessage;
        private final boolean playerOnly;
        private final int minArgs;
        private final int maxArgs;
        private final Object handler;
        private final Method method;
        
        public RegisteredCommand(
                RubidiumPlugin plugin,
                String name,
                String[] aliases,
                String description,
                String usage,
                String permission,
                String permissionMessage,
                boolean playerOnly,
                int minArgs,
                int maxArgs,
                Object handler,
                Method method
        ) {
            this.plugin = plugin;
            this.name = name;
            this.aliases = aliases;
            this.description = description;
            this.usage = usage.isEmpty() ? "/" + name : usage;
            this.permission = permission;
            this.permissionMessage = permissionMessage;
            this.playerOnly = playerOnly;
            this.minArgs = minArgs;
            this.maxArgs = maxArgs;
            this.handler = handler;
            this.method = method;
        }
        
        public RubidiumPlugin getPlugin() { return plugin; }
        public String getName() { return name; }
        public String[] getAliases() { return aliases; }
        public String getDescription() { return description; }
        public String getUsage() { return usage; }
        public String getPermission() { return permission; }
        public String getPermissionMessage() { return permissionMessage; }
        public boolean isPlayerOnly() { return playerOnly; }
        public int getMinArgs() { return minArgs; }
        public int getMaxArgs() { return maxArgs; }
        
        public void execute(CommandSender sender, String[] args) throws Exception {
            method.invoke(handler, sender, args);
        }
    }
}
