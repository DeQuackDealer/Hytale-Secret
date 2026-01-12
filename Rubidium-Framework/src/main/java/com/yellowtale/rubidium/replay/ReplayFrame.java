package com.yellowtale.rubidium.replay;

import java.nio.ByteBuffer;
import java.util.UUID;

public final class ReplayFrame {
    
    private static final int SERIALIZED_SIZE = 128;
    
    private long timestamp;
    private UUID playerId;
    
    private double x, y, z;
    private float yaw, pitch;
    private double velX, velY, velZ;
    
    private byte stance;
    private byte animation;
    private byte heldItemSlot;
    private int heldItemId;
    
    private float health;
    private float armor;
    private int statusEffectsMask;
    
    private short actionCode;
    private UUID targetEntityId;
    private int blockX, blockY, blockZ;
    private byte blockAction;
    
    private boolean onGround;
    private boolean sprinting;
    private boolean sneaking;
    private boolean swimming;
    private boolean flying;
    private boolean gliding;
    
    public ReplayFrame() {}
    
    public ReplayFrame copy() {
        ReplayFrame copy = new ReplayFrame();
        copy.timestamp = this.timestamp;
        copy.playerId = this.playerId;
        copy.x = this.x;
        copy.y = this.y;
        copy.z = this.z;
        copy.yaw = this.yaw;
        copy.pitch = this.pitch;
        copy.velX = this.velX;
        copy.velY = this.velY;
        copy.velZ = this.velZ;
        copy.stance = this.stance;
        copy.animation = this.animation;
        copy.heldItemSlot = this.heldItemSlot;
        copy.heldItemId = this.heldItemId;
        copy.health = this.health;
        copy.armor = this.armor;
        copy.statusEffectsMask = this.statusEffectsMask;
        copy.actionCode = this.actionCode;
        copy.targetEntityId = this.targetEntityId;
        copy.blockX = this.blockX;
        copy.blockY = this.blockY;
        copy.blockZ = this.blockZ;
        copy.blockAction = this.blockAction;
        copy.onGround = this.onGround;
        copy.sprinting = this.sprinting;
        copy.sneaking = this.sneaking;
        copy.swimming = this.swimming;
        copy.flying = this.flying;
        copy.gliding = this.gliding;
        return copy;
    }
    
    public void reset() {
        this.timestamp = 0;
        this.playerId = null;
        this.x = this.y = this.z = 0;
        this.yaw = this.pitch = 0;
        this.velX = this.velY = this.velZ = 0;
        this.stance = this.animation = this.heldItemSlot = 0;
        this.heldItemId = 0;
        this.health = this.armor = 0;
        this.statusEffectsMask = 0;
        this.actionCode = 0;
        this.targetEntityId = null;
        this.blockX = this.blockY = this.blockZ = 0;
        this.blockAction = 0;
        this.onGround = this.sprinting = this.sneaking = false;
        this.swimming = this.flying = this.gliding = false;
    }
    
    public int serialize(ByteBuffer buffer) {
        int start = buffer.position();
        
        buffer.putLong(timestamp);
        buffer.putLong(playerId != null ? playerId.getMostSignificantBits() : 0);
        buffer.putLong(playerId != null ? playerId.getLeastSignificantBits() : 0);
        
        buffer.putDouble(x);
        buffer.putDouble(y);
        buffer.putDouble(z);
        buffer.putFloat(yaw);
        buffer.putFloat(pitch);
        
        buffer.putFloat((float) velX);
        buffer.putFloat((float) velY);
        buffer.putFloat((float) velZ);
        
        buffer.put(stance);
        buffer.put(animation);
        buffer.put(heldItemSlot);
        buffer.putInt(heldItemId);
        
        buffer.putFloat(health);
        buffer.putFloat(armor);
        buffer.putInt(statusEffectsMask);
        
        buffer.putShort(actionCode);
        buffer.putLong(targetEntityId != null ? targetEntityId.getMostSignificantBits() : 0);
        buffer.putLong(targetEntityId != null ? targetEntityId.getLeastSignificantBits() : 0);
        
        buffer.putInt(blockX);
        buffer.putInt(blockY);
        buffer.putInt(blockZ);
        buffer.put(blockAction);
        
        byte flags = 0;
        if (onGround) flags |= 0x01;
        if (sprinting) flags |= 0x02;
        if (sneaking) flags |= 0x04;
        if (swimming) flags |= 0x08;
        if (flying) flags |= 0x10;
        if (gliding) flags |= 0x20;
        buffer.put(flags);
        
        return buffer.position() - start;
    }
    
