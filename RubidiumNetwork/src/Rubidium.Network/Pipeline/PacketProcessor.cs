using System.Collections.Concurrent;
using System.Runtime.CompilerServices;
using System.Threading.Channels;
using Microsoft.Extensions.Logging;
using Rubidium.Network.Core;
using Rubidium.Network.Memory;
using Rubidium.Network.Protocol;

namespace Rubidium.Network.Pipeline;

public sealed class PacketProcessor : IAsyncDisposable
{
    private readonly NetworkConfiguration _config;
    private readonly MemoryPoolManager _memoryPool;
    private readonly ILogger _logger;
    
    private readonly Channel<PacketTask> _processingChannel;
    private readonly ConcurrentDictionary<int, IPacketHandler> _handlers;
    private readonly Task[] _workerTasks;
    private readonly CancellationTokenSource _cts;
    
    private long _packetsProcessed;
    private long _packetsDropped;
    private long _processingErrors;
    
    public PacketProcessor(NetworkConfiguration config, MemoryPoolManager memoryPool, ILogger logger)
    {
        _config = config;
        _memoryPool = memoryPool;
        _logger = logger;
        _handlers = new ConcurrentDictionary<int, IPacketHandler>();
        _cts = new CancellationTokenSource();
        
        _processingChannel = Channel.CreateBounded<PacketTask>(new BoundedChannelOptions(10000)
        {
            FullMode = BoundedChannelFullMode.DropOldest,
            SingleReader = false,
            SingleWriter = false,
            AllowSynchronousContinuations = false
        });
        
        _workerTasks = new Task[config.WorkerThreads];
        for (int i = 0; i < _workerTasks.Length; i++)
        {
            _workerTasks[i] = ProcessWorkerAsync(_cts.Token);
        }
    }
    
    public void RegisterHandler<T>(int packetId, IPacketHandler<T> handler) where T : IPacket
    {
        _handlers[packetId] = new TypedPacketHandlerWrapper<T>(handler);
    }
    
    public void RegisterHandler(int packetId, IPacketHandler handler)
    {
        _handlers[packetId] = handler;
    }
    
    [MethodImpl(MethodImplOptions.AggressiveInlining)]
    public ValueTask EnqueueAsync(Guid connectionId, ReadOnlyMemory<byte> data, CancellationToken ct = default)
    {
        if (!_processingChannel.Writer.TryWrite(new PacketTask(connectionId, data)))
        {
            Interlocked.Increment(ref _packetsDropped);
            return ValueTask.CompletedTask;
        }
        
        return ValueTask.CompletedTask;
    }
    
    private async Task ProcessWorkerAsync(CancellationToken ct)
    {
        await foreach (var task in _processingChannel.Reader.ReadAllAsync(ct))
        {
            try
            {
                await ProcessPacketAsync(task, ct);
                Interlocked.Increment(ref _packetsProcessed);
            }
            catch (Exception ex)
            {
                Interlocked.Increment(ref _processingErrors);
                _logger.LogDebug(ex, "Error processing packet from {ConnectionId}", task.ConnectionId);
            }
        }
    }
    
    private async ValueTask ProcessPacketAsync(PacketTask task, CancellationToken ct)
    {
        if (task.Data.Length < 4)
        {
            return;
        }
        
        var packetId = BitConverter.ToInt32(task.Data.Span);
        
        if (!_handlers.TryGetValue(packetId, out var handler))
        {
            _logger.LogDebug("No handler for packet ID {PacketId}", packetId);
            return;
        }
        
        var packetData = task.Data.Slice(4);
        
        await handler.HandleAsync(task.ConnectionId, packetData, ct);
    }
    
    public PacketProcessorStats GetStats() => new()
    {
        PacketsProcessed = Interlocked.Read(ref _packetsProcessed),
        PacketsDropped = Interlocked.Read(ref _packetsDropped),
        ProcessingErrors = Interlocked.Read(ref _processingErrors),
        PendingPackets = _processingChannel.Reader.Count,
        RegisteredHandlers = _handlers.Count
    };
    
    public async ValueTask DisposeAsync()
    {
        _cts.Cancel();
        _processingChannel.Writer.Complete();
        
        await Task.WhenAll(_workerTasks);
        
        _cts.Dispose();
    }
    
    private readonly record struct PacketTask(Guid ConnectionId, ReadOnlyMemory<byte> Data);
    
    private sealed class TypedPacketHandlerWrapper<T>(IPacketHandler<T> inner) : IPacketHandler where T : IPacket
    {
        public async ValueTask HandleAsync(Guid connectionId, ReadOnlyMemory<byte> data, CancellationToken ct)
        {
            var packet = DeserializePacket(data);
            await inner.HandleAsync(connectionId, packet, ct);
        }
        
        private static T DeserializePacket(ReadOnlyMemory<byte> data)
        {
            // Protocol-specific deserialization
            throw new NotImplementedException("Implement protocol-specific deserialization");
        }
    }
}

public interface IPacketHandler
{
    ValueTask HandleAsync(Guid connectionId, ReadOnlyMemory<byte> data, CancellationToken ct);
}

public interface IPacketHandler<T> where T : IPacket
{
    ValueTask HandleAsync(Guid connectionId, T packet, CancellationToken ct);
}

public interface IPacket
{
    int PacketId { get; }
}

public record PacketProcessorStats
{
    public long PacketsProcessed { get; init; }
    public long PacketsDropped { get; init; }
    public long ProcessingErrors { get; init; }
    public int PendingPackets { get; init; }
    public int RegisteredHandlers { get; init; }
}
