package rubidium.api.event.player;

import rubidium.api.player.Player;
import rubidium.api.event.Cancellable;
import rubidium.inventory.ItemStack;

public class PlayerInteractEvent extends PlayerEvent implements Cancellable {
    
    private final String action;
    private final ItemStack itemInHand;
    private boolean cancelled = false;
    
    public PlayerInteractEvent(Player player, String action, ItemStack itemInHand) {
        super(player);
        this.action = action;
        this.itemInHand = itemInHand;
    }
    
    public String getAction() {
        return action;
    }
    
    public ItemStack getItemInHand() {
        return itemInHand;
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
