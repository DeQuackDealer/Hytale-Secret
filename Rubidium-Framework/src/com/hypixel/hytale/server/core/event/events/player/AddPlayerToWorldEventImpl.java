package com.hypixel.hytale.server.core.event.events.player;

import com.hypixel.hytale.server.core.entity.entities.player.ServerPlayer;

public class AddPlayerToWorldEventImpl implements AddPlayerToWorldEvent {
    
    private final ServerPlayer player;
    private final String world;
    private boolean cancelled = false;
    
    public AddPlayerToWorldEventImpl(ServerPlayer player, String world) {
        this.player = player;
        this.world = world;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    @Override
    public ServerPlayer getPlayer() {
        return player;
    }
    
    @Override
    public String getWorld() {
        return world;
    }
}
