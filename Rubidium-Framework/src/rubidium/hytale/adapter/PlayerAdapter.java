package rubidium.hytale.adapter;

import rubidium.hytale.api.player.Player;
import com.hypixel.hytale.server.core.entity.entities.player.ServerPlayer;

import java.util.UUID;

/**
 * Adapts the real Hytale ServerPlayer to Rubidium's Player interface.
 * This enables Rubidium to work with the actual Hytale server at runtime.
 */
public class PlayerAdapter implements Player {
    
    private final ServerPlayer hytalePlayer;
    
    public PlayerAdapter(ServerPlayer hytalePlayer) {
        this.hytalePlayer = hytalePlayer;
    }
    
    public ServerPlayer getHandle() {
        return hytalePlayer;
    }
    
    @Override
    public UUID getUuid() {
        return hytalePlayer.getUuid();
    }
    
    @Override
    public String getUsername() {
        return hytalePlayer.getUsername();
    }
    
    @Override
    public String getDisplayName() {
        return hytalePlayer.getDisplayName();
    }
    
    @Override
    public void setDisplayName(String displayName) {
        hytalePlayer.setDisplayName(displayName);
    }
    
    @Override
    public double getX() {
        return hytalePlayer.getX();
    }
    
    @Override
    public double getY() {
        return hytalePlayer.getY();
    }
    
    @Override
    public double getZ() {
        return hytalePlayer.getZ();
    }
    
    @Override
    public float getYaw() {
        return hytalePlayer.getYaw();
    }
    
    @Override
    public float getPitch() {
        return hytalePlayer.getPitch();
    }
    
    @Override
    public String getWorld() {
        return hytalePlayer.getWorld();
    }
    
    @Override
    public void teleport(double x, double y, double z) {
        hytalePlayer.teleport(x, y, z);
    }
    
    @Override
    public void teleport(double x, double y, double z, float yaw, float pitch) {
        hytalePlayer.teleport(x, y, z, yaw, pitch);
    }
    
    @Override
    public void sendMessage(String message) {
        hytalePlayer.sendMessage(message);
    }
    
    @Override
    public void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        hytalePlayer.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }
    
    @Override
    public void sendActionBar(String message) {
        hytalePlayer.sendActionBar(message);
    }
    
    @Override
    public boolean hasPermission(String permission) {
        return hytalePlayer.hasPermission(permission);
    }
    
    @Override
    public void kick(String reason) {
        hytalePlayer.kick(reason);
    }
    
    @Override
    public boolean isOnline() {
        return hytalePlayer.isOnline();
    }
    
    @Override
    public void sendPacket(Object packet) {
        hytalePlayer.sendPacket(packet);
    }
    
    @Override
    public float getHealth() {
        return hytalePlayer.getHealth();
    }
    
    @Override
    public void setHealth(float health) {
        hytalePlayer.setHealth(health);
    }
    
    @Override
    public float getMaxHealth() {
        return hytalePlayer.getMaxHealth();
    }
    
    @Override
    public void setMaxHealth(float maxHealth) {
        hytalePlayer.setMaxHealth(maxHealth);
    }
    
    @Override
    public int getPing() {
        return hytalePlayer.getPing();
    }
    
    @Override
    public void playSound(String sound, float volume, float pitch) {
        hytalePlayer.playSound(sound, volume, pitch);
    }
}
