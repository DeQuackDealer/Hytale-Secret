namespace Rubidium.Network.Core;

public sealed record NetworkConfiguration
{
    public string BindAddress { get; init; } = "0.0.0.0";
    public int TcpPort { get; init; } = 25565;
    public int UdpPort { get; init; } = 25566;
    public int GrpcPort { get; init; } = 50051;
    
    public int MaxConnections { get; init; } = 10000;
    public int MaxPendingConnections { get; init; } = 1000;
    
    public int ReceiveBufferSize { get; init; } = 65536;
    public int SendBufferSize { get; init; } = 65536;
    public int MaxPacketSize { get; init; } = 1048576;
    
    public TimeSpan ConnectionTimeout { get; init; } = TimeSpan.FromSeconds(30);
    public TimeSpan ReadTimeout { get; init; } = TimeSpan.FromSeconds(60);
    public TimeSpan WriteTimeout { get; init; } = TimeSpan.FromSeconds(30);
    public TimeSpan KeepAliveInterval { get; init; } = TimeSpan.FromSeconds(15);
    
    public int WorkerThreads { get; init; } = Environment.ProcessorCount * 2;
    public int IoCompletionThreads { get; init; } = Environment.ProcessorCount * 2;
    
    public bool EnableNagle { get; init; } = false;
    public bool EnableKeepAlive { get; init; } = true;
    public bool EnableDualMode { get; init; } = true;
    
    public CompressionLevel CompressionLevel { get; init; } = CompressionLevel.Fast;
    public int CompressionThreshold { get; init; } = 256;
    
    public bool EnableEncryption { get; init; } = true;
    public string? CertificatePath { get; init; }
    
    public bool EnableMetrics { get; init; } = true;
    public TimeSpan MetricsInterval { get; init; } = TimeSpan.FromSeconds(10);
    
    public static NetworkConfiguration Default => new();
    
    public static NetworkConfiguration HighPerformance => new()
    {
        MaxConnections = 100000,
        MaxPendingConnections = 5000,
        ReceiveBufferSize = 131072,
        SendBufferSize = 131072,
        WorkerThreads = Environment.ProcessorCount * 4,
        IoCompletionThreads = Environment.ProcessorCount * 4,
        CompressionLevel = CompressionLevel.Optimal
    };
}

public enum CompressionLevel
{
    None,
    Fast,
    Optimal,
    Maximum
}
