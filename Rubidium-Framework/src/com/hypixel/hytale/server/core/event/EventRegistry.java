package com.hypixel.hytale.server.core.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Stub for Hytale's EventRegistry.
 * At runtime, the real EventRegistry from HytaleServer.jar will be used.
 */
public class EventRegistry {
    
    private static final EventRegistry INSTANCE = new EventRegistry();
    private final Map<Class<?>, List<Consumer<?>>> handlers = new ConcurrentHashMap<>();
    
    public static EventRegistry get() {
        return INSTANCE;
    }
    
    @SuppressWarnings("unchecked")
    public <T> void register(Class<T> eventClass, Consumer<T> handler) {
        handlers.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>())
                .add(handler);
    }
    
    public <T> void unregister(Class<T> eventClass, Consumer<T> handler) {
        List<Consumer<?>> list = handlers.get(eventClass);
        if (list != null) {
            list.remove(handler);
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T> void fire(T event) {
        List<Consumer<?>> list = handlers.get(event.getClass());
        if (list != null) {
            for (Consumer<?> handler : list) {
                try {
                    ((Consumer<T>) handler).accept(event);
                } catch (Exception e) {
                    System.err.println("Error handling event: " + e.getMessage());
                }
            }
        }
    }
}
