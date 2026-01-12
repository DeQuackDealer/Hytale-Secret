package com.yellowtale.rubidium.api.anticheat;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class Finding {
    private final UUID id;
    private final UUID playerId;
    private final FindingType type;
    private final FindingLevel level;
    private final String description;
    private final String data;
    private final Instant timestamp;
    private final long tick;
    
    public Finding(UUID id, UUID playerId, FindingType type, FindingLevel level, 
                   String description, String data, Instant timestamp, long tick) {
        this.id = id;
        this.playerId = playerId;
        this.type = type;
        this.level = level;
        this.description = description;
        this.data = data;
        this.timestamp = timestamp;
        this.tick = tick;
    }
    
    public UUID getId() { return id; }
    public UUID getPlayerId() { return playerId; }
    public FindingType getType() { return type; }
    public FindingLevel getLevel() { return level; }
    public String getDescription() { return description; }
    public Optional<String> getData() { return Optional.ofNullable(data); }
    public Instant getTimestamp() { return timestamp; }
    public long getTick() { return tick; }
    
    public enum FindingType {
        SPEED_HACK,
        FLY_HACK,
        NO_FALL,
        TELEPORT,
        INVALID_MOVEMENT,
        HIGH_CPS,
        REACH,
        KILLAURA,
        INVALID_SWING,
        PACKET_FLOOD,
        INVALID_PACKET,
        KEEPALIVE_MANIPULATION,
        TIMER_HACK
    }
    
    public enum FindingLevel {
        INFO,
        SUSPICIOUS,
        LIKELY,
        DEFINITE
    }
}
