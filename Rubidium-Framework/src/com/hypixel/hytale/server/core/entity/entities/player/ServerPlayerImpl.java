package com.hypixel.hytale.server.core.entity.entities.player;

import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.protocol.packets.PacketTracker;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUICommand;

import java.util.UUID;
import java.util.logging.Logger;

public class ServerPlayerImpl implements ServerPlayer {
    
    private static final Logger LOGGER = Logger.getLogger("ServerPlayer");
    
    private final UUID uuid;
    private final String username;
    private String displayName;
    private final PageManager pageManager;
    private final HudManager hudManager;
    
    private boolean online = true;
    private double x, y, z;
    private float yaw, pitch;
    private String world = "world";
    private String gameMode = "SURVIVAL";
    private float health = 20.0f;
    private float maxHealth = 20.0f;
    
    public ServerPlayerImpl(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
        this.displayName = username;
        this.pageManager = new PageManager();
        this.hudManager = new HudManager();
    }
    
    @Override
    public UUID getUuid() {
        return uuid;
    }
    
    @Override
    public int getEntityId() {
        return uuid.hashCode();
    }
    
    @Override
    public String getUsername() {
        return username;
    }
    
    @Override
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    @Override
    public void sendMessage(String message) {
        LOGGER.info("[" + username + "] " + message);
    }
    
    @Override
    public void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        LOGGER.info("[" + username + "] TITLE: " + title + " | " + subtitle);
    }
    
    @Override
    public void sendActionBar(String message) {
        LOGGER.info("[" + username + "] ACTION_BAR: " + message);
    }
    
    @Override
    public boolean hasPermission(String permission) {
        return true;
    }
    
    @Override
    public void kick(String reason) {
        LOGGER.info("[" + username + "] KICKED: " + reason);
        online = false;
    }
    
    @Override
    public boolean isOnline() {
        return online;
    }
    
    @Override
    public int getPing() {
        return 50;
    }
    
    @Override
    public String getGameMode() {
        return gameMode;
    }
    
    @Override
    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
    }
    
    @Override
    public float getHealth() {
        return health;
    }
    
    @Override
    public void setHealth(float health) {
        this.health = health;
    }
    
    @Override
    public float getMaxHealth() {
        return maxHealth;
    }
    
    @Override
    public void setMaxHealth(float maxHealth) {
        this.maxHealth = maxHealth;
    }
    
    @Override
    public void playSound(String sound, float volume, float pitch) {
        LOGGER.fine("[" + username + "] SOUND: " + sound);
    }
    
    @Override
    public void sendPacket(Object packet) {
        LOGGER.fine("[" + username + "] PACKET: " + packet.getClass().getSimpleName());
    }
    
    @Override
    public PageManager getPageManager() {
        return pageManager;
    }
    
    @Override
    public HudManager getHudManager() {
        return hudManager;
    }
    
    @Override
    public void sendCustomPageOpen(String pageId, String uiPath, CustomPageLifetime lifetime, CustomUICommand[] commands) {
        LOGGER.info("[" + username + "] UI_PAGE_OPEN: pageId=" + pageId + ", path=" + uiPath + ", lifetime=" + lifetime + ", commands=" + (commands != null ? commands.length : 0));
        
        PacketTracker.get().trackPageOpen(uuid, pageId, uiPath, lifetime, commands != null ? commands.length : 0);
        
        pageManager.openCustomPage(pageId, uiPath, lifetime, commands);
    }
    
    @Override
    public void sendCustomPageClose(String pageId) {
        LOGGER.info("[" + username + "] UI_PAGE_CLOSE: pageId=" + pageId);
        
        PacketTracker.get().trackPageClose(uuid, pageId);
        
        pageManager.closeCustomPage(pageId);
    }
    
    @Override
    public void sendCustomUICommands(String pageId, CustomUICommand[] commands) {
        LOGGER.fine("[" + username + "] UI_COMMANDS: pageId=" + pageId + ", commands=" + (commands != null ? commands.length : 0));
        
        PacketTracker.get().trackUICommands(uuid, pageId, commands != null ? commands.length : 0);
        
        pageManager.sendUICommands(pageId, commands);
    }
    
    @Override
    public double getX() { return x; }
    
    @Override
    public double getY() { return y; }
    
    @Override
    public double getZ() { return z; }
    
    @Override
    public float getYaw() { return yaw; }
    
    @Override
    public float getPitch() { return pitch; }
    
    @Override
    public String getWorld() { return world; }
    
    @Override
    public String getWorldName() { return world; }
    
    @Override
    public void teleport(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    @Override
    public void teleport(double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }
    
    @Override
    public boolean isValid() {
        return online;
    }
    
    @Override
    public void remove() {
        online = false;
    }
    
    public void setOnline(boolean online) {
        this.online = online;
    }
    
    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
