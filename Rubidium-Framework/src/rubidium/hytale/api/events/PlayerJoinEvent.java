package rubidium.hytale.api.events;

import rubidium.hytale.api.event.Event;
import rubidium.hytale.api.event.Cancellable;
import rubidium.hytale.api.player.Player;

/**
 * Event fired when a player joins the server.
 */
public class PlayerJoinEvent implements Event, Cancellable {
    
    private final Player player;
    private String joinMessage;
    private boolean cancelled;
    private final boolean firstJoin;
    
    public PlayerJoinEvent(Player player, String joinMessage, boolean firstJoin) {
        this.player = player;
        this.joinMessage = joinMessage;
        this.firstJoin = firstJoin;
    }
    
    public Player getPlayer() { return player; }
    
    public String getJoinMessage() { return joinMessage; }
    public void setJoinMessage(String message) { this.joinMessage = message; }
    
    public boolean isFirstJoin() { return firstJoin; }
    
    @Override
    public boolean isCancelled() { return cancelled; }
    
    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    
    @Override
    public String getEventName() { return "PlayerJoinEvent"; }
}
