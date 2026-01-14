package com.hypixel.hytale.server.core.event.events.player;

/**
 * Event fired when a player is added to a world.
 */
public interface AddPlayerToWorldEvent extends PlayerEvent {
    
    String getWorld();
}
