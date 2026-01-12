package com.yellowtale.rubidium.api.event.player;

import com.yellowtale.rubidium.api.event.Cancellable;
import com.yellowtale.rubidium.api.player.Player;

public class PlayerJoinEvent extends PlayerEvent implements Cancellable {
    
    private boolean cancelled;
    private String joinMessage;
    
    public PlayerJoinEvent(Player player, String joinMessage) {
        super(player);
        this.joinMessage = joinMessage;
    }
    
    public String getJoinMessage() {
        return joinMessage;
    }
    
    public void setJoinMessage(String joinMessage) {
        this.joinMessage = joinMessage;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
