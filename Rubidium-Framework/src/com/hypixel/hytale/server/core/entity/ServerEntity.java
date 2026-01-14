package com.hypixel.hytale.server.core.entity;

import java.util.UUID;

/**
 * Stub interface for server entities.
 * Replaced by real implementation at runtime.
 */
public interface ServerEntity {
    
    UUID getUuid();
    
    int getEntityId();
    
    double getX();
    double getY();
    double getZ();
    
    float getYaw();
    float getPitch();
    
    String getWorld();
    
    void teleport(double x, double y, double z);
    void teleport(double x, double y, double z, float yaw, float pitch);
    
    boolean isValid();
    
    void remove();
    
    void sendPacket(Object packet);
}
