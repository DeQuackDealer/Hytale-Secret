package com.yellowtale.rubidium.core.network;

/**
 * Interceptor for inspecting and modifying packets.
 * 
 * Interceptors are called in order for both inbound and outbound packets.
 * Returning null from an interceptor method will cancel the packet.
 */
public interface PacketInterceptor {
    
    /**
     * Intercept an inbound packet.
     * 
     * @param connectionId The source connection
     * @param packet The received packet
     * @return The packet to process (may be modified), or null to cancel
     */
    default Packet onInbound(String connectionId, Packet packet) {
        return packet;
    }
    
    /**
     * Intercept an outbound packet.
     * 
     * @param connectionId The target connection
     * @param packet The packet to send
     * @return The packet to send (may be modified), or null to cancel
     */
    default Packet onOutbound(String connectionId, Packet packet) {
        return packet;
    }
}
