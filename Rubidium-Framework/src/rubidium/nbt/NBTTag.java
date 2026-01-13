package rubidium.nbt;

import java.io.*;
import java.util.*;

public sealed interface NBTTag permits 
    NBTTag.ByteTag, NBTTag.ShortTag, NBTTag.IntTag, NBTTag.LongTag,
    NBTTag.FloatTag, NBTTag.DoubleTag, NBTTag.ByteArrayTag,
    NBTTag.StringTag, NBTTag.ListTag, NBTTag.CompoundTag,
    NBTTag.IntArrayTag, NBTTag.LongArrayTag {
    
    byte getId();
    
    void write(DataOutput output) throws IOException;
    
    NBTTag copy();
    
    record ByteTag(byte value) implements NBTTag {
        public static final byte ID = 1;
        @Override public byte getId() { return ID; }
        @Override public void write(DataOutput output) throws IOException { output.writeByte(value); }
        @Override public NBTTag copy() { return new ByteTag(value); }
    }
    
    record ShortTag(short value) implements NBTTag {
        public static final byte ID = 2;
        @Override public byte getId() { return ID; }
        @Override public void write(DataOutput output) throws IOException { output.writeShort(value); }
        @Override public NBTTag copy() { return new ShortTag(value); }
    }
    
    record IntTag(int value) implements NBTTag {
        public static final byte ID = 3;
        @Override public byte getId() { return ID; }
        @Override public void write(DataOutput output) throws IOException { output.writeInt(value); }
        @Override public NBTTag copy() { return new IntTag(value); }
    }
    
    record LongTag(long value) implements NBTTag {
        public static final byte ID = 4;
        @Override public byte getId() { return ID; }
        @Override public void write(DataOutput output) throws IOException { output.writeLong(value); }
        @Override public NBTTag copy() { return new LongTag(value); }
    }
    
    record FloatTag(float value) implements NBTTag {
        public static final byte ID = 5;
        @Override public byte getId() { return ID; }
        @Override public void write(DataOutput output) throws IOException { output.writeFloat(value); }
        @Override public NBTTag copy() { return new FloatTag(value); }
    }
    
    record DoubleTag(double value) implements NBTTag {
        public static final byte ID = 6;
        @Override public byte getId() { return ID; }
        @Override public void write(DataOutput output) throws IOException { output.writeDouble(value); }
        @Override public NBTTag copy() { return new DoubleTag(value); }
    }
    
    record ByteArrayTag(byte[] value) implements NBTTag {
        public static final byte ID = 7;
        @Override public byte getId() { return ID; }
        @Override public void write(DataOutput output) throws IOException {
            output.writeInt(value.length);
            output.write(value);
        }
        @Override public NBTTag copy() { return new ByteArrayTag(Arrays.copyOf(value, value.length)); }
    }
    
    record StringTag(String value) implements NBTTag {
        public static final byte ID = 8;
        @Override public byte getId() { return ID; }
        @Override public void write(DataOutput output) throws IOException { output.writeUTF(value); }
        @Override public NBTTag copy() { return new StringTag(value); }
    }
    
    record ListTag(List<NBTTag> value, byte elementType) implements NBTTag {
        public static final byte ID = 9;
        @Override public byte getId() { return ID; }
        @Override public void write(DataOutput output) throws IOException {
            output.writeByte(elementType);
            output.writeInt(value.size());
            for (NBTTag tag : value) {
                tag.write(output);
            }
        }
        @Override public NBTTag copy() {
            List<NBTTag> copied = new ArrayList<>();
            for (NBTTag tag : value) {
                copied.add(tag.copy());
            }
            return new ListTag(copied, elementType);
        }
        
        public int size() { return value.size(); }
        public NBTTag get(int index) { return value.get(index); }
        public void add(NBTTag tag) { value.add(tag); }
    }
    
    record CompoundTag(Map<String, NBTTag> value) implements NBTTag {
        public static final byte ID = 10;
        
        public CompoundTag() {
            this(new LinkedHashMap<>());
        }
        
        @Override public byte getId() { return ID; }
        
        @Override public void write(DataOutput output) throws IOException {
            for (Map.Entry<String, NBTTag> entry : value.entrySet()) {
                NBTTag tag = entry.getValue();
                output.writeByte(tag.getId());
                output.writeUTF(entry.getKey());
                tag.write(output);
            }
            output.writeByte(0);
        }
        
        @Override public NBTTag copy() {
            Map<String, NBTTag> copied = new LinkedHashMap<>();
            for (Map.Entry<String, NBTTag> entry : value.entrySet()) {
                copied.put(entry.getKey(), entry.getValue().copy());
            }
            return new CompoundTag(copied);
        }
        
        public NBTTag get(String key) { return value.get(key); }
        public void put(String key, NBTTag tag) { value.put(key, tag); }
        public boolean contains(String key) { return value.containsKey(key); }
        public void remove(String key) { value.remove(key); }
        public Set<String> keys() { return value.keySet(); }
        public int size() { return value.size(); }
        public boolean isEmpty() { return value.isEmpty(); }
        
        public byte getByte(String key) {
            NBTTag tag = value.get(key);
            return tag instanceof ByteTag bt ? bt.value() : 0;
        }
        
        public short getShort(String key) {
            NBTTag tag = value.get(key);
            return tag instanceof ShortTag st ? st.value() : 0;
        }
        
        public int getInt(String key) {
            NBTTag tag = value.get(key);
            return tag instanceof IntTag it ? it.value() : 0;
        }
        
        public long getLong(String key) {
            NBTTag tag = value.get(key);
            return tag instanceof LongTag lt ? lt.value() : 0;
        }
        
        public float getFloat(String key) {
            NBTTag tag = value.get(key);
            return tag instanceof FloatTag ft ? ft.value() : 0;
        }
        
        public double getDouble(String key) {
            NBTTag tag = value.get(key);
            return tag instanceof DoubleTag dt ? dt.value() : 0;
        }
        
        public String getString(String key) {
            NBTTag tag = value.get(key);
            return tag instanceof StringTag st ? st.value() : "";
        }
        
        public CompoundTag getCompound(String key) {
            NBTTag tag = value.get(key);
            return tag instanceof CompoundTag ct ? ct : new CompoundTag();
        }
        
        public ListTag getList(String key) {
            NBTTag tag = value.get(key);
            return tag instanceof ListTag lt ? lt : new ListTag(new ArrayList<>(), (byte) 0);
        }
        
        public void putByte(String key, byte value) { this.value.put(key, new ByteTag(value)); }
        public void putShort(String key, short value) { this.value.put(key, new ShortTag(value)); }
        public void putInt(String key, int value) { this.value.put(key, new IntTag(value)); }
        public void putLong(String key, long value) { this.value.put(key, new LongTag(value)); }
        public void putFloat(String key, float value) { this.value.put(key, new FloatTag(value)); }
        public void putDouble(String key, double value) { this.value.put(key, new DoubleTag(value)); }
        public void putString(String key, String value) { this.value.put(key, new StringTag(value)); }
    }
    
    record IntArrayTag(int[] value) implements NBTTag {
        public static final byte ID = 11;
        @Override public byte getId() { return ID; }
        @Override public void write(DataOutput output) throws IOException {
            output.writeInt(value.length);
            for (int i : value) output.writeInt(i);
        }
        @Override public NBTTag copy() { return new IntArrayTag(Arrays.copyOf(value, value.length)); }
    }
    
    record LongArrayTag(long[] value) implements NBTTag {
        public static final byte ID = 12;
        @Override public byte getId() { return ID; }
        @Override public void write(DataOutput output) throws IOException {
            output.writeInt(value.length);
            for (long l : value) output.writeLong(l);
        }
        @Override public NBTTag copy() { return new LongArrayTag(Arrays.copyOf(value, value.length)); }
    }
}
