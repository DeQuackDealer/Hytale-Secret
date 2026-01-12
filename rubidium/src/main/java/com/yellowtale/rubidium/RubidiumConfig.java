package com.yellowtale.rubidium;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class RubidiumConfig {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private boolean ipcEnabled = true;
    private int ipcPort = 19100;
    private boolean metricsEnabled = true;
    private int metricsInterval = 1000;
    private boolean hotReloadEnabled = false;
    private int tickBudgetMs = 50;
    private boolean asyncTasksEnabled = true;
    private int maxAsyncThreads = 4;
    
    private boolean rpalSimdEnabled = true;
    private boolean rpalDirectBuffersEnabled = true;
    private boolean rpalObjectPoolingEnabled = true;
    private boolean rpalTickSchedulerEnabled = true;
    private boolean rpalPreallocateBuffers = true;
    private int rpalAsyncThreads = Runtime.getRuntime().availableProcessors();
    
    public static RubidiumConfig load() {
        Path configPath = Path.of("rubidium", "config.json");
        
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                return gson.fromJson(reader, RubidiumConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        RubidiumConfig config = new RubidiumConfig();
        config.save();
        return config;
    }
    
    public void save() {
        Path configPath = Path.of("rubidium", "config.json");
        
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                gson.toJson(this, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public boolean isIpcEnabled() { return ipcEnabled; }
    public void setIpcEnabled(boolean enabled) { this.ipcEnabled = enabled; }
    
    public int getIpcPort() { return ipcPort; }
    public void setIpcPort(int port) { this.ipcPort = port; }
    
    public boolean isMetricsEnabled() { return metricsEnabled; }
    public void setMetricsEnabled(boolean enabled) { this.metricsEnabled = enabled; }
    
    public int getMetricsInterval() { return metricsInterval; }
    public void setMetricsInterval(int interval) { this.metricsInterval = interval; }
    
    public boolean isHotReloadEnabled() { return hotReloadEnabled; }
    public void setHotReloadEnabled(boolean enabled) { this.hotReloadEnabled = enabled; }
    
    public int getTickBudgetMs() { return tickBudgetMs; }
    public void setTickBudgetMs(int ms) { this.tickBudgetMs = ms; }
    
    public boolean isAsyncTasksEnabled() { return asyncTasksEnabled; }
    public void setAsyncTasksEnabled(boolean enabled) { this.asyncTasksEnabled = enabled; }
    
    public int getMaxAsyncThreads() { return maxAsyncThreads; }
    public void setMaxAsyncThreads(int threads) { this.maxAsyncThreads = threads; }
    
    public boolean isRpalSimdEnabled() { return rpalSimdEnabled; }
    public void setRpalSimdEnabled(boolean enabled) { this.rpalSimdEnabled = enabled; }
    
    public boolean isRpalDirectBuffersEnabled() { return rpalDirectBuffersEnabled; }
    public void setRpalDirectBuffersEnabled(boolean enabled) { this.rpalDirectBuffersEnabled = enabled; }
    
    public boolean isRpalObjectPoolingEnabled() { return rpalObjectPoolingEnabled; }
    public void setRpalObjectPoolingEnabled(boolean enabled) { this.rpalObjectPoolingEnabled = enabled; }
    
    public boolean isRpalTickSchedulerEnabled() { return rpalTickSchedulerEnabled; }
    public void setRpalTickSchedulerEnabled(boolean enabled) { this.rpalTickSchedulerEnabled = enabled; }
    
    public boolean isRpalPreallocateBuffers() { return rpalPreallocateBuffers; }
    public void setRpalPreallocateBuffers(boolean enabled) { this.rpalPreallocateBuffers = enabled; }
    
    public int getRpalAsyncThreads() { return rpalAsyncThreads; }
    public void setRpalAsyncThreads(int threads) { this.rpalAsyncThreads = threads; }
}
