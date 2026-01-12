package com.yellowtale.rubidium.api.event;

import com.yellowtale.rubidium.annotations.EventHandler;
import com.yellowtale.rubidium.api.RubidiumPlugin;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {
    
    private final Map<Class<? extends Event>, List<RegisteredListener>> listeners;
    
    public EventBus() {
        this.listeners = new ConcurrentHashMap<>();
    }
    
    public void registerListener(RubidiumPlugin plugin, Object listener) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            EventHandler handler = method.getAnnotation(EventHandler.class);
            if (handler == null) continue;
            
            Class<?>[] params = method.getParameterTypes();
            if (params.length != 1) continue;
            
            Class<?> eventClass = params[0];
            if (!Event.class.isAssignableFrom(eventClass)) continue;
            
            @SuppressWarnings("unchecked")
            Class<? extends Event> eventType = (Class<? extends Event>) eventClass;
            
            method.setAccessible(true);
            
            RegisteredListener registered = new RegisteredListener(
                plugin,
                listener,
                method,
                handler.priority(),
                handler.ignoreCancelled()
            );
            
            listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                    .add(registered);
            
            sortListeners(eventType);
        }
    }
    
    public void unregisterListener(RubidiumPlugin plugin) {
        for (List<RegisteredListener> list : listeners.values()) {
            list.removeIf(l -> l.getPlugin().equals(plugin));
        }
    }
    
    public void unregisterAll() {
        listeners.clear();
    }
    
    public <T extends Event> T callEvent(T event) {
        List<RegisteredListener> handlers = listeners.get(event.getClass());
        if (handlers == null || handlers.isEmpty()) {
            return event;
        }
        
        for (RegisteredListener listener : handlers) {
            if (event instanceof Cancellable) {
                if (((Cancellable) event).isCancelled() && listener.isIgnoreCancelled()) {
                    continue;
                }
            }
            
            try {
                listener.callEvent(event);
            } catch (Exception e) {
                listener.getPlugin().getLogger().error(
                    "Error handling event {} in plugin {}",
                    event.getEventName(),
                    listener.getPlugin().getId(),
                    e
                );
            }
        }
        
        return event;
    }
    
    private void sortListeners(Class<? extends Event> eventType) {
        List<RegisteredListener> list = listeners.get(eventType);
        if (list != null) {
            list.sort(Comparator.comparingInt(l -> l.getPriority().getSlot()));
        }
    }
    
    public static class RegisteredListener {
        private final RubidiumPlugin plugin;
        private final Object listener;
        private final Method method;
        private final EventPriority priority;
        private final boolean ignoreCancelled;
        
        public RegisteredListener(
                RubidiumPlugin plugin,
                Object listener,
                Method method,
                EventPriority priority,
                boolean ignoreCancelled
        ) {
            this.plugin = plugin;
            this.listener = listener;
            this.method = method;
            this.priority = priority;
            this.ignoreCancelled = ignoreCancelled;
        }
        
        public RubidiumPlugin getPlugin() {
            return plugin;
        }
        
        public EventPriority getPriority() {
            return priority;
        }
        
        public boolean isIgnoreCancelled() {
            return ignoreCancelled;
        }
        
        public void callEvent(Event event) throws Exception {
            method.invoke(listener, event);
        }
    }
}
