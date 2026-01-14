using System.IO.Pipes;
using System.Text.Json;
using System.Threading.Channels;
using Microsoft.Extensions.Logging;
using Rubidium.Network.Core;

namespace Rubidium.Network.Bridge;

public sealed class JavaBridge : IAsyncDisposable
{
    private readonly ILogger<JavaBridge> _logger;
    private readonly NetworkServer _networkServer;
    private readonly string _pipeName;
    
    private NamedPipeServerStream? _pipeServer;
    private StreamReader? _reader;
    private StreamWriter? _writer;
    private CancellationTokenSource? _cts;
    private Task? _readTask;
    private Task? _eventForwardTask;
    
    private readonly Channel<BridgeMessage> _outgoingChannel;
    private readonly Channel<BridgeMessage> _incomingChannel;
    
    public ChannelReader<BridgeMessage> IncomingMessages => _incomingChannel.Reader;
    
    public JavaBridge(NetworkServer networkServer, ILogger<JavaBridge> logger, string pipeName = "rubidium-network")
    {
        _networkServer = networkServer;
        _logger = logger;
        _pipeName = pipeName;
        
        _outgoingChannel = Channel.CreateUnbounded<BridgeMessage>();
        _incomingChannel = Channel.CreateUnbounded<BridgeMessage>();
    }
    
    public async Task StartAsync(CancellationToken ct = default)
    {
        _cts = CancellationTokenSource.CreateLinkedTokenSource(ct);
        
        _pipeServer = new NamedPipeServerStream(
            _pipeName,
            PipeDirection.InOut,
            1,
            PipeTransmissionMode.Byte,
            PipeOptions.Asynchronous
        );
        
        _logger.LogInformation("Waiting for Java process to connect on pipe: {PipeName}", _pipeName);
        
        await _pipeServer.WaitForConnectionAsync(_cts.Token);
        
        _logger.LogInformation("Java process connected");
        
        _reader = new StreamReader(_pipeServer);
        _writer = new StreamWriter(_pipeServer) { AutoFlush = true };
        
        _readTask = ReadMessagesAsync(_cts.Token);
        _eventForwardTask = ForwardNetworkEventsAsync(_cts.Token);
        
        _ = WriteMessagesAsync(_cts.Token);
    }
    
    private async Task ReadMessagesAsync(CancellationToken ct)
    {
        try
        {
            while (!ct.IsCancellationRequested && _reader is not null)
            {
                var line = await _reader.ReadLineAsync(ct);
                
                if (string.IsNullOrEmpty(line))
                {
                    if (!_pipeServer!.IsConnected)
                    {
                        _logger.LogWarning("Java process disconnected");
                        break;
                    }
                    continue;
                }
                
                try
                {
                    var message = JsonSerializer.Deserialize<BridgeMessage>(line);
                    if (message is not null)
                    {
                        await HandleIncomingMessageAsync(message, ct);
                    }
                }
                catch (JsonException ex)
                {
                    _logger.LogWarning(ex, "Invalid JSON from Java: {Line}", line);
                }
            }
        }
        catch (OperationCanceledException)
        {
            // Normal shutdown
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error reading from Java bridge");
        }
    }
    
    private async Task HandleIncomingMessageAsync(BridgeMessage message, CancellationToken ct)
    {
        switch (message.Type)
        {
            case MessageType.SendPacket:
                if (message.ConnectionId.HasValue && message.Data is not null)
                {
                    await _networkServer.SendAsync(
                        message.ConnectionId.Value,
                        Convert.FromBase64String(message.Data),
                        ct
                    );
                }
                break;
                
            case MessageType.Broadcast:
                if (message.Data is not null)
                {
                    await _networkServer.BroadcastAsync(
                        Convert.FromBase64String(message.Data),
                        ct
                    );
                }
                break;
                
            case MessageType.Disconnect:
                if (message.ConnectionId.HasValue)
                {
                    await _networkServer.DisconnectAsync(
                        message.ConnectionId.Value,
                        Enum.Parse<DisconnectReason>(message.Reason ?? "Normal")
                    );
                }
                break;
                
            case MessageType.GetMetrics:
                var metrics = _networkServer.GetMetrics();
                await SendMessageAsync(new BridgeMessage
                {
                    Type = MessageType.MetricsResponse,
                    Data = JsonSerializer.Serialize(metrics)
                }, ct);
                break;
                
            default:
                await _incomingChannel.Writer.WriteAsync(message, ct);
                break;
        }
    }
    
    private async Task ForwardNetworkEventsAsync(CancellationToken ct)
    {
        try
        {
            await foreach (var evt in _networkServer.Events.ReadAllAsync(ct))
            {
                var message = evt switch
                {
                    NetworkEvent.Connected connected => new BridgeMessage
                    {
                        Type = MessageType.PlayerConnected,
                        ConnectionId = connected.ConnectionId,
                        Address = connected.RemoteEndPoint.ToString()
                    },
                    
                    NetworkEvent.Disconnected disconnected => new BridgeMessage
                    {
                        Type = MessageType.PlayerDisconnected,
                        ConnectionId = disconnected.ConnectionId,
                        Reason = disconnected.Reason.ToString()
                    },
                    
                    NetworkEvent.PacketReceived packet => new BridgeMessage
                    {
                        Type = MessageType.PacketReceived,
                        ConnectionId = packet.ConnectionId,
                        Data = Convert.ToBase64String(packet.Data.ToArray())
                    },
                    
                    _ => null
                };
                
                if (message is not null)
                {
                    await SendMessageAsync(message, ct);
                }
            }
        }
        catch (OperationCanceledException)
        {
            // Normal shutdown
        }
    }
    
    private async Task WriteMessagesAsync(CancellationToken ct)
    {
        try
        {
            await foreach (var message in _outgoingChannel.Reader.ReadAllAsync(ct))
            {
                if (_writer is null) break;
                
                var json = JsonSerializer.Serialize(message);
                await _writer.WriteLineAsync(json);
            }
        }
        catch (OperationCanceledException)
        {
            // Normal shutdown
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error writing to Java bridge");
        }
    }
    
    public async ValueTask SendMessageAsync(BridgeMessage message, CancellationToken ct = default)
    {
        await _outgoingChannel.Writer.WriteAsync(message, ct);
    }
    
    public async ValueTask DisposeAsync()
    {
        _cts?.Cancel();
        _outgoingChannel.Writer.Complete();
        
        if (_readTask is not null)
        {
            try { await _readTask; } catch { }
        }
        
        if (_eventForwardTask is not null)
        {
            try { await _eventForwardTask; } catch { }
        }
        
        _reader?.Dispose();
        _writer?.Dispose();
        
        if (_pipeServer is not null)
        {
            await _pipeServer.DisposeAsync();
        }
        
        _cts?.Dispose();
        
        _logger.LogInformation("Java bridge disposed");
    }
}

public sealed class BridgeMessage
{
    public MessageType Type { get; set; }
    public Guid? ConnectionId { get; set; }
    public string? Data { get; set; }
    public string? Address { get; set; }
    public string? Reason { get; set; }
    public Dictionary<string, object>? Metadata { get; set; }
}

public enum MessageType
{
    PlayerConnected,
    PlayerDisconnected,
    PacketReceived,
    SendPacket,
    Broadcast,
    Disconnect,
    GetMetrics,
    MetricsResponse,
    Custom
}
