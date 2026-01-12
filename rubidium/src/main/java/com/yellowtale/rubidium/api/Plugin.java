package com.yellowtale.rubidium.api;

import java.io.File;

public interface Plugin {
    void onLoad();
    void onEnable();
    void onDisable();
    
    String getName();
    String getVersion();
    File getDataFolder();
    
    default String getDescription() {
        return "";
    }
    
    default String[] getAuthors() {
        return new String[0];
    }
    
    default String[] getDependencies() {
        return new String[0];
    }
    
    default String[] getSoftDependencies() {
        return new String[0];
    }
}
