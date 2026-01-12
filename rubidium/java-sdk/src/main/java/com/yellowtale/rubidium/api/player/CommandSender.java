package com.yellowtale.rubidium.api.player;

public interface CommandSender {
    
    void sendMessage(String message);
    
    void sendMessage(String... messages);
    
    boolean hasPermission(String permission);
    
    boolean isPlayer();
    
    String getName();
}
