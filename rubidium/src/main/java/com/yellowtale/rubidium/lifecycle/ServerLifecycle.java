package com.yellowtale.rubidium.lifecycle;

import com.yellowtale.rubidium.event.EventBus;
import com.yellowtale.rubidium.event.events.ServerStartEvent;
import com.yellowtale.rubidium.event.events.ServerStopEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerLifecycle {
    private static final Logger logger = LoggerFactory.getLogger(ServerLifecycle.class);
    
    private final EventBus eventBus;
    private ServerState state = ServerState.STOPPED;
    private long startTime = 0;
    
    public ServerLifecycle(EventBus eventBus) {
        this.eventBus = eventBus;
    }
    
    public void onServerStart() {
        state = ServerState.STARTING;
        logger.info("Server starting...");
        
        startTime = System.currentTimeMillis();
        eventBus.fire(new ServerStartEvent());
        
        state = ServerState.RUNNING;
        logger.info("Server started.");
    }
    
    public void onServerStop() {
        state = ServerState.STOPPING;
        logger.info("Server stopping...");
        
        eventBus.fire(new ServerStopEvent(ServerStopEvent.StopReason.SHUTDOWN));
        
        state = ServerState.STOPPED;
        logger.info("Server stopped.");
    }
    
    public ServerState getState() {
        return state;
    }
    
    public long getUptime() {
        if (state != ServerState.RUNNING || startTime == 0) {
            return 0;
        }
        return System.currentTimeMillis() - startTime;
    }
    
    public enum ServerState {
        STOPPED,
        STARTING,
        RUNNING,
        STOPPING,
        CRASHED
    }
}
