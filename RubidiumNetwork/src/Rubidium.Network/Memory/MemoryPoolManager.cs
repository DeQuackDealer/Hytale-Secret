using System.Buffers;
using System.Runtime.CompilerServices;
using Microsoft.Extensions.ObjectPool;

namespace Rubidium.Network.Memory;

public sealed class MemoryPoolManager : IDisposable
{
    private readonly ArrayPool<byte> _arrayPool;
    private readonly ObjectPool<PooledBuffer> _bufferPool;
    private readonly int _maxBufferSize;
    
    private long _totalAllocations;
    private long _totalReturns;
    private long _currentRented;
    private long _peakRented;
    private long _totalBytesAllocated;
    
    public MemoryPoolManager(int maxBufferSize = 1048576)
    {
        _maxBufferSize = maxBufferSize;
        _arrayPool = ArrayPool<byte>.Create(maxBufferSize, 100);
        _bufferPool = new DefaultObjectPool<PooledBuffer>(
            new PooledBufferPolicy(this),
            Environment.ProcessorCount * 4
        );
    }
    
    [MethodImpl(MethodImplOptions.AggressiveInlining)]
    public Memory<byte> Rent(int minimumLength)
    {
        if (minimumLength > _maxBufferSize)
        {
            throw new ArgumentOutOfRangeException(nameof(minimumLength),
                $"Requested size {minimumLength} exceeds maximum {_maxBufferSize}");
        }
        
        var array = _arrayPool.Rent(minimumLength);
        
        Interlocked.Increment(ref _totalAllocations);
        Interlocked.Increment(ref _currentRented);
        Interlocked.Add(ref _totalBytesAllocated, array.Length);
        
        UpdatePeakRented();
        
        return new Memory<byte>(array, 0, minimumLength);
    }
    
    [MethodImpl(MethodImplOptions.AggressiveInlining)]
    public void Return(Memory<byte> memory)
    {
        if (memory.IsEmpty) return;
        
        if (System.Runtime.InteropServices.MemoryMarshal.TryGetArray<byte>(memory, out var segment) && segment.Array is not null)
        {
            _arrayPool.Return(segment.Array, clearArray: true);
            Interlocked.Increment(ref _totalReturns);
            Interlocked.Decrement(ref _currentRented);
        }
    }
    
    public PooledBuffer RentBuffer(int minimumLength)
    {
        var buffer = _bufferPool.Get();
        buffer.Initialize(minimumLength);
        return buffer;
    }
    
    public void ReturnBuffer(PooledBuffer buffer)
    {
        buffer.Reset();
        _bufferPool.Return(buffer);
    }
    
    private void UpdatePeakRented()
    {
        long current;
        long peak;
        do
        {
            current = Interlocked.Read(ref _currentRented);
            peak = Interlocked.Read(ref _peakRented);
            if (current <= peak) return;
        } while (Interlocked.CompareExchange(ref _peakRented, current, peak) != peak);
    }
    
    public MemoryPoolStats GetStats() => new()
    {
        TotalAllocations = Interlocked.Read(ref _totalAllocations),
        TotalReturns = Interlocked.Read(ref _totalReturns),
        CurrentRented = Interlocked.Read(ref _currentRented),
        PeakRented = Interlocked.Read(ref _peakRented),
        TotalBytesAllocated = Interlocked.Read(ref _totalBytesAllocated)
    };
    
    public void Dispose()
    {
        // ArrayPool manages its own cleanup
    }
    
    private sealed class PooledBufferPolicy(MemoryPoolManager pool) : IPooledObjectPolicy<PooledBuffer>
    {
        public PooledBuffer Create() => new(pool);
        
        public bool Return(PooledBuffer obj)
        {
            obj.Reset();
            return true;
        }
    }
}

public sealed class PooledBuffer : IDisposable
{
    private readonly MemoryPoolManager _pool;
    private Memory<byte> _memory;
    private int _length;
    private bool _disposed;
    
    public PooledBuffer(MemoryPoolManager pool)
    {
        _pool = pool;
    }
    
    public Memory<byte> Memory => _memory.Slice(0, _length);
    public Span<byte> Span => _memory.Span.Slice(0, _length);
    public int Length => _length;
    public int Capacity => _memory.Length;
    
    internal void Initialize(int minimumLength)
    {
        if (_memory.Length < minimumLength)
        {
            if (!_memory.IsEmpty)
            {
                _pool.Return(_memory);
            }
            _memory = _pool.Rent(minimumLength);
        }
        _length = 0;
        _disposed = false;
    }
    
    public void Advance(int count)
    {
        if (_length + count > _memory.Length)
        {
            throw new InvalidOperationException("Cannot advance beyond buffer capacity");
        }
        _length += count;
    }
    
    public void Reset()
    {
        _length = 0;
        if (!_memory.IsEmpty)
        {
            _memory.Span.Clear();
        }
    }
    
    public void Dispose()
    {
        if (_disposed) return;
        _disposed = true;
        
        if (!_memory.IsEmpty)
        {
            _pool.Return(_memory);
            _memory = Memory<byte>.Empty;
        }
    }
}

public record MemoryPoolStats
{
    public long TotalAllocations { get; init; }
    public long TotalReturns { get; init; }
    public long CurrentRented { get; init; }
    public long PeakRented { get; init; }
    public long TotalBytesAllocated { get; init; }
    
    public double ReuseRatio => TotalAllocations > 0 
        ? (double)TotalReturns / TotalAllocations 
        : 0;
}
