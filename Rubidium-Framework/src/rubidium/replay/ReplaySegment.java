package rubidium.replay;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class ReplaySegment {
    
    private static final int MAGIC = 0x52425831;
    private static final byte VERSION = 1;
    private static final int HEADER_SIZE = 64;
    private static final int MAX_SEGMENT_BYTES = 16 * 1024 * 1024;
    
    private UUID playerId;
    private long startTimestamp;
    private long endTimestamp;
    private int tickRate;
    private int captureRadius;
    private int frameCount;
    
    private ReplayFrame keyframe;
    private List<ReplayDelta> deltas;
    
    public ReplaySegment(UUID playerId, int tickRate, int captureRadius) {
        this.playerId = playerId;
        this.tickRate = tickRate;
        this.captureRadius = captureRadius;
        this.deltas = new ArrayList<>();
    }
    
    public ReplaySegment() {
        this.deltas = new ArrayList<>();
    }
    
    public void setKeyframe(ReplayFrame frame) {
        this.keyframe = frame.copy();
        this.startTimestamp = frame.getTimestamp();
        this.endTimestamp = frame.getTimestamp();
        this.frameCount = 1;
    }
    
    public void addFrame(ReplayFrame frame) {
        if (keyframe == null) {
            setKeyframe(frame);
            return;
        }
        
        ReplayFrame lastFrame = reconstructFrame(frameCount - 1);
        ReplayDelta delta = ReplayDelta.compute(lastFrame, frame);
        
        if (delta.hasChanges() || frame.getTimestamp() != lastFrame.getTimestamp()) {
            deltas.add(delta);
            endTimestamp = frame.getTimestamp();
            frameCount++;
        }
    }
    
    public ReplayFrame reconstructFrame(int index) {
        if (index < 0 || index >= frameCount) {
            throw new IndexOutOfBoundsException("Frame index: " + index);
        }
        
        if (index == 0) {
            return keyframe.copy();
        }
        
        ReplayFrame current = keyframe.copy();
        ReplayFrame next = new ReplayFrame();
        
        for (int i = 0; i < index && i < deltas.size(); i++) {
            deltas.get(i).apply(current, next);
            ReplayFrame temp = current;
            current = next;
            next = temp;
        }
        
        return current;
    }
    
    public byte[] serialize() {
        ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
        
        serializeFrameToStream(keyframe, dataStream);
        int actualFrameCount = 1;
        long actualEndTimestamp = keyframe.getTimestamp();
        
        for (ReplayDelta delta : deltas) {
            int sizeBefore = dataStream.size();
            serializeDeltaToStream(delta, dataStream);
            
            if (dataStream.size() > MAX_SEGMENT_BYTES) {
                byte[] truncated = new byte[sizeBefore];
                System.arraycopy(dataStream.toByteArray(), 0, truncated, 0, sizeBefore);
                dataStream.reset();
                dataStream.writeBytes(truncated);
                break;
            }
            
            actualFrameCount++;
        }
        
        if (actualFrameCount > 1 && !deltas.isEmpty()) {
            ReplayFrame lastFrame = reconstructFrame(actualFrameCount - 1);
            actualEndTimestamp = lastFrame.getTimestamp();
        }
        
        byte[] uncompressedData = dataStream.toByteArray();
        byte[] compressedData = compress(uncompressedData);
        
        ByteBuffer output = ByteBuffer.allocate(HEADER_SIZE + compressedData.length);
        output.order(ByteOrder.BIG_ENDIAN);
        
        output.putInt(MAGIC);
        output.put(VERSION);
        output.putLong(playerId.getMostSignificantBits());
        output.putLong(playerId.getLeastSignificantBits());
        output.putLong(startTimestamp);
        output.putLong(actualEndTimestamp);
        output.putInt(tickRate);
        output.putInt(captureRadius);
        output.putInt(actualFrameCount);
        output.putInt(uncompressedData.length);
        output.putInt(compressedData.length);
        
        int checksum = computeChecksum(compressedData);
        output.putInt(checksum);
        
        while (output.position() < HEADER_SIZE) {
            output.put((byte) 0);
        }
        
        output.put(compressedData);
        
        return output.array();
    }
    
    private void serializeFrameToStream(ReplayFrame frame, ByteArrayOutputStream stream) {
        ByteBuffer buffer = ByteBuffer.allocate(512);
        buffer.order(ByteOrder.BIG_ENDIAN);
        int written = frame.serialize(buffer);
        buffer.flip();
        byte[] data = new byte[written];
        buffer.get(data);
        stream.writeBytes(data);
    }
    
    private void serializeDeltaToStream(ReplayDelta delta, ByteArrayOutputStream stream) {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        buffer.order(ByteOrder.BIG_ENDIAN);
        int written = delta.serialize(buffer);
        buffer.flip();
        byte[] data = new byte[written];
        buffer.get(data);
        stream.writeBytes(data);
    }
    
    public static ReplaySegment deserialize(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.BIG_ENDIAN);
        
        int magic = buffer.getInt();
        if (magic != MAGIC) {
            throw new IllegalArgumentException("Invalid replay segment magic");
        }
        
        byte version = buffer.get();
        if (version != VERSION) {
            throw new IllegalArgumentException("Unsupported replay version: " + version);
        }
        
        ReplaySegment segment = new ReplaySegment();
        
        long msb = buffer.getLong();
        long lsb = buffer.getLong();
        segment.playerId = new UUID(msb, lsb);
        
        segment.startTimestamp = buffer.getLong();
        segment.endTimestamp = buffer.getLong();
        segment.tickRate = buffer.getInt();
        segment.captureRadius = buffer.getInt();
        segment.frameCount = buffer.getInt();
        int uncompressedSize = buffer.getInt();
        int compressedSize = buffer.getInt();
        int storedChecksum = buffer.getInt();
        
        buffer.position(HEADER_SIZE);
        
        byte[] compressedData = new byte[compressedSize];
        buffer.get(compressedData);
        
        int actualChecksum = computeChecksum(compressedData);
        if (actualChecksum != storedChecksum) {
            throw new IllegalArgumentException("Checksum mismatch");
        }
        
        byte[] uncompressedData = decompress(compressedData, uncompressedSize);
        ByteBuffer dataBuffer = ByteBuffer.wrap(uncompressedData);
        dataBuffer.order(ByteOrder.BIG_ENDIAN);
        
        segment.keyframe = new ReplayFrame();
        segment.keyframe.deserialize(dataBuffer);
        
        segment.deltas = new ArrayList<>();
        for (int i = 1; i < segment.frameCount; i++) {
            ReplayDelta delta = new ReplayDelta();
            delta.deserialize(dataBuffer);
            segment.deltas.add(delta);
        }
        
        return segment;
    }
    
    private static byte[] compress(byte[] data) {
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(data);
        deflater.finish();
        
        byte[] buffer = new byte[data.length];
        int compressedLength = deflater.deflate(buffer);
        deflater.end();
        
        byte[] result = new byte[compressedLength];
        System.arraycopy(buffer, 0, result, 0, compressedLength);
        return result;
    }
    
    private static byte[] decompress(byte[] data, int expectedSize) {
        try {
            Inflater inflater = new Inflater();
            inflater.setInput(data);
            
            byte[] result = new byte[expectedSize];
            int actualSize = inflater.inflate(result);
            inflater.end();
            
            if (actualSize != expectedSize) {
                throw new IllegalStateException("Decompression size mismatch");
            }
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to decompress replay data", e);
        }
    }
    
    private static int computeChecksum(byte[] data) {
        int checksum = 0;
        for (byte b : data) {
            checksum = ((checksum << 5) + checksum) + (b & 0xFF);
        }
        return checksum;
    }
    
    public UUID getPlayerId() { return playerId; }
    public long getStartTimestamp() { return startTimestamp; }
    public long getEndTimestamp() { return endTimestamp; }
    public int getTickRate() { return tickRate; }
    public int getCaptureRadius() { return captureRadius; }
    public int getFrameCount() { return frameCount; }
    public long getDurationMs() { return endTimestamp - startTimestamp; }
}
