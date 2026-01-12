package com.yellowtale.rubidium.event.events;

import com.yellowtale.rubidium.api.Plugin;
import com.yellowtale.rubidium.event.Event;

public class PluginEvent extends Event {
    private final Plugin plugin;
    private final PluginEventType type;
    
    public PluginEvent(Plugin plugin, PluginEventType type) {
        super();
        this.plugin = plugin;
        this.type = type;
    }
    
    public Plugin getPlugin() {
        return plugin;
    }
    
    public PluginEventType getType() {
        return type;
    }
    
    public enum PluginEventType {
        LOAD,
        ENABLE,
        DISABLE,
        UNLOAD
    }
}
