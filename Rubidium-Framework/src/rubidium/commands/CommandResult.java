package rubidium.commands;

public record CommandResult(
    boolean succeeded,
    String message
) {
    public boolean success() {
        return succeeded;
    }
    
    public static CommandResult ok() {
        return new CommandResult(true, null);
    }
    
    public static CommandResult ok(String msg) {
        return new CommandResult(true, msg);
    }
    
    public static CommandResult failure(String msg) {
        return new CommandResult(false, msg);
    }
    
    public static CommandResult noPermission() {
        return new CommandResult(false, "You don't have permission to use this command.");
    }
    
    public static CommandResult playerOnly() {
        return new CommandResult(false, "This command can only be used by players.");
    }
    
    public static CommandResult invalidArgs(String usage) {
        return new CommandResult(false, "Invalid arguments. Usage: " + usage);
    }
}
