package com.hypixel.hytale.server.core.entity.entities.player.pages;

public class PageManager {
    
    private CustomUIPage currentPage;
    
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
}
