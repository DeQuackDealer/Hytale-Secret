package com.hypixel.hytale.server.core.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class EventRegistry {
    
    private static final Logger LOGGER = Logger.getLogger("EventRegistry");
    private static final EventRegistry INSTANCE = new EventRegistry();
    private final Map<Class<?>, List<Consumer<?>>> handlers = new ConcurrentHashMap<>();
    
    public static EventRegistry get() {
        return INSTANCE;
    }
    
    @SuppressWarnings("unchecked")
    public <T> void register(Class<T> eventClass, Consumer<T> handler) {
        handlers.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>())
                .add(handler);
        LOGGER.fine("[EventRegistry] Registered handler for " + eventClass.getSimpleName());
    }
    
    public <T> void unregister(Class<T> eventClass, Consumer<T> handler) {
        List<Consumer<?>> list = handlers.get(eventClass);
        if (list != null) {
            list.remove(handler);
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T> void fire(T event) {
        Class<?> eventClass = event.getClass();
        
        List<Consumer<?>> allHandlers = new ArrayList<>();
        allHandlers.addAll(handlers.getOrDefault(eventClass, List.of()));
        
        for (Class<?> iface : eventClass.getInterfaces()) {
            allHandlers.addAll(handlers.getOrDefault(iface, List.of()));
        }
        
        Class<?> superClass = eventClass.getSuperclass();
        while (superClass != null && superClass != Object.class) {
            allHandlers.addAll(handlers.getOrDefault(superClass, List.of()));
            for (Class<?> iface : superClass.getInterfaces()) {
                allHandlers.addAll(handlers.getOrDefault(iface, List.of()));
            }
            superClass = superClass.getSuperclass();
        }
        
        if (!allHandlers.isEmpty()) {
            LOGGER.info("[EventRegistry] Firing " + eventClass.getSimpleName() + " to " + allHandlers.size() + " handlers");
            
            for (Consumer<?> handler : allHandlers) {
                try {
                    ((Consumer<T>) handler).accept(event);
                } catch (Exception e) {
                    LOGGER.severe("[EventRegistry] Error handling event: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
    
    public int getHandlerCount() {
        return handlers.values().stream().mapToInt(List::size).sum();
    }
}
