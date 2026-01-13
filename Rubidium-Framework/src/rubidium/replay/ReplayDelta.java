package rubidium.replay;

import java.nio.ByteBuffer;

public final class ReplayDelta {
    
    private static final int FLAG_POSITION = 0x0001;
    private static final int FLAG_ROTATION = 0x0002;
    private static final int FLAG_VELOCITY = 0x0004;
    private static final int FLAG_STANCE = 0x0008;
    private static final int FLAG_ANIMATION = 0x0010;
    private static final int FLAG_HELD_ITEM = 0x0020;
    private static final int FLAG_HEALTH = 0x0040;
    private static final int FLAG_ARMOR = 0x0080;
    private static final int FLAG_STATUS_EFFECTS = 0x0100;
    private static final int FLAG_ACTION = 0x0200;
    private static final int FLAG_BLOCK_ACTION = 0x0400;
    private static final int FLAG_MOVEMENT_FLAGS = 0x0800;
    
    private int changeFlags;
    private long timestampDelta;
    
    private short deltaX, deltaY, deltaZ;
    private byte deltaYaw, deltaPitch;
    private short deltaVelX, deltaVelY, deltaVelZ;
    
    private byte stance;
    private byte animation;
    private byte heldItemSlot;
    private int heldItemId;
    
    private float health;
    private float armor;
    private int statusEffectsMask;
    
    private short actionCode;
    private int blockX, blockY, blockZ;
    private byte blockAction;
    
    private byte movementFlags;
    
    public static ReplayDelta compute(ReplayFrame base, ReplayFrame current) {
        ReplayDelta delta = new ReplayDelta();
        delta.timestampDelta = current.getTimestamp() - base.getTimestamp();
        
        double dx = current.getX() - base.getX();
        double dy = current.getY() - base.getY();
        double dz = current.getZ() - base.getZ();
        
        if (Math.abs(dx) > 0.001 || Math.abs(dy) > 0.001 || Math.abs(dz) > 0.001) {
            delta.changeFlags |= FLAG_POSITION;
            delta.deltaX = quantizePosition(dx);
            delta.deltaY = quantizePosition(dy);
            delta.deltaZ = quantizePosition(dz);
        }
        
        float dyaw = current.getYaw() - base.getYaw();
        float dpitch = current.getPitch() - base.getPitch();
        if (Math.abs(dyaw) > 0.1 || Math.abs(dpitch) > 0.1) {
            delta.changeFlags |= FLAG_ROTATION;
            delta.deltaYaw = quantizeAngle(dyaw);
            delta.deltaPitch = quantizeAngle(dpitch);
        }
        
        double dvx = current.getVelX() - base.getVelX();
        double dvy = current.getVelY() - base.getVelY();
        double dvz = current.getVelZ() - base.getVelZ();
        if (Math.abs(dvx) > 0.01 || Math.abs(dvy) > 0.01 || Math.abs(dvz) > 0.01) {
            delta.changeFlags |= FLAG_VELOCITY;
            delta.deltaVelX = quantizeVelocity(dvx);
            delta.deltaVelY = quantizeVelocity(dvy);
            delta.deltaVelZ = quantizeVelocity(dvz);
        }
        
        if (current.getStance() != base.getStance()) {
            delta.changeFlags |= FLAG_STANCE;
            delta.stance = current.getStance();
        }
        
        if (current.getAnimation() != base.getAnimation()) {
            delta.changeFlags |= FLAG_ANIMATION;
            delta.animation = current.getAnimation();
        }
        
        if (current.getHeldItemSlot() != base.getHeldItemSlot() || 
            current.getHeldItemId() != base.getHeldItemId()) {
            delta.changeFlags |= FLAG_HELD_ITEM;
            delta.heldItemSlot = current.getHeldItemSlot();
            delta.heldItemId = current.getHeldItemId();
        }
        
        if (Math.abs(current.getHealth() - base.getHealth()) > 0.01) {
            delta.changeFlags |= FLAG_HEALTH;
            delta.health = current.getHealth();
        }
        
        if (Math.abs(current.getArmor() - base.getArmor()) > 0.01) {
            delta.changeFlags |= FLAG_ARMOR;
            delta.armor = current.getArmor();
        }
        
        if (current.getStatusEffectsMask() != base.getStatusEffectsMask()) {
            delta.changeFlags |= FLAG_STATUS_EFFECTS;
            delta.statusEffectsMask = current.getStatusEffectsMask();
        }
        
        if (current.getActionCode() != 0) {
            delta.changeFlags |= FLAG_ACTION;
            delta.actionCode = current.getActionCode();
        }
        
        if (current.getBlockAction() != 0) {
            delta.changeFlags |= FLAG_BLOCK_ACTION;
            delta.blockX = current.getBlockX();
            delta.blockY = current.getBlockY();
            delta.blockZ = current.getBlockZ();
            delta.blockAction = current.getBlockAction();
        }
        
        byte currentFlags = packMovementFlags(current);
        byte baseFlags = packMovementFlags(base);
        if (currentFlags != baseFlags) {
            delta.changeFlags |= FLAG_MOVEMENT_FLAGS;
            delta.movementFlags = currentFlags;
        }
        
        return delta;
    }
    
