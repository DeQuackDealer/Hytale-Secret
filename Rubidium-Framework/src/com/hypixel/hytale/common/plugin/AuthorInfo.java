package com.hypixel.hytale.common.plugin;

/**
 * Development stub for Hytale's AuthorInfo.
 * Contains author metadata for plugins.
 * At runtime, the real AuthorInfo from HytaleServer.jar will be used.
 * 
 * DO NOT modify this file - it mirrors the official Hytale API.
 */
public class AuthorInfo {
    
    private String name;
    private String url;
    
    public AuthorInfo() {}
    
    public AuthorInfo(String name, String url) {
        this.name = name;
        this.url = url;
    }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    @Override
    public String toString() {
        return name + (url != null ? " (" + url + ")" : "");
    }
}
