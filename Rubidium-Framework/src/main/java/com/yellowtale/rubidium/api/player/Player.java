package com.yellowtale.rubidium.api.player;

import java.util.UUID;

public interface Player extends CommandSender {
    
    UUID getUniqueId();
    
    String getName();
    
    String getDisplayName();
    
    void setDisplayName(String displayName);
    
    boolean isOnline();
    
    void kick(String reason);
    
    void teleport(double x, double y, double z);
    
    void teleport(double x, double y, double z, float yaw, float pitch);
    
    double getX();
    
    double getY();
    
    double getZ();
    
    float getYaw();
    
    float getPitch();
    
    String getWorld();
    
    int getPing();
    
    String getAddress();
    
    long getFirstPlayed();
    
    long getLastPlayed();
    
    boolean hasPlayedBefore();
    
    void setOp(boolean op);
    
    boolean isOp();
    
    void showTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut);
    
    void showActionBar(String message);
    
    void playSound(String sound, float volume, float pitch);
    
    PlayerInventory getInventory();
    
    PlayerData getData();
}
