package rubidium.hytale.api.command;

import rubidium.hytale.api.player.Player;

/**
 * Context for command execution.
 */
public class CommandContext {
    
    private final CommandSender sender;
    private final Player player;
    private final String label;
    private final String[] args;
    
    public CommandContext(CommandSender sender, String label, String[] args) {
        this.sender = sender;
        this.player = sender instanceof Player p ? p : null;
        this.label = label;
        this.args = args;
    }
    
    public CommandSender getSender() { return sender; }
    
    public Player getPlayer() { return player; }
    
    public boolean isPlayer() { return player != null; }
    
    public String getLabel() { return label; }
    
    public String[] getArgs() { return args; }
    
    public int getArgCount() { return args.length; }
    
    public String getArg(int index) {
        return index >= 0 && index < args.length ? args[index] : null;
    }
    
    public String getArg(int index, String defaultValue) {
        return index >= 0 && index < args.length ? args[index] : defaultValue;
    }
    
    public int getArgAsInt(int index, int defaultValue) {
        try {
            return Integer.parseInt(getArg(index, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public double getArgAsDouble(int index, double defaultValue) {
        try {
            return Double.parseDouble(getArg(index, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public String joinArgs(int start) {
        if (start >= args.length) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) sb.append(" ");
            sb.append(args[i]);
        }
        return sb.toString();
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
}
