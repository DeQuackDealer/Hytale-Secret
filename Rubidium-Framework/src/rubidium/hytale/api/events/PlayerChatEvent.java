package rubidium.hytale.api.events;

import rubidium.hytale.api.event.Event;
import rubidium.hytale.api.event.Cancellable;
import rubidium.hytale.api.player.Player;

import java.util.Set;

/**
 * Event fired when a player sends a chat message.
 */
public class PlayerChatEvent implements Event, Cancellable {
    
    private final Player player;
    private String message;
    private String format;
    private Set<Player> recipients;
    private boolean cancelled;
    
    public PlayerChatEvent(Player player, String message, String format, Set<Player> recipients) {
        this.player = player;
        this.message = message;
        this.format = format;
        this.recipients = recipients;
    }
    
    public Player getPlayer() { return player; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    
    public Set<Player> getRecipients() { return recipients; }
    public void setRecipients(Set<Player> recipients) { this.recipients = recipients; }
    
    @Override
    public boolean isCancelled() { return cancelled; }
    
    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    
    @Override
    public String getEventName() { return "PlayerChatEvent"; }
}
