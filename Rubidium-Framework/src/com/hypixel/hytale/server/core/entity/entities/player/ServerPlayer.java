package com.hypixel.hytale.server.core.entity.entities.player;

import com.hypixel.hytale.server.core.entity.ServerEntity;

import java.util.UUID;

/**
 * Stub interface for server players.
 * Replaced by real implementation at runtime.
 */
public interface ServerPlayer extends ServerEntity {
    
    String getUsername();
    
    String getDisplayName();
    void setDisplayName(String displayName);
    
    void sendMessage(String message);
    
    void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut);
    
    void sendActionBar(String message);
    
    boolean hasPermission(String permission);
    
    void kick(String reason);
    
    boolean isOnline();
    
    int getPing();
    
    String getGameMode();
    void setGameMode(String gameMode);
    
    float getHealth();
    void setHealth(float health);
    
    float getMaxHealth();
    void setMaxHealth(float maxHealth);
    
    void playSound(String sound, float volume, float pitch);
    
    void sendPacket(Object packet);
}
