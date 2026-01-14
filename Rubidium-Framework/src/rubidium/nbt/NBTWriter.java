package rubidium.nbt;

import java.io.*;
import java.nio.file.*;
import java.util.zip.GZIPOutputStream;

public class NBTWriter {
    
    public static void write(Path path, NBTTag.CompoundTag tag) throws IOException {
        write(path, tag, "", true);
    }
    
    public static void write(Path path, NBTTag.CompoundTag tag, String rootName, boolean compressed) throws IOException {
        try (OutputStream os = Files.newOutputStream(path)) {
            write(os, tag, rootName, compressed);
        }
    }
    
    public static void write(OutputStream output, NBTTag.CompoundTag tag, String rootName, boolean compressed) throws IOException {
        DataOutputStream dos;
        if (compressed) {
            dos = new DataOutputStream(new GZIPOutputStream(output));
        } else {
            dos = new DataOutputStream(output);
        }
        
        dos.writeByte(NBTTag.CompoundTag.ID);
        dos.writeUTF(rootName);
        tag.write(dos);
        
        dos.flush();
        if (compressed) {
            dos.close();
        }
    }
    
    public static byte[] write(NBTTag.CompoundTag tag) throws IOException {
        return write(tag, "");
    }
    
    public static byte[] write(NBTTag.CompoundTag tag, String rootName) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        write(baos, tag, rootName, false);
        return baos.toByteArray();
    }
}
