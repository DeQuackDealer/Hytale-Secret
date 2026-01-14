package com.hypixel.hytale.server.core.event;

/**
 * Base event interface.
 */
public interface Event {
    
    boolean isCancelled();
    
    void setCancelled(boolean cancelled);
}
