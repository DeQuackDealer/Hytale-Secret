package com.hypixel.hytale.server.core.entity.entities.player;

import com.hypixel.hytale.server.core.entity.ServerEntity;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUICommand;

import java.util.UUID;

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
    
    PageManager getPageManager();
    
    HudManager getHudManager();
    
    void sendCustomPageOpen(String pageId, String uiPath, CustomPageLifetime lifetime, CustomUICommand[] commands);
    
    void sendCustomPageClose(String pageId);
    
    void sendCustomUICommands(String pageId, CustomUICommand[] commands);
    
    void teleport(double x, double y, double z);
    
    void teleport(double x, double y, double z, float yaw, float pitch);
    
    String getWorldName();
}
