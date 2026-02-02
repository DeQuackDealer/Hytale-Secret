package rubidium.api.ui;

import com.hypixel.hytale.server.api.player.Player;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUICommand;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class RubidiumOverlayPage<T> {
    
    protected Player player;
    protected final UIState state = new UIState();
    protected boolean visible = true;
    
    public abstract String getPageId();
    public abstract String getUIPath();
    
    public void setPlayer(Player player) {
        this.player = player;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    protected CustomPageLifetime customLifetime = CustomPageLifetime.ALWAYS;
    
    public void setLifetime(CustomPageLifetime lifetime) {
        this.customLifetime = lifetime != null ? lifetime : CustomPageLifetime.ALWAYS;
    }
    
    public CustomPageLifetime getLifetime() {
        return customLifetime;
    }
    
    public UIState getState() {
        return state;
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public void setVisible(boolean visible) {
        this.visible = visible;
        state.set("root.visible", visible);
        rebuild();
    }
    
    protected void registerEvents(UIEventBuilder events) {
    }
    
    public CustomUICommand[] buildInitialCommands() {
        UICommandBuilder builder = new UICommandBuilder();
        buildUI(builder);
        return builder.getCommands();
    }
    
    public CustomUICommand[] buildUpdateCommands() {
        if (!state.isDirty()) {
            return new CustomUICommand[0];
        }
        
        UICommandBuilder builder = new UICommandBuilder();
        applyStateChanges(builder);
        state.clearDirty();
        return builder.getCommands();
    }
    
    protected void buildUI(UICommandBuilder builder) {
    }
    
    protected void applyStateChanges(UICommandBuilder builder) {
        for (Map.Entry<String, Object> entry : state.getDirtyEntries().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof String) {
                builder.set(key, (String) value);
            } else if (value instanceof Boolean) {
                builder.set(key, (Boolean) value);
            } else if (value instanceof Integer) {
                builder.set(key, (Integer) value);
            } else if (value instanceof Float) {
                builder.set(key, (Float) value);
            } else if (value instanceof Double) {
                builder.set(key, (Double) value);
            }
        }
    }
    
    public void onOpened() {
    }
    
    public void onClosed() {
    }
    
    protected void bindButton(UIEventBuilder events, String elementId, Runnable handler) {
        events.on(elementId, "click", BuilderCodec.VOID, v -> {
            if (handler != null) {
                handler.run();
            }
        });
    }
    
    public void rebuild() {
        if (player != null) {
            RubidiumUI.rebuildOverlay(player, this);
        }
    }
    
    public void close() {
        if (player != null) {
            RubidiumUI.closeOverlay(player, getPageId());
        }
    }
}
