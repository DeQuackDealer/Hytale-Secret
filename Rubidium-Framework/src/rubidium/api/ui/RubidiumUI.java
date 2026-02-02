package rubidium.api.ui;

import com.hypixel.hytale.server.api.player.Player;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUICommand;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class RubidiumUI {
    
    private static final Logger LOGGER = Logger.getLogger("RubidiumUI");
    private static final Map<UUID, Map<String, RubidiumPage<?>>> activePagesPerPlayer = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, RubidiumOverlayPage<?>>> activeOverlaysPerPlayer = new ConcurrentHashMap<>();
    
    private RubidiumUI() {}
    
    public static <T> void openPage(Player player, RubidiumPage<T> page) {
        openPage(player, page, CustomPageLifetime.CLOSE_ON_DISCONNECT);
    }
    
    public static <T> void openPage(Player player, RubidiumPage<T> page, CustomPageLifetime lifetime) {
        UUID playerId = player.getUUID();
        String pageId = page.getPageId();
        
        activePagesPerPlayer.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(pageId, page);
        
        page.setPlayer(player);
        page.setLifetime(lifetime);
        
        String uiPath = page.getUIPath();
        CustomUICommand[] initCommands = page.buildInitialCommands();
        
        sendPageOpen(player, pageId, uiPath, lifetime, initCommands);
        
        page.onOpened();
        LOGGER.info("Opened page '" + pageId + "' for player " + playerId);
    }
    
    public static <T> void openOverlay(Player player, RubidiumOverlayPage<T> overlay) {
        UUID playerId = player.getUUID();
        String overlayId = overlay.getPageId();
        
        activeOverlaysPerPlayer.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(overlayId, overlay);
        
        overlay.setPlayer(player);
        overlay.setLifetime(CustomPageLifetime.ALWAYS);
        
        String uiPath = overlay.getUIPath();
        CustomUICommand[] initCommands = overlay.buildInitialCommands();
        
        sendPageOpen(player, overlayId, uiPath, CustomPageLifetime.ALWAYS, initCommands);
        
        overlay.onOpened();
        LOGGER.info("Opened overlay '" + overlayId + "' for player " + playerId);
    }
    
    public static void closePage(Player player, String pageId) {
        UUID playerId = player.getUUID();
        
        Map<String, RubidiumPage<?>> pages = activePagesPerPlayer.get(playerId);
        if (pages != null) {
            RubidiumPage<?> page = pages.remove(pageId);
            if (page != null) {
                page.onClosed();
            }
        }
        
        sendPageClose(player, pageId);
        LOGGER.info("Closed page '" + pageId + "' for player " + playerId);
    }
    
    public static void closeOverlay(Player player, String overlayId) {
        UUID playerId = player.getUUID();
        
        Map<String, RubidiumOverlayPage<?>> overlays = activeOverlaysPerPlayer.get(playerId);
        if (overlays != null) {
            RubidiumOverlayPage<?> overlay = overlays.remove(overlayId);
            if (overlay != null) {
                overlay.onClosed();
            }
        }
        
        sendPageClose(player, overlayId);
        LOGGER.info("Closed overlay '" + overlayId + "' for player " + playerId);
    }
    
    public static <T> void closeOverlay(Player player, RubidiumOverlayPage<T> overlay) {
        closeOverlay(player, overlay.getPageId());
    }
    
    public static void rebuild(Player player, RubidiumPage<?> page) {
        CustomUICommand[] commands = page.buildUpdateCommands();
        if (commands.length > 0) {
            sendUICommands(player, page.getPageId(), commands);
        }
    }
    
    public static void rebuildOverlay(Player player, RubidiumOverlayPage<?> overlay) {
        CustomUICommand[] commands = overlay.buildUpdateCommands();
        if (commands.length > 0) {
            sendUICommands(player, overlay.getPageId(), commands);
        }
    }
    
    public static RubidiumPage<?> getActivePage(Player player, String pageId) {
        Map<String, RubidiumPage<?>> pages = activePagesPerPlayer.get(player.getUUID());
        return pages != null ? pages.get(pageId) : null;
    }
    
    public static RubidiumOverlayPage<?> getActiveOverlay(Player player, String overlayId) {
        Map<String, RubidiumOverlayPage<?>> overlays = activeOverlaysPerPlayer.get(player.getUUID());
        return overlays != null ? overlays.get(overlayId) : null;
    }
    
    public static void cleanupPlayer(UUID playerId) {
        Map<String, RubidiumPage<?>> pages = activePagesPerPlayer.remove(playerId);
        if (pages != null) {
            pages.values().forEach(RubidiumPage::onClosed);
        }
        
        Map<String, RubidiumOverlayPage<?>> overlays = activeOverlaysPerPlayer.remove(playerId);
        if (overlays != null) {
            overlays.values().forEach(RubidiumOverlayPage::onClosed);
        }
    }
    
    private static void sendPageOpen(Player player, String pageId, String uiPath, CustomPageLifetime lifetime, CustomUICommand[] commands) {
        player.sendCustomPageOpen(pageId, uiPath, lifetime, commands);
    }
    
    private static void sendPageClose(Player player, String pageId) {
        player.sendCustomPageClose(pageId);
    }
    
    private static void sendUICommands(Player player, String pageId, CustomUICommand[] commands) {
        player.sendCustomUICommands(pageId, commands);
    }
}
