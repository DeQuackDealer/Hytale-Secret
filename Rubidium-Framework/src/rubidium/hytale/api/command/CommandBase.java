package rubidium.hytale.api.command;

import rubidium.hytale.api.player.Player;

import java.util.*;

/**
 * Base class for command handlers.
 */
public abstract class CommandBase {
    
    private final String name;
    private String description = "";
    private String usage = "";
    private String permission;
    private final List<String> aliases = new ArrayList<>();
    
    public CommandBase(String name) {
        this.name = name;
    }
    
    public abstract void executeSync(CommandContext context);
    
    public List<String> tabComplete(CommandContext context) {
        return Collections.emptyList();
    }
    
    public String getName() { return name; }
    
    public String getDescription() { return description; }
    public CommandBase setDescription(String description) { 
        this.description = description; 
        return this; 
    }
    
    public String getUsage() { return usage; }
    public CommandBase setUsage(String usage) { 
        this.usage = usage; 
        return this; 
    }
    
    public String getPermission() { return permission; }
    public CommandBase setPermission(String permission) { 
        this.permission = permission; 
        return this; 
    }
    
    public List<String> getAliases() { return aliases; }
    public CommandBase addAlias(String alias) { 
        aliases.add(alias); 
        return this; 
    }
    
    public boolean hasPermission(Player player) {
        return permission == null || player.hasPermission(permission);
    }
}
