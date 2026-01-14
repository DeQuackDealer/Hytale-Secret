package rubidium.event;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class EventBus {
    
    private static final Logger logger = Logger.getLogger("Rubidium-EventBus");
    
    private final Map<Class<? extends Event>, List<RegisteredListener>> listeners;
    private final ExecutorService asyncExecutor;
    
    public EventBus() {
        this.listeners = new ConcurrentHashMap<>();
        this.asyncExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "Rubidium-Event-Async");
            t.setDaemon(true);
            return t;
        });
    }
    
    public void register(Object listener) {
        Class<?> clazz = listener.getClass();
        
        for (Method method : clazz.getDeclaredMethods()) {
            EventHandler annotation = method.getAnnotation(EventHandler.class);
            if (annotation == null) continue;
            
            Class<?>[] params = method.getParameterTypes();
            if (params.length != 1) {
                logger.warn("EventHandler method must have exactly 1 parameter: " + method);
                continue;
            }
            
            if (!Event.class.isAssignableFrom(params[0])) {
                logger.warn("EventHandler parameter must extend Event: " + method);
                continue;
            }
            
            @SuppressWarnings("unchecked")
            Class<? extends Event> eventClass = (Class<? extends Event>) params[0];
            
            method.setAccessible(true);
            
            RegisteredListener reg = new RegisteredListener(
                listener, method, annotation.priority(), annotation.ignoreCancelled(), annotation.async()
            );
            
            listeners.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>()).add(reg);
            listeners.get(eventClass).sort(Comparator.comparingInt(l -> l.priority().ordinal()));
            
            logger.fine("Registered event handler: " + eventClass.getSimpleName() + " -> " + method.getName());
        }
    }
    
    public void unregister(Object listener) {
        for (List<RegisteredListener> listenerList : listeners.values()) {
            listenerList.removeIf(l -> l.listener() == listener);
        }
    }
    
    public <T extends Event> T call(T event) {
        List<RegisteredListener> listenerList = listeners.get(event.getClass());
        if (listenerList == null || listenerList.isEmpty()) {
            return event;
        }
        
        for (RegisteredListener reg : listenerList) {
            if (event instanceof Cancellable c && c.isCancelled() && reg.ignoreCancelled()) {
                continue;
            }
            
            try {
                if (reg.async()) {
                    asyncExecutor.submit(() -> invokeHandler(reg, event));
                } else {
                    invokeHandler(reg, event);
                }
            } catch (Exception e) {
                logger.error("Error handling event " + event.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        return event;
    }
    
    public <T extends Event> CompletableFuture<T> callAsync(T event) {
        return CompletableFuture.supplyAsync(() -> call(event), asyncExecutor);
    }
    
    private void invokeHandler(RegisteredListener reg, Event event) {
        try {
            reg.method().invoke(reg.listener(), event);
        } catch (Exception e) {
            logger.error("Failed to invoke event handler: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void shutdown() {
        asyncExecutor.shutdown();
        try {
            if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface EventHandler {
        EventPriority priority() default EventPriority.NORMAL;
        boolean ignoreCancelled() default false;
        boolean async() default false;
    }
    
    public enum EventPriority {
        LOWEST, LOW, NORMAL, HIGH, HIGHEST, MONITOR
    }
    
    public interface Cancellable {
        boolean isCancelled();
        void setCancelled(boolean cancelled);
    }
    
    public static abstract class Event {
        private final boolean async;
        
        protected Event() {
            this(false);
        }
        
        protected Event(boolean async) {
            this.async = async;
        }
        
        public boolean isAsync() {
            return async;
        }
    }
    
    public static abstract class CancellableEvent extends Event implements Cancellable {
        private boolean cancelled = false;
        
        protected CancellableEvent() {
            super();
        }
        
        protected CancellableEvent(boolean async) {
            super(async);
        }
        
        @Override
        public boolean isCancelled() {
            return cancelled;
        }
        
        @Override
        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }
    }
    
    private record RegisteredListener(
        Object listener, 
        Method method, 
        EventPriority priority, 
        boolean ignoreCancelled,
        boolean async
    ) {}
}
