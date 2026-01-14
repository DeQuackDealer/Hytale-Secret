package rubidium.voice.protocol;

import java.nio.ByteBuffer;
import java.util.UUID;

public abstract class VoicePacket {
    
    public abstract byte getPacketId();
    public abstract void write(ByteBuffer buffer);
    public abstract void read(ByteBuffer buffer);
    
    public static class AudioData extends VoicePacket {
        public static final byte ID = 0x01;
        
        public UUID speakerId;
        public long sequenceNumber;
        public byte[] opusData;
        public boolean whisper;
        public float volume;
        
        @Override
        public byte getPacketId() { return ID; }
        
        @Override
        public void write(ByteBuffer buffer) {
            buffer.putLong(speakerId.getMostSignificantBits());
            buffer.putLong(speakerId.getLeastSignificantBits());
            buffer.putLong(sequenceNumber);
            buffer.put(whisper ? (byte) 1 : (byte) 0);
            buffer.putFloat(volume);
            buffer.putShort((short) opusData.length);
            buffer.put(opusData);
        }
        
        @Override
        public void read(ByteBuffer buffer) {
            speakerId = new UUID(buffer.getLong(), buffer.getLong());
            sequenceNumber = buffer.getLong();
            whisper = buffer.get() == 1;
            volume = buffer.getFloat();
            int length = buffer.getShort() & 0xFFFF;
            opusData = new byte[length];
            buffer.get(opusData);
        }
    }
    
    public static class StateUpdate extends VoicePacket {
        public static final byte ID = 0x02;
        
        public UUID playerId;
        public boolean speaking;
        public boolean muted;
        public boolean deafened;
        public boolean whispering;
        public byte activationMode;
        
        @Override
        public byte getPacketId() { return ID; }
        
        @Override
        public void write(ByteBuffer buffer) {
            buffer.putLong(playerId.getMostSignificantBits());
            buffer.putLong(playerId.getLeastSignificantBits());
            byte flags = 0;
            if (speaking) flags |= 0x01;
            if (muted) flags |= 0x02;
            if (deafened) flags |= 0x04;
            if (whispering) flags |= 0x08;
            buffer.put(flags);
            buffer.put(activationMode);
        }
        
        @Override
        public void read(ByteBuffer buffer) {
            playerId = new UUID(buffer.getLong(), buffer.getLong());
            byte flags = buffer.get();
            speaking = (flags & 0x01) != 0;
            muted = (flags & 0x02) != 0;
            deafened = (flags & 0x04) != 0;
            whispering = (flags & 0x08) != 0;
            activationMode = buffer.get();
        }
    }
    
    public static class GroupUpdate extends VoicePacket {
        public static final byte ID = 0x03;
        
        public String groupId;
        public String groupName;
        public byte action;
        public UUID playerId;
        
        public static final byte ACTION_JOIN = 0;
        public static final byte ACTION_LEAVE = 1;
        public static final byte ACTION_CREATE = 2;
        public static final byte ACTION_DELETE = 3;
        
        @Override
        public byte getPacketId() { return ID; }
        
        @Override
        public void write(ByteBuffer buffer) {
            byte[] idBytes = groupId.getBytes();
            buffer.putShort((short) idBytes.length);
            buffer.put(idBytes);
            byte[] nameBytes = groupName.getBytes();
            buffer.putShort((short) nameBytes.length);
            buffer.put(nameBytes);
            buffer.put(action);
            buffer.putLong(playerId.getMostSignificantBits());
            buffer.putLong(playerId.getLeastSignificantBits());
        }
        
        @Override
        public void read(ByteBuffer buffer) {
            int idLen = buffer.getShort() & 0xFFFF;
            byte[] idBytes = new byte[idLen];
            buffer.get(idBytes);
            groupId = new String(idBytes);
            int nameLen = buffer.getShort() & 0xFFFF;
            byte[] nameBytes = new byte[nameLen];
            buffer.get(nameBytes);
            groupName = new String(nameBytes);
            action = buffer.get();
            playerId = new UUID(buffer.getLong(), buffer.getLong());
        }
    }
    
    public static class PlayerVolumeUpdate extends VoicePacket {
        public static final byte ID = 0x04;
        
        public UUID targetId;
        public float volume;
        
        @Override
        public byte getPacketId() { return ID; }
        
        @Override
        public void write(ByteBuffer buffer) {
            buffer.putLong(targetId.getMostSignificantBits());
            buffer.putLong(targetId.getLeastSignificantBits());
            buffer.putFloat(volume);
        }
        
        @Override
        public void read(ByteBuffer buffer) {
            targetId = new UUID(buffer.getLong(), buffer.getLong());
            volume = buffer.getFloat();
        }
    }
    
    public static class ActivationModeChange extends VoicePacket {
        public static final byte ID = 0x05;
        
        public byte mode;
        
        @Override
        public byte getPacketId() { return ID; }
        
        @Override
        public void write(ByteBuffer buffer) {
            buffer.put(mode);
        }
        
        @Override
        public void read(ByteBuffer buffer) {
            mode = buffer.get();
        }
    }
}
