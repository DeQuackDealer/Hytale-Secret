package rubidium.hytale.api.events;

import rubidium.hytale.api.event.Event;
import rubidium.hytale.api.player.Player;

/**
 * Event fired when a player quits the server.
 */
public class PlayerQuitEvent implements Event {
    
    private final Player player;
    private String quitMessage;
    private final QuitReason reason;
    
    public PlayerQuitEvent(Player player, String quitMessage, QuitReason reason) {
        this.player = player;
        this.quitMessage = quitMessage;
        this.reason = reason;
    }
    
    public Player getPlayer() { return player; }
    
    public String getQuitMessage() { return quitMessage; }
    public void setQuitMessage(String message) { this.quitMessage = message; }
    
    public QuitReason getReason() { return reason; }
    
    @Override
    public String getEventName() { return "PlayerQuitEvent"; }
    
    public enum QuitReason {
        DISCONNECTED,
        KICKED,
        TIMED_OUT,
        BANNED
    }
}
