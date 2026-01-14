package rubidium.hytale.api.events;

import rubidium.hytale.api.event.Event;
import rubidium.hytale.api.player.Player;
import rubidium.hytale.api.world.Location;

/**
 * Event fired when a player dies.
 */
public class PlayerDeathEvent implements Event {
    
    private final Player player;
    private final Player killer;
    private final String deathCause;
    private String deathMessage;
    private boolean keepInventory;
    private Location respawnLocation;
    
    public PlayerDeathEvent(Player player, Player killer, String deathCause, String deathMessage) {
        this.player = player;
        this.killer = killer;
        this.deathCause = deathCause;
        this.deathMessage = deathMessage;
        this.keepInventory = false;
    }
    
    public Player getPlayer() { return player; }
    public Player getKiller() { return killer; }
    public boolean hasKiller() { return killer != null; }
    
    public String getDeathCause() { return deathCause; }
    
    public String getDeathMessage() { return deathMessage; }
    public void setDeathMessage(String message) { this.deathMessage = message; }
    
    public boolean isKeepInventory() { return keepInventory; }
    public void setKeepInventory(boolean keep) { this.keepInventory = keep; }
    
    public Location getRespawnLocation() { return respawnLocation; }
    public void setRespawnLocation(Location location) { this.respawnLocation = location; }
    
    @Override
    public String getEventName() { return "PlayerDeathEvent"; }
}
