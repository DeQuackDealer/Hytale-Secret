package com.hypixel.hytale.protocol.packets.interface_;

public interface CustomUICommand {
    
    enum CommandType {
        SET,
        CLEAR,
        REMOVE,
        APPEND,
        INSERT_BEFORE,
        SET_NULL
    }
    
    CommandType getType();
    String getPath();
    Object getValue();
    
    static CustomUICommand set(String path, Object value) {
        return new SetCommand(path, value);
    }
    
    static CustomUICommand clear(String id) {
        return new ClearCommand(id);
    }
    
    static CustomUICommand remove(String id) {
        return new RemoveCommand(id);
    }
    
    static CustomUICommand append(String parent, String id, String content) {
        return new AppendCommand(parent, id, content);
    }
    
    class SetCommand implements CustomUICommand {
        private final String path;
        private final Object value;
        
        SetCommand(String path, Object value) {
            this.path = path;
            this.value = value;
        }
        
        @Override
        public CommandType getType() {
            return CommandType.SET;
        }
        
        @Override
        public String getPath() {
            return path;
        }
        
        @Override
        public Object getValue() {
            return value;
        }
    }
    
    class ClearCommand implements CustomUICommand {
        private final String id;
        
        ClearCommand(String id) {
            this.id = id;
        }
        
        @Override
        public CommandType getType() {
            return CommandType.CLEAR;
        }
        
        @Override
        public String getPath() {
            return id;
        }
        
        @Override
        public Object getValue() {
            return null;
        }
    }
    
    class RemoveCommand implements CustomUICommand {
        private final String id;
        
        RemoveCommand(String id) {
            this.id = id;
        }
        
        @Override
        public CommandType getType() {
            return CommandType.REMOVE;
        }
        
        @Override
        public String getPath() {
            return id;
        }
        
        @Override
        public Object getValue() {
            return null;
        }
    }
    
    class AppendCommand implements CustomUICommand {
        private final String parent;
        private final String id;
        private final String content;
        
        AppendCommand(String parent, String id, String content) {
            this.parent = parent;
            this.id = id;
            this.content = content;
        }
        
        @Override
        public CommandType getType() {
            return CommandType.APPEND;
        }
        
        @Override
        public String getPath() {
            return parent;
        }
        
        @Override
        public Object getValue() {
            return new String[]{id, content};
        }
        
        public String getId() {
            return id;
        }
        
        public String getContent() {
            return content;
        }
    }
}
