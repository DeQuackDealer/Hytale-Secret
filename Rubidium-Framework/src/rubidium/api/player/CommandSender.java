package rubidium.api.player;

public interface CommandSender {
    
    String getName();
    
    void sendMessage(String message);
    
    void sendMessage(String... messages);
    
    boolean hasPermission(String permission);
    
    boolean isPlayer();
    
    boolean isConsole();
}
