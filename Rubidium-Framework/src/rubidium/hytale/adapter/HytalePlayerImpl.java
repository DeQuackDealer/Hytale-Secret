package rubidium.hytale.adapter;

import com.hypixel.hytale.server.api.player.Player;
import com.hypixel.hytale.server.core.entity.entities.player.ServerPlayer;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUICommand;

import java.util.UUID;
import java.util.logging.Logger;

public class HytalePlayerImpl implements Player {
    
    private static final Logger LOGGER = Logger.getLogger("Rubidium-Player");
    
    private final UUID uuid;
    private final String name;
    private final ServerPlayer serverPlayer;
    private final PageManager pageManager;
    
    private boolean online = true;
    private double x, y, z;
    private String world = "world";
    
    public HytalePlayerImpl(UUID uuid, String name, ServerPlayer serverPlayer) {
        this.uuid = uuid;
        this.name = name;
        this.serverPlayer = serverPlayer;
        this.pageManager = serverPlayer != null ? serverPlayer.getPageManager() : null;
    }
    
    public static HytalePlayerImpl wrap(ServerPlayer serverPlayer) {
        if (serverPlayer == null) return null;
        
        UUID uuid = serverPlayer.getUuid();
        String name = serverPlayer.getUsername();
        
        return new HytalePlayerImpl(uuid, name, serverPlayer);
    }
    
    @Override
    public UUID getUUID() {
        return uuid;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean isOnline() {
        return online && serverPlayer != null && serverPlayer.isOnline();
    }
    
    @Override
    public void sendMessage(String message) {
        if (serverPlayer != null) {
            serverPlayer.sendMessage(message);
        }
    }
    
    @Override
    public void sendCustomPageOpen(String pageId, String uiPath, CustomPageLifetime lifetime, CustomUICommand[] commands) {
        LOGGER.info("[Rubidium] Opening page '" + pageId + "' for " + name + " with path: " + uiPath);
        
        if (serverPlayer != null) {
            serverPlayer.sendCustomPageOpen(pageId, uiPath, lifetime, commands);
        } else if (pageManager != null) {
            pageManager.openCustomPage(pageId, uiPath, lifetime, commands);
        } else {
            LOGGER.warning("[Rubidium] No server player or page manager available for " + name);
        }
    }
    
    @Override
    public void sendCustomPageClose(String pageId) {
        LOGGER.info("[Rubidium] Closing page '" + pageId + "' for " + name);
        
        if (serverPlayer != null) {
            serverPlayer.sendCustomPageClose(pageId);
        } else if (pageManager != null) {
            pageManager.closeCustomPage(pageId);
        }
    }
    
    @Override
    public void sendCustomUICommands(String pageId, CustomUICommand[] commands) {
        LOGGER.fine("[Rubidium] Sending " + commands.length + " UI commands to page '" + pageId + "' for " + name);
        
        if (serverPlayer != null) {
            serverPlayer.sendCustomUICommands(pageId, commands);
        } else if (pageManager != null) {
            pageManager.sendUICommands(pageId, commands);
        }
    }
    
    @Override
    public void kick(String reason) {
        if (serverPlayer != null) {
            serverPlayer.kick(reason);
        }
        online = false;
    }
    
    @Override
    public boolean hasPermission(String permission) {
        if (serverPlayer != null) {
            return serverPlayer.hasPermission(permission);
        }
        return false;
    }
    
    @Override
    public void teleport(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        if (serverPlayer != null) {
            serverPlayer.teleport(x, y, z);
        }
    }
    
    @Override
    public double getX() {
        if (serverPlayer != null) {
            return serverPlayer.getX();
        }
        return x;
    }
    
    @Override
    public double getY() {
        if (serverPlayer != null) {
            return serverPlayer.getY();
        }
        return y;
    }
    
    @Override
    public double getZ() {
        if (serverPlayer != null) {
            return serverPlayer.getZ();
        }
        return z;
    }
    
    @Override
    public String getWorld() {
        if (serverPlayer != null) {
            return serverPlayer.getWorldName();
        }
        return world;
    }
    
    public ServerPlayer getServerPlayer() {
        return serverPlayer;
    }
    
    public PageManager getPageManager() {
        return pageManager;
    }
    
    void setOnline(boolean online) {
        this.online = online;
    }
}
