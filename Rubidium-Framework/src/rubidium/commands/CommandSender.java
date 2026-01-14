package rubidium.commands;

import java.util.UUID;

public interface CommandSender {
    
    String getName();
    
    void sendMessage(String message);
    
    boolean hasPermission(String permission);
    
    default UUID getUniqueId() {
        return null;
    }
    
    default boolean isPlayer() {
        return getUniqueId() != null;
    }
    
    default boolean isConsole() {
        return !isPlayer();
    }
}
