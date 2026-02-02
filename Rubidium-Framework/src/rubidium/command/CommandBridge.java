package rubidium.command;

import com.hypixel.hytale.server.core.command.CommandSender;
import com.hypixel.hytale.server.core.command.PluginCommand;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import rubidium.api.command.CommandAPI;
import rubidium.api.command.CommandAPI.CommandDefinition;
import rubidium.api.command.CommandAPI.CommandContext;

import java.util.*;
import java.util.logging.Logger;

public final class CommandBridge {
    
    private static final Logger LOGGER = Logger.getLogger("Rubidium-CommandBridge");
    private static JavaPlugin plugin;
    private static boolean initialized = false;
    
    private CommandBridge() {}
    
    public static void initialize(JavaPlugin pluginInstance) {
        if (initialized) return;
        plugin = pluginInstance;
        initialized = true;
        LOGGER.info("[CommandBridge] Command bridge initialized");
    }
    
    public static void registerWithHytale(CommandDefinition definition) {
        if (plugin == null) {
            LOGGER.warning("[CommandBridge] Cannot register command - plugin not initialized");
            return;
        }
        
        String[] aliasArray = definition.getAliases().toArray(new String[0]);
        
        PluginCommand hytaleCommand = new PluginCommand(
            definition.getName(),
            definition.getDescription(),
            definition.getPermission(),
            aliasArray
        ) {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                CommandContext ctx = new CommandContext(sender, label, args);
                return definition.execute(ctx);
            }
            
            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                CommandContext ctx = new CommandContext(sender, "", args);
                return definition.tabComplete(ctx);
            }
        };
        
        plugin.registerCommand(hytaleCommand);
        LOGGER.info("[CommandBridge] Registered command /" + definition.getName() + " with Hytale");
    }
    
    public static void registerAllPending() {
        if (plugin == null) {
            LOGGER.warning("[CommandBridge] Cannot register commands - plugin not initialized");
            return;
        }
        
        for (CommandDefinition cmd : CommandAPI.all()) {
            registerWithHytale(cmd);
        }
        
        LOGGER.info("[CommandBridge] Registered " + CommandAPI.all().size() + " commands with Hytale");
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
}
