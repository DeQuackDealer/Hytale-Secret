package rubidium.hytale.api.event;

/**
 * Interface for cancellable events.
 */
public interface Cancellable {
    
    boolean isCancelled();
    
    void setCancelled(boolean cancelled);
}
