package rubidium.hytale.api.event;

/**
 * Base class for cancellable events.
 */
public abstract class CancellableEvent {
    
    private boolean cancelled = false;
    
    public boolean isCancelled() {
        return cancelled;
    }
    
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
