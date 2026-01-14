package com.hypixel.hytale.server.core.event.events.ecs;

import com.hypixel.hytale.server.core.event.Event;

/**
 * Event fired when a block is broken.
 */
public interface BreakBlockEvent extends Event {
    
    int getX();
    int getY();
    int getZ();
    
    String getWorld();
    
    String getBlockType();
}
