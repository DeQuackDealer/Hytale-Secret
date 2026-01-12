package com.yellowtale.rubidium.core.network;

/**
 * Base interface for network packets.
 * 
 * Implementations should:
 * - Be immutable
 * - Provide efficient serialization
 * - Report accurate size estimates
 * 
 * NOTE: This is an abstraction. Actual packet format will depend
 * on the Hytale server protocol when available.
 */
public interface Packet {
    
    /**
     * Get the packet type identifier.
     * Used for routing and handler lookup.
     */
    int getTypeId();
    
    /**
     * Get the estimated size of this packet in bytes.
     * Used for bandwidth limiting and batching decisions.
     */
    int getSize();
    
    /**
     * Serialize this packet to a byte array.
     * 
     * TODO: Replace with actual protocol serialization
     */
    default byte[] serialize() {
        throw new UnsupportedOperationException(
            "Packet serialization requires Hytale server API"
        );
    }
    
    /**
     * Check if this packet requires reliable delivery.
     */
    default boolean isReliable() {
        return true;
    }
    
    /**
     * Check if this packet can be combined with others.
     */
    default boolean isBatchable() {
        return true;
    }
}
