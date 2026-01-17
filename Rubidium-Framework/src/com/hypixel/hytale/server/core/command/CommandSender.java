package com.hypixel.hytale.server.core.command;

import java.util.UUID;

/**
 * Stub for Hytale's CommandSender.
 * At runtime, the real CommandSender from HytaleServer.jar will be used.
 */
public interface CommandSender {
    
    void sendMessage(String message);
    
    boolean hasPermission(String permission);
    
    String getName();
    
    boolean isPlayer();
    
    boolean isConsole();
    
    UUID getUniqueId();
}
