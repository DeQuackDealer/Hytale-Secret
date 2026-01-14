using System.Net;
using System.Net.Sockets;
using System.Runtime.CompilerServices;
using Rubidium.Network.Core;
using Rubidium.Network.Memory;
using Rubidium.Network.Pipeline;
using Rubidium.Network.Protocol;

namespace Rubidium.Network.Transport;

public sealed class Connection : IAsyncDisposable
{
    private readonly Socket _socket;
    private readonly NetworkStream _stream;
    private readonly PacketPipeline _pipeline;
    private readonly MemoryPoolManager _memoryPool;
    private readonly SemaphoreSlim _sendLock;
    
    private readonly CancellationTokenSource _cts;
    private volatile ConnectionState _state;
    private DateTime _connectedAt;
    private DateTime _lastActivityAt;
    private long _bytesReceived;
    private long _bytesSent;
    private long _packetsReceived;
    private long _packetsSent;
    
    public Guid Id { get; }
    public IPEndPoint RemoteEndPoint { get; }
    public ConnectionState State => _state;
    public DateTime ConnectedAt => _connectedAt;
    public DateTime LastActivityAt => _lastActivityAt;
    public TimeSpan UpTime => DateTime.UtcNow - _connectedAt;
    
    public Connection(
        Guid id,
        Socket socket,
        MemoryPoolManager memoryPool,
        IPacketCodec codec,
        int maxPacketSize)
    {
        Id = id;
        _socket = socket;
        _memoryPool = memoryPool;
        _stream = new NetworkStream(socket, ownsSocket: false);
        _pipeline = new PacketPipeline(memoryPool, codec, maxPacketSize);
        _sendLock = new SemaphoreSlim(1, 1);
        _cts = new CancellationTokenSource();
        _state = ConnectionState.Connected;
        _connectedAt = DateTime.UtcNow;
        _lastActivityAt = DateTime.UtcNow;
        
        RemoteEndPoint = (IPEndPoint)socket.RemoteEndPoint!;
    }
    
    public async Task ProcessAsync(Action<ReadOnlyMemory<byte>> packetHandler, CancellationToken ct)
    {
        var linkedCts = CancellationTokenSource.CreateLinkedTokenSource(ct, _cts.Token);
        
        try
        {
            await _pipeline.StartAsync(
                _stream,
                _stream,
                async data =>
                {
                    Interlocked.Increment(ref _packetsReceived);
                    Interlocked.Add(ref _bytesReceived, data.Length);
                    _lastActivityAt = DateTime.UtcNow;
                    packetHandler(data);
                },
                linkedCts.Token
            );
        }
        finally
        {
            linkedCts.Dispose();
        }
    }
    
    [MethodImpl(MethodImplOptions.AggressiveInlining)]
    public async ValueTask SendAsync(ReadOnlyMemory<byte> data, CancellationToken ct = default)
    {
        if (_state != ConnectionState.Connected) return;
        
        await _sendLock.WaitAsync(ct);
        try
        {
            await _pipeline.SendPacketAsync(data, ct);
            
            Interlocked.Increment(ref _packetsSent);
            Interlocked.Add(ref _bytesSent, data.Length);
            _lastActivityAt = DateTime.UtcNow;
        }
        finally
        {
            _sendLock.Release();
        }
    }
    
    public async ValueTask DisconnectAsync(DisconnectReason reason = DisconnectReason.Normal)
    {
        if (_state == ConnectionState.Disconnected) return;
        
        _state = ConnectionState.Disconnecting;
        
        _cts.Cancel();
        
        try
        {
            _socket.Shutdown(SocketShutdown.Both);
        }
        catch { }
        
        _state = ConnectionState.Disconnected;
    }
    
    public ConnectionStats GetStats() => new()
    {
        Id = Id,
        RemoteEndPoint = RemoteEndPoint,
        State = _state,
        ConnectedAt = _connectedAt,
        LastActivityAt = _lastActivityAt,
        BytesReceived = Interlocked.Read(ref _bytesReceived),
        BytesSent = Interlocked.Read(ref _bytesSent),
        PacketsReceived = Interlocked.Read(ref _packetsReceived),
        PacketsSent = Interlocked.Read(ref _packetsSent)
    };
    
    public async ValueTask DisposeAsync()
    {
        await DisconnectAsync(DisconnectReason.Normal);
        await _pipeline.DisposeAsync();
        
        await _stream.DisposeAsync();
        _socket.Dispose();
        _sendLock.Dispose();
        _cts.Dispose();
    }
}

public enum ConnectionState
{
    Connecting,
    Connected,
    Disconnecting,
    Disconnected
}

public record ConnectionStats
{
    public Guid Id { get; init; }
    public IPEndPoint? RemoteEndPoint { get; init; }
    public ConnectionState State { get; init; }
    public DateTime ConnectedAt { get; init; }
    public DateTime LastActivityAt { get; init; }
    public long BytesReceived { get; init; }
    public long BytesSent { get; init; }
    public long PacketsReceived { get; init; }
    public long PacketsSent { get; init; }
}
