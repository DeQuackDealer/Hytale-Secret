package com.hypixel.hytale.server.core.ui.builder;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Consumer;

public class UIEventBuilder {
    
    private final Map<String, EventHandler<?>> handlers = new HashMap<>();
    
    public <T> UIEventBuilder on(String event, BuilderCodec<T> codec) {
        handlers.put(event, new EventHandler<>(codec, v -> {}));
        return this;
    }
    
    public <T> UIEventBuilder on(String elementId, String eventType, BuilderCodec<T> codec) {
        String key = elementId + ":" + eventType;
        handlers.put(key, new EventHandler<>(codec, v -> {}));
        return this;
    }
    
    public <T> UIEventBuilder on(String elementId, String eventType, BuilderCodec<T> codec, Consumer<T> handler) {
        String key = elementId + ":" + eventType;
        handlers.put(key, new EventHandler<>(codec, handler));
        return this;
    }
    
    public UIEventBuilder on(String event) {
        return this;
    }
    
    public UIEventBuilder onClick(String elementId, Runnable handler) {
        return on(elementId, "click", BuilderCodec.VOID, v -> handler.run());
    }
    
    public UIEventBuilder onToggle(String elementId, Consumer<Boolean> handler) {
        return on(elementId, "toggle", BuilderCodec.BOOLEAN, handler);
    }
    
    public UIEventBuilder onSliderChange(String elementId, Consumer<Float> handler) {
        return on(elementId, "change", BuilderCodec.FLOAT, handler);
    }
    
    public UIEventBuilder onSubmit(String elementId, Consumer<String> handler) {
        return on(elementId, "submit", BuilderCodec.STRING, handler);
    }
    
    @SuppressWarnings("unchecked")
    public <T> void dispatch(String elementId, String eventType, String data) {
        String key = elementId + ":" + eventType;
        EventHandler<?> handler = handlers.get(key);
        if (handler != null) {
            ((EventHandler<T>) handler).handle(data);
        }
    }
    
    public boolean hasHandler(String elementId, String eventType) {
        return handlers.containsKey(elementId + ":" + eventType);
    }
    
    public int getHandlerCount() {
        return handlers.size();
    }
    
    private static class EventHandler<T> {
        private final BuilderCodec<T> codec;
        private final Consumer<T> handler;
        
        EventHandler(BuilderCodec<T> codec, Consumer<T> handler) {
            this.codec = codec;
            this.handler = handler;
        }
        
        void handle(String data) {
            T value = codec.decode(data);
            handler.accept(value);
        }
    }
}
