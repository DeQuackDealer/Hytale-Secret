package com.hypixel.hytale.server.core.event.events.player;

/**
 * Event fired when a player is removed from a world.
 */
public interface DrainPlayerFromWorldEvent extends PlayerEvent {
    
    String getWorld();
}
