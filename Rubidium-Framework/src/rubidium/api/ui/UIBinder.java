package rubidium.api.ui;

import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class UIBinder {
    
    private final UIEventBuilder events;
    private final Map<String, Object> bindings = new HashMap<>();
    
    public UIBinder(UIEventBuilder events) {
        this.events = events;
    }
    
    public UIBinder bindToggle(String elementId, Consumer<Boolean> handler) {
        events.on(elementId, "toggle", BuilderCodec.BOOLEAN, handler);
        bindings.put(elementId + ":toggle", handler);
        return this;
    }
    
    public UIBinder bindSlider(String elementId, Consumer<Float> handler) {
        events.on(elementId, "change", BuilderCodec.FLOAT, handler);
        bindings.put(elementId + ":change", handler);
        return this;
    }
    
    public UIBinder bindClick(String elementId, Runnable handler) {
        events.on(elementId, "click", BuilderCodec.VOID, v -> handler.run());
        bindings.put(elementId + ":click", handler);
        return this;
    }
    
    public UIBinder bindSubmit(String elementId, Consumer<String> handler) {
        events.on(elementId, "submit", BuilderCodec.STRING, handler);
        bindings.put(elementId + ":submit", handler);
        return this;
    }
    
    public UIBinder bindInput(String elementId, Consumer<String> handler) {
        events.on(elementId, "input", BuilderCodec.STRING, handler);
        bindings.put(elementId + ":input", handler);
        return this;
    }
    
    public UIBinder bindSelect(String elementId, Consumer<Integer> handler) {
        events.on(elementId, "select", BuilderCodec.INTEGER, handler);
        bindings.put(elementId + ":select", handler);
        return this;
    }
    
    public UIBinder bindHover(String elementId, Consumer<Boolean> handler) {
        events.on(elementId, "hover", BuilderCodec.BOOLEAN, handler);
        bindings.put(elementId + ":hover", handler);
        return this;
    }
    
    public UIBinder bindFocus(String elementId, Consumer<Boolean> handler) {
        events.on(elementId, "focus", BuilderCodec.BOOLEAN, handler);
        bindings.put(elementId + ":focus", handler);
        return this;
    }
    
    public UIBinder bindCustom(String elementId, String eventType, Consumer<String> handler) {
        events.on(elementId, eventType, BuilderCodec.STRING, handler);
        bindings.put(elementId + ":" + eventType, handler);
        return this;
    }
    
    public <T> UIBinder bind(String elementId, String eventType, BuilderCodec<T> codec, Consumer<T> handler) {
        events.on(elementId, eventType, codec, handler);
        bindings.put(elementId + ":" + eventType, handler);
        return this;
    }
    
    public boolean hasBinding(String elementId, String eventType) {
        return bindings.containsKey(elementId + ":" + eventType);
    }
    
    public int getBindingCount() {
        return bindings.size();
    }
}