    public void deserialize(ByteBuffer buffer) {
        timestamp = buffer.getLong();
        long msb = buffer.getLong();
        long lsb = buffer.getLong();
        playerId = (msb != 0 || lsb != 0) ? new UUID(msb, lsb) : null;
        
        x = buffer.getDouble();
        y = buffer.getDouble();
        z = buffer.getDouble();
        yaw = buffer.getFloat();
        pitch = buffer.getFloat();
        
        velX = buffer.getFloat();
        velY = buffer.getFloat();
        velZ = buffer.getFloat();
        
        stance = buffer.get();
        animation = buffer.get();
        heldItemSlot = buffer.get();
        heldItemId = buffer.getInt();
        
        health = buffer.getFloat();
        armor = buffer.getFloat();
        statusEffectsMask = buffer.getInt();
        
        actionCode = buffer.getShort();
        msb = buffer.getLong();
        lsb = buffer.getLong();
        targetEntityId = (msb != 0 || lsb != 0) ? new UUID(msb, lsb) : null;
        
        blockX = buffer.getInt();
        blockY = buffer.getInt();
        blockZ = buffer.getInt();
        blockAction = buffer.get();
        
        byte flags = buffer.get();
        onGround = (flags & 0x01) != 0;
        sprinting = (flags & 0x02) != 0;
        sneaking = (flags & 0x04) != 0;
        swimming = (flags & 0x08) != 0;
        flying = (flags & 0x10) != 0;
        gliding = (flags & 0x20) != 0;
    }
    
    public static int getSerializedSize() {
        return SERIALIZED_SIZE;
    }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public UUID getPlayerId() { return playerId; }
    public void setPlayerId(UUID playerId) { this.playerId = playerId; }
    
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    
    public double getZ() { return z; }
    public void setZ(double z) { this.z = z; }
    
    public float getYaw() { return yaw; }
    public void setYaw(float yaw) { this.yaw = yaw; }
    
    public float getPitch() { return pitch; }
    public void setPitch(float pitch) { this.pitch = pitch; }
    
    public double getVelX() { return velX; }
    public void setVelX(double velX) { this.velX = velX; }
    
    public double getVelY() { return velY; }
    public void setVelY(double velY) { this.velY = velY; }
    
    public double getVelZ() { return velZ; }
    public void setVelZ(double velZ) { this.velZ = velZ; }
    
    public byte getStance() { return stance; }
    public void setStance(byte stance) { this.stance = stance; }
    
    public byte getAnimation() { return animation; }
    public void setAnimation(byte animation) { this.animation = animation; }
    
    public byte getHeldItemSlot() { return heldItemSlot; }
    public void setHeldItemSlot(byte heldItemSlot) { this.heldItemSlot = heldItemSlot; }
    
    public int getHeldItemId() { return heldItemId; }
    public void setHeldItemId(int heldItemId) { this.heldItemId = heldItemId; }
    
    public float getHealth() { return health; }
    public void setHealth(float health) { this.health = health; }
    
    public float getArmor() { return armor; }
    public void setArmor(float armor) { this.armor = armor; }
    
    public int getStatusEffectsMask() { return statusEffectsMask; }
    public void setStatusEffectsMask(int statusEffectsMask) { this.statusEffectsMask = statusEffectsMask; }
    
    public short getActionCode() { return actionCode; }
    public void setActionCode(short actionCode) { this.actionCode = actionCode; }
    
    public UUID getTargetEntityId() { return targetEntityId; }
    public void setTargetEntityId(UUID targetEntityId) { this.targetEntityId = targetEntityId; }
    
    public int getBlockX() { return blockX; }
    public void setBlockX(int blockX) { this.blockX = blockX; }
    
    public int getBlockY() { return blockY; }
    public void setBlockY(int blockY) { this.blockY = blockY; }
    
    public int getBlockZ() { return blockZ; }
    public void setBlockZ(int blockZ) { this.blockZ = blockZ; }
    
    public byte getBlockAction() { return blockAction; }
    public void setBlockAction(byte blockAction) { this.blockAction = blockAction; }
    
    public boolean isOnGround() { return onGround; }
    public void setOnGround(boolean onGround) { this.onGround = onGround; }
    
    public boolean isSprinting() { return sprinting; }
    public void setSprinting(boolean sprinting) { this.sprinting = sprinting; }
    
    public boolean isSneaking() { return sneaking; }
    public void setSneaking(boolean sneaking) { this.sneaking = sneaking; }
    
    public boolean isSwimming() { return swimming; }
    public void setSwimming(boolean swimming) { this.swimming = swimming; }
    
    public boolean isFlying() { return flying; }
    public void setFlying(boolean flying) { this.flying = flying; }
    
    public boolean isGliding() { return gliding; }
    public void setGliding(boolean gliding) { this.gliding = gliding; }
    
    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public void setRotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }
    
    public void setVelocity(double velX, double velY, double velZ) {
        this.velX = velX;
        this.velY = velY;
        this.velZ = velZ;
    }
}
