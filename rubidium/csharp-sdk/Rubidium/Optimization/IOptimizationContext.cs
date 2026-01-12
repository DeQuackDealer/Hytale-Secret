namespace Rubidium.Optimization;

public interface IOptimizationContext
{
    IServerPerformance Performance { get; }
    ITickOptimizer TickOptimizer { get; }
    IMemoryManager MemoryManager { get; }
    IEntityBudget EntityBudget { get; }
    IChunkOptimizer ChunkOptimizer { get; }
    INetworkOptimizer NetworkOptimizer { get; }
}

public interface IServerPerformance
{
    double TPS { get; }
    double AverageTPS { get; }
    long TickTimeMs { get; }
    long AverageTickTimeMs { get; }
    double CPUUsage { get; }
    long MemoryUsed { get; }
    long MemoryMax { get; }
    int LoadedChunks { get; }
    int EntityCount { get; }
    int PlayerCount { get; }
}

public interface ITickOptimizer
{
    void RequestExtraTick();
    void RequestReducedLoad(int durationTicks);
    void SetTickPriority(string task, TickPriority priority);
    bool IsOptimizationActive { get; }
}

public interface IMemoryManager
{
    void RequestGC();
    void TrimWorkingSets();
    long AvailableMemory { get; }
    event Action? OnMemoryPressure;
}

public interface IEntityBudget
{
    int EntityLimit { get; set; }
    int CurrentEntityCount { get; }
    bool CanSpawnEntity { get; }
    void RegisterEntity(string type);
    void UnregisterEntity(string type);
}

public interface IChunkOptimizer
{
    Task PreloadChunksAsync(string world, int centerX, int centerZ, int radius);
    void UnloadDistantChunks(string world);
    int ChunkTickDistance { get; set; }
}

public interface INetworkOptimizer
{
    bool PacketBatchingEnabled { get; set; }
    int CompressionLevel { get; set; }
    void PrioritizePlayer(Guid playerId);
    INetworkStats Stats { get; }
}

public interface INetworkStats
{
    long BytesSent { get; }
    long BytesReceived { get; }
    int PacketsSent { get; }
    int PacketsReceived { get; }
    double AverageLatency { get; }
}

public enum TickPriority
{
    Critical,
    High,
    Normal,
    Low,
    Background
}
