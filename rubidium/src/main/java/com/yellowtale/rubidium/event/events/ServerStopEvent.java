package com.yellowtale.rubidium.event.events;

import com.yellowtale.rubidium.event.Event;

public class ServerStopEvent extends Event {
    private final StopReason reason;
    
    public ServerStopEvent(StopReason reason) {
        super();
        this.reason = reason;
    }
    
    public StopReason getReason() {
        return reason;
    }
    
    public enum StopReason {
        SHUTDOWN,
        CRASH,
        RESTART,
        UNKNOWN
    }
}
