package com.yellowtale.rubidium.api.event;

public abstract class Event {
    
    private final boolean async;
    private String name;
    
    protected Event() {
        this(false);
    }
    
    protected Event(boolean async) {
        this.async = async;
    }
    
    public String getEventName() {
        if (name == null) {
            name = getClass().getSimpleName();
        }
        return name;
    }
    
    public boolean isAsynchronous() {
        return async;
    }
}
