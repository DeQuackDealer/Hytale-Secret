package com.yellowtale.rubidium.api.event;

public interface Cancellable {
    
    boolean isCancelled();
    
    void setCancelled(boolean cancelled);
}
