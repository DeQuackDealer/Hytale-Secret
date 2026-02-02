package com.hypixel.hytale.server.api.player;

import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUICommand;

import java.util.UUID;

public interface Player {
    
    UUID getUUID();
    
    String getName();
    
    boolean isOnline();
    
    void sendMessage(String message);
    
    void sendCustomPageOpen(String pageId, String uiPath, CustomPageLifetime lifetime, CustomUICommand[] commands);
    
    void sendCustomPageClose(String pageId);
    
    void sendCustomUICommands(String pageId, CustomUICommand[] commands);
    
    void kick(String reason);
    
    boolean hasPermission(String permission);
    
    void teleport(double x, double y, double z);
    
    double getX();
    double getY();
    double getZ();
    
    String getWorld();
}
