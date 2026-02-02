package com.hypixel.hytale.server.core.ui.builder;

import com.hypixel.hytale.protocol.packets.interface_.CustomUICommand;
import java.util.ArrayList;
import java.util.List;

public class UICommandBuilder {
    
    private final List<CustomUICommand> commands = new ArrayList<>();
    
    public UICommandBuilder clear(String id) {
        commands.add(CustomUICommand.clear(id));
        return this;
    }
    
    public UICommandBuilder remove(String id) {
        commands.add(CustomUICommand.remove(id));
        return this;
    }
    
    public UICommandBuilder append(String parent) {
        commands.add(CustomUICommand.append(parent, "", ""));
        return this;
    }
    
    public UICommandBuilder append(String parent, String id) {
        commands.add(CustomUICommand.append(parent, id, ""));
        return this;
    }
    
    public UICommandBuilder append(String parent, String id, String content) {
        commands.add(CustomUICommand.append(parent, id, content));
        return this;
    }
    
    public UICommandBuilder appendInline(String parent, String id) {
        commands.add(CustomUICommand.append(parent, id, ""));
        return this;
    }
    
    public UICommandBuilder insertBefore(String target, String id) {
        commands.add(new InsertBeforeCommand(target, id));
        return this;
    }
    
    private static class InsertBeforeCommand implements CustomUICommand {
        private final String target;
        private final String id;
        
        InsertBeforeCommand(String target, String id) {
            this.target = target;
            this.id = id;
        }
        
        @Override
        public CustomUICommand.CommandType getType() {
            return CustomUICommand.CommandType.INSERT_BEFORE;
        }
        
        @Override
        public String getPath() {
            return target;
        }
        
        @Override
        public Object getValue() {
            return id;
        }
    }
    
    public UICommandBuilder set(String path, String value) {
        commands.add(CustomUICommand.set(path, value));
        return this;
    }
    
    public UICommandBuilder set(String path, boolean value) {
        commands.add(CustomUICommand.set(path, value));
        return this;
    }
    
    public UICommandBuilder set(String path, int value) {
        commands.add(CustomUICommand.set(path, value));
        return this;
    }
    
    public UICommandBuilder set(String path, float value) {
        commands.add(CustomUICommand.set(path, value));
        return this;
    }
    
    public UICommandBuilder set(String path, double value) {
        commands.add(CustomUICommand.set(path, value));
        return this;
    }
    
    public UICommandBuilder setNull(String path) {
        commands.add(CustomUICommand.set(path, null));
        return this;
    }
    
    public <T> UICommandBuilder set(String path, T[] value) {
        commands.add(CustomUICommand.set(path, value));
        return this;
    }
    
    public <T> UICommandBuilder set(String path, List<T> value) {
        commands.add(CustomUICommand.set(path, value));
        return this;
    }
    
    public CustomUICommand[] getCommands() {
        return commands.toArray(new CustomUICommand[0]);
    }
    
    public int getCommandCount() {
        return commands.size();
    }
    
    public void reset() {
        commands.clear();
    }
}
