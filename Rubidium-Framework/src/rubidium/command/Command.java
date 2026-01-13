package rubidium.command;

import rubidium.api.player.CommandSender;

import java.lang.annotation.*;
import java.util.*;

public abstract class Command {
    
    private final String name;
    private final String description;
    private final String usage;
    private final String permission;
    private final List<String> aliases;
    private final List<SubCommand> subCommands;
    
    protected Command(String name) {
        this(name, "", "/" + name, null);
    }
    
    protected Command(String name, String description, String usage, String permission) {
        this.name = name;
        this.description = description;
        this.usage = usage;
        this.permission = permission;
        this.aliases = new ArrayList<>();
        this.subCommands = new ArrayList<>();
        
        registerSubCommands();
    }
    
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getUsage() { return usage; }
    public String getPermission() { return permission; }
    public List<String> getAliases() { return Collections.unmodifiableList(aliases); }
    public List<SubCommand> getSubCommands() { return Collections.unmodifiableList(subCommands); }
    
    protected void addAlias(String... aliases) {
        this.aliases.addAll(Arrays.asList(aliases));
    }
    
    protected void addSubCommand(SubCommand subCommand) {
        this.subCommands.add(subCommand);
    }
    
    protected void registerSubCommands() {}
    
    public abstract boolean execute(CommandSender sender, String label, String[] args);
    
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && !subCommands.isEmpty()) {
            List<String> completions = new ArrayList<>();
            String partial = args[0].toLowerCase();
            for (SubCommand sub : subCommands) {
                if (sub.getName().toLowerCase().startsWith(partial)) {
                    if (sub.getPermission() == null || sender.hasPermission(sub.getPermission())) {
                        completions.add(sub.getName());
                    }
                }
            }
            return completions;
        }
        
        if (args.length > 1 && !subCommands.isEmpty()) {
            String subName = args[0].toLowerCase();
            for (SubCommand sub : subCommands) {
                if (sub.getName().equalsIgnoreCase(subName)) {
                    return sub.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
                }
            }
        }
        
        return Collections.emptyList();
    }
    
    protected boolean executeSubCommand(CommandSender sender, String[] args) {
        if (args.length == 0 || subCommands.isEmpty()) {
            return false;
        }
        
        String subName = args[0].toLowerCase();
        for (SubCommand sub : subCommands) {
            if (sub.getName().equalsIgnoreCase(subName)) {
                if (sub.getPermission() != null && !sender.hasPermission(sub.getPermission())) {
                    sender.sendMessage("You don't have permission to use this command.");
                    return true;
                }
                return sub.execute(sender, Arrays.copyOfRange(args, 1, args.length));
            }
        }
        
        return false;
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Info {
        String name();
        String description() default "";
        String usage() default "";
        String permission() default "";
        String[] aliases() default {};
    }
    
    public static abstract class SubCommand {
        private final String name;
        private final String description;
        private final String permission;
        
        protected SubCommand(String name) {
            this(name, "", null);
        }
        
        protected SubCommand(String name, String description, String permission) {
            this.name = name;
            this.description = description;
            this.permission = permission;
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getPermission() { return permission; }
        
        public abstract boolean execute(CommandSender sender, String[] args);
        
        public List<String> tabComplete(CommandSender sender, String[] args) {
            return Collections.emptyList();
        }
    }
}
