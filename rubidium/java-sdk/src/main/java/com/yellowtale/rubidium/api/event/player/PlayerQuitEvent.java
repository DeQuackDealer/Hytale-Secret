package com.yellowtale.rubidium.api.event.player;

import com.yellowtale.rubidium.api.player.Player;

public class PlayerQuitEvent extends PlayerEvent {
    
    private String quitMessage;
    private final QuitReason reason;
    
    public PlayerQuitEvent(Player player, String quitMessage, QuitReason reason) {
        super(player);
        this.quitMessage = quitMessage;
        this.reason = reason;
    }
    
    public String getQuitMessage() {
        return quitMessage;
    }
    
    public void setQuitMessage(String quitMessage) {
        this.quitMessage = quitMessage;
    }
    
    public QuitReason getReason() {
        return reason;
    }
    
    public enum QuitReason {
        DISCONNECTED,
        KICKED,
        TIMED_OUT,
        SERVER_SHUTDOWN
    }
}
