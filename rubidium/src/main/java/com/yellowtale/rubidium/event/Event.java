package com.yellowtale.rubidium.event;

public abstract class Event {
    private final long timestamp;
    private final boolean async;
    
    protected Event() {
        this(false);
    }
    
    protected Event(boolean async) {
        this.timestamp = System.currentTimeMillis();
        this.async = async;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public boolean isAsync() {
        return async;
    }
    
    public String getEventName() {
        return getClass().getSimpleName();
    }
}
