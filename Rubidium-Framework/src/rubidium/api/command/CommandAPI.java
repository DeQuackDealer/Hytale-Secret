package rubidium.api.command;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class CommandAPI {
    
    private static final Map<String, CommandDefinition> commands = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> aliases = new ConcurrentHashMap<>();
    
    private CommandAPI() {}
    
    public static CommandDefinition.Builder create(String name) {
        return new CommandDefinition.Builder(name);
    }
    
    public static CommandDefinition register(CommandDefinition command) {
        commands.put(command.getName().toLowerCase(), command);
        for (String alias : command.getAliases()) {
            aliases.computeIfAbsent(alias.toLowerCase(), k -> new ArrayList<>()).add(command.getName());
        }
        return command;
    }
    
    public static CommandDefinition register(CommandDefinition.Builder builder) {
        return register(builder.build());
    }
    
    public static Optional<CommandDefinition> get(String name) {
        CommandDefinition cmd = commands.get(name.toLowerCase());
        if (cmd == null) {
            List<String> aliasTargets = aliases.get(name.toLowerCase());
            if (aliasTargets != null && !aliasTargets.isEmpty()) {
                cmd = commands.get(aliasTargets.get(0).toLowerCase());
            }
        }
        return Optional.ofNullable(cmd);
    }
    
    public static Collection<CommandDefinition> all() {
        return commands.values();
    }
    
    public static boolean execute(Object sender, String commandLine) {
        String[] parts = commandLine.split("\\s+", 2);
        String name = parts[0].startsWith("/") ? parts[0].substring(1) : parts[0];
        String[] args = parts.length > 1 ? parts[1].split("\\s+") : new String[0];
        
        return get(name).map(cmd -> {
            CommandContext ctx = new CommandContext(sender, name, args);
            return cmd.execute(ctx);
        }).orElse(false);
    }
    
    public static CommandDefinition simple(String name, String permission, BiConsumer<Object, String[]> handler) {
        return create(name)
            .permission(permission)
            .executor((ctx) -> { handler.accept(ctx.sender(), ctx.args()); return true; })
            .build();
    }
    
    public static CommandDefinition playerOnly(String name, String permission, BiConsumer<Object, String[]> handler) {
        return create(name)
            .permission(permission)
            .playerOnly()
            .executor((ctx) -> { handler.accept(ctx.sender(), ctx.args()); return true; })
            .build();
    }
    
    public record CommandContext(Object sender, String label, String[] args) {
        public String arg(int index) {
            return index < args.length ? args[index] : null;
        }
        
        public String arg(int index, String defaultValue) {
            return index < args.length ? args[index] : defaultValue;
        }
        
        public int argInt(int index, int defaultValue) {
            try {
                return index < args.length ? Integer.parseInt(args[index]) : defaultValue;
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        
        public double argDouble(int index, double defaultValue) {
            try {
                return index < args.length ? Double.parseDouble(args[index]) : defaultValue;
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        
        public boolean hasArg(int index) {
            return index < args.length;
        }
        
        public int argCount() {
            return args.length;
        }
        
        public String joinArgs(int startIndex) {
            if (startIndex >= args.length) return "";
            return String.join(" ", Arrays.copyOfRange(args, startIndex, args.length));
        }
    }
    
    public static class CommandDefinition {
        private final String name;
        private final String description;
        private final String usage;
        private final String permission;
        private final List<String> aliases;
        private final boolean playerOnly;
        private final Function<CommandContext, Boolean> executor;
        private final Function<CommandContext, List<String>> tabCompleter;
        private final List<SubCommand> subCommands;
        
        private CommandDefinition(Builder builder) {
            this.name = builder.name;
            this.description = builder.description;
            this.usage = builder.usage;
            this.permission = builder.permission;
            this.aliases = List.copyOf(builder.aliases);
            this.playerOnly = builder.playerOnly;
            this.executor = builder.executor;
            this.tabCompleter = builder.tabCompleter;
            this.subCommands = List.copyOf(builder.subCommands);
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getUsage() { return usage; }
        public String getPermission() { return permission; }
        public List<String> getAliases() { return aliases; }
        public boolean isPlayerOnly() { return playerOnly; }
        
        public boolean execute(CommandContext ctx) {
            if (!subCommands.isEmpty() && ctx.hasArg(0)) {
                String subName = ctx.arg(0);
                for (SubCommand sub : subCommands) {
                    if (sub.name().equalsIgnoreCase(subName)) {
                        String[] subArgs = Arrays.copyOfRange(ctx.args(), 1, ctx.args().length);
                        return sub.executor().apply(new CommandContext(ctx.sender(), sub.name(), subArgs));
                    }
                }
            }
            return executor != null && executor.apply(ctx);
        }
        
        public List<String> tabComplete(CommandContext ctx) {
            if (tabCompleter != null) return tabCompleter.apply(ctx);
            if (!subCommands.isEmpty() && ctx.argCount() == 1) {
                return subCommands.stream().map(SubCommand::name).toList();
            }
            return List.of();
        }
        
        public static class Builder {
            private final String name;
            private String description = "";
            private String usage = "";
            private String permission = null;
            private List<String> aliases = new ArrayList<>();
            private boolean playerOnly = false;
            private Function<CommandContext, Boolean> executor;
            private Function<CommandContext, List<String>> tabCompleter;
            private List<SubCommand> subCommands = new ArrayList<>();
            
            public Builder(String name) {
                this.name = name;
            }
            
            public Builder description(String desc) { this.description = desc; return this; }
            public Builder usage(String usage) { this.usage = usage; return this; }
            public Builder permission(String perm) { this.permission = perm; return this; }
            public Builder alias(String alias) { this.aliases.add(alias); return this; }
            public Builder aliases(String... aliases) { this.aliases.addAll(Arrays.asList(aliases)); return this; }
            public Builder playerOnly() { this.playerOnly = true; return this; }
            public Builder executor(Function<CommandContext, Boolean> exec) { this.executor = exec; return this; }
            public Builder tabCompleter(Function<CommandContext, List<String>> completer) { this.tabCompleter = completer; return this; }
            
            public Builder subCommand(String name, Function<CommandContext, Boolean> exec) {
                this.subCommands.add(new SubCommand(name, null, exec));
                return this;
            }
            
            public Builder subCommand(String name, String permission, Function<CommandContext, Boolean> exec) {
                this.subCommands.add(new SubCommand(name, permission, exec));
                return this;
            }
            
            public CommandDefinition build() {
                return new CommandDefinition(this);
            }
        }
        
        public record SubCommand(String name, String permission, Function<CommandContext, Boolean> executor) {}
    }
}
