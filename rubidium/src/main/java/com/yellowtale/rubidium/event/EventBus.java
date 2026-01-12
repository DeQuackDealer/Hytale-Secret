package com.yellowtale.rubidium.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {
    private static final Logger logger = LoggerFactory.getLogger(EventBus.class);
    
    private final Map<Class<? extends Event>, List<HandlerEntry>> handlers = new ConcurrentHashMap<>();
    private final Set<Object> registeredListeners = ConcurrentHashMap.newKeySet();
    
    public void register(Object listener) {
        if (registeredListeners.contains(listener)) {
            return;
        }
        
        for (Method method : listener.getClass().getMethods()) {
            EventHandler annotation = method.getAnnotation(EventHandler.class);
            if (annotation == null) continue;
            
            Class<?>[] params = method.getParameterTypes();
            if (params.length != 1) continue;
            
            Class<?> eventClass = params[0];
            if (!Event.class.isAssignableFrom(eventClass)) continue;
            
            @SuppressWarnings("unchecked")
            Class<? extends Event> typedEventClass = (Class<? extends Event>) eventClass;
            
            handlers.computeIfAbsent(typedEventClass, k -> new CopyOnWriteArrayList<>())
                .add(new HandlerEntry(listener, method, annotation.priority(), annotation.ignoreCancelled()));
            
            handlers.get(typedEventClass).sort(Comparator.comparingInt(h -> -h.priority.ordinal()));
        }
        
        registeredListeners.add(listener);
    }
    
    public void unregister(Object listener) {
        registeredListeners.remove(listener);
        
        for (List<HandlerEntry> entries : handlers.values()) {
            entries.removeIf(e -> e.listener == listener);
        }
    }
    
    public <T extends Event> T fire(T event) {
        List<HandlerEntry> entries = handlers.get(event.getClass());
        if (entries == null || entries.isEmpty()) {
            return event;
        }
        
        for (HandlerEntry entry : entries) {
            if (event instanceof Cancellable && ((Cancellable) event).isCancelled() && entry.ignoreCancelled) {
                continue;
            }
            
            try {
                entry.method.invoke(entry.listener, event);
            } catch (Exception e) {
                logger.error("Error dispatching event {} to {}", 
                    event.getClass().getSimpleName(), 
                    entry.listener.getClass().getName(), 
                    e);
            }
        }
        
        return event;
    }
    
    public void fireAsync(Event event) {
        Thread.ofVirtual().start(() -> fire(event));
    }
    
    private static class HandlerEntry {
        final Object listener;
        final Method method;
        final EventPriority priority;
        final boolean ignoreCancelled;
        
        HandlerEntry(Object listener, Method method, EventPriority priority, boolean ignoreCancelled) {
            this.listener = listener;
            this.method = method;
            this.priority = priority;
            this.ignoreCancelled = ignoreCancelled;
        }
    }
}
