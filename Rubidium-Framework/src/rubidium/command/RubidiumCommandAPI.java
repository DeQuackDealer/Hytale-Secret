package rubidium.command;

import com.hypixel.hytale.server.core.command.CommandSender;
import com.hypixel.hytale.server.core.command.PluginCommand;
import com.hypixel.hytale.server.core.command.CommandManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

public class RubidiumCommandAPI {
    
    private static final RubidiumCommandAPI INSTANCE = new RubidiumCommandAPI();
    
    private final Map<String, RubidiumCommand> commands = new ConcurrentHashMap<>();
    private final Map<String, String> aliases = new ConcurrentHashMap<>();
    
    public static RubidiumCommandAPI get() {
        return INSTANCE;
    }
    
    public void register(RubidiumCommand command) {
        String name = command.getName().toLowerCase();
        commands.put(name, command);
        
        for (String alias : command.getAliases()) {
            aliases.put(alias.toLowerCase(), name);
        }
        
        CommandManager.get().registerCommand(null, new PluginCommand(
            command.getName(),
            command.getDescription(),
            command.getPermission(),
            command.getAliases()
        ) {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                return command.execute(new RubidiumCommandContext(sender, label, args));
            }
            
            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                return command.tabComplete(new RubidiumCommandContext(sender, "", args));
            }
        });
        
        System.out.println("[Rubidium-Commands] Registered: /" + name);
    }
    
    public void unregister(String name) {
        RubidiumCommand cmd = commands.remove(name.toLowerCase());
        if (cmd != null) {
            for (String alias : cmd.getAliases()) {
                aliases.remove(alias.toLowerCase());
            }
            CommandManager.get().unregisterCommand(name);
        }
    }
    
    public Optional<RubidiumCommand> getCommand(String name) {
        String resolved = aliases.getOrDefault(name.toLowerCase(), name.toLowerCase());
        return Optional.ofNullable(commands.get(resolved));
    }
    
    public Collection<RubidiumCommand> getAllCommands() {
        return Collections.unmodifiableCollection(commands.values());
    }
    
    public static abstract class RubidiumCommand {
        protected final String name;
        protected final String description;
        protected final String permission;
        protected final String[] aliases;
        
        public RubidiumCommand(String name, String description, String permission, String... aliases) {
            this.name = name;
            this.description = description;
            this.permission = permission;
            this.aliases = aliases != null ? aliases : new String[0];
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getPermission() { return permission; }
        public String[] getAliases() { return aliases; }
        
        public abstract boolean execute(RubidiumCommandContext ctx);
        
        public List<String> tabComplete(RubidiumCommandContext ctx) {
            return Collections.emptyList();
        }
    }
    
    public static class RubidiumCommandContext {
        private final CommandSender sender;
        private final String label;
        private final String[] args;
        
        public RubidiumCommandContext(CommandSender sender, String label, String[] args) {
            this.sender = sender;
            this.label = label;
            this.args = args;
        }
        
        public CommandSender getSender() { return sender; }
        public String getLabel() { return label; }
        public String[] getArgs() { return args; }
        
        public int argCount() { return args.length; }
        
        public String arg(int index) {
            return index >= 0 && index < args.length ? args[index] : null;
        }
        
        public String arg(int index, String defaultValue) {
            String value = arg(index);
            return value != null ? value : defaultValue;
        }
        
        public int argInt(int index, int defaultValue) {
            try {
                return Integer.parseInt(arg(index));
            } catch (Exception e) {
                return defaultValue;
            }
        }
        
        public float argFloat(int index, float defaultValue) {
            try {
                return Float.parseFloat(arg(index));
            } catch (Exception e) {
                return defaultValue;
            }
        }
        
        public void reply(String message) {
            sender.sendMessage(message);
        }
        
        public void success(String message) {
            sender.sendMessage("&a" + message);
        }
        
        public void error(String message) {
            sender.sendMessage("&c" + message);
        }
        
        public void info(String message) {
            sender.sendMessage("&7" + message);
        }
        
        public boolean isPlayer() {
            return sender.isPlayer();
        }
        
        public boolean hasPermission(String permission) {
            return permission == null || sender.hasPermission(permission);
        }
    }
    
    public static class CommandBuilder {
        private final String name;
        private String description = "";
        private String permission = null;
        private List<String> aliases = new ArrayList<>();
        private BiFunction<RubidiumCommandContext, String[], Boolean> executor;
        private BiFunction<RubidiumCommandContext, String[], List<String>> tabCompleter;
        
        public CommandBuilder(String name) {
            this.name = name;
        }
        
        public CommandBuilder description(String desc) {
            this.description = desc;
            return this;
        }
        
        public CommandBuilder permission(String perm) {
            this.permission = perm;
            return this;
        }
        
        public CommandBuilder aliases(String... aliases) {
            this.aliases.addAll(Arrays.asList(aliases));
            return this;
        }
        
        public CommandBuilder executor(BiFunction<RubidiumCommandContext, String[], Boolean> exec) {
            this.executor = exec;
            return this;
        }
        
        public CommandBuilder tabComplete(BiFunction<RubidiumCommandContext, String[], List<String>> tab) {
            this.tabCompleter = tab;
            return this;
        }
        
        public RubidiumCommand build() {
            return new RubidiumCommand(name, description, permission, aliases.toArray(new String[0])) {
                @Override
                public boolean execute(RubidiumCommandContext ctx) {
                    if (executor != null) {
                        return executor.apply(ctx, ctx.getArgs());
                    }
                    return false;
                }
                
                @Override
                public List<String> tabComplete(RubidiumCommandContext ctx) {
                    if (tabCompleter != null) {
                        return tabCompleter.apply(ctx, ctx.getArgs());
                    }
                    return super.tabComplete(ctx);
                }
            };
        }
        
        public void register() {
            RubidiumCommandAPI.get().register(build());
        }
    }
    
    public static CommandBuilder command(String name) {
        return new CommandBuilder(name);
    }
}
