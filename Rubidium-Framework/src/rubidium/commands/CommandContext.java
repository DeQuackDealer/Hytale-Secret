package rubidium.commands;

import java.util.Arrays;

public class CommandContext {
    
    private final CommandSender sender;
    private final String label;
    private final String[] args;
    
    public CommandContext(CommandSender sender, String label, String[] args) {
        this.sender = sender;
        this.label = label;
        this.args = args != null ? args : new String[0];
    }
    
    public CommandSender sender() { 
        return sender; 
    }
    
    public String label() { 
        return label; 
    }
    
    public String[] args() { 
        return args; 
    }
    
    public int argCount() { 
        return args.length; 
    }
    
    public String arg(int index) {
        return index >= 0 && index < args.length ? args[index] : null;
    }
    
    public String arg(int index, String defaultValue) {
        return index >= 0 && index < args.length ? args[index] : defaultValue;
    }
    
    public int argAsInt(int index, int defaultValue) {
        try {
            return Integer.parseInt(arg(index, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public double argAsDouble(int index, double defaultValue) {
        try {
            return Double.parseDouble(arg(index, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public String joinArgs(int start) {
        if (start >= args.length) return "";
        return String.join(" ", Arrays.copyOfRange(args, start, args.length));
    }
    
    public void reply(String message) {
        sender.sendMessage(message);
    }
    
    public void replyError(String message) {
        sender.sendMessage("§c" + message);
    }
    
    public void replySuccess(String message) {
        sender.sendMessage("§a" + message);
    }
    
    public boolean hasPermission(String permission) {
        return sender.hasPermission(permission);
    }
}
