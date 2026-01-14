package rubidium.hytale.api.event;

import rubidium.hytale.api.player.Player;

/**
 * Rubidium's player join event.
 */
public class PlayerJoinEvent {
    
    private final Player player;
    private String joinMessage;
    
    public PlayerJoinEvent(Player player) {
        this.player = player;
        this.joinMessage = player.getUsername() + " joined the game";
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public String getJoinMessage() {
        return joinMessage;
    }
    
    public void setJoinMessage(String message) {
        this.joinMessage = message;
    }
}
