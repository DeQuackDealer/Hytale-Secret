using System.Net;
using System.Net.Sockets;
using System.Threading.Channels;
using Microsoft.Extensions.Logging;
using Rubidium.Network.Pipeline;
using Rubidium.Network.Memory;
using Rubidium.Network.Transport;

namespace Rubidium.Network.Core;

public sealed class NetworkServer : IAsyncDisposable
{
    private readonly NetworkConfiguration _config;
    private readonly ILogger<NetworkServer> _logger;
    private readonly ConnectionManager _connectionManager;
    private readonly PacketProcessor _packetProcessor;
    private readonly MemoryPoolManager _memoryPool;
    
    private Socket? _tcpListener;
    private Socket? _udpSocket;
    private CancellationTokenSource? _cts;
    private Task? _acceptTask;
    private Task? _udpTask;
    
    private readonly Channel<NetworkEvent> _eventChannel;
    private long _totalBytesReceived;
    private long _totalBytesSent;
    private long _totalPacketsReceived;
    private long _totalPacketsSent;
    private long _totalConnections;
    private long _activeConnections;
    
    public NetworkServer(NetworkConfiguration config, ILogger<NetworkServer> logger)
    {
        _config = config;
        _logger = logger;
        _memoryPool = new MemoryPoolManager(config.MaxPacketSize);
        _connectionManager = new ConnectionManager(config, _memoryPool, logger);
        _packetProcessor = new PacketProcessor(config, _memoryPool, logger);
        _eventChannel = Channel.CreateUnbounded<NetworkEvent>(new UnboundedChannelOptions
        {
            SingleReader = false,
            SingleWriter = false,
            AllowSynchronousContinuations = false
        });
    }
    
    public ChannelReader<NetworkEvent> Events => _eventChannel.Reader;
    
    public NetworkMetrics GetMetrics() => new()
    {
        TotalBytesReceived = Interlocked.Read(ref _totalBytesReceived),
        TotalBytesSent = Interlocked.Read(ref _totalBytesSent),
        TotalPacketsReceived = Interlocked.Read(ref _totalPacketsReceived),
        TotalPacketsSent = Interlocked.Read(ref _totalPacketsSent),
        TotalConnections = Interlocked.Read(ref _totalConnections),
        ActiveConnections = Interlocked.Read(ref _activeConnections),
        MemoryPoolStats = _memoryPool.GetStats()
    };
    
    public async Task StartAsync(CancellationToken cancellationToken = default)
    {
        _cts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        
        ConfigureThreadPool();
        
        await StartTcpListenerAsync();
        await StartUdpSocketAsync();
        
        _acceptTask = AcceptConnectionsAsync(_cts.Token);
        _udpTask = ProcessUdpAsync(_cts.Token);
        
        _logger.LogInformation(
            "Network server started on TCP:{TcpPort} UDP:{UdpPort}",
            _config.TcpPort,
            _config.UdpPort
        );
    }
    
    private void ConfigureThreadPool()
    {
        ThreadPool.SetMinThreads(_config.WorkerThreads, _config.IoCompletionThreads);
    }
    
    private Task StartTcpListenerAsync()
    {
        var endpoint = new IPEndPoint(IPAddress.Parse(_config.BindAddress), _config.TcpPort);
        
        _tcpListener = new Socket(
            _config.EnableDualMode ? AddressFamily.InterNetworkV6 : AddressFamily.InterNetwork,
            SocketType.Stream,
            ProtocolType.Tcp
        );
        
        if (_config.EnableDualMode)
        {
            _tcpListener.DualMode = true;
        }
        
        _tcpListener.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.ReuseAddress, true);
        _tcpListener.SetSocketOption(SocketOptionLevel.Tcp, SocketOptionName.NoDelay, !_config.EnableNagle);
        _tcpListener.ReceiveBufferSize = _config.ReceiveBufferSize;
        _tcpListener.SendBufferSize = _config.SendBufferSize;
        
