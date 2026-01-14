using System.Buffers;
using System.IO.Pipelines;
using System.Runtime.CompilerServices;
using Rubidium.Network.Memory;
using Rubidium.Network.Protocol;

namespace Rubidium.Network.Pipeline;

public sealed class PacketPipeline : IAsyncDisposable
{
    private readonly Pipe _receivePipe;
    private readonly Pipe _sendPipe;
    private readonly MemoryPoolManager _memoryPool;
    private readonly IPacketCodec _codec;
    private readonly int _maxPacketSize;
    
    private Task? _receiveTask;
    private Task? _sendTask;
    private CancellationTokenSource? _cts;
    
    public PacketPipeline(MemoryPoolManager memoryPool, IPacketCodec codec, int maxPacketSize = 1048576)
    {
        _memoryPool = memoryPool;
        _codec = codec;
        _maxPacketSize = maxPacketSize;
        
        var options = new PipeOptions(
            pool: MemoryPool<byte>.Shared,
            readerScheduler: PipeScheduler.ThreadPool,
            writerScheduler: PipeScheduler.ThreadPool,
            pauseWriterThreshold: maxPacketSize * 4,
            resumeWriterThreshold: maxPacketSize * 2,
            minimumSegmentSize: 4096,
            useSynchronizationContext: false
        );
        
        _receivePipe = new Pipe(options);
        _sendPipe = new Pipe(options);
    }
    
    public PipeWriter ReceiveWriter => _receivePipe.Writer;
    public PipeReader ReceiveReader => _receivePipe.Reader;
    public PipeWriter SendWriter => _sendPipe.Writer;
    public PipeReader SendReader => _sendPipe.Reader;
    
    public async Task StartAsync(
        Stream inputStream,
        Stream outputStream,
        Func<ReadOnlyMemory<byte>, ValueTask> packetHandler,
        CancellationToken ct)
    {
        _cts = CancellationTokenSource.CreateLinkedTokenSource(ct);
        
        var fillTask = FillReceivePipeAsync(inputStream, _cts.Token);
        var readTask = ReadPacketsAsync(packetHandler, _cts.Token);
        var writeTask = WriteSendPipeAsync(outputStream, _cts.Token);
        
        _receiveTask = Task.WhenAll(fillTask, readTask);
        _sendTask = writeTask;
        
        await Task.WhenAll(_receiveTask, _sendTask);
    }
    
    private async Task FillReceivePipeAsync(Stream stream, CancellationToken ct)
    {
        const int minimumBufferSize = 4096;
        
        try
        {
            while (!ct.IsCancellationRequested)
            {
                var memory = ReceiveWriter.GetMemory(minimumBufferSize);
                
                var bytesRead = await stream.ReadAsync(memory, ct);
                
                if (bytesRead == 0)
                {
                    break;
                }
                
                ReceiveWriter.Advance(bytesRead);
                
                var result = await ReceiveWriter.FlushAsync(ct);
                
                if (result.IsCompleted || result.IsCanceled)
                {
                    break;
                }
            }
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            // Log error
        }
        finally
        {
            await ReceiveWriter.CompleteAsync();
        }
    }
    
    private async Task ReadPacketsAsync(Func<ReadOnlyMemory<byte>, ValueTask> packetHandler, CancellationToken ct)
    {
        try
        {
            while (!ct.IsCancellationRequested)
            {
                var result = await ReceiveReader.ReadAsync(ct);
                var buffer = result.Buffer;
                
                while (TryReadPacket(ref buffer, out var packet))
                {
                    await packetHandler(packet.ToArray());
                }
                
                ReceiveReader.AdvanceTo(buffer.Start, buffer.End);
                
                if (result.IsCompleted || result.IsCanceled)
                {
                    break;
                }
            }
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            // Log error
        }
        finally
        {
            await ReceiveReader.CompleteAsync();
        }
    }
    
    [MethodImpl(MethodImplOptions.AggressiveInlining)]
    private bool TryReadPacket(ref ReadOnlySequence<byte> buffer, out ReadOnlySequence<byte> packet)
    {
        packet = default;
        
        if (buffer.Length < 4)
        {
            return false;
        }
        
        var lengthSpan = buffer.Slice(0, 4);
        Span<byte> lengthBytes = stackalloc byte[4];
        lengthSpan.CopyTo(lengthBytes);
        
        var packetLength = BitConverter.ToInt32(lengthBytes);
        
        if (packetLength <= 0 || packetLength > _maxPacketSize)
        {
            throw new InvalidDataException($"Invalid packet length: {packetLength}");
        }
        
        var totalLength = 4 + packetLength;
        
        if (buffer.Length < totalLength)
        {
            return false;
        }
        
        packet = buffer.Slice(4, packetLength);
        buffer = buffer.Slice(totalLength);
        
        return true;
    }
    
    public async ValueTask SendPacketAsync(ReadOnlyMemory<byte> data, CancellationToken ct = default)
    {
        var lengthBytes = BitConverter.GetBytes(data.Length);
        
        var memory = SendWriter.GetMemory(4 + data.Length);
        lengthBytes.CopyTo(memory);
        data.CopyTo(memory.Slice(4));
        SendWriter.Advance(4 + data.Length);
        
        await SendWriter.FlushAsync(ct);
    }
    
    private async Task WriteSendPipeAsync(Stream stream, CancellationToken ct)
    {
        try
        {
            while (!ct.IsCancellationRequested)
            {
                var result = await SendReader.ReadAsync(ct);
                var buffer = result.Buffer;
                
                foreach (var segment in buffer)
                {
                    await stream.WriteAsync(segment, ct);
                }
                
                SendReader.AdvanceTo(buffer.End);
                
                await stream.FlushAsync(ct);
                
                if (result.IsCompleted || result.IsCanceled)
                {
                    break;
                }
            }
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            // Log error
        }
        finally
        {
            await SendReader.CompleteAsync();
        }
    }
    
    public async ValueTask DisposeAsync()
    {
        _cts?.Cancel();
        
        await ReceiveWriter.CompleteAsync();
        await SendWriter.CompleteAsync();
        
        if (_receiveTask is not null)
        {
            try { await _receiveTask; } catch { }
        }
        
        if (_sendTask is not null)
        {
            try { await _sendTask; } catch { }
        }
        
        _cts?.Dispose();
    }
}
