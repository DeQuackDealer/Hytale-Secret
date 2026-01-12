package com.yellowtale.rubidium.api.event.player;

import com.yellowtale.rubidium.api.event.Event;
import com.yellowtale.rubidium.api.player.Player;

public abstract class PlayerEvent extends Event {
    
    protected final Player player;
    
    protected PlayerEvent(Player player) {
        this.player = player;
    }
    
    protected PlayerEvent(Player player, boolean async) {
        super(async);
        this.player = player;
    }
    
    public Player getPlayer() {
        return player;
    }
}
