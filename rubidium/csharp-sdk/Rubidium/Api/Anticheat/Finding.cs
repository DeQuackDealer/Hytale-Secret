using System;

namespace Rubidium.Api.Anticheat
{
    public class Finding
    {
        public Guid Id { get; }
        public Guid PlayerId { get; }
        public FindingType Type { get; }
        public FindingLevel Level { get; }
        public string Description { get; }
        public string? Data { get; }
        public DateTimeOffset Timestamp { get; }
        public long Tick { get; }
        
        public Finding(Guid id, Guid playerId, FindingType type, FindingLevel level,
                       string description, string? data, DateTimeOffset timestamp, long tick)
        {
            Id = id;
            PlayerId = playerId;
            Type = type;
            Level = level;
            Description = description;
            Data = data;
            Timestamp = timestamp;
            Tick = tick;
        }
    }
    
    public enum FindingType
    {
        SpeedHack,
        FlyHack,
        NoFall,
        Teleport,
        InvalidMovement,
        HighCps,
        Reach,
        Killaura,
        InvalidSwing,
        PacketFlood,
        InvalidPacket,
        KeepAliveManipulation,
        TimerHack
    }
    
    public enum FindingLevel
    {
        Info,
        Suspicious,
        Likely,
        Definite
    }
}
