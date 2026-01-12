package com.yellowtale.rubidium.core.network;

/**
 * Handler for processing received packets.
 * 
 * @param <T> The packet type this handler processes
 */
@FunctionalInterface
public interface PacketHandler<T extends Packet> {
    
    /**
     * Handle a received packet.
     * 
     * @param connectionId The connection the packet came from
     * @param packet The received packet
     */
    void handle(String connectionId, T packet);
}
