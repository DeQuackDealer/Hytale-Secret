package rubidium.nbt;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class NBTReader {
    
    public static NBTTag.CompoundTag read(Path path) throws IOException {
        return read(path, true);
    }
    
    public static NBTTag.CompoundTag read(Path path, boolean compressed) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            return read(is, compressed);
        }
    }
    
    public static NBTTag.CompoundTag read(InputStream input, boolean compressed) throws IOException {
        DataInputStream dis;
        if (compressed) {
            dis = new DataInputStream(new GZIPInputStream(input));
        } else {
            dis = new DataInputStream(input);
        }
        
        byte type = dis.readByte();
        if (type != NBTTag.CompoundTag.ID) {
            throw new IOException("Root tag must be compound, got type " + type);
        }
        
        dis.readUTF();
        return readCompound(dis);
    }
    
    public static NBTTag.CompoundTag read(byte[] data) throws IOException {
        return read(new ByteArrayInputStream(data), false);
    }
    
    private static NBTTag readTag(DataInput input, byte type) throws IOException {
        return switch (type) {
            case 1 -> new NBTTag.ByteTag(input.readByte());
            case 2 -> new NBTTag.ShortTag(input.readShort());
            case 3 -> new NBTTag.IntTag(input.readInt());
            case 4 -> new NBTTag.LongTag(input.readLong());
            case 5 -> new NBTTag.FloatTag(input.readFloat());
            case 6 -> new NBTTag.DoubleTag(input.readDouble());
            case 7 -> readByteArray(input);
            case 8 -> new NBTTag.StringTag(input.readUTF());
            case 9 -> readList(input);
            case 10 -> readCompound(input);
            case 11 -> readIntArray(input);
            case 12 -> readLongArray(input);
            default -> throw new IOException("Unknown tag type: " + type);
        };
    }
    
    private static NBTTag.CompoundTag readCompound(DataInput input) throws IOException {
        Map<String, NBTTag> map = new LinkedHashMap<>();
        
        while (true) {
            byte type = input.readByte();
            if (type == 0) break;
            
            String name = input.readUTF();
            NBTTag tag = readTag(input, type);
            map.put(name, tag);
        }
        
        return new NBTTag.CompoundTag(map);
    }
    
    private static NBTTag.ListTag readList(DataInput input) throws IOException {
        byte elementType = input.readByte();
        int size = input.readInt();
        
        List<NBTTag> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(readTag(input, elementType));
        }
        
        return new NBTTag.ListTag(list, elementType);
    }
    
    private static NBTTag.ByteArrayTag readByteArray(DataInput input) throws IOException {
        int size = input.readInt();
        byte[] data = new byte[size];
        input.readFully(data);
        return new NBTTag.ByteArrayTag(data);
    }
    
    private static NBTTag.IntArrayTag readIntArray(DataInput input) throws IOException {
        int size = input.readInt();
        int[] data = new int[size];
        for (int i = 0; i < size; i++) {
            data[i] = input.readInt();
        }
        return new NBTTag.IntArrayTag(data);
    }
    
    private static NBTTag.LongArrayTag readLongArray(DataInput input) throws IOException {
        int size = input.readInt();
        long[] data = new long[size];
        for (int i = 0; i < size; i++) {
            data[i] = input.readLong();
        }
        return new NBTTag.LongArrayTag(data);
    }
}
