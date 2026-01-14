package com.hypixel.hytale.server.core.event.events.player;

import com.hypixel.hytale.server.core.entity.entities.player.ServerPlayer;
import com.hypixel.hytale.server.core.event.Event;

/**
 * Base player event.
 */
public interface PlayerEvent extends Event {
    
    ServerPlayer getPlayer();
}
