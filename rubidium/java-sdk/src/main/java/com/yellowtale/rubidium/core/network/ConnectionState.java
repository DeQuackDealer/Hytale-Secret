package com.yellowtale.rubidium.core.network;

/**
 * States for network connections.
 */
public enum ConnectionState {
    /**
     * Connection is being established.
     */
    CONNECTING,
    
    /**
     * Connection is open and ready for data.
     */
    CONNECTED,
    
    /**
     * Connection is being gracefully closed.
     */
    DISCONNECTING,
    
    /**
     * Connection has been closed.
     */
    DISCONNECTED,
    
    /**
     * Connection failed or errored.
     */
    FAILED
}
