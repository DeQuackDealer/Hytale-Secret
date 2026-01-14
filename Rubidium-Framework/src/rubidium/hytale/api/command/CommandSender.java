package rubidium.hytale.api.command;

/**
 * Interface for command senders.
 */
public interface CommandSender {
    
    String getName();
    
    void sendMessage(String message);
    
    boolean hasPermission(String permission);
}