        if (_config.EnableKeepAlive)
        {
            _tcpListener.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.KeepAlive, true);
        }
        
        _tcpListener.Bind(endpoint);
        _tcpListener.Listen(_config.MaxPendingConnections);
        
        return Task.CompletedTask;
    }
    
    private Task StartUdpSocketAsync()
    {
        var endpoint = new IPEndPoint(IPAddress.Parse(_config.BindAddress), _config.UdpPort);
        
        _udpSocket = new Socket(
            _config.EnableDualMode ? AddressFamily.InterNetworkV6 : AddressFamily.InterNetwork,
            SocketType.Dgram,
            ProtocolType.Udp
        );
        
        if (_config.EnableDualMode)
        {
            _udpSocket.DualMode = true;
        }
        
        _udpSocket.ReceiveBufferSize = _config.ReceiveBufferSize;
        _udpSocket.SendBufferSize = _config.SendBufferSize;
        _udpSocket.Bind(endpoint);
        
        return Task.CompletedTask;
    }
    
    private async Task AcceptConnectionsAsync(CancellationToken ct)
    {
        while (!ct.IsCancellationRequested)
        {
            try
            {
                var clientSocket = await _tcpListener!.AcceptAsync(ct);
                
                if (Interlocked.Read(ref _activeConnections) >= _config.MaxConnections)
                {
                    _logger.LogWarning("Max connections reached, rejecting new connection");
                    clientSocket.Close();
                    continue;
                }
                
                Interlocked.Increment(ref _totalConnections);
                Interlocked.Increment(ref _activeConnections);
                
                _ = HandleConnectionAsync(clientSocket, ct);
            }
            catch (OperationCanceledException)
            {
                break;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error accepting connection");
            }
        }
    }
    
    private async Task HandleConnectionAsync(Socket socket, CancellationToken ct)
    {
        var connection = await _connectionManager.CreateConnectionAsync(socket, ct);
        
        try
        {
            await _eventChannel.Writer.WriteAsync(
                new NetworkEvent.Connected(connection.Id, connection.RemoteEndPoint),
                ct
            );
            
            await connection.ProcessAsync(
                packet => OnPacketReceived(connection.Id, packet),
                ct
            );
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            _logger.LogDebug(ex, "Connection {ConnectionId} error", connection.Id);
        }
        finally
        {
            await _connectionManager.RemoveConnectionAsync(connection.Id);
            Interlocked.Decrement(ref _activeConnections);
            
            await _eventChannel.Writer.WriteAsync(
                new NetworkEvent.Disconnected(connection.Id, DisconnectReason.Normal),
                ct
            );
        }
    }
    
    private void OnPacketReceived(Guid connectionId, ReadOnlyMemory<byte> packet)
    {
        Interlocked.Increment(ref _totalPacketsReceived);
        Interlocked.Add(ref _totalBytesReceived, packet.Length);
        
        _eventChannel.Writer.TryWrite(new NetworkEvent.PacketReceived(connectionId, packet));
    }
    
    private async Task ProcessUdpAsync(CancellationToken ct)
    {
        var buffer = _memoryPool.Rent(_config.ReceiveBufferSize);
        var remoteEndPoint = new IPEndPoint(IPAddress.Any, 0) as EndPoint;
        
        try
        {
            while (!ct.IsCancellationRequested)
            {
                try
                {
                    var result = await _udpSocket!.ReceiveFromAsync(buffer, SocketFlags.None, remoteEndPoint, ct);
                    
                    Interlocked.Increment(ref _totalPacketsReceived);
                    Interlocked.Add(ref _totalBytesReceived, result.ReceivedBytes);
                    
                    var packetData = buffer.Slice(0, result.ReceivedBytes).ToArray();
                    await _eventChannel.Writer.WriteAsync(
                        new NetworkEvent.UdpPacketReceived((IPEndPoint)result.RemoteEndPoint, packetData),
                        ct
                    );
                }
                catch (OperationCanceledException)
                {
                    break;
                }
                catch (Exception ex)
                {
                    _logger.LogDebug(ex, "UDP receive error");
                }
            }
        }
        finally
        {
            _memoryPool.Return(buffer);
        }
    }
    
    public async ValueTask SendAsync(Guid connectionId, ReadOnlyMemory<byte> data, CancellationToken ct = default)
    {
        var connection = _connectionManager.GetConnection(connectionId);
        if (connection is null) return;
        
        await connection.SendAsync(data, ct);
        
        Interlocked.Increment(ref _totalPacketsSent);
        Interlocked.Add(ref _totalBytesSent, data.Length);
    }
    
    public async ValueTask SendUdpAsync(IPEndPoint endpoint, ReadOnlyMemory<byte> data, CancellationToken ct = default)
    {
        if (_udpSocket is null) return;
        
        await _udpSocket.SendToAsync(data, SocketFlags.None, endpoint, ct);
        
        Interlocked.Increment(ref _totalPacketsSent);
        Interlocked.Add(ref _totalBytesSent, data.Length);
    }
    
    public async ValueTask BroadcastAsync(ReadOnlyMemory<byte> data, CancellationToken ct = default)
    {
        var tasks = _connectionManager.GetAllConnections()
            .Select(c => SendAsync(c.Id, data, ct).AsTask());
        
        await Task.WhenAll(tasks);
    }
    
    public async ValueTask DisconnectAsync(Guid connectionId, DisconnectReason reason = DisconnectReason.Normal)
    {
        await _connectionManager.DisconnectAsync(connectionId, reason);
        Interlocked.Decrement(ref _activeConnections);
    }
    
    public async ValueTask DisposeAsync()
    {
        _cts?.Cancel();
        
        if (_acceptTask is not null)
        {
            try { await _acceptTask; } catch { }
        }
        
        if (_udpTask is not null)
        {
            try { await _udpTask; } catch { }
        }
        
        await _connectionManager.DisposeAsync();
        
        _tcpListener?.Close();
        _udpSocket?.Close();
        _cts?.Dispose();
        
        _eventChannel.Writer.Complete();
        
        _logger.LogInformation("Network server stopped");
    }
}

public abstract record NetworkEvent
{
    public sealed record Connected(Guid ConnectionId, IPEndPoint RemoteEndPoint) : NetworkEvent;
    public sealed record Disconnected(Guid ConnectionId, DisconnectReason Reason) : NetworkEvent;
    public sealed record PacketReceived(Guid ConnectionId, ReadOnlyMemory<byte> Data) : NetworkEvent;
    public sealed record UdpPacketReceived(IPEndPoint RemoteEndPoint, ReadOnlyMemory<byte> Data) : NetworkEvent;
}

public enum DisconnectReason
{
    Normal,
    Timeout,
    Kicked,
    Banned,
    ServerShutdown,
    ProtocolError,
    InvalidPacket
}

public record NetworkMetrics
{
    public long TotalBytesReceived { get; init; }
    public long TotalBytesSent { get; init; }
    public long TotalPacketsReceived { get; init; }
    public long TotalPacketsSent { get; init; }
    public long TotalConnections { get; init; }
    public long ActiveConnections { get; init; }
    public MemoryPoolStats MemoryPoolStats { get; init; } = new();
}
