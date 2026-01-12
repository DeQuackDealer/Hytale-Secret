package com.yellowtale.rubidium.core.network;

/**
 * Priority levels for network packets.
 * Higher priority packets are sent first.
 */
public enum PacketPriority {
    /**
     * Low priority, can be delayed under load.
     * Use for non-essential data like cosmetics, analytics.
     */
    LOW,
    
    /**
     * Normal priority, standard delivery.
     * Use for regular game data.
     */
    NORMAL,
    
    /**
     * High priority, preferred delivery.
     * Use for important game state updates.
     */
    HIGH,
    
    /**
     * Critical priority, immediate delivery.
     * Use for authentication, disconnection, critical state.
     */
    CRITICAL
}
