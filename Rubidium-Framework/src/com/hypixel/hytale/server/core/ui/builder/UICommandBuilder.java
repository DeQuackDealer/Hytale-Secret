package com.hypixel.hytale.server.core.ui.builder;

import com.hypixel.hytale.protocol.packets.interface_.CustomUICommand;
import java.util.ArrayList;
import java.util.List;

public class UICommandBuilder {
    
    private final List<CustomUICommand> commands = new ArrayList<>();
    
    public UICommandBuilder clear(String id) {
        return this;
    }
    
    public UICommandBuilder remove(String id) {
        return this;
    }
    
    public UICommandBuilder append(String parent) {
        return this;
    }
    
    public UICommandBuilder append(String parent, String id) {
        return this;
    }
    
    public UICommandBuilder appendInline(String parent, String id) {
        return this;
    }
    
    public UICommandBuilder insertBefore(String target, String id) {
        return this;
    }
    
    public UICommandBuilder set(String path, String value) {
        return this;
    }
    
    public UICommandBuilder set(String path, boolean value) {
        return this;
    }
    
    public UICommandBuilder set(String path, int value) {
        return this;
    }
    
    public UICommandBuilder set(String path, float value) {
        return this;
    }
    
    public UICommandBuilder set(String path, double value) {
        return this;
    }
    
    public UICommandBuilder setNull(String path) {
        return this;
    }
    
    public <T> UICommandBuilder set(String path, T[] value) {
        return this;
    }
    
    public <T> UICommandBuilder set(String path, List<T> value) {
        return this;
    }
    
    public CustomUICommand[] getCommands() {
        return commands.toArray(new CustomUICommand[0]);
    }
}
