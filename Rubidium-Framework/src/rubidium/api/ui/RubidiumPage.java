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

public abstract class RubidiumPage<T> {
    
    protected Player player;
    protected CustomPageLifetime lifetime = CustomPageLifetime.CLOSE_ON_DISCONNECT;
    protected final UIState state = new UIState();
    protected final Map<String, Consumer<T>> eventHandlers = new HashMap<>();
    
    public abstract String getPageId();
    public abstract String getUIPath();
    
    public void setPlayer(Player player) {
        this.player = player;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public void setLifetime(CustomPageLifetime lifetime) {
        this.lifetime = lifetime;
    }
    
    public CustomPageLifetime getLifetime() {
        return lifetime;
    }
    
    public UIState getState() {
        return state;
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
    
    protected void bindToggle(UIEventBuilder events, String elementId, Consumer<Boolean> handler) {
        events.on(elementId, "toggle", BuilderCodec.BOOLEAN, value -> {
            if (handler != null) {
                handler.accept(value);
            }
        });
    }
    
    protected void bindSlider(UIEventBuilder events, String elementId, Consumer<Float> handler) {
        events.on(elementId, "change", BuilderCodec.FLOAT, value -> {
            if (handler != null) {
                handler.accept(value);
            }
        });
    }
    
    protected void bindButton(UIEventBuilder events, String elementId, Runnable handler) {
        events.on(elementId, "click", BuilderCodec.VOID, v -> {
            if (handler != null) {
                handler.run();
            }
        });
    }
    
    protected void bindInput(UIEventBuilder events, String elementId, Consumer<String> handler) {
        events.on(elementId, "submit", BuilderCodec.STRING, value -> {
            if (handler != null) {
                handler.accept(value);
            }
        });
    }
    
    protected void setElementText(String elementId, String text) {
        state.set(elementId + ".text", text);
        rebuild();
    }
    
    protected void setElementVisible(String elementId, boolean visible) {
        state.set(elementId + ".visible", visible);
        rebuild();
    }
    
    protected void setElementEnabled(String elementId, boolean enabled) {
        state.set(elementId + ".enabled", enabled);
        rebuild();
    }
    
    protected void setToggleState(String elementId, boolean checked) {
        state.set(elementId + ".checked", checked);
        rebuild();
    }
    
    protected void setSliderValue(String elementId, float value) {
        state.set(elementId + ".value", value);
        rebuild();
    }
    
    public void rebuild() {
        if (player != null) {
            RubidiumUI.rebuild(player, this);
        }
    }
    
    public void close() {
        if (player != null) {
            RubidiumUI.closePage(player, getPageId());
        }
    }
}
