package rubidium.hytale.api.event;

import rubidium.hytale.api.player.Player;

/**
 * Rubidium's player quit event.
 */
public class PlayerQuitEvent {
    
    private final Player player;
    private String quitMessage;
    
    public PlayerQuitEvent(Player player) {
        this.player = player;
        this.quitMessage = player.getUsername() + " left the game";
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public String getQuitMessage() {
        return quitMessage;
    }
    
    public void setQuitMessage(String message) {
        this.quitMessage = message;
    }
}
