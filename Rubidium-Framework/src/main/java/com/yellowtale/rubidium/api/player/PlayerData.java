package com.yellowtale.rubidium.api.player;

import java.util.Optional;

public interface PlayerData {
    
    void set(String key, Object value);
    
    <T> Optional<T> get(String key, Class<T> type);
    
    String getString(String key);
    
    String getString(String key, String defaultValue);
    
    int getInt(String key);
    
    int getInt(String key, int defaultValue);
    
    long getLong(String key);
    
    long getLong(String key, long defaultValue);
    
    double getDouble(String key);
    
    double getDouble(String key, double defaultValue);
    
    boolean getBoolean(String key);
    
    boolean getBoolean(String key, boolean defaultValue);
    
    boolean has(String key);
    
    void remove(String key);
    
    void save();
    
    void reload();
}
