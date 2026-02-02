package com.hypixel.hytale.server.core.entity.entities.player.pages;

import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUICommand;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class PageManager {
    
    private static final Logger LOGGER = Logger.getLogger("PageManager");
    
    private CustomUIPage currentPage;
    private final Map<String, CustomPageData> customPages = new ConcurrentHashMap<>();
    
    public void openPage(CustomUIPage page) {
        this.currentPage = page;
    }
    
    public void closePage() {
        this.currentPage = null;
    }
    
    public CustomUIPage getCurrentPage() {
        return currentPage;
    }
    
    public boolean hasOpenPage() {
        return currentPage != null;
    }
    
    public void openCustomPage(String pageId, String uiPath, CustomPageLifetime lifetime, CustomUICommand[] commands) {
        LOGGER.fine("[PageManager] Opening custom page: " + pageId + " (" + uiPath + ")");
        customPages.put(pageId, new CustomPageData(pageId, uiPath, lifetime, commands));
    }
    
    public void closeCustomPage(String pageId) {
        LOGGER.fine("[PageManager] Closing custom page: " + pageId);
        customPages.remove(pageId);
    }
    
    public void sendUICommands(String pageId, CustomUICommand[] commands) {
        LOGGER.fine("[PageManager] Sending " + commands.length + " commands to page: " + pageId);
        CustomPageData data = customPages.get(pageId);
        if (data != null) {
            data.addCommands(commands);
        }
    }
    
    public boolean hasCustomPage(String pageId) {
        return customPages.containsKey(pageId);
    }
    
    public int getCustomPageCount() {
        return customPages.size();
    }
    
    public record CustomPageData(String pageId, String uiPath, CustomPageLifetime lifetime, CustomUICommand[] initialCommands) {
        private static final Map<String, java.util.List<CustomUICommand>> commandHistory = new ConcurrentHashMap<>();
        
        public void addCommands(CustomUICommand[] commands) {
            commandHistory.computeIfAbsent(pageId, k -> new java.util.ArrayList<>())
                .addAll(java.util.Arrays.asList(commands));
        }
    }
}
