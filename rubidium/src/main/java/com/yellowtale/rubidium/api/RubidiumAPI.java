package com.yellowtale.rubidium.api;

import com.yellowtale.rubidium.RubidiumPlugin;
import com.yellowtale.rubidium.event.EventBus;
import com.yellowtale.rubidium.performance.PerformanceMonitor;

public class RubidiumAPI {
    private final RubidiumPlugin rubidium;
    
    public RubidiumAPI(RubidiumPlugin rubidium) {
        this.rubidium = rubidium;
    }
    
    public String getVersion() {
        return rubidium.getVersion();
    }
    
    public EventBus getEventBus() {
        return rubidium.getEventBus();
    }
    
    public PluginManager getPluginManager() {
        return rubidium.getPluginManager();
    }
    
    public PerformanceMonitor getPerformanceMonitor() {
        return rubidium.getPerformanceMonitor();
    }
    
    public Plugin getPlugin(String name) {
        return rubidium.getPluginManager().getPlugin(name);
    }
    
    public boolean isPluginEnabled(String name) {
        return rubidium.getPluginManager().getState(name) == PluginManager.PluginState.ENABLED;
    }
    
    public void registerListener(Object listener) {
        rubidium.getEventBus().register(listener);
    }
    
    public void unregisterListener(Object listener) {
        rubidium.getEventBus().unregister(listener);
    }
}
