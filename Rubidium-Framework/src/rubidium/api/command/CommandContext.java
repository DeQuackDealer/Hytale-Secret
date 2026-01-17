package rubidium.api.command;

import rubidium.api.player.Player;

public interface CommandContext {
    
    Player getPlayer();
    
    String[] getArgs();
    
    String getArg(int index);
    
    int getArgAsInt(int index);
    
    double getArgAsDouble(int index);
    
    boolean hasArg(int index);
    
    int getArgCount();
    
    void sendMessage(String message);
    
    void sendSuccess(String message);
    
    void sendError(String message);
    
    void sendUsage();
}
