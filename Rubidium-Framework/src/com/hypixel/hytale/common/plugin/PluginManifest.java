package com.hypixel.hytale.common.plugin;

import java.util.List;
import java.util.Map;

/**
 * Development stub for Hytale's PluginManifest.
 * Contains plugin metadata loaded from manifests.json.
 * At runtime, the real PluginManifest from HytaleServer.jar will be used.
 * 
 * DO NOT modify this file - it mirrors the official Hytale API.
 */
public class PluginManifest {
    
    private String group;
    private String name;
    private String version;
    private String description;
    private String main;
    private String website;
    private List<AuthorInfo> authors;
    private boolean disabledByDefault;
    private boolean includesAssetPack;
    
    public PluginManifest() {}
    
    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getMain() { return main; }
    public void setMain(String main) { this.main = main; }
    
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    
    public List<AuthorInfo> getAuthors() { return authors; }
    public void setAuthors(List<AuthorInfo> authors) { this.authors = authors; }
    
    public boolean isDisabledByDefault() { return disabledByDefault; }
    public void setDisabledByDefault(boolean disabledByDefault) { this.disabledByDefault = disabledByDefault; }
    
    public boolean includesAssetPack() { return includesAssetPack; }
    public void setIncludesAssetPack(boolean includesAssetPack) { this.includesAssetPack = includesAssetPack; }
    
    @Override
    public String toString() {
        return group + ":" + name + ":" + version;
    }
}
