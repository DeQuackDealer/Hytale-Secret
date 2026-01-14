using System.Buffers;
using System.IO.Compression;
using System.Runtime.CompilerServices;
using Rubidium.Network.Memory;

namespace Rubidium.Network.Protocol;

public interface IPacketCodec
{
    ReadOnlyMemory<byte> Encode(ReadOnlySpan<byte> data);
    ReadOnlyMemory<byte> Decode(ReadOnlySpan<byte> data);
    
    bool TryEncode(ReadOnlySpan<byte> data, Span<byte> destination, out int bytesWritten);
    bool TryDecode(ReadOnlySpan<byte> data, Span<byte> destination, out int bytesWritten);
}

public sealed class DefaultPacketCodec : IPacketCodec
{
    [MethodImpl(MethodImplOptions.AggressiveInlining)]
    public ReadOnlyMemory<byte> Encode(ReadOnlySpan<byte> data)
    {
        return data.ToArray();
    }
    
    [MethodImpl(MethodImplOptions.AggressiveInlining)]
    public ReadOnlyMemory<byte> Decode(ReadOnlySpan<byte> data)
    {
        return data.ToArray();
    }
    
    [MethodImpl(MethodImplOptions.AggressiveInlining)]
    public bool TryEncode(ReadOnlySpan<byte> data, Span<byte> destination, out int bytesWritten)
    {
        if (destination.Length < data.Length)
        {
            bytesWritten = 0;
            return false;
        }
        
        data.CopyTo(destination);
        bytesWritten = data.Length;
        return true;
    }
    
    [MethodImpl(MethodImplOptions.AggressiveInlining)]
    public bool TryDecode(ReadOnlySpan<byte> data, Span<byte> destination, out int bytesWritten)
    {
        if (destination.Length < data.Length)
        {
            bytesWritten = 0;
            return false;
        }
        
        data.CopyTo(destination);
        bytesWritten = data.Length;
        return true;
    }
}

public sealed class CompressedPacketCodec : IPacketCodec
{
    private readonly int _compressionThreshold;
    private readonly CompressionLevel _compressionLevel;
    private readonly ArrayPool<byte> _arrayPool;
    
    private const byte FLAG_UNCOMPRESSED = 0x00;
    private const byte FLAG_COMPRESSED = 0x01;
    
    public CompressedPacketCodec(
        int compressionThreshold = 256,
        CompressionLevel compressionLevel = CompressionLevel.Fastest)
    {
        _compressionThreshold = compressionThreshold;
        _compressionLevel = compressionLevel;
        _arrayPool = ArrayPool<byte>.Shared;
    }
    
    public ReadOnlyMemory<byte> Encode(ReadOnlySpan<byte> data)
    {
        if (data.Length < _compressionThreshold)
        {
            var result = new byte[data.Length + 1];
            result[0] = FLAG_UNCOMPRESSED;
            data.CopyTo(result.AsSpan(1));
            return result;
        }
        
        using var outputStream = new MemoryStream();
        outputStream.WriteByte(FLAG_COMPRESSED);
        
        using (var compressionStream = new DeflateStream(outputStream, _compressionLevel, leaveOpen: true))
        {
            compressionStream.Write(data);
        }
        
        return outputStream.ToArray();
    }
    
    public ReadOnlyMemory<byte> Decode(ReadOnlySpan<byte> data)
    {
        if (data.Length == 0) return Array.Empty<byte>();
        
        var flag = data[0];
        var payload = data.Slice(1);
        
        if (flag == FLAG_UNCOMPRESSED)
        {
            return payload.ToArray();
        }
        
        using var inputStream = new MemoryStream(payload.ToArray());
        using var decompressionStream = new DeflateStream(inputStream, CompressionMode.Decompress);
        using var outputStream = new MemoryStream();
        
        decompressionStream.CopyTo(outputStream);
        
        return outputStream.ToArray();
    }
    
    public bool TryEncode(ReadOnlySpan<byte> data, Span<byte> destination, out int bytesWritten)
    {
        var encoded = Encode(data);
        
        if (destination.Length < encoded.Length)
        {
            bytesWritten = 0;
            return false;
        }
        
        encoded.Span.CopyTo(destination);
        bytesWritten = encoded.Length;
        return true;
    }
    
    public bool TryDecode(ReadOnlySpan<byte> data, Span<byte> destination, out int bytesWritten)
    {
        var decoded = Decode(data);
        
        if (destination.Length < decoded.Length)
        {
            bytesWritten = 0;
            return false;
        }
        
        decoded.Span.CopyTo(destination);
        bytesWritten = decoded.Length;
        return true;
    }
}

public sealed class EncryptedPacketCodec : IPacketCodec
{
    private readonly byte[] _key;
    private readonly IPacketCodec _innerCodec;
    
    public EncryptedPacketCodec(byte[] key, IPacketCodec? innerCodec = null)
    {
        if (key.Length != 32)
        {
            throw new ArgumentException("Key must be 32 bytes for AES-256", nameof(key));
        }
        
        _key = key;
        _innerCodec = innerCodec ?? new DefaultPacketCodec();
    }
    
    public ReadOnlyMemory<byte> Encode(ReadOnlySpan<byte> data)
    {
        var innerEncoded = _innerCodec.Encode(data);
        
        using var aes = System.Security.Cryptography.Aes.Create();
        aes.Key = _key;
        aes.GenerateIV();
        
        using var encryptor = aes.CreateEncryptor();
        using var outputStream = new MemoryStream();
        
        outputStream.Write(aes.IV);
        
        using (var cryptoStream = new System.Security.Cryptography.CryptoStream(
            outputStream, encryptor, System.Security.Cryptography.CryptoStreamMode.Write, leaveOpen: true))
        {
            cryptoStream.Write(innerEncoded.Span);
        }
        
        return outputStream.ToArray();
    }
    
    public ReadOnlyMemory<byte> Decode(ReadOnlySpan<byte> data)
    {
        if (data.Length < 16)
        {
            throw new ArgumentException("Data too short for encrypted packet", nameof(data));
        }
        
        var iv = data.Slice(0, 16).ToArray();
        var ciphertext = data.Slice(16);
        
        using var aes = System.Security.Cryptography.Aes.Create();
        aes.Key = _key;
        aes.IV = iv;
        
        using var decryptor = aes.CreateDecryptor();
        using var inputStream = new MemoryStream(ciphertext.ToArray());
        using var cryptoStream = new System.Security.Cryptography.CryptoStream(
            inputStream, decryptor, System.Security.Cryptography.CryptoStreamMode.Read);
        using var outputStream = new MemoryStream();
        
        cryptoStream.CopyTo(outputStream);
        
        return _innerCodec.Decode(outputStream.ToArray());
    }
    
    public bool TryEncode(ReadOnlySpan<byte> data, Span<byte> destination, out int bytesWritten)
    {
        try
        {
            var encoded = Encode(data);
            if (destination.Length < encoded.Length)
            {
                bytesWritten = 0;
                return false;
            }
            
            encoded.Span.CopyTo(destination);
            bytesWritten = encoded.Length;
            return true;
        }
        catch
        {
            bytesWritten = 0;
            return false;
        }
    }
    
    public bool TryDecode(ReadOnlySpan<byte> data, Span<byte> destination, out int bytesWritten)
    {
        try
        {
            var decoded = Decode(data);
            if (destination.Length < decoded.Length)
            {
                bytesWritten = 0;
                return false;
            }
            
            decoded.Span.CopyTo(destination);
            bytesWritten = decoded.Length;
            return true;
        }
        catch
        {
            bytesWritten = 0;
            return false;
        }
    }
}
