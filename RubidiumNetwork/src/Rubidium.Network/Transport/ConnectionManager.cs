using System.Collections.Concurrent;
using System.Net.Sockets;
using Microsoft.Extensions.Logging;
using Rubidium.Network.Core;
using Rubidium.Network.Memory;
using Rubidium.Network.Protocol;

namespace Rubidium.Network.Transport;

public sealed class ConnectionManager : IAsyncDisposable
{
    private readonly NetworkConfiguration _config;
    private readonly MemoryPoolManager _memoryPool;
    private readonly ILogger _logger;
    private readonly IPacketCodec _codec;
    
    private readonly ConcurrentDictionary<Guid, Connection> _connections;
    private readonly SemaphoreSlim _connectionLock;
    private readonly Timer _cleanupTimer;
    
    public int ActiveConnections => _connections.Count;
    
    public ConnectionManager(
        NetworkConfiguration config,
        MemoryPoolManager memoryPool,
        ILogger logger,
        IPacketCodec? codec = null)
    {
        _config = config;
        _memoryPool = memoryPool;
        _logger = logger;
        _codec = codec ?? new DefaultPacketCodec();
        
        _connections = new ConcurrentDictionary<Guid, Connection>();
        _connectionLock = new SemaphoreSlim(config.MaxConnections, config.MaxConnections);
        
        _cleanupTimer = new Timer(
            CleanupStaleConnections,
            null,
            TimeSpan.FromSeconds(30),
            TimeSpan.FromSeconds(30)
        );
    }
    
    public async Task<Connection> CreateConnectionAsync(Socket socket, CancellationToken ct)
    {
        await _connectionLock.WaitAsync(ct);
        
        try
        {
            var id = Guid.NewGuid();
            var connection = new Connection(
                id,
                socket,
                _memoryPool,
                _codec,
                _config.MaxPacketSize
            );
            
            if (!_connections.TryAdd(id, connection))
            {
                await connection.DisposeAsync();
                throw new InvalidOperationException("Failed to register connection");
            }
            
            _logger.LogDebug(
                "Connection {ConnectionId} established from {RemoteEndPoint}",
                id,
                connection.RemoteEndPoint
            );
            
            return connection;
        }
        catch
        {
            _connectionLock.Release();
            throw;
        }
    }
    
    public Connection? GetConnection(Guid id)
    {
        _connections.TryGetValue(id, out var connection);
        return connection;
    }
    
    public IEnumerable<Connection> GetAllConnections()
    {
        return _connections.Values;
    }
    
    public async Task RemoveConnectionAsync(Guid id)
    {
        if (_connections.TryRemove(id, out var connection))
        {
            await connection.DisposeAsync();
            _connectionLock.Release();
            
            _logger.LogDebug("Connection {ConnectionId} removed", id);
        }
    }
    
    public async Task DisconnectAsync(Guid id, DisconnectReason reason)
    {
        if (_connections.TryGetValue(id, out var connection))
        {
            await connection.DisconnectAsync(reason);
            await RemoveConnectionAsync(id);
        }
    }
    
    public async Task DisconnectAllAsync(DisconnectReason reason)
    {
        var tasks = _connections.Values
            .Select(c => c.DisconnectAsync(reason).AsTask());
        
        await Task.WhenAll(tasks);
        
        _connections.Clear();
    }
    
    private void CleanupStaleConnections(object? state)
    {
        var now = DateTime.UtcNow;
        var staleConnections = _connections.Values
            .Where(c => now - c.LastActivityAt > _config.ConnectionTimeout)
            .ToList();
        
        foreach (var connection in staleConnections)
        {
            _ = DisconnectAsync(connection.Id, DisconnectReason.Timeout);
            _logger.LogDebug("Connection {ConnectionId} timed out", connection.Id);
        }
    }
    
    public ConnectionManagerStats GetStats()
    {
        var connections = _connections.Values.ToList();
        
        return new ConnectionManagerStats
        {
            TotalConnections = connections.Count,
            TotalBytesReceived = connections.Sum(c => c.GetStats().BytesReceived),
            TotalBytesSent = connections.Sum(c => c.GetStats().BytesSent),
            AverageConnectionDuration = connections.Count > 0
                ? TimeSpan.FromTicks((long)connections.Average(c => c.UpTime.Ticks))
                : TimeSpan.Zero
        };
    }
    
    public async ValueTask DisposeAsync()
    {
        await _cleanupTimer.DisposeAsync();
        
        await DisconnectAllAsync(DisconnectReason.ServerShutdown);
        
        _connectionLock.Dispose();
    }
}

public record ConnectionManagerStats
{
    public int TotalConnections { get; init; }
    public long TotalBytesReceived { get; init; }
    public long TotalBytesSent { get; init; }
    public TimeSpan AverageConnectionDuration { get; init; }
}