    private static byte packMovementFlags(ReplayFrame frame) {
        byte flags = 0;
        if (frame.isOnGround()) flags |= 0x01;
        if (frame.isSprinting()) flags |= 0x02;
        if (frame.isSneaking()) flags |= 0x04;
        if (frame.isSwimming()) flags |= 0x08;
        if (frame.isFlying()) flags |= 0x10;
        if (frame.isGliding()) flags |= 0x20;
        return flags;
    }
    
    private static short quantizePosition(double value) {
        return (short) Math.max(-32768, Math.min(32767, Math.round(value * 4096)));
    }
    
    private static double dequantizePosition(short value) {
        return value / 4096.0;
    }
    
    private static byte quantizeAngle(float value) {
        return (byte) Math.round((value % 360) / 360.0 * 256);
    }
    
    private static float dequantizeAngle(byte value) {
        return (value & 0xFF) * 360.0f / 256.0f;
    }
    
    private static short quantizeVelocity(double value) {
        return (short) Math.max(-32768, Math.min(32767, Math.round(value * 8000)));
    }
    
    private static double dequantizeVelocity(short value) {
        return value / 8000.0;
    }
    
    public void apply(ReplayFrame base, ReplayFrame target) {
        target.setTimestamp(base.getTimestamp() + timestampDelta);
        target.setPlayerId(base.getPlayerId());
        
        if ((changeFlags & FLAG_POSITION) != 0) {
            target.setX(base.getX() + dequantizePosition(deltaX));
            target.setY(base.getY() + dequantizePosition(deltaY));
            target.setZ(base.getZ() + dequantizePosition(deltaZ));
        } else {
            target.setPosition(base.getX(), base.getY(), base.getZ());
        }
        
        if ((changeFlags & FLAG_ROTATION) != 0) {
            target.setYaw(base.getYaw() + dequantizeAngle(deltaYaw));
            target.setPitch(base.getPitch() + dequantizeAngle(deltaPitch));
        } else {
            target.setRotation(base.getYaw(), base.getPitch());
        }
        
        if ((changeFlags & FLAG_VELOCITY) != 0) {
            target.setVelX(base.getVelX() + dequantizeVelocity(deltaVelX));
            target.setVelY(base.getVelY() + dequantizeVelocity(deltaVelY));
            target.setVelZ(base.getVelZ() + dequantizeVelocity(deltaVelZ));
        } else {
            target.setVelocity(base.getVelX(), base.getVelY(), base.getVelZ());
        }
        
        target.setStance((changeFlags & FLAG_STANCE) != 0 ? stance : base.getStance());
        target.setAnimation((changeFlags & FLAG_ANIMATION) != 0 ? animation : base.getAnimation());
        
        if ((changeFlags & FLAG_HELD_ITEM) != 0) {
            target.setHeldItemSlot(heldItemSlot);
            target.setHeldItemId(heldItemId);
        } else {
            target.setHeldItemSlot(base.getHeldItemSlot());
            target.setHeldItemId(base.getHeldItemId());
        }
        
        target.setHealth((changeFlags & FLAG_HEALTH) != 0 ? health : base.getHealth());
        target.setArmor((changeFlags & FLAG_ARMOR) != 0 ? armor : base.getArmor());
        target.setStatusEffectsMask((changeFlags & FLAG_STATUS_EFFECTS) != 0 ? 
            statusEffectsMask : base.getStatusEffectsMask());
        
        target.setActionCode((changeFlags & FLAG_ACTION) != 0 ? actionCode : (short) 0);
        
        if ((changeFlags & FLAG_BLOCK_ACTION) != 0) {
            target.setBlockX(blockX);
            target.setBlockY(blockY);
            target.setBlockZ(blockZ);
            target.setBlockAction(blockAction);
        } else {
            target.setBlockAction((byte) 0);
        }
        
        if ((changeFlags & FLAG_MOVEMENT_FLAGS) != 0) {
            target.setOnGround((movementFlags & 0x01) != 0);
            target.setSprinting((movementFlags & 0x02) != 0);
            target.setSneaking((movementFlags & 0x04) != 0);
            target.setSwimming((movementFlags & 0x08) != 0);
            target.setFlying((movementFlags & 0x10) != 0);
            target.setGliding((movementFlags & 0x20) != 0);
        } else {
            target.setOnGround(base.isOnGround());
            target.setSprinting(base.isSprinting());
            target.setSneaking(base.isSneaking());
            target.setSwimming(base.isSwimming());
            target.setFlying(base.isFlying());
            target.setGliding(base.isGliding());
        }
    }
    
