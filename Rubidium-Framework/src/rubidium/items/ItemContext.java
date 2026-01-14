package rubidium.items;

import rubidium.hytale.api.player.Player;
import rubidium.hytale.api.world.Location;

/**
 * Context for item interactions.
 */
public class ItemContext {
    
    private final Player player;
    private final CustomItem item;
    private final Action action;
    private final Location location;
    private boolean cancelled;
    
    public ItemContext(Player player, CustomItem item, Action action, Location location) {
        this.player = player;
        this.item = item;
        this.action = action;
        this.location = location;
        this.cancelled = false;
    }
    
    public Player getPlayer() { return player; }
    public CustomItem getItem() { return item; }
    public Action getAction() { return action; }
    public Location getLocation() { return location; }
    
    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    
    public enum Action {
        LEFT_CLICK,
        RIGHT_CLICK,
        USE,
        DROP,
        PICKUP,
        EQUIP,
        UNEQUIP
    }
}
