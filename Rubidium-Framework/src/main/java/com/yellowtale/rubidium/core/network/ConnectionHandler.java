package com.yellowtale.rubidium.core.network;

/**
 * Interface for connection implementations.
 * 
 * This abstraction allows different transport mechanisms to be used
 * (TCP, UDP, WebSocket, etc.) without changing the network layer.
 * 
 * TODO: Implement actual connection handlers when Hytale server API is available
 */
public interface ConnectionHandler {
    
    /**
     * Get the unique connection identifier.
     */
    String getId();
    
    /**
     * Get the remote address of this connection.
     */
    String getRemoteAddress();
    
    /**
     * Check if the connection is open.
     */
    boolean isOpen();
    
    /**
     * Send a packet through this connection.
     * 
     * @param packet The packet to send
     */
    void send(Packet packet);
    
    /**
     * Close the connection.
     */
    void close();
    
    /**
     * Get connection state.
     */
    ConnectionState getState();
    
    /**
     * Get the latency in milliseconds.
     */
    default int getLatencyMs() {
        return -1;
    }
    
    /**
     * Get the time this connection was established.
     */
    default long getConnectedTime() {
        return System.currentTimeMillis();
    }
}
