package com.hypixel.hytale.server.core;

/**
 * Stub interface for the Hytale server main class.
 * This is replaced by the real implementation from HytaleServer.jar at runtime.
 */
public interface HytaleServer {
    
    String getServerName();
    
    String getVersion();
    
    void broadcast(String message);
    
    int getOnlinePlayerCount();
    
    int getMaxPlayers();
    
    boolean isRunning();
    
    void shutdown();
}