    public int serialize(ByteBuffer buffer) {
        int start = buffer.position();
        
        buffer.putShort((short) changeFlags);
        writeVarLong(buffer, timestampDelta);
        
        if ((changeFlags & FLAG_POSITION) != 0) {
            buffer.putShort(deltaX);
            buffer.putShort(deltaY);
            buffer.putShort(deltaZ);
        }
        
        if ((changeFlags & FLAG_ROTATION) != 0) {
            buffer.put(deltaYaw);
            buffer.put(deltaPitch);
        }
        
        if ((changeFlags & FLAG_VELOCITY) != 0) {
            buffer.putShort(deltaVelX);
            buffer.putShort(deltaVelY);
            buffer.putShort(deltaVelZ);
        }
        
        if ((changeFlags & FLAG_STANCE) != 0) buffer.put(stance);
        if ((changeFlags & FLAG_ANIMATION) != 0) buffer.put(animation);
        
        if ((changeFlags & FLAG_HELD_ITEM) != 0) {
            buffer.put(heldItemSlot);
            buffer.putInt(heldItemId);
        }
        
        if ((changeFlags & FLAG_HEALTH) != 0) buffer.putFloat(health);
        if ((changeFlags & FLAG_ARMOR) != 0) buffer.putFloat(armor);
        if ((changeFlags & FLAG_STATUS_EFFECTS) != 0) buffer.putInt(statusEffectsMask);
        if ((changeFlags & FLAG_ACTION) != 0) buffer.putShort(actionCode);
        
        if ((changeFlags & FLAG_BLOCK_ACTION) != 0) {
            buffer.putInt(blockX);
            buffer.putInt(blockY);
            buffer.putInt(blockZ);
            buffer.put(blockAction);
        }
        
        if ((changeFlags & FLAG_MOVEMENT_FLAGS) != 0) buffer.put(movementFlags);
        
        return buffer.position() - start;
    }
    
    public void deserialize(ByteBuffer buffer) {
        changeFlags = buffer.getShort() & 0xFFFF;
        timestampDelta = readVarLong(buffer);
        
        if ((changeFlags & FLAG_POSITION) != 0) {
            deltaX = buffer.getShort();
            deltaY = buffer.getShort();
            deltaZ = buffer.getShort();
        }
        
        if ((changeFlags & FLAG_ROTATION) != 0) {
            deltaYaw = buffer.get();
            deltaPitch = buffer.get();
        }
        
        if ((changeFlags & FLAG_VELOCITY) != 0) {
            deltaVelX = buffer.getShort();
            deltaVelY = buffer.getShort();
            deltaVelZ = buffer.getShort();
        }
        
        if ((changeFlags & FLAG_STANCE) != 0) stance = buffer.get();
        if ((changeFlags & FLAG_ANIMATION) != 0) animation = buffer.get();
        
        if ((changeFlags & FLAG_HELD_ITEM) != 0) {
            heldItemSlot = buffer.get();
            heldItemId = buffer.getInt();
        }
        
        if ((changeFlags & FLAG_HEALTH) != 0) health = buffer.getFloat();
        if ((changeFlags & FLAG_ARMOR) != 0) armor = buffer.getFloat();
        if ((changeFlags & FLAG_STATUS_EFFECTS) != 0) statusEffectsMask = buffer.getInt();
        if ((changeFlags & FLAG_ACTION) != 0) actionCode = buffer.getShort();
        
        if ((changeFlags & FLAG_BLOCK_ACTION) != 0) {
            blockX = buffer.getInt();
            blockY = buffer.getInt();
            blockZ = buffer.getInt();
            blockAction = buffer.get();
        }
        
        if ((changeFlags & FLAG_MOVEMENT_FLAGS) != 0) movementFlags = buffer.get();
    }
    
    private static void writeVarLong(ByteBuffer buffer, long value) {
        while ((value & ~0x7FL) != 0) {
            buffer.put((byte) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        buffer.put((byte) value);
    }
    
    private static long readVarLong(ByteBuffer buffer) {
        long result = 0;
        int shift = 0;
        byte b;
        do {
            b = buffer.get();
            result |= (long) (b & 0x7F) << shift;
            shift += 7;
        } while ((b & 0x80) != 0);
        return result;
    }
    
    public int getChangeFlags() {
        return changeFlags;
    }
    
    public boolean hasChanges() {
        return changeFlags != 0;
    }
}
